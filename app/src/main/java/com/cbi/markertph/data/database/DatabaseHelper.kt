package com.cbi.markertph.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "cbi_cmp_tph"
        const val DATABASE_VERSION = 2
        const val DB_ARCHIVE = "archive"

        val DB_TABLE_TPH = "db_tph"
        val KEY_ID = "id"
        val KEY_ESTATE = "estate"
        val KEY_ESTATE_ID = "id_estate"
        val KEY_TANGGAL = "tanggal"
        val KEY_AFDELING = "afdeling"
        val KEY_AFDELING_ID = "id_afdeling"
        val KEY_BLOK = "blok"
        val KEY_BLOK_ID = "id_blok"
        val KEY_TPH = "tph"
        val KEY_TPH_ID = "id_tph"
        val KEY_LAT = "latitude"
        val KEY_LON = "longitude"


    }

    private val createTableTPH = """
        CREATE TABLE IF NOT EXISTS $DB_TABLE_TPH (
            $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $KEY_TANGGAL VARCHAR,
            $KEY_ESTATE VARCHAR,
            $KEY_ESTATE_ID INTEGER,
           $KEY_AFDELING VARCHAR,
           $KEY_AFDELING_ID INTEGER,
           $KEY_BLOK VARCHAR,
            $KEY_BLOK_ID INTEGER,
           $KEY_TPH VARCHAR,
           $KEY_TPH_ID INTEGER,
            $KEY_LAT VARCHAR,
            $KEY_LON VARCHAR,
            $DB_ARCHIVE INTEGER
        
        )
        """.trimIndent()

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createTableTPH)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add the new archive column
            db.execSQL("ALTER TABLE $DB_TABLE_TPH ADD COLUMN $DB_ARCHIVE INTEGER DEFAULT 0")
        }
    }

}