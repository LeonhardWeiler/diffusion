use std::{
    collections::{HashMap, HashSet},
    fs,
    path::Path,
    sync::{LazyLock, Mutex},
    time::{Duration, SystemTime},
};

use git2::{
    FetchOptions, IndexAddOption, Progress, PushOptions, RemoteCallbacks, Repository, Signature,
    StatusOptions, TreeWalkMode, TreeWalkResult,
};

use crate::callback::ProgressCB;
use crate::cred::{Cred, GitAuthor};
use crate::error::Error;
use transport::{HOME_PATH, apply_ssh_workaround, certificate_check, credential_helper};

mod merge;
mod transport;

#[cfg(test)]
mod test;

#[cfg(test)]
mod test_merge;

const REMOTE: &str = "origin";

static REPO: LazyLock<Mutex<Option<Repository>>> = LazyLock::new(|| Mutex::new(None));

pub fn init_lib(home_path: String) {
    info!("home_path: {home_path}");
    let _ = HOME_PATH.set(home_path.clone());

    unsafe {
        std::env::set_var("HOME", &home_path);
    }

    let git_config_path = Path::new(&home_path).join(".gitconfig");

    let git_config_content = "[safe]\n\tdirectory = *";

    match fs::exists(&git_config_path) {
        Ok(true) => {}
        Ok(false) => {
            if let Err(e) = fs::create_dir_all(git_config_path.parent().unwrap()) {
                error!("gitconfig: {e}");
            }

            if let Err(e) = fs::write(&git_config_path, git_config_content) {
                error!("gitconfig: {e}");
            } else {
                debug!("successfully written the gitconfig file")
            }
        }
        Err(e) => {
            error!("gitconfig: {e}");
        }
    }

    unsafe {
        if let Err(e) = git2::opts::set_server_connect_timeout_in_milliseconds(7000) {
            error!("set_server_connect_timeout_in_milliseconds: {e}");
        }

        if let Err(e) = git2::opts::set_server_timeout_in_milliseconds(7000) {
            error!("set_server_timeout_in_milliseconds: {e}");
        }
    };
}

pub fn open_repo(repo_path: &str) -> Result<(), Error> {
    let repo = Repository::open(repo_path).map_err(|e| Error::git2(e, "Repository::open"))?;

    REPO.lock().unwrap().replace(repo);

    Ok(())
}

fn current_branch(repo: &Repository) -> Result<String, Error> {
    let head = repo.head().map_err(|e| Error::git2(e, "head"))?;

    if head.is_branch()
        && let Ok(name) = head.shorthand()
    {
        return Ok(name.to_string());
    }

    // Detached HEAD or not a branch
    Err(Error::git2(
        git2::Error::from_str("unable to determine default branch"),
        "",
    ))
}

pub fn clone_repo(
    repo_path: &str,
    remote_url: &str,
    cred: Option<Cred>,
    mut cb: ProgressCB,
) -> Result<(), Error> {
    apply_ssh_workaround(true);
    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(certificate_check);

    if let Some(cred) = cred {
        callbacks
            .credentials(move |_url, _username_from_url, _allowed_types| credential_helper(&cred));
    }

    callbacks.transfer_progress(|stats: Progress| {
        let progress = stats.indexed_objects() as f32 / stats.total_objects() as f32 * 100.;

        cb.progress(progress as i32)
    });

    let mut fetch_options = FetchOptions::new();
    fetch_options
        .remote_callbacks(callbacks)
        .download_tags(git2::AutotagOption::None);

    let mut builder = git2::build::RepoBuilder::new();

    let repo = builder
        .fetch_options(fetch_options)
        .clone(remote_url, std::path::Path::new(&repo_path))
        .map_err(|e| Error::git2(e, "clone"))?;

    // Everything that just landed carries the time of the checkout. Left that
    // way, a repository of years of notes would read as written this minute.
    if let Err(e) = apply_commit_timestamps_to(&repo, None) {
        error!("apply_commit_timestamps: {e}");
    }

    REPO.lock().unwrap().replace(repo);

    Ok(())
}

/// Points the repository at [url], adding the remote if it has none.
///
/// Written into the repository itself, because that is where push and pull look
/// for it — what the app stores in its preferences is a copy for the settings
/// screen, not the thing git reads.
pub fn set_remote_url(url: &str) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    if repo.find_remote(REMOTE).is_ok() {
        repo.remote_set_url(REMOTE, url)
            .map_err(|e| Error::git2(e, "remote_set_url"))?;
    } else {
        repo.remote(REMOTE, url)
            .map_err(|e| Error::git2(e, "remote"))?;
    }

    Ok(())
}

