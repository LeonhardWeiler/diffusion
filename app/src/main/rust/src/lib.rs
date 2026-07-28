use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jint};
use jni::{Env, NativeMethod, jni_sig, jni_str, native_method};

use crate::callback::ProgressCB;
use crate::cred::{Cred, GitAuthor};
use crate::error::{LAST_ERROR, OK};
use crate::key_gen::gen_keys;
use crate::utils::install_panic_hook;

#[macro_use]
extern crate log;
#[macro_use]
mod utils;

mod callback;
mod cred;
mod error;
mod key_gen;
mod libgit2;
mod mime_types;
mod url;

#[cfg(test)]
mod test;

const _INIT_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn init_lib(home_path: JString) -> jint,
};

const _OPEN_REPO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn open_repo_lib(repo_path: JString) -> jint,
};

const _CLONE_REPO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    export = "Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_cloneRepoLib",
    static extern fn clone_repo_lib(repo_path: JString, remote_url: JString, cred: JObject, progress_callback: JObject) -> jint,
};

const _LAST_COMMIT_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn last_commit_lib() -> JString,
};

const _REMOTE_URL_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn remote_url_lib() -> JString,
};

const _COMMIT_ALL_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn commit_all_lib(name: JString, email: JString, message: JString) -> jint,
};

const _CURRENT_SIGNATURE_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    export = "Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_currentSignatureLib",
    static extern fn current_signature_lib() -> JObject,
};

const _PUSH_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    export = "Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pushLib",
    static extern fn push_lib(cred: JObject) -> jint,
};

const _PULL_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    export = "Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_pullLib",
    static extern fn pull_lib(cred: JObject, name: JString, email: JString) -> jint,
};

const _FREE_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn free_lib(),
};

const _CLOSE_REPO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn close_repo_lib(),
};

const _IS_CHANGE_LIB_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn is_change_lib() -> jint,
};

const _GET_TIMESTAMPS_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    export = "Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_getTimestampsLib",
    static extern fn get_timestamps_lib(j_map: JObject) -> jint,
};

const _GENERATE_SSH_KEYS_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    export = "Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_generateSshKeysLib",
    static extern fn generate_ssh_keys_lib() -> JObject,
};

const _EXTENSION_TYPE_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.MimeTypeManagerKt",
    static extern fn extension_type_lib(extension: JString) -> jint,
};

const _IS_EXTENSION_SUPPORTED_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.MimeTypeManagerKt",
    static extern fn is_extension_supported_lib(extension: JString) -> jboolean,
};

const _GET_URL_INFO_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    export = "Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_getUrlInfoLib",
    static extern fn get_url_info_lib(url: JString) -> JObject,
};

const _BROWSER_URL_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    static extern fn browser_url_lib(url: JString) -> JString,
};

const _LAST_ERROR_MESSAGE_LIB_METHOD: NativeMethod = native_method! {
    java_type = "io.github.wiiznokes.gitnote.manager.GitManagerKt",
    export = "Java_io_github_wiiznokes_gitnote_manager_GitManagerKt_lastErrorMessageLib",
    static extern fn last_error_message_lib() -> JObject,
};

fn init_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    home_path: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let home_path = home_path.try_to_string(env).unwrap();
    libgit2::init_lib(home_path);

    install_panic_hook();

    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Trace)
            .with_tag("rust")
            .with_filter(
                android_logger::FilterBuilder::new()
                    .parse("warn,git_wrapper=debug")
                    .build(),
            ),
    );

    Ok(OK)
}

fn open_repo_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    repo_path: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let repo_path = repo_path.try_to_string(env).unwrap();

    unwrap_or_log!(libgit2::open_repo(&repo_path), "open_repo");

    Ok(OK)
}

