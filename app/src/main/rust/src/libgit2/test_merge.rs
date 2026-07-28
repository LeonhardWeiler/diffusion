use git2::{Repository, Signature, build::CheckoutBuilder};
use std::path::Path;
use std::time::{Duration, SystemTime};
use std::{fs, io};

use crate::cred::GitAuthor;
use crate::error::Error;
use crate::libgit2::commit_message;
use crate::libgit2::date_pulled_notes;
use crate::libgit2::merge::do_merge;
use crate::libgit2::unresolved_conflicts;

fn clear_dir<P: AsRef<Path>>(path: P) -> io::Result<()> {
    for entry in fs::read_dir(path)? {
        let entry = entry?;
        let path = entry.path();

        if path.is_dir() {
            fs::remove_dir_all(path)?;
        } else {
            fs::remove_file(path)?;
        }
    }
    Ok(())
}

fn signature() -> Signature<'static> {
    Signature::now("Moi", "test@example.com").unwrap()
}

fn switch_to_branch(repo: &Repository, branch_name: &str) {
    let ref_name = format!("refs/heads/{}", branch_name);
    let obj = repo
        .revparse_single(&ref_name)
        .unwrap()
        .peel_to_commit()
        .unwrap();

    let mut opts = CheckoutBuilder::new();
    opts.force();

    repo.checkout_tree(obj.as_object(), Some(&mut opts))
        .unwrap();
    repo.set_head(&ref_name).unwrap();
}

pub fn commit_current_state(repo: &Repository, message: &str) -> git2::Oid {
    let sig = signature();
    let mut index = repo.index().unwrap();
    let tree_id = index.write_tree().unwrap();
    let tree = repo.find_tree(tree_id).unwrap();

    // Récupère le parent actuel (HEAD)
    let parent = repo
        .head()
        .ok()
        .and_then(|h| h.target())
        .and_then(|id| repo.find_commit(id).ok());

    let parents = match &parent {
        Some(c) => vec![c],
        None => vec![],
    };

    repo.commit(Some("HEAD"), &sig, &sig, message, &tree, &parents)
        .unwrap()
}

fn add_file(repo: &Repository, filename: &str, content: &str) {
    let path = repo.workdir().unwrap().join(filename);
    fs::write(path, content).unwrap();

    let mut index = repo.index().unwrap();
    index.add_path(Path::new(filename)).unwrap();
    index.write().unwrap();
}

fn set_modified(repo: &Repository, filename: &str, time: SystemTime) {
    fs::File::options()
        .write(true)
        .open(repo.workdir().unwrap().join(filename))
        .unwrap()
        .set_modified(time)
        .unwrap();
}

fn modified(repo: &Repository, filename: &str) -> SystemTime {
    fs::metadata(repo.workdir().unwrap().join(filename))
        .unwrap()
        .modified()
        .unwrap()
}

fn assert_content(repo: &Repository, path: &str, content: &str) {
    let path = repo.workdir().unwrap().join(path);

    let real_content = fs::read_to_string(&path).unwrap();

    assert_eq!(real_content, content);
}

#[test]
fn test_clean_merge_flow() {
    let path = "repo_test/clean_repo";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    // 1. Premier commit sur Master
    add_file(&repo, "file1.txt", "hello");
    let oid1 = commit_current_state(&repo, "Initial commit on master");

    // 2. Créer et passer sur la branche 'dev'
    let commit1 = repo.find_commit(oid1).unwrap();
    repo.branch("dev", &commit1, false).unwrap();
    switch_to_branch(&repo, "dev");

    // 3. Commit sur 'dev' (file2.txt)
    add_file(&repo, "file2.txt", "hello");
    commit_current_state(&repo, "Add file2 on dev");

    // 4. Retour sur 'master' et commit (file3.txt)
    switch_to_branch(&repo, "master");
    add_file(&repo, "file1.txt", "hello world");
    commit_current_state(&repo, "Modif file1 on master");

    // 5. Merge 'dev' dans 'master'
    let annotated_dev = {
        let dev_ref = repo.find_reference("refs/heads/dev").unwrap();
        repo.reference_to_annotated_commit(&dev_ref).unwrap()
    };

    let author = GitAuthor::from(signature());
    do_merge(&repo, "dev", annotated_dev, &author).expect("Merge failed");

    assert_content(&repo, "file1.txt", "hello world");
    assert_content(&repo, "file2.txt", "hello");
}

