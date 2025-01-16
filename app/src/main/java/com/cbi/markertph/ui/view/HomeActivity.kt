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
import com.cbi.markertph.data.model.CompanyCodeModel
import com.cbi.markertph.data.model.DivisionCodeModel
import com.cbi.markertph.data.model.FieldCodeModel
import com.cbi.markertph.data.model.TPHModel
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
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream


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
    private var selectedEstate: String = ""
    private var selectedAfdeling: String = ""
    private var selectedBlok: String = ""
    private var selectedTPH: String = ""
    private var selectedAncak: String = ""
    var currentAccuracy : Float = 0F
    private var bUnitCodesList: List<BUnitCodeModel> = emptyList()
    private var divisionCodesList: List<DivisionCodeModel> = emptyList()
    private var fieldCodesList: List<FieldCodeModel> = emptyList()
    private var tphDataList: List<TPHModel> = emptyList()
    private var tphIds: List<Int> = emptyList()
    private lateinit var dataCacheManager: DataCacheManager
    var tphNames: List<String> = emptyList()
    private lateinit var loadingDialog: LoadingDialog
    private var selectedBUnitCodeValue: Int? = null
    private var selectedDivisionCodeValue: Int? = null
    private var selectedTahunTanamValue: String? = null
    private var selectedFieldCodeValue: Int? = null
    private var selectedAncakValue: Int? = null
    private var selectedTPHValue: Int? = null
    private var selectedIDTPHAncak: Int? = null


    private val locationSettingsCallback = object : LocationCallback() {
        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            super.onLocationAvailability(locationAvailability)

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // Only show toast if GPS is actually disabled in system settings
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
    private var selectedBUnitSpinnerIndex: Int? = null
    private var selectedFieldCodeSpinnerIndex: Int? = null
    private var selectedAncakSpinnerIndex: Int? = null
    private var selectedTPHSpinnerIndex: Int? = null
    private var companyCodeList: List<CompanyCodeModel> = emptyList()
    private var bUnitCodeList: List<BUnitCodeModel> = emptyList()
    private var divisionCodeList: List<DivisionCodeModel> = emptyList()
    private var fieldCodeList: List<FieldCodeModel> = emptyList()
    private var tphList: List<TPHModel>? = null // Lazy-loaded

    enum class InputType {
        SPINNER,
        EDITTEXT
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
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

//            if (currentAccuracy == null || currentAccuracy > 10.0f) {
//                vibrate()
//                AlertDialogUtility.withSingleAction(
//                    this,
//                    stringXML(R.string.al_back),
//                    stringXML(R.string.al_location_not_accurate),
//                    stringXML(R.string.al_location_under_ten_meter),
//                    "warning.json",
//                    R.color.colorRedDark
//                ) {}
//                return@setOnClickListener
//            }

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
                        estate = selectedEstate,
                        id_estate = selectedBUnitCodeValue ?: 0,
                        afdeling = selectedAfdeling,
                        id_afdeling = selectedDivisionCodeValue ?: 0,
                        tahun_tanam = selectedTahunTanamValue ?: "",
                        blok = selectedBlok,
                        id_blok = selectedFieldCodeValue ?: 0,
                        ancak = selectedAncak,
                        id_ancak = selectedIDTPHAncak!!,
                        tph = selectedTPH,
                        id_tph = selectedIDTPHAncak!!,
                        panen_ulang = 0,
                        latitude = lat.toString(),
                        longitude = lon.toString(),
                        app_version = app_version.toString(),

                    )

                    tphViewModel.insertDBTPH.observe(this) { isInserted ->
                        if (isInserted) {
                            AlertDialogUtility.alertDialogAction(
                                this,
                                getString(R.string.al_success_save_local),
                                getString(R.string.al_description_success_save_local),
                                "success.json"
                            ) {
                                prefManager!!.user_input = userInput
                                prefManager!!.id_selected_estate = selectedBUnitSpinnerIndex
                                prefManager!!.id_selected_afdeling = selectedDivisionSpinnerIndex
                                prefManager!!.id_selected_tahun_tanam = selectedTahunTanamValue
                                prefManager!!.id_selected_blok = selectedFieldCodeSpinnerIndex
                                prefManager!!.id_selected_ancak = selectedAncakSpinnerIndex
                                prefManager!!.id_selected_tph = selectedTPHSpinnerIndex
                            }
                        } else {
                            AlertDialogUtility.alertDialogAction(
                                this,
                                getString(R.string.al_failed_save_local),
                                getString(R.string.al_description_failed_save_local),
                                "warning.json"
                            ) {}
                            Toast.makeText(
                                this,
                                getString(R.string.toast_failed_save_local),
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
            Triple(binding.layoutEstate, getString(R.string.field_estate), InputType.SPINNER),
            Triple(binding.layoutAfdeling, getString(R.string.field_afdeling), InputType.SPINNER),
            Triple(binding.layoutTahunTanam, getString(R.string.field_tahun_tanam), InputType.SPINNER),
            Triple(binding.layoutBlok, getString(R.string.field_blok), InputType.SPINNER),
            Triple(binding.layoutAncak, getString(R.string.field_ancak), InputType.SPINNER),
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

        val savedEstateIndexSpinner = prefManager?.id_selected_estate ?: 0
        val savedAfdelingIndexSpinner = prefManager?.id_selected_afdeling ?: 0
        val savedTahunTanamIndexSpinner = prefManager?.id_selected_tahun_tanam ?: 0
        val savedBlockIndexSpinner = prefManager?.id_selected_blok ?: 0
        val savedAncakIndexSpinner = prefManager?.id_selected_ancak ?: 0
        val savedTPHIndexSpinner = prefManager?.id_selected_tph ?: 0

//        Log.d("testing", "Estate Index: $savedEstateIndexSpinner")
//        Log.d("testing", "Afdeling Index: $savedAfdelingIndexSpinner")
//        Log.d("testing", "Block Index: $savedBlockIndexSpinner")
//        Log.d("testing", "Ancak Index: $savedAncakIndexSpinner")
//        Log.d("testing", "TPH Index: $savedTPHIndexSpinner")

//        inputMappings.forEach { (layoutBinding, key, inputType) ->
//            updateTextInPertanyaan(layoutBinding, key)
//            when (inputType) {
//                InputType.SPINNER -> {
//                    when (layoutBinding) {
//                        binding.layoutEstate -> {
//                            val estateOptions = bUnitCodeList.map { it.BUnitName }
//                            setupSpinnerView(layoutBinding, estateOptions)
//
//                            Log.d("testing", estateOptions.toString())
//                            if (savedEstateIndexSpinner in estateOptions.indices) {
//                                binding.layoutEstate.spHomeMarkerTPH.setSelectedIndex(savedEstateIndexSpinner)
//                                selectedBUnitSpinnerIndex = savedEstateIndexSpinner
//                                selectedEstate = estateOptions[savedEstateIndexSpinner]
//                                selectedBUnitCodeValue = bUnitCodeList[savedEstateIndexSpinner].BUnitCode
//
//                                val afdelingOptions = divisionCodeList
//                                    .filter { it.BUnitCode == selectedBUnitCodeValue }
//                                    .map { it.DivisionName }
//
//                                Log.d("testing", "Afdeling Options: $afdelingOptions")
//                                Log.d("testing", "Selected BUnitCode: $selectedBUnitCodeValue")
//                                binding.layoutAfdeling.root.visibility = View.VISIBLE
//                                setupSpinnerView(binding.layoutAfdeling, afdelingOptions)
//
//
////                                 Handle Afdeling spinner saved selection
//                                    if (savedAfdelingIndexSpinner in afdelingOptions.indices) {
//                                        Log.d("testing", "Saved Afdeling Index: $savedAfdelingIndexSpinner")
//                                        Log.d("testing", "Selected Afdeling: ${afdelingOptions[savedAfdelingIndexSpinner]}")
//
//                                        binding.layoutAfdeling.spHomeMarkerTPH.setSelectedIndex(savedAfdelingIndexSpinner)
//                                        selectedDivisionSpinnerIndex = savedAfdelingIndexSpinner
//                                        selectedAfdeling = afdelingOptions[savedAfdelingIndexSpinner]
//                                        selectedDivisionCodeValue = divisionCodeList
//                                            .filter { it.BUnitCode == selectedBUnitCodeValue }[savedAfdelingIndexSpinner].DivisionCode
//
//                                        Log.d("testing", "Selected DivisionCode: $selectedDivisionCodeValue")
//
//                                        // Get field codes for the selected division
//                                        val fieldCodeOptions = fieldCodeList
//                                            .filter {
////                                                Log.d("testing", "Checking field: BUnit=${it.BUnitCode}==$selectedBUnitCodeValue, Division=${it.DivisionCode}==$selectedDivisionCodeValue")
//                                                it.BUnitCode == selectedBUnitCodeValue &&
//                                                        it.DivisionCode == selectedDivisionCodeValue
//                                            }
//                                            .map { it.FieldName }
//
//                                        Log.d("testing", "Field code list size: ${fieldCodeList.size}")
//                                        Log.d("testing", "Filtered field options size: ${fieldCodeOptions.size}")
//                                        Log.d("testing", "Field options: $fieldCodeOptions")
//
//                                        setupSpinnerView(binding.layoutBlok, fieldCodeOptions)
//                                        binding.layoutBlok.root.visibility = View.VISIBLE
//                                    } else {
//                                        Log.d("testing", "Saved Afdeling index ($savedAfdelingIndexSpinner) out of bounds for options size ${afdelingOptions.size}")
//                                    }
//                            } else {
//                                Log.d("testing", "Saved Estate index ($savedEstateIndexSpinner) out of bounds for options size ${estateOptions.size}")
//                            }
//                        }
//                        else -> {
//                            setupSpinnerView(layoutBinding, emptyList())
//                        }
//                    }
//                }
//                InputType.EDITTEXT -> setupEditTextView(layoutBinding)
//            }
//        }

        lifecycleScope.launch {
            // Wait until data is ready
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (bUnitCodeList.isNotEmpty()) {
                    val estateOptions = bUnitCodeList.map { it.BUnitName }
                    setupSpinnerView(binding.layoutEstate, estateOptions)

                    if (savedEstateIndexSpinner in estateOptions.indices) {
                        binding.layoutEstate.spHomeMarkerTPH.setSelectedIndex(savedEstateIndexSpinner)
                        selectedBUnitSpinnerIndex = savedEstateIndexSpinner
                        selectedEstate = estateOptions[savedEstateIndexSpinner]
                        selectedBUnitCodeValue = bUnitCodeList[savedEstateIndexSpinner].BUnitCode

                        val afdelingOptions = divisionCodeList
                            .filter { it.BUnitCode == selectedBUnitCodeValue }
                            .map { it.DivisionName }

                        binding.layoutAfdeling.root.visibility = View.VISIBLE
                        setupSpinnerView(binding.layoutAfdeling, afdelingOptions)

                        if (savedAfdelingIndexSpinner in afdelingOptions.indices) {
                            binding.layoutAfdeling.spHomeMarkerTPH.setSelectedIndex(savedAfdelingIndexSpinner)
                            selectedDivisionSpinnerIndex = savedAfdelingIndexSpinner
                            selectedAfdeling = afdelingOptions[savedAfdelingIndexSpinner]
                            selectedDivisionCodeValue = divisionCodeList
                                .filter { it.BUnitCode == selectedBUnitCodeValue }[savedAfdelingIndexSpinner].DivisionCode

                            // Now filter field codes for planting years
                            val filteredFieldCodes = fieldCodeList.filter { fieldCode ->
                                fieldCode.BUnitCode == selectedBUnitCodeValue &&
                                        fieldCode.DivisionCode == selectedDivisionCodeValue
                            }

                            val plantingYears = filteredFieldCodes
                                .map { it.PlantingYear.toString() }
                                .distinct()
                                .sorted()

                            binding.layoutTahunTanam.root.visibility = View.VISIBLE

                            setupSpinnerView(binding.layoutTahunTanam, plantingYears)

                            val tahunTanamIndex = plantingYears.indexOf(savedTahunTanamIndexSpinner.toString())
                            if (tahunTanamIndex != -1) {
                                binding.layoutTahunTanam.spHomeMarkerTPH.setSelectedIndex(tahunTanamIndex)
                                selectedTahunTanamValue = savedTahunTanamIndexSpinner.toString()

                                // Filter field codes for selected planting year
                                val fieldCodeOptions = fieldCodeList.filter { fieldCode ->
                                    fieldCode.BUnitCode == selectedBUnitCodeValue &&
                                            fieldCode.DivisionCode == selectedDivisionCodeValue &&
                                            fieldCode.PlantingYear.toString() == selectedTahunTanamValue
                                }.map { it.FieldName }

                                binding.layoutBlok.root.visibility = View.VISIBLE
                                setupSpinnerView(binding.layoutBlok, fieldCodeOptions)

                                // Handle Block saved selection
                                if (savedBlockIndexSpinner in fieldCodeOptions.indices) {
                                    binding.layoutBlok.spHomeMarkerTPH.setSelectedIndex(savedBlockIndexSpinner)
                                    selectedBlok = fieldCodeOptions[savedBlockIndexSpinner]
                                    selectedFieldCodeSpinnerIndex = savedBlockIndexSpinner
                                    selectedFieldCodeValue = fieldCodeList.find { it.FieldName == selectedBlok }?.FieldCode

                                    // Filter TPH for Ancak
                                    val filteredTPH = tphList?.filter { tph ->
                                        tph.BUnitCode == selectedBUnitCodeValue &&
                                                tph.DivisionCode == selectedDivisionCodeValue &&
                                                tph.planting_year == selectedTahunTanamValue!!.toInt() &&
                                                tph.FieldCode == selectedFieldCodeValue
                                    }

                                    val ancakValues = filteredTPH?.map { it.ancak }?.distinct()?.map { it.toString() } ?: emptyList()

                                    binding.layoutAncak.root.visibility = View.VISIBLE
                                    setupSpinnerView(binding.layoutAncak, ancakValues)

                                    if (savedAncakIndexSpinner in ancakValues.indices) {
                                        binding.layoutAncak.spHomeMarkerTPH.setSelectedIndex(savedAncakIndexSpinner)
                                        selectedAncak = ancakValues[savedAncakIndexSpinner]
                                        selectedAncakSpinnerIndex = savedAncakIndexSpinner
                                        selectedAncakValue = tphList?.find { tph ->
                                            tph.BUnitCode == selectedBUnitCodeValue &&
                                                    tph.DivisionCode == selectedDivisionCodeValue &&
                                                    tph.planting_year == selectedTahunTanamValue?.toInt() &&
                                                    tph.FieldCode == selectedFieldCodeValue &&
                                                    tph.ancak.toString() == selectedAncak
                                        }?.ancak

                                        // Filter TPH values
                                        val tphValues = filteredTPH
                                            ?.filter { it.ancak == selectedAncakValue }
                                            ?.map { it.tph.toString() }
                                            ?.distinct() ?: emptyList()

                                        binding.layoutTPH.root.visibility = View.VISIBLE
                                        setupSpinnerView(binding.layoutTPH, tphValues)

                                        if (savedTPHIndexSpinner in tphValues.indices) {
                                            binding.layoutTPH.spHomeMarkerTPH.setSelectedIndex(savedTPHIndexSpinner)
                                            selectedTPH = tphValues[savedTPHIndexSpinner]
                                            selectedTPHSpinnerIndex = savedTPHIndexSpinner
                                            selectedIDTPHAncak = filteredTPH?.firstOrNull {
                                                it.ancak == selectedAncakValue &&
                                                        it.tph.toString() == selectedTPH
                                            }?.id

                                            findViewById<MaterialButton>(R.id.mbSaveDataTPH).visibility = View.VISIBLE
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Log.d("testing", "Saved Estate index ($savedEstateIndexSpinner) out of bounds for options size ${estateOptions.size}")
                    }
                }
            }
        }

    }

    private fun loadAllFilesAsync() {
        val filesToDownload = AppUtils.ApiCallManager.apiCallList.map { it.first }
        loadingDialog.show()
        val progressJob = lifecycleScope.launch(Dispatchers.Main) {
            var dots = 1
            while (true) {
                loadingDialog.setMessage("${stringXML(R.string.fetching_dataset)}${".".repeat(dots)}")
                dots = if (dots >= 3) 1 else dots + 1
                delay(500) // Update every 500ms
            }
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    filesToDownload.forEachIndexed  { index, fileName ->
                        val file = File(application.getExternalFilesDir(null), fileName)
                        if (file.exists()) {
                            decompressFile(file, index == filesToDownload.lastIndex) // Process each file
                        } else {
                            Log.e("LoadFileAsync", "File not found: $fileName")
                        }
                    }
                }

                dataCacheManager.saveDatasets(
                    companyCodeList,
                    bUnitCodeList,
                    divisionCodeList,
                    fieldCodeList,
                    tphList!!
                )
            } catch (e: Exception) {
                Log.e("LoadFileAsync", "Error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    progressJob.cancel()
                    setupLayout()
                }
            }
        }
    }

    private fun decompressFile(file: File,  isLastFile: Boolean) {
        try {
            // Read the GZIP-compressed file directly
            val gzipInputStream = GZIPInputStream(file.inputStream())
            val decompressedData = gzipInputStream.readBytes()

            // Convert the decompressed bytes to a JSON string
            val jsonString = String(decompressedData, Charsets.UTF_8)
            Log.d("DecompressedJSON", "Decompressed JSON: $jsonString")

            parseJsonData(jsonString, isLastFile)

        } catch (e: Exception) {
            Log.e("DecompressFile", "Error decompressing file: ${e.message}")
            e.printStackTrace()
        }
    }


    private fun parseJsonData(jsonString: String,  isLastFile: Boolean) {
        try {
            val jsonObject = JSONObject(jsonString)
            val gson = Gson()

            val keyObject = jsonObject.getJSONObject("key")

            // Parse CompanyCodeDB
            if (jsonObject.has("CompanyCodeDB")) {
                val companyCodeArray = jsonObject.getJSONArray("CompanyCodeDB")
                val transformedCompanyCodeArray = transformJsonArray(companyCodeArray, keyObject)
                val companyCodeList: List<CompanyCodeModel> = gson.fromJson(
                    transformedCompanyCodeArray.toString(),
                    object : TypeToken<List<CompanyCodeModel>>() {}.type
                )
                Log.d("ParsedData", "CompanyCode: $companyCodeList")
                this.companyCodeList = companyCodeList
            } else {
                Log.e("ParseJsonData", "CompanyCodeDB key is missing")
            }

            // Parse BUnitCodeDB
            if (jsonObject.has("BUnitCodeDB")) {
                val bUnitCodeArray = jsonObject.getJSONArray("BUnitCodeDB")
                val transformedBUnitCodeArray = transformJsonArray(bUnitCodeArray, keyObject)
                val bUnitCodeList: List<BUnitCodeModel> = gson.fromJson(
                    transformedBUnitCodeArray.toString(),
                    object : TypeToken<List<BUnitCodeModel>>() {}.type
                )
                Log.d("ParsedData", "BUnitCode: $bUnitCodeList")
                this.bUnitCodeList = bUnitCodeList
            } else {
                Log.e("ParseJsonData", "BUnitCodeDB key is missing")
            }

            // Parse DivisionCodeDB
            if (jsonObject.has("DivisionCodeDB")) {
                val divisionCodeArray = jsonObject.getJSONArray("DivisionCodeDB")
                val transformedDivisionCodeArray = transformJsonArray(divisionCodeArray, keyObject)
                val divisionCodeList: List<DivisionCodeModel> = gson.fromJson(
                    transformedDivisionCodeArray.toString(),
                    object : TypeToken<List<DivisionCodeModel>>() {}.type
                )
                Log.d("ParsedData", "DivisionCode: $divisionCodeList")
                this.divisionCodeList = divisionCodeList
            } else {
                Log.e("ParseJsonData", "DivisionCodeDB key is missing")
            }

            // Parse FieldCodeDB
            if (jsonObject.has("FieldCodeDB")) {
                val fieldCodeArray = jsonObject.getJSONArray("FieldCodeDB")
                val transformedFieldCodeArray = transformJsonArray(fieldCodeArray, keyObject)
                val fieldCodeList: List<FieldCodeModel> = gson.fromJson(
                    transformedFieldCodeArray.toString(),
                    object : TypeToken<List<FieldCodeModel>>() {}.type
                )
                Log.d("ParsedData", "FieldCode: $fieldCodeList")
                this.fieldCodeList = fieldCodeList
            } else {
                Log.e("ParseJsonData", "FieldCodeDB key is missing")
            }

            // Cache lightweight data
            this.companyCodeList = companyCodeList
            this.bUnitCodeList = bUnitCodeList
            this.divisionCodeList = divisionCodeList
            this.fieldCodeList = fieldCodeList


            if (isLastFile) {
                loadTPHData(jsonObject)
            }

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

    private fun loadTPHData(jsonObject: JSONObject) {
        try {
            // Check if the tphList is null or needs to be loaded
            if (tphList == null) {
                val gson = Gson()

                if (jsonObject.has("TPHDB")) {
                    // Dynamically transform and parse TPH data
                    val tphArray = jsonObject.getJSONArray("TPHDB")
                    val transformedTphArray = transformJsonArray(tphArray, jsonObject.getJSONObject("key"))
                    tphList = gson.fromJson(
                        transformedTphArray.toString(),
                        object : TypeToken<List<TPHModel>>() {}.type
                    )

                }
                // Log the number of entries loaded
                Log.d("ParsedData", "Loaded TPH data with ${tphList?.size} entries")
            }
        } catch (e: Exception) {
            Log.e("TPHData", "Error loading TPH data", e)
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
                    stringXML(R.string.field_estate) -> {

                        resetViewsBelow(binding.layoutEstate)
                        selectedEstate = item.toString()

                        Log.d("testing", selectedEstate.toString())
                        val selectedBUnitCode = bUnitCodesList.find { it.BUnitName == selectedEstate }?.BUnitCode
                        selectedBUnitCodeValue = selectedBUnitCode
                        selectedBUnitSpinnerIndex = position
                        val selectedBUnit = bUnitCodeList.getOrNull(position)
                        selectedBUnit?.let { bUnit ->
                            val filteredDivisionCodes = divisionCodeList.filter { division ->
                                division.BUnitCode == bUnit.BUnitCode  // Match the code (adjust field name as needed)
                            }
                            val divisionCodeNames = filteredDivisionCodes.map { it.DivisionName }

                            selectedBUnitCodeValue = bUnit.BUnitCode
                            setupSpinnerView(binding.layoutAfdeling, divisionCodeNames)
                            findViewById<LinearLayout>(R.id.layoutAfdeling).visibility = View.VISIBLE

                        } ?: run {
                            Log.e("Spinner", "Invalid BUnitCode selection")
                        }

                    }
                    stringXML(R.string.field_afdeling) -> {
                        resetViewsBelow(binding.layoutAfdeling)

                        selectedAfdeling = item.toString()

                        val selectedDivisionCode = divisionCodeList.find { it.DivisionName == selectedAfdeling }?.DivisionCode
                        selectedDivisionSpinnerIndex = position
                        selectedDivisionCodeValue = selectedDivisionCode ?: run {
                            // Handle the case where no matching DivisionCode is found
                            Log.e("Spinner", "No DivisionCode found for DivisionName: $selectedAfdeling")
                            null
                        }

                        // Filter the fieldCodeList based on the selected BUnitCode and DivisionCode
                        val filteredFieldCodes = fieldCodeList.filter { fieldCode ->
                            fieldCode.BUnitCode == selectedBUnitCodeValue && fieldCode.DivisionCode == selectedDivisionCodeValue
                        }

                        if (filteredFieldCodes.isNotEmpty()) {
                            // Extract PlantingYear from the filtered results
                            val plantingYears = filteredFieldCodes
                                .map { it.PlantingYear.toString() } // Convert each PlantingYear to String
                                .distinct() // Remove duplicate years
                                .sorted() // Sort the years in ascending order

                            val plantingYearLayoutView = findViewById<LinearLayout>(R.id.layoutTahunTanam)
                            plantingYearLayoutView.visibility = View.VISIBLE
                            setupSpinnerView(binding.layoutTahunTanam, plantingYears)

                        } else {
                            setupSpinnerView(binding.layoutTahunTanam, emptyList())
                        }
                    }
                    stringXML(R.string.field_tahun_tanam)->{
                        val selectedTahunTanam = item.toString()
                        resetViewsBelow(binding.layoutTahunTanam)
                        selectedTahunTanamValue = selectedTahunTanam
                        val filteredFieldCodes = fieldCodeList.filter { fieldCode ->
                            fieldCode.BUnitCode == selectedBUnitCodeValue &&
                                    fieldCode.DivisionCode == selectedDivisionCodeValue &&
                                    fieldCode.PlantingYear.toString() == selectedTahunTanam // Match the selected PlantingYear
                        }

                        if (filteredFieldCodes.isNotEmpty()) {
                            // Extract the FieldName for the filtered fieldCodes
                            val fieldNames = filteredFieldCodes.map { it.FieldName }

                            val blokLayoutView = findViewById<LinearLayout>(R.id.layoutBlok)
                            blokLayoutView.visibility = View.VISIBLE
                            setupSpinnerView(binding.layoutBlok, fieldNames)
                        } else {
                            val blokLayoutView = findViewById<LinearLayout>(R.id.layoutBlok)
                            setupSpinnerView(binding.layoutBlok, emptyList())
                        }
                    }
                    stringXML(R.string.field_blok) -> {
                        resetViewsBelow(binding.layoutBlok)

                        selectedBlok = item.toString()
                        selectedFieldCodeSpinnerIndex = position
                        val selectedFieldCode = fieldCodeList.find { it.FieldName == selectedBlok }?.FieldCode
                        selectedFieldCodeValue = selectedFieldCode ?: run {
                            null
                        }

                        val filteredTPH = tphList?.filter { tph ->
                            tph.BUnitCode == selectedBUnitCodeValue &&
                                    tph.DivisionCode == selectedDivisionCodeValue &&
                                    tph.planting_year == selectedTahunTanamValue!!.toInt() &&
                                    tph.FieldCode == selectedFieldCodeValue
                        }

                        if (filteredTPH != null && filteredTPH.isNotEmpty()) {
                            // Extract distinct values for 'Ancak' from the filtered TPH data
                            val ancakValues = filteredTPH.map { it.ancak }.distinct()

                            // Find the layout for 'Ancak' (assuming it's R.id.layoutAncak)
                            val ancakLayoutView = findViewById<LinearLayout>(R.id.layoutAncak)
                            ancakLayoutView.visibility = View.VISIBLE
                            setupSpinnerView(binding.layoutAncak, ancakValues.map { it.toString() }) // Convert to String for spinner
                        } else {
                            // Set an empty list to the spinner for Ancak
                            val ancakLayoutView = findViewById<LinearLayout>(R.id.layoutAncak)
                            setupSpinnerView(binding.layoutAncak, emptyList()) // Empty list when no data is found
                        }
                    }
                    stringXML(R.string.field_ancak) -> {

                        selectedAncak = item.toString()
                        selectedAncakSpinnerIndex = position
                        resetViewsBelow(binding.layoutAncak)

                        val selectedAncakCode = tphList?.find { tph ->
                            tph.BUnitCode == selectedBUnitCodeValue &&
                                    tph.DivisionCode == selectedDivisionCodeValue &&
                                    tph.planting_year == selectedTahunTanamValue?.toInt() &&
                                    tph.FieldCode == selectedFieldCodeValue &&
                                    tph.ancak.toString() == selectedAncak // Match the selectedAncak with TPH's ancak
                        }?.ancak

                        selectedAncakValue = selectedAncakCode ?: run {
                            null
                        }


                        val filteredTPH = tphList?.filter { tph ->
                            tph.BUnitCode == selectedBUnitCodeValue &&
                                    tph.DivisionCode == selectedDivisionCodeValue &&
                                    tph.planting_year == selectedTahunTanamValue?.toInt() &&
                                    tph.FieldCode == selectedFieldCodeValue &&
                                    tph.ancak == selectedAncakValue
                        }

                        if (filteredTPH != null && filteredTPH.isNotEmpty()) {

                            val tphValues = filteredTPH.map { it.tph }.distinct()

                            val tphLayoutView = findViewById<LinearLayout>(R.id.layoutTPH)
                            tphLayoutView.visibility = View.VISIBLE
                            setupSpinnerView(binding.layoutTPH, tphValues.map { it.toString() }) // Convert to String for spinner
                        } else {

                            val ancakLayoutView = findViewById<LinearLayout>(R.id.layoutTPH)
                            setupSpinnerView(binding.layoutTPH, emptyList())
                        }
                    }
                    stringXML(R.string.field_tph) -> {
                        selectedTPH = item.toString()
                        selectedTPHSpinnerIndex = position

                        val filteredTPH = tphList?.filter { tph ->
                            tph.BUnitCode == selectedBUnitCodeValue &&
                                    tph.DivisionCode == selectedDivisionCodeValue &&
                                    tph.planting_year == selectedTahunTanamValue?.toInt() &&
                                    tph.FieldCode == selectedFieldCodeValue &&
                                    tph.ancak == selectedAncakValue &&
                                    tph.tph == selectedTPH
                        }

                        selectedIDTPHAncak = filteredTPH?.firstOrNull()?.id


                        val mbSave = findViewById<MaterialButton>(R.id.mbSaveDataTPH)
                        mbSave.visibility = View.VISIBLE
                    }
                }
            }
        }
    }


    fun resetViewsBelow(triggeredLayout: PertanyaanSpinnerLayoutBinding) {
        val mbSave = findViewById<MaterialButton>(R.id.mbSaveDataTPH)
        mbSave.visibility = View.GONE
        when (triggeredLayout) {
            binding.layoutEstate -> {
                clearSpinnerView(binding.layoutAfdeling, ::resetSelectedDivisionCode)
                clearSpinnerView(binding.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(binding.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(binding.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)

            }
            binding.layoutAfdeling -> {
                clearSpinnerView(binding.layoutTahunTanam, ::resetSelectedTahunTanam)
                clearSpinnerView(binding.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(binding.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
            binding.layoutTahunTanam -> {
                clearSpinnerView(binding.layoutBlok, ::resetSelectedFieldCode)
                clearSpinnerView(binding.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
            binding.layoutBlok -> {
                clearSpinnerView(binding.layoutAncak, ::resetSelectedAncak)
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
            binding.layoutAncak -> {
                clearSpinnerView(binding.layoutTPH, ::resetSelectedTPH)
            }
        }
    }

    fun clearSpinnerView(layoutBinding: PertanyaanSpinnerLayoutBinding, resetSelectedValue: () -> Unit) {
        layoutBinding.root.visibility = if (layoutBinding != binding.layoutAfdeling) View.GONE else View.VISIBLE
        setupSpinnerView(layoutBinding, emptyList()) // Reset spinner with an empty list
        resetSelectedValue() // Reset the associated selected value
    }


    // Functions to reset selected values
    fun resetSelectedDivisionCode() {
        selectedDivisionCodeValue = null
    }

    fun resetSelectedTahunTanam() {
        selectedTahunTanamValue = null
    }

    fun resetSelectedFieldCode() {
        selectedFieldCodeValue = null
    }

    fun resetSelectedAncak() {
        selectedAncakValue = null
    }

    fun resetSelectedTPH() {
        // Assuming you have a variable for TPH selection
        selectedTPHValue = null
    }

    private fun setupEditTextView(layoutBinding: PertanyaanSpinnerLayoutBinding) {
        with(layoutBinding) {
            spHomeMarkerTPH.visibility = View.GONE
            etHomeMarkerTPH.visibility = View.VISIBLE


            if (layoutBinding == binding.layoutUserInput) {
                val savedUserInput = prefManager?.user_input ?: ""
                if (savedUserInput.isNotEmpty()) {
                    etHomeMarkerTPH.setText(savedUserInput)
                    userInput = savedUserInput  // Update the userInput variable as well
                }
            }

            etHomeMarkerTPH.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Hide keyboard
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
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }


    private fun validateAndShowErrors(): Boolean {


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
                        binding.layoutAncak -> selectedAncak.isEmpty()
                        binding.layoutTPH -> selectedTPH.isEmpty()
                        else -> layoutBinding.spHomeMarkerTPH.selectedIndex == -1
                    }
                }
                InputType.EDITTEXT -> {
                    when (key) {
                        "User Input" -> userInput.trim().isEmpty()
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
                val needsDownload = savedFileList.getOrNull(index) == null || !file.exists()
                Log.d("FileDownload", "File: $fileName, Needs download: $needsDownload")
                needsDownload
            }

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
    ): Pair<Boolean, String> {  // Changed return type to include message
        return try {
            withContext(Dispatchers.IO) {
                val response = apiCall()
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val file = File(downloadsDir, fileName)
                        saveFileToStorage(responseBody, file)
                        fileList.add(fileName)
                        Log.d("FileDownload", "$fileName downloaded successfully.")
                        Pair(true, "Unduh Selesai")
                    } else {
                        fileList.add(null)
                        Log.e("FileDownload", "Response body is null.")
                        Pair(false, "Response body kosong")
                    }
                } else {
                    fileList.add(null)
                    try {
                        val errorBody = response.errorBody()?.string()
                        val gson = Gson()
                        val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)
                        Log.e("FileDownload", "Error message: ${errorResponse.message}")
                        Pair(false, "Unduh Gagal! ${errorResponse.message}")  // Added prefix here
                    } catch (e: Exception) {
                        Pair(false, "Unduh Gagal! Kode: ${response.code()}")  // Added prefix here
                    }
                }
            }
        } catch (e: Exception) {
            fileList.add(null)
            Log.e("FileDownload", "Error downloading file: ${e.message}")
            Pair(false, "Unduh Gagal! ${e.message}")  // Added prefix here
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

    private fun shouldStartFileDownload(): Boolean {
        val savedFileList = prefManager!!.getFileList() // Retrieve the saved file list
        val downloadsDir = this.getExternalFilesDir(null) // Get the downloads directory

        Log.d("FileCheck", "Saved file list: $savedFileList")
        Log.d("FileCheck", "Downloads directory: $downloadsDir")
        Log.d("FileCheck", "Is first time launch: ${prefManager!!.isFirstTimeLaunch}")

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

        return false
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                permissionRequestCode
            )
        }else{
            if (shouldStartFileDownload()) {
                Log.d("FileCheck", "Starting file download...")
                startFileDownload()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        loadingDialog.show()  // Show loading at start
                        loadingDialog.setMessage("Loading data...")
                    }

                    try {
                        val cachedData = dataCacheManager.getDatasets()
                        if (cachedData != null && !dataCacheManager.needsRefresh()) {
                            // Check if any of the datasets are empty
                            val hasEmptyDatasets = cachedData.companyCodeList.isEmpty() ||
                                    cachedData.bUnitCodeList.isEmpty() ||
                                    cachedData.divisionCodeList.isEmpty() ||
                                    cachedData.fieldCodeList.isEmpty() ||
                                    cachedData.tphList.isEmpty()

                            if (hasEmptyDatasets) {
                                withContext(Dispatchers.Main) {
                                    loadingDialog.dismiss()
                                    loadAllFilesAsync()  // This will reload all datasets
                                }
                            } else {
                                // All datasets have values, use cached data
                                companyCodeList = cachedData.companyCodeList
                                bUnitCodeList = cachedData.bUnitCodeList
                                divisionCodeList = cachedData.divisionCodeList
                                fieldCodeList = cachedData.fieldCodeList
                                tphList = cachedData.tphList

                                withContext(Dispatchers.Main) {
                                    loadingDialog.dismiss()
                                    setupLayout()
                                }
                            }
                        } else {
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
                if (shouldStartFileDownload()) {
                    Log.d("FileCheck", "Starting file download...")
                    startFileDownload()
                } else {
                    Log.d("FileCheck", "File download not required.")
                }
            }
        }
    }

    companion object {
        @SuppressLint("HardwareIds")
        private fun getDeviceInfo(context: Context): JSONObject {
            val json = JSONObject()

            val appVersion = context.getString(R.string.app_version)
            json.put("app_version", appVersion)
            json.put("os_version", Build.VERSION.RELEASE)
            json.put("device_model", Build.MODEL)

            val imeiOrAndroidId = if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    val telephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    telephonyManager.deviceId
                } else {
                    null
                }
            } else {
                null
            }

            val uniqueId = imeiOrAndroidId ?: Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            json.put("unique_id", uniqueId)

            return json
        }
    }
}