fn clone_repo_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    repo_path: JString<'local>,
    remote_url: JString<'local>,
    cred: JObject<'local>,
    progress_callback: JObject<'local>,
) -> Result<jint, jni::errors::Error> {
    let repo_path = repo_path.try_to_string(env).unwrap();
    let remote_url = remote_url.try_to_string(env).unwrap();

    let cred = match Cred::from_jni(env, &cred) {
        Ok(cred) => cred,
        Err(e) => {
            error!("Cred::from_jni: {e}");
            panic!()
        }
    };

    let cb = ProgressCB::new(env, progress_callback);

    unwrap_or_log!(
        libgit2::clone_repo(&repo_path, &remote_url, cred, cb),
        "clone_repo"
    );

    Ok(OK)
}
fn last_commit_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
) -> Result<JString<'local>, jni::errors::Error> {
    let commit = match libgit2::last_commit() {
        Some(commit) => commit,
        None => return Ok(JString::null()),
    };

    let s = env
        .new_string(commit)
        .expect("Couldn't create Java string!");

    Ok(s)
}
fn remote_url_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
) -> Result<JString<'local>, jni::errors::Error> {
    let url = match libgit2::remote_url() {
        Some(url) => url,
        None => return Ok(JString::null()),
    };

    Ok(env.new_string(url).expect("Couldn't create Java string!"))
}

fn commit_all_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    name: JString<'local>,
    email: JString<'local>,
    message: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let name = name.try_to_string(env).unwrap();
    let email = email.try_to_string(env).unwrap();
    let message = message.try_to_string(env).unwrap();

    unwrap_or_log!(libgit2::commit_all(&name, &email, &message), "commit_all");

    Ok(OK)
}

fn current_signature_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
) -> Result<JObject<'local>, jni::errors::Error> {
    let signature = match libgit2::signature() {
        Some(signature) => signature,
        None => return Ok(JObject::null()),
    };

    let name_jstring = env.new_string(&signature.0).unwrap();
    let email_jstring = env.new_string(&signature.1).unwrap();

    let pair_class = env.find_class(jni_str!("kotlin/Pair")).unwrap();

    let pair_obj = env
        .new_object(
            &pair_class,
            jni_sig!((JObject, JObject)),
            &[(&name_jstring).into(), (&email_jstring).into()],
        )
        .unwrap();

    Ok(pair_obj)
}
fn push_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    cred: JObject<'local>,
) -> Result<jint, jni::errors::Error> {
    let cred = Cred::from_jni(env, &cred).unwrap();
    unwrap_or_log!(libgit2::push(cred), "push");
    Ok(OK)
}

fn pull_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    cred: JObject<'local>,
    name: JString<'local>,
    email: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let cred = Cred::from_jni(env, &cred).unwrap();
    let name: String = name.try_to_string(env).unwrap();
    let email: String = email.try_to_string(env).unwrap();
    let author = GitAuthor { name, email };
    unwrap_or_log!(libgit2::pull(cred, &author), "pull");
    Ok(OK)
}

fn free_lib<'local>(
    _env: &mut Env<'local>,
    _class: JClass<'local>,
) -> Result<(), jni::errors::Error> {
    Ok(())
}

fn close_repo_lib<'local>(
    _env: &mut Env<'local>,
    _class: JClass<'local>,
) -> Result<(), jni::errors::Error> {
    libgit2::close();
    Ok(())
}
fn is_change_lib<'local>(
    _env: &mut Env<'local>,
    _class: JClass<'local>,
) -> Result<jint, jni::errors::Error> {
    let is_change = unwrap_or_log!(libgit2::is_change(), "is_change");

    Ok(is_change as jint)
}

fn get_timestamps_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    j_map: JObject<'local>,
) -> Result<jint, jni::errors::Error> {
    let timestamps = unwrap_or_log!(libgit2::get_timestamps(), "get_timestamps");

    if let Err(e) = get_timestamps_jni(env, &j_map, timestamps.iter()) {
        error!("get_timestamps_jni: {e}");
        return Ok(-1);
    }

    Ok(OK)
}

