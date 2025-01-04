package com.cbi.markertph.data.model

import com.google.gson.annotations.SerializedName

data class FetchResponseTPH(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: AllData
)

data class AllData(
    @SerializedName("CompanyCode") val companyCode: List<CompanyCodeModel>,
    @SerializedName("BUnitCode") val bUnitCode: List<BUnitCodeModel>,
    @SerializedName("DivisionCode") val divisionCode: List<DivisionCodeModel>,
    @SerializedName("FieldCode") val fieldCode: List<FieldCodeModel>,
    @SerializedName("TPH") val tph: List<TPHModel>
)