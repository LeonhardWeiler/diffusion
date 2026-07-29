# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Diffusion is an Android note app (Kotlin + Jetpack Compose) whose notes are plain files in a real Git repository. Git itself is not implemented in Kotlin: a Rust `cdylib` (`libgit_wrapper.so`, crate `git_wrapper` in `app/src/main/rust`) wraps `libgit2` (with vendored openssl/libssh2) and is called through JNI. libgit2 is built **without** the `https` feature — the app reaches a remote over ssh and nothing else — which takes `libssl` out of the link but not `libcrypto`, which libssh2 needs for its ciphers.

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
./gradlew connectedDebugAndroidTest                            # instrumented tests, needs a device (none left)
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
Three things have to stay true for `:app:generateBaselineProfile` to run: the
`:baselineprofile` module declares **the app's build types** (`nightly` among
them, empty — a variant it does not have is a dependency the app cannot
resolve); the generator asks the plugin which app it is profiling
(`androidx.benchmark.targetPackageName`) rather than naming it, since two of the
three ids carry a suffix; and the `nonMinified*`/`benchmark*` variants are
signed with the nightly key in `app/build.gradle.kts`, because they inherit the
release signing config and its keystore is not in the repository. The recorded
profiles land in `app/src/<variant>/generated/baselineProfiles/`.
`ui-tooling-preview` is `compileOnly` plus `debugImplementation`: the previews
sit next to what they preview, so the annotation has to compile everywhere, but
the release must not carry the package.

Build variants: `debug` (`.debug` suffix), `nightly` (release-shrunk, signed with the committed `nightly-signing-key.jks`, `.nightly` suffix), `release` (signing via `KEY_ALIAS`/`KEY_PASSWORD`/`STORE_PASSWORD` env vars + `app/key.jks`). All three share one launcher icon: the per-variant drawables distinguished nothing, and the label already says which build it is. The icon is `assets/app_icon.svg` and everything drawn from it — `drawable/ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml` carry the path as it stands there, and the `mipmap-*` webp files are that canvas cropped to the 72 dp a launcher shows of an adaptive icon, the round one clipped to a circle. Changing the svg means redrawing all of them.

`checkJniLibsAreRelease` runs before every non-debug packaging task and fails above 20 MB per `.so`: `jniLibs` is gitignored and filled by `make build_install`, so a release built after a debug `make` silently carried a 68 MB debug library instead of a 7 MB one.

`lint { abortOnError = true }` — a lint error stops the build, and there are currently no findings at all on debug. `AndroidGradlePluginVersion` is disabled because it fails for a reason outside the code.

CI (`.github/workflows/ci.yml`) runs exactly: Rust `make build_install`, then `./gradlew assembleDebug lintDebug testDebugUnitTest`, plus `cargo test` in a separate job.

## Architecture

### Layering

```
Compose UI (ui/screen/**)  ->  ViewModels (ui/viewmodel/**)
        -> StorageManager (single write path: filesystem + index + git)
             -> FileSystem (data/platform) -> real files in the repo
             -> NoteIndex (data/index)     -> the repository in memory
             -> GitManager                 -> JNI -> Rust -> libgit2
```

`MyApp.appModule` is a hand-rolled service locator (`AppModule`/`AppModuleImpl`, all `by lazy`). There is no DI framework — new singletons go there, and managers reach each other via `MyApp.appModule.*`.

Writes to storage or git run in `AppModule.appScope`, not in `viewModelScope`: the edit view model is cleared right after saving, which would cancel the write.

### The note list is read from the files, and from nothing else

The files on disk are the only thing that says what exists. `NoteIndex`
(`data/index/NoteIndex.kt`) holds the repository in memory — a path, a name, a
date and an id per file, and every folder — and `rebuild(rootPath)` walks the
working tree to fill it. It is built when a repository is opened and read again
whenever something other than this app may have written to it: `MainViewModel.tryInit`
at start, the pull inside a sync, and the reload in the search bar's menu
(`GridViewModel.reloadIndex`). Nothing survives a run, so nothing can be stale
in a way that has to be detected.

There was a Room database here, with an FTS4 table and a ranking function over
its match info. What it bought was a list that was there before the files had
been read; what it cost was everything that kept it honest — the commit the
rows were built from (`databaseCommit`), the HEAD comparison in front of every
write, a destructive migration, exported schemas, and SQL functions that forced
the database open through requery. Reading the repository is a few hundred
milliseconds on a real repository on a real phone, behind a screen that is
coming up anyway, so it happens at every start now. If that ever stops being
true, `NoteIndex.rebuild` logs how many files in how many folders and how long
it took: `adb logcat -s NoteIndex`.