fn get_timestamps_jni<'local, 'a>(
    env: &mut Env<'local>,
    j_map: &JObject<'local>,
    timestamps: impl Iterator<Item = (&'a String, &'a i64)>,
) -> Result<(), Box<dyn std::error::Error>> {
    let map_class = env.get_object_class(j_map)?;
    let put_method = env.get_method_id(
        map_class,
        jni_str!("put"),
        jni_sig!((JObject, JObject) -> JObject),
    )?;

    let long_class = env.find_class(jni_str!("java/lang/Long"))?;
    let long_ctor = env.get_method_id(&long_class, jni_str!("<init>"), jni_sig!((jlong)))?;

    for (path, timestamp) in timestamps {
        let j_key: JString = env.new_string(path)?;

        unsafe {
            let j_value = env.new_object_unchecked(
                &long_class,
                long_ctor,
                &[JValue::Long(*timestamp).as_jni()],
            )?;

            env.call_method_unchecked(
                j_map,
                put_method,
                jni::signature::ReturnType::Object,
                &[
                    JValue::Object(&JObject::from(j_key)).as_jni(),
                    JValue::Object(&j_value).as_jni(),
                ],
            )?;
        }
    }

    Ok(())
}

fn generate_ssh_keys_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
) -> Result<JObject<'local>, jni::errors::Error> {
    let keys = match gen_keys() {
        Ok(keys) => keys,
        Err(e) => {
            error!("can't gen keys: {e}");
            return Ok(JObject::null());
        }
    };

    let public_jstring = env.new_string(&keys.public).unwrap();
    let private_jstring = env.new_string(&keys.private).unwrap();

    let pair_class = env.find_class(jni_str!("kotlin/Pair")).unwrap();

    let pair_obj = env
        .new_object(
            &pair_class,
            jni_sig!((JObject, JObject)),
            &[(&public_jstring).into(), (&private_jstring).into()],
        )
        .unwrap();

    Ok(pair_obj)
}

/// Hands over the message behind the last negative return code, and clears it so
/// a later call cannot report an error that has already been shown.
fn last_error_message_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
) -> Result<JObject<'local>, jni::errors::Error> {
    let message = LAST_ERROR
        .lock()
        .ok()
        .and_then(|mut last_error| last_error.take());

    match message {
        Some(message) => Ok(env.new_string(&message)?.into()),
        None => Ok(JObject::null()),
    }
}

fn extension_type_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    extension: JString<'local>,
) -> Result<jint, jni::errors::Error> {
    let extension = extension.try_to_string(env).unwrap();

    let res = match mime_types::extension_type(extension.as_str()) {
        Some(ext_type) => ext_type as jint,
        None => 0,
    };

    Ok(res)
}

fn is_extension_supported_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    extension: JString<'local>,
) -> Result<jboolean, jni::errors::Error> {
    let extension = extension.try_to_string(env).unwrap();

    let res = mime_types::is_extension_supported(extension.as_str());
    Ok(res)
}

fn browser_url_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    url: JString<'local>,
) -> Result<JString<'local>, jni::errors::Error> {
    let url = url.try_to_string(env).unwrap();

    let browser_url = match url::browser_url(&url) {
        Ok(url) => url,
        Err(e) => {
            error!("browser_url: {e}");
            return Ok(JString::null());
        }
    };

    Ok(env
        .new_string(browser_url)
        .expect("Couldn't create Java string!"))
}

fn get_url_info_lib<'local>(
    env: &mut Env<'local>,
    _class: JClass<'local>,
    url: JString<'local>,
) -> Result<JObject<'local>, jni::errors::Error> {
    let url = url.try_to_string(env).unwrap();

    let url_info = match url::parse_url(&url) {
        Ok(info) => info,
        Err(e) => {
            error!("{e}");
            return Ok(JObject::null());
        }
    };

    let is_ssh = url_info.kind == url::UrlKind::Ssh;

    let boolean_class = env.find_class(jni_str!("java/lang/Boolean")).unwrap();

    let obj = env
        .new_object(boolean_class, jni_sig!((jboolean)), &[JValue::Bool(is_ssh)])
        .unwrap();

    Ok(obj)
}
