package com.cbi.markertph.utils

import android.content.Context
import com.cbi.markertph.data.model.BUnitCodeModel
import com.cbi.markertph.data.model.CompanyCodeModel
import com.cbi.markertph.data.model.DivisionCodeModel
import com.cbi.markertph.data.model.FieldCodeModel
import com.cbi.markertph.data.model.TPHModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataCacheManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("data_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDatasets(
        companyCodeList: List<CompanyCodeModel>,
        bUnitCodeList: List<BUnitCodeModel>,
        divisionCodeList: List<DivisionCodeModel>,
        fieldCodeList: List<FieldCodeModel>,
        tphList: List<TPHModel>
    ) {
        prefs.edit().apply {
            putString("CompanyCodeDB", gson.toJson(companyCodeList))
            putString("BUnitCodeDB", gson.toJson(bUnitCodeList))
            putString("DivisionCodeDB", gson.toJson(divisionCodeList))
            putString("FieldCodeDB", gson.toJson(fieldCodeList))
            putString("TPHDB", gson.toJson(tphList))
            putLong("last_update", System.currentTimeMillis())
            apply()
        }
    }

    fun getDatasets(): DataSets? {
        // Check if we have cached data
        val companyCodesJson = prefs.getString("CompanyCodeDB", null) ?: return null

        return try {
            DataSets(
                companyCodeList = gson.fromJson(companyCodesJson, object : TypeToken<List<CompanyCodeModel>>() {}.type),
                bUnitCodeList = gson.fromJson(prefs.getString("BUnitCodeDB", "[]"), object : TypeToken<List<BUnitCodeModel>>() {}.type),
                divisionCodeList = gson.fromJson(prefs.getString("DivisionCodeDB", "[]"), object : TypeToken<List<DivisionCodeModel>>() {}.type),
                fieldCodeList = gson.fromJson(prefs.getString("FieldCodeDB", "[]"), object : TypeToken<List<FieldCodeModel>>() {}.type),
                tphList = gson.fromJson(prefs.getString("TPHDB", "[]"), object : TypeToken<List<TPHModel>>() {}.type)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun needsRefresh(): Boolean {
        val lastUpdate = prefs.getLong("last_update", 0)
        val oneDayInMillis = 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - lastUpdate > oneDayInMillis
    }
}

data class DataSets(
    val companyCodeList: List<CompanyCodeModel>,
    val bUnitCodeList: List<BUnitCodeModel>,
    val divisionCodeList: List<DivisionCodeModel>,
    val fieldCodeList: List<FieldCodeModel>,
    val tphList: List<TPHModel>
)