/// The url the repository pushes to and pulls from, as it is configured. None
/// when the repository has no remote at all, which a purely local one has not.
pub fn remote_url() -> Option<String> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref()?;

    let remote = repo.find_remote(REMOTE).ok()?;

    remote.url().map(str::to_string).ok()
}

pub fn last_commit() -> Option<String> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    // new repo have no commit, so this function can fail
    let head = repo.refname_to_id("HEAD").ok()?;

    Some(head.to_string())
}

pub fn signature() -> Option<(String, String)> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref()?;

    if let Ok(signature) = repo.signature() {
        let name = signature.name().unwrap_or_default().to_string();
        let email = signature.email().unwrap_or_default().to_string();

        if !name.is_empty() || !email.is_empty() {
            return Some((name, email));
        }
    }

    let head = repo.head().ok()?;
    let commit = head.peel_to_commit().ok()?;
    let author = commit.author();

    Some((
        author.name().unwrap_or_default().to_string(),
        author.email().unwrap_or_default().to_string(),
    ))
}

/// What the next commit follows.
///
/// HEAD, which a repository without commits does not have. And, when a merge
/// stopped on a conflict, whatever it was merging in: a commit that ends such a
/// merge has to name both sides, or the merge is undone by the very commit that
/// finishes it and the same conflict arrives again on the next pull.
fn commit_parents(repo: &Repository) -> Result<Vec<git2::Commit<'_>>, Error> {
    let mut parents = Vec::new();

    if let Ok(head) = repo.head().and_then(|r| r.peel_to_commit()) {
        parents.push(head);
    }

    // Read rather than walked with mergehead_foreach, which wants the
    // repository mutably and there is only one side to a pull anyway.
    if repo.state() == git2::RepositoryState::Merge
        && let Ok(merge_head) = repo.revparse_single("MERGE_HEAD")
        && let Ok(commit) = merge_head.peel_to_commit()
    {
        parents.push(commit);
    }

    Ok(parents)
}

/// The lines a conflict leaves behind that no note would hold otherwise.
///
/// "=======" is not among them: seven equals signs at the start of a line are
/// how markdown underlines a heading, and a note that has one is not a note with
/// a conflict in it.
const CONFLICT_MARKERS: [&str; 2] = ["<<<<<<<", ">>>>>>>"];

/// The notes still holding the markers a conflict wrote into them.
///
/// Only the ones the merge could not merge are looked at: they are the only
/// ones a marker could have got into, and reading the whole repository to find
/// that out would be paid for on every sync.
fn unresolved_conflicts(repo: &Repository, index: &git2::Index) -> Vec<String> {
    let Some(workdir) = repo.workdir() else {
        return Vec::new();
    };

    let Ok(conflicts) = index.conflicts() else {
        return Vec::new();
    };

    let mut paths: Vec<String> = conflicts
        .filter_map(|conflict| conflict.ok())
        .filter_map(|conflict| conflict.our.or(conflict.their).or(conflict.ancestor))
        .map(|entry| String::from_utf8_lossy(&entry.path).into_owned())
        .filter(|path| {
            // a note that was deleted rather than edited resolved the conflict
            // as well, and reading it is how that is noticed
            fs::read_to_string(workdir.join(path)).is_ok_and(|content| {
                content.lines().any(|line| {
                    CONFLICT_MARKERS
                        .iter()
                        .any(|marker| line.starts_with(marker))
                })
            })
        })
        .collect();

    paths.sort();
    paths.dedup();
    paths
}

/// How many names a group of the subject line carries before the rest of them
/// are only counted.
const MAX_NAMES_IN_SUBJECT: usize = 3;

/// The notes a commit is about, grouped by what happened to them.
#[derive(Default)]
struct Changes {
    added: Vec<String>,
    changed: Vec<String>,
    deleted: Vec<String>,
}

impl Changes {
    /// What the index holds that HEAD does not.
    fn of(repo: &Repository, index: &git2::Index) -> Self {
        let mut changes = Self::default();

        let head_tree = repo.head().ok().and_then(|head| head.peel_to_tree().ok());

        let Ok(diff) = repo.diff_tree_to_index(head_tree.as_ref(), Some(index), None) else {
            return changes;
        };

        for delta in diff.deltas() {
            let path = |file: git2::DiffFile| {
                file.path()
                    .map(|path| path.to_string_lossy().into_owned())
                    .unwrap_or_default()
            };

            match delta.status() {
                git2::Delta::Added | git2::Delta::Copied => {
                    changes.added.push(path(delta.new_file()))
                }
                git2::Delta::Deleted => changes.deleted.push(path(delta.old_file())),
                // A rename is a name that changed, which is the only thing the
                // list shows about a note anyway. Detection is off by default,
                // so this is the rare arm.
                git2::Delta::Modified | git2::Delta::Renamed | git2::Delta::Typechange => {
                    changes.changed.push(path(delta.new_file()))
                }
                _ => {}
            }
        }

        changes.added.sort();
        changes.changed.sort();
        changes.deleted.sort();
        changes
    }

