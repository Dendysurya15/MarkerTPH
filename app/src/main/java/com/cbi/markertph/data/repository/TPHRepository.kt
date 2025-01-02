package com.cbi.markertph.data.repository

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cbi.markertph.R
import com.cbi.markertph.data.database.DatabaseHelper
import com.cbi.markertph.data.database.DatabaseHelper.Companion.DB_ARCHIVE
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
import com.cbi.markertph.data.model.TPHModel
import com.cbi.markertph.data.model.UploadData
import com.cbi.markertph.data.model.UploadResponse
import com.cbi.markertph.data.network.RetrofitClient
import com.cbi.markertph.utils.AlertDialogUtility
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors

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
            put(DatabaseHelper.KEY_APP_VERSION, data.app_version)
        }

        val rowsAffected = db.insert(DatabaseHelper.DB_TABLE_TPH, null, values)
//        db.close()

        return rowsAffected > 0
    }

    @SuppressLint("Range")
    fun fetchAllData(archive: Int = 0): List<TPHModel> {
        val dataTPH = mutableListOf<TPHModel>()
        val db = databaseHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $DB_TABLE_TPH WHERE $DB_ARCHIVE = ?",
            arrayOf(archive.toString()))

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
                    archive =  it.getInt(it.getColumnIndex(DB_ARCHIVE)),
                    app_version = it.getString(it.getColumnIndex(KEY_APP_VERSION))
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
                val rowsAffected = db.delete(DatabaseHelper.DB_TABLE_TPH, "id=?", arrayOf(id))
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
    }

    fun uploadDataServer(context: Context, dataList: List<UploadData>): LiveData<Result<List<UploadResponse>>> {
        val result = MutableLiveData<Result<List<UploadResponse>>>()
        val executor = Executors.newSingleThreadExecutor()
        val responses = mutableListOf<UploadResponse>()

        fun attemptUpload(index: Int) {
            val currentData = dataList[index]
            val call = apiService.uploadData(currentData)

            call.enqueue(object : Callback<UploadResponse> {
                override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val uploadResponse = response.body()!!

                        if (uploadResponse.success) {
                            // If the response is a success, proceed as before
                            responses.add(uploadResponse)
                            Log.d("upload", "Item ${index + 1} uploaded successfully. Response: ${uploadResponse.message}")

                            try {
                                updateArchiveStatus(currentData.id, 1)
                                Log.d("upload", "Archive status updated for item ${currentData.id}")
                            } catch (e: Exception) {
                                Log.e("upload", "Error updating archive status: ${e.message}")
                            }

                            if (responses.size == dataList.size) {
                                result.postValue(Result.success(responses))
                                executor.shutdown()
                            } else if (index + 1 < dataList.size) {
                                attemptUpload(index + 1)
                            }
                        } else {
                            // If the server returns failure, directly extract the message from the body
                            val errorMessage = response.body()?.message ?: "Unknown error"
                            Log.d("testing", errorMessage)
                            handleFailure(errorMessage)
                        }
                    } else {
                        // If the response is unsuccessful, handle the error message
                        try {
                            val errorJson = response.errorBody()?.string()
                            val errorObj = JSONObject(errorJson!!)
                            val errorMessage = errorObj.getString("message")
                            handleFailure(errorMessage)
                        } catch (e: Exception) {
                            handleFailure("Error uploading data: Unknown error")
                        }
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    handleFailure("Network failure: ${t.message}")
                }

                private fun handleFailure(errorMessage: String) {
                    Log.e("upload", errorMessage)

                    // Show the dialog with just the error message
                    AlertDialogUtility.withSingleAction(
                        context,
                        "Kembali",
                        "Terjadi Kesalahan Upload!",
                        "Periksa kembali data yang di-upload! Error: $errorMessage",
                        "warning.json",
                        R.color.colorRedDark
                    ) {}

                    // Post the failure with just the error message
                    result.postValue(Result.failure(Exception(errorMessage)))
                    executor.shutdown()
                }
            })
        }

        executor.execute { attemptUpload(0) }
        return result
    }




    fun updateArchiveStatus(id: Int, status: Int) {
        val db = databaseHelper.readableDatabase
        val values = ContentValues().apply {
            put(DB_ARCHIVE, status)  // Using the constant for archive column
        }

        db.update(
            DB_TABLE_TPH,           // Using the constant for table name
            values,
            "$KEY_ID = ?",          // Using the constant for ID column
            arrayOf(id.toString())
        )
    }

}