**No text is held.** The index has no content column and no equivalent: the
editor reads the file it opens (`NoteIndex.loadNote`, off the main thread), and
the search reads the files it looks into, one at a time, cancelled the moment
another letter is typed (`mapLatest`). A file above `LIMIT_FILE_SIZE` (2 MB) is
listed, is not looked into and does not open here. `Note` carries the text and
is what a write is made of; `NoteHeader` is what a row is, and the two are
separate types on purpose.

`IndexState` is a snapshot and is never written into: every change swaps it for
a new one, so whoever is drawing a list is drawing one that agrees with itself.
`Note` derives `parentPath` and `fileName` from `relativePath` in its
constructor defaults, so anything that builds one by hand has to leave them
agreeing (`ModelTest`).

Sorting is alphabetical, notes and folders alike, and there is nothing to
choose: `notesIn`, `foldersIn` and `search` are functions over an `IndexState`
and are what `NoteIndexTest` pins down.

`StorageManager` holds a `Mutex` around every mutation and is the only thing
that calls into the index; `GitManager` holds another around every libgit2 call.
Mutating notes outside `StorageManager` leaves the list describing something
that is no longer there until the next rebuild.

`StorageManager.update` writes **the files first and the index second**. Both
happen one after the other and the process can die between them, so the only
question is which may be behind: a row left over for a file that is gone is
noticed by `openNote` ("this note is no longer there"), and a file with no row
is a note that is not in the list — and the next start reads the files again
either way.

Writing and syncing are separate: `StorageManager.update` writes the files and the index rows and stops there, so edits live in the working tree uncommitted, and the sync button carries a dot (`hasLocalChanges`) for as long as that is true. `syncWithRemote()` is the only thing that commits (`commitAll`), pulls and pushes. It runs when the user taps the cloud button, and on its own when the app opens (`MainViewModel.tryInit`) and when it is left (`MainActivity.onStop`), both with `announceErrors = false` so a missing network shows on the icon instead of opening a tooltip over the list, and both only while `AppPreferences.syncOnOpenAndClose` is on. Four things guard that automatic sync: it returns at once when `gitManager.isRepoInitialized` is false, because the app is stopped several times during the setup and the failure used to stay on the button of the repository that was then cloned; it `join()`s `StorageManager.lastWrite` first, so the editor's `ON_STOP` write is in the commit rather than one sync late — which is why every note write goes through `StorageManager.startWrite`; `hasLocalChanges` is set by any write but re-asked from git (`refreshChangeState`) when a note was written back to what it was, and again when the repository is opened at start, because being force closed does not commit anything and a fresh process starts the flag at false; and it waits for the network (`NetworkMonitor.awaitOnline`, `ACCESS_NETWORK_STATE`, up to 8 s, `VALIDATED` rather than merely `INTERNET`) before it touches the remote — the app is opened and left exactly when a phone comes back from sleep, and libgit2 failed at dns for a sync nobody asked for. Without a network, a sync that was asked for says so and one that ran by itself says nothing. That wait is not enough on its own — the system reports a validated network a moment before this process can resolve a name — so a pull that comes back `NetworkUnreachable` is tried **once more** after `RETRY_AFTER_NETWORK_FAILURE_MS`, and if that fails too, `reportSyncFailure` puts the button back to `Idle` for an unrequested sync instead of leaving an error on it (`GitManager` logs it without a stack trace as well). Ssh-class failures are deliberately *not* treated that way: they share a class with authentication failures. `refreshLocalChanges()` runs **before** `SyncState.Ok` is emitted, or the dot stands under a button that has already said the notes went out, for as long as walking the working tree takes. The one state the tap sets itself is `SyncState.Starting` (`announceSyncStart()`, not suspending, called before the coroutine): everything up to the first pull is otherwise a button that looks like it was not pressed. `GridViewModel.reloadIndex` calls `rebuildIndex()` instead: files into the list, no network. There is no pull to refresh — a gesture on the list looked like syncing while doing something else. Only a pull makes the sync read the repository again (`pulledFiles`, a merge conflict among it, because that is written into the notes without HEAD moving); a commit does not touch the working tree, and every write of this app's own is put into the index as it is written.

