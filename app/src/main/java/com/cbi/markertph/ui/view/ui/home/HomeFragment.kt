package com.cbi.markertph.ui.view.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.cbi.markertph.R
import com.cbi.markertph.data.model.BUnitCodeModel
import com.cbi.markertph.data.model.DivisionCodeModel
import com.cbi.markertph.data.model.FieldCodeModel
import com.cbi.markertph.data.model.TPHModel
import com.cbi.markertph.data.repository.TPHRepository
import com.cbi.markertph.databinding.FragmentHomeBinding
import com.cbi.markertph.databinding.PertanyaanSpinnerLayoutBinding
import com.cbi.markertph.ui.viewModel.LocationViewModel
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.AlertDialogUtility
import com.cbi.markertph.utils.AppUtils
import com.cbi.markertph.utils.AppUtils.stringXML
import com.cbi.markertph.utils.AppUtils.vibrate
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tphViewModel: TPHViewModel
    private lateinit var locationViewModel: LocationViewModel
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

    private var bUnitCodesList: List<BUnitCodeModel> = emptyList() // Add this variable to store the full list
    private var divisionCodesList: List<DivisionCodeModel> = emptyList() // Add this to store division codes
    private var fieldCodesList: List<FieldCodeModel> = emptyList() // Add this
    private var tphDataList: List<TPHModel> = emptyList() // Add this to store the full TPH data
    private var filteredTPHList: List<TPHModel> = emptyList() // Add this to store the full TPH data
    var tphNames: List<String> = emptyList()

    private var selectedBUnitCodeValue: Int? = null
    private var selectedDivisionCodeValue: Int? = null // Add this
    private var selectedFieldCodeValue: Int? = null // Add this
    private var selectedAncakInt: Int? = null // Add this to store selected ancak

    enum class InputType {
        SPINNER,
        EDITTEXT
    }
    private var savedBUnitCode: Int? = null
    private var savedDivisionCode: Int? = null
    private var savedFieldCode: Int? = null
    private var savedAncak: String? = null
    private var savedTPHIds: List<Int> = emptyList()

    private lateinit var inputMappings: List<Triple<PertanyaanSpinnerLayoutBinding, String, InputType>>
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationViewModel.startLocationUpdates()
            } else {
                showSnackbar(stringXML(R.string.location_permission_denied))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)


        initViewModel()
        setupLayout()
        setAppVersion()
        Log.d("LIFECYCLE", "onCreateView")
