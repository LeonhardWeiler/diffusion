package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.helper.repoWebUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepoWebUrlTest {

    @Test
    fun the_scp_form_is_the_one_a_provider_offers() {
        assertEquals(
            "https://github.com/LeonhardWeiler/diffusion",
            repoWebUrl("git@github.com:LeonhardWeiler/diffusion.git")
        )
    }

    @Test
    fun the_git_suffix_is_optional() {
        assertEquals(
            "https://github.com/LeonhardWeiler/diffusion",
            repoWebUrl("git@github.com:LeonhardWeiler/diffusion")
        )
    }

    @Test
    fun an_ssh_url_loses_its_user_and_its_port() {
        assertEquals(
            "https://git.example.org/notes/private",
            repoWebUrl("ssh://git@git.example.org:2222/notes/private.git")
        )
    }

    @Test
    fun an_https_url_is_already_the_page() {
        assertEquals(
            "https://gitlab.com/group/sub/repo",
            repoWebUrl("https://gitlab.com/group/sub/repo.git")
        )
    }

    @Test
    fun surrounding_whitespace_does_not_count() {
        assertEquals(
            "https://github.com/a/b",
            repoWebUrl("  git@github.com:a/b.git\n")
        )
    }

    @Test
    fun what_has_no_repository_behind_it_has_no_page() {
        assertNull(repoWebUrl(""))
        // a folder on the device
        assertNull(repoWebUrl("/storage/emulated/0/notes"))
        // a host and nothing else
        assertNull(repoWebUrl("git@github.com:"))
        assertNull(repoWebUrl("ssh://git@github.com/"))
    }
}