The date a note shows is the file's mtime and only that. Git is asked once per checkout instead of once per rebuild: `apply_commit_timestamps` (rust) stamps a file that agrees with HEAD with the time of the commit that last wrote it, and runs at the end of `clone_repo` and `pull` — the two things that write files the user did not write. A modified file is skipped, because its own mtime is when the user typed and no commit can speak for that. `SetupViewModel.openRepo` calls it once for a repository the app has not seen before; nothing calls it per start, and committing does not move a date because a commit does not touch the working tree.

Two things narrow that further, and both are about a date the user would recognise. A pull dates **only what it wrote** (`date_pulled_notes` diffs the commit HEAD stood at before the merge against the one it stands at after, and hands the paths to `apply_commit_timestamps_to`): the sync commits before it pulls, so without that every note just committed would agree with HEAD as well and be moved to the minute of the sync. And a note undone back to what it was keeps its old date — `TextVM` holds the note it was opened with (`openedNote`), a save that ends at exactly that carries its `lastModifiedTimeMillis`, and `StorageManager` stamps the file with the date its row holds (`dateBy`) so the two agree before and after a rebuild.

The reading mode parses the note with `rememberMarkdownState`, which re-parses whenever its input changes and shows nothing while it does. Two rules keep that from being felt: the state lives outside the read-only branch in `MarkDownContent`, so it survives switching to writing and back, and its input only follows the note when the reading mode is entered — an unchanged note is never parsed twice. A ticked checkbox is therefore **not** allowed to reach it: the tick is remembered per offset and written to the note separately (`[ ]` and `[x]` are the same length, so parser offsets stay valid).

The writing mode scrolls the column around the field, not the field itself (a `TextField` that scrolls itself is re-measured when the keyboard takes half the screen and comes back at the first line). **Where the caret is, is worked out, never received.** Three rounds went into deciding which of the `bringIntoView` requests a field sends out to believe — the one that arrives with the focus is about the caret as the note was opened, because the tap that moves it is applied a frame or two later, and every attempt to tell them apart left a case where the note still jumped to the top. So `Modifier.caretIntoView` swallows them all (a `BringIntoViewModifierNode` that does nothing, which is also what stops the column above from answering), and `caretRectOf(layout, selection, textTop)` says where the caret stands: the field's `TextLayoutResult` plus the selection the view model holds. That is why the field is a `BasicTextField` — the material one keeps its text layout to itself — padded with `TextFieldDefaults.contentPaddingWithoutLabel()`, which is what the reader pads its list with so that reading and writing start at the same height. Two effects scroll, both instantly and never animated: one on the selection, which brings a caret that has left the visible band back with a line of room on either side (that is also what carries the view along while typing), and one on `ScrollState.viewportSize`, which puts a caret the keyboard has just covered in the middle of what is left. A caret that can already be seen is not moved at all, and taking focus moves nothing: `CaretScroller.focusGained` writes the offset down as handled, so the first thing ever acted on is the tap itself.

What a note is called is a **path**, not a name: `resolveRepoPath` (`helper/RepoPath.kt`, pinned by `RepoPathTest`) reads `../notes.md` as a folder up, `archive/notes.md` as one beside it, `/notes.md` as the root, and the extension is part of it — a last segment with no dot keeps the one the note already has (`keepExtension`, same file, used by the editor and by the rename alike). Two rules make that safe: the typed path is read against the folder the thing is in *now*, and the target folder has to exist already, the way `mv` wants it. A path that cannot be given says which of the four things is wrong with it — `PathProblem.{Empty, NamesFolder, InvalidCharacter, AboveRoot}`, put into words by `describe(uiHelper)` in the same file — and the character a name cannot hold is quoted back, because "Name is invalid" pointed at nothing while somebody looked at a colon they could not see.

**Renaming happens from the row, not from the open note.** The name above an open note is read-only for a note that exists — it was a field whose every keystroke went into the next save, so a rename happened halfway through a name nobody had finished typing — and typeable only while the note is being created (`isNewNote` in `EditScreen`, remembered once, because `vm.editType` turns into `Update` at the first save). `GridViewModel.renameNote` resolves what was typed and `StorageManager.renameNote` moves the file (`NodeFs.File.moveTo`, so the bytes are never read, the date stays, and a file too large to be indexed moves like any other) and rewrites the row with the id it had. `StorageManager.renameNoteFolder` does the same for a folder — one `Files.move` of the directory, then the rows under it rewritten by `NoteIndex.moveFolder` rather than read from disk, so a rename does not walk the whole repository. The reading/writing toggle is only shown for markdown (`hasReadingMode`), because reading mode *is* markdown being rendered and a text file was left in a field that refused to be typed in.

