package com.cbi.markertph.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cbi.markertph.data.model.TPHModel
import com.cbi.markertph.data.repository.TPHRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TPHViewModel(application: Application, private val repository: TPHRepository) : AndroidViewModel(application){

    private val _insertDBTPH = MutableLiveData<Boolean>()
    val insertDBTPH: LiveData<Boolean> get() = _insertDBTPH


    private val _dataTPHAll = MutableLiveData<List<TPHModel>>()

    val dataTPHAll: LiveData<List<TPHModel>> get() = _dataTPHAll

    fun insertPanenTBSVM(
        id: Int? = 0,
        tanggal: String,
        estate: String,
        id_estate: Int,
        afdeling: String,
        id_afdeling: Int,
        blok: String,
        id_blok: Int,
        tph: String,
        id_tph: Int,
        latitude: String,
        longitude: String,

    ) {
        viewModelScope.launch {
            try {

                val data = TPHModel(
                    id!!,
                    tanggal,
                    estate,
                    id_estate,
                    afdeling,
                    id_afdeling,
                    blok,
                    id_blok,
                    tph,
                    id_tph,
                    latitude,
                    longitude,
                    0
                )

                // Insert data into the repository
                val isInserted = repository.insertTPHRepo(data)

                // Update LiveData
                _insertDBTPH.postValue(isInserted)
            } catch (e: Exception) {
                e.printStackTrace()
                _insertDBTPH.postValue(false)
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