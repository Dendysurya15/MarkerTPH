package com.cbi.markertph.ui.view

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.cbi.markertph.R
import com.cbi.markertph.data.model.BUnitCodeModel
import com.cbi.markertph.data.model.DivisionCodeModel
import com.cbi.markertph.data.model.FieldCodeModel
import com.cbi.markertph.data.model.TPHModel
import com.cbi.markertph.data.repository.TPHRepository
import com.cbi.markertph.databinding.ActivityHomeBinding
import com.cbi.markertph.databinding.PertanyaanSpinnerLayoutBinding
import com.cbi.markertph.ui.view.ui.home.HomeFragment.InputType
import com.cbi.markertph.ui.viewModel.LocationViewModel
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.AlertDialogUtility
import com.cbi.markertph.utils.AppUtils
import com.cbi.markertph.utils.AppUtils.stringXML
import com.cbi.markertph.utils.AppUtils.vibrate
import com.cbi.markertph.utils.PrefManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
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

    private var selectedDivisionSpinnerIndex: Int? = null
    private var selectedBUnitSpinnerIndex: Int? = null
    private var selectedFieldCodeSpinnerIndex: Int? = null
    private var selectedAncakSpinnerIndex: Int? = null
    private var selectedTPHSpinnerIndex: Int? = null


    enum class InputType {
        SPINNER,
        EDITTEXT
    }

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
        setupLayout()
        setAppVersion()


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

        // Load all BUnit codes first
        tphViewModel.getAllBUnitCodes().observe(this) { bUnitCodes ->
            bUnitCodesList = bUnitCodes
            val estateNames = bUnitCodes.map { it.BUnitName }
            setupSpinnerView(binding.layoutEstate, estateNames)

            if (savedEstateIndexSpinner >= 0 && savedEstateIndexSpinner < estateNames.size) {
                binding.layoutEstate.spPanenTBS.setSelectedIndex(savedEstateIndexSpinner)
                selectedBUnitSpinnerIndex = savedEstateIndexSpinner
                selectedEstate = estateNames[savedEstateIndexSpinner]
                selectedBUnitCodeValue = bUnitCodes[savedEstateIndexSpinner].BUnitCode

                // Load division codes based on selected estate
                tphViewModel.getDivisionCodesByBUnitCode(selectedBUnitCodeValue!!).observe(this) { divisionCodes ->
                    divisionCodesList = divisionCodes
                    val divisionNames = divisionCodes.map { it.DivisionName }
                    setupSpinnerView(binding.layoutAfdeling, divisionNames)

                    if (savedAfdelingIndexSpinner >= 0 && savedAfdelingIndexSpinner < divisionNames.size) {
                        binding.layoutAfdeling.spPanenTBS.setSelectedIndex(savedAfdelingIndexSpinner)
                        selectedDivisionSpinnerIndex = savedAfdelingIndexSpinner
                        selectedAfdeling = divisionNames[savedAfdelingIndexSpinner]
                        selectedDivisionCodeValue = divisionCodes[savedAfdelingIndexSpinner].DivisionCode

                        // Load field codes based on selected division
                        tphViewModel.getFieldCodesByBUnitAndDivision(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!).observe(this) { fieldCodes ->
                            fieldCodesList = fieldCodes
                            val fieldNames = fieldCodes.map { it.FieldName }
                            setupSpinnerView(binding.layoutBlok, fieldNames)

                            if (savedBlockIndexSpinner >= 0 && savedBlockIndexSpinner < fieldNames.size) {
                                binding.layoutBlok.spPanenTBS.setSelectedIndex(savedBlockIndexSpinner)
                                selectedFieldCodeSpinnerIndex = savedBlockIndexSpinner
                                selectedBlok = fieldNames[savedBlockIndexSpinner]
                                selectedFieldCodeValue = fieldCodes[savedBlockIndexSpinner].FieldCode

                                tphViewModel.getAncakByFieldCode(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!).observe(this) { dataList ->
                                    tphDataList = dataList
                                    val ancakList = dataList.map { it.ancak.toString() }.distinct()
                                    setupSpinnerView(binding.layoutAncak, ancakList)

                                    if (savedAncakIndexSpinner >= 0 && savedAncakIndexSpinner < ancakList.size) {
                                        binding.layoutAncak.spPanenTBS.setSelectedIndex(savedAncakIndexSpinner)
                                        selectedAncakSpinnerIndex = savedAncakIndexSpinner
                                        selectedAncak = ancakList[savedAncakIndexSpinner]

                                        val filteredTPHList = tphDataList.filter { it.ancak.toString() == selectedAncak }
                                        tphIds = filteredTPHList.map { it.id }

                                        // Load TPH data based on selected ancak
                                        tphViewModel.getTPHByAncakNumbers(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!, tphIds).observe(this) { tphModels ->
                                            val tphNames = tphModels.map { it.tph }.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                                            setupSpinnerView(binding.layoutTPH, tphNames)

                                            if (savedTPHIndexSpinner >= 0 && savedTPHIndexSpinner < tphNames.size) {
                                                binding.layoutTPH.spPanenTBS.setSelectedIndex(savedTPHIndexSpinner)
                                                selectedTPHSpinnerIndex = savedTPHIndexSpinner
                                                selectedTPH = tphNames[savedTPHIndexSpinner]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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

                when (tvPanenTBS.text.toString()) {
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

                        // Clear dependent spinners
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

    override fun onResume() {
        super.onResume()
        locationViewModel.refreshLocationStatus()

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
        layoutBinding.tvPanenTBS.text = text
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

    override fun onPause() {
        super.onPause()
        locationViewModel.stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationViewModel.stopLocationUpdates()
    }
}