The editor has no save button: `TextVM.scheduleSave()` writes the note after the last keystroke, and `saveNow()` runs when the editor is left, on `ON_STOP` and in `onCleared`. The pause is `SAVE_DEBOUNCE_MS` up to `CHEAP_NOTE_CHARS` and grows with the note beyond that, to `MAX_SAVE_DEBOUNCE_MS`: a save writes the whole file, and a note with a book pasted into it was paying that twice a second. Nothing is risked, because the three unconditional saves above still write straight away. Text that has no usable file name yet — the one thing a save cannot take — is `TextVM.draft()`, put into the edit screen's own `rememberSaveable` at `ON_STOP` and handed back to the view model factory when the process comes up again. (`NoteSaver`, a serialised file in the private directory, did that before.) Because saving runs constantly, `StorageManager.updateNote` only deletes the old file when the note was renamed; otherwise the row is written over in place and `write` truncates the file.

### JNI boundary

Kotlin side: `manager/GitManager.kt` and `manager/MimeTypeManager.kt` declare top-level `external fun`s. Because they are top-level, the JVM looks for them on the synthetic classes `GitManagerKt` / `MimeTypeManagerKt` — the Rust `native_method!` declarations in `rust/src/lib.rs` name exactly those `java_type`s. Renaming a Kotlin file or moving an `external fun` breaks linkage at runtime, not at compile time.

Return convention: most calls return `jint`, `0` = OK and negative = the libgit2 raw error code, which `GitManager` turns into a `GitException` with a localized string. Three things ride along with that: `MERGE_CONFLICT` (`-1000`), `UNRESOLVED_CONFLICT` (`-1001`) and `NETWORK_UNREACHABLE` (`-1002`), all defined in `rust/src/error.rs` and in `GitManager` — keep the trios equal — are not libgit2 codes but a pull that could not be merged, a commit that would have carried conflict markers, and a remote that was never reached (libgit2 says that only in the error *class*, which never crossed the boundary otherwise), and `lastErrorMessageLib()` hands over the message behind the last code so the user is not shown a bare number (reading it clears it). Callbacks go the other way too: `GitManager.progressCb` is annotated `@Keep` and invoked from Rust during clone (keep `@Keep` — release builds are minified).

Rust side keeps the open repository in a process-global `static REPO: LazyLock<Mutex<Option<Repository>>>` (`libgit2/mod.rs`), so "open repo / close repo" is global state, not a handle passed around. `init_lib` also applies an SSH workaround by setting `HOME`/`GIT_OPT_SET_HOMEDIR` to the app's `filesDir`. Everything about reaching the far end lives in `libgit2/transport.rs`: TLS certificates are validated by libgit2 (`CertificatePassthrough`); ssh host keys are pinned on first use in `.ssh/pinned_hosts`, because libgit2 would only consult `known_hosts`, which the app never fills. **Who we log in as is read off the remote url**, not stored: `credential_helper` takes the `username_from_url` libgit2 hands its credentials callback and only falls back to `git` when the url carries no name at all. `Cred` therefore has no username field on either side of the boundary — it held `"git"` for everyone, and `ssh://tom@host/notes.git` failed at authentication for it.

What a commit is called is decided on the rust side, because that is the side that stages the files: `commit_message` diffs the index it just wrote against HEAD and names the notes — `[fresh.md] added, [kept.md] changed, [gone.md] deleted`, with the first `MAX_NAMES_IN_SUBJECT` of a group in the subject and the rest counted there and listed underneath. Kotlin passes only the name to fall back on when there is nothing to name; a merge that changed no file is called "Merge".

`lib.rs` holds the `native_method!` table and the entry points it names, and nothing else — the error type and `LAST_ERROR` are in `error.rs`, the credentials arriving from Kotlin in `cred.rs`, the clone progress callback in `callback.rs`.

### Supported file extensions

