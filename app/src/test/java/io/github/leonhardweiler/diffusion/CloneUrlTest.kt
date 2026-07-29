package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.helper.CloneUrlKind
import io.github.leonhardweiler.diffusion.helper.cloneUrlKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CloneUrlTest {

    @Test
    fun sshUrlsAreRecognisedInBothShapes() {
        val ssh = listOf(
            "ssh://username@host:5555/dir/repo.git",
            "git@github.com:LeonhardWeiler/diffusion.git",
            "git@git.sr.ht:~user/notes",
            "ssh://name@9.9.9.9:111/name/name.git",
            "name@git.dom.hu:111/name/name.git",
            "ssh://name@git.dom.hu:111/name/name.git",
            "name@git.dom.hu:/name/name.git",
            "name@git.dom.hu:repos/name.git",
        )

        for (url in ssh) {
            assertEquals(CloneUrlKind.Ssh, cloneUrlKind(url), url)
        }
    }

    @Test
    fun theWebTransportsAreToldApartFromSsh() {
        assertEquals(
            CloneUrlKind.Https,
            cloneUrlKind("https://github.com/LeonhardWeiler/diffusion.git")
        )
        assertEquals(CloneUrlKind.Http, cloneUrlKind("http://git.example.com/notes.git"))
    }

    @Test
    fun aFolderOnTheDeviceIsNotACloneUrl() {
        assertNull(cloneUrlKind("/storage/emulated/0/notes"))
        assertNull(cloneUrlKind("./notes"))
        assertNull(cloneUrlKind("~/notes"))
        assertNull(cloneUrlKind("notes"))
    }

    @Test
    fun nothingIsNotACloneUrl() {
        assertNull(cloneUrlKind(""))
        assertNull(cloneUrlKind("   "))
    }

    @Test
    fun aTransportThisAppHasNoneOfIsRefused() {
        assertNull(cloneUrlKind("git://github.com/LeonhardWeiler/diffusion.git"))
        assertNull(cloneUrlKind("file:///tmp/notes"))
    }

    @Test
    fun anAddressWithoutAHostIsNotOne() {
        assertNull(cloneUrlKind("ssh:///dir/repo.git"))
        assertNull(cloneUrlKind("git@:notes.git"))
    }

    @Test
    fun surroundingWhitespaceDoesNotMatter() {
        assertEquals(CloneUrlKind.Ssh, cloneUrlKind("  git@github.com:owner/repo.git\n"))
    }
}
