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

Diffusion is an Android app that primarily thought for note taking and syncing them between different devices over a git repository.

## Why

Because all apps which integrate git on Android either separate the note title from the name of the file or use old UI/UX frameworks
Other similar apps like [gittasks](https://github.com/christianjann/gittasks) or [gitnote](https://github.com/wiiznokes/gitnote) (which this project is a fork of) exist, but they did not really fulfill my needs and especially the simplicity and performance I need. Instead of contributing to one of the projects I forked it, because I wanted my app to be done quick.

## Goals & Non-goals

The main goal for this project is syncing notes over a git repository via ssh keys, having a file browser and a basic markdown editor. The editor is by far the biggest part of the project, that may get kicked out at some point. The goal isn't to support as many filetypes as possible, but instead the functionality that is needed from a note syncing app. The reason why the editor might get pushed out, but the file browser won't is because I believe files and there git status should be at the same place.

## Functionality

To add a repository to Diffusion you can either clone or open an existing one. You have to copy the ssh key as a deploy key to your repo hoster of your choice.

Notes are written to the repository while you type, so there is no save button.
Nothing is committed until a sync: that commits what has changed, pulls and
pushes in one step, and the commit is named after the notes it carries -
`[fresh.md] added, [kept.md] changed, [gone.md] deleted`. The cloud button in
the search bar is the way to ask for one at any time and a dot on it says when there is something the remote has not been
told about yet. The app also syncs by itself when it is opened and when it is
left; that can be turned off under Settings → Repository if you would rather it
only happened when asked.

## Current limitation

- A repository has to live on the shared internal storage. A memory card or a usb stick has no file path git can be pointed at, and the app's own private directory is not offered because nothing else could reach the repository there.
- Android does not differentiate case for file name, so if you have a folder named `A` and another folder named `a`, `a` will not be displayed.
- A note above 2 MB is listed but not opened here - the editor is handed what the index holds, and files that large are not read into it. Use another app for those.
- Conflicts are resolved by hand, in the note. When the same note was changed here and on the remote, the sync stops and reports which notes it could not merge. Both versions are then in the note, between `<<<<<<<` and `>>>>>>>` markers: edit it down to what you want to keep and sync again, and that sync is what finishes the merge. Until then every sync refuses to commit, so the markers cannot end up in the history by themselves.

## Credits

Diffusion is a fork of GitNote, created by
[wiiznokes](https://github.com/wiiznokes) - see
[wiiznokes/gitnote](https://github.com/wiiznokes/gitnote). Everything up to the
fork is their work, and it stays under the same licence - see
[LICENSE](./LICENSE).

## AI

This project uses Claude Opus 5 for most of its work. It allows me to develop at a much faster speed and really get the app I want. I still very carefully review the code changes and which features are implemented. If you don't like the use of AI, a different project might be better suited for you <3