The list of extensions treated as notes lives in `app/src/main/rust/supported_extensions/{text,markdown}.txt`, is embedded at compile time via `include_lines!`, and is looked up with `binary_search` — so the files **must stay sorted**. `build.rs` sorts them on every Rust build, and `just sort-supported-extension` does it manually. Kotlin asks Rust once per extension and remembers the answer (`extensionType` / `isExtensionSupported` in `MimeTypeManager`, backed by a `ConcurrentHashMap`): every row of the list asks whether its file is a note, and without the memo that was a JNI transition per row per frame. Adding an extension is a Rust-side change that requires rebuilding the `.so`.

**The extension decides what a file is, not whether it is listed.** `NoteIndex.rebuild` gives every file in the repository a row; the extension only decides whether the search looks inside it. Everything else is a row with a name, a date and nothing to search in — and tapping it opens the Android chooser (`helper/OpenExternally.kt` → `FileProvider`, authority `${applicationId}.fileprovider`, paths in `res/xml/file_paths.xml`). Hidden files and symlinks are skipped, which is what keeps the `.gitkeep` of every empty folder out of the list. A file above `LIMIT_FILE_SIZE` (2 MB) is listed but refused by `GridViewModel.openNote` and not read by the search: pulling two megabytes into memory for a substring is not what a note list is for.

### Navigation & UI

Navigation is this repository's own (`ui/navigation/NavHost.kt`, ~160 lines): a `Backstack` of `@Parcelize` destinations in `rememberSaveable`, an `AnimatedContent` over its top, and a `BackHandler`. Each entry gets **its own `ViewModelStore`** — without that one store would serve every screen and the second note opened would be handed the first note's `TextVM`, because the factory is only asked when there is none — and its own `SaveableStateProvider`, which is what keeps the note list where it was scrolled. A store is cleared once its destination is neither on the backstack nor still on screen, not when the backstack changes: both screens are on the way through the animation then, and clearing the outgoing one would run `TextVM.onCleared` (which writes the note) while it is still drawn. Two things are easy to break here: the `BackHandler` must be **off** when there is nothing to pop and no `onBack`, or back stops closing the app; and of two back handlers the one composed *last* wins, which is why the one refusing to abandon a running clone sits below the `NavHost` in `RemoteNav`. Destinations live in `ui/destination/`: top-level `Destination.Setup` vs `Destination.App`, then `AppDestination.{Grid, Edit, Settings}` — `Grid` is the note list, the staggered grid it was named after is gone. Folders are rows in that list, and `GridViewModel.currentNoteFolderRelativePath` is what `notesIn` filters on, so the list is never recursive; only the search is. `GridViewModel.gridItems` is one `StateFlow<List<GridItem>>` built from three things — the index, the folder being looked at and the query — with the `..` row and the folder rows at the front and the notes after them. It was a paged query with a second flow of folder rows folded into it, and that was a crash waiting for the second tap of a multiple selection: a `PagingData` may be collected exactly once, so anything combined in before `cachedIn` handed it a stream that had already been read. Whether a row is selected is asked of `selectedNotes`/`selectedFolders` in the row itself, never folded into `GridNote`. The selection holds folders as well as notes (`selectionSize` is the two together), `selectAll()` marks what the list is currently showing, and deleting it takes each folder with everything inside it — notes that stand in one of those folders are dropped from the list first, so nothing complains about a file that is already gone.

A search shows no folder rows at all (it spans subfolders, so they are not what was asked for) and names its results by their full path.

The folder to open on start and the full-path display are not preferences anymore, and neither is the sort order: it is alphabetical. The undo history is not in the edit view model either (it is cleared as soon as a note is left): `AppModule.editHistoryStore` keeps one `EditHistory` per note id, the last `MAX_REMEMBERED_NOTES` of them in access order (each holds up to `MAX_UNDO_STEPS` whole copies of a note, and no more than `MAX_UNDO_CHARS` of text in total, so a very long note is undone less far rather than costing a garbage collection per keystroke). A note keeps its id across a save, so the history stays where it is. `EditHistory` also decides what counts as one undo step: the editor reports every keystroke, and consecutive states are folded together until something ends the edit (`EditHistoryTest` says where those lines fall). `MainActivity` picks the start destination by `runBlocking { vm.tryInit() }` (whether a repo is already configured) and, when that fails, by `vm.repoAwaitingPermission()`: a repository that is set up and cannot be read is `Destination.MissingPermission`, not the setup. That call is held in **`remember`, never `rememberSaveable`**, and `tryInit` is what it opens the repository with: libgit2's open repository and the note index are both process state, while the backstack is written into the bundle and comes back pointing at the app — saved, the start after a killed process was an empty note list whose sync button could do nothing, because nothing had opened the repository again. Two things pay for that: `tryInit` returns at once while `gitManager.isRepoInitialized`, so a rotation does not walk the working tree and sync a second time, and a `LaunchedEffect` replaces a restored backstack that stands on `Destination.App` when this process could not open anything — one direction only, because a setup that has already opened its repository is mid-flight and stays where it is. Everything about it is stored — path, remote, ssh key, every setting — and what a new build installed over the old one takes away is the permission to read all files, so `StoragePermissionScreen` asks for that and calls `tryInit()` again rather than sending the user to pick the same folder a second time.

