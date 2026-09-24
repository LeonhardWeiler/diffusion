# CLAUDE.md

Diffusion is an Android note app (Kotlin, Jetpack Compose). Notes are plain files
in a real git repository, handled by JGit (`org.eclipse.jgit`) with jsch
(`com.github.mwiede:jsch`) and bouncycastle for ssh. No native code.

## Build & test

`nix develop` provides jdk 21, the Android SDK, gradle and just.

```bash
./gradlew assembleDebug lintDebug testDebugUnitTest            # the build
./gradlew testDebugUnitTest --tests '*PlatformTest*'           # one test class
./gradlew assembleNightly                                      # checks R8 keeps JGit/jsch working
./gradlew :app:generateBaselineProfile                         # needs a device with a repo set up
just fix                                                       # lintFix
```

- Variants: `debug` (`.debug`), `nightly` (shrunk, committed `nightly-signing-key.jks`,
  `.nightly`), `release` (env `KEY_ALIAS`/`KEY_PASSWORD`/`STORE_PASSWORD` + `app/key.jks`).
- `lint { abortOnError = true }`. Debug has 5 known warnings and no errors.
- `proguard-rules.pro` keeps all of `org.eclipse.jgit.**` and `com.jcraft.jsch.**`.
- Bouncycastle is required: Android has no ed25519 in its JCA. Unit tests on the jdk stay green without it.
- `:baselineprofile` must declare the app's build types (incl. `nightly`); the plugin is pinned to `1.5.0-alpha` for AGP 9.
- The launcher icon is `assets/app_icon.svg`; changing it means redrawing
  `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` and all `mipmap-*` webp files.
- Unit tests run with `isReturnDefaultValues`, so `android.util.Log` does not throw.

## Architecture

```
Compose UI (ui/screen/**)  ->  ViewModels (ui/viewmodel/**)
        -> RepoManager (which repositories there are, which one is shown)
             -> RepoSession  (one repository, one of each of the below)
                  -> StorageManager (single write path: filesystem + index + git)
                       -> FileSystem (data/platform) -> real files in the repo
                       -> NoteIndex (data/index)     -> the repository in memory
                       -> RepoSync                   -> commit, pull, push, sync state
                       -> GitManager                 -> manager/git -> JGit
```

- `MyApp.appModule` is a hand-rolled service locator. New singletons go there. No DI framework.
- Writes to storage or git run in `AppModule.appScope`, not `viewModelScope`.

### Repositories

- A repository is a `RepoConfig` in `RepoStore`, every field a DataStore preference keyed by its id.
  Its name is its folder (`repoNameOf`). Ssh keys live in `SshKeyStore`, shared between repositories.
- Every repository is opened; only the shown one reads its working tree (`showsItsNotes`).
- `RepoManager.active` is never null; `RepoSession.none` stands in.
- `Destination.App` carries the repository id, so switching gives a new `ViewModelStore`.
- Removing a repository never touches its folder. Its key is removed only when no remaining
  repository uses it and at least one repository remains (`RepoManager.pruneKey`).
- The setup works on `RepoManager.draft` and only `adopt`s it when it succeeds.

### Note index

- The files are the only source of truth. `NoteIndex.rebuild` reads the working tree at start,
  after a pull and from the settings reload row. No database, no text held in memory.
- Files above `LIMIT_FILE_SIZE` (2 MB) are listed but not searched or opened.
- `IndexState` is an immutable snapshot. `NoteHeader`/`NoteFolder` compare by value, not by id;
  the selection asks by id (`holds`/`without`, never `contains`/`minus`).
- Sorting: newest first, folders take their newest note's date, ties by path, folders before notes.
  The list is re-sorted only where nobody sees it (`GridViewModel.resort`, `sortDates`):
  folder change, search start/end, `ON_STOP` (not `ON_START`), full reads.
- Only `StorageManager` mutates the index, under its `Mutex`. It writes files first, index second.
  `GitManager` holds a second `Mutex` around every JGit call.
- A note's date is its file mtime. `applyCommitTimestamps` stamps files after clone and pull
  (only what the pull wrote) and writes the new times into the git index too.

### Sync

