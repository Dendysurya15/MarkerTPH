package com.cbi.markertph.data.model

import androidx.room.Ignore

data class KoordinatTPHModel(
    @Ignore val id: Int,
    val tanggal: String,
    val user_input: String,
    val estate: String,
    val id_estate: Int,
    val afdeling: String,
    val id_afdeling: Int,
    val blok: String,
    val id_blok: Int,
    val ancak: String,
    val id_ancak: Int,
    val tph: String,
    val id_tph: Int,
    val latitude: String,
    val longitude: String,
    val archive:Int,
    val app_version:String,
)