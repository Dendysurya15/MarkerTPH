package com.cbi.markertph.data.repository

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import com.cbi.markertph.data.database.DatabaseHelper
import com.cbi.markertph.data.database.DatabaseHelper.Companion.DB_ARCHIVE
import com.cbi.markertph.data.database.DatabaseHelper.Companion.DB_TABLE_TPH
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_BLOK
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_BLOK_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ESTATE
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ESTATE_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_LAT
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_LON
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_TANGGAL
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_TPH
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_TPH_ID
import com.cbi.markertph.data.model.TPHModel

class TPHRepository(context: Context) {
    private val databaseHelper: DatabaseHelper = DatabaseHelper(context)

    fun insertTPHRepo(data: TPHModel): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.KEY_TANGGAL, data.tanggal)
            put(DatabaseHelper.KEY_ESTATE, data.estate)
            put(DatabaseHelper.KEY_ESTATE_ID, data.id_estate)
            put(DatabaseHelper.KEY_AFDELING, data.afdeling)
            put(DatabaseHelper.KEY_AFDELING_ID, data.id_afdeling)
            put(DatabaseHelper.KEY_BLOK, data.blok)
            put(DatabaseHelper.KEY_BLOK_ID, data.id_blok)
            put(DatabaseHelper.KEY_TPH, data.tph)
            put(DatabaseHelper.KEY_TPH_ID, data.id_tph)
            put(DatabaseHelper.KEY_LAT, data.latitude)
            put(DatabaseHelper.KEY_LON, data.longitude)
            put(DatabaseHelper.DB_ARCHIVE, data.archive)
        }

        val rowsAffected = db.insert(DatabaseHelper.DB_TABLE_TPH, null, values)
//        db.close()

        return rowsAffected > 0
    }

    @SuppressLint("Range")
    fun fetchAllData(): List<TPHModel> {
        val dataTPH = mutableListOf<TPHModel>()
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $DB_TABLE_TPH WHERE $DB_ARCHIVE = 0", null)

        cursor.use {
            while (it.moveToNext()) {
                val data = TPHModel(
                    id = it.getInt(it.getColumnIndex(KEY_ID)),
                    tanggal = it.getString(it.getColumnIndex(KEY_TANGGAL)),
                    estate = it.getString(it.getColumnIndex(KEY_ESTATE)),
                    id_estate = it.getInt(it.getColumnIndex(KEY_ESTATE_ID)),
                    afdeling = it.getString(it.getColumnIndex(KEY_AFDELING)),
                    id_afdeling = it.getInt(it.getColumnIndex(KEY_AFDELING_ID)),
                    blok = it.getString(it.getColumnIndex(KEY_BLOK)),
                    id_blok = it.getInt(it.getColumnIndex(KEY_BLOK_ID)),
                    tph = it.getString(it.getColumnIndex(KEY_TPH)),
                    id_tph = it.getInt(it.getColumnIndex(KEY_TPH_ID)),
                    latitude = it.getString(it.getColumnIndex(KEY_LAT)),
                    longitude = it.getString(it.getColumnIndex(KEY_LON)),
                    archive =  it.getInt(it.getColumnIndex(DB_ARCHIVE))
                )
                dataTPH.add(data)
            }
        }

        return dataTPH
    }
}