- Writing and syncing are separate. `RepoSync` is the only thing that commits, pulls and pushes.
- Automatic sync on open and `MainActivity.onStop` (`syncAllQuietly`): skips uninitialised repos,
  joins `StorageManager.lastWrite` first (every note write goes through `startWrite`),
  waits for a validated network, retries a `NetworkUnreachable` pull once, and stays quiet on failure.
- `refreshLocalChanges()` runs before `SyncState.Ok` is emitted.
- `push` throws for every `RemoteRefUpdate` that is not `OK` or `UP_TO_DATE`.
- An empty repository is valid: no assumed `main`, no assumed commit.
- A commit always has an author: `GitAuthor.orFallback` in `setIdent`.
- Merge uses `setCommit(false)`; a conflict throws `MergeConflictException` and is written into the notes.
  `commitAll` refuses while a conflicted path still has `<<<<<<<` or `>>>>>>>` at a line start (never check `=======`).

### Git layer (`manager/git/`)

- Every function takes the `Git` it works on; tests run against real temp repositories (`GitFixture`).
- `GitEnvironment.install(filesDir)` must run before the first JGit call.
- Ssh only. One `JSch` with the configured key. Host keys pinned on first use in
  `.ssh/pinned_hosts` as `host type sha256hex`; the type is required.
- The ssh user comes from the remote url, default `git`.
- Staging is two `add` calls; the second (`setUpdate(true)`) records deletions.
- `remoteUrl` is written into the repository as well (`GitManager.setRemoteUrl`).

### Files and notes

- Note extensions: `app/src/main/resources/supported_extensions/{text,markdown}.txt`
  (`just sort-supported-extension`). No extension is text. Other files open via `FileProvider`.
  Hidden files and symlinks are skipped.
- A note is a path (`resolveRepoPath`, `RepoPathTest`), resolved against its current folder;
  the target folder must exist. Errors are `PathProblem`.
- `+` creates the file first and leaves it in the list; the editor only opens existing notes.
- Rename moves the file (`NodeFs.File.moveTo`) and keeps the row id.
- Deleting always asks first. Deleting a folder is recursive in both `NodeFs` and `NoteIndex.removeFolders`.
- Android filenames are case-insensitive.

### Editor

- No save button: `TextVM.scheduleSave()` debounces, `saveNow()` on leave, `ON_STOP` and `onCleared`.
- Markdown editing is in `ui/viewmodel/edit/`; `MarkdownEditingTest` pins it. Change the test first.
- Reading mode keeps `rememberMarkdownState` outside the read-only branch. A ticked checkbox
  must not reach the parser.
- The column scrolls, not the field. `Modifier.caretIntoView` swallows all `bringIntoView`
  requests; `caretRectOf` computes the caret. The field is a `BasicTextField`.
- Reading/writing mode is remembered per note (`AppPreferences.opensInReadingMode`).
- Tables are laid out by `WideTable` in `MarkDown.kt`, not by the markdown library.

### Navigation & UI

- Own navigation (`ui/navigation/NavHost.kt`): one `ViewModelStore` and `SaveableStateProvider`
  per entry, cleared only once the destination is off the backstack and off screen.
- The `BackHandler` must be off when there is nothing to pop. The last composed back handler wins.
- `GridViewModel.gridItems` is built by `ListInput.gridItems()`. A search shows no folder rows.
- Transitions are written out without `SizeTransform`.
- The start destination is held in `remember`, never `rememberSaveable`. `tryInit` returns
  early when the repository is already open.
- `SetupViewModel.openRepo` must reset `InitState` on every exit path.
- A repository already on the device ends the setup in one sync (commit, pull, push).
- The clone in `GenerateNewSshKeysScreen` is enabled only after the key was copied.
- A missing storage permission goes to `Destination.MissingPermission`, not the setup.
- Theme: light or dark only, no dynamic colour. Write out the `surfaceContainer*` family.
- In list rows compose nothing invisible; collect fast `StateFlow`s where they are used.
- `SimpleIcon` has no default `contentDescription`; `null` only for decorative icons.

## Conventions

- English only. `res/values/` is the only string source; no `values-*` directories.
- Git LFS for `*.png` and `*.zip`; the gradle wrapper jar is not in LFS.
- To test logic inside a stateful class, lift the decision out into a function.
