# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Diffusion is an Android note app (Kotlin + Jetpack Compose) whose notes are plain files in a real Git repository. Git itself is not implemented in Kotlin: a Rust `cdylib` (`libgit_wrapper.so`, crate `git_wrapper` in `app/src/main/rust`) wraps `libgit2` (with vendored openssl/libssh2) and is called through JNI.

Consequence for any build: **the Rust library must be compiled before Gradle can produce a runnable APK.** The `.so` files live in `app/src/main/jniLibs/` (gitignored, LFS-tracked in CI).

## Build & test

Two toolchains, pinned by files that must stay in sync:

- `app/src/main/rust/RUST_VERSION` and `app/src/main/rust/NDK_VERSION` — read by CI
- `ndkVersion` in `app/build.gradle.kts` — must equal `NDK_VERSION`
- `flake.nix` mirrors all three for the NixOS dev shell (`nix develop`)

### Rust layer (run first, from `app/src/main/rust`)

```bash
make build_install                 # debug build for both targets + copy to ../jniLibs
make build_install DEBUG=0         # release build
make NDK_PATH=/path/to/ndk/toolchains/llvm/prebuilt/linux-x86_64/bin -C app/src/main/rust build_install
cargo test                         # host tests (no Android needed)
cargo test test_name -- --nocapture
make fmt                           # cargo fmt --all
make fix                           # clippy --fix
```

`NDK_PATH` must point at the NDK **bin** directory; the Makefile defaults to `~/Android/Sdk/ndk/<version>/toolchains/...`. Targets needed: `rustup target add x86_64-linux-android aarch64-linux-android`.

On Windows, openssl cannot be built from source — run `make unzip_openssl_prebuild` first, which extracts `openssl-prebuild/{x86_64,aarch64}/install.zip`; the Makefile then switches to `OPENSSL_NO_VENDOR=1`. See `BUILD.md`.

