<div align="center">

<h1>
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo_wide_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="assets/logo_wide_light.svg">
  <img alt="GitNote" width="50%" src="assets/logo_wide_light.svg">
</picture>
</h1>

</div>

[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/LeonhardWeiler/gitnote.svg?logo=github&label=GitHub&cacheSeconds=3600)](https://github.com/LeonhardWeiler/gitnote/releases/latest)

Android note app which integrate Git. You can use this app with other desktop editors.

## Why

Because all apps which integrate git on Android either separate the note title from the name of the file or use old UI/UX frameworks

# Features

- [x] open or clone repositories
- [x] notes search across the whole repository
- [x] folder navigation
- [x] edit view
- [x] private repo (SSH and HTTPS)
- [x] remote sync
- [x] time based sort

Notes are written to the repository while you type, so there is no save button.
Nothing is committed until a sync: that commits what has changed, pulls and
pushes in one step. The cloud button in the search bar is the way to ask for one
at any time — there is no pull to refresh — and a dot on it says when there is
something the remote has not been told about yet. The app also syncs by itself
when it is opened and when it is left; that can be turned off under Settings →
Repository if you would rather it only happened when asked.

Cloning wants a clone url and the credentials for it: ssh keys, which the app can
generate for you to add as a deploy key, or a username and an access token. No
account is connected and nothing is authorised on your behalf.

Opening a repository that is already on the device picks up its remote and its
author, so it is set up with what it already knows and only asks for the
credentials it cannot read. A repository without a remote is asked about rather
than left as it is, so notes written into it can still be synced later.

<p  style="text-align: center;">
  <img src="https://media.githubusercontent.com/media/LeonhardWeiler/gitnote/master/assets/edit.png" width="32%"  alt="edit screen"/>
</p>

_Supported Android versions: 11 to 16_

_Supported Architecture: `arm64-v8a`, `x86_64`_

# Build

[See](./BUILD.md).

# Current limitation

- A repository has to live on the shared internal storage. A memory card or a usb stick has no file path git can be pointed at, and the app's own private directory is not offered because nothing else could reach the repository there.
- Android does not differentiate case for file name, so if you have a folder named `A` and another folder named `a`, `a` will not be displayed.
- Conflicts are resolved by hand, in the note. When the same note was changed here and on the remote, the sync stops and reports which notes it could not merge. Both versions are then in the note, between `<<<<<<<` and `>>>>>>>` markers: edit it down to what you want to keep and sync again, and that sync is what finishes the merge. Until then every sync refuses to commit, so the markers cannot end up in the history by themselves.

## Contributing

See [this file](./CONTRIBUTING.md).

## Credits

GitNote was created by [wiiznokes](https://github.com/wiiznokes), and this is a
fork of [wiiznokes/gitnote](https://github.com/wiiznokes/gitnote) that goes its
own way. Everything up to the fork is their work, and it stays under the same
licence — see [LICENSE](./LICENSE).
