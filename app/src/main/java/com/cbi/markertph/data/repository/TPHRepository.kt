package com.cbi.markertph.data.repository

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cbi.markertph.R
import com.cbi.markertph.data.database.DatabaseHelper
import com.cbi.markertph.data.database.DatabaseHelper.Companion.DB_ARCHIVE
import com.cbi.markertph.data.database.DatabaseHelper.Companion.DB_TABLE_KOORDINAT_TPH
import com.cbi.markertph.data.database.DatabaseHelper.Companion.DB_TABLE_TPH
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_APP_VERSION
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
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_USER_INPUT
import com.cbi.markertph.data.model.BUnitCodeModel
import com.cbi.markertph.data.model.BatchUploadRequest
import com.cbi.markertph.data.model.CompanyCodeModel
import com.cbi.markertph.data.model.DivisionCodeModel
import com.cbi.markertph.data.model.FieldCodeModel
import com.cbi.markertph.data.model.KoordinatTPHModel
import com.cbi.markertph.data.model.TPHModel
import com.cbi.markertph.data.model.UploadData
import com.cbi.markertph.data.model.UploadResponse
import com.cbi.markertph.data.network.RetrofitClient
import com.cbi.markertph.utils.AlertDialogUtility
import com.cbi.markertph.utils.AppUtils.stringXML
import com.google.gson.Gson
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.zip.GZIPOutputStream

class TPHRepository(context: Context) {
    private val databaseHelper: DatabaseHelper = DatabaseHelper(context)

    fun insertCompanyCodeRepo(data: CompanyCodeModel): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.KEY_COMPANY_CODE, data.CompanyCode)
            put(DatabaseHelper.KEY_COMPANY_NAME, data.CompanyName)
        }

        val rowsAffected = db.insert(DatabaseHelper.DB_TABLE_COMPANY_CODE, null, values)
        return rowsAffected > 0
    }

    fun insertBUnitCodeRepo(data: BUnitCodeModel): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.KEY_BUNIT_CODE, data.BUnitCode)
            put(DatabaseHelper.KEY_BUNIT_NAME, data.BUnitName)
            put(DatabaseHelper.KEY_COMPANY_CODE, data.CompanyCode)
        }
        val rowsAffected = db.insert(DatabaseHelper.DB_TABLE_BUNIT_CODE, null, values)
        return rowsAffected > 0
    }

    fun insertDivisionCodeRepo(data: DivisionCodeModel): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.KEY_DIVISION_CODE, data.DivisionCode)
            put(DatabaseHelper.KEY_DIVISION_NAME, data.DivisionName)
            put(DatabaseHelper.KEY_BUNIT_CODE, data.BUnitCode)
        }
        val rowsAffected = db.insert(DatabaseHelper.DB_TABLE_DIVISION_CODE, null, values)
        return rowsAffected > 0
    }

    fun insertFieldCodeRepo(data: FieldCodeModel): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.KEY_FIELD_CODE, data.FieldCode)
            put(DatabaseHelper.KEY_BUNIT_CODE, data.BUnitCode)
            put(DatabaseHelper.KEY_DIVISION_CODE, data.DivisionCode)
            put(DatabaseHelper.KEY_FIELD_NAME, data.FieldName)
            put(DatabaseHelper.KEY_FIELD_NUMBER, data.FieldNumber)
            put(DatabaseHelper.KEY_FIELD_LAND_AREA, data.FieldLandArea)
            put(DatabaseHelper.KEY_PLANTING_YEAR, data.PlantingYear)
            put(DatabaseHelper.KEY_INITIAL_NO_OF_PLANTS, data.IntialNoOfPlants)
            put(DatabaseHelper.KEY_PLANTS_PER_HECTARE, data.PlantsPerHectare)
            put(DatabaseHelper.KEY_IS_MATURED, data.isMatured)
        }
        val rowsAffected = db.insert(DatabaseHelper.DB_TABLE_FIELD_CODE, null, values)
        return rowsAffected > 0
    }

    fun insertTPHBatch(dataList: List<TPHModel>): Boolean {
        var db: SQLiteDatabase? = null
        try {

            db = databaseHelper.writableDatabase

            db.beginTransaction()
            try {
                // Use batch size of 1000 records at a time
                val batchSize = 1000
                dataList.chunked(batchSize).forEachIndexed { index, batch ->
                    batch.forEach { data ->
                        val values = ContentValues().apply {
                            put(DatabaseHelper.KEY_ID, data.id)
                            put(DatabaseHelper.KEY_COMPANY_CODE, data.CompanyCode)
                            put(DatabaseHelper.KEY_REGIONAL, data.Regional)
                            put(DatabaseHelper.KEY_BUNIT_CODE, data.BUnitCode)
                            put(DatabaseHelper.KEY_DIVISION_CODE, data.DivisionCode)
                            put(DatabaseHelper.KEY_FIELD_CODE, data.FieldCode)
                            put(DatabaseHelper.KEY_PLANTING_YEAR_TPH, data.planting_year)
                            put(DatabaseHelper.KEY_ANCAK, data.ancak)
                            put(DatabaseHelper.KEY_TPH, data.tph)
                        }
                        db.insert(DatabaseHelper.DB_TABLE_TPH, null, values)
                    }
                    Log.d("TPHRepo", "Inserted batch ${index + 1} (${batch.size} records)")
                }

                db.setTransactionSuccessful()
                Log.d("TPHRepo", "Batch insertion completed successfully")
                return true
            } catch (e: Exception) {
                Log.e("TPHRepo", "Error during batch insertion: ${e.message}")
                e.printStackTrace()
                return false
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("TPHRepo", "Critical error in batch insertion: ${e.message}")
            e.printStackTrace()
            return false
        } finally {
            db?.close()
        }
    }

    fun getCompanyCodeCount(): Int {
        val db = databaseHelper.readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.DB_TABLE_COMPANY_CODE}", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun getBUnitCodeCount(): Int {
        val db = databaseHelper.readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.DB_TABLE_BUNIT_CODE}", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun getDivisionCodeCount(): Int {
        val db = databaseHelper.readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.DB_TABLE_DIVISION_CODE}", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun getFieldCodeCount(): Int {
        val db = databaseHelper.readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.DB_TABLE_FIELD_CODE}", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun getTPHCount(): Int {
        val db = databaseHelper.readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.DB_TABLE_TPH}", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun insertKoordinatTPHRepo(data: KoordinatTPHModel): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.KEY_TANGGAL, data.tanggal)
            put(DatabaseHelper.KEY_USER_INPUT, data.user_input)
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
            put(DatabaseHelper.KEY_APP_VERSION, data.app_version)
        }

        val rowsAffected = db.insert(DatabaseHelper.DB_TABLE_KOORDINAT_TPH, null, values)
