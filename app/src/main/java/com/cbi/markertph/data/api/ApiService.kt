package com.cbi.markertph.data.api

import com.cbi.markertph.data.model.BatchUploadRequest
import com.cbi.markertph.data.model.FetchResponseTPH
import com.cbi.markertph.data.model.UploadData
import com.cbi.markertph.data.model.UploadResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming


interface ApiService {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("storeDataTPHKoordinat")
    fun uploadData(@Body data: BatchUploadRequest): retrofit2.Call<UploadResponse>

    @GET("fetchRawDataTPH")
    fun fetchRawData(): retrofit2.Call<FetchResponseTPH>

//    @Streaming
//    @GET("downloadDatasetCompanyJson")
//    suspend fun downloadDatasetCompany(): Response<ResponseBody>
//
    @Streaming
    @GET("downloadDatasetTPHNewJson")
    suspend fun downloadDatasetTPHNewJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetBlokJson")
    suspend fun downloadDatasetBlokJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetDivisiJson")
    suspend fun downloadDatasetDivisiJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetDeptJson")
    suspend fun downloadDatasetDeptJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetWilayahJson")
    suspend fun downloadDatasetWilayahJson(): Response<ResponseBody>

    @Streaming
    @GET("downloadDatasetRegionalJson")
    suspend fun downloadDatasetRegionalJson(): Response<ResponseBody>


    @GET("getTablesLatestModified")
    suspend fun getTablesLatestModified(): Response<TablesModifiedResponse>

    data class TablesModifiedResponse(
        val statusCode: Int,
        val message: String,
        val data: Map<String, String?>
    )
}