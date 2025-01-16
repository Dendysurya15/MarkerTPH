package com.cbi.markertph.ui.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ID
import com.cbi.markertph.data.model.BUnitCodeModel
import com.cbi.markertph.data.model.CompanyCodeModel
import com.cbi.markertph.data.model.DivisionCodeModel
import com.cbi.markertph.data.model.FieldCodeModel
import com.cbi.markertph.data.model.KoordinatTPHModel
import com.cbi.markertph.data.model.TPHModel
import com.cbi.markertph.data.model.UploadData
import com.cbi.markertph.data.model.UploadResponse
import com.cbi.markertph.data.repository.TPHRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TPHViewModel(application: Application, private val repository: TPHRepository) : AndroidViewModel(application){

    private val _insertDBTPH = MutableLiveData<Boolean>()
    val insertDBTPH: LiveData<Boolean> get() = _insertDBTPH


    private val _dataTPHAll = MutableLiveData<List<KoordinatTPHModel>>()
    val dataTPHAll: LiveData<List<KoordinatTPHModel>> get() = _dataTPHAll
    private val _insertStatus = MutableLiveData<Boolean>()
    val insertStatus: LiveData<Boolean> = _insertStatus

    private val _deleteItemsResult = MutableLiveData<Boolean>()
    val deleteItemsResult: LiveData<Boolean> = _deleteItemsResult

    private val _resultCountDataArchive = MutableLiveData<Int>()
    val resultCountDataArchive: LiveData<Int> = _resultCountDataArchive

    private val _resultCountDataNonArchive = MutableLiveData<Int>()
    val resultCountDataNonArchive: LiveData<Int> = _resultCountDataNonArchive

    private val _tphDataList = MutableLiveData<List<String>>()
    val tphDataList: LiveData<List<String>> get() = _tphDataList

    fun updateTPHData(newData: List<String>) {
        _tphDataList.value = newData
    }

    // Example method to trigger the LiveData update (replace this with your logic)
    fun fetchTPHData() {
        val hardcodedTPHValues = listOf(
            1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 3,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 4, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 5, 50,
            51, 52, 53, 54, 55, 56, 57, 58, 59, 6, 60, 61, 62, 63, 7, 8, 9
        )
        _tphDataList.value = hardcodedTPHValues.map { it.toString() }
    }

    // Change from List<UploadResponse> to single UploadResponse
    fun uploadData(context: Context, dataList: List<UploadData>): LiveData<Result<UploadResponse>> {
        return repository.uploadDataServer(context, dataList)
    }

    fun getCompanyCodeCount(): Int {
        return repository.getCompanyCodeCount()
    }

    fun getBUnitCodeCount(): Int {
        return repository.getBUnitCodeCount()
    }

    fun getDivisionCodeCount(): Int {
        return repository.getDivisionCodeCount()
    }

    fun getFieldCodeCount(): Int {
        return repository.getFieldCodeCount()
    }

    fun getTPHCount(): Int {
        return repository.getTPHCount()
    }


    fun getAllBUnitCodes(): LiveData<List<BUnitCodeModel>> {
        val bUnitCodesLiveData = MutableLiveData<List<BUnitCodeModel>>()
        viewModelScope.launch(Dispatchers.IO) {
            val bUnitCodes = repository.getAllBUnitCodes()
            bUnitCodesLiveData.postValue(bUnitCodes)
        }
        return bUnitCodesLiveData
    }

    fun getDivisionCodesByBUnitCode(bUnitCode: Int): LiveData<List<DivisionCodeModel>> {
        val divisionCodesLiveData = MutableLiveData<List<DivisionCodeModel>>()
        viewModelScope.launch(Dispatchers.IO) {
            val divisions = repository.getDivisionCodesByBUnitCode(bUnitCode)
            divisionCodesLiveData.postValue(divisions)
        }
        return divisionCodesLiveData
    }

    fun getFieldCodesByBUnitAndDivision(bUnitCode: Int, divisionCode: Int): LiveData<List<FieldCodeModel>> {
        val fieldCodesLiveData = MutableLiveData<List<FieldCodeModel>>()
        viewModelScope.launch(Dispatchers.IO) {
            val fields = repository.getFieldCodesByBUnitAndDivision(bUnitCode, divisionCode)
            fieldCodesLiveData.postValue(fields)
        }
        return fieldCodesLiveData
    }

    fun getAncakByFieldCode(bUnitCode: Int, divisionCode: Int, fieldCode: Int): LiveData<List<TPHModel>> {
        val tphLiveData = MutableLiveData<List<TPHModel>>()
        viewModelScope.launch(Dispatchers.IO) {
            val tphList = repository.getAncakByFieldCode(bUnitCode, divisionCode, fieldCode)
            tphLiveData.postValue(tphList)
        }
        return tphLiveData
    }

    fun getTPHByAncakNumbers(bUnitCode: Int, divisionCode: Int, fieldCode: Int, tphIds: List<Int>): LiveData<List<TPHModel>> {
        val tphLiveData = MutableLiveData<List<TPHModel>>()
        viewModelScope.launch(Dispatchers.IO) {
            val tphList = repository.getTPHByAncakNumbers(bUnitCode, divisionCode, fieldCode, tphIds)
            tphLiveData.postValue(tphList)
        }
        return tphLiveData
    }

    fun insertCompanyCode(companyCodeModel: CompanyCodeModel) {
        viewModelScope.launch {
            try {
                val isInserted = repository.insertCompanyCodeRepo(companyCodeModel)
                // You can update LiveData here if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertBUnitCode(bUnitCodeModel: BUnitCodeModel) {
        viewModelScope.launch {
            try {
                val isInserted = repository.insertBUnitCodeRepo(bUnitCodeModel)
                // You can update LiveData here if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertDivisionCode(divisionCodeModel: DivisionCodeModel) {
        viewModelScope.launch {
            try {
                val isInserted = repository.insertDivisionCodeRepo(divisionCodeModel)
                // You can update LiveData here if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertFieldCode(fieldCodeModel: FieldCodeModel) {
        viewModelScope.launch {
            try {
                val isInserted = repository.insertFieldCodeRepo(fieldCodeModel)
                // You can update LiveData here if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertTPHBatch(tphList: List<TPHModel>) {
        viewModelScope.launch {
            try {
                Log.d("TPHViewModel", "Starting batch insertion of ${tphList.size} records")
                val isInserted = repository.insertTPHBatch(tphList)
                Log.d("TPHViewModel", "Batch insertion completed: $isInserted")
            } catch (e: Exception) {
                Log.e("TPHViewModel", "Error during batch insertion", e)
            }
        }
    }

    fun insertPanenTBSVM(
        id: Int? = 0,
        tanggal: String,
        user_input: String,
        estate: String,
        id_estate: Int,
        afdeling: String,
        id_afdeling: Int,
        tahun_tanam : String,
        blok: String,
        id_blok: Int,
        ancak: String,
        id_ancak: Int,
        tph: String,
        id_tph: Int,
        panen_ulang : Int,
        latitude: String,
        longitude: String,
        app_version: String
    ) {
        viewModelScope.launch {
            try {

                val data = KoordinatTPHModel(
                    id!!,
                    tanggal,
                    user_input,
                    estate,
                    id_estate,
                    afdeling,
                    id_afdeling,
                    tahun_tanam ,
                    blok,
                    id_blok,
                    ancak,
                    id_ancak,
                    tph,
                    id_tph,
                    panen_ulang,
                    latitude,
                    longitude,
                    0,
                    app_version
                )


                // Insert data into the repository
                val isInserted = repository.insertKoordinatTPHRepo(data)

                // Update LiveData
                _insertDBTPH.postValue(isInserted)
            } catch (e: Exception) {
                e.printStackTrace()
                _insertDBTPH.postValue(false)
            }
        }
    }

    fun countDataArchive() {
        viewModelScope.launch {
            val dataUnit = withContext(Dispatchers.IO) {
                repository.fetchAllData(1)
            }
            _resultCountDataArchive.value = dataUnit.size
        }
    }

    fun countDataNonArchive() {
        viewModelScope.launch {
            val dataUnit = withContext(Dispatchers.IO) {
                repository.fetchAllData(0)
            }
            _resultCountDataNonArchive.value = dataUnit.size
        }
    }

    fun deleteMultipleItems(items: List<Map<String, Any>>) {
        viewModelScope.launch {
            try {
                val ids = items.map { it[KEY_ID].toString() }
                val isDeleted = repository.deleteMultipleItems(ids)
                _deleteItemsResult.value = isDeleted
            } catch (e: Exception) {
                e.printStackTrace()
                _deleteItemsResult.value = false
            }
        }
    }

    fun loadDataAllTPH(archive: Int = 0) {
        viewModelScope.launch {
            val dataUnit = withContext(Dispatchers.IO) {
                repository.fetchAllData(archive)
            }
            _dataTPHAll.value = dataUnit
        }
    }




    @Suppress("UNCHECKED_CAST")
    class Factory(private val application: Application, private val repository: TPHRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TPHViewModel::class.java)) {
                return TPHViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}