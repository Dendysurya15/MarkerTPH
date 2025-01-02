package com.cbi.markertph.data.model

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)