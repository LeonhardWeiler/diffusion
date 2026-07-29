<div align="center">

<h1>
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo_wide_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="assets/logo_wide_light.svg">
  <img alt="Diffusion" width="50%" src="assets/logo_wide_light.svg">
</picture>
</h1>

</div>

[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/LeonhardWeiler/diffusion.svg?logo=github&label=GitHub&cacheSeconds=3600)](https://github.com/LeonhardWeiler/diffusion/releases/latest)

Android note app which integrate Git. You can use this app with other desktop editors.

## Why

Because all apps which integrate git on Android either separate the note title from the name of the file or use old UI/UX frameworks

# Features

- [x] open or clone repositories
- [x] every file in the repository is listed, not only the notes
- [x] notes search across the whole repository
- [x] folder navigation
- [x] rename and move, for notes and folders
- [x] edit view
- [x] private repo, over SSH
- [x] remote sync
- [x] time based sort
- [x] pure black theme for OLED screens

Notes are written to the repository while you type, so there is no save button.
Nothing is committed until a sync: that commits what has changed, pulls and
pushes in one step, and the commit is named after the notes it carries —
`[fresh.md] added, [kept.md] changed, [gone.md] deleted`. The cloud button in
the search bar is the way to ask for one at any time — there is no pull to
refresh — and a dot on it says when there is something the remote has not been
told about yet. The app also syncs by itself when it is opened and when it is
left; that can be turned off under Settings → Repository if you would rather it
only happened when asked. A sync waits briefly for the network before it gives
up, so coming back to a phone that has not reconnected yet is not an error.

The list shows every file in the repository, not only the ones the app can read.
Tapping a note opens the editor; tapping a photo, a pdf or anything else hands it
to whichever app on the device knows what to do with it, and every row has "Open
with another app" in its menu. The name above an open note is a path, so renaming
and moving are one act: `notes.md` renames it, `../notes.md` moves it a folder
up, `archive/notes.md` into one beside it. Writing an extension changes the type.
Folders have the same thing in their menu, and everything under them comes along.

Long pressing a row selects it, and from there tapping selects more. Folders can
be selected too, and deleting a selected folder takes everything in it. Deleting
anything asks first.

Cloning wants an SSH clone url and a key for it. The app can generate one for you
to add as a deploy key, take one off the device, or offer the one it already has
— with its fingerprint, so you can tell which. HTTPS is not offered: it is the
transport that wants a password or a token kept in the app to be replayed on
every sync. No account is connected and nothing is authorised on your behalf.

Opening a repository that is already on the device picks up its remote and its
author, so it is set up with what it already knows and only asks for the
credentials it cannot read. A repository without a remote is asked about rather
than left as it is, so notes written into it can still be synced later.

_Supported Android versions: 11 to 16_

_Supported Architecture: `arm64-v8a`, `x86_64`_

# Build

[See](./BUILD.md).

# Current limitation

- A repository has to live on the shared internal storage. A memory card or a usb stick has no file path git can be pointed at, and the app's own private directory is not offered because nothing else could reach the repository there.
- Android does not differentiate case for file name, so if you have a folder named `A` and another folder named `a`, `a` will not be displayed.
- A note above 2 MB is listed but not opened here — the editor is handed what the index holds, and files that large are not read into it. Use another app for those.
- Conflicts are resolved by hand, in the note. When the same note was changed here and on the remote, the sync stops and reports which notes it could not merge. Both versions are then in the note, between `<<<<<<<` and `>>>>>>>` markers: edit it down to what you want to keep and sync again, and that sync is what finishes the merge. Until then every sync refuses to commit, so the markers cannot end up in the history by themselves.

## Credits

Diffusion is a fork of GitNote, created by
[wiiznokes](https://github.com/wiiznokes) — see
[wiiznokes/gitnote](https://github.com/wiiznokes/gitnote). Everything up to the
fork is their work, and it stays under the same licence — see
[LICENSE](./LICENSE).