#[test]
fn test_clean_merge_flow2() {
    let path = "repo_test/clean_repo2";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    // 1. Premier commit sur Master
    add_file(&repo, "file1.txt", "Contenu Initial");
    let oid1 = commit_current_state(&repo, "Initial commit on master");

    // 2. Créer et passer sur la branche 'dev'
    let commit1 = repo.find_commit(oid1).unwrap();
    repo.branch("dev", &commit1, false).unwrap();
    switch_to_branch(&repo, "dev");

    // 3. Commit sur 'dev' (file2.txt)
    add_file(&repo, "file2.txt", "Contenu Dev");
    commit_current_state(&repo, "Add file2 on dev");

    // 4. Retour sur 'master' et commit (file3.txt)
    switch_to_branch(&repo, "master");
    add_file(&repo, "file3.txt", "Contenu Master");
    commit_current_state(&repo, "Add file3 on master");

    // 5. Merge 'dev' dans 'master'
    let annotated_dev = {
        let dev_ref = repo.find_reference("refs/heads/dev").unwrap();
        repo.reference_to_annotated_commit(&dev_ref).unwrap()
    };

    let author = GitAuthor::from(signature());
    do_merge(&repo, "dev", annotated_dev, &author).expect("Merge failed");

    assert_content(&repo, "file1.txt", "Contenu Initial");
    assert_content(&repo, "file2.txt", "Contenu Dev");
    assert_content(&repo, "file3.txt", "Contenu Master");
}

/// Both sides change the same file. The merge reports the conflict and writes
/// it into the file, where it can be read and fixed, and leaves the merge open
/// so that the commit which ends it keeps both sides.
#[test]
fn test_conflicting_merge_is_reported() {
    let path = "repo_test/conflict_repo";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    add_file(&repo, "file1.txt", "shared line");
    let oid1 = commit_current_state(&repo, "Initial commit on master");

    let commit1 = repo.find_commit(oid1).unwrap();
    repo.branch("dev", &commit1, false).unwrap();
    switch_to_branch(&repo, "dev");

    add_file(&repo, "file1.txt", "changed on dev");
    commit_current_state(&repo, "Modif file1 on dev");

    switch_to_branch(&repo, "master");
    add_file(&repo, "file1.txt", "changed on master");
    commit_current_state(&repo, "Modif file1 on master");

    let annotated_dev = {
        let dev_ref = repo.find_reference("refs/heads/dev").unwrap();
        repo.reference_to_annotated_commit(&dev_ref).unwrap()
    };

    let author = GitAuthor::from(signature());
    let err = do_merge(&repo, "dev", annotated_dev, &author)
        .expect_err("a conflicting merge must not report success");

    match err {
        Error::MergeConflict { paths } => assert_eq!(paths, vec!["file1.txt".to_string()]),
        other => panic!("expected a merge conflict, got {other:?}"),
    }

    // Both versions are in the file, between markers, for someone to choose
    // between in the editor.
    let content = fs::read_to_string(repo.workdir().unwrap().join("file1.txt")).unwrap();
    assert!(
        content.contains("<<<<<<<"),
        "no conflict markers in {content}"
    );
    assert!(content.contains("changed on master"), "ours missing");
    assert!(content.contains("changed on dev"), "theirs missing");

    // The merge is still open, so the commit that ends it can name both sides.
    assert_eq!(repo.state(), git2::RepositoryState::Merge);
}

/// A pull dates the notes it brought in, and only those.
///
/// The sync commits before it pulls, so the note written on this device agrees
/// with HEAD by the time the merge is done — and dating it by its commit would
/// move it to the minute the sync ran, which is how a month of notes ended up
/// all written on the same evening.
#[test]
fn test_pull_dates_only_what_it_wrote() {
    let path = "repo_test/dates_repo";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    add_file(&repo, "here.md", "written on this device");
    let oid1 = commit_current_state(&repo, "Initial commit on master");

    let commit1 = repo.find_commit(oid1).unwrap();
    repo.branch("dev", &commit1, false).unwrap();
    switch_to_branch(&repo, "dev");

    add_file(&repo, "there.md", "written on the other device");
    commit_current_state(&repo, "Add there.md on dev");

    switch_to_branch(&repo, "master");

    // the day the note here was actually written on
    let written_here = SystemTime::UNIX_EPOCH + Duration::from_secs(1_000_000_000);
    set_modified(&repo, "here.md", written_here);

    let before = repo.head().unwrap().peel_to_commit().unwrap().id();

    let annotated_dev = {
        let dev_ref = repo.find_reference("refs/heads/dev").unwrap();
        repo.reference_to_annotated_commit(&dev_ref).unwrap()
    };

    let author = GitAuthor::from(signature());
    do_merge(&repo, "dev", annotated_dev, &author).expect("Merge failed");

    // what the checkout wrote carries the moment it ran, which is what the
    // dating is there to correct
    set_modified(&repo, "there.md", SystemTime::UNIX_EPOCH);

    date_pulled_notes(&repo, Some(before)).unwrap();

    assert_eq!(
        modified(&repo, "here.md"),
        written_here,
        "the pull re-dated a note it never wrote"
    );

    let commit_time = repo
        .head()
        .unwrap()
        .peel_to_commit()
        .unwrap()
        .time()
        .seconds();
    assert_eq!(
        modified(&repo, "there.md"),
        SystemTime::UNIX_EPOCH + Duration::from_secs(commit_time as u64),
        "the note that came in was not dated by the commit that wrote it"
    );
}

