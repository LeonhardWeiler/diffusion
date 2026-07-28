//! Getting to the other side: who we say we are, and who we accept on the
//! far end.
//!
//! TLS certificates are libgit2's business. Ssh host keys are not — libgit2
//! would only ever consult known_hosts, and nothing in this app writes one. So
//! a fingerprint is pinned the first time a host is seen, and a later change
//! stops the connection instead of asking a question nobody can answer.

use std::{fs, str::FromStr, sync::OnceLock};

use git2::CertificateCheckStatus;

use crate::cred::Cred;

// https://github.com/libgit2/libgit2/pull/7056
pub(super) static HOME_PATH: OnceLock<String> = OnceLock::new();

pub(super) fn apply_ssh_workaround(clone: bool) {
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
pub(super) fn certificate_check(
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

pub(super) fn credential_helper(cred: &Cred) -> Result<git2::Cred, git2::Error> {
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
