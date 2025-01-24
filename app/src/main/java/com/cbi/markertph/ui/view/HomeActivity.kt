package com.cbi.markertph.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.R
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
import com.cbi.markertph.data.network.RetrofitClient
import com.cbi.markertph.data.repository.TPHRepository
import com.cbi.markertph.databinding.ActivityHomeBinding
import com.cbi.markertph.databinding.PertanyaanSpinnerLayoutBinding
import com.cbi.markertph.ui.adapter.ProgressUploadAdapter
import com.cbi.markertph.ui.view.ui.home.HomeFragment.InputType
import com.cbi.markertph.ui.viewModel.LocationViewModel
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.AlertDialogUtility
import com.cbi.markertph.utils.AppUtils
import com.cbi.markertph.utils.AppUtils.stringXML
import com.cbi.markertph.utils.AppUtils.vibrate
import com.cbi.markertph.utils.DataCacheManager
import com.cbi.markertph.utils.LoadingDialog
import com.cbi.markertph.utils.PrefManager
import com.cbi.markertph.utils.ReleaseLogger
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import android.text.InputType as AndroidInputType

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var tphViewModel: TPHViewModel
    private var prefManager: PrefManager? = null
    private var locationEnable: Boolean = false
    private var isPermissionRationaleShown = false
    private var lat: Double? = null
    private var lon: Double? = null
    private var userInput: String = ""
    private var ancakInput: String = ""
    private var selectedRegional: String = ""
    private var selectedWilayah: String = ""
    private var selectedEstate: String = ""
    private var selectedAfdeling: String = ""
    private var selectedBlok: String = ""
    private var selectedTPH: String = ""
    private var selectedAncak: String = ""
    var currentAccuracy : Float = 0F

    private var filesToUpdate = mutableListOf<String>()

    companion object {
        private const val CHUNK_SIZE = 8192 // 8KB chunks
        private const val DEFAULT_BUFFER_SIZE = 8192 * 4 // Increased buffer size for better performance
    }
    private lateinit var dataCacheManager: DataCacheManager
    private lateinit var loadingDialog: LoadingDialog
    private var selectedRegionalValue: Int? = null
    private var selectedWilayahValue: Int? = null
    private var selectedEstateValue: Int? = null
    private var selectedDivisiValue: Int? = null
    private var selectedBlokValue: Int? = null
    private var selectedDivisionCodeValue: Int? = null
    private var selectedTahunTanamValue: String? = null
    private var selectedFieldCodeValue: Int? = null
    private var selectedTPHValue: Int? = null


    private val locationSettingsCallback = object : LocationCallback() {
        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            super.onLocationAvailability(locationAvailability)

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (!isGpsEnabled) {
                locationViewModel.refreshLocationStatus()
                locationEnable = false
                AlertDialogUtility.withSingleAction(
                    this@HomeActivity,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_location_not_ready),
                    stringXML(R.string.al_location_description_failed),
                    "warning.json",
                    R.color.colorRedDark
                ) {}
            }
        }
    }

    private var selectedDivisionSpinnerIndex: Int? = null
    private var selectedRegionalSpinnerIndex: Int? = null
    private var selectedWilayahSpinnerIndex: Int? = null
    private var selectedEstateSpinnerIndex: Int? = null
    private var selectedBUnitSpinnerIndex: Int? = null
    private var selectedFieldCodeSpinnerIndex: Int? = null
    private var selectedTPHSpinnerIndex: Int? = null
    private var regionalList: List<RegionalModel> = emptyList()
    private var wilayahList: List<WilayahModel> = emptyList()
    private var deptList: List<DeptModel> = emptyList()
    private var divisiList: List<DivisiModel> = emptyList()
    private var blokList: List<BlokModel> = emptyList()
    private var tphList: List<TPHNewModel>? = null // Lazy-loaded

    enum class InputType {
        SPINNER,
        EDITTEXT
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION, // Add fine location permission
        Manifest.permission.ACCESS_COARSE_LOCATION // Add coarse location permission (optional)
        )

    private val locationSettingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                if (!isGpsEnabled) {
                    locationEnable = false
                    locationViewModel.refreshLocationStatus()
                    AlertDialogUtility.withSingleAction(
                        this@HomeActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_location_not_ready),
                        stringXML(R.string.al_location_description_failed),
                        "warning.json",
                        R.color.colorRedDark
                    ) {}
                } else {
                    // GPS is enabled, start location updates
                    locationEnable = true
                    locationViewModel.startLocationUpdates()
                }
            }
        }
    }
    data class ErrorResponse(
        val statusCode: Int,
        val message: String,
        val error: String? = null
    )
    private val permissionRequestCode = 1001

    private lateinit var inputMappings: List<Triple<PertanyaanSpinnerLayoutBinding, String, InputType>>

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationViewModel.startLocationUpdates()
            } else {
//                showSnackbar(getString(R.string.location_permission_denied))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PrefManager(this)
        dataCacheManager = DataCacheManager(this)
        loadingDialog = LoadingDialog(this)
        initViewModel()
        checkPermissions()
        setupLayout()
        setAppVersion()

        getDeviceInfo(this)


        registerReceiver(
            locationSettingsReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )
        binding.menuUpload.setOnClickListener{
            this.vibrate()
            startActivity(Intent(this@HomeActivity, UploadDataActivity::class.java))
            finish()
        }

        binding.mbSaveDataTPH.setOnClickListener {

            if (currentAccuracy == null || currentAccuracy > 10.0f) {
                vibrate()
                AlertDialogUtility.withSingleAction(
                    this,
                    stringXML(R.string.al_back),
                    stringXML(R.string.al_location_not_accurate),
                    stringXML(R.string.al_location_under_ten_meter),
                    "warning.json",
                    R.color.colorRedDark
                ) {}
                return@setOnClickListener
            }

            if (validateAndShowErrors()) {
                AlertDialogUtility.withTwoActions(
                    this,
                    getString(R.string.al_save),
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.confirmation_dialog_description),
                    "warning.json"
                ) {
                    val app_version = getDeviceInfo(this)
                    tphViewModel.insertPanenTBSVM(
                        tanggal = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                            Date()
                        ),
                        user_input = userInput,
                        regional = selectedRegional,
                        regional_id = selectedRegionalValue!!,
                        wilayah = selectedWilayah,
                        wilayah_id = selectedWilayahValue!!,
                        estate = selectedEstate,
                        id_estate = selectedEstateValue ?: 0,
                        afdeling = selectedAfdeling,
                        id_afdeling = selectedDivisiValue ?: 0,
                        tahun_tanam = selectedTahunTanamValue ?: "",
                        blok = selectedBlok,
                        id_blok = selectedBlokValue ?: 0,
                        ancak = ancakInput,
                        tph = selectedTPH,
                        id_tph = selectedTPHValue!!,
                        panen_ulang = 0,
                        latitude = lat.toString(),
                        longitude = lon.toString(),
                        app_version = app_version.toString(),
                    )

                    tphViewModel.insertDBTPH.observe(this) { result ->
                        val (isInserted, errorMessage) = result  // Destructuring the Pair

                        if (isInserted) {
                            AlertDialogUtility.alertDialogAction(
                                this,
                                getString(R.string.al_success_save_local),
                                getString(R.string.al_description_success_save_local),
                                "success.json"
                            ) {
                                // Your existing success handling code
                            }
                        } else {
                            AlertDialogUtility.alertDialogAction(
                                this,
                                getString(R.string.al_failed_save_local),
                                "${getString(R.string.al_description_failed_save_local)} $errorMessage",
                                "warning.json"
                            ) {}

                            // Show error message in Toast if available
                            Toast.makeText(
                                this,
                                errorMessage ?: getString(R.string.toast_failed_save_local),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }

    private fun initViewModel() {
        tphViewModel = ViewModelProvider(
            this,
            TPHViewModel.Factory(application, TPHRepository(this))
        )[TPHViewModel::class.java]

        locationViewModel = ViewModelProvider(
            this,
            LocationViewModel.Factory(application, binding.statusLocation, this)
        )[LocationViewModel::class.java]
    }

    private fun setAppVersion() {
        val appVersion = AppUtils.getAppVersion(this)
        binding.versionApp.text = appVersion
    }

    private fun setupLayout() {
        inputMappings = listOf(
            Triple(binding.layoutUserInput, getString(R.string.field_nama_user), InputType.EDITTEXT),
            Triple(binding.layoutRegional, getString(R.string.field_regional), InputType.SPINNER),
            Triple(binding.layoutWilayah, getString(R.string.field_wilayah), InputType.SPINNER),
            Triple(binding.layoutEstate, getString(R.string.field_estate), InputType.SPINNER),
            Triple(binding.layoutAfdeling, getString(R.string.field_afdeling), InputType.SPINNER),
            Triple(binding.layoutTahunTanam, getString(R.string.field_tahun_tanam), InputType.SPINNER),
            Triple(binding.layoutBlok, getString(R.string.field_blok), InputType.SPINNER),
            Triple(binding.layoutAncak, getString(R.string.field_ancak), InputType.EDITTEXT),
            Triple(binding.layoutTPH, getString(R.string.field_tph), InputType.SPINNER)
        )

        // First set up basic layout structure
        inputMappings.forEach { (layoutBinding, key, inputType) ->
            updateTextInPertanyaan(layoutBinding, key)
            when (inputType) {
                InputType.EDITTEXT -> setupEditTextView(layoutBinding)
                InputType.SPINNER -> setupSpinnerView(layoutBinding, emptyList()) // Initialize empty first
            }
        }

//        val savedRegionalIndexSpinner = prefManager?.id_selected_regional ?: 0
//        val savedWilayahIndexSpinner = prefManager?.id_selected_wilayah ?: 0
//        val savedEstateIndexSpinner = prefManager?.id_selected_estate ?: 0
//        val savedAfdelingIndexSpinner = prefManager?.id_selected_afdeling ?: 0
//        val savedTahunTanamIndexSpinner = prefManager?.id_selected_tahun_tanam ?: 0
//        val savedBlockIndexSpinner = prefManager?.id_selected_blok ?: 0
//        val savedTPHIndexSpinner = prefManager?.id_selected_tph ?: 0


        lifecycleScope.launch {
            // Wait until data is ready
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (regionalList.isNotEmpty()) {
                    val regionalOptions = regionalList.map { it.nama }

                    Log.d("testing", regionalOptions.toString())
                    setupSpinnerView(binding.layoutRegional, regionalOptions)

//                    if (savedEstateIndexSpinner in estateOptions.indices) {
//                        binding.layoutEstate.spHomeMarkerTPH.setSelectedIndex(savedEstateIndexSpinner)
//                        selectedBUnitSpinnerIndex = savedEstateIndexSpinner
//                        selectedEstate = estateOptions[savedEstateIndexSpinner]
//                        selectedBUnitCodeValue = bUnitCodeList[savedEstateIndexSpinner].BUnitCode
//
//                        val afdelingOptions = divisionCodeList
//                            .filter { it.BUnitCode == selectedBUnitCodeValue }
//                            .map { it.DivisionName }
//
//                        binding.layoutAfdeling.root.visibility = View.VISIBLE
//                        setupSpinnerView(binding.layoutAfdeling, afdelingOptions)
//
//                        if (savedAfdelingIndexSpinner in afdelingOptions.indices) {
//                            binding.layoutAfdeling.spHomeMarkerTPH.setSelectedIndex(savedAfdelingIndexSpinner)
//                            selectedDivisionSpinnerIndex = savedAfdelingIndexSpinner
//                            selectedAfdeling = afdelingOptions[savedAfdelingIndexSpinner]
//                            selectedDivisionCodeValue = divisionCodeList
//                                .filter { it.BUnitCode == selectedBUnitCodeValue }[savedAfdelingIndexSpinner].DivisionCode
//
//                            // Now filter field codes for planting years
//                            val filteredFieldCodes = fieldCodeList.filter { fieldCode ->
//                                fieldCode.BUnitCode == selectedBUnitCodeValue &&
//                                        fieldCode.DivisionCode == selectedDivisionCodeValue
//                            }
//
//                            val plantingYears = filteredFieldCodes
//                                .map { it.PlantingYear.toString() }
//                                .distinct()
//                                .sorted()
//
//                            binding.layoutTahunTanam.root.visibility = View.VISIBLE
//
//                            setupSpinnerView(binding.layoutTahunTanam, plantingYears)
//
//                            val tahunTanamIndex = plantingYears.indexOf(savedTahunTanamIndexSpinner.toString())
//                            if (tahunTanamIndex != -1) {
//                                binding.layoutTahunTanam.spHomeMarkerTPH.setSelectedIndex(tahunTanamIndex)
//                                selectedTahunTanamValue = savedTahunTanamIndexSpinner.toString()
//
//                                // Filter field codes for selected planting year
//                                val fieldCodeOptions = fieldCodeList.filter { fieldCode ->
//                                    fieldCode.BUnitCode == selectedBUnitCodeValue &&
//                                            fieldCode.DivisionCode == selectedDivisionCodeValue &&
//                                            fieldCode.PlantingYear.toString() == selectedTahunTanamValue
//                                }.map { it.FieldName }
//
//                                binding.layoutBlok.root.visibility = View.VISIBLE
//                                setupSpinnerView(binding.layoutBlok, fieldCodeOptions)
//
//                                // Handle Block saved selection
//                                if (savedBlockIndexSpinner in fieldCodeOptions.indices) {
//                                    binding.layoutBlok.spHomeMarkerTPH.setSelectedIndex(savedBlockIndexSpinner)
//                                    selectedBlok = fieldCodeOptions[savedBlockIndexSpinner]
//                                    selectedFieldCodeSpinnerIndex = savedBlockIndexSpinner
//                                    selectedFieldCodeValue = fieldCodeList.find { it.FieldName == selectedBlok }?.FieldCode
//
//                                    // Filter TPH for Ancak
//                                    val filteredTPH = tphList?.filter { tph ->
//                                        tph.BUnitCode == selectedBUnitCodeValue &&
//                                                tph.DivisionCode == selectedDivisionCodeValue &&
//                                                tph.planting_year == selectedTahunTanamValue!!.toInt() &&
//                                                tph.FieldCode == selectedFieldCodeValue
//                                    }
//
//                                    val ancakValues = filteredTPH?.map { it.ancak }?.distinct()?.map { it.toString() } ?: emptyList()
//
//                                    binding.layoutAncak.root.visibility = View.VISIBLE
//                                    setupSpinnerView(binding.layoutAncak, ancakValues)
//
//                                    if (savedAncakIndexSpinner in ancakValues.indices) {
//                                        binding.layoutAncak.spHomeMarkerTPH.setSelectedIndex(savedAncakIndexSpinner)
//                                        selectedAncak = ancakValues[savedAncakIndexSpinner]
//                                        selectedAncakSpinnerIndex = savedAncakIndexSpinner
//                                        selectedAncakValue = tphList?.find { tph ->
//                                            tph.BUnitCode == selectedBUnitCodeValue &&
//                                                    tph.DivisionCode == selectedDivisionCodeValue &&
//                                                    tph.planting_year == selectedTahunTanamValue?.toInt() &&
//                                                    tph.FieldCode == selectedFieldCodeValue &&
//                                                    tph.ancak.toString() == selectedAncak
//                                        }?.ancak
//
//                                        // Filter TPH values
//                                        val tphValues = filteredTPH
//                                            ?.filter { it.ancak == selectedAncakValue }
//                                            ?.map { it.tph.toString() }
//                                            ?.distinct() ?: emptyList()
//
//                                        binding.layoutTPH.root.visibility = View.VISIBLE
//                                        setupSpinnerView(binding.layoutTPH, tphValues)
//
//                                        if (savedTPHIndexSpinner in tphValues.indices) {
//                                            binding.layoutTPH.spHomeMarkerTPH.setSelectedIndex(savedTPHIndexSpinner)
//                                            selectedTPH = tphValues[savedTPHIndexSpinner]
//                                            selectedTPHSpinnerIndex = savedTPHIndexSpinner
//                                            selectedIDTPHAncak = filteredTPH?.firstOrNull {
//                                                it.ancak == selectedAncakValue &&
//                                                        it.tph.toString() == selectedTPH
//                                            }?.id
//
//                                            findViewById<MaterialButton>(R.id.mbSaveDataTPH).visibility = View.VISIBLE
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    } else {
//                        Log.d("testing", "Saved Estate index ($savedEstateIndexSpinner) out of bounds for options size ${estateOptions.size}")
//                    }
                }
            }
        }

    }

        private fun loadAllFilesAsync() {
        val filesToDownload = AppUtils.ApiCallManager.apiCallList.map { it.first }
        ReleaseLogger.d("LoadFiles", "Starting to load files: ${filesToDownload.joinToString()}")

        loadingDialog.show()
        val progressJob = lifecycleScope.launch(Dispatchers.Main) {
            var dots = 1
            while (true) {
                loadingDialog.setMessage("${stringXML(R.string.fetching_dataset)}${".".repeat(dots)}")
                dots = if (dots >= 3) 1 else dots + 1
                delay(500)
            }
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    filesToDownload.forEachIndexed { index, fileName ->
                        ReleaseLogger.d("LoadFiles", "Processing file $fileName (${index + 1}/${filesToDownload.size})")
                        val file = File(application.getExternalFilesDir(null), fileName)
                        if (file.exists()) {
                            ReleaseLogger.d("LoadFiles", "File exists: ${file.length()} bytes")
                            decompressFile(file, index == filesToDownload.lastIndex)
                        } else {
                            ReleaseLogger.e("LoadFiles", "File not found: $fileName")
                        }
                    }
                }

                ReleaseLogger.d("LoadFiles", """
                Data loaded:
                - Regionals: ${regionalList.size}
                - Wilayah: ${wilayahList.size}
                - Dept: ${deptList.size}
                - Divisi: ${divisiList.size}
                - Blok: ${blokList.size}
                - TPH: ${tphList?.size ?: 0}
            """.trimIndent())

                dataCacheManager.saveDatasets(
                    regionalList,
                    wilayahList,
                    deptList,
                    divisiList,
                    blokList,
                    tphList!!
                )
            } catch (e: Exception) {
                ReleaseLogger.e("LoadFiles", "Error loading files", e)
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    progressJob.cancel()
                    setupLayout()
                }
            }
        }
    }

    private fun decompressFile(file: File, isLastFile: Boolean) {
        try {
            ReleaseLogger.d("Decompress", "Starting decompression of ${file.name}")

            when (file.name) {
                "datasetTPH.zip" -> {
                    ReleaseLogger.d("Decompress", "Processing large TPH file")
                    handleLargeFileChunked(file, isLastFile)
                }
                else -> {
                    GZIPInputStream(file.inputStream()).use { gzipInputStream ->
                        val decompressedData = gzipInputStream.readBytes()
                        ReleaseLogger.d("Decompress", "Decompressed ${file.name}: ${decompressedData.size} bytes")
                        val jsonString = String(decompressedData, Charsets.UTF_8)
                        parseJsonData(jsonString, isLastFile)
                    }
                }
            }
        } catch (e: Exception) {
            ReleaseLogger.e("Decompress", "Error processing ${file.name}", e)
            throw e
        }
    }


    private fun handleLargeFileChunked(file: File, isLastFile: Boolean) {
        try {
            Log.d("HandleLargeFile", "Starting chunked processing of: ${file.name}")
            val startTime = System.currentTimeMillis()

            // Create a temporary file to store decompressed data
            val tempFile = File(file.parent, "temp_decompressed.json")

            // Step 1: Decompress the file
            GZIPInputStream(file.inputStream().buffered(DEFAULT_BUFFER_SIZE)).use { gzipInputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var totalBytes = 0L
                    var bytesRead: Int

                    while (gzipInputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        if (totalBytes % (10 * 1024 * 1024) == 0L) {
                            Log.d("HandleLargeFile", "Decompressed: ${totalBytes / (1024 * 1024)} MB")
                        }
                    }
                    Log.d("HandleLargeFile", "Total decompressed size: ${totalBytes / (1024 * 1024)} MB")
                }
            }

            // Step 2: Read and parse in a controlled way
            try {
                tempFile.inputStream().bufferedReader().use { reader ->
                    val jsonContent = reader.readText()
                    Log.d("HandleLargeFile", "JSON loaded into memory, size: ${jsonContent.length} chars")

                    val jsonObject = JSONObject(jsonContent)
                    Log.d("HandleLargeFile", "JSON successfully parsed")

                    // Check if this is TPH data
                    if (jsonObject.has("TPHDB")) {
                        Log.d("HandleLargeFile", "Found TPHDB")
                        // Process TPH data directly
                        loadTPHData(jsonObject)
                    } else {
                        // Process other data
                        Log.d("HandleLargeFile", "Processing regular data")
                        parseJsonData(jsonContent, isLastFile)
                    }
                }
            } catch (e: OutOfMemoryError) {
                Log.e("HandleLargeFile", "OutOfMemoryError: ${e.message}")
                System.gc() // Request garbage collection
                e.printStackTrace()
            } catch (e: JSONException) {
                Log.e("HandleLargeFile", "JSON parsing error: ${e.message}")
                e.printStackTrace()
            }

            // Clean up
            tempFile.delete()

            val endTime = System.currentTimeMillis()
            Log.d("HandleLargeFile", "Total processing time: ${endTime - startTime} ms")

        } catch (e: Exception) {
            Log.e("HandleLargeFile", "Error in processing: ${e.message}")
            e.printStackTrace()
        }
    }

