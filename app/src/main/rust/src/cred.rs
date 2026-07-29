//! The credentials and the author, as Kotlin hands them over.
//!
//! Both cross the JNI boundary as objects rather than as strings, so reading
//! them means asking the JVM for one field at a time.

use std::fmt::Debug;

use anyhow::anyhow;
use git2::Signature;
use jni::objects::{JObject, JString};
use jni::{Env, jni_sig, jni_str};

/// How the remote is authenticated against. An ssh key, and nothing else — the
/// username and password pair went with https.
pub enum Cred {
    Ssh {
        username: String,
        public_key: String,
        private_key: String,
        passphrase: Option<String>,
    },
}

pub struct GitAuthor {
    pub name: String,
    pub email: String,
}

impl<'a> From<Signature<'a>> for GitAuthor {
    fn from(value: Signature<'a>) -> Self {
        GitAuthor {
            name: value.name().unwrap_or("").to_string(),
            email: value.email().unwrap_or("").to_string(),
        }
    }
}

impl Debug for Cred {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Ssh {
                username,
                public_key,
                private_key: _private_key,
                passphrase: _passphrase,
            } => f
                .debug_struct("Ssh")
                .field("username", username)
                .field("public_key", public_key)
                .finish(),
        }
    }
}

macro_rules! jstring_field {
    ($env:expr, $obj:expr, $field:literal) => {{
        let obj = $env
            .get_field($obj, jni_str!($field), jni_sig!(JString))?
            .l()?;

        $env.as_cast::<JString>(&obj)?
            .mutf8_chars($env)?
            .to_string()
    }};
}

macro_rules! jstring_field_nullable {
    ($env:expr, $obj:expr, $field:literal) => {{
        let obj = $env
            .get_field($obj, jni_str!($field), jni_sig!(JString))?
            .l()?;

        if obj.is_null() {
            None
        } else {
            Some(
                $env.as_cast::<JString>(&obj)?
                    .mutf8_chars($env)?
                    .to_string(),
            )
        }
    }};
}

impl Cred {
    pub fn from_jni(env: &mut Env, cred_obj: &JObject) -> anyhow::Result<Option<Self>> {
        if cred_obj.is_null() {
            return Ok(None);
        }

        let class_name = {
            let class = env.get_object_class(cred_obj)?;

            let obj = env
                .call_method(class, jni_str!("getName"), jni_sig!(() -> JString), &[])?
                .l()?;

            let jstring = env.as_cast::<JString>(&obj)?;

            jstring.mutf8_chars(env)?.to_string()
        };

        match class_name.as_str() {
            "io.github.leonhardweiler.diffusion.ui.model.Cred$Ssh" => {
                let username = jstring_field!(env, cred_obj, "username");
                let public_key = jstring_field!(env, cred_obj, "publicKey");

                let private_key = jstring_field!(env, cred_obj, "privateKey");
                let passphrase = jstring_field_nullable!(env, cred_obj, "passphrase");

                Ok(Some(Cred::Ssh {
                    username,
                    public_key,
                    private_key,
                    passphrase,
                }))
            }
            other => Err(anyhow!("Unknown class name: {}", other)),
        }
    }
}