### Android layer (from repo root)

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest                                    # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests '*PlatformTest*'           # single unit test class
./gradlew connectedDebugAndroidTest                            # instrumented tests, needs a device
./gradlew lintDebug
./gradlew :app:generateBaselineProfile                         # records the profile, needs a device
just fix                                                       # ./gradlew lintFix
just prettier                                                  # formats md/yml/json via npx prettier
```

`:baselineprofile` is a `com.android.test` module whose only test walks the app
(start, scroll, open a note, back) so that `androidx.baselineprofile` can record
what to precompile. It runs on a device or an emulator and never in CI, and the
device needs a repository already set up or the walk ends in the setup screen.
The plugin is pinned to a `1.5.0-alpha` because the last stable refuses AGP 9.
`ui-tooling-preview` is `compileOnly` plus `debugImplementation`: the previews
sit next to what they preview, so the annotation has to compile everywhere, but
the release must not carry the package.

Build variants: `debug` (`.debug` suffix), `nightly` (release-shrunk, signed with the committed `nightly-signing-key.jks`, `.nightly` suffix), `release` (signing via `KEY_ALIAS`/`KEY_PASSWORD`/`STORE_PASSWORD` env vars + `app/key.jks`).

CI (`.github/workflows/ci.yml`) runs exactly: Rust `make build_install`, then `./gradlew assembleDebug lintDebug testDebugUnitTest`, plus `cargo test` in a separate job.

## Architecture

### Layering

```
Compose UI (ui/screen/**)  ->  ViewModels (ui/viewmodel/**)
        -> StorageManager (single write path: filesystem + Room + git)
             -> FileSystem (data/platform)   -> real files in the repo
             -> RepoDatabaseDao (Room + FTS4) -> search/listing index
             -> GitManager                    -> JNI -> Rust -> libgit2
```

`MyApp.appModule` is a hand-rolled service locator (`AppModule`/`AppModuleImpl`, all `by lazy`). There is no DI framework — new singletons go there, and managers reach each other via `MyApp.appModule.*`.

Writes to storage or git run in `AppModule.appScope`, not in `viewModelScope`: the edit view model is cleared right after saving, which would cancel the write.

### The database is a derived cache, not the source of truth

The files on disk are authoritative. Room mirrors them so the note list and FTS search are fast. `AppPreferences.databaseCommit` stores the commit the DB was built from; `StorageManager.updateDatabaseWithoutLocker` compares it against `gitManager.lastCommit()` and, on mismatch, runs `dao.clearAndInit(...)` — a full rebuild from the filesystem, dates included. Schema migrations use `fallbackToDestructiveMigration`, and the destructive-migration callback clears `databaseCommit` so the next start rebuilds. Never treat Room rows as data that can survive independently of the files.

Room is opened through `RequerySQLiteOpenHelperFactory` (not the platform SQLite) so custom SQL functions `rank`, `parentPath`, `fullName` can be registered — the FTS ranking and the folder queries in `Dao.kt` depend on them. The **note** queries do not: `Note` stores `parentPath` and `fileName` as indexed columns, derived from `relativePath` by the constructor defaults, because a value computed per row cannot use an index. Anything that builds a `Note` by hand (rather than `Note.new` or `copy` without touching `relativePath`) has to keep them consistent.

The note list does not read `Note` at all: `gridNotes`/`gridNotesWithQuery` project onto `NoteHeader` (path, name, date, id) so the content of every listed note does not sit in memory to draw two lines of text. Tapping a row loads the note by primary key (`Dao.note`) before the editor opens — a header cannot be handed to the editor, which is the point of it being its own type.

`StorageManager` holds a `Mutex` around every mutation; `GitManager` holds another around every libgit2 call. Mutating notes outside `StorageManager` will desync the DB from disk.

Writing and syncing are separate: `StorageManager.update` writes the files and the Room rows and stops there, so edits live in the working tree uncommitted, and the sync button carries a dot (`hasLocalChanges`) for as long as that is true. `syncWithRemote()` is the only thing that commits (`commitAll`), pulls and pushes. It runs when the user taps the cloud button, and on its own when the app opens (`MainViewModel.tryInit`) and when it is left (`MainActivity.onStop`), both with `announceErrors = false` so a missing network shows on the icon instead of opening a tooltip over the list, and both only while `AppPreferences.syncOnOpenAndClose` is on. Four things guard that automatic sync: it returns at once when `gitManager.isRepoInitialized` is false, because the app is stopped several times during the setup and the failure used to stay on the button of the repository that was then cloned; it `join()`s `StorageManager.lastWrite` first, so the editor's `ON_STOP` write is in the commit rather than one sync late — which is why every note write goes through `StorageManager.startWrite`; `hasLocalChanges` is set by any write but re-asked from git (`refreshChangeState`) when a note was written back to what it was, and again when the repository is opened at start, because being force closed does not commit anything and a fresh process starts the flag at false; and it waits for the network (`NetworkMonitor.awaitOnline`, `ACCESS_NETWORK_STATE`, up to 8 s, `VALIDATED` rather than merely `INTERNET`) before it touches the remote — the app is opened and left exactly when a phone comes back from sleep, and libgit2 failed at dns for a sync nobody asked for. Without a network, a sync that was asked for says so and one that ran by itself says nothing. `refreshLocalChanges()` runs **before** `SyncState.Ok` is emitted, or the dot stands under a button that has already said the notes went out, for as long as walking the working tree takes. The one state the tap sets itself is `SyncState.Starting` (`announceSyncStart()`, not suspending, called before the coroutine): everything up to the first pull is otherwise a button that looks like it was not pressed. `GridViewModel.reloadDatabase` calls `updateDatabase(...)` instead: files into the database, no network. There is no pull to refresh — a gesture on the list looked like syncing while doing something else. Because HEAD does not move while notes are written, the `databaseCommit` check stays satisfied and the DB is kept in step incrementally; a rebuild reads the working tree, so uncommitted notes survive it.

The date a note shows is the file's mtime and only that. Git is asked once per checkout instead of once per rebuild: `apply_commit_timestamps` (rust) stamps a file that agrees with HEAD with the time of the commit that last wrote it, and runs at the end of `clone_repo` and `pull` — the two things that write files the user did not write. A modified file is skipped, because its own mtime is when the user typed and no commit can speak for that. `SetupViewModel.openRepo` calls it once for a repository the app has not seen before; nothing calls it per start, and committing does not move a date because a commit does not touch the working tree.

Two things narrow that further, and both are about a date the user would recognise. A pull dates **only what it wrote** (`date_pulled_notes` diffs the commit HEAD stood at before the merge against the one it stands at after, and hands the paths to `apply_commit_timestamps_to`): the sync commits before it pulls, so without that every note just committed would agree with HEAD as well and be moved to the minute of the sync. And a note undone back to what it was keeps its old date — `TextVM` holds the note it was opened with (`openedNote`), a save that ends at exactly that carries its `lastModifiedTimeMillis`, and `StorageManager` stamps the file with the date its row holds (`dateBy`) so the two agree before and after a rebuild.

The reading mode parses the note with `rememberMarkdownState`, which re-parses whenever its input changes and shows nothing while it does. Two rules keep that from being felt: the state lives outside the read-only branch in `MarkDownContent`, so it survives switching to writing and back, and its input only follows the note when the reading mode is entered — an unchanged note is never parsed twice. A ticked checkbox is therefore **not** allowed to reach it: the tick is remembered per offset and written to the note separately (`[ ]` and `[x]` are the same length, so parser offsets stay valid).

The writing mode scrolls the column around the field, not the field itself (a `TextField` that scrolls itself is re-measured when the keyboard takes half the screen and comes back at the first line). Where the caret has to be for it to be seen is decided by `CaretScroller` (`ui/screen/app/edit/CaretIntoView.kt`) and **not** by the column: `Modifier.caretIntoView` is a `BringIntoViewModifierNode` on the field, so every request stops there and nothing is passed on. That is what makes it possible to ignore the wrong one — a field that gains focus asks straight away, and until the tap has been applied the caret is still where the note was opened, so the ask arrived as "scroll to the top". `hold()` takes `KEEP_SCROLL_FRAMES` frames out for that. What is answered is answered instantly, never animated: a caret that has left the visible band is brought back with a line of room on either side (that is also what carries the view along while typing), and a caret the keyboard has just covered goes to the middle of what is left (`keepCaretVisible()`, triggered by `ScrollState.viewportSize` changing). A caret that can already be seen is not moved at all. The reader pads its list with `TextFieldDefaults.contentPaddingWithoutLabel()` so that reading and writing start at the same height.

The editor has no save button: `TextVM.scheduleSave()` writes the note after the last keystroke, and `saveNow()` runs when the editor is left, on `ON_STOP` and in `onCleared`. The pause is `SAVE_DEBOUNCE_MS` up to `CHEAP_NOTE_CHARS` and grows with the note beyond that, to `MAX_SAVE_DEBOUNCE_MS`: a save writes the whole file, the whole row and the search index built again from it, and a note with a book pasted into it was paying that twice a second. Nothing is risked, because the three unconditional saves above still write straight away. `NoteSaver` is still there but only as the net for text that has no usable file name yet. Because that runs constantly, `StorageManager.updateNote` only deletes the old row and the old file when the note was renamed; otherwise the row is rewritten in place and `write` truncates the file. `Dao.insertNote` is **not** an `@Upsert`: Room implements that by inserting and catching the constraint violation, so every save threw and caught an exception and bound the whole content twice — logcat carried 172 "UNIQUE constraint failed" lines from one session. It updates first and inserts only when nothing was updated; `clearAndInit`, where the table was just cleared, calls `insertNoteRow`/`insertNoteFolderRow` straight.

### JNI boundary

Kotlin side: `manager/GitManager.kt` and `manager/MimeTypeManager.kt` declare top-level `external fun`s. Because they are top-level, the JVM looks for them on the synthetic classes `GitManagerKt` / `MimeTypeManagerKt` — the Rust `native_method!` declarations in `rust/src/lib.rs` name exactly those `java_type`s. Renaming a Kotlin file or moving an `external fun` breaks linkage at runtime, not at compile time.

Return convention: most calls return `jint`, `0` = OK and negative = the libgit2 raw error code, which `GitManager` turns into a `GitException` with a localized string. Two things ride along with that: `MERGE_CONFLICT` (`-1000`) and `UNRESOLVED_CONFLICT` (`-1001`), both defined in `rust/src/error.rs` and in `GitManager` — keep the pairs equal — are not libgit2 codes but a pull that could not be merged and a commit that would have carried conflict markers, and `lastErrorMessageLib()` hands over the message behind the last code so the user is not shown a bare number (reading it clears it). Callbacks go the other way too: `GitManager.progressCb` is annotated `@Keep` and invoked from Rust during clone (keep `@Keep` — release builds are minified).

Rust side keeps the open repository in a process-global `static REPO: LazyLock<Mutex<Option<Repository>>>` (`libgit2/mod.rs`), so "open repo / close repo" is global state, not a handle passed around. `init_lib` also applies an SSH workaround by setting `HOME`/`GIT_OPT_SET_HOMEDIR` to the app's `filesDir`. Everything about reaching the far end lives in `libgit2/transport.rs`: TLS certificates are validated by libgit2 (`CertificatePassthrough`); ssh host keys are pinned on first use in `.ssh/pinned_hosts`, because libgit2 would only consult `known_hosts`, which the app never fills.

What a commit is called is decided on the rust side, because that is the side that stages the files: `commit_message` diffs the index it just wrote against HEAD and names the notes — `[fresh.md] added, [kept.md] changed, [gone.md] deleted`, with the first `MAX_NAMES_IN_SUBJECT` of a group in the subject and the rest counted there and listed underneath. Kotlin passes only the name to fall back on when there is nothing to name; a merge that changed no file is called "Merge".

`lib.rs` holds the `native_method!` table and the entry points it names, and nothing else — the error type and `LAST_ERROR` are in `error.rs`, the credentials arriving from Kotlin in `cred.rs`, the clone progress callback in `callback.rs`.

### Supported file extensions

The list of extensions treated as notes lives in `app/src/main/rust/supported_extensions/{text,markdown}.txt`, is embedded at compile time via `include_lines!`, and is looked up with `binary_search` — so the files **must stay sorted**. `build.rs` sorts them on every Rust build, and `just sort-supported-extension` does it manually. Kotlin asks Rust about extensions (`extensionType`, `isExtensionSupportedLib`), including inside `Dao.clearAndInit`, so adding an extension is a Rust-side change that requires rebuilding the `.so`.

### Navigation & UI

`navigation-reimagined` with `@Parcelize` sealed destinations (`ui/destination/`): top-level `Destination.Setup` vs `Destination.App`, then `AppDestination.{Grid, Edit, Settings}` — `Grid` is the note list, the staggered grid it was named after is gone. Folders are rows in that list, and `GridViewModel.currentNoteFolderRelativePath` is what `Dao.gridNotes` filters on with `parentPath(...)`, so the list is never recursive; only the search is. The folder rows and the `..` row are not a second list next to the paged notes: `GridViewModel.gridItems` folds them into the same `PagingData` as `GridItem.Folder`/`GridItem.ParentFolder` headers, because two sources answering at their own pace made opening a folder rebuild the layout twice. **Nothing may be combined into the notes before `cachedIn`.** A `PagingData` may be collected exactly once, and `combine` re-emits the one that arrived last whenever the other side changes — which hands `cachedIn` a stream that has already been read, and that is an outright crash (`Attempt to collect twice from pageEventFlow`). It was the selection that used to be combined in there, so the app died on the second tap of a multiple selection. `pagedNotes` therefore ends at `cachedIn` and the folder rows are combined in *after* it, which is safe and is the point of `cachedIn`: what it holds is multicast. Whether a row is selected is asked of `selectedNotes`/`selectedFolders` in the row itself, never folded into `GridNote`. The selection holds folders as well as notes (`selectionSize` is the two together), `selectAll()` marks what the list is currently showing, and deleting it takes each folder with everything inside it — notes that stand in one of those folders are dropped from the list first, so nothing complains about a file that is already gone.

A search shows no folder rows at all (it spans subfolders, so they are not what was asked for) and names its results by their full path.

Sorting, the folder to open on start and the full-path display are not preferences anymore — the constants live in `GridViewModel`. The undo history is not in the edit view model either (it is cleared as soon as a note is left): `AppModule.editHistoryStore` keeps one `EditHistory` per note id, the last `MAX_REMEMBERED_NOTES` of them in access order (each holds up to `MAX_UNDO_STEPS` whole copies of a note, and no more than `MAX_UNDO_CHARS` of text in total, so a very long note is undone less far rather than costing a garbage collection per keystroke). A note keeps its id across a save, so the history stays where it is. `EditHistory` also decides what counts as one undo step: the editor reports every keystroke, and consecutive states are folded together until something ends the edit (`EditHistoryTest` says where those lines fall). `MainActivity` picks the start destination by `runBlocking { vm.tryInit() }` (whether a repo is already configured).

Opening a repository takes a second or two of libgit2 plus the whole working tree being read, and it happens after the folder picker has closed — so `SetupViewModel.openRepo` emits `InitState.OpeningRepo` and then the folder-by-folder progress the clone already reported, and `NewRepoMethodScreen` shows that instead of the two buttons. Every way out of that function has to put the state back, or the screen keeps spinning.

The setup offers opening an existing repository or cloning one; there is no create, no account to log into and no choice of storage — a repository always lives in a folder on the shared storage, so `StorageConfiguration` is just a path. The folder is chosen with the system picker (`ACTION_OPEN_DOCUMENT_TREE`), and `pickedFolderPath` turns the tree uri back into a real path because libgit2 cannot work with documents — only the shared storage yields one. Opening a repository reads its remote (`GitManager.remoteUrl`) and its author from the repository itself; if it has one, the setup continues into the same credential screens the clone uses and `SetupViewModel` skips the cloning, since the files are already there. If it has none, the user is asked whether to add one — and `GitManager.setRemoteUrl` writes it into the repository, because push and pull read the remote from there and not from the preferences.

There is no provider integration anymore: the OAuth application it used to log into belonged to the upstream author, its client secret stood in the source of every apk, and it asked for full access to every private repository of whoever authorised it. What is left is what never needed an account — a clone url plus ssh keys or a token — so nothing in `ui/screen/setup/remote/` talks to a service, and the `gitnote-identity://` callback, the stored token and the provider preference are gone with it.

Preferences are DataStore-backed with a small typed wrapper (`PreferencesManager.booleanPreference/stringPreference/enumPreference`), exposed as Compose state via `.getAsState()`. Two of them are not only preferences: `remoteUrl` has to be written into the repository as well (`SettingsViewModel.updateRemoteUrl` → `GitManager.setRemoteUrl`), because push and pull read the remote from there and a url that only sat in the store left the settings screen naming one address while every sync used another; and `pureBlack` reaches `DiffusionTheme`, which blacks out `background`/`surface` on top of whichever scheme was chosen, but only while the dark theme is showing.

## Conventions

- The app is English only. `app/src/main/res/values/` and `metadata/en-US/` are the only string sources; localisation was removed rather than left half done, so do not add `values-*` directories.
- Git LFS is used for `app/src/main/jniLibs/**`, `*.png`, `*.zip` — clone with LFS enabled. The gradle wrapper jar deliberately is **not** in LFS, so `./gradlew` works without it.
- `app/schemas/` holds exported Room schemas (KSP `room.schemaLocation`); bump `version` in `RepoDatabase` when entities change.
- The markdown editing lives in `ui/viewmodel/edit/`, split by subject: `MarkdownList.kt` parses a list line, `MarkdownActions.kt` holds the seven button actions, `MarkdownSmartEditor.kt` what enter and delete do by themselves, `TextEdits.kt` the line arithmetic under all of it. `MarkdownEditingTest` pins their behaviour down case by case — change it there first, deliberately, or not at all.
- Unit tests run with `testOptions.unitTests.isReturnDefaultValues`, so `android.util.Log` returns instead of throwing.
- In list rows, compose nothing that is not visible: a `CustomDropDown` returns early while closed, because one closed menu per row means one `MaterialTheme` and one `DropdownMenu` per row. For the same reason a `StateFlow` that changes often (the search query) is collected as far down as it is used, not passed as a value from above.
- `SimpleIcon` (`ui/component/Icon.kt`) takes `contentDescription` without a default, so every call site has to decide. Pass `null` only for an icon that decorates a label right next to it; anything clickable gets a string.
- Known limitation to keep in mind (from README): Android is case-insensitive for filenames.
- Deleting a folder is recursive on both sides and has to stay that way: `NodeFs.Folder.delete()` removes the directory tree, and `Dao.deleteNoteFolder` deletes the notes under it, **the rows of its subfolders**, and then the folder itself. Without the middle one a subfolder stayed in the list as a row that opened onto nothing.
- The clone in `GenerateNewSshKeysScreen` waits until the copy button has been used. A key that was generated here and never taken away is one the remote has never seen, and the authentication error that follows says nothing about the step that was skipped. Regenerating takes the permission back with it.
- A pull that cannot be merged writes the conflict into the notes. `normal_merge` uses `repo.merge`, which writes the index **and** the working tree and leaves `MERGE_HEAD` behind, then returns `Error::MergeConflict { paths }`. Two things follow from that and must stay together: `commit_all` reads `MERGE_HEAD` (`commit_parents`) so the sync that ends the conflict makes a real merge commit — otherwise the merge is undone by the commit that finishes it and the same conflict returns on every pull — and it calls `cleanup_state()` afterwards. On the Kotlin side, HEAD has not moved for a conflict, so `syncWithRemoteWithoutLocker` rebuilds the database with `force` when the pull failed with `GitExceptionType.MergeConflict`, and skips the push after any failed pull.
- Nothing commits a conflict that has not been read. Before it adds anything, `commit_all` walks the paths the index still lists as conflicted and looks for `<<<<<<<` or `>>>>>>>` at the start of a line (never `=======`, which is how markdown underlines a heading); if one is still there it returns `Error::UnresolvedConflict` and the sync stops with the names of those notes. Without it, leaving the app was enough to write both versions into the history, since the sync runs by itself.
- The reader's table is laid out in `MarkDown.kt` (`WideTable`), not by the markdown library: the library gives every column the same width and ellipsises what does not fit on one line. Ours measures each column by its widest cell up to `MaxTableColumnWidth`, wraps beyond that, and scrolls sideways when the table is wider than the screen.
