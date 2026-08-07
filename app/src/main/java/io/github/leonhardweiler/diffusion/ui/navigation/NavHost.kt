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

@Stable
class Backstack<T : Parcelable> internal constructor(initial: List<T>) {
    private val stack = mutableStateListOf<T>().apply { addAll(initial) }

    val entries: List<T> get() = stack

    val current: T get() = stack.last()

    var wentBack by mutableStateOf(false)
        private set

    fun navigate(destination: T) {
        wentBack = false
        stack.add(destination)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false

        wentBack = true
        stack.removeAt(stack.size - 1)
        return true
    }

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

@Composable
fun <T : Parcelable> NavHost(
    backstack: Backstack<T>,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    transition: (from: T, to: T, wentBack: Boolean) -> ContentTransform,
    content: @Composable (T) -> Unit,
) {
    val savedStates = rememberSaveableStateHolder()
    val stores = remember { mutableMapOf<T, ViewModelStore>() }
    val composed = remember { mutableStateListOf<T>() }

    // off when there is nothing to pop, or back stops closing the app
    BackHandler(enabled = backstack.entries.size > 1 || onBack != null) {
        if (!backstack.pop()) onBack?.invoke()
    }

    // a store goes once its destination is neither on the backstack nor still
    // on screen: during the transition both are drawn, and clearing the
    // outgoing one runs TextVM.onCleared, which writes the note
    LaunchedEffect(backstack.entries.toList(), composed.toList()) {
        val alive = backstack.entries.toSet() + composed.toSet()

        stores.keys.filterNot { it in alive }.toList().forEach { key ->
            stores.remove(key)?.clear()
            savedStates.removeState(key)
        }
    }

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

        // its own store per entry: one store for every screen and the second
        // note opened is handed the first note's TextVM
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