Opening a repository takes a second or two of libgit2 plus the whole working tree being read, and it happens after the folder picker has closed — so `SetupViewModel.openRepo` emits `InitState.OpeningRepo` and then the folder-by-folder progress the clone already reported, and `NewRepoMethodScreen` shows that instead of the two buttons. Every way out of that function has to put the state back, or the screen keeps spinning.

The setup offers opening an existing repository or cloning one; there is no create, no account to log into and no choice of storage — a repository always lives in a folder on the shared storage, so `StorageConfiguration` is just a path. The folder is chosen with the system picker (`ACTION_OPEN_DOCUMENT_TREE`), and `pickedFolderPath` turns the tree uri back into a real path because libgit2 cannot work with documents — only the shared storage yields one. Opening a repository reads its remote (`GitManager.remoteUrl`) and its author from the repository itself; if it has one, the setup continues into the same credential screens the clone uses and `SetupViewModel` skips the cloning, since the files are already there. If it has none, the user is asked whether to add one — and `GitManager.setRemoteUrl` writes it into the repository, because push and pull read the remote from there and not from the preferences.

**A repository that is already on the device ends the setup in a sync, not in a clone that never happens** (`SetupDestination.Remote.alreadyOnDevice` → the key screens say "Try Syncing…", `SetupViewModel.syncOnce` → `InitState.SyncingRepo`). Nothing had reached the remote otherwise, so a deploy key that was never added first showed up as a failed sync on a screen that says nothing about setting one up. It commits, pulls and pushes once — the push is the part that says whether the key may *write* — and only all three going through finishes the setup; `cancelClone` returns true for such a repository so the way back to the key screen exists after a refusal. The step that asks for the deploy key carries a button to the repository's page, from `repoWebUrl` (`helper/RepoWebUrl.kt`, pinned by `RepoWebUrlTest`), which reads both the scp form and a real ssh url.

There is no provider integration anymore: the OAuth application it used to log into belonged to the upstream author, its client secret stood in the source of every apk, and it asked for full access to every private repository of whoever authorised it. What is left is a clone url plus an ssh key, so nothing in `ui/screen/setup/remote/` talks to a service, and the `gitnote-identity://` callback, the stored token and the provider preference are gone with it.

**ssh only.** There is no `Cred.UserPassPlainText` on either side of the JNI boundary and no credentials screen: https is the transport that wants a password or a token in the app's own storage, in the clear, to be replayed on every sync. `parse_url` still recognises an https url so that `EnterUrlScreen` can say in words why it will not take it, and an opened repository whose remote is https is routed through that screen with the old address in the field — the ssh one that replaces it is written into the repository. The ssh screen offers three ways to a key: generate one, load one from the device, or reuse the pair already in the store (`SetupViewModel.storedSshKey`), which is what stops every setup costing the repository another deploy key. A generated key gets three steps — copy it, add it as a deploy key, clone — and a stored one gets two, because it has been through the first of those already: add it to the provider if it is not there, then sync. Nothing offers to regenerate a stored key; that is what the screen before it is for.

Preferences are DataStore-backed with a small typed wrapper (`PreferencesManager.booleanPreference/stringPreference/enumPreference`), exposed as Compose state via `.getAsState()`. Two of them are not only preferences: `remoteUrl` has to be written into the repository as well (`SettingsViewModel.updateRemoteUrl` → `GitManager.setRemoteUrl`), because push and pull read the remote from there and a url that only sat in the store left the settings screen naming one address while every sync used another; and the theme is a choice between light and dark and nothing else — `ui/theme/Color.kt` is black on white and white on black, greys where something has to be told apart from the page under it, and red only for errors. There is no dynamic colour and no pure-black setting: dark *is* black, which is what that setting was offering. The `surfaceContainer*` family is written out on purpose, because Material draws menus and sheets on it and its defaults are a tinted grey.