//    private fun loadTPHData(jsonObject: JSONObject) {
//        try {
//            Log.d("LoadTPHData", "Starting TPH data processing")
//            val tphArray = jsonObject.getJSONArray("TPHDB")
//            val keyObject = jsonObject.getJSONObject("key")
//            val totalItems = tphArray.length()
//
//            // Increased chunk size for better performance
//            val chunkSize = 5000
//
//            // Pre-allocate the ArrayList with expected capacity
//            val accumulatedTPHList = ArrayList<TPHNewModel>(totalItems)
//
//            // Create Gson instance once
//            val gson = Gson()
//            val type = object : TypeToken<List<TPHNewModel>>() {}.type
//
//            // StringBuilder for JSON transformation
//            val stringBuilder = StringBuilder(chunkSize * 200) // Approximate size per object
//
//            // Process the TPHDB array in larger chunks
//            val chunks = totalItems / chunkSize + (if (totalItems % chunkSize > 0) 1 else 0)
//            for (chunk in 0 until chunks) {
//                val startIndex = chunk * chunkSize
//                val endIndex = minOf(startIndex + chunkSize, totalItems)
//
//                // Create chunk array with pre-allocated capacity
//                val jsonChunk = JSONArray()
//
//                // Build chunk
//                for (i in startIndex until endIndex) {
//                    jsonChunk.put(tphArray.getJSONObject(i))
//                }
//
//                // Transform and parse in one go
//                stringBuilder.setLength(0) // Clear StringBuilder
//                val transformedChunk = transformJsonArray(jsonChunk, keyObject)
//
//                // Parse and add to list
//                val chunkList: List<TPHNewModel> = gson.fromJson(
//                    transformedChunk.toString(),
//                    type
//                )
//
//                accumulatedTPHList.addAll(chunkList)
//
//                // Log progress every 20k items
//                if (startIndex % 20000 == 0) {
//                    val progress = (startIndex.toFloat() / totalItems * 100).toInt()
//                    Log.d("LoadTPHData", "Progress: $progress% ($startIndex/$totalItems)")
//
//                    // Request garbage collection if memory pressure is high
//                    if (Runtime.getRuntime().freeMemory() < Runtime.getRuntime().totalMemory() * 0.2) {
//                        System.gc()
//                    }
//                }
//            }
//
//
//            this.tphList = accumulatedTPHList
//
//            val dateModified = jsonObject.getString("date_modified")
//            prefManager?.setDateModified("TPHDB", dateModified)
//
//            Log.d("LoadTPHData", "Completed processing $totalItems TPH items")
//
//        } catch (e: Exception) {
//            Log.e("LoadTPHData", "Error processing TPH data: ${e.message}")
//            e.printStackTrace()
//        }
//    }


    @SuppressLint("SuspiciousIndentation")
    private fun parseJsonData(jsonString: String, isLastFile: Boolean) {
        try {
            val jsonObject = JSONObject(jsonString)
            val gson = Gson()

            val keyObject = jsonObject.getJSONObject("key")
            val dateModified = jsonObject.getString("date_modified")

            // Parse CompanyCodeDB
            if (jsonObject.has("RegionalDB")) {
                val companyCodeArray = jsonObject.getJSONArray("RegionalDB")

                val dateModified = jsonObject.getString("date_modified")
                val transformedCompanyCodeArray = transformJsonArray(companyCodeArray, keyObject)
                val regionalList: List<RegionalModel> = gson.fromJson(
                    transformedCompanyCodeArray.toString(),
                    object : TypeToken<List<RegionalModel>>() {}.type
                )

                prefManager?.setDateModified("RegionalDB", dateModified) // Store dynamically

                this.regionalList = regionalList
            } else {
                Log.e("ParseJsonData", "RegionalDB key is missing")
            }

            // Parse CompanyCodeDB
            if (jsonObject.has("WilayahDB")) {
                val companyCodeArray = jsonObject.getJSONArray("WilayahDB")
                val transformedCompanyCodeArray = transformJsonArray(companyCodeArray, keyObject)
                val wilayahList: List<WilayahModel> = gson.fromJson(
                    transformedCompanyCodeArray.toString(),
                    object : TypeToken<List<WilayahModel>>() {}.type
                )
                Log.d("ParsedData", "WilayahDB: $wilayahList")
                this.wilayahList = wilayahList
                prefManager?.setDateModified("WilayahDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "WilayahDB key is missing")
            }

            if (jsonObject.has("DeptDB")) {
                val bUnitCodeArray = jsonObject.getJSONArray("DeptDB")
                val transformedBUnitCodeArray = transformJsonArray(bUnitCodeArray, keyObject)
                val deptList: List<DeptModel> = gson.fromJson(
                    transformedBUnitCodeArray.toString(),
                    object : TypeToken<List<DeptModel>>() {}.type
                )
                Log.d("ParsedData", "BUnitCode: $deptList")
                this.deptList = deptList
                prefManager?.setDateModified("DeptDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "DeptDB key is missing")
            }

            // Parse DivisionCodeDB
            if (jsonObject.has("DivisiDB")) {
                val divisionCodeArray = jsonObject.getJSONArray("DivisiDB")
                val transformedDivisionCodeArray = transformJsonArray(divisionCodeArray, keyObject)
                val divisiList: List<DivisiModel> = gson.fromJson(
                    transformedDivisionCodeArray.toString(),
                    object : TypeToken<List<DivisiModel>>() {}.type
                )
                Log.d("ParsedData", "DivisionCode: $divisiList")
                this.divisiList = divisiList
                prefManager?.setDateModified("DivisiDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "DivisiDB key is missing")
            }

            // Parse FieldCodeDB
            if (jsonObject.has("BlokDB")) {
                val fieldCodeArray = jsonObject.getJSONArray("BlokDB")
                val transformedFieldCodeArray = transformJsonArrayInChunks(fieldCodeArray, keyObject)
                val blokList: List<BlokModel> = gson.fromJson(
                    transformedFieldCodeArray.toString(),
                    object : TypeToken<List<BlokModel>>() {}.type
                )
                Log.d("ParsedData", "FieldCode: $blokList")
                this.blokList = blokList
                prefManager?.setDateModified("BlokDB", dateModified) // Store dynamically
            } else {
                Log.e("ParseJsonData", "BlokDB key is missing")
            }


//            if (isLastFile) {
//                Log.d("ParsedData", "masuk ges ? ")
//                loadTPHData(jsonObject)
//            }

            // Cache lightweight data
            this.regionalList = regionalList
            this.wilayahList = wilayahList
            this.deptList = deptList
            this.divisiList = divisiList
            this.blokList = blokList



        } catch (e: JSONException) {
            Log.e("ParseJsonData", "Error parsing JSON: ${e.message}")
        }
    }

    fun transformJsonArray(jsonArray: JSONArray, keyObject: JSONObject): JSONArray {
        val transformedArray = JSONArray()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val transformedItem = JSONObject()

            keyObject.keys().forEach { key ->
                val fieldName = keyObject.getString(key)  // This gets the field name from the key object
                val fieldValue = item.get(key)  // This gets the corresponding value from the item
                transformedItem.put(fieldName, fieldValue)
            }

            transformedArray.put(transformedItem)
        }

        return transformedArray
    }


    fun transformJsonArrayInChunks(jsonArray: JSONArray, keyObject: JSONObject): JSONArray {
        val transformedArray = JSONArray()
        val chunkSize = 30 // Adjust this based on your needs

        try {
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val transformedItem = JSONObject()

                keyObject.keys().forEach { key ->
                    val fieldName = keyObject.getString(key)
                    val fieldValue = item.get(key)
                    transformedItem.put(fieldName, fieldValue)
                }

                transformedArray.put(transformedItem)

                // After each chunk is processed, suggest garbage collection
                if (i % chunkSize == 0) {
                    System.gc()
                }
            }
        } catch (e: Exception) {
            Log.e("Transform", "Error transforming array: ${e.message}")
        }

        return transformedArray
    }




    private fun loadTPHData(jsonObject: JSONObject) {
        try {
            if (jsonObject.has("TPHDB")) {
                Log.d("testing", "masuk sini ges")
                val tphArray = jsonObject.getJSONArray("TPHDB")
                val keyObject = jsonObject.getJSONObject("key")
                val chunkSize = 50 // Adjust the chunk size as needed

                // Create a mutable list to accumulate all TPHNewModel objects
                val accumulatedTPHList = mutableListOf<TPHNewModel>()

                // Process the TPHDB array in chunks
                for (i in 0 until tphArray.length() step chunkSize) {
                    val chunk = JSONArray()
                    for (j in i until (i + chunkSize).coerceAtMost(tphArray.length())) {
                        chunk.put(tphArray.getJSONObject(j))
                    }

                    // Transform the current chunk
                    val transformedChunk = transformJsonArray(chunk, keyObject)

                    // Parse the transformed chunk into a list of TPHNewModel
                    val chunkList: List<TPHNewModel> = Gson().fromJson(
                        transformedChunk.toString(),
                        object : TypeToken<List<TPHNewModel>>() {}.type
                    )

                    accumulatedTPHList.addAll(chunkList)

                    Log.d("LoadTPHData", "Processed chunk: $i to ${(i + chunkSize - 1).coerceAtMost(tphArray.length() - 1)}")
                }


                // Assign the accumulated list to the lazy-loaded tphList variable
                this.tphList = accumulatedTPHList
                val dateModified = jsonObject.getString("date_modified")

                prefManager?.setDateModified("TPHDB", dateModified) // Store dynamically
                Log.d("ParsedData", "Total TPHDB items: ${tphList?.size ?: 0}")
            } else {
                Log.e("LoadTPHData", "TPHDB key is missing")
            }
        } catch (e: JSONException) {
            Log.e("LoadTPHData", "Error processing TPH data: ${e.message}")
        }
    }





    // Modified setupSpinnerView to handle selection changes properly
    private fun setupSpinnerView(layoutBinding: PertanyaanSpinnerLayoutBinding, data: List<String>) {
        with(layoutBinding) {

            spHomeMarkerTPH.visibility = View.VISIBLE
            etHomeMarkerTPH.visibility = View.GONE

            spHomeMarkerTPH.setItems(data)
            spHomeMarkerTPH.setOnItemSelectedListener { _, position, _, item ->
                tvError.visibility = View.GONE
                MCVSpinner.strokeColor = ContextCompat.getColor(root.context, R.color.graytextdark)

                when (tvTitleForm.text.toString()) {
                    stringXML(R.string.field_regional) -> {

                        resetViewsBelow(binding.layoutRegional)
                        selectedRegional = item.toString()

                        val selectedRegionalId = regionalList.find { it.nama == selectedRegional }?.id
                        selectedRegionalValue = selectedRegionalId
                        selectedRegionalSpinnerIndex = position

                        if (selectedRegionalId != null) {
                            val filteredWilayahList = wilayahList.filter{
                                it.regional == selectedRegionalId}
                            val wilayahNames = filteredWilayahList.map { it.nama }

                            Log.d("Testing", wilayahList.toString())
                            Log.d("testing", wilayahNames.toString())
                            if (wilayahNames.isNotEmpty()) {
                                setupSpinnerView(binding.layoutWilayah, wilayahNames)
                                binding.layoutWilayah.root.visibility = View.VISIBLE
                            } else {
                                binding.layoutWilayah.root.visibility = View.GONE
                            }
                        } else {
                            binding.layoutWilayah.root.visibility = View.GONE
                        }
                    }
                    stringXML(R.string.field_wilayah) -> {
                        resetViewsBelow(binding.layoutWilayah)
                        selectedWilayah = item.toString()

                        val selectedWilayahId = wilayahList.find {
                            it.regional == selectedRegionalValue
                            it.nama == selectedWilayah
                        }?.id
                        selectedWilayahValue = selectedWilayahId
                        selectedWilayahSpinnerIndex = position

                        if (selectedWilayahId != null) {
                            val filteredDeptList = deptList.filter {
                                it.regional == selectedRegionalValue &&
                                        it.wilayah == selectedWilayahValue }
                            val deptCodeNames = filteredDeptList.map { it.nama }


                            if (deptCodeNames.isNotEmpty()) {
                                setupSpinnerView(binding.layoutEstate, deptCodeNames)
                                binding.layoutEstate.root.visibility = View.VISIBLE
                            } else {
                                binding.layoutEstate.root.visibility = View.GONE
                            }
                        } else {
                            binding.layoutEstate.root.visibility = View.GONE
                        }
                    }
                    stringXML(R.string.field_estate) -> {
                        selectedEstateValue = null
                        resetViewsBelow(binding.layoutEstate)
                        selectedEstate = item.toString()
                        val selectedEstateId = deptList.find {
                                    it.regional == selectedRegionalValue &&
                                    it.wilayah == selectedWilayahValue &&
                                    it.nama == selectedEstate
                                    }
                            ?.id

                        selectedEstateValue = selectedEstateId
                        selectedEstateSpinnerIndex = position

                        if (selectedEstateId != null){
                            val filteredDivisiList = divisiList.filter{it.dept == selectedEstateId}

                            val divisiCodeNames = filteredDivisiList.map{it.abbr}
                            if (divisiCodeNames.isNotEmpty()){
                                setupSpinnerView(binding.layoutAfdeling, divisiCodeNames)
                                binding.layoutAfdeling.root.visibility = View.VISIBLE
                            }else{
                                binding.layoutAfdeling.root.visibility = View.GONE
                            }
                        }else{
                            binding.layoutAfdeling.root.visibility = View.GONE
                        }

                    }
                    stringXML(R.string.field_afdeling) -> {
                        resetViewsBelow(binding.layoutAfdeling)
                        selectedAfdeling = item.toString()
                        val selectedDivisiId = divisiList.find {
                            it.abbr == selectedAfdeling &&
                                    it.dept == selectedEstateValue  }?.id
                        selectedDivisionSpinnerIndex = position
                        selectedDivisiValue = selectedDivisiId

                        Log.d("testing", selectedRegionalValue.toString())
                        Log.d("testing", selectedEstateValue.toString())
                        Log.d("testing", selectedDivisiValue.toString())

                        if (selectedDivisiId != null) {
                            if (selectedRegionalValue == 3) {
                                // Hanya memfilter berdasarkan estateAbbr
                                val estateAbbr = deptList.find { it.id == selectedEstateValue }?.abbr
                                val filteredBlokList = blokList.filter {
                                    it.regional == selectedRegionalValue &&
                                            it.dept == selectedEstateValue &&
                                            it.dept_abbr == estateAbbr
                                }

                                val tahunTanamList = filteredBlokList.map { it.tahun }.distinct().sorted()

                                if (tahunTanamList.isNotEmpty()) {
                                    setupSpinnerView(binding.layoutTahunTanam, tahunTanamList)
                                    binding.layoutTahunTanam.root.visibility = View.VISIBLE
                                } else {
                                    binding.layoutTahunTanam.root.visibility = View.GONE
                                }
                            } else {
                                // Memfilter berdasarkan divisi saja
                                val filteredBlokList = blokList.filter {
                                    it.regional == selectedRegionalValue &&
                                            it.dept == selectedEstateValue &&
                                            it.divisi == selectedDivisiId
                                }

                                val tahunTanamList = filteredBlokList.map { it.tahun }.distinct().sorted()

                                if (tahunTanamList.isNotEmpty()) {
                                    setupSpinnerView(binding.layoutTahunTanam, tahunTanamList)
                                    binding.layoutTahunTanam.root.visibility = View.VISIBLE
                                } else {
                                    binding.layoutTahunTanam.root.visibility = View.GONE
                                }
                            }
                        } else {
                            binding.layoutBlok.root.visibility = View.GONE
                        }

                    }

                    stringXML(R.string.field_tahun_tanam) -> {
                        val selectedTahunTanam = item.toString()
                        resetViewsBelow(binding.layoutTahunTanam)
                        selectedTahunTanamValue = selectedTahunTanam

                        if (selectedRegionalValue == 3) {
                            // Filter berdasarkan estateAbbr saja
                            val estateAbbr = deptList.find { it.id == selectedEstateValue }?.abbr
                            val filteredBlokCodes = blokList.filter {
                                it.regional == selectedRegionalValue &&
                                        it.dept == selectedEstateValue &&
                                        it.dept_abbr == estateAbbr &&
                                        it.tahun == selectedTahunTanamValue
                            }

                            if (filteredBlokCodes.isNotEmpty()) {
                                val blokNames = filteredBlokCodes.map { it.kode }
                                setupSpinnerView(binding.layoutBlok, blokNames)
                                binding.layoutBlok.root.visibility = View.VISIBLE
                            } else {
                                binding.layoutBlok.root.visibility = View.GONE
                            }
                        } else {
                            // Filter berdasarkan divisi saja
                            val filteredBlokCodes = blokList.filter {
                                it.regional == selectedRegionalValue &&
                                        it.dept == selectedEstateValue &&
                                        it.divisi == selectedDivisiValue &&
                                        it.tahun == selectedTahunTanamValue
                            }

                            if (filteredBlokCodes.isNotEmpty()) {
                                val blokNames = filteredBlokCodes.map { it.kode }
                                setupSpinnerView(binding.layoutBlok, blokNames)
                                binding.layoutBlok.root.visibility = View.VISIBLE
                            } else {
                                binding.layoutBlok.root.visibility = View.GONE
                            }
                        }
                    }

                    stringXML(R.string.field_blok) -> {
                        resetViewsBelow(binding.layoutBlok)
                        binding.layoutAncak.root.visibility = View.VISIBLE
                        selectedBlok = item.toString()
                        selectedFieldCodeSpinnerIndex = position

                        if (selectedRegionalValue == 3) {
                            val estateAbbr = deptList.find { it.id == selectedEstateValue }?.abbr

                            val selectedFieldId = blokList.find { blok ->
                                blok.regional == selectedRegionalValue &&
                                        blok.dept == selectedEstateValue &&
                                        blok.dept_abbr == estateAbbr &&
                                        blok.tahun == selectedTahunTanamValue &&
                                        blok.kode == selectedBlok
                            }?.id
                            selectedBlokValue = selectedFieldId

                            lifecycleScope.launch {
                                val filteredTPH = withContext(Dispatchers.Default) {
                                    tphList?.filter { tph ->
                                        tph.regional == selectedRegionalValue &&
                                                tph.dept == selectedEstateValue &&
                                                tph.dept_abbr == estateAbbr &&
                                                tph.tahun == selectedTahunTanamValue &&
                                                tph.blok == selectedBlokValue
                                    }
                                }

                                // Menangani hasil filteredTPH
                                val mbSave = findViewById<MaterialButton>(R.id.mbSaveDataTPH)
                                if (!filteredTPH.isNullOrEmpty()) {
                                    val tphNumbers = filteredTPH.map { it.nomor }
                                    setupSpinnerView(binding.layoutTPH, tphNumbers)
                                    binding.layoutTPH.root.visibility = View.VISIBLE
                                    mbSave.visibility = View.VISIBLE
                                } else {
                                    findViewById<LinearLayout>(R.id.layoutTPH).visibility = View.VISIBLE
                                    mbSave.visibility = View.GONE
                                }
                            }
                        } else {
                            val selectedFieldId = blokList.find { blok ->
                                blok.regional == selectedRegionalValue &&
                                        blok.dept == selectedEstateValue &&
                                        blok.divisi == selectedDivisiValue &&
                                        blok.tahun == selectedTahunTanamValue &&
                                        blok.kode == selectedBlok
                            }?.id
                            selectedBlokValue = selectedFieldId

                            lifecycleScope.launch {
                                val filteredTPH = withContext(Dispatchers.Default) {
                                    tphList?.filter { tph ->
                                        tph.regional == selectedRegionalValue &&
                                                tph.dept == selectedEstateValue &&
                                                tph.divisi == selectedDivisiValue &&
                                                tph.tahun == selectedTahunTanamValue &&
                                                tph.blok == selectedBlokValue
                                    }
                                }

                                // Menangani hasil filteredTPH
                                val mbSave = findViewById<MaterialButton>(R.id.mbSaveDataTPH)
                                if (!filteredTPH.isNullOrEmpty()) {
                                    val tphNumbers = filteredTPH.map { it.nomor }
                                    setupSpinnerView(binding.layoutTPH, tphNumbers)
                                    binding.layoutTPH.root.visibility = View.VISIBLE
                                    mbSave.visibility = View.VISIBLE
                                } else {
                                    findViewById<LinearLayout>(R.id.layoutTPH).visibility = View.VISIBLE
                                    mbSave.visibility = View.GONE
                                }
                            }
                        }
                    }


//                    stringXML(R.string.field_blok) -> {
//                        resetViewsBelow(binding.layoutBlok)
//                        binding.layoutAncak.root.visibility = View.VISIBLE
//                        selectedBlok = item.toString()
//                        selectedFieldCodeSpinnerIndex = position
//
//                        // Fetch the estate abbreviation for fallback filtering
//                        val estateAbbr = deptList.find { it.id == selectedEstateValue }?.abbr
//
//                        // Find the selected field ID using either divisi OR estate abbreviation
//                        val selectedFieldId = blokList.find { blok ->
//                            blok.regional == selectedRegionalValue &&
//                                    blok.dept == selectedEstateValue &&
//                                    (blok.divisi == selectedDivisiValue || blok.dept_abbr == estateAbbr)  &&
//                                    blok.tahun == selectedTahunTanamValue &&
//                                    blok.kode == selectedBlok
//
//                        }?.id
//                        selectedBlokValue = selectedFieldId
//
//                        // Debug logs to track selected values
//                        Log.d("testing", selectedRegionalValue.toString())
//                        Log.d("testing", selectedEstateValue.toString())
//                        Log.d("testing", selectedDivisiValue.toString())
//                        Log.d("testing", selectedBlok.toString())
//                        Log.d("testing", selectedBlokValue.toString())
//                        Log.d("testing", selectedTahunTanamValue.toString())
//
//                        lifecycleScope.launch {
//                            val filteredTPH = withContext(Dispatchers.Default) {
//                                tphList?.filter { tph ->
//                                    tph.regional == selectedRegionalValue &&
//                                            tph.dept == selectedEstateValue &&
//                                            (tph.divisi == selectedDivisiValue || tph.dept_abbr == estateAbbr) && // Highlighted OR condition
//                                            tph.tahun == selectedTahunTanamValue &&
//                                            tph.blok == selectedBlokValue
//                                }
//                            }
//
//                            // Log the filtered TPH list for debugging
//                            Log.d("testing", filteredTPH.toString())
//
//                            val mbSave = findViewById<MaterialButton>(R.id.mbSaveDataTPH)
//                            if (!filteredTPH.isNullOrEmpty()) {
//                                val tphNumbers = filteredTPH.map { it.nomor }
//                                setupSpinnerView(binding.layoutTPH, tphNumbers)
//                                binding.layoutTPH.root.visibility = View.VISIBLE
//                                mbSave.visibility = View.VISIBLE
//                            } else {
//                                findViewById<LinearLayout>(R.id.layoutTPH).visibility = View.VISIBLE
//                                mbSave.visibility = View.GONE
//                            }
//                        }
//                    }

                    stringXML(R.string.field_tph) -> {
                        selectedTPH = item.toString()
                        selectedTPHSpinnerIndex = position

                        val materialCardView = findViewById<MaterialCardView>(R.id.cardKoordinatTerdaftar)
                        val materialCardViewTPHKoorSalah = findViewById<MaterialCardView>(R.id.cardKoordinatKurangTepat)
                        val detectUserInput = findViewById<TextView>(R.id.detect_user_input)
                        val detectTanggalInput = findViewById<TextView>(R.id.detect_tanggal_input)
                        val detectLatInput = findViewById<TextView>(R.id.detect_lat_input)
                        val detectLonInput = findViewById<TextView>(R.id.detect_lon_input)

                        val estateAbbr = deptList.find { it.id == selectedEstateValue }?.abbr

                        // Filter TPH berdasarkan kondisi
                        val selectedTPHId = if (selectedRegionalValue == 3) {
                            // Kondisi menggunakan estateAbbr
                            tphList!!.find {
                                it.regional == selectedRegionalValue &&
                                        it.dept == selectedEstateValue &&
                                        it.dept_abbr == estateAbbr &&
                                        it.blok == selectedBlokValue &&
                                        it.tahun == selectedTahunTanamValue &&
                                        it.nomor == selectedTPH
                            }
                        } else {
                            // Kondisi menggunakan divisi
                            tphList!!.find {
                                it.regional == selectedRegionalValue &&
                                        it.dept == selectedEstateValue &&
                                        it.divisi == selectedDivisiValue &&
                                        it.blok == selectedBlokValue &&
                                        it.tahun == selectedTahunTanamValue &&
                                        it.nomor == selectedTPH
                            }
                        }

                        selectedTPHValue = selectedTPHId?.id
                        val selectedTPHLat = selectedTPHId?.lat
                        val selectedTPHLon = selectedTPHId?.lon
                        val selectedTPHUserInput = selectedTPHId?.user_input
                        val selectedTPHUpdateDate = selectedTPHId?.update_date?.let { AppUtils.formatDateToIndonesian(it) }
                        val selectedTPHStatus = selectedTPHId?.status

                        val latPattern = "^-?([0-8]?[0-9]|90)\\.\\d+$".toRegex()
                        val lonPattern = "^-?((1[0-7][0-9])|([0-9]?[0-9]))\\.\\d+$".toRegex()

                        // Menampilkan informasi koordinat
                        if (selectedTPHLat?.matches(latPattern) == true &&
                            selectedTPHLon?.matches(lonPattern) == true
                        ) {
                            detectLatInput.text = "Latitude: $selectedTPHLat"
                            detectLonInput.text = "Longitude: $selectedTPHLon"
                            val formattedInput = selectedTPHUserInput.orEmpty().replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                            }
                            detectUserInput.text = "User Input: $formattedInput"
                            detectTanggalInput.text = "Last Update: $selectedTPHUpdateDate"
                            materialCardView.visibility = View.VISIBLE
                        } else {
                            materialCardView.visibility = View.GONE
                        }

                        // Menampilkan status TPH
                        if (selectedTPHStatus == "2") {
                            materialCardViewTPHKoorSalah.visibility = View.VISIBLE
                        } else {
                            materialCardViewTPHKoorSalah.visibility = View.GONE
                        }
                    }


//                    stringXML(R.string.field_tph)->{
//                        selectedTPH = item.toString()
//                        selectedTPHSpinnerIndex = position
//
//                        val materialCardView = findViewById<MaterialCardView>(R.id.cardKoordinatTerdaftar)
//                        val materialCardViewTPHKoorSalah = findViewById<MaterialCardView>(R.id.cardKoordinatKurangTepat)
//                        val detectUserInput = findViewById<TextView>(R.id.detect_user_input)
//                        val detectTanggalInput = findViewById<TextView>(R.id.detect_tanggal_input)
//                        val detectLatInput = findViewById<TextView>(R.id.detect_lat_input)
//                        val detectLonInput = findViewById<TextView>(R.id.detect_lon_input)
//
//                        val estateAbbr = deptList.find { it.id == selectedEstateValue }?.abbr
//
//                        val selectedTPHId = tphList!!.find {
//                            it.regional == selectedRegionalValue &&
//                                    it.dept == selectedEstateValue &&
//                                    (it.divisi == selectedDivisiValue || it.dept_abbr == estateAbbr)
//                                    it.blok == selectedBlokValue &&
//                                    it.tahun == selectedTahunTanamValue &&
//                                    it.nomor == selectedTPH
//                        }
//
//                        selectedTPHValue = selectedTPHId?.id
//                        val selectedTPHLat = selectedTPHId?.lat
//                        val selectedTPHLon = selectedTPHId?.lon
//                        val selectedTPHUserInput = selectedTPHId?.user_input
//                        val selectedTPHUpdateDate =AppUtils.formatDateToIndonesian(selectedTPHId?.update_date!!)
//                        val selectedTPHStatus = selectedTPHId?.status
//
//
//
//                        val latPattern = "^-?([0-8]?[0-9]|90)\\.\\d+$".toRegex()
//                        val lonPattern = "^-?((1[0-7][0-9])|([0-9]?[0-9]))\\.\\d+$".toRegex()
//
//                        if (selectedTPHLat?.matches(latPattern) == true &&
//                            selectedTPHLon?.matches(lonPattern) == true) {
//                            detectLatInput.text = "Latitude: $selectedTPHLat"
//                            detectLonInput.text = "Longitude: $selectedTPHLon"
//                            val selectedTPHUserInput = selectedTPHUserInput.toString() // Example input
//                            val formattedInput = selectedTPHUserInput.replaceFirstChar {
//                                if (it.isLowerCase()) it.titlecase() else it.toString()
//                            }
//                            detectUserInput.text = "User Input: $formattedInput"
//                            detectTanggalInput.text = "Last Update: $selectedTPHUpdateDate"
//                            materialCardView.visibility = View.VISIBLE
//                        } else {
//                            materialCardView.visibility = View.GONE
//                        }
//
//                        if (selectedTPHStatus == "2") {
//                            materialCardViewTPHKoorSalah.visibility = View.VISIBLE
//                        }else{
//                            materialCardViewTPHKoorSalah.visibility = View.GONE
//                        }
//
//                    }
                }
            }
        }
    }


    fun resetViewsBelow(triggeredLayout: PertanyaanSpinnerLayoutBinding) {
        val mbSave = findViewById<MaterialButton>(R.id.mbSaveDataTPH)
        mbSave.visibility = View.GONE
        val materialCardView = findViewById<MaterialCardView>(R.id.cardKoordinatTerdaftar)
        materialCardView.visibility = View.GONE
        val materialCardViewTPHKoorSalah = findViewById<MaterialCardView>(R.id.cardKoordinatKurangTepat)
        materialCardViewTPHKoorSalah.visibility = View.GONE
        findViewById<LinearLayout>(R.id.layoutAncak).visibility = View.GONE
        when (triggeredLayout) {
            binding.layoutRegional -> {
                clearSpinnerView(binding.layoutWilayah, ::resetSelectedWilayahCode)
                clearSpinnerView(binding.layoutEstate, ::resetSelectedEstateCode)
                clearSpinnerView(binding.layoutAfdeling, ::resetSelectedDivisionCode)
                clearSpinnerView(binding.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(binding.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
            binding.layoutWilayah -> {
                clearSpinnerView(binding.layoutEstate, ::resetSelectedEstateCode)
                clearSpinnerView(binding.layoutAfdeling, ::resetSelectedDivisionCode)
                clearSpinnerView(binding.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(binding.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
            binding.layoutEstate -> {
                clearSpinnerView(binding.layoutAfdeling, ::resetSelectedDivisionCode)
                clearSpinnerView(binding.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(binding.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
            binding.layoutAfdeling -> {
                clearSpinnerView(binding.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(binding.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
            binding.layoutTahunTanam -> {
                clearSpinnerView(binding.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
            binding.layoutBlok -> {
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
        }
    }

    fun clearSpinnerView(layoutBinding: PertanyaanSpinnerLayoutBinding, resetSelectedValue: () -> Unit) {
        layoutBinding.root.visibility = if (layoutBinding != binding.layoutWilayah) View.GONE else View.VISIBLE
        setupSpinnerView(layoutBinding, emptyList()) // Reset spinner with an empty list
        resetSelectedValue() // Reset the associated selected value
    }


    fun resetSelectedWilayahCode() {
        selectedWilayahValue = null
    }


    fun resetSelectedEstateCode() {
        selectedEstateValue = null
    }


    fun resetSelectedDivisionCode() {
        selectedDivisiValue = null
    }

    fun resetSelectedTahunTanam() {
        selectedTahunTanamValue = null
    }

    fun resetSelectedFieldCode() {
        selectedBlokValue = null
    }

    fun resetSelectedTPH() {
        selectedTPHValue = null
        selectedTPH = ""
    }

    private fun setupEditTextView(layoutBinding: PertanyaanSpinnerLayoutBinding) {
        with(layoutBinding) {
            spHomeMarkerTPH.visibility = View.GONE
            etHomeMarkerTPH.visibility = View.VISIBLE
            // Set input type based on layout
            when (layoutBinding) {
                binding.layoutAncak -> {
                    etHomeMarkerTPH.inputType = AndroidInputType.TYPE_CLASS_NUMBER
                }
                else -> {
                    etHomeMarkerTPH.inputType = AndroidInputType.TYPE_CLASS_TEXT
                }
            }

            if (layoutBinding == binding.layoutUserInput) {
                val savedUserInput = prefManager?.user_input ?: ""
                if (savedUserInput.isNotEmpty()) {
                    etHomeMarkerTPH.setText(savedUserInput)
                    userInput = savedUserInput
                }

                val savedAncakInput = prefManager?.ancak_input ?: ""
                if (savedAncakInput.isNotEmpty()) {
                    etHomeMarkerTPH.setText(savedAncakInput)
                    ancakInput = savedAncakInput
                }
            }

            etHomeMarkerTPH.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val imm = application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    binding.layoutEstate.spHomeMarkerTPH.requestFocus()
                    true
                } else {
                    false
                }
            }

            etHomeMarkerTPH.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tvError.visibility = View.GONE
                    MCVSpinner.strokeColor = ContextCompat.getColor(root.context, R.color.graytextdark)

                    when (layoutBinding) {
                        binding.layoutUserInput -> {
                            userInput = s.toString()
                            Log.d("EditText", "UserInput updated: $userInput")
                        }
                        binding.layoutAncak -> {
                            ancakInput = s.toString()
                            Log.d("EditText", "AncakInput updated: $ancakInput")
                        }
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }


    private fun validateAndShowErrors(): Boolean {



        Log.d("testing", selectedTPH.toString())
        Log.d("testing", selectedTPHValue.toString())

        var isValid = true
        val missingFields = mutableListOf<String>()

        if (!locationEnable || lat == 0.0 || lon == 0.0) {
            isValid = false
            this.vibrate()
            AlertDialogUtility.withSingleAction(
                this,
                stringXML(R.string.al_back),
                stringXML(R.string.al_location_not_ready),
                stringXML(R.string.al_location_description_failed),
                "warning.json",
                R.color.colorRedDark
            ) {

                locationViewModel.refreshLocationStatus()
            }
            return false
        }

        Log.d("Validation", "Starting validation for ${inputMappings.size} fields")

        inputMappings.forEach { (layoutBinding, key, inputType) ->
            Log.d("Validation", "Validating $key with type $inputType")

            val isEmpty = when (inputType) {
                InputType.SPINNER -> {
                    when (layoutBinding) {
                        binding.layoutEstate -> selectedEstate.isEmpty()
                        binding.layoutAfdeling -> selectedAfdeling.isEmpty()
                        binding.layoutTahunTanam -> selectedTahunTanamValue!!.isEmpty()
                        binding.layoutBlok -> selectedBlok.isEmpty()
                        binding.layoutTPH -> selectedTPH.isEmpty()
                        else -> layoutBinding.spHomeMarkerTPH.selectedIndex == -1
                    }
                }
                InputType.EDITTEXT -> {
                    when (key) {
                        "User Input" -> userInput.trim().isEmpty()
                        "Ancak Input" -> ancakInput.trim().isEmpty()
                        else -> layoutBinding.etHomeMarkerTPH.text.toString().trim().isEmpty()
                    }
                }
            }

            if (isEmpty) {
                layoutBinding.tvError.visibility = View.VISIBLE
                layoutBinding.MCVSpinner.strokeColor = ContextCompat.getColor(
                    this,
                    R.color.colorRedDark
                )
                missingFields.add(key)
                isValid = false
                Log.d("Validation", "Field $key is empty")
            } else {
                layoutBinding.tvError.visibility = View.GONE
                layoutBinding.MCVSpinner.strokeColor = ContextCompat.getColor(
                    this,
                    R.color.graytextdark
                )
                Log.d("Validation", "Field $key is valid")
            }
        }

        if (!isValid) {
            vibrate()
            AlertDialogUtility.withSingleAction(
                this,
                stringXML(R.string.al_back),
                stringXML(R.string.al_data_not_completed),
                "${stringXML(R.string.al_pls_complete_data)}",
                "warning.json",
                R.color.colorRedDark
            ) {}
        }

        Log.d("Validation", "Final validation result: $isValid")
        return isValid
    }

    private fun showExitDialog() {
        AlertDialogUtility.withTwoActions(
            this,
            stringXML(R.string.al_yes),
            stringXML(R.string.confirmation_dialog_title),
            stringXML(R.string.al_confirm_out),
            "warning.json"
        ) {
            finish()
        }
    }

    private fun getDeviceInfo(context: Context): JSONObject {
        val json = JSONObject()

        val appVersion = context.getString(R.string.app_version)

        json.put("app_version", appVersion)
        json.put("os_version", Build.VERSION.RELEASE)
        json.put("device_model", Build.MODEL)

        return json
    }


    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        locationViewModel.refreshLocationStatus()
        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(
                LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    interval = 5000 // Check every 5 seconds
                },
                locationSettingsCallback,
                null
            )


        locationViewModel.locationPermissions.observe(this) { isLocationEnabled ->
            if (!isLocationEnabled) {
                requestLocationPermission()
            } else {
                locationViewModel.startLocationUpdates()
            }
        }

        locationViewModel.locationIconState.observe(this) { isEnabled ->
            binding.statusLocation.setImageResource(R.drawable.baseline_location_pin_24)
            binding.statusLocation.imageTintList = ColorStateList.valueOf(
                resources.getColor(
                    if (isEnabled) R.color.greenbutton else R.color.colorRed
                )
            )
        }

        locationViewModel.locationData.observe(this) { location ->
            locationEnable = true
            lat = location.latitude
            lon = location.longitude
        }

        locationViewModel.locationAccuracy.observe(this) { accuracy ->
            binding.accuracyLocation.text = String.format("%.1f m", accuracy)

            currentAccuracy = accuracy
        }
    }


    private fun updateTextInPertanyaan(layoutBinding: PertanyaanSpinnerLayoutBinding, text: String) {
        layoutBinding.tvTitleForm.text = text
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showSnackbar(stringXML(R.string.location_permission_message))
            isPermissionRationaleShown = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        // Check internet capability and perform ping
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && pingGoogle()
    }

    /**
     * Pings Google to verify internet connectivity.
     */
    private fun pingGoogle(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 www.google.com")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            Log.e("PingGoogle", "Ping failed: ${e.message}")
            false
        }
    }

    private fun startFileDownload() {

        if (!isInternetAvailable()) {
            AlertDialogUtility.withSingleAction(
                this,
                stringXML(R.string.al_back),
                stringXML(R.string.al_no_internet_connection),
                stringXML(R.string.al_no_internet_connection_description_download_dataset),
                "network_error.json",
                R.color.colorRedDark
            ) {}
            return
        }

        lifecycleScope.launch {
            // Inflate dialog layout
            val dialogView = layoutInflater.inflate(R.layout.list_card_upload, null)
            val alertDialog = AlertDialog.Builder(this@HomeActivity)
                .setCancelable(false)
                .setView(dialogView)
                .create()

            alertDialog.show()
            alertDialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.features_recycler_view)
            recyclerView?.layoutManager = LinearLayoutManager(this@HomeActivity, LinearLayoutManager.VERTICAL, false)

            // Get saved file list and determine which files need downloading
            val savedFileList = prefManager!!.getFileList().let { list ->
                if (list.isEmpty() || list.size != AppUtils.ApiCallManager.apiCallList.size) {
                    // If empty or wrong size, create new list with correct size
                    MutableList<String?>(AppUtils.ApiCallManager.apiCallList.size) { index ->
                        // Copy existing values if any, null otherwise
                        list.getOrNull(index)
                    }
                } else {
                    list.toMutableList()
                }
            }

            val filesToDownload = AppUtils.ApiCallManager.apiCallList.filterIndexed { index, pair ->
                val fileName = pair.first
                val file = File(this@HomeActivity.getExternalFilesDir(null), fileName)
                val needsDownload = savedFileList.getOrNull(index) == null || !file.exists() ||  filesToUpdate.contains(fileName)
                Log.d("FileDownload", "File: $fileName, Needs download: $needsDownload")
                needsDownload
            }


//            Log.d("testing", filesToDownload.toString())

            val apiCallsSize = filesToDownload.size
            if (apiCallsSize == 0) {
                Log.d("FileDownload", "No files need downloading")
                alertDialog.dismiss()
                return@launch
            }

            val progressList = MutableList(apiCallsSize) { 0 }
            val statusList = MutableList(apiCallsSize) { "Menunggu" }
            val iconList = MutableList(apiCallsSize) { R.id.progress_circular_loading }
            val fileNames = filesToDownload.map { it.first }

            val progressAdapter = ProgressUploadAdapter(progressList, statusList, iconList, fileNames.toMutableList())
            recyclerView?.adapter = progressAdapter

            val titleTextView = dialogView.findViewById<TextView>(R.id.tvTitleProgressBarLayout)
            val counterTextView = dialogView.findViewById<TextView>(R.id.counter_dataset)
            counterTextView.text = "0 / $apiCallsSize"

            lifecycleScope.launch(Dispatchers.Main) {
                var dots = 0
                while (alertDialog.isShowing) {
                    titleTextView.text = "Mengunduh Dataset" + ".".repeat(dots)
                    dots = if (dots >= 4) 1 else dots + 1
                    delay(500)
                }
            }

            for (i in 0 until apiCallsSize) {
                withContext(Dispatchers.Main) {
                    progressAdapter.updateProgress(i, 0, "Menunggu", R.id.progress_circular_loading)
                }
            }

            var completedCount = 0
            val downloadsDir = this@HomeActivity.getExternalFilesDir(null)

            for ((index, apiCall) in filesToDownload.withIndex()) {
                val fileName = apiCall.first


                val apiCallFunction = apiCall.second
                val originalIndex = AppUtils.ApiCallManager.apiCallList.indexOfFirst { it.first == fileName }

                withContext(Dispatchers.Main) {
                    progressAdapter.resetProgress(index)
                    progressAdapter.updateProgress(index, 0, "Sedang Mengunduh", R.id.progress_circular_loading)
                }

                for (progress in 0..100 step 10) {
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, progress, "Sedang Mengunduh", R.id.progress_circular_loading)
                    }
                }

                val (isSuccessful, message) = downloadFile(fileName, apiCallFunction, downloadsDir, savedFileList)

                if (isSuccessful) {
                    completedCount++
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, 100, message, R.drawable.baseline_check_24)
                    }
                    savedFileList[originalIndex] = fileName
                } else {
                    withContext(Dispatchers.Main) {
                        progressAdapter.updateProgress(index, 100, message, R.drawable.baseline_close_24)
                    }
                    savedFileList[originalIndex] = null
                }

                withContext(Dispatchers.Main) {
                    counterTextView.text = "$completedCount / $apiCallsSize"
                }
            }
            val cleanedList = savedFileList.toMutableList()
            for (i in cleanedList.indices.reversed()) {
                val fileName = cleanedList[i]
                if (fileName != null) {
                    // Check if this fileName appears earlier in the list
                    val firstIndex = cleanedList.indexOf(fileName)
                    if (firstIndex != i) {
                        // If found earlier, remove this duplicate
                        cleanedList.removeAt(i)
                    }
                }
            }

            prefManager!!.saveFileList(cleanedList)
            val closeText = dialogView.findViewById<TextView>(R.id.close_progress_statement)
            closeText.visibility = View.VISIBLE

            for (i in 3 downTo 1) {
                withContext(Dispatchers.Main) {
                    closeText.text = "Dialog tertutup otomatis dalam ${i} detik"
                    delay(1000)
                }
            }

            loadAllFilesAsync()

            alertDialog.dismiss()
        }
    }

    private suspend fun downloadFile(
        fileName: String,
        apiCall: suspend () -> Response<ResponseBody>,
        downloadsDir: File?,
        fileList: MutableList<String?>
    ): Pair<Boolean, String> {
        return try {
            withContext(Dispatchers.IO) {
                val response = apiCall()
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val file = File(downloadsDir, fileName)
                        try {
                            val success = saveFileToStorage(responseBody, file)
                            if (success) {
                                fileList.add(fileName)
                                Log.d("FileDownload", "$fileName downloaded successfully.")
                                Pair(true, "Unduh Selesai")
                            } else {
                                fileList.add(null)
                                Pair(false, "Gagal menyimpan file")
                            }
                        } catch (e: Exception) {
                            fileList.add(null)
                            Log.e("FileDownload", "Error saving file: ${e.message}")
                            Pair(false, "Unduh Gagal! Gagal menyimpan file")
                        }
                    } else {
                        fileList.add(null)
                        Log.e("FileDownload", "Response body is null.")
                        Pair(false, "Response body kosong")
                    }
                } else {
                    fileList.add(null)
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrEmpty()) {
                            val gson = Gson()
                            try {
                                val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                                errorResponse.message
                            } catch (e: Exception) {
                                "Error code: ${response.code()}"
                            }
                        } else {
                            "Error code: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        Log.e("FileDownload", "Error handling response", e)
                        "Error code: ${response.code()}"
                    }
                    Pair(false, "Unduh Gagal! $errorMessage")
                }
            }
        } catch (e: Exception) {
            fileList.add(null)
            Log.e("FileDownload", "Error downloading file: ${e.message}")
            Pair(false, "Unduh Gagal! ${e.message ?: "Unknown error"}")
        }
    }

    private fun saveFileToStorage(body: ResponseBody, file: File): Boolean {
        return try {
            body.byteStream().use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("FileDownload", "Error saving file: ${e.message}")
            false
        }
    }

    private suspend fun shouldStartFileDownload(): Boolean {
        val savedFileList = prefManager!!.getFileList()
        val downloadsDir = this.getExternalFilesDir(null)

        if (prefManager!!.isFirstTimeLaunch) {
            Log.d("FileCheck", "First time launch detected.")
            prefManager!!.isFirstTimeLaunch = false
            return true
        }

        if (savedFileList.isNotEmpty()) {
            if (savedFileList.contains(null)) {
                Log.e("FileCheck", "Null entries found in savedFileList.")
                return true
            }

            val missingFiles = savedFileList.filterNot { fileName ->
                val file = File(downloadsDir, fileName)
                val exists = file.exists()
                Log.d("FileCheck", "Checking file: ${file.path} -> Exists: $exists")
                fileName != null && exists
            }

            if (missingFiles.isNotEmpty()) {
                Log.e("FileCheck", "Missing files detected: $missingFiles")
                return true
            }
        } else {
            Log.d("FileCheck", "Saved file list is empty.")
            return true
        }


        if (!isInternetAvailable()) {
            Log.d("NetworkCheck", "No internet connection available")
            filesToUpdate.clear()  // Clear any pending updates
            return false
        }

        val shouldDownload = checkServerDates()
        Log.d("testing", filesToUpdate.toString())
        Log.d("testing", shouldDownload.toString())

        return shouldDownload
    }

    private suspend fun checkServerDates(): Boolean {
        try {
            filesToUpdate.clear()

            val response = RetrofitClient.instance.getTablesLatestModified()
            if (response.isSuccessful && response.body()?.statusCode == 1) {
                val serverData = response.body()?.data ?: return true
                val localData = prefManager?.getAllDateModified() ?: return true

                val keyMapping = mapOf(
                    AppUtils.ApiCallManager.apiCallList[0].first to Pair("regional", "RegionalDB"),
                    AppUtils.ApiCallManager.apiCallList[1].first to Pair("wilayah", "WilayahDB"),
                    AppUtils.ApiCallManager.apiCallList[2].first to Pair("dept", "DeptDB"),
                    AppUtils.ApiCallManager.apiCallList[3].first to Pair("divisi", "DivisiDB"),
                    AppUtils.ApiCallManager.apiCallList[4].first to Pair("blok", "BlokDB"),
                    AppUtils.ApiCallManager.apiCallList[5].first to Pair("tph", "TPHDB")
                )

                keyMapping.forEach { (filename, keys) ->
                    val (serverKey, localKey) = keys
                    val serverDate = serverData[serverKey]
                    val localDate = localData[localKey]

                    Log.d("DateComparison", """
                    Comparing dates for $localKey:
                    Server date ($serverKey): $serverDate
                    Local date ($localKey): $localDate
                """.trimIndent())

                    if (serverDate != null && localDate != null) {
                        val serverDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .parse(serverDate)
                        val localDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .parse(localDate)

                        if (serverDateTime != null && localDateTime != null) {
                            if (serverDateTime.after(localDateTime)) {
                                Log.d("DateComparison", "$localKey needs update: Server date is newer")
                                filesToUpdate.add(filename)
                            }
                        }
                    }
                }

                return filesToUpdate.isNotEmpty()
            }
            return true
        } catch (e: Exception) {
            Log.e("FileCheck", "Error checking server dates", e)
            return true
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        } else {
            lifecycleScope.launch {
                val shouldDownload = shouldStartFileDownload()
                if (shouldDownload) {
                    Log.d("FileCheck", "Starting file download...")
                    startFileDownload()
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            loadingDialog.show()
                            loadingDialog.setMessage("Loading data...")
                        }

                        try {
                            val cachedData = dataCacheManager.getDatasets()

                            if (cachedData != null) {
                                val hasEmptyDatasets =
                                    cachedData.regionalList.isEmpty() || cachedData.wilayahList.isEmpty() ||
                                            cachedData.deptList.isEmpty() ||
                                            cachedData.divisiList.isEmpty() ||
                                            cachedData.blokList.isEmpty() ||
                                            cachedData.tphList.isEmpty()

                                if (hasEmptyDatasets) {
                                    withContext(Dispatchers.Main) {
                                        loadingDialog.dismiss()
                                        loadAllFilesAsync()
                                    }
                                } else {
                                    regionalList = cachedData.regionalList
                                    wilayahList = cachedData.wilayahList
                                    deptList = cachedData.deptList
                                    divisiList = cachedData.divisiList
                                    blokList = cachedData.blokList
                                    tphList = cachedData.tphList

                                    withContext(Dispatchers.Main) {
                                        loadingDialog.dismiss()
                                        setupLayout()
                                    }
                                }
                            } else {
                                Log.d("testing", "masuk sini")
                                withContext(Dispatchers.Main) {
                                    loadingDialog.dismiss()
                                    loadAllFilesAsync()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("DataLoading", "Error loading data: ${e.message}")
                            withContext(Dispatchers.Main) {
                                loadingDialog.dismiss()
                            }
                        }
                    }
                }
            }
        }
    }



    override fun onPause() {
        super.onPause()
        locationViewModel.stopLocationUpdates()

        LocationServices.getFusedLocationProviderClient(this)
            .removeLocationUpdates(locationSettingsCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationViewModel.stopLocationUpdates()
        unregisterReceiver(locationSettingsReceiver)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            val deniedPermissions = mutableListOf<String>()
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }

            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "The following permissions are required: ${deniedPermissions.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()
            }else {
                lifecycleScope.launch {
                    if (shouldStartFileDownload()) {
                        Log.d("FileCheck", "Starting file download...")
                        startFileDownload()
                    } else {
                        Log.d("FileCheck", "File download not required.")
                    }
                }
            }
        }
    }

}