//        db.close()

        return rowsAffected > 0
    }

    @SuppressLint("Range")
    fun fetchAllData(archive: Int = 0): List<KoordinatTPHModel> {
        val dataTPH = mutableListOf<KoordinatTPHModel>()
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $DB_TABLE_KOORDINAT_TPH WHERE $DB_ARCHIVE = ?",
            arrayOf(archive.toString()))

        cursor.use {
            while (it.moveToNext()) {
                val data = KoordinatTPHModel(
                    id = it.getInt(it.getColumnIndexOrThrow(KEY_ID)),
                    tanggal = it.getString(it.getColumnIndexOrThrow(KEY_TANGGAL)) ?: "",
                    estate = it.getString(it.getColumnIndexOrThrow(KEY_ESTATE)) ?: "",
                    user_input = it.getString(it.getColumnIndexOrThrow(KEY_USER_INPUT)) ?: "",
                    id_estate = it.getInt(it.getColumnIndexOrThrow(KEY_ESTATE_ID)),
                    afdeling = it.getString(it.getColumnIndexOrThrow(KEY_AFDELING)) ?: "",
                    id_afdeling = it.getInt(it.getColumnIndexOrThrow(KEY_AFDELING_ID)),
                    blok = it.getString(it.getColumnIndexOrThrow(KEY_BLOK)) ?: "",
                    id_blok = it.getInt(it.getColumnIndexOrThrow(KEY_BLOK_ID)),
                    tph = it.getString(it.getColumnIndexOrThrow(KEY_TPH)) ?: "",
                    id_tph = it.getInt(it.getColumnIndexOrThrow(KEY_TPH_ID)),
                    latitude = it.getString(it.getColumnIndexOrThrow(KEY_LAT)) ?: "",
                    longitude = it.getString(it.getColumnIndexOrThrow(KEY_LON)) ?: "",
                    archive = it.getInt(it.getColumnIndexOrThrow(DB_ARCHIVE)),
                    app_version = it.getString(it.getColumnIndexOrThrow(KEY_APP_VERSION)) ?: ""
                )
                dataTPH.add(data)
            }
        }

        return dataTPH
    }

    fun deleteMultipleItems(ids: List<String>): Boolean {
        val db = databaseHelper.writableDatabase
        var success = true

        try {
            db.beginTransaction()

            ids.forEach { id ->
                val rowsAffected = db.delete(DatabaseHelper.DB_TABLE_KOORDINAT_TPH, "id=?", arrayOf(id))
                if (rowsAffected <= 0) {
                    success = false
                }
            }

            if (success) {
                db.setTransactionSuccessful()
            }
        } catch (e: Exception) {
            success = false
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }

        return success
    }

    private val apiService = RetrofitClient.instance

    companion object {
        const val RETRY_DELAY_MS = 6000L // Retry delay of 6 seconds
        const val TIMEOUT_MS = 30000L   // Timeout of 1 minute
        private const val TAG = "UploadRepository" // Tag for logging
    }

    // Repository function
    fun uploadDataServer(context: Context, dataList: List<UploadData>): LiveData<Result<UploadResponse>> {
        val result = MutableLiveData<Result<UploadResponse>>()

        try {
            Log.d(TAG, "Starting upload process for ${dataList.size} items")

            // Convert data list to JSON string
            val gson = Gson()
            val jsonString = try {
                gson.toJson(dataList)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to convert data to JSON: ${e.message}", e)
                handleFailure(context, result, "Error converting data to JSON: ${e.message}")
                return result
            }

            Log.d(TAG, "Successfully converted data to JSON")

            // Compress using GZIP
            val byteStream = ByteArrayOutputStream()
            try {
                GZIPOutputStream(byteStream).use { gzipStream ->
                    gzipStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to compress data: ${e.message}", e)
                handleFailure(context, result, "Error compressing data: ${e.message}")
                return result
            }

            Log.d(TAG, "Successfully compressed data")

            // Convert to Base64
            val base64Data = try {
                Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to encode data to Base64: ${e.message}", e)
                handleFailure(context, result, "Error encoding data: ${e.message}")
                return result
            }

            Log.d(TAG, "Successfully encoded data to Base64")

            // Create request
            val request = BatchUploadRequest(base64Data)

            // Make API call
            val call = apiService.uploadData(request)
            Log.d(TAG, "Initiating API call")

            call.enqueue(object : Callback<UploadResponse> {
                override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                    Log.d(TAG, "Received API response. Status code: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val uploadResponse = response.body()!!
                        Log.d(TAG, "Response successful. Success flag: ${uploadResponse.statusCode}")

                        if (response.isSuccessful && response.body() != null) {
                            val uploadResponse = response.body()!!
                            Log.d(TAG, "Response successful. Status code: ${uploadResponse.statusCode}")

                            when (uploadResponse.statusCode) {
                                2 -> {  // All duplicated
                                    Log.d(TAG, "All records are duplicates")

                                    AlertDialogUtility.withSingleAction(
                                        context,
                                        context.stringXML(R.string.al_back),
                                        context.stringXML(R.string.al_data_duplicate),
                                        "${dataList.size} ${context.stringXML(R.string.al_data_duplicate_description)}",
                                        "warning.json",
                                        R.color.orange
                                    ) {
                                    }
                                    Toast.makeText(context, "${dataList.size} ${context.stringXML(R.string.al_data_duplicate_description)}", Toast.LENGTH_LONG).show()
                                    result.postValue(Result.success(uploadResponse))
                                }
                                1 -> {
                                    try {
                                        val responseData = uploadResponse.data as? Map<*, *>
                                        val storedData = responseData?.get("stored") as? List<*>
                                        val duplicateCount = dataList.size - (storedData?.size ?: 0)


                                        var successfulUpdates = 0
                                        dataList.forEach { data ->
                                            val wasStored = storedData?.any { stored ->
                                                (stored as? Map<*, *>)?.let { map ->
                                                    map["datetime"] == data.datetime &&
                                                            map["user_input"] == data.user_input &&
                                                    map["estate"] == data.estate &&
                                                    map["afdeling"] == data.afdeling &&
                                                    map["blok"] == data.blok &&
                                                    map["tph"] == data.tph
                                                } ?: false
                                            } ?: false

                                            if (wasStored) {
                                                updateArchiveStatus(data.id, 1)
                                                successfulUpdates++
                                                Log.d(TAG, "Updated archive status for ID: ${data.id}")
                                            } else {
                                                Log.d(TAG, "Skipped archive status update for ID: ${data.id}")
                                            }
                                        }

                                        // Show appropriate message based on what happened
                                        if (duplicateCount > 0) {
                                            // If there are both new and duplicate records, show a dialog that requires acknowledgment
                                            AlertDialogUtility.withSingleAction(
                                                context,
                                                context.stringXML(R.string.al_back),
                                                context.stringXML(R.string.al_success_upload),
                                                "${context.stringXML(R.string.al_success_upload_description)} ${successfulUpdates} data\n" +
                                                        "${duplicateCount} ${context.stringXML(R.string.al_data_duplicate_description)}",
                                                "success.json",
                                                R.color.orange
                                            ) {

                                            }
                                        } else {
                                            // If all records are new and successful, show auto-dismissing success dialog
                                            AlertDialogUtility.alertDialogAction(
                                                context,
                                                context.stringXML(R.string.al_success_upload),
                                                "${context.stringXML(R.string.al_success_upload_description)} ${successfulUpdates} data!",
                                                "success.json"
                                            ) {
                                                Log.d(TAG, "Success dialog auto-dismissed")
                                            }
                                        }

                                        result.postValue(Result.success(uploadResponse))
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error updating archive status: ${e.message}", e)
                                        result.postValue(Result.failure(e))
                                    }
                                }
                                else -> {  // Failed (0 or any other value)
                                    Log.e(TAG, "Upload failed with message: ${uploadResponse.message}")
                                    handleFailure(context, result, uploadResponse.message)
                                }
                            }
                        } else {
                            Log.e(TAG, "Upload failed with message: ${uploadResponse.message}")
                            handleFailure(context, result, uploadResponse.message)
                        }
                    } else {
                        try {
                            val errorJson = response.errorBody()?.string()
                            Log.e(TAG, "Error response body: $errorJson")

                            val errorObj = JSONObject(errorJson!!)
                            val errorMessage = errorObj.getString("message")
                            Log.e(TAG, "Parsed error message: $errorMessage")

                            handleFailure(context, result, errorMessage)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse error response: ${e.message}", e)
                            handleFailure(context, result, "Error uploading data: Unknown error")
                        }
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Log.e(TAG, "Network call failed", t)
                    handleFailure(context, result, "Network failure: ${t.message}")
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during upload process", e)
            handleFailure(context, result, "Error preparing data: ${e.message}")
        }

        return result
    }

    private fun handleFailure(context: Context, result: MutableLiveData<Result<UploadResponse>>, errorMessage: String) {
        Log.e(TAG, "Handling failure: $errorMessage")

        AlertDialogUtility.withSingleAction(
            context,
            context.stringXML(R.string.al_back),
            context.stringXML(R.string.al_failed_upload),
            "${context.stringXML(R.string.al_failed_upload_description)}: $errorMessage",
            "warning.json",
            R.color.colorRedDark
        ) {

        }

        result.postValue(Result.failure(Exception(errorMessage)))
    }




    fun updateArchiveStatus(id: Int, status: Int) {
        val db = databaseHelper.readableDatabase
        val values = ContentValues().apply {
            put(DB_ARCHIVE, status)  // Using the constant for archive column
        }

        db.update(
            DB_TABLE_KOORDINAT_TPH,           // Using the constant for table name
            values,
            "$KEY_ID = ?",          // Using the constant for ID column
            arrayOf(id.toString())
        )
    }

}