/// A conflict that has not been edited down is not something to commit: the
/// markers would go into the history, and with the sync running by itself that
/// would take no tap at all.
#[test]
fn test_conflict_markers_are_seen_until_they_are_gone() {
    let path = "repo_test/markers_repo";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    add_file(&repo, "note.md", "shared line");
    let oid1 = commit_current_state(&repo, "Initial commit on master");

    let commit1 = repo.find_commit(oid1).unwrap();
    repo.branch("dev", &commit1, false).unwrap();
    switch_to_branch(&repo, "dev");

    add_file(&repo, "note.md", "changed on dev");
    commit_current_state(&repo, "Modif note on dev");

    switch_to_branch(&repo, "master");
    add_file(&repo, "note.md", "changed on master");
    commit_current_state(&repo, "Modif note on master");

    let annotated_dev = {
        let dev_ref = repo.find_reference("refs/heads/dev").unwrap();
        repo.reference_to_annotated_commit(&dev_ref).unwrap()
    };

    let author = GitAuthor::from(signature());
    do_merge(&repo, "dev", annotated_dev, &author).expect_err("the merge must not go through");

    let index = repo.index().unwrap();
    assert_eq!(
        unresolved_conflicts(&repo, &index),
        vec!["note.md".to_string()],
        "the note with the markers in it was not noticed"
    );

    // edited down to the version to keep, which is what ends the merge
    fs::write(repo.workdir().unwrap().join("note.md"), "changed on master").unwrap();

    assert!(
        unresolved_conflicts(&repo, &index).is_empty(),
        "a note that was edited still counted as conflicted"
    );
}

/// Removes a file from the working tree and stages that, which is what
/// deleting a note ends up as.
fn remove_file(repo: &Repository, filename: &str) {
    fs::remove_file(repo.workdir().unwrap().join(filename)).unwrap();

    let mut index = repo.index().unwrap();
    index.remove_path(Path::new(filename)).unwrap();
    index.write().unwrap();
}

#[test]
fn test_commit_message_names_the_notes() {
    let path = "repo_test/message_repo";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    add_file(&repo, "kept.md", "one");
    add_file(&repo, "gone.md", "two");
    commit_current_state(&repo, "Initial commit");

    add_file(&repo, "kept.md", "one, edited");
    add_file(&repo, "fresh.md", "three");
    remove_file(&repo, "gone.md");

    let index = repo.index().unwrap();
    assert_eq!(
        commit_message(&repo, &index, "fallback"),
        "[fresh.md] added, [kept.md] changed, [gone.md] deleted"
    );
}

#[test]
fn test_commit_message_counts_what_it_leaves_out() {
    let path = "repo_test/message_many_repo";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    add_file(&repo, "a.md", "a");
    commit_current_state(&repo, "Initial commit");

    for name in ["b.md", "c.md", "d.md", "e.md", "f.md"] {
        add_file(&repo, name, name);
    }

    let index = repo.index().unwrap();
    let message = commit_message(&repo, &index, "fallback");

    let (subject, body) = message.split_once("\n\n").expect("a body was expected");

    assert_eq!(subject, "[b.md, c.md, d.md and 2 more] added");
    assert_eq!(body, "added:\n  b.md\n  c.md\n  d.md\n  e.md\n  f.md");
}

#[test]
fn test_commit_message_falls_back_when_nothing_changed() {
    let path = "repo_test/message_empty_repo";
    let _ = clear_dir(path);
    let repo = Repository::init(path).unwrap();

    add_file(&repo, "a.md", "a");
    commit_current_state(&repo, "Initial commit");

    let index = repo.index().unwrap();
    assert_eq!(commit_message(&repo, &index, "fallback"), "fallback");
}
