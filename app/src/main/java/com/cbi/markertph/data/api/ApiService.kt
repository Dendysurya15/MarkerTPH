package com.cbi.markertph.data.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.android.volley.toolbox.JsonObjectRequest
import com.cbi.markertph.data.model.BatchUploadRequest
import com.cbi.markertph.data.model.FetchResponseTPH
import com.cbi.markertph.data.model.UploadData
import com.cbi.markertph.data.model.UploadResponse
import kotlinx.coroutines.CompletableDeferred
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.Volley
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeoutException
import java.util.zip.GZIPOutputStream

object VolleyApiService {
    private const val BASE_URL = "https://auth.srs-ssms.com/api/"

    fun downloadFile(context: Context, endpoint: String): CompletableDeferred<ByteArray> {
        val deferred = CompletableDeferred<ByteArray>()
        val url = BASE_URL + endpoint

        val request = InputStreamVolleyRequest(
            Request.Method.GET,
            url,
            { response -> deferred.complete(response) },
            { error -> deferred.completeExceptionally(error) },
            null
        )

        VolleySingleton.getInstance(context).addToRequestQueue(request)
        return deferred
    }

    fun makeRequest(context: Context, endpoint: String): CompletableDeferred<JSONObject> {
        val deferred = CompletableDeferred<JSONObject>()
        val url = BASE_URL + endpoint

        val request = object : JsonObjectRequest(Method.GET, url, null,
            { response -> deferred.complete(response) },
            { error -> deferred.completeExceptionally(error) }
        ) {
            override fun getHeaders() = mutableMapOf(
                "Accept" to "application/json",
                "Content-Type" to "application/json"
            )
        }

        VolleySingleton.getInstance(context).addToRequestQueue(request)
        return deferred
    }

    fun uploadData(context: Context, dataList: List<UploadData>): CompletableDeferred<JSONObject> {
        val deferred = CompletableDeferred<JSONObject>()
        val startTime = System.currentTimeMillis()
        var retryCount = 0

        fun makeRequest() {
            try {
                // Prepare data
                val jsonString = Gson().toJson(dataList)
                val compressedData = ByteArrayOutputStream().apply {
                    GZIPOutputStream(this).use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }
                }
                val base64Data = Base64.encodeToString(compressedData.toByteArray(), Base64.NO_WRAP)

                val request = object : JsonObjectRequest(
                    Method.POST,
                    "${BASE_URL}storeDataTPHKoordinat",
                    JSONObject().put("data", base64Data),
                    { response -> deferred.complete(response) },
                    { error ->
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - startTime >= 60000) { // 60 seconds timeout
                            deferred.completeExceptionally(TimeoutException("Request timed out after 60 seconds"))
                        } else {
                            retryCount++
                            Handler(Looper.getMainLooper()).postDelayed({
                                makeRequest()
                            }, 7000) // 7 seconds retry delay
                        }
                    }
                ) {
                    override fun getHeaders() = mutableMapOf(
                        "Accept" to "application/json",
                        "Content-Type" to "application/json"
                    )
                }

                VolleySingleton.getInstance(context).addToRequestQueue(request)
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            }
        }

        makeRequest()
        return deferred
    }
}

class InputStreamVolleyRequest(
    method: Int,
    url: String,
    private val onResponse: (ByteArray) -> Unit,
    errorListener: Response.ErrorListener,
    private val params: Map<String, String>?
) : Request<ByteArray>(method, url, errorListener) {

    override fun getHeaders(): MutableMap<String, String> =
        mutableMapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json"
        )

    override fun deliverResponse(response: ByteArray) = onResponse(response)

    override fun parseNetworkResponse(response: NetworkResponse): Response<ByteArray> =
        Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response))
}



class VolleySingleton private constructor(context: Context) {
    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }

    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null

        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context).also { INSTANCE = it }
            }
    }
}