package com.cbi.markertph.data.model

import com.google.gson.annotations.SerializedName

data class UploadData(
    @SerializedName("id") val id: Int,
    @SerializedName("datetime") val datetime: String,
    @SerializedName("estate") val estate: String?,
    @SerializedName("afdeling") val afdeling: String?,
    @SerializedName("blok") val blok: String?,
    @SerializedName("tph") val tph: String?,
    @SerializedName("lat") val lat: String,      // Changed from latitude
    @SerializedName("lon") val lon: String,      // Changed from longitude
    @SerializedName("app_version") val app_version: String
)

// This is the new class for the batch request
data class BatchUploadRequest(
    @SerializedName("data") val data: String // Base64 encoded GZIP data
)