package com.cbi.markertph.utils

import android.content.Context
import com.cbi.markertph.data.model.BUnitCodeModel
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.CompanyCodeModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel
import com.cbi.markertph.data.model.DivisionCodeModel
import com.cbi.markertph.data.model.FieldCodeModel
import com.cbi.markertph.data.model.RegionalModel
import com.cbi.markertph.data.model.TPHModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataCacheManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("data_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDatasets(
        regionalList: List<RegionalModel>,
        wilayahList: List<WilayahModel>,
        deptList: List<DeptModel>,
        divisiList: List<DivisiModel>,
        blokList: List<BlokModel>,
        tphList: List<TPHNewModel>
    ) {
        prefs.edit().apply {
            putString("RegionalDB", gson.toJson(regionalList))
            putString("WilayahDB", gson.toJson(wilayahList))
            putString("DeptDB", gson.toJson(deptList))
            putString("DivisiDB", gson.toJson(divisiList))
            putString("BlokDB", gson.toJson(blokList))
            putString("TPHDB", gson.toJson(tphList))
            putLong("last_update", System.currentTimeMillis())
            apply()
        }
    }

    fun getDatasets(): DataSets? {
        // Check if we have cached data
        val companyCodesJson = prefs.getString("RegionalDB", null) ?: return null

        return try {
            DataSets(
                regionalList = gson.fromJson(companyCodesJson, object : TypeToken<List<RegionalModel>>() {}.type),
                wilayahList = gson.fromJson(prefs.getString("WilayahDB", "[]"), object : TypeToken<List<WilayahModel>>() {}.type),
                deptList = gson.fromJson(prefs.getString("DeptDB", "[]"), object : TypeToken<List<DeptModel>>() {}.type),
                divisiList = gson.fromJson(prefs.getString("DivisiDB", "[]"), object : TypeToken<List<DivisiModel>>() {}.type),
                blokList = gson.fromJson(prefs.getString("BlokDB", "[]"), object : TypeToken<List<BlokModel>>() {}.type),
                tphList = gson.fromJson(prefs.getString("TPHDB", "[]"), object : TypeToken<List<TPHNewModel>>() {}.type)
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
    val regionalList: List<RegionalModel>,
    val wilayahList: List<WilayahModel>,
    val deptList: List<DeptModel>,
    val divisiList: List<DivisiModel>,
    val blokList: List<BlokModel>,
    val tphList: List<TPHNewModel>
)