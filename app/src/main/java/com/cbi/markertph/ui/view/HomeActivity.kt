package com.cbi.markertph.ui.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.cbi.markertph.R
import com.cbi.markertph.data.model.TPHModel
import com.cbi.markertph.data.repository.TPHRepository
import com.cbi.markertph.ui.viewModel.LocationViewModel
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.AlertDialogUtility
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner

class HomeActivity : AppCompatActivity() {
    private lateinit var locationViewModel: LocationViewModel
    private var locationEnable:Boolean = false
    private var isPermissionRationaleShown = false
    private lateinit var tphViewModel: TPHViewModel

    private var lat: Double? = null
    private var lon: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViewModel()
        setupLayout()


        val mbSaveDataTPH = findViewById<MaterialButton>(R.id.mbSaveDataTPH)
        mbSaveDataTPH.setOnClickListener{

            AlertDialogUtility.withTwoActions(
                this,
                "Simpan",
                getString(R.string.confirmation_dialog_title),
                getString(R.string.confirmation_dialog_description)
            ) {

                tphViewModel.insertPanenTBSVM(

                    tanggal = "2024-04-24 20:00:00",
                    estate = "Estate 1",
                    id_estate = 101,
                    afdeling = "Afdeling A",
                    id_afdeling = 202,
                    blok = "Blok X",
                    id_blok = 303,
                    tph = "TPH-1",
                    id_tph = 606,
                    latitude = "1.234567",
                    longitude = "103.456789"
                )

                tphViewModel.insertDBTPH.observe(this) { isInserted ->
                    if (isInserted){
                        AlertDialogUtility.alertDialogAction(
                            this,
                            "Sukses",
                            "Data berhasil disimpan!",

                            ) {
                            Toast.makeText(
                                this,
                                "sukses bro",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }else{
                        Toast.makeText(
                            this,
                            "Gagal bro",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }


            }

        }
    }

    private fun setupLayout() {

        val spinnerMappings = listOf(
            Pair(R.id.layoutEstate, "Estate"),
            Pair(R.id.layoutAfdeling, "Afdeling"),
            Pair(R.id.layoutBlok, "Blok"),
            Pair(R.id.layoutTPH, "TPH"),
        )
        val dummyData = mapOf(
            "Estate" to listOf("NBE", "SLE"),
            "Afdeling" to listOf("OA", "OB", "OC"),
            "Blok" to listOf("A03", "A04", "A05"),
            "TPH" to listOf("TPH 10", "TPH 11", "TPH 12")
        )

        spinnerMappings.forEach { (layoutId, key) ->
            val data = dummyData[key] ?: emptyList()
            updateTextInPertanyaanSpinner(layoutId, R.id.tvPanenTBS, key)
            setupSpinnerDropdown(layoutId, data)
        }

    }

    private fun setupSpinnerDropdown(layoutId: Int, items: List<String>) {
        val layout = findViewById<View>(layoutId)
        val spinner = layout.findViewById<MaterialSpinner>(R.id.spPanenTBS) // Ensure there's a Spinner in your layout


        spinner.setItems(items)


//        spinner.setOnItemSelectedListener { view, position, id, item ->
//            val selectedItem = item.toString()
//            Toast.makeText(this@HomeActivity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
//        }


    }



    private fun updateTextInPertanyaanSpinner(layoutId: Int, textViewId: Int, newText: String) {
        val includedLayout = findViewById<View>(layoutId)
        val textView = includedLayout.findViewById<TextView>(textViewId)
        textView.text = newText
    }

    private fun initViewModel() {

        tphViewModel = ViewModelProvider(
            this,
            TPHViewModel.Factory(application, TPHRepository(this))
        )[TPHViewModel::class.java]
        val status_location = findViewById<ImageView>(R.id.statusLocation)
        locationViewModel = ViewModelProvider(
            this,
            LocationViewModel.Factory(application,status_location, this)
        )[LocationViewModel::class.java]
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showSnackbar("Location permission is required for this app. Change in Settings App")
            isPermissionRationaleShown = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationViewModel.startLocationUpdates()
            } else {
                showSnackbar("Location permission denied.")
            }
        }

    override fun onResume() {
        super.onResume()
        locationViewModel.locationPermissions.observe(this) { isLocationEnabled ->
            if (!isLocationEnabled) {
                requestLocationPermission()
            } else {
                locationViewModel.startLocationUpdates()
            }
        }

        locationViewModel.locationData.observe(this) { location ->
            locationEnable = true
            lat = location.latitude
            lon = location.longitude
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