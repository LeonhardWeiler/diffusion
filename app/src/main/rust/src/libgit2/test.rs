use super::*;

/// What [commit_timestamps] found, oldest first, for reading by eye.
fn timestamps_of(repo_path: &str) -> Vec<(String, i64)> {
    open_repo(repo_path).unwrap();

    let repo = REPO.lock().expect("repo lock");
    let repo = repo.as_ref().expect("repo");

    let mut timestamps = commit_timestamps(repo)
        .unwrap()
        .into_iter()
        .collect::<Vec<_>>();

    timestamps.sort_by_key(|(_, time)| *time);
    timestamps
}

#[test]
#[ignore = "local repo"]
fn timestamp() {
    dbg!(timestamps_of("../../../../../repo_test"));
}

#[test]
#[ignore = "local repo"]
fn timestamp2() {
    dbg!(timestamps_of("../../../../../note-pv"));
}
