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
use crate::mime_types::is_extension_supported;
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
    if let Err(e) = apply_commit_timestamps_to(&repo) {
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

pub fn commit_all(name: &str, email: &str, message: &str) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut index = repo.index().map_err(|e| Error::git2(e, "index"))?;

    // Takes the working tree as it stands, conflicted paths included: a note
    // whose markers the user edited away is thereby resolved, which is what
    // ends the merge.
    index
        .add_all(["*"].iter(), IndexAddOption::DEFAULT, None)
        .map_err(|e| Error::git2(e, "add_all"))?;

    // Write index to disk
    index.write().map_err(|e| Error::git2(e, "write"))?;

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

    repo.commit(Some("HEAD"), &sig, &sig, message, &tree, &parents)
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
    if let Err(e) = apply_commit_timestamps_to(repo) {
        error!("apply_commit_timestamps: {e}");
    }

    Ok(())
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
fn commit_timestamps(repo: &Repository) -> Result<HashMap<String, i64>, Error> {
    // A repository without commits has no HEAD to walk. It has no timestamps to
    // offer either, so the files keep the ones the filesystem gives them.
    let Ok(head) = repo.head().and_then(|head| head.peel_to_commit()) else {
        return Ok(HashMap::new());
    };

    let mut pending = HashSet::new();

    head.tree()?.walk(TreeWalkMode::PreOrder, |root, entry| {
        if entry.kind() == Some(git2::ObjectType::Blob)
            && let Ok(name) = entry.name()
            && let Some(extension) = Path::new(name).extension()
            && let Some(extension) = extension.to_str()
            && is_extension_supported(extension)
        {
            pending.insert(format!("{root}{name}"));
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

    apply_commit_timestamps_to(repo)
}

/// The body of [apply_commit_timestamps], for the callers that already hold the
/// repository — the lock is not reentrant, so taking it again would hang.
fn apply_commit_timestamps_to(repo: &Repository) -> Result<(), Error> {
    let Some(workdir) = repo.workdir().map(Path::to_path_buf) else {
        return Ok(());
    };

    let dirty = dirty_paths(repo)?;

    for (path, seconds) in commit_timestamps(repo)? {
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
    fs::File::options().write(true).open(path)?.set_modified(time)
}
