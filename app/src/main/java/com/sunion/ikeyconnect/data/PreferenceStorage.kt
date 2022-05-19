package com.sunion.ikeyconnect.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Singleton
class PreferenceStorage @Inject constructor(
    @ApplicationContext appContext: Context
) : PreferenceStore {

    companion object {
        const val PREFS_NAME = "iKey_preference"
        const val KEY_APP_FIRST_LAUNCH = "key_app_first_launch"
        const val KEY_LAST_CONNECTED_MAC = "key_last_connected_mac"
        const val KEY_CURRENT_DEVICE_INDEX = "key_current_device_index"
        const val KEY_IS_ENTER_NOTIFY = "key_is_enter_notify"
        const val KEY_IS_EXIT_NOTIFY = "key_is_exit_notify"

        const val KEY_IS_GUIDE_BUTTON_PRESSED = "is_guide_button_pressed"
        const val KEY_IS_GUIDE_POPUP_MENU_PRESSED = "is_guide_popup_menu_pressed"
    }

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var firstUse by BooleanPreference(prefs, KEY_APP_FIRST_LAUNCH, true)
    override var connectedMacAddress by StringPreference(prefs, KEY_LAST_CONNECTED_MAC, "")
    override var lockCurrentItem by StringPreference(prefs, KEY_CURRENT_DEVICE_INDEX, "0")
    override var isEnterNotify by BooleanPreference(prefs, KEY_IS_ENTER_NOTIFY, false)
    override var isExitNotify by BooleanPreference(prefs, KEY_IS_EXIT_NOTIFY, false)

    override var isGuideButtonPressed by BooleanPreference(prefs, KEY_IS_GUIDE_BUTTON_PRESSED, false)
    override var isGuidePopupMenuPressed by BooleanPreference(prefs, KEY_IS_GUIDE_POPUP_MENU_PRESSED, false)

    class StringPreference(
        private val preferences: SharedPreferences,
        private val prefKey: String,
        private val default: String
    ) : ReadWriteProperty<Any, String> {

        @WorkerThread
        override fun getValue(thisRef: Any, property: KProperty<*>): String =
            preferences.getString(prefKey, default) ?: ""

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
            preferences
                .edit()
                .putString(prefKey, value)
                .apply()
        }
    }

    class BooleanPreference(
        private val preferences: SharedPreferences,
        private val prefKey: String,
        private val default: Boolean
    ) : ReadWriteProperty<Any, Boolean> {

        @WorkerThread
        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return preferences.getBoolean(prefKey, default)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
            preferences
                .edit()
                .putBoolean(prefKey, value)
                .apply()
        }
    }

    override fun checkIfExist(key: String): Boolean {
        return prefs.contains(key)
    }
}