    fn is_empty(&self) -> bool {
        self.added.is_empty() && self.changed.is_empty() && self.deleted.is_empty()
    }

    fn groups(&self) -> [(&str, &Vec<String>); 3] {
        [
            ("added", &self.added),
            ("changed", &self.changed),
            ("deleted", &self.deleted),
        ]
    }
}

/// One group of the subject line: `[a.md, b.md] added`.
fn subject_group(paths: &[String], verb: &str) -> Option<String> {
    if paths.is_empty() {
        return None;
    }

    let mut list = paths
        .iter()
        .take(MAX_NAMES_IN_SUBJECT)
        .map(String::as_str)
        .collect::<Vec<_>>()
        .join(", ");

    let hidden = paths.len().saturating_sub(MAX_NAMES_IN_SUBJECT);
    if hidden > 0 {
        list.push_str(&format!(" and {hidden} more"));
    }

    Some(format!("[{list}] {verb}"))
}

/// What the commit is about, read off the index.
///
/// Every commit the app ever made said "commit from gitnote", so the history
/// recorded when something had been synced and never what. The names are the
/// paths of the notes, which is what a history is read by; a subject line that
/// would grow without end counts the rest and lists them underneath.
fn commit_message(repo: &Repository, index: &git2::Index, fallback: &str) -> String {
    let changes = Changes::of(repo, index);

    if changes.is_empty() {
        // A merge that changed no file still needs a commit to close it, and
        // that is the one thing it can honestly be called.
        return if repo.state() == git2::RepositoryState::Merge {
            "Merge".to_string()
        } else {
            fallback.to_string()
        };
    }

    let subject = changes
        .groups()
        .iter()
        .filter_map(|(verb, paths)| subject_group(paths, verb))
        .collect::<Vec<_>>()
        .join(", ");

    // Only when the subject had to leave names out: repeating three paths
    // underneath the line that already names them says nothing.
    if changes
        .groups()
        .iter()
        .all(|(_, paths)| paths.len() <= MAX_NAMES_IN_SUBJECT)
    {
        return subject;
    }

    let body = changes
        .groups()
        .iter()
        .filter(|(_, paths)| !paths.is_empty())
        .map(|(verb, paths)| {
            let names = paths
                .iter()
                .map(|path| format!("  {path}"))
                .collect::<Vec<_>>()
                .join("\n");
            format!("{verb}:\n{names}")
        })
        .collect::<Vec<_>>()
        .join("\n\n");

    format!("{subject}\n\n{body}")
}

pub fn commit_all(name: &str, email: &str, fallback_message: &str) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut index = repo.index().map_err(|e| Error::git2(e, "index"))?;

    // A conflict is fixed by editing the note, and the sync after that is what
    // ends the merge. Nothing says the note was edited, though, and this commits
    // the working tree as it stands — so without asking, closing the app would
    // be enough to write "<<<<<<<" and both versions into the history, and the
    // automatic sync means it takes no tap at all.
    let unresolved = unresolved_conflicts(repo, &index);
    if !unresolved.is_empty() {
        error!("conflict markers still in: {}", unresolved.join(", "));
        return Err(Error::UnresolvedConflict { paths: unresolved });
    }

    // Takes the working tree as it stands, conflicted paths included: a note
    // whose markers the user edited away is thereby resolved, which is what
    // ends the merge.
    index
        .add_all(["*"].iter(), IndexAddOption::DEFAULT, None)
        .map_err(|e| Error::git2(e, "add_all"))?;

    // Write index to disk
    index.write().map_err(|e| Error::git2(e, "write"))?;

    // Read off the index rather than the working tree: what is about to be
    // committed is exactly what was just staged.
    let message = commit_message(repo, &index, fallback_message);

    // Write tree
    let tree_oid = index
        .write_tree()
        .map_err(|e| Error::git2(e, "write_tree"))?;

    let tree = repo
        .find_tree(tree_oid)
        .map_err(|e| Error::git2(e, "find_tree"))?;

    let parents = commit_parents(repo)?;
    let parents: Vec<&git2::Commit> = parents.iter().collect();

    let sig = Signature::now(name, email).map_err(|e| Error::git2(e, "Signature::now"))?;

    repo.commit(Some("HEAD"), &sig, &sig, &message, &tree, &parents)
        .map_err(|e| Error::git2(e, "commit"))?;

    // Nothing is in progress anymore, whether or not anything was.
    repo.cleanup_state()
        .map_err(|e| Error::git2(e, "cleanup_state"))
}

