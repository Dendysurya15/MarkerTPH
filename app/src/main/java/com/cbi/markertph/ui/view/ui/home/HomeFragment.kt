package com.cbi.markertph.ui.view.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cbi.markertph.R
import com.cbi.markertph.data.repository.TPHRepository
import com.cbi.markertph.databinding.FragmentHomeBinding
import com.cbi.markertph.databinding.PertanyaanSpinnerLayoutBinding
import com.cbi.markertph.ui.viewModel.LocationViewModel
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.AlertDialogUtility
import com.cbi.markertph.utils.AppUtils
import com.cbi.markertph.utils.AppUtils.vibrate
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
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

    private var selectedEstate: String = ""
    private var selectedAfdeling: String = ""
    private var selectedBlok: String = ""
    private var selectedTPH: String = ""


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationViewModel.startLocationUpdates()
            } else {
                showSnackbar("Location permission denied.")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        initViewModel()
        setupLayout()
        setAppVersion()
        val mbSaveDataTPH = binding.mbSaveDataTPH
        mbSaveDataTPH.setOnClickListener{


            if (validateAndShowErrors()) {
                AlertDialogUtility.withTwoActions(
                    requireContext(),
                    "Simpan",
                    getString(R.string.confirmation_dialog_title),
                    getString(R.string.confirmation_dialog_description),
                    "warning.json"
                ) {

                    val app_version = requireContext().getString(R.string.app_version)
                    tphViewModel.insertPanenTBSVM(
                        tanggal = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                            Date()
                        ),
                        estate = selectedEstate,
                        id_estate = 1,
                        afdeling = selectedAfdeling,
                        id_afdeling =2,
                        blok = selectedBlok,
                        id_blok = 3,
                        tph = selectedTPH,
                        id_tph = 4,
                        latitude = lat.toString(),
                        longitude = lon.toString(),
                        app_version = app_version
                    )

                    tphViewModel.insertDBTPH.observe(requireActivity()) { isInserted ->
                        if (isInserted){
                            AlertDialogUtility.alertDialogAction(
                                requireContext(),
                                "Sukses Simpan",
                                "Data berhasil disimpan!",
                                "success.json"
                                ) {
                            }
                        }else{
                            Toast.makeText(
                                requireContext(),
                                "Error Saving Data",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }


                }


            }else{

            }



        }
        return binding.root
    }


    // In HomeFragment
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
        val spinnerMappings = listOf(
            Pair(binding.layoutEstate, "Estate"),
            Pair(binding.layoutAfdeling, "Afdeling"),
            Pair(binding.layoutBlok, "Blok"),
            Pair(binding.layoutTPH, "TPH"),
        )
        val dummyData = mapOf(
            "Estate" to listOf("NBE", "SLE"),
            "Afdeling" to listOf("OA", "OB", "OC"),
            "Blok" to listOf("A03", "A04", "A05"),
            "TPH" to listOf("TPH 10", "TPH 11", "TPH 12")
        )

        spinnerMappings.forEach { (layoutBinding, key) ->
            val data = dummyData[key] ?: emptyList()
            updateTextInPertanyaanSpinner(layoutBinding, key)
            setupSpinnerDropdown(layoutBinding, data)
        }
    }

    private fun setupSpinnerDropdown(
        layoutBinding: PertanyaanSpinnerLayoutBinding,
        items: List<String>
    ) {
        val spinner = layoutBinding.spPanenTBS
        spinner.setItems(items)

        spinner.setOnItemSelectedListener { _, _, _, item ->
            val selectedItem = item.toString()
            when (layoutBinding) {
                binding.layoutEstate -> selectedEstate = selectedItem
                binding.layoutAfdeling -> selectedAfdeling = selectedItem
                binding.layoutBlok -> selectedBlok = selectedItem
                binding.layoutTPH -> selectedTPH = selectedItem
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun validateAndShowErrors(): Boolean {
        var isValid = true
        val missingFields = mutableListOf<String>()

        // Check each field and update UI immediately
        if (selectedEstate.isEmpty()) {
            binding.layoutEstate.tvError.visibility = View.VISIBLE
            missingFields.add("Estate")
            binding.layoutEstate.MCVSpinner.strokeColor = ContextCompat.getColor(requireContext(), R.color.colorRedDark)

            isValid = false
        }
        if (selectedAfdeling.isEmpty()) {
            binding.layoutAfdeling.tvError.visibility = View.VISIBLE
            missingFields.add("Afdeling")
            binding.layoutAfdeling.MCVSpinner.strokeColor = ContextCompat.getColor(requireContext(), R.color.colorRedDark)
            isValid = false
        }
        if (selectedBlok.isEmpty()) {
            binding.layoutBlok.tvError.visibility = View.VISIBLE
            missingFields.add("Blok")
            binding.layoutBlok.MCVSpinner.strokeColor = ContextCompat.getColor(requireContext(), R.color.colorRedDark)
            isValid = false
        }
        if (selectedTPH.isEmpty()) {
            binding.layoutTPH.tvError.visibility = View.VISIBLE
            missingFields.add("TPH")
            binding.layoutTPH.MCVSpinner.strokeColor = ContextCompat.getColor(requireContext(), R.color.colorRedDark)
            isValid = false
        }

        if (!isValid) {
            requireContext().vibrate()
            // Create a user-friendly error message
//            val errorMessage = buildString {
//                append("Mohon lengkapi data berikut:\n\n")
//                missingFields.forEachIndexed { index, field ->
//                    append("${index + 1}. $field")
//                    if (index < missingFields.size - 1) append("\n")
//                }
//            }

            AlertDialogUtility.withSingleAction(
                requireContext(),
                "Kembali",
                "Data Belum Lengkap!",
                "Mohon Lengkapi Data yang belum diisi!",
                "warning.json",
                R.color.colorRedDark
            ) {
                // Optional: Scroll to first error or highlight fields
            }
        }

        return isValid
    }

    private fun updateTextInPertanyaanSpinner(
        layoutBinding: PertanyaanSpinnerLayoutBinding,
        newText: String
    ) {
        layoutBinding.tvPanenTBS.text = newText // Directly use the binding reference
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
            showSnackbar("Location permission is required for this app. Change in Settings App")
            isPermissionRationaleShown = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()

        // Force refresh the location status when fragment resumes
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
    }

    override fun onPause() {
        super.onPause()
        locationViewModel.stopLocationUpdates()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        locationViewModel.stopLocationUpdates()
    }

}