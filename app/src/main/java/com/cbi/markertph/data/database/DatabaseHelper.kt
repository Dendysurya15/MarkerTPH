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

        val DB_TABLE_KOORDINAT_TPH = "KoordinatTPH"
        val DB_TABLE_TPH = "TPH"
        val DB_TABLE_COMPANY_CODE = "CompanyCode"
        val DB_TABLE_BUNIT_CODE = "BUnitCode"
        val DB_TABLE_DIVISION_CODE = "DivisionCode"
        val DB_TABLE_FIELD_CODE = "FieldCode"

        //Koordinat TPH
        val KEY_ID = "id"
        val KEY_USER_INPUT = "user_input"
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
        val KEY_APP_VERSION = "app_version"

//        companyCode
        val KEY_COMPANY_CODE = "CompanyCode"
        val KEY_COMPANY_NAME = "CompanyName"

//        divisionCode
        val KEY_DIVISION_CODE = "DivisionCode"
        val KEY_DIVISION_NAME = "DivisionName"
        val KEY_BUNIT_CODE = "BUnitCode"

//        bunitCode
        val KEY_BUNIT_NAME = "BUnitName"

//        fieldCode
        val KEY_FIELD_CODE = "FieldCode"
        val KEY_FIELD_NAME = "FieldName"
        val KEY_FIELD_NUMBER = "FieldNumber"
        val KEY_FIELD_LAND_AREA = "FieldLandArea"
        val KEY_PLANTING_YEAR = "PlantingYear"
        val KEY_INITIAL_NO_OF_PLANTS = "InitialNoOfPlants"
        val KEY_PLANTS_PER_HECTARE = "PlantsPerHectare"
        val KEY_IS_MATURED = "IsMatured"

//        TPH
        val KEY_REGIONAL = "Regional"
        val KEY_PLANTING_YEAR_TPH = "planting_year"
        val KEY_ANCAK = "ancak"
    }

    private val createTableCompanyCode = """
        CREATE TABLE IF NOT EXISTS $DB_TABLE_COMPANY_CODE (
            $KEY_COMPANY_CODE INTEGER,
            $KEY_COMPANY_NAME VARCHAR
        )
        """.trimIndent()

    private val createTableBUnitCode = """
        CREATE TABLE IF NOT EXISTS $DB_TABLE_BUNIT_CODE (
            $KEY_BUNIT_CODE INTEGER,
            $KEY_BUNIT_NAME VARCHAR,
            $KEY_COMPANY_CODE INTEGER
        )
        """.trimIndent()

    private val createTableDivisionCode = """
        CREATE TABLE IF NOT EXISTS $DB_TABLE_DIVISION_CODE (
            $KEY_DIVISION_CODE INTEGER,
            $KEY_DIVISION_NAME VARCHAR,
            $KEY_BUNIT_CODE INTEGER
        )
        """.trimIndent()

    private val createTableFieldCode = """
        CREATE TABLE IF NOT EXISTS $DB_TABLE_FIELD_CODE (
            $KEY_FIELD_CODE INTEGER,
            $KEY_BUNIT_CODE INTEGER,
            $KEY_DIVISION_CODE INTEGER,
            $KEY_FIELD_NAME VARCHAR,
            $KEY_FIELD_NUMBER VARCHAR,
            $KEY_FIELD_LAND_AREA VARCHAR,
            $KEY_PLANTING_YEAR INTEGER,
            $KEY_INITIAL_NO_OF_PLANTS INTEGER,
            $KEY_PLANTS_PER_HECTARE INTEGER,
            $KEY_IS_MATURED VARCHAR
        )
        """.trimIndent()

    private val createTableTPH = """
        CREATE TABLE IF NOT EXISTS $DB_TABLE_TPH (
            $KEY_ID INTEGER,
            $KEY_COMPANY_CODE INTEGER,
            $KEY_REGIONAL INTEGER,
            $KEY_BUNIT_CODE INTEGER,
            $KEY_DIVISION_CODE INTEGER,
            $KEY_FIELD_CODE INTEGER,
            $KEY_PLANTING_YEAR_TPH INTEGER,
            $KEY_ANCAK INTEGER,
            $KEY_TPH VARCHAR
        )
        """.trimIndent()


    private val createTableKoordinatTPH = """
        CREATE TABLE IF NOT EXISTS $DB_TABLE_KOORDINAT_TPH (
            $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $KEY_TANGGAL VARCHAR,
            $KEY_USER_INPUT VARCHAR,
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
            $DB_ARCHIVE INTEGER,
            $KEY_APP_VERSION VARCHAR
        )
        """.trimIndent()

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(createTableKoordinatTPH)
        db?.execSQL(createTableCompanyCode)
        db?.execSQL(createTableBUnitCode)
        db?.execSQL(createTableDivisionCode)
        db?.execSQL(createTableTPH)
        db?.execSQL(createTableFieldCode)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
//        if (oldVersion < 2) {
//            // Add the new archive column
//            db.execSQL("ALTER TABLE $DB_TABLE_TPH ADD COLUMN $DB_ARCHIVE INTEGER DEFAULT 0")
//        }
    }

}