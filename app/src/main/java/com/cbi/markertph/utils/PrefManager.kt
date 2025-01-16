package com.cbi.markertph.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PrefManager(_context: Context) {
    private var pref: SharedPreferences
    private var editor: SharedPreferences.Editor
    var privateMode = 0

    fun saveFileList(fileList: List<String?>) {
        val json = Gson().toJson(fileList)
        editor.putString("downloaded_file_list", json)
        editor.apply()
    }

    // Retrieve the list of files
    fun getFileList(): List<String?> {
        val json = pref.getString("downloaded_file_list", "[]")
        return try {
            val type = object : TypeToken<List<String?>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Clear the file list
    fun clearFileList() {
        editor.remove("downloaded_file_list").apply()
    }

    var isFirstTimeLaunch: Boolean
        get() = pref.getBoolean("IsFirstTimeLaunch", true)  // Default is true
        set(isFirstTime) {
            editor.putBoolean("IsFirstTimeLaunch", isFirstTime)
            editor.apply()  // Use apply() instead of commit() for async write
        }

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

    var id_selected_estate: Int?
        get() = pref.getInt("id_selected_estate", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_estate", hexDataWl!!)
            editor.commit()
        }

    var id_selected_afdeling: Int?
        get() = pref.getInt("id_selected_afdeling", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_afdeling", hexDataWl!!)
            editor.commit()
        }

    var id_selected_tahun_tanam: String?
        get() = pref.getString("id_selected_tahun_tanam", "")
        set(hexDataWl) {
            editor.putString("id_selected_tahun_tanam", hexDataWl!!)
            editor.commit()
        }

    var id_selected_blok: Int?
        get() = pref.getInt("id_selected_blok", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_blok", hexDataWl!!)
            editor.commit()
        }



    var id_selected_ancak: Int?
        get() = pref.getInt("id_selected_ancak", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_ancak", hexDataWl!!)
            editor.commit()
        }

    var id_selected_tph: Int?
        get() = pref.getInt("id_selected_tph", 0)
        set(hexDataWl) {
            editor.putInt("id_selected_tph", hexDataWl!!)
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
        const val id_selected_estate = "id_selected_estate"
        const val id_selected_afdeling = "id_selected_afdeling"
        const val id_selected_blok = "id_selected_blok"
        const val id_selected_ancak = "id_selected_ancak"
        const val id_selected_tph = "id_selected_tph"
    }

    init {
        pref = _context.getSharedPreferences(PREF_NAME, privateMode)
        editor = pref.edit()
    }
}