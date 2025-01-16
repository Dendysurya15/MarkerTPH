package com.cbi.markertph.data.model

import com.google.gson.annotations.SerializedName

data class UploadData(
    @SerializedName("id") val id: Int,
    @SerializedName("datetime") val datetime: String,
    @SerializedName("user_input") val user_input: String,
    @SerializedName("estate") val estate: String?,
    @SerializedName("BUnitCode") val id_estate: Int,
    @SerializedName("afdeling") val afdeling: String?,
    @SerializedName("DivisionCode") val id_afdeling: Int,
    @SerializedName("planting_year") val tahun_tanam: String,
    @SerializedName("blok") val blok: String?,
    @SerializedName("FieldCode") val id_blok: Int,
    @SerializedName("ancak") val ancak: String?,
    @SerializedName("tph") val tph: String?,
    @SerializedName("id_tph") val id_tph: Int,
    @SerializedName("panen_ulang") val panen_ulang: Int,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("app_version") val app_version: String
)

// This is the new class for the batch request
data class BatchUploadRequest(
    @SerializedName("data") val data: String // Base64 encoded GZIP data
)