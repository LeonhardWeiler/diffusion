<div align="center">

<h1>
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo_wide_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="assets/logo_wide_light.svg">
  <img alt="GitNote" width="50%" src="assets/logo_wide_light.svg">
</picture>
</h1>

</div>

[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/wiiznokes/gitnote.svg?logo=github&label=GitHub&cacheSeconds=3600)](https://github.com/wiiznokes/gitnote/releases/latest)
[![F-Droid](https://img.shields.io/f-droid/v/io.github.wiiznokes.gitnote?logo=f-droid&label=F-Droid&cacheSeconds=3600)](https://f-droid.org/packages/io.github.wiiznokes.gitnote)
[![IzzyOnDroid](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/io.github.wiiznokes.gitnote)](https://apt.izzysoft.de/fdroid/index/apk/io.github.wiiznokes.gitnote)

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
Nothing is committed or sent anywhere until you tap the cloud button in the
search bar: that commits what has changed, pulls and pushes in one step. It is
the only thing that talks to the remote — there is no pull to refresh.

Opening a repository that is already on the device picks up its remote and its
author, so it is set up with what it already knows and only asks for the
credentials it cannot read.

<p  style="text-align: center;">
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/edit.png" width="32%"  alt="edit screen"/>
</p>

_Supported Android versions: 11 to 16_

_Supported Architecture: `arm64-v8a`, `x86_64`_

# Build

[See](./BUILD.md).

# Current limitation

- A repository has to live on the shared internal storage. A memory card or a usb stick has no file path git can be pointed at.
- Android does not differentiate case for file name, so if you have a folder named `A` and another folder named `a`, `a` will not be displayed.
- Conflicts cannot be resolved in the app. When the same note was changed here and on the remote, the sync stops and reports it; your local version is kept untouched, and you need a git client to resolve it.

## Contributing

See [this file](./CONTRIBUTING.md).
