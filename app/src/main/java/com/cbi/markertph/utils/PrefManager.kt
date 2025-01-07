package com.cbi.markertph.utils

import android.content.Context
import android.content.SharedPreferences

class PrefManager(_context: Context) {
    private var pref: SharedPreferences
    private var editor: SharedPreferences.Editor
    var privateMode = 0


    var version: Int
        get() = pref.getInt(version_tag, 0)
        set(versionCount) {
            editor.putInt(version_tag, versionCount)
            editor.commit()
        }

    var user_input: String?
        get() = pref.getString("user_input", "")
        set(hexDataWl) {
            editor.putString("user_input", hexDataWl)
            editor.commit()
        }

    companion object {
        // Shared preferences file name
        private const val PREF_NAME = "marker_tph"
        private const val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"
        private const val LOGIN = "Login"
        private const val SESSION = "Session"

        const val version_tag = "version"

        const val user_input = "user_input"

    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }
}