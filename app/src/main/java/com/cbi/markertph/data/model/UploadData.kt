package com.cbi.markertph.data.model

import com.google.gson.annotations.SerializedName

data class UploadData(
    @SerializedName("id") val id: Int,
    @SerializedName("datetime") val datetime: String,
    @SerializedName("user_input") val user_input: String,
    @SerializedName("estate") val estate: String?,
    @SerializedName("dept") val dept: Int,
    @SerializedName("afdeling") val afdeling: String?,
    @SerializedName("divisi") val divisi: Int,
    @SerializedName("tahun") val tahun: String,
    @SerializedName("blok") val blok: Int,
    @SerializedName("ancak") val ancak: String?,
    @SerializedName("nomor") val nomor: String?,
    @SerializedName("panen_ulang") val panen_ulang: Int,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("app_version") val app_version: String
)

// This is the new class for the batch request
data class BatchUploadRequest(
    @SerializedName("data") val data: String // Base64 encoded GZIP data
)