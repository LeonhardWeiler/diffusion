use std::{
    collections::{HashMap, HashSet},
    fs,
    path::Path,
    str::FromStr,
    sync::{LazyLock, Mutex, OnceLock},
};

use git2::{
    CertificateCheckStatus, FetchOptions, IndexAddOption, Progress, PushOptions, RemoteCallbacks,
    Repository, Signature, StatusOptions, TreeWalkMode, TreeWalkResult,
};

use crate::callback::ProgressCB;
use crate::cred::{Cred, GitAuthor};
use crate::error::Error;
use crate::mime_types::is_extension_supported;

mod merge;

#[cfg(test)]
mod test;

#[cfg(test)]
mod test_merge;

const REMOTE: &str = "origin";

static REPO: LazyLock<Mutex<Option<Repository>>> = LazyLock::new(|| Mutex::new(None));

// https://github.com/libgit2/libgit2/pull/7056
static HOME_PATH: OnceLock<String> = OnceLock::new();

fn apply_ssh_workaround(clone: bool) {
    let home = HOME_PATH.get().unwrap();

    if clone {
        unsafe {
            std::env::set_var("HOME", home);
        }
    } else {
        let c_path = std::ffi::CString::from_str(home).expect("CString::new failed");

        unsafe {
            libgit2_sys::git_libgit2_opts(
                libgit2_sys::GIT_OPT_SET_HOMEDIR as std::ffi::c_int,
                c_path.as_ptr(),
            )
        };
    }

    if let Err(e) = std::fs::create_dir_all(format!("{home}/.ssh")) {
        error!("{e}");
    }
    if let Err(e) = fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(format!("{home}/.ssh/known_hosts"))
    {
        error!("{e}");
    }
}

fn pinned_hosts_path() -> String {
    format!("{}/.ssh/pinned_hosts", HOME_PATH.get().unwrap())
}

fn pinned_fingerprint(host: &str) -> Option<String> {
    let content = fs::read_to_string(pinned_hosts_path()).ok()?;

    content.lines().find_map(|line| {
        let (pinned_host, fingerprint) = line.split_once(' ')?;
        (pinned_host == host).then(|| fingerprint.to_string())
    })
}

fn pin_fingerprint(host: &str, fingerprint: &str) -> std::io::Result<()> {
    use std::io::Write;

    let mut file = fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(pinned_hosts_path())?;

    writeln!(file, "{host} {fingerprint}")
}

/// SSH host keys are pinned on first use. The known_hosts file of libgit2 is
/// never filled by the app, so its built in check would reject every host.
/// TLS certificates are left to libgit2 and the system trust store.
fn certificate_check(
    cert: &git2::cert::Cert<'_>,
    host: &str,
) -> Result<CertificateCheckStatus, git2::Error> {
    let Some(host_key) = cert.as_hostkey() else {
        return Ok(CertificateCheckStatus::CertificatePassthrough);
    };

    let Some(hash) = host_key.hash_sha256() else {
        return Err(git2::Error::from_str(
            "the host did not present a sha256 host key fingerprint",
        ));
    };

    let fingerprint: String = hash.iter().map(|byte| format!("{byte:02x}")).collect();

    match pinned_fingerprint(host) {
        Some(pinned) if pinned == fingerprint => Ok(CertificateCheckStatus::CertificateOk),
        Some(_) => Err(git2::Error::from_str(&format!(
            "the host key of {host} changed since the last connection"
        ))),
        None => {
            info!("pinning host key of {host}");
            if let Err(e) = pin_fingerprint(host, &fingerprint) {
                error!("{e}");
            }
            Ok(CertificateCheckStatus::CertificateOk)
        }
    }
}

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

pub fn create_repo(repo_path: &str) -> Result<(), Error> {
    let repo = Repository::init(repo_path).map_err(|e| Error::git2(e, "Repository::init"))?;

    REPO.lock().unwrap().replace(repo);

    Ok(())
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

fn credential_helper(cred: &Cred) -> Result<git2::Cred, git2::Error> {
    match cred {
        Cred::UserPassPlainText { username, password } => {
            git2::Cred::userpass_plaintext(username, password)
        }
        Cred::Ssh {
            username,
            private_key,
            public_key,
            passphrase,
        } => git2::Cred::ssh_key_from_memory(
            username,
            Some(public_key),
            private_key,
            passphrase.as_deref(),
        ),
    }
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

    REPO.lock().unwrap().replace(repo);

    Ok(())
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

pub fn commit_all(name: &str, email: &str, message: &str) -> Result<(), Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut index = repo.index().map_err(|e| Error::git2(e, "index"))?;

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

    // Get HEAD commit as parent, and Allow initial commit
    let parent_commit = repo.head().and_then(|r| r.peel_to_commit()).ok();

    let sig = Signature::now(name, email).map_err(|e| Error::git2(e, "Signature::now"))?;

    // Create commit
    match parent_commit {
        Some(ref parent) => repo.commit(Some("HEAD"), &sig, &sig, message, &tree, &[parent]),
        None => repo.commit(Some("HEAD"), &sig, &sig, message, &tree, &[]),
    }
    .map(|_| ())
    .map_err(|e| Error::git2(e, "commit"))
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

pub fn get_timestamps() -> Result<HashMap<String, i64>, Error> {
    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let head = repo.head()?.peel_to_commit()?;

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
        let time = commit.time().seconds() * 1000;

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
