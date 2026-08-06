# Upstream

`upstream` is <https://github.com/wiiznokes/gitnote>, which this repository was
forked from. What it is worth watching for, and what it is not.

## What this fork no longer shares with it

Rust and libgit2 (JGit here), Room and its FTS table (`NoteIndex` here), paging
in the note list (a `StateFlow` of snapshots here), https and password
credentials (ssh only here), the OAuth provider integration (gone), and the
navigation library (`ui/navigation/NavHost.kt` here).

Those are the four or five subsystems upstream is actually working on, so a
patch of its own hardly ever applies. Reading its commits one by one costs more
than it returns.

## How to look, then

Only at the `### Fixed` sections of its changelog, and only every few months:

```bash
git fetch upstream && git log -p master..upstream/master -- CHANGELOG.md
```

A behaviour bug it finds can be one this fork has as well, even when the patch
is unusable — the ssh username below is exactly that case. Read those for the
bug, not for the diff.

## Reviewed up to `ea93766` (2026-08-06)

All 19 commits between `ba81060` and `ea93766` were read. One line was taken: an
unused `private val TAG` in `CustomDropDown.kt`, deleted rather than made
`const`.

Nothing else applied, and three are worth writing down because they will come up
again:

- **The timestamp algorithm** (`c2693a6`, sold as O(n·m) → O(n)) is behind
  `CommitTimestamps.kt`, which already walks the history once and stops as soon
  as every pending path has been dated. Upstream's version walks all of it and
  writes every path on every commit it appears in, so with upsert semantics the
  *oldest* commit wins rather than the newest.
- **The confirmation dialog before a folder is deleted in the setup** (`b6b39b6`)
  guards a danger this fork does not have: there is no create method here, and a
  clone into a folder that is not empty is refused (`SetupViewModel.cloneRepo`)
  instead of deleting what is in it.
- **The ssh username read off the url** (`b23dcdf`) was fixed here first
  (`SshTransport.kt`, `DEFAULT_SSH_USER`).

Two string changes were refused on purpose: upstream renames "deploy key" to
"deployment key", and "deploy key" is what the page the button leads to calls it.
