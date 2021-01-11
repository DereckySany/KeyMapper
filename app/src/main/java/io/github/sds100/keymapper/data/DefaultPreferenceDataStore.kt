package io.github.sds100.keymapper.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.preferences.createDataStore
import io.github.sds100.keymapper.util.defaultSharedPreferences
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 20/02/2020.
 */

class DefaultPreferenceDataStore(ctx: Context) : IPreferenceDataStore {

    private val ctx = ctx.applicationContext

    private val prefs: SharedPreferences
        get() = ctx.defaultSharedPreferences

    override val fingerprintGestureDataStore = ctx.createDataStore("fingerprint_gestures")

    override fun getBoolPref(key: Int): Boolean {
        return prefs.getBoolean(ctx.str(key), false)
    }

    override fun setBoolPref(key: Int, value: Boolean) {
        prefs.edit {
            putBoolean(ctx.str(key), value)
        }
    }

    override fun getStringPref(key: Int): String? {
        return prefs.getString(ctx.str(key), null)
    }

    override fun setStringPref(key: Int, value: String) {
        prefs.edit {
            putString(ctx.str(key), value)
        }
    }
}