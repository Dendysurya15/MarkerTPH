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
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.R
import com.cbi.markertph.data.model.BUnitCodeModel
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
import com.cbi.markertph.utils.PrefManager
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    var tphNames: List<String> = emptyList()

    private var selectedBUnitCodeValue: Int? = null
    private var selectedDivisionCodeValue: Int? = null
    private var selectedFieldCodeValue: Int? = null
    private var selectedAncakInt: Int? = null
    private var selectedTPHInt: Int? = null

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
//                    Toast.makeText(
//                        this@HomeActivity,
//                        "GPS is turned off. Please enable location services",
//                        Toast.LENGTH_SHORT
//                    ).show()
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
                showSnackbar(getString(R.string.location_permission_denied))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PrefManager(this)
        initViewModel()
        checkPermissions()
        setupLayout()
        setAppVersion()



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
                    val app_version = getString(R.string.app_version)
                    tphViewModel.insertPanenTBSVM(
                        tanggal = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                            Date()
                        ),
                        user_input = userInput,
                        estate = selectedEstate,
                        id_estate = selectedBUnitCodeValue ?: 0,
                        afdeling = selectedAfdeling,
                        id_afdeling = selectedDivisionCodeValue ?: 0,
                        blok = selectedBlok,
                        id_blok = selectedFieldCodeValue ?: 0,
                        ancak = selectedAncak,
                        id_ancak = 0,
                        tph = selectedTPH,
                        id_tph = tphDataList.firstOrNull { it.tph.toString() == selectedTPH }?.id ?: 0,
                        latitude = lat.toString(),
                        longitude = lon.toString(),
                        app_version = app_version
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
            Triple(binding.layoutBlok, getString(R.string.field_blok), InputType.SPINNER),
            Triple(binding.layoutAncak, getString(R.string.field_ancak), InputType.SPINNER),
            Triple(binding.layoutTPH, getString(R.string.field_tph), InputType.SPINNER)
        )

        // First set up all spinners with empty data and text
        inputMappings.forEach { (layoutBinding, key, inputType) ->
            updateTextInPertanyaan(layoutBinding, key)
            when (inputType) {
                InputType.SPINNER -> setupSpinnerView(layoutBinding, emptyList())
                InputType.EDITTEXT -> setupEditTextView(layoutBinding)
            }
        }

        val savedEstateIndexSpinner = prefManager?.id_selected_estate ?: 0
        val savedAfdelingIndexSpinner = prefManager?.id_selected_afdeling ?: 0
        val savedBlockIndexSpinner = prefManager?.id_selected_blok ?: 0
        val savedAncakIndexSpinner = prefManager?.id_selected_ancak ?: 0
        val savedTPHIndexSpinner = prefManager?.id_selected_tph ?: 0

        inputMappings.forEach { (layoutBinding, key, inputType) ->
            updateTextInPertanyaan(layoutBinding, key)
            when (inputType) {
                InputType.SPINNER -> {
//                    when (layoutBinding.id) {
//                        R.id.layoutEstate -> {
//
//                            val bUnitNames = bUnitCodeList.map { it.BUnitName }
//                            setupSpinnerView(layoutView, bUnitNames)
//                        }
//                        R.id.layoutTipePanen->{
//                            val tipePanenOptions = resources.getStringArray(R.array.tipe_panen_options).toList()
//                            setupSpinnerView(layoutView, tipePanenOptions)
//                        }
//                        else -> {
//                            // Set empty list for any other spinner
//                            setupSpinnerView(layoutView, emptyList())
//                        }
//
//                    }
                }
                InputType.EDITTEXT -> setupEditTextView(layoutBinding)
            }
        }

        // Load all BUnit codes first
