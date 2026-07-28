# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

GitNote is an Android note app (Kotlin + Jetpack Compose) whose notes are plain files in a real Git repository. Git itself is not implemented in Kotlin: a Rust `cdylib` (`libgit_wrapper.so`, crate `git_wrapper` in `app/src/main/rust`) wraps `libgit2` (with vendored openssl/libssh2) and is called through JNI.

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
just fix                                                       # ./gradlew lintFix
just prettier                                                  # formats md/yml/json via npx prettier
```

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

The files on disk are authoritative. Room mirrors them so the note list and FTS search are fast. `AppPreferences.databaseCommit` stores the commit the DB was built from; `StorageManager.updateDatabaseWithoutLocker` compares it against `gitManager.lastCommit()` and, on mismatch, runs `dao.clearAndInit(...)` — a full rebuild from the filesystem plus git commit timestamps (`getTimestamps`). Schema migrations use `fallbackToDestructiveMigration`, and the destructive-migration callback clears `databaseCommit` so the next start rebuilds. Never treat Room rows as data that can survive independently of the files.

Room is opened through `RequerySQLiteOpenHelperFactory` (not the platform SQLite) so custom SQL functions `rank`, `parentPath`, `fullName` can be registered — the FTS ranking and the folder queries in `Dao.kt` depend on them. The **note** queries do not: `Note` stores `parentPath` and `fileName` as indexed columns, derived from `relativePath` by the constructor defaults, because a value computed per row cannot use an index. Anything that builds a `Note` by hand (rather than `Note.new` or `copy` without touching `relativePath`) has to keep them consistent.

The note list does not read `Note` at all: `gridNotes`/`gridNotesWithQuery` project onto `NoteHeader` (path, name, date, id) so the content of every listed note does not sit in memory to draw two lines of text. Tapping a row loads the note by primary key (`Dao.note`) before the editor opens — a header cannot be handed to the editor, which is the point of it being its own type.

`StorageManager` holds a `Mutex` around every mutation; `GitManager` holds another around every libgit2 call. Mutating notes outside `StorageManager` will desync the DB from disk.

Writing, committing and syncing are three separate things, and only the first happens by itself. `StorageManager.update` writes the files and the Room rows and stops there, so edits live in the working tree uncommitted. `syncWithRemote()` is the only thing that commits (`commitAll`), pulls and pushes, and it runs only when the user taps the cloud button in the search bar. Start up (`MainViewModel.tryInit`) and pull to refresh call `updateDatabase(...)` instead: files into the database, no network. Because HEAD does not move while notes are written, the `databaseCommit` check stays satisfied and the DB is kept in step incrementally; a rebuild reads the working tree, so uncommitted notes survive it (their timestamps then come from the file mtime rather than git).

The editor has no save button: `TextVM.scheduleSave()` writes the note `SAVE_DEBOUNCE_MS` after the last keystroke, and `saveNow()` runs when the editor is left, on `ON_STOP` and in `onCleared`. `NoteSaver` is still there but only as the net for text that has no usable file name yet. Because that runs constantly, `StorageManager.updateNote` only deletes the old row and the old file when the note was renamed; otherwise the upsert on `relativePath` rewrites the row in place and `write` truncates the file.

### JNI boundary

Kotlin side: `manager/GitManager.kt` and `manager/MimeTypeManager.kt` declare top-level `external fun`s. Because they are top-level, the JVM looks for them on the synthetic classes `GitManagerKt` / `MimeTypeManagerKt` — the Rust `native_method!` declarations in `rust/src/lib.rs` name exactly those `java_type`s. Renaming a Kotlin file or moving an `external fun` breaks linkage at runtime, not at compile time.

Return convention: most calls return `jint`, `0` = OK and negative = the libgit2 raw error code, which `GitManager` turns into a `GitException` with a localized string. Two things ride along with that: `MERGE_CONFLICT` (`-1000`, defined in both `rust/src/error.rs` and `GitManager` — keep them equal) is not a libgit2 code but a pull that could not be merged, and `lastErrorMessageLib()` hands over the message behind the last code so the user is not shown a bare number (reading it clears it). Callbacks go the other way too: `GitManager.progressCb` is annotated `@Keep` and invoked from Rust during clone (keep `@Keep` — release builds are minified).

Rust side keeps the open repository in a process-global `static REPO: LazyLock<Mutex<Option<Repository>>>` (`libgit2/mod.rs`), so "open repo / close repo" is global state, not a handle passed around. `init_lib` also applies an SSH workaround by setting `HOME`/`GIT_OPT_SET_HOMEDIR` to the app's `filesDir`. Everything about reaching the far end lives in `libgit2/transport.rs`: TLS certificates are validated by libgit2 (`CertificatePassthrough`); ssh host keys are pinned on first use in `.ssh/pinned_hosts`, because libgit2 would only consult `known_hosts`, which the app never fills.

`lib.rs` holds the `native_method!` table and the entry points it names, and nothing else — the error type and `LAST_ERROR` are in `error.rs`, the credentials arriving from Kotlin in `cred.rs`, the clone progress callback in `callback.rs`.

### Supported file extensions

The list of extensions treated as notes lives in `app/src/main/rust/supported_extensions/{text,markdown}.txt`, is embedded at compile time via `include_lines!`, and is looked up with `binary_search` — so the files **must stay sorted**. `build.rs` sorts them on every Rust build, and `just sort-supported-extension` does it manually. Kotlin asks Rust about extensions (`extensionType`, `isExtensionSupportedLib`), including inside `Dao.clearAndInit`, so adding an extension is a Rust-side change that requires rebuilding the `.so`.

### Navigation & UI

`navigation-reimagined` with `@Parcelize` sealed destinations (`ui/destination/`): top-level `Destination.Setup` vs `Destination.App`, then `AppDestination.{Grid, Edit, Settings}` — `Grid` is the note list, the staggered grid it was named after is gone. Folders are rows in that list, and `GridViewModel.currentNoteFolderRelativePath` is what `Dao.gridNotes` filters on with `parentPath(...)`, so the list is never recursive; only the search is. The folder rows and the `..` row are not a second list next to the paged notes: `GridViewModel.gridItems` folds them into the same `PagingData` as `GridItem.Folder`/`GridItem.ParentFolder` headers, because two sources answering at their own pace made opening a folder rebuild the layout twice.

Sorting, the folder to open on start and the full-path display are not preferences anymore — the constants live in `GridViewModel`. The undo history is not in the edit view model either (it is cleared as soon as a note is left): `AppModule.editHistoryStore` keeps one `EditHistory` per note id for as long as the process lives, capped at `MAX_UNDO_STEPS`. A note keeps its id across a save, so the history stays where it is. `EditHistory` also decides what counts as one undo step: the editor reports every keystroke, and consecutive states are folded together until something ends the edit (`EditHistoryTest` says where those lines fall). `MainActivity` picks the start destination by `runBlocking { vm.tryInit() }` (whether a repo is already configured). OAuth for the GitHub provider (`provider/GitHub.kt`) comes back through `onNewIntent` on the `gitnote-identity://register-callback` URI and is forwarded via `MainActivity.authFlow`.

