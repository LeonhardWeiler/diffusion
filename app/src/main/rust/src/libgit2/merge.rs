use git2::{Repository, Signature};

use crate::cred::GitAuthor;
use crate::error::Error;

fn fast_forward(
    repo: &Repository,
    lb: &mut git2::Reference,
    rc: &git2::AnnotatedCommit,
) -> Result<(), git2::Error> {
    let name = match lb.name() {
        Ok(s) => s.to_string(),
        Err(_) => String::from_utf8_lossy(lb.name_bytes()).to_string(),
    };
    let msg = format!("Fast-Forward: Setting {} to id: {}", name, rc.id());
    lb.set_target(rc.id(), &msg)?;
    repo.set_head(&name)?;
    repo.checkout_head(Some(
        git2::build::CheckoutBuilder::default()
            // For some reason the force is required to make the working directory actually get updated
            // I suspect we should be adding some logic to handle dirty working directory states
            // but this is just an example so maybe not.
            .force(),
    ))?;
    Ok(())
}

/// The paths libgit2 could not merge on its own, for the log and the error.
fn conflicting_paths(idx: &git2::Index) -> Vec<String> {
    let Ok(conflicts) = idx.conflicts() else {
        return Vec::new();
    };

    conflicts
        .filter_map(|conflict| conflict.ok())
        .filter_map(|conflict| conflict.our.or(conflict.their).or(conflict.ancestor))
        .map(|entry| String::from_utf8_lossy(&entry.path).into_owned())
        .collect()
}

fn normal_merge(
    repo: &Repository,
    local: &git2::AnnotatedCommit,
    remote: &git2::AnnotatedCommit,
    author: &GitAuthor,
) -> Result<(), Error> {
    // Merges into the index and the working tree together, and leaves
    // MERGE_HEAD behind. Both matter when it does not go through: the notes
    // are where a conflict can be seen, and MERGE_HEAD is what makes the
    // commit that ends the merge a merge.
    let mut checkout = git2::build::CheckoutBuilder::new();
    checkout.force().conflict_style_merge(true);

    repo.merge(&[remote], None, Some(&mut checkout))
        .map_err(|e| Error::git2(e, "merge"))?;

    let mut idx = repo.index()?;

    if idx.has_conflicts() {
        // Each conflicted note now holds both versions between markers, which
        // is something that can be read and fixed in the editor. The merge
        // stays open until that is committed, so finishing it does not throw
        // away where the other side came from and fetch the same conflict for
        // ever after.
        let paths = conflicting_paths(&idx);
        error!("merge conflict in: {}", paths.join(", "));
        return Err(Error::MergeConflict { paths });
    }

    let result_tree = repo.find_tree(idx.write_tree_to(repo)?)?;
    // now create the merge commit
    let msg = format!("Merge: {} into {}", remote.id(), local.id());
    let sig = Signature::now(&author.name, &author.email)?;

    let local_commit = repo.find_commit(local.id())?;
    let remote_commit = repo.find_commit(remote.id())?;
    // Do our merge commit and set current branch head to that commit.
    let _merge_commit = repo.commit(
        Some("HEAD"),
        &sig,
        &sig,
        &msg,
        &result_tree,
        &[&local_commit, &remote_commit],
    )?;

    // Set working tree to match head, and put the merge away: it is over.
    let mut checkout_opts = git2::build::CheckoutBuilder::new();
    checkout_opts.force();
    repo.checkout_head(Some(&mut checkout_opts))?;
    repo.cleanup_state()?;

    Ok(())
}

pub fn do_merge<'a>(
    repo: &'a Repository,
    remote_branch: &str,
    fetch_commit: git2::AnnotatedCommit<'a>,
    author: &GitAuthor,
) -> Result<(), Error> {
    // 1. do a merge analysis
    let analysis = repo
        .merge_analysis(&[&fetch_commit])
        .map_err(|e| Error::git2(e, "merge_analysis"))?;

    // 2. Do the appropriate merge
    if analysis.0.is_fast_forward() {
        // do a fast forward
        let refname = format!("refs/heads/{remote_branch}");
        match repo.find_reference(&refname) {
            Ok(mut r) => {
                fast_forward(repo, &mut r, &fetch_commit)?;
            }
            Err(_) => {
                // The branch doesn't exist so just set the reference to the
                // commit directly. Usually this is because you are pulling
                // into an empty repository.
                repo.reference(
                    &refname,
                    fetch_commit.id(),
                    true,
                    &format!("Setting {} to {}", remote_branch, fetch_commit.id()),
                )
                .map_err(|e| Error::git2(e, "reference"))?;
                repo.set_head(&refname)
                    .map_err(|e| Error::git2(e, "set_head"))?;
                repo.checkout_head(Some(
                    git2::build::CheckoutBuilder::default()
                        .allow_conflicts(true)
                        .conflict_style_merge(true)
                        .force(),
                ))
                .map_err(|e| Error::git2(e, "checkout_head"))?;
            }
        };
    } else if analysis.0.is_normal() {
        // do a normal merge
        let head_commit = repo
            .reference_to_annotated_commit(&repo.head()?)
            .map_err(|e| Error::git2(e, "reference_to_annotated_commit"))?;
        normal_merge(repo, &head_commit, &fetch_commit, author)
            .map_err(|e| e.add_message("normal_merge"))?;
    } else {
        // Nothing to do...
    }
    Ok(())
}
