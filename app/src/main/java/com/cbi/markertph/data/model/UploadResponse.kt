package com.cbi.markertph.data.model

import com.google.gson.annotations.SerializedName

data class UploadResponse(
    @SerializedName("statusCode") val statusCode: Int, // 0: failed, 1: success, 2: all duplicated
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Any? = null
)