Preferences are DataStore-backed with a small typed wrapper (`PreferencesManager.booleanPreference/stringPreference/enumPreference`), exposed as Compose state via `.getAsState()`.

## Conventions

- The app is English only. `app/src/main/res/values/` and `metadata/en-US/` are the only string sources; localisation was removed rather than left half done, so do not add `values-*` directories.
- Git LFS is used for `app/src/main/jniLibs/**`, `*.png`, `*.zip` — clone with LFS enabled. The gradle wrapper jar deliberately is **not** in LFS, so `./gradlew` works without it.
- `app/schemas/` holds exported Room schemas (KSP `room.schemaLocation`); bump `version` in `RepoDatabase` when entities change.
- The markdown editing lives in `ui/viewmodel/edit/`, split by subject: `MarkdownList.kt` parses a list line, `MarkdownActions.kt` holds the seven button actions, `MarkdownSmartEditor.kt` what enter and delete do by themselves, `TextEdits.kt` the line arithmetic under all of it. `MarkdownEditingTest` pins their behaviour down case by case — change it there first, deliberately, or not at all.
- Unit tests run with `testOptions.unitTests.isReturnDefaultValues`, so `android.util.Log` returns instead of throwing.
- `SimpleIcon` (`ui/component/Icon.kt`) takes `contentDescription` without a default, so every call site has to decide. Pass `null` only for an icon that decorates a label right next to it; anything clickable gets a string.
- Known limitations to keep in mind (from README): Android is case-insensitive for filenames, and merge conflicts cannot be resolved in the app — the sync stops, reports them and leaves the working tree alone.
