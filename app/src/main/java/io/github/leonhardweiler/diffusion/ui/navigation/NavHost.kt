package io.github.leonhardweiler.diffusion.ui.navigation

import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * The screens the app is standing on, the last one being the one shown.
 *
 * Destinations are parcelable, so this survives the process dying: it is held in
 * [rememberSaveable] and Android writes it out with the rest of the saved state.
 * Everything else about a screen — where its list was scrolled, what its view
 * model holds — hangs off the destination as a key, which is why two entries
 * must never be equal.
 */
@Stable
class Backstack<T : Parcelable> internal constructor(initial: List<T>) {

    private val stack = mutableStateListOf<T>().apply { addAll(initial) }

    val entries: List<T> get() = stack

    val current: T get() = stack.last()

    /**
     * Whether the last thing that happened was going back, which is all the
     * animation needs to know to run the other way.
     */
    var wentBack by mutableStateOf(false)
        private set

    fun navigate(destination: T) {
        wentBack = false
        stack.add(destination)
    }

    /** False when there is nothing to go back to, so the caller can decide. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false

        wentBack = true
        stack.removeAt(stack.size - 1)
        return true
    }

    /** Leaves one screen standing, whatever was there before. */
    fun replaceAll(destination: T) {
        wentBack = false
        stack.clear()
        stack.add(destination)
    }

    internal companion object {
        fun <T : Parcelable> saver() = listSaver<Backstack<T>, T>(
            save = { it.entries.toList() },
            restore = { Backstack(it) }
        )
    }
}

@Composable
fun <T : Parcelable> rememberBackstack(vararg initial: T): Backstack<T> =
    rememberSaveable(saver = Backstack.saver()) { Backstack(initial.toList()) }

/**
 * Shows the top of [backstack], and gives each screen a place of its own to keep
 * what it has.
 *
 * There are three things a screen expects to survive being navigated away from
 * and come back to, and each of them is keyed by the destination:
 *
 * Its compose state, through a [rememberSaveableStateHolder] — the note list is
 * where it was scrolled to after the editor is closed.
 *
 * Its view models, through a [ViewModelStore] of its own. Without that, one
 * store would serve every screen and the editor's view model, which is built
 * from the note it was opened with, would be handed straight back for the next
 * note — the factory is only asked when there is none.
 *
 * And going back, through [BackHandler], which is on for as long as there is
 * something below — or an [onBack] that means something.
 *
 * What a screen is done with is let go of after the animation, not when the
 * backstack changes: both screens are on the way through it, and clearing the
 * one that is leaving would run its view model's onCleared while it is still
 * being drawn — which for the editor means writing the note it still holds.
 */
@Composable
fun <T : Parcelable> NavHost(
    backstack: Backstack<T>,
    modifier: Modifier = Modifier,
    /**
     * What going back means when there is nothing left on the backstack. Null
     * where that is the system's business — leaving the app — so that back at
     * the first screen still closes it rather than doing nothing.
     */
    onBack: (() -> Unit)? = null,
    transition: (from: T, to: T, wentBack: Boolean) -> ContentTransform,
    content: @Composable (T) -> Unit,
) {
    val savedStates = rememberSaveableStateHolder()
    val stores = remember { mutableMapOf<T, ViewModelStore>() }
    val composed = remember { mutableStateListOf<T>() }

    BackHandler(enabled = backstack.entries.size > 1 || onBack != null) {
        if (!backstack.pop()) onBack?.invoke()
    }

    // Everything that is neither on the backstack nor still on screen. After
    // the composition, so that a screen leaving has already handed its state
    // over and removeState is not undone by it.
    LaunchedEffect(backstack.entries.toList(), composed.toList()) {
        val alive = backstack.entries.toSet() + composed.toSet()

        stores.keys.filterNot { it in alive }.toList().forEach { key ->
            stores.remove(key)?.clear()
            savedStates.removeState(key)
        }
    }

    // the whole host going away takes every view model with it, the way an
    // activity being finished does
    DisposableEffect(Unit) {
        onDispose {
            stores.values.forEach { it.clear() }
            stores.clear()
        }
    }

    AnimatedContent(
        targetState = backstack.current,
        modifier = modifier,
        transitionSpec = { transition(initialState, targetState, backstack.wentBack) },
        label = "navigation",
    ) { destination ->

        DisposableEffect(destination) {
            composed.add(destination)
            onDispose { composed.remove(destination) }
        }

        val owner = remember(destination) {
            val store = stores.getOrPut(destination) { ViewModelStore() }
            object : ViewModelStoreOwner {
                override val viewModelStore = store
            }
        }

        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
            savedStates.SaveableStateProvider(destination) {
                content(destination)
            }
        }
    }
}
