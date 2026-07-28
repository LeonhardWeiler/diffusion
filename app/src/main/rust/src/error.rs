//! What can go wrong on the way to libgit2, and how it reaches Kotlin.
//!
//! Kotlin only gets an integer back from every call, so an error has to be
//! squeezed into one — and its text parked somewhere the caller can pick it up.

use std::fmt::Display;
use std::sync::Mutex;

use jni::sys::jint;

pub(crate) const OK: jint = 0;

/// Returned when a pull cannot be merged automatically. Not a libgit2 error, so
/// it needs a code of its own, far away from the raw codes libgit2 uses.
/// Kotlin knows this value as GitManager.MERGE_CONFLICT.
pub(crate) const MERGE_CONFLICT: jint = -1000;

#[derive(Debug)]
pub(crate) enum Error {
    Git2 {
        error: git2::Error,
        msg: String,
    },
    /// The remote and the local side changed the same lines. The working tree
    /// is left untouched, the caller has to resolve this outside of the app.
    MergeConflict {
        paths: Vec<String>,
    },
}

impl From<git2::Error> for Error {
    fn from(value: git2::Error) -> Self {
        Self::git2(value, "")
    }
}

impl Error {
    pub(crate) fn git2(error: git2::Error, msg: &str) -> Self {
        Self::Git2 {
            error,
            msg: msg.into(),
        }
    }

    pub(crate) fn add_message(self, msg1: &str) -> Self {
        match self {
            Error::Git2 { error, msg } => Error::Git2 {
                error,
                msg: format!("{}: {}", msg1, msg),
            },
            Error::MergeConflict { paths } => Error::MergeConflict { paths },
        }
    }
}

/// The message of the last error that was turned into a return code. Kotlin only
/// gets an integer back, so without this the text libgit2 produced would be
/// dropped and the user would be shown a bare number.
pub(crate) static LAST_ERROR: Mutex<Option<String>> = Mutex::new(None);

impl From<Error> for jint {
    fn from(value: Error) -> Self {
        if let Ok(mut last_error) = LAST_ERROR.lock() {
            *last_error = Some(value.to_string());
        }

        match value {
            Error::Git2 { error, .. } => error.raw_code(),
            Error::MergeConflict { .. } => MERGE_CONFLICT,
        }
    }
}

impl Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Error::Git2 { error, msg } => {
                write!(f, "{msg}: {error}")
            }
            Error::MergeConflict { paths } => {
                write!(f, "merge conflict in: {}", paths.join(", "))
            }
        }
    }
}
