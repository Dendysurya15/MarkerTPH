package com.cbi.markertph.ui.view.ui.uploadData

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cbi.markertph.R
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ANCAK
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ANCAK_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_APP_VERSION
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_BLOK
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_BLOK_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ESTATE
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ESTATE_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_LAT
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_LON
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_TANGGAL
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_TPH
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_TPH_ID
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_USER_INPUT
import com.cbi.markertph.data.model.UploadData
import com.cbi.markertph.data.repository.TPHRepository
import com.cbi.markertph.databinding.FragmentUploadDataBinding
import com.cbi.markertph.ui.adapter.TPHAdapter
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.AlertDialogUtility
import com.cbi.markertph.utils.AppUtils
import com.cbi.markertph.utils.AppUtils.stringXML
import com.cbi.markertph.utils.AppUtils.vibrate
import com.cbi.markertph.utils.LoadingDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.leinardi.android.speeddial.SpeedDialActionItem

class UploadDataFragment : Fragment() {

    private var _binding: FragmentUploadDataBinding? = null
    private lateinit var tphViewModel: TPHViewModel
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private val dataTPHList = mutableListOf<Map<String, Any>>()
    private lateinit var tphAdapter: TPHAdapter

    private var currentArchiveState = 0 // 0 for Tersimpan, 1 for Terupload
    private var isSettingUpCheckbox = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val uploadDataViewModel = ViewModelProvider(this).get(UploadDataViewModel::class.java)

        _binding = FragmentUploadDataBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.tableHeader.headerCheckBoxPanen.visibility = View.VISIBLE
        binding.layoutItemTersimpan.apply {
            // Change the stroke color of the LayerDrawable
            val layerDrawable = binding.layoutItemTersimpan.background as LayerDrawable
            val strokeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
            strokeDrawable.setStroke(
                4, // Stroke width in dp
                ContextCompat.getColor(requireContext(), R.color.greendarkerbutton) // New stroke color
            )

            // Update text color for `listItemTersimpan`
            binding.listItemTersimpan.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.greendarkerbutton)
            )

            // Update the background color of `counterItemTersimpan`
            binding.counterItemTersimpan.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.rounded_box_small_border // Ensure you update this drawable
            )
            val counterBackground = binding.counterItemTersimpan.background as GradientDrawable
            counterBackground.setColor(
                ContextCompat.getColor(requireContext(), R.color.greendarkerbutton)
            )
        }


        binding.layoutItemTerupload.apply {

            val layerDrawable = binding.layoutItemTerupload.background as LayerDrawable
            val strokeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
            strokeDrawable.setStroke(
                4, // Stroke width in dp
                ContextCompat.getColor(requireContext(), R.color.white) // New stroke color
            )

            binding.listItemTerupload.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.black)
            )

            binding.counterItemTerupload.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.rounded_box_small_border // Ensure you update this drawable
            )
            val counterBackground = binding.counterItemTerupload.background as GradientDrawable
            counterBackground.setColor(
                ContextCompat.getColor(requireContext(), R.color.black)
            )
        }
//        setAppVersion()
//        initViewModel()
//        setupRecyclerView()
//        setupMenuListeners()
//        adjustBottomMargin()
//        setupSpeedDial()
//        loadingDialog = LoadingDialog(requireContext())
//        setupCheckboxControl()
//        observeData()



        return root
    }


