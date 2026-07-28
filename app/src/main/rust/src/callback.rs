//! The way back into Kotlin while a clone is running.
//!
//! libgit2 reports progress by calling us; we pass it on to the Kotlin method
//! GitManager.progressCb, which is annotated @Keep for exactly this reason.

use jni::{Env, jni_sig, jni_str, objects::JObject};

pub struct ProgressCB<'ptr, 'local> {
    env: &'ptr mut Env<'local>,
    callback_class: JObject<'local>,
}

impl<'ptr, 'local> ProgressCB<'ptr, 'local> {
    pub fn new(env: &'ptr mut Env<'local>, callback_class: JObject<'local>) -> Self {
        Self {
            env,
            callback_class,
        }
    }
    pub fn progress(&mut self, progress: i32) -> bool {
        let res = self
            .env
            .call_method(
                &self.callback_class,
                jni_str!("progressCb"),
                jni_sig!((jint) -> jboolean),
                &[progress.into()],
            )
            .unwrap();

        res.z().unwrap()
    }
}