pub fn push(cred: Option<Cred>) -> Result<(), Error> {
    apply_ssh_workaround(false);

    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = repo
        .find_remote(REMOTE)
        .map_err(|e| Error::git2(e, "find_remote"))?;

    let branch = current_branch(repo)?;
    let refspecs = [format!("refs/heads/{branch}:refs/heads/{branch}")];

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(certificate_check);

    if let Some(cred) = cred {
        callbacks
            .credentials(move |_url, _username_from_url, _allowed_types| credential_helper(&cred));
    }

    let mut push_opts = PushOptions::new();
    push_opts.remote_callbacks(callbacks);

    remote
        .push(&refspecs, Some(&mut push_opts))
        .map_err(|e| Error::git2(e, "push"))?;

    Ok(())
}

pub fn pull(cred: Option<Cred>, author: &GitAuthor) -> Result<(), Error> {
    apply_ssh_workaround(false);

    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut remote = repo
        .find_remote(REMOTE)
        .map_err(|e| Error::git2(e, "find_remote"))?;

    let mut callbacks = RemoteCallbacks::new();

    callbacks.certificate_check(certificate_check);

    if let Some(cred) = cred {
        callbacks
            .credentials(move |_url, _username_from_url, _allowed_types| credential_helper(&cred));
    }

    let mut fetch_options = FetchOptions::new();
    fetch_options
        .remote_callbacks(callbacks)
        .download_tags(git2::AutotagOption::None);

    // What the working tree stood at before anything came in. The dating below
    // is the only reason it is kept.
    let before = repo
        .head()
        .and_then(|head| head.peel_to_commit())
        .map(|commit| commit.id())
        .ok();

    let branch = current_branch(repo)?;
    let refspec = format!("+refs/heads/{}:refs/remotes/origin/{}", branch, branch);
    remote
        .fetch(&[&refspec], Some(&mut fetch_options), None)
        .map_err(|e| Error::git2(e, "fetch"))?;

    let fetch_head = repo
        .find_reference("FETCH_HEAD")
        .map_err(|e| Error::git2(e, "find_reference"))?;

    let commit = repo
        .reference_to_annotated_commit(&fetch_head)
        .map_err(|e| Error::git2(e, "reference_to_annotated_commit"))?;

    merge::do_merge(repo, &branch, commit, author).map_err(|e| e.add_message("do_merge"))?;

    // The merge checked out whatever came in, which dates those notes to now
    // rather than to when they were written on the other device.
    if let Err(e) = date_pulled_notes(repo, before) {
        error!("apply_commit_timestamps: {e}");
    }

    Ok(())
}

/// Dates the notes a pull brought in, and only those.
///
/// The sync commits before it pulls, so by the time the merge is done the notes
/// written on this device agree with HEAD as well — and dating those by their
/// commit would move every one of them to the minute the sync ran. A note
/// written on Monday and synced on Friday is from Monday. What the pull itself
/// wrote is the exception: the checkout stamped it with the moment it ran, and
/// nothing but the commit behind it can say when it was written.
fn date_pulled_notes(repo: &Repository, before: Option<git2::Oid>) -> Result<(), Error> {
    let Some(before) = before else {
        // No commit before the pull means no working tree before it either, so
        // everything standing in it now arrived with the pull.
        return apply_commit_timestamps_to(repo, None);
    };

    let before = repo.find_commit(before)?;
    let head = repo.head()?.peel_to_commit()?;

    if head.id() == before.id() {
        // the pull brought nothing, so it wrote nothing
        return Ok(());
    }

    let diff = repo.diff_tree_to_tree(Some(&before.tree()?), Some(&head.tree()?), None)?;

    let written: HashSet<String> = diff
        .deltas()
        .filter_map(|delta| delta.new_file().path().and_then(Path::to_str))
        .map(str::to_string)
        .collect();

    apply_commit_timestamps_to(repo, Some(&written))
}

pub fn close() {
    let mut repo = REPO.lock().expect("repo lock");
    repo.take();
}

pub fn is_change() -> Result<bool, Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut opts = StatusOptions::new();
    opts.include_untracked(true).recurse_untracked_dirs(true);

    let statuses = repo
        .statuses(Some(&mut opts))
        .map_err(|e| Error::git2(e, "statuses"))?;

    let count = statuses.len();

    Ok(count > 0)
}

