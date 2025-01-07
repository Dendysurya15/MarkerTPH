package com.cbi.markertph.data.api

import com.cbi.markertph.data.model.BatchUploadRequest
import com.cbi.markertph.data.model.FetchResponseTPH
import com.cbi.markertph.data.model.UploadData
import com.cbi.markertph.data.model.UploadResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST


interface ApiService {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("storeDataTPH")
    fun uploadData(@Body data: BatchUploadRequest): retrofit2.Call<UploadResponse>

    @GET("fetchRawDataTPH")
    fun fetchRawData(): retrofit2.Call<FetchResponseTPH>
}