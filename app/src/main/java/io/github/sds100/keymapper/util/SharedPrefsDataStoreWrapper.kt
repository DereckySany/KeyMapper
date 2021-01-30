package io.github.sds100.keymapper.util

import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.preferencesSetKey
import androidx.preference.PreferenceDataStore
import io.github.sds100.keymapper.data.db.IDataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 19/01/21.
 */
class SharedPrefsDataStoreWrapper(private val scope: CoroutineScope,
                                  private val dataStoreManager: IDataStoreManager) : PreferenceDataStore() {

    override fun getBoolean(key: String, defValue: Boolean) = getFromSharedPrefs(key, defValue)
    override fun putBoolean(key: String, value: Boolean) = setFromSharedPrefs(key, value)

    override fun getString(key: String, defValue: String?) = getFromSharedPrefs(key, defValue)
    override fun putString(key: String, value: String?) = setFromSharedPrefs(key, value)

    override fun getInt(key: String, defValue: Int) = getFromSharedPrefs(key, defValue)
    override fun putInt(key: String, value: Int) = setFromSharedPrefs(key, value)

    override fun getStringSet(key: String, defValues: MutableSet<String>?) =
        getSetFromSharedPrefs(key, defValues)

    override fun putStringSet(key: String, defValues: MutableSet<String>?) =
        setSetFromSharedPrefs(key, defValues)

    private inline fun <reified T> getFromSharedPrefs(key: String, default: T): T {
        return runBlocking {
            dataStoreManager.get(preferencesKey(key)) ?: default
        }
    }

    private inline fun <reified T : Any> setFromSharedPrefs(key: String?, value: T?) {
        key ?: return

        scope.launch {
            dataStoreManager.set(preferencesKey(key), value)
        }
    }

    private inline fun <reified T : Any> getSetFromSharedPrefs(key: String, default: Set<T>?): Set<T> {
        return runBlocking {
            dataStoreManager.get(preferencesSetKey(key)) ?: emptySet()
        }
    }

    private inline fun <reified T : Any> setSetFromSharedPrefs(key: String?, value: Set<T>?) {
        key ?: return

        scope.launch {
            dataStoreManager.set(preferencesSetKey(key), value)
        }
    }
}