/// The paths the working tree does not agree with HEAD about, whether they are
/// changed, staged or not tracked at all.
fn dirty_paths(repo: &Repository) -> Result<HashSet<String>, Error> {
    let mut opts = StatusOptions::new();
    opts.include_untracked(true).recurse_untracked_dirs(true);

    let statuses = repo
        .statuses(Some(&mut opts))
        .map_err(|e| Error::git2(e, "statuses"))?;

    Ok(statuses
        .iter()
        .filter_map(|entry| entry.path().map(str::to_string).ok())
        .collect())
}

/// When each note was last changed by a commit, in seconds since the epoch.
///
/// [only] narrows that to the paths named in it, which also ends the walk over
/// the history as soon as those have been found.
fn commit_timestamps(
    repo: &Repository,
    only: Option<&HashSet<String>>,
) -> Result<HashMap<String, i64>, Error> {
    // A repository without commits has no HEAD to walk. It has no timestamps to
    // offer either, so the files keep the ones the filesystem gives them.
    let Ok(head) = repo.head().and_then(|head| head.peel_to_commit()) else {
        return Ok(HashMap::new());
    };

    let mut pending = HashSet::new();

    // Every blob, not only the ones the app can read itself: the list shows
    // every file in the repository with a date beside it, and a photo that was
    // committed a year ago should not read as written the minute it was cloned.
    head.tree()?.walk(TreeWalkMode::PreOrder, |root, entry| {
        if entry.kind() == Some(git2::ObjectType::Blob)
            && let Ok(name) = entry.name()
        {
            let path = format!("{root}{name}");
            if only.is_none_or(|only| only.contains(&path)) {
                pending.insert(path);
            }
        }
        TreeWalkResult::Ok
    })?;

    let mut file_timestamps = HashMap::new();

    // One walk over the history, taking the first commit that touches a path.
    // Walking it once per file does the same work again for every file.
    let mut revwalk = repo.revwalk()?;
    revwalk.push_head()?;
    revwalk.set_sorting(git2::Sort::TIME)?;

    for oid in revwalk {
        if pending.is_empty() {
            break;
        }

        let commit = repo.find_commit(oid?)?;
        let parent_tree = commit
            .parents()
            .next()
            .map(|parent| parent.tree())
            .transpose()?;

        let diff = repo.diff_tree_to_tree(parent_tree.as_ref(), Some(&commit.tree()?), None)?;
        let time = commit.time().seconds();

        for delta in diff.deltas() {
            if let Some(path) = delta.new_file().path().and_then(|path| path.to_str())
                && pending.remove(path)
            {
                file_timestamps.insert(path.to_string(), time);
            }
        }
    }

    Ok(file_timestamps)
}

/// Gives every unchanged note the time of the commit that last wrote it.
///
/// The note list reads its dates off the filesystem, which is the only place a
/// note that was never committed has one. A checkout does not honour that: it
/// stamps every file it writes with the moment it ran, so without this a clone
/// would show a repository of years of notes as all written just now.
///
/// Files the working tree disagrees with HEAD about are left alone. Their own
/// timestamp is the true one — it is when the user typed, which is later than
/// any commit that could speak for them.
pub fn apply_commit_timestamps() -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    apply_commit_timestamps_to(repo, None)
}

/// The body of [apply_commit_timestamps], for the callers that already hold the
/// repository — the lock is not reentrant, so taking it again would hang.
///
/// [only] names the notes to date, None every one of them. A pull passes the
/// ones it wrote: the rest of the working tree it did not touch, and a date it
/// did not touch is not one it may move.
fn apply_commit_timestamps_to(
    repo: &Repository,
    only: Option<&HashSet<String>>,
) -> Result<(), Error> {
    let Some(workdir) = repo.workdir().map(Path::to_path_buf) else {
        return Ok(());
    };

    let dirty = dirty_paths(repo)?;

    for (path, seconds) in commit_timestamps(repo, only)? {
        if dirty.contains(&path) {
            continue;
        }

        let Ok(seconds) = u64::try_from(seconds) else {
            continue;
        };

        let time = SystemTime::UNIX_EPOCH + Duration::from_secs(seconds);

        // Best effort: a note whose date could not be written still reads
        // fine, it only carries the time of the checkout.
        if let Err(e) = set_modified(&workdir.join(&path), time) {
            debug!("could not date {path}: {e}");
        }
    }

    Ok(())
}

fn set_modified(path: &Path, time: SystemTime) -> Result<(), std::io::Error> {
    fs::File::options()
        .write(true)
        .open(path)?
        .set_modified(time)
}
