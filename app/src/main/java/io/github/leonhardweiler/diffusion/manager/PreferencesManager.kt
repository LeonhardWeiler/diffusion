package io.github.leonhardweiler.diffusion.manager

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leonhardweiler.diffusion.manager.PreferencesManager.Companion.editor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

abstract class PreferencesManager(private val context: Context, name: String) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = name)
    protected val dataStore get() = context.dataStore

    suspend fun preload() {
        dataStore.data.first()
    }

    protected fun stringPreference(key: String, default: String = "") =
        StringPreference(dataStore, key, default)

    protected fun booleanPreference(key: String, default: Boolean) =
        BooleanPreference(dataStore, key, default)

    protected inline fun <reified E : Enum<E>> enumPreference(
        key: String,
        default: E
    ) = EnumPreference(dataStore, key, default, enumValues())

    companion object {
        suspend inline fun DataStore<Preferences>.editor(crossinline block: EditorContext.() -> Unit) {
            edit {
                EditorContext(it).run(block)
            }
        }
    }
}

class EditorContext(private val prefs: MutablePreferences) {
    var <T> Preference<T>.value
        get() = prefs.run { read() }
        set(value) = prefs.run { write(value) }

    /**
     * Takes the key out of the store rather than writing a default over it.
     *
     * For the preferences that belong to something that can go away — a
     * repository, an ssh key. They are keyed by an id nothing will use again, so
     * a value left behind is one nothing would ever read and nothing would ever
     * clean up.
     */
    fun <T> Preference<T>.forget() = prefs.run { erase() }
}

abstract class Preference<T>(
    private val dataStore: DataStore<Preferences>,
    protected val default: T
) {
    internal abstract fun Preferences.read(): T
    internal abstract fun MutablePreferences.write(value: T)
    internal abstract fun MutablePreferences.erase()

    /**
     * What this says in a snapshot that has already been read, for the callers
     * that read several preferences out of the same one — a whole repository, a
     * whole key — rather than collecting each of them separately.
     */
    internal fun valueIn(preferences: Preferences): T = with(preferences) { read() }

    private val flow = dataStore.data.map { with(it) { read() } ?: default }.distinctUntilChanged()

    suspend fun get() = flow.first()
    fun getBlocking() = runBlocking { get() }

    @Composable
    fun getAsState() = flow.collectAsStateWithLifecycle(initialValue = remember {
        getBlocking()
    })

    suspend fun update(value: T) = dataStore.editor {
        this@Preference.value = value
    }
}

class EnumPreference<E : Enum<E>>(
    dataStore: DataStore<Preferences>,
    key: String,
    default: E,
    private val enumValues: Array<E>
) : Preference<E>(dataStore, default) {
    private val key = stringPreferencesKey(key)
    override fun Preferences.read() =
        this[key]?.let { name ->
            enumValues.find { it.name == name }
        } ?: default

    override fun MutablePreferences.write(value: E) {
        this[key] = value.name
    }

    override fun MutablePreferences.erase() {
        remove(key)
    }
}

abstract class BasePreference<T>(dataStore: DataStore<Preferences>, default: T) :
    Preference<T>(dataStore, default) {
    protected abstract val key: Preferences.Key<T>
    override fun Preferences.read() = this[key] ?: default
    override fun MutablePreferences.write(value: T) {
        this[key] = value
    }

    override fun MutablePreferences.erase() {
        remove(key)
    }
}

class StringPreference(
    dataStore: DataStore<Preferences>,
    key: String,
    default: String
) : BasePreference<String>(dataStore, default) {
    override val key = stringPreferencesKey(key)
}

class BooleanPreference(
    dataStore: DataStore<Preferences>,
    key: String,
    default: Boolean
) : BasePreference<Boolean>(dataStore, default) {
    override val key = booleanPreferencesKey(key)
}


