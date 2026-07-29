package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.helper.NameValidation

import kotlin.test.Test

class PlatformTest {


    @Test
    fun name() {
        assert(NameValidation.check("a"))

        assert(!NameValidation.check(" "))
        assert(!NameValidation.check(""))
        assert(!NameValidation.check("/"))
        assert(!NameValidation.check("\\"))

        assert(!NameValidation.check("a/a"))
        assert(!NameValidation.check("a\\a"))
        assert(!NameValidation.check("a\\a\\a"))
        assert(!NameValidation.check("a\\a\\a//\\b\\b/a/"))
        assert(!NameValidation.check("a\\a\\a//\\b\\b/a"))

        assert(!NameValidation.check("aaf\nef"))
    }

}