//        tphViewModel.getAllBUnitCodes().observe(this) { bUnitCodes ->
//            bUnitCodesList = bUnitCodes
//            val estateNames = bUnitCodes.map { it.BUnitName }
//            setupSpinnerView(binding.layoutEstate, estateNames)
//
//            if (savedEstateIndexSpinner >= 0 && savedEstateIndexSpinner < estateNames.size) {
//                binding.layoutEstate.spPanenTBS.setSelectedIndex(savedEstateIndexSpinner)
//                selectedBUnitSpinnerIndex = savedEstateIndexSpinner
//                selectedEstate = estateNames[savedEstateIndexSpinner]
//                selectedBUnitCodeValue = bUnitCodes[savedEstateIndexSpinner].BUnitCode
//
//                // Load division codes based on selected estate
//                tphViewModel.getDivisionCodesByBUnitCode(selectedBUnitCodeValue!!).observe(this) { divisionCodes ->
//                    divisionCodesList = divisionCodes
//                    val divisionNames = divisionCodes.map { it.DivisionName }
//                    setupSpinnerView(binding.layoutAfdeling, divisionNames)
//
//                    if (savedAfdelingIndexSpinner >= 0 && savedAfdelingIndexSpinner < divisionNames.size) {
//                        binding.layoutAfdeling.spPanenTBS.setSelectedIndex(savedAfdelingIndexSpinner)
//                        selectedDivisionSpinnerIndex = savedAfdelingIndexSpinner
//                        selectedAfdeling = divisionNames[savedAfdelingIndexSpinner]
//                        selectedDivisionCodeValue = divisionCodes[savedAfdelingIndexSpinner].DivisionCode
//
//                        // Load field codes based on selected division
//                        tphViewModel.getFieldCodesByBUnitAndDivision(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!).observe(this) { fieldCodes ->
//                            fieldCodesList = fieldCodes
//                            val fieldNames = fieldCodes.map { it.FieldName }
//                            setupSpinnerView(binding.layoutBlok, fieldNames)
//
//                            if (savedBlockIndexSpinner >= 0 && savedBlockIndexSpinner < fieldNames.size) {
//                                binding.layoutBlok.spPanenTBS.setSelectedIndex(savedBlockIndexSpinner)
//                                selectedFieldCodeSpinnerIndex = savedBlockIndexSpinner
//                                selectedBlok = fieldNames[savedBlockIndexSpinner]
//                                selectedFieldCodeValue = fieldCodes[savedBlockIndexSpinner].FieldCode
//
//                                tphViewModel.getAncakByFieldCode(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!).observe(this) { dataList ->
//                                    tphDataList = dataList
//                                    val ancakList = dataList.map { it.ancak.toString() }.distinct()
//                                    setupSpinnerView(binding.layoutAncak, ancakList)
//
//                                    if (savedAncakIndexSpinner >= 0 && savedAncakIndexSpinner < ancakList.size) {
//                                        binding.layoutAncak.spPanenTBS.setSelectedIndex(savedAncakIndexSpinner)
//                                        selectedAncakSpinnerIndex = savedAncakIndexSpinner
//                                        selectedAncak = ancakList[savedAncakIndexSpinner]
//
//                                        val filteredTPHList = tphDataList.filter { it.ancak.toString() == selectedAncak }
//                                        tphIds = filteredTPHList.map { it.id }
//
//                                        // Load TPH data based on selected ancak
//                                        tphViewModel.getTPHByAncakNumbers(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!, tphIds).observe(this) { tphModels ->
//                                            val tphNames = tphModels.map { it.tph }.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
//                                            setupSpinnerView(binding.layoutTPH, tphNames)
//
//                                            if (savedTPHIndexSpinner >= 0 && savedTPHIndexSpinner < tphNames.size) {
//                                                binding.layoutTPH.spPanenTBS.setSelectedIndex(savedTPHIndexSpinner)
//                                                selectedTPHSpinnerIndex = savedTPHIndexSpinner
//                                                selectedTPH = tphNames[savedTPHIndexSpinner]
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    // Modified setupSpinnerView to handle selection changes properly
    private fun setupSpinnerView(layoutBinding: PertanyaanSpinnerLayoutBinding, data: List<String>) {
        with(layoutBinding) {
            spPanenTBS.visibility = View.VISIBLE
            etPanenTBS.visibility = View.GONE

            spPanenTBS.setItems(data)
            spPanenTBS.setOnItemSelectedListener { _, position, _, item ->
                tvError.visibility = View.GONE
                MCVSpinner.strokeColor = ContextCompat.getColor(root.context, R.color.graytextdark)

                when (tvTitleForm.text.toString()) {
                    stringXML(R.string.field_estate) -> {
                        selectedEstate = item.toString()
                        val selectedBUnitCode = bUnitCodesList.find { it.BUnitName == selectedEstate }?.BUnitCode
                        selectedBUnitCodeValue = selectedBUnitCode
                        selectedBUnitSpinnerIndex = position

                        // Clear dependent spinners
                        setupSpinnerView(binding.layoutAfdeling, emptyList())
                        setupSpinnerView(binding.layoutBlok, emptyList())
                        setupSpinnerView(binding.layoutAncak, emptyList())
                        setupSpinnerView(binding.layoutTPH, emptyList())

                        selectedAfdeling = ""
                        selectedDivisionCodeValue = null
                        selectedDivisionSpinnerIndex = null

                        selectedBlok = ""
                        selectedFieldCodeValue = null
                        selectedFieldCodeSpinnerIndex = null

                        selectedAncak = ""
                        selectedAncakInt = null
                        selectedAncakSpinnerIndex = null

                        selectedTPH = ""
                        selectedTPHInt = null
                        selectedTPHSpinnerIndex = null

                        selectedBUnitCode?.let { code ->
                            loadDivisionCodes(code)
                        }
                    }
                    stringXML(R.string.field_afdeling) -> {
                        selectedAfdeling = item.toString()
                        val selectedDivisionCode = divisionCodesList.find { it.DivisionName == selectedAfdeling }?.DivisionCode
                        selectedDivisionCodeValue = selectedDivisionCode
                        selectedDivisionSpinnerIndex = position

                        // Clear dependent spinners
                        setupSpinnerView(binding.layoutBlok, emptyList())
                        setupSpinnerView(binding.layoutAncak, emptyList())
                        setupSpinnerView(binding.layoutTPH, emptyList())

                        selectedBlok = ""
                        selectedFieldCodeValue = null
                        selectedFieldCodeSpinnerIndex = null

                        selectedAncak = ""
                        selectedAncakInt = null
                        selectedAncakSpinnerIndex = null

                        selectedTPH = ""
                        selectedTPHInt = null
                        selectedTPHSpinnerIndex = null

                        if (selectedBUnitCodeValue != null && selectedDivisionCode != null) {
                            loadFieldCodes(selectedBUnitCodeValue!!, selectedDivisionCode)
                        }
                    }
                    stringXML(R.string.field_blok) -> {
                        selectedBlok = item.toString()
                        selectedFieldCodeSpinnerIndex = position
                        selectedFieldCodeValue = fieldCodesList.find { it.FieldName == selectedBlok }?.FieldCode

                        setupSpinnerView(binding.layoutAncak, emptyList())
                        setupSpinnerView(binding.layoutTPH, emptyList())

                        selectedAncak = ""
                        selectedAncakInt = null
                        selectedAncakSpinnerIndex = null

                        selectedTPH = ""
                        selectedTPHInt = null
                        selectedTPHSpinnerIndex = null

                        if (selectedBUnitCodeValue != null && selectedDivisionCodeValue != null && selectedFieldCodeValue != null) {
                            loadAncakData(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!)
                        }
                    }
                    stringXML(R.string.field_ancak) -> {
                        selectedAncak = item.toString()
                        selectedAncakSpinnerIndex = position
                        selectedAncakInt = position

                        // Clear TPH spinner
                        setupSpinnerView(binding.layoutTPH, emptyList())

                        selectedTPH = ""
                        selectedTPHInt = null
                        selectedTPHSpinnerIndex = null

                        try {
                            val filteredTPHList = tphDataList.filter { it.ancak.toString() == selectedAncak }
                            tphIds = filteredTPHList.map { it.id }

                            if (selectedBUnitCodeValue != null && selectedDivisionCodeValue != null && selectedFieldCodeValue != null && tphIds.isNotEmpty()) {
                                loadTPHData(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!, tphIds)
                            }
                        } catch (e: Exception) {
                            Log.e("TPH_LIFECYCLE", "Error in ancak selection", e)
                        }
                    }
                    stringXML(R.string.field_tph) -> {
                        selectedTPH = item.toString()
                        selectedTPHInt = position
                        selectedTPHSpinnerIndex = position
                    }
                }
            }
        }
    }


    private fun setupEditTextView(layoutBinding: PertanyaanSpinnerLayoutBinding) {
        with(layoutBinding) {
            spPanenTBS.visibility = View.GONE
            etPanenTBS.visibility = View.VISIBLE


            if (layoutBinding == binding.layoutUserInput) {
                val savedUserInput = prefManager?.user_input ?: ""
                if (savedUserInput.isNotEmpty()) {
                    etPanenTBS.setText(savedUserInput)
                    userInput = savedUserInput  // Update the userInput variable as well
                }
            }

            etPanenTBS.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Hide keyboard
                    val imm = application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)

                    binding.layoutEstate.spPanenTBS.requestFocus()
                    true
                } else {
                    false
                }
            }

            etPanenTBS.addTextChangedListener(object : TextWatcher {
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


    private fun loadDivisionCodes(bUnitCode: Int) {
        tphViewModel.getDivisionCodesByBUnitCode(bUnitCode).observe(this) { divisionCodes ->
            divisionCodesList = divisionCodes // Store the full list
            val divisionNames = divisionCodes.map { it.DivisionName }
            setupSpinnerView(binding.layoutAfdeling, divisionNames)

            setupSpinnerView(binding.layoutBlok, emptyList())
            setupSpinnerView(binding.layoutAncak, emptyList())
            setupSpinnerView(binding.layoutTPH, emptyList())

        }
    }

    private fun loadFieldCodes(bUnitCode: Int, divisionCode: Int) {
        tphViewModel.getFieldCodesByBUnitAndDivision(bUnitCode, divisionCode).observe(this) { fieldCodes ->
            fieldCodesList = fieldCodes // Store the full list
            val fieldNames = fieldCodes.map { it.FieldName }
            setupSpinnerView(binding.layoutBlok, fieldNames)

        }
    }

    private fun loadAncakData(bUnitCode: Int, divisionCode: Int, fieldCode: Int) {
        tphViewModel.getAncakByFieldCode(bUnitCode, divisionCode, fieldCode).observe(this) { dataList ->
            tphDataList = dataList // Store the full list
            val ancakList = dataList.map { it.ancak.toString() }.distinct()

            setupSpinnerView(binding.layoutAncak, ancakList)

        }
    }


    private fun loadTPHData(bUnitCode: Int, divisionCode: Int, fieldCode: Int, tphIds: List<Int>) {


        tphViewModel.getTPHByAncakNumbers(bUnitCode, divisionCode, fieldCode, tphIds)
            .observe(this) { tphModels ->
                try {
                    if (tphModels.isEmpty()) {

                        binding.layoutTPH.spPanenTBS.setItems(emptyList<String>())
                        return@observe
                    }

                    tphNames = tphModels.map { it.tph }.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE } // Handles numeric sorting, even if `tph` is a string.

                    binding.layoutTPH.spPanenTBS.setItems(tphNames)


                } catch (e: Exception) {
                    Log.e("TPH_DEBUG", "Error in loadTPHData: ${e.message}")
                }
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
                        binding.layoutBlok -> selectedBlok.isEmpty()
                        binding.layoutAncak -> selectedAncak.isEmpty()
                        binding.layoutTPH -> selectedTPH.isEmpty()
                        else -> layoutBinding.spPanenTBS.selectedIndex == -1
                    }
                }
                InputType.EDITTEXT -> {
                    when (key) {
                        "User Input" -> userInput.trim().isEmpty()
                        else -> layoutBinding.etPanenTBS.text.toString().trim().isEmpty()
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
                Log.d("FileCheck", "File download not required.")
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
}