## Conventions

- The app is English only. `app/src/main/res/values/` is the only string source; localisation was removed rather than left half done, so do not add `values-*` directories.
- Git LFS is used for `app/src/main/jniLibs/**`, `*.png`, `*.zip` — clone with LFS enabled. The gradle wrapper jar deliberately is **not** in LFS, so `./gradlew` works without it.
- The markdown editing lives in `ui/viewmodel/edit/`, split by subject: `MarkdownList.kt` parses a list line, `MarkdownActions.kt` holds the seven button actions, `MarkdownSmartEditor.kt` what enter and delete do by themselves, `TextEdits.kt` the line arithmetic under all of it. `MarkdownEditingTest` pins their behaviour down case by case — change it there first, deliberately, or not at all.
- Unit tests run with `testOptions.unitTests.isReturnDefaultValues`, so `android.util.Log` returns instead of throwing.
- The way to test something that sits inside a class holding state is to lift the *decision* out of it: `scrollTargetFor`/`centreTargetFor` (out of `CaretScroller`, which needs a `ScrollState` only layout can fill), `saveDelayMillis` (out of `TextVM`), `movedUnder` and `resolveRepoPath` (out of `StorageManager`). `StorageManager` itself is still untestable because it reaches into `MyApp.appModule` for six things.
- In list rows, compose nothing that is not visible: a `CustomDropDown` returns early while closed, because one closed menu per row means one `MaterialTheme` and one `DropdownMenu` per row. For the same reason a `StateFlow` that changes often (the search query) is collected as far down as it is used, not passed as a value from above.
- `SimpleIcon` (`ui/component/Icon.kt`) takes `contentDescription` without a default, so every call site has to decide. Pass `null` only for an icon that decorates a label right next to it; anything clickable gets a string.
- Known limitation to keep in mind (from README): Android is case-insensitive for filenames.
- Deleting anything asks first (`RequestConfirmationDialog`), from all three places: a note's menu, a folder's menu and the selection bar. The selection dialog warns about folders taking everything under them only when one is actually marked (`confirm_delete_selection` vs `confirm_delete_selection_with_folders`) — a warning that does not match the screen is one that stops being read. The dialog's state lives in the row rather than in its menu, which returns early while closed and would take the state with it.
- Deleting a folder is recursive on both sides and has to stay that way: `NodeFs.Folder.delete()` removes the directory tree, and `NoteIndex.removeFolders` drops the notes under it, **the rows of its subfolders**, and then the folder itself. Without the middle one a subfolder stayed in the list as a row that opened onto nothing.
- The clone in `GenerateNewSshKeysScreen` waits until the copy button has been used. A key that was generated here and never taken away is one the remote has never seen, and the authentication error that follows says nothing about the step that was skipped. Regenerating takes the permission back with it.
- A pull that cannot be merged writes the conflict into the notes. `normal_merge` uses `repo.merge`, which writes the index **and** the working tree and leaves `MERGE_HEAD` behind, then returns `Error::MergeConflict { paths }`. Two things follow from that and must stay together: `commit_all` reads `MERGE_HEAD` (`commit_parents`) so the sync that ends the conflict makes a real merge commit — otherwise the merge is undone by the commit that finishes it and the same conflict returns on every pull — and it calls `cleanup_state()` afterwards. On the Kotlin side, HEAD has not moved for a conflict, so `syncWithRemoteWithoutLocker` rebuilds the database with `force` when the pull failed with `GitExceptionType.MergeConflict`, and skips the push after any failed pull.
- Nothing commits a conflict that has not been read. Before it adds anything, `commit_all` walks the paths the index still lists as conflicted and looks for `<<<<<<<` or `>>>>>>>` at the start of a line (never `=======`, which is how markdown underlines a heading); if one is still there it returns `Error::UnresolvedConflict` and the sync stops with the names of those notes. Without it, leaving the app was enough to write both versions into the history, since the sync runs by itself.
- The reader's table is laid out in `MarkDown.kt` (`WideTable`), not by the markdown library: the library gives every column the same width and ellipsises what does not fit on one line. Ours measures each column by its widest cell up to `MaxTableColumnWidth`, wraps beyond that, and scrolls sideways when the table is wider than the screen.
