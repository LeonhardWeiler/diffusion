<div align="center">

<h1>
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo_wide_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="assets/logo_wide_light.svg">
  <img alt="Diffusion" width="50%" src="assets/logo_wide_light.svg">
</picture>
</h1>

</div>

Diffusion is an Android app primarily designed for taking notes and syncing them between devices using a Git repository.

## Download

[GitHub Releases](https://github.com/LeonhardWeiler/diffusion/releases/latest)

[Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22io.github.leonhardweiler.diffusion%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FLeonhardWeiler%2Fdiffusion%22%2C%22author%22%3A%22LeonhardWeiler%22%2C%22name%22%3A%22Diffusion%22%2C%22preferredApkIndex%22%3A0%7D)

## Android signing certificate SHA-256:

71:9D:ED:3C:0C:A8:2E:7B:3C:28:CD:50:78:16:C7:67:04:62:8B:03:49:DE:BB:55:FC:FD:20:94:5C:49:FF:21

## Why

Other similar apps, such as [gittasks](https://github.com/christianjann/gittasks) and [gitnote](https://github.com/wiiznokes/gitnote) (which this project is forked from), already exist. However, they did not fully meet my needs, especially when it came to simplicity and performance. Rather than contributing upstream, I decided to fork GitNote because I wanted to build the app I envisioned as quickly as possible.

## Goals & Non-goals

The primary goal of this project is to sync notes through a Git repository using SSH keys, while providing a file browser and a basic Markdown editor. The editor is by far the largest part of the project, and it may eventually become a separate component or even be removed. The goal is not to support as many file types as possible, but to provide the functionality that a note-syncing app actually needs.

The editor might change in the future, but the file browser will remain. I believe that your files and their Git status belong in the same place.

## Functionality

To add a repository to Diffusion, you can either clone a repository or open an existing one. You will need to add the generated SSH key as a deploy key on your Git hosting service of choice.

Notes are written directly to the repository as you type, so there is no save button.

Creating a note asks for its name and then leaves it in the list; it is not opened for you. The name is exactly what you type, extension included — a name without one is treated as a plain text note. Renaming or moving a note happens from its row in the list, not from the note itself.

The list shows folders first and then notes, both ordered by what was written most recently. A folder takes the date of the newest note inside it, however deep.

Markdown notes carry a button above them that switches between reading and writing. That choice is remembered for each note.

Nothing is committed until a sync is performed. A sync commits all changes, pulls from the remote, and pushes local commits in a single step. Commit messages are automatically generated from the affected notes, for example:

```
[fresh.md] added, [kept.md] changed, [gone.md] deleted
```

The cloud button in the search bar starts a sync at any time. A dot on the button indicates that there are local changes that have not yet been pushed to the remote.

By default, the app also syncs automatically when it is opened and when it is closed. This behavior can be disabled under **Settings → Repository** if you prefer to sync only manually.

## Current limitations

- A repository must be stored on shared internal storage. Repositories on SD cards or USB drives cannot be used because Git cannot be pointed to them through Android's storage APIs. The app's private storage is also not supported because other apps would not be able to access the repository there.
- Android uses a case-insensitive file system, so if your repository contains both a folder named `A` and another named `a`, only one of them will be visible.
- Notes larger than 2 MB are listed but cannot be opened in the editor, and their contents are not included in search results. Files that large are intentionally not loaded into memory. You should use another editor for them.
- The note list is rebuilt from the repository every time the app starts. For very large repositories, this may result in a short delay before all notes appear. Nothing is cached between launches; the repository itself is always treated as the source of truth.
- Merge conflicts must be resolved manually. If the same note has been modified both locally and remotely, syncing stops and reports the affected files. Git conflict markers (`<<<<<<<`, `=======`, and `>>>>>>>`) are inserted into the note. Edit the file until only the version you want remains, then sync again to complete the merge. Until the conflict is resolved, syncing will not create any new commits, preventing conflict markers from accidentally being committed.

## Credits

Diffusion is a fork of GitNote, originally created by [wiiznokes](https://github.com/wiiznokes). Everything up to the point of the fork is their work and remains under the same license. See [wiiznokes/gitnote](https://github.com/wiiznokes/gitnote) and the [LICENSE](./LICENSE) file for details.

## AI

This project uses Claude Opus 5 for most of its development. It allows me to work much faster and build the app I want. I still carefully review every code change and decide which features are implemented.

If you prefer software that is developed without the use of AI, another project may be a better fit for you. <3
