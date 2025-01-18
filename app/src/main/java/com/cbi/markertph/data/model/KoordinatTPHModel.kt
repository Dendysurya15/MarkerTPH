package com.cbi.markertph.data.model

import androidx.room.Ignore

data class KoordinatTPHModel(
    @Ignore val id: Int,
    val tanggal: String,
    val user_input: String,
    val regional: String,
    val regional_id: Int,
    val wilayah: String,
    val wilayah_id: Int,
    val estate: String,
    val id_estate: Int,
    val afdeling: String,
    val id_afdeling: Int,
    val tahun_tanam : String,
    val blok: String,
    val id_blok: Int,
    val ancak: String,
    val tph: String,
    val id_tph: Int,
    val panen_ulang:Int,
    val latitude: String,
    val longitude: String,
    val archive:Int,
    val app_version:String,

)