//        Log.d("testing",fieldCodesList.toString() )
//        Log.d("testing",tphDataList.toString() )
//
//
        Log.d("testing",selectedBUnitCodeValue.toString() )
        Log.d("testing",selectedDivisionCodeValue.toString() )
        Log.d("testing",selectedFieldCodeValue.toString() )

        val mbSaveDataTPH = binding.mbSaveDataTPH
        mbSaveDataTPH.setOnClickListener{
            if (validateAndShowErrors()) {
                AlertDialogUtility.withTwoActions(
                    requireContext(),
                    stringXML(R.string.al_save),
                    stringXML(R.string.confirmation_dialog_title),
                    stringXML(R.string.confirmation_dialog_description),
                    "warning.json"
                ) {

                    val app_version = requireContext().getString(R.string.app_version)
                    tphViewModel.insertPanenTBSVM(
                        tanggal = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
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
                        id_tph = 0,
                        latitude = lat.toString(),
                        longitude = lon.toString(),
                        app_version = app_version
                    )

                    tphViewModel.insertDBTPH.observe(requireActivity()) { isInserted ->
                        if (isInserted){
                            AlertDialogUtility.alertDialogAction(
                                requireContext(),
                                stringXML(R.string.al_success_save_local),
                                stringXML(R.string.al_description_success_save_local),
                                "success.json"
                                ) {

                            }
                        }else{
                            AlertDialogUtility.alertDialogAction(
                                requireContext(),
                                stringXML(R.string.al_failed_save_local),
                                stringXML(R.string.al_description_failed_save_local),
                                "warning.json"
                            ) {
                            }
                            Toast.makeText(
                                requireContext(),
                                stringXML(R.string.toast_failed_save_local),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }


                }

            }else{
            }
        }

        Log.d("testing","sdjkfkljdsf" )

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })

        return binding.root
    }


    private fun initViewModel() {
        tphViewModel = ViewModelProvider(
            this,
            TPHViewModel.Factory(requireActivity().application, TPHRepository(requireActivity()))
        )[TPHViewModel::class.java]

        // Change this to use requireActivity() instead of 'this'
        val statusLocation = binding.statusLocation
        locationViewModel = ViewModelProvider(
            requireActivity(),  // Change this from 'this' to requireActivity()
            LocationViewModel.Factory(requireActivity().application, statusLocation, requireActivity())
        )[LocationViewModel::class.java]
    }

    private fun setAppVersion() {
        val versionTextView: TextView = binding.versionApp
        val appVersion = AppUtils.getAppVersion(requireContext()) // Use AppUtils here
        versionTextView.text = "$appVersion"
    }

    private fun setupLayout() {
        inputMappings = listOf(
            Triple(binding.layoutUserInput, stringXML(R.string.field_nama_user), InputType.EDITTEXT),
            Triple(binding.layoutEstate, stringXML(R.string.field_estate), InputType.SPINNER),
            Triple(binding.layoutAfdeling, stringXML(R.string.field_afdeling), InputType.SPINNER),
            Triple(binding.layoutBlok, stringXML(R.string.field_blok), InputType.SPINNER),
            Triple(binding.layoutAncak, stringXML(R.string.field_ancak), InputType.SPINNER),
                    Triple(binding.layoutTPH, stringXML(R.string.field_tph), InputType.SPINNER)
        )

        // Initialize all spinners empty first
        inputMappings.forEach { (layoutBinding, key, inputType) ->
            updateTextInPertanyaan(layoutBinding, key)
            when (inputType) {
                InputType.SPINNER -> setupSpinnerView(layoutBinding, emptyList())
                InputType.EDITTEXT -> setupEditTextView(layoutBinding)
            }
        }

        tphViewModel.getAllBUnitCodes().observe(viewLifecycleOwner) { bUnitCodes ->
            bUnitCodesList = bUnitCodes // Store the full list
            val estateNames = bUnitCodes.map { it.BUnitName }
            setupSpinnerView(binding.layoutEstate, estateNames)
            setupSpinnerView(binding.layoutAfdeling, emptyList())
            setupSpinnerView(binding.layoutBlok, emptyList())
            setupSpinnerView(binding.layoutAncak, emptyList())
        }
    }

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
                        savedBUnitCode = selectedBUnitCode  // Save state
                        selectedBUnitCode?.let { code ->
                            loadDivisionCodes(code)
                        }
                    }
                    stringXML(R.string.field_afdeling) -> {
                        selectedAfdeling = item.toString()
                        val selectedDivisionCode = divisionCodesList.find { it.DivisionName == selectedAfdeling }?.DivisionCode
                        selectedDivisionCodeValue = selectedDivisionCode
                        savedDivisionCode = selectedDivisionCode  // Save state
                        if (selectedBUnitCodeValue != null && selectedDivisionCode != null) {
                            loadFieldCodes(selectedBUnitCodeValue!!, selectedDivisionCode)
                        }
                    }
                    stringXML(R.string.field_blok) -> {
                        selectedBlok = item.toString()
                        selectedFieldCodeValue = fieldCodesList.find { it.FieldName == selectedBlok }?.FieldCode
                        savedFieldCode = selectedFieldCodeValue  // Save state
                        if (selectedBUnitCodeValue != null && selectedDivisionCodeValue != null && selectedFieldCodeValue != null) {
                            loadAncakData(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!)
                        }
                    }
                    stringXML(R.string.field_ancak) -> {
                        Log.d("TPH_LIFECYCLE", "Ancak selection started")
                        selectedAncak = item.toString()
                        Log.d("TPH_LIFECYCLE", "Selected Ancak: $selectedAncak")
                        Log.d("TPH_LIFECYCLE", "TPH Data List size: ${tphDataList.size}")
                        savedAncak = selectedAncak
                        try {
                            val filteredTPHList = tphDataList.filter {
                                Log.d("TPH_LIFECYCLE", "Filtering TPH with ancak: ${it.ancak}")
                                it.ancak.toString() == selectedAncak
                            }
                            Log.d("TPH_LIFECYCLE", "Filtered TPH List: $filteredTPHList")

                            val tphIds = filteredTPHList.map { it.id }
                            Log.d("TPH_LIFECYCLE", "TPH IDs for filtering: $tphIds")
                            savedTPHIds = tphIds  // Save state
                            if (selectedBUnitCodeValue == null || selectedDivisionCodeValue == null || selectedFieldCodeValue == null) {
                                Log.e("TPH_LIFECYCLE", "Required values are null - bUnit: $selectedBUnitCodeValue, division: $selectedDivisionCodeValue, field: $selectedFieldCodeValue")
                                return@setOnItemSelectedListener
                            }

                            if (tphIds.isNotEmpty()) {
                                Log.d("TPH_LIFECYCLE", "Calling loadTPHData")
                                loadTPHData(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!, tphIds)
                            } else {
                                Log.e("TPH_LIFECYCLE", "No TPH IDs found for selected ancak")
                            }
                        } catch (e: Exception) {
                            Log.e("TPH_LIFECYCLE", "Error in ancak selection", e)
                            e.printStackTrace()
                        }
                    }
                    stringXML(R.string.field_tph) -> {
//                        try {
//                            if (filteredTPHList.isEmpty()) {
//                                Log.d("TPH_DEBUG", "filteredTPHList is empty, reinitializing data...")
//                                // If needed, reload your data here
//                                if (selectedBUnitCodeValue != null && selectedDivisionCodeValue != null &&
//                                    selectedFieldCodeValue != null && selectedAncak.isNotEmpty()) {
//                                    loadAncakData(selectedBUnitCodeValue!!, selectedDivisionCodeValue!!, selectedFieldCodeValue!!)
//                                }
//                                return@setOnItemSelectedListener
//                            }
//
//                            val selectedTempTPH = item.toString()
                            selectedTPH = item.toString()

                        Log.d("testing", selectedTPH.toString())
                            // Safely find the TPH model
//                            val selectedTPHModel = filteredTPHList.find {
//                                it.tph.toString() == selectedTempTPH
//                            }
//
//                            if (selectedTPHModel != null) {
//                                selectedTPH = selectedTempTPH
//                                Log.d("testing", "Selected TPH: $selectedTPH")
//                                Log.d("testing", "Selected TPH Key: ${selectedTPHModel.id}")
//                            } else {
//                                Log.d("Error", "No matching TPHModel found for TPH: $selectedTempTPH")
//                                // Reset the selection if needed
//                                selectedTPH = ""
//                            }
//                        } catch (e: Exception) {
//                            Log.e("TPH_ERROR", "Error in TPH selection: ${e.message}")
//                            selectedTPH = ""
//                            // Reset spinner if needed
//
//                        }
                    }
                }
            }
        }
    }

    private fun loadDivisionCodes(bUnitCode: Int) {
        tphViewModel.getDivisionCodesByBUnitCode(bUnitCode).observe(viewLifecycleOwner) { divisionCodes ->
            divisionCodesList = divisionCodes // Store the full list
            val divisionNames = divisionCodes.map { it.DivisionName }
            setupSpinnerView(binding.layoutAfdeling, divisionNames)

            setupSpinnerView(binding.layoutBlok, emptyList())
            setupSpinnerView(binding.layoutAncak, emptyList())

        }
    }

    private fun loadFieldCodes(bUnitCode: Int, divisionCode: Int) {
        tphViewModel.getFieldCodesByBUnitAndDivision(bUnitCode, divisionCode).observe(viewLifecycleOwner) { fieldCodes ->
            fieldCodesList = fieldCodes // Store the full list
            val fieldNames = fieldCodes.map { it.FieldName }
            setupSpinnerView(binding.layoutBlok, fieldNames)
            setupSpinnerView(binding.layoutAncak, emptyList()) // Clear TPH spinner
            setupSpinnerView(binding.layoutTPH, emptyList()) // Clear TPH spinner
        }
    }

    private fun loadAncakData(bUnitCode: Int, divisionCode: Int, fieldCode: Int) {
        tphViewModel.getAncakByFieldCode(bUnitCode, divisionCode, fieldCode).observe(viewLifecycleOwner) { dataList ->
            tphDataList = dataList // Store the full list
            val ancakList = dataList.map { it.ancak.toString() }.distinct()

            setupSpinnerView(binding.layoutAncak, ancakList)
            setupSpinnerView(binding.layoutTPH, emptyList()) // Clear TPH spinner

        }
    }

    private fun loadTPHData(bUnitCode: Int, divisionCode: Int, fieldCode: Int, tphIds: List<Int>) {
        Log.d("TPH_DEBUG", "Loading TPH Data with ids: $tphIds")


        if (!isAdded) {
            Log.e("TPH_DEBUG", "Fragment not attached, skipping loadTPHData")
            return
        }

        tphViewModel.getTPHByAncakNumbers(bUnitCode, divisionCode, fieldCode, tphIds)
            .observe(viewLifecycleOwner) { tphModels ->
                try {
                    if (tphModels.isEmpty()) {

                        binding.layoutTPH.spPanenTBS.setItems(emptyList<String>())
                        return@observe
                    }

                    tphNames = tphModels.map { it.tph }
                    Log.d("TPH_DEBUG", "Setting ${tphNames.size} items to spinner")

                    if (view != null && isAdded) {
                        binding.layoutTPH.spPanenTBS.setItems(tphNames)
                    }
                } catch (e: Exception) {
                    Log.e("TPH_DEBUG", "Error in loadTPHData: ${e.message}")
                }
            }
    }


    private fun setupEditTextView(layoutBinding: PertanyaanSpinnerLayoutBinding) {
        with(layoutBinding) {
            spPanenTBS.visibility = View.GONE
            etPanenTBS.visibility = View.VISIBLE

            // Handle enter key
            etPanenTBS.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Hide keyboard
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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

    @SuppressLint("ResourceAsColor")
    private fun validateAndShowErrors(): Boolean {

        Log.d("testing","skdjfklajslkdf" )
        var isValid = true
        val missingFields = mutableListOf<String>()

        if (!locationEnable || lat == 0.0 || lon == 0.0) {
            isValid = false
            requireContext().vibrate()
            AlertDialogUtility.withSingleAction(
                requireContext(),
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
                    requireContext(),
                    R.color.colorRedDark
                )
                missingFields.add(key)
                isValid = false
                Log.d("Validation", "Field $key is empty")
            } else {
                layoutBinding.tvError.visibility = View.GONE
                layoutBinding.MCVSpinner.strokeColor = ContextCompat.getColor(
                    requireContext(),
                    R.color.graytextdark
                )
                Log.d("Validation", "Field $key is valid")
            }
        }

        if (!isValid) {
            requireContext().vibrate()
            AlertDialogUtility.withSingleAction(
                requireContext(),
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

    private fun updateTextInPertanyaan(layoutBinding: PertanyaanSpinnerLayoutBinding, text: String) {
        layoutBinding.tvPanenTBS.text = text
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showSnackbar(stringXML(R.string.location_permission_message))
            isPermissionRationaleShown = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("LIFECYCLE", "onResume")
        locationViewModel.refreshLocationStatus()

        locationViewModel.locationPermissions.observe(viewLifecycleOwner) { isLocationEnabled ->
            if (!isLocationEnabled) {
                requestLocationPermission()
            } else {
                locationViewModel.startLocationUpdates()
            }
        }

        locationViewModel.locationIconState.observe(viewLifecycleOwner) { isEnabled ->
            // Update your UI based on location icon state if needed
            binding.statusLocation.setImageResource(R.drawable.baseline_location_pin_24)
            binding.statusLocation.imageTintList = ColorStateList.valueOf(
                requireActivity().resources.getColor(
                    if (isEnabled) R.color.greenbutton else R.color.colorRed
                )
            )
        }

        locationViewModel.locationData.observe(viewLifecycleOwner) { location ->
            locationEnable = true
            lat = location.latitude
            lon = location.longitude
        }

        locationViewModel.locationAccuracy.observe(viewLifecycleOwner) { accuracy ->
            binding.accuracyLocation.text = String.format("%.1f m", accuracy)
        }

    }


    override fun onPause() {
        super.onPause()
        Log.d("LIFECYCLE", "onPause")
        locationViewModel.stopLocationUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("LIFECYCLE", "onDestroyView")
        _binding = null
        locationViewModel.stopLocationUpdates()
    }

    private fun showExitDialog() {
        AlertDialogUtility.withTwoActions(
            requireContext(),
            "Batal",
            "Keluar",
            "Apakah anda yakin untuk keluar dari aplikasi MobilePro?",
            "warning.json",

        ){
            requireActivity().finish()
        }
    }
}