//    private fun setupCheckboxControl() {
//        binding.tableHeader.headerCheckBoxPanen.apply {
//            visibility = View.VISIBLE
//            setOnCheckedChangeListener(null) // Remove any existing listeners
//            setOnCheckedChangeListener { _, isChecked ->
//                if (!isSettingUpCheckbox) {
//                    tphAdapter.selectAll(isChecked)
//                }
//            }
//        }
//
//        tphAdapter.setOnSelectionChangedListener { selectedCount ->
//            isSettingUpCheckbox = true
//            binding.tableHeader.headerCheckBoxPanen.isChecked = tphAdapter.isAllSelected()
//            binding.dialTphList.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
//            isSettingUpCheckbox = false
//        }
//    }
//
//    private fun setAppVersion() {
//        val versionTextView: TextView = binding.versionApp
//        val appVersion = AppUtils.getAppVersion(requireContext()) // Use AppUtils here
//        versionTextView.text = "$appVersion"
//    }
//
//    private fun handleUpload(selectedItems: List<Map<String, Any>>) {
//        AlertDialogUtility.withTwoActions(
//            requireContext(),
//            stringXML(R.string.al_submit),
//            stringXML(R.string.confirmation_dialog_title),
//            "${stringXML(R.string.al_make_sure_upload)} ${selectedItems.size} data?",
//            "warning.json"
//        ) {
//
//            loadingDialog.show()
//
////            val uploadDataList = selectedItems.map {
////                UploadData(
////                    id = it["id"] as Int,
////                    datetime = it["tanggal"] as String,
////                    estate = it["estate"] as String,
////                    user_input = it["user_input"] as String,
////                    afdeling = it["afdeling"] as String,
////                    blok = it["blok"] as String,
////                    ancak = it["ancak"] as String,
////                    tph = it["tph"] as String,
////                    lat = it["latitude"] as String,
////                    lon = it["longitude"] as String,
////                    app_version = it["app_version"] as String
////                )
////            }
////
////
////            tphViewModel.uploadData(requireContext(), uploadDataList).observe(requireActivity()) { result ->
////                when {
////                    result.isSuccess -> {
////                        val responses = result.getOrNull()
////
////                        tphViewModel.loadDataAllTPH(currentArchiveState)
////                        tphViewModel.countDataArchive()
////                        tphViewModel.countDataNonArchive()
////
////                        loadingDialog.dismiss()
////                    }
////                    result.isFailure -> {
////                        // Handle failure
////                        val error = result.exceptionOrNull()
////                        Log.e("testing", "Failed to upload: ${error?.message}")
////
////                        // Show error message
////                        Toast.makeText(context, "${stringXML(R.string.al_failed_upload)} :${error?.message}", Toast.LENGTH_SHORT).show()
////
////                        loadingDialog.dismiss()
////                    }
////                }
////            }
//            tphAdapter.clearSelections()
//
//        }
//    }
//
//    private fun handleDelete(selectedItems: List<Map<String, Any>>) {
//        requireContext().vibrate()
//        AlertDialogUtility.withTwoActions(
//            requireContext(),
//            stringXML(R.string.al_delete),
//            stringXML(R.string.confirmation_dialog_title),
//            "${stringXML(R.string.al_make_sure_delete)} ${selectedItems.size} data?",
//            "warning.json",
//            ContextCompat.getColor(requireContext(), R.color.colorRedDark)
//        ) {
//
//            tphViewModel.deleteMultipleItems(selectedItems)
//            tphViewModel.deleteItemsResult.observe(viewLifecycleOwner) { isSuccess ->
//                if (isSuccess) {
//                    Toast.makeText(context, "${stringXML(R.string.al_success_delete)} ${selectedItems.size} data", Toast.LENGTH_SHORT).show()
//                    observeData()
//                } else {
//                    Toast.makeText(context, "${stringXML(R.string.al_failed_delete)} data", Toast.LENGTH_SHORT).show()
//                }
//                binding.tableHeader.headerCheckBoxPanen.isChecked = false
//                tphAdapter.clearSelections()
//            }
//
//        }
//    }
//
//    private fun setupSpeedDial() {
//
//
//        binding.dialTphList.apply {
//
//            addActionItem(
//                SpeedDialActionItem.Builder(R.id.cancelSelection, R.drawable.baseline_check_box_outline_blank_24)  // Use appropriate icon
//                    .setLabel(stringXML(R.string.dial_unselect_item))
//                    .setFabBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellowbutton))  // Use appropriate color
//                    .create()
//            )
//
//            addActionItem(
//                SpeedDialActionItem.Builder(R.id.uploadSelected, R.drawable.baseline_cloud_upload_24)
//                    .setLabel(stringXML(R.string.dial_upload_item))
//                    .setFabBackgroundColor(ContextCompat.getColor(requireContext(), R.color.greendarkerbutton))
//                    .create()
//            )
//
//            addActionItem(
//                SpeedDialActionItem.Builder(R.id.deleteSelected, R.drawable.baseline_delete_24)
//                    .setLabel(stringXML(R.string.dial_delete_item))
//                    .setFabBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorRedDark))
//                    .create()
//            )
//
//            visibility = View.GONE
//
//            setOnActionSelectedListener { actionItem ->
//                when (actionItem.id) {
//
//                    R.id.cancelSelection -> {
//                        tphAdapter.clearSelections()
//                        true
//                    }
//                    R.id.deleteSelected -> {
//                        val selectedItems = tphAdapter.getSelectedItems()
//                        handleDelete(selectedItems)
//                        true
//                    }
//                    R.id.uploadSelected -> {
//                        val selectedItems = tphAdapter.getSelectedItems()
//
//                        if (AppUtils.isInternetAvailable(requireContext())) {
//                            handleUpload(selectedItems)
//                        } else {
//                            AlertDialogUtility.withSingleAction(
//                                requireContext(),
//                                stringXML(R.string.al_back),
//                                stringXML(R.string.al_no_internet_connection),
//                                stringXML(R.string.al_no_internet_connection_description),
//                                "network_error.json",
//                                R.color.colorRedDark
//                            ) {
//                                // Optional: Scroll to first error or highlight fields
//                            }
//                        }
//
//                        true
//                    }
//                    else -> false
//                }
//            }
//        }
//
//        // Setup selection listener
//        tphAdapter.setOnSelectionChangedListener { selectedCount ->
//            binding.dialTphList.visibility = if (selectedCount > 0 && currentArchiveState == 0)
//                View.VISIBLE
//            else
//                View.GONE
//        }
//
//
//
//
//    }
//
//    private fun updateMenuState() {
//        // Update the "Tersimpan" state
//        binding.layoutItemTersimpan.apply {
//            val isActive = currentArchiveState == 0
//
//            // If switching from Terupload to Tersimpan, uncheck all items
//            if (isActive) {
//                tphAdapter.clearSelections() // Add this method to your adapter
//            }
//            binding.listItemTersimpan.setTextColor(
//                ContextCompat.getColor(
//                    requireContext(),
//                    if (isActive) R.color.greendarkerbutton else R.color.black
//                )
//            )
//
//            val layerDrawable = background as LayerDrawable
//            val strokeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
//            strokeDrawable.setStroke(
//                4, // Stroke width in dp
//                ContextCompat.getColor(
//                    requireContext(),
//                    if (isActive) R.color.greendarkerbutton else R.color.white
//                )
//            )
//
//            binding.counterItemTersimpan.background = ContextCompat.getDrawable(
//                requireContext(),
//                R.drawable.rounded_box_small_border
//            )
//            val counterBackground = binding.counterItemTersimpan.background as GradientDrawable
//            counterBackground.setColor(
//                ContextCompat.getColor(
//                    requireContext(),
//                    if (isActive) R.color.greendarkerbutton else R.color.black
//                )
//            )
//        }
//
//        binding.layoutItemTerupload.apply {
//            val isActive = currentArchiveState == 1
//
//            // Update text color for `listItemTerupload`
//            binding.listItemTerupload.setTextColor(
//                ContextCompat.getColor(
//                    requireContext(),
//                    if (isActive) R.color.greendarkerbutton else R.color.black
//                )
//            )
//
//            // Update stroke color for the background
//            val layerDrawable = background as LayerDrawable
//            val strokeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
//            strokeDrawable.setStroke(
//                4, // Stroke width in dp
//                ContextCompat.getColor(
//                    requireContext(),
//                    if (isActive) R.color.greendarkerbutton else R.color.white
//                )
//            )
//
//            // Update the background color for `counterItemTerupload`
//            binding.counterItemTerupload.background = ContextCompat.getDrawable(
//                requireContext(),
//                R.drawable.rounded_box_small_border
//            )
//            val counterBackground = binding.counterItemTerupload.background as GradientDrawable
//            counterBackground.setColor(
//                ContextCompat.getColor(
//                    requireContext(),
//                    if (isActive) R.color.greendarkerbutton else R.color.black
//                )
//            )
//        }
//
//
//        binding.dialTphList.visibility = if (currentArchiveState == 1) View.GONE else binding.dialTphList.visibility
//
//    }
//
//
//
//    private fun adjustBottomMargin() {
//        // Get reference to the bottom navigation
//        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
//
//        // Wait for the bottom navigation to be laid out
//        bottomNav.post {
//            val bottomNavHeight = bottomNav.height
//
//            // Get the content layout
//            val contentLayout = binding.contentLayout
//
//            // Create margin
//            val params = contentLayout.layoutParams as ConstraintLayout.LayoutParams
//            params.bottomMargin = bottomNavHeight
//            contentLayout.layoutParams = params
//        }
//    }
//
//    private fun setupRecyclerView() {
//        tphAdapter = TPHAdapter()
//        binding.rvTableData.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = tphAdapter
//            setHasFixedSize(true)
//            itemAnimator = null // Disable animations to prevent glitches
//        }
//    }
//
//    private fun setupMenuListeners() {
//        binding.listItemTersimpan.setOnClickListener {
//            if (currentArchiveState != 0) {
//                currentArchiveState = 0
//                updateMenuState()
//
//
//                binding.tableHeader.headerCheckBoxPanen.visibility = View.VISIBLE
//                binding.tableHeader.headerNoList.visibility = View.GONE
//
//                tphAdapter.updateArchiveState(0)
//
//                dataTPHList.clear()
//                tphAdapter.updateData(dataTPHList)
//
//                observeData()
//            }
//        }
//
//        binding.listItemTerupload.setOnClickListener {
//            if (currentArchiveState != 1) {
//                currentArchiveState = 1
//                updateMenuState()
//                binding.tableHeader.headerNoList.visibility = View.VISIBLE
//                binding.tableHeader.headerCheckBoxPanen.visibility = View.GONE
//                tphAdapter.updateArchiveState(1)
//                dataTPHList.clear()
//                tphAdapter.updateData(dataTPHList)
//
//                observeData()
//            }
//        }
//    }
//
//
//    private fun observeData() {
//        loadingDialog.show()
//
//        tphViewModel.loadDataAllTPH(currentArchiveState) // State 0
//        tphViewModel.dataTPHAll.observe(viewLifecycleOwner) { data ->
//            if (data != null) {
//                dataTPHList.clear()
//
//                data.forEach { record ->
//                    record?.let { safeRecord ->  // Only process non-null records
//                        val recordMap = mutableMapOf<String, Any>()
//                        recordMap[KEY_ID] = safeRecord.id ?: 0
//                        recordMap[KEY_TANGGAL] = safeRecord.tanggal ?: ""
//                        recordMap[KEY_ESTATE] = safeRecord.estate ?: ""
//                        recordMap[KEY_USER_INPUT] = safeRecord.user_input ?: ""
//                        recordMap[KEY_ESTATE_ID] = safeRecord.id_estate ?: 0
//                        recordMap[KEY_AFDELING] = safeRecord.afdeling ?: ""
//                        recordMap[KEY_AFDELING_ID] = safeRecord.id_afdeling ?: 0
//                        recordMap[KEY_BLOK] = safeRecord.blok ?: ""
//                        recordMap[KEY_BLOK_ID] = safeRecord.id_blok ?: 0
//                        recordMap[KEY_ANCAK] = safeRecord.ancak ?: ""
//                        recordMap[KEY_ANCAK_ID] = safeRecord.id_ancak ?: 0
//                        recordMap[KEY_TPH] = safeRecord.tph ?: ""
//                        recordMap[KEY_TPH_ID] = safeRecord.id_tph ?: 0
//                        recordMap[KEY_LAT] = safeRecord.latitude ?: ""
//                        recordMap[KEY_LON] = safeRecord.longitude ?: ""
//                        recordMap[KEY_APP_VERSION] = safeRecord.app_version ?: ""
//                        dataTPHList.add(recordMap)
//                    }
//                }
//
//                // Update RecyclerView with new data
//                tphAdapter.updateData(dataTPHList)
//
//                tphViewModel.countDataArchive()
//                tphViewModel.countDataNonArchive()
//                tphViewModel.resultCountDataArchive.observe(viewLifecycleOwner) { count ->
//                    binding.counterItemTerupload.text = count.toString()
//                }
//
//                tphViewModel.resultCountDataNonArchive.observe(viewLifecycleOwner) { count ->
//                    binding.counterItemTersimpan.text = count.toString()
//                }
//
//
//                Handler(Looper.getMainLooper()).postDelayed({
//                    loadingDialog.dismiss()
//                }, 1000) // 1000ms = 1 second
//            } else {
//                loadingDialog.dismiss()
//                // Optionally show error message
//                Handler(Looper.getMainLooper()).postDelayed({
//                    loadingDialog.dismiss()
//                    Toast.makeText(context, stringXML(R.string.toast_failed_load_data), Toast.LENGTH_SHORT).show()
//                }, 1000)
//            }
//        }
//    }
//
//    private fun     initViewModel() {
//        tphViewModel = ViewModelProvider(
//            this,
//            TPHViewModel.Factory(requireActivity().application, TPHRepository(requireActivity()))
//        )[TPHViewModel::class.java]
//
//    }
//
//    override fun onDestroyView() {
//        tphAdapter.clearAll() // We'll create this method
//        binding.tableHeader.headerCheckBoxPanen.setOnCheckedChangeListener(null)
//        super.onDestroyView()
//        _binding = null
//    }
}