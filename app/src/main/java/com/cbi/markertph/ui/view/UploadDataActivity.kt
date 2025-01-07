package com.cbi.markertph.ui.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.cbi.markertph.databinding.ActivityUploadDataBinding
import com.cbi.markertph.ui.adapter.TPHAdapter
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.AlertDialogUtility
import com.cbi.markertph.utils.AppUtils
import com.cbi.markertph.utils.AppUtils.vibrate
import com.cbi.markertph.utils.LoadingDialog
import com.leinardi.android.speeddial.SpeedDialActionItem

class UploadDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadDataBinding
    private lateinit var tphViewModel: TPHViewModel
    private lateinit var loadingDialog: LoadingDialog
    private val dataTPHList = mutableListOf<Map<String, Any>>()
    private lateinit var tphAdapter: TPHAdapter

    private var currentArchiveState = 0 // 0 for Tersimpan, 1 for Terupload
    private var isSettingUpCheckbox = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tableHeader.headerCheckBoxPanen.visibility = View.VISIBLE
        setupInitialLayout()
        setAppVersion()
        initViewModel()
        setupRecyclerView()
        setupMenuListeners()
        setupSpeedDial()
        loadingDialog = LoadingDialog(this)
        setupCheckboxControl()
        observeData()
    }

    private fun setupInitialLayout() {

        binding.btnBack.setOnClickListener{

            startActivity(Intent(this@UploadDataActivity, HomeActivity::class.java))
            finish()
        }
        binding.menuBack.setOnClickListener{

            startActivity(Intent(this@UploadDataActivity, HomeActivity::class.java))
            finish()
        }
        binding.layoutItemTersimpan.apply {
            val layerDrawable = background as LayerDrawable
            val strokeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
            strokeDrawable.setStroke(
                4,
                ContextCompat.getColor(this@UploadDataActivity, R.color.greendarkerbutton)
            )

            binding.listItemTersimpan.setTextColor(
                ContextCompat.getColor(this@UploadDataActivity, R.color.greendarkerbutton)
            )

            binding.counterItemTersimpan.background = ContextCompat.getDrawable(
                this@UploadDataActivity,
                R.drawable.rounded_box_small_border
            )
            val counterBackground = binding.counterItemTersimpan.background as GradientDrawable
            counterBackground.setColor(
                ContextCompat.getColor(this@UploadDataActivity, R.color.greendarkerbutton)
            )
        }

        binding.layoutItemTerupload.apply {
            val layerDrawable = background as LayerDrawable
            val strokeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
            strokeDrawable.setStroke(
                4,
                ContextCompat.getColor(this@UploadDataActivity, R.color.white)
            )

            binding.listItemTerupload.setTextColor(
                ContextCompat.getColor(this@UploadDataActivity, R.color.black)
            )

            binding.counterItemTerupload.background = ContextCompat.getDrawable(
                this@UploadDataActivity,
                R.drawable.rounded_box_small_border
            )
            val counterBackground = binding.counterItemTerupload.background as GradientDrawable
            counterBackground.setColor(
                ContextCompat.getColor(this@UploadDataActivity, R.color.black)
            )
        }
    }

    private fun setupCheckboxControl() {
        binding.tableHeader.headerCheckBoxPanen.apply {
            visibility = View.VISIBLE
            setOnCheckedChangeListener(null)
            setOnCheckedChangeListener { _, isChecked ->
                if (!isSettingUpCheckbox) {
                    tphAdapter.selectAll(isChecked)
                }
            }
        }

        tphAdapter.setOnSelectionChangedListener { selectedCount ->
            isSettingUpCheckbox = true
            binding.tableHeader.headerCheckBoxPanen.isChecked = tphAdapter.isAllSelected()
            binding.dialTphList.visibility = if (selectedCount > 0) View.VISIBLE else View.GONE
            isSettingUpCheckbox = false
        }
    }

    private fun setAppVersion() {
        val versionTextView: TextView = binding.versionApp
        val appVersion = AppUtils.getAppVersion(this)
        versionTextView.text = appVersion
    }

    private fun handleUpload(selectedItems: List<Map<String, Any>>) {
        AlertDialogUtility.withTwoActions(
            this,
            getString(R.string.al_submit),
            getString(R.string.confirmation_dialog_title),
            "${getString(R.string.al_make_sure_upload)} ${selectedItems.size} data?",
            "warning.json"
        ) {
            loadingDialog.show()

            val uploadDataList = selectedItems.map {
                UploadData(
                    id = it["id"] as Int,
                    datetime = it["tanggal"] as String,
                    estate = it["estate"] as String,
                    user_input = it["user_input"] as String,
                    afdeling = it["afdeling"] as String,
                    blok = it["blok"] as String,
                    ancak = it["ancak"] as String,
                    tph = it["tph"] as String,
                    lat = it["latitude"] as String,
                    lon = it["longitude"] as String,
                    app_version = it["app_version"] as String
                )
            }

            tphViewModel.uploadData(this, uploadDataList).observe(this) { result ->
                when {
                    result.isSuccess -> {
                        tphViewModel.loadDataAllTPH(currentArchiveState)
                        tphViewModel.countDataArchive()
                        tphViewModel.countDataNonArchive()
                        loadingDialog.dismiss()
                    }
                    result.isFailure -> {
                        val error = result.exceptionOrNull()
                        Log.e("testing", "Failed to upload: ${error?.message}")
                        Toast.makeText(this, "${getString(R.string.al_failed_upload)} :${error?.message}", Toast.LENGTH_SHORT).show()
                        loadingDialog.dismiss()
                    }
                }
            }
            tphAdapter.clearSelections()
        }
    }

    private fun handleDelete(selectedItems: List<Map<String, Any>>) {
        this.vibrate()
        AlertDialogUtility.withTwoActions(
            this,
            getString(R.string.al_delete),
            getString(R.string.confirmation_dialog_title),
            "${getString(R.string.al_make_sure_delete)} ${selectedItems.size} data?",
            "warning.json",
            ContextCompat.getColor(this, R.color.colorRedDark)
        ) {
            tphViewModel.deleteMultipleItems(selectedItems)
            tphViewModel.deleteItemsResult.observe(this) { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(this, "${getString(R.string.al_success_delete)} ${selectedItems.size} data", Toast.LENGTH_SHORT).show()
                    observeData()
                } else {
                    Toast.makeText(this, "${getString(R.string.al_failed_delete)} data", Toast.LENGTH_SHORT).show()
                }
                binding.tableHeader.headerCheckBoxPanen.isChecked = false
                tphAdapter.clearSelections()
            }
        }
    }

    private fun setupSpeedDial() {
        binding.dialTphList.apply {
            addActionItem(
                SpeedDialActionItem.Builder(R.id.cancelSelection, R.drawable.baseline_check_box_outline_blank_24)
                    .setLabel(getString(R.string.dial_unselect_item))
                    .setFabBackgroundColor(ContextCompat.getColor(this@UploadDataActivity, R.color.yellowbutton))
                    .create()
            )

            addActionItem(
                SpeedDialActionItem.Builder(R.id.uploadSelected, R.drawable.baseline_cloud_upload_24)
                    .setLabel(getString(R.string.dial_upload_item))
                    .setFabBackgroundColor(ContextCompat.getColor(this@UploadDataActivity, R.color.greendarkerbutton))
                    .create()
            )

            addActionItem(
                SpeedDialActionItem.Builder(R.id.deleteSelected, R.drawable.baseline_delete_24)
                    .setLabel(getString(R.string.dial_delete_item))
                    .setFabBackgroundColor(ContextCompat.getColor(this@UploadDataActivity, R.color.colorRedDark))
                    .create()
            )

            visibility = View.GONE

            setOnActionSelectedListener { actionItem ->
                when (actionItem.id) {
                    R.id.cancelSelection -> {
                        tphAdapter.clearSelections()
                        true
                    }
                    R.id.deleteSelected -> {
                        val selectedItems = tphAdapter.getSelectedItems()
                        handleDelete(selectedItems)
                        true
                    }
                    R.id.uploadSelected -> {
                        val selectedItems = tphAdapter.getSelectedItems()
                        if (AppUtils.isInternetAvailable(this@UploadDataActivity)) {
                            handleUpload(selectedItems)
                        } else {
                            AlertDialogUtility.withSingleAction(
                                this@UploadDataActivity,
                                getString(R.string.al_back),
                                getString(R.string.al_no_internet_connection),
                                getString(R.string.al_no_internet_connection_description),
                                "network_error.json",
                                R.color.colorRedDark
                            ) {}
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        tphAdapter.setOnSelectionChangedListener { selectedCount ->
            binding.dialTphList.visibility = if (selectedCount > 0 && currentArchiveState == 0)
                View.VISIBLE
            else
                View.GONE
        }
    }

    private fun updateMenuState() {
        binding.layoutItemTersimpan.apply {
            val isActive = currentArchiveState == 0
            if (isActive) {
                tphAdapter.clearSelections()
            }
            binding.listItemTersimpan.setTextColor(
                ContextCompat.getColor(
                    this@UploadDataActivity,
                    if (isActive) R.color.greendarkerbutton else R.color.black
                )
            )

            val layerDrawable = background as LayerDrawable
            val strokeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
            strokeDrawable.setStroke(
                4,
                ContextCompat.getColor(
                    this@UploadDataActivity,
                    if (isActive) R.color.greendarkerbutton else R.color.white
                )
            )

            binding.counterItemTersimpan.background = ContextCompat.getDrawable(
                this@UploadDataActivity,
                R.drawable.rounded_box_small_border
            )
            val counterBackground = binding.counterItemTersimpan.background as GradientDrawable
            counterBackground.setColor(
                ContextCompat.getColor(
                    this@UploadDataActivity,
                    if (isActive) R.color.greendarkerbutton else R.color.black
                )
            )
        }

        binding.layoutItemTerupload.apply {
            val isActive = currentArchiveState == 1
            binding.listItemTerupload.setTextColor(
                ContextCompat.getColor(
                    this@UploadDataActivity,
                    if (isActive) R.color.greendarkerbutton else R.color.black
                )
            )

            val layerDrawable = background as LayerDrawable
            val strokeDrawable = layerDrawable.getDrawable(1) as GradientDrawable
            strokeDrawable.setStroke(
                4,
                ContextCompat.getColor(
                    this@UploadDataActivity,
                    if (isActive) R.color.greendarkerbutton else R.color.white
                )
            )

            binding.counterItemTerupload.background = ContextCompat.getDrawable(
                this@UploadDataActivity,
                R.drawable.rounded_box_small_border
            )
            val counterBackground = binding.counterItemTerupload.background as GradientDrawable
            counterBackground.setColor(
                ContextCompat.getColor(
                    this@UploadDataActivity,
                    if (isActive) R.color.greendarkerbutton else R.color.black
                )
            )
        }

        binding.dialTphList.visibility = if (currentArchiveState == 1) View.GONE else binding.dialTphList.visibility
    }

    private fun setupRecyclerView() {
        tphAdapter = TPHAdapter()
        binding.rvTableData.apply {
            layoutManager = LinearLayoutManager(this@UploadDataActivity)
            adapter = tphAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun setupMenuListeners() {
        binding.listItemTersimpan.setOnClickListener {
            if (currentArchiveState != 0) {
                currentArchiveState = 0
                updateMenuState()
                binding.tableHeader.headerCheckBoxPanen.visibility = View.VISIBLE
                binding.tableHeader.headerNoList.visibility = View.GONE
                tphAdapter.updateArchiveState(0)
                dataTPHList.clear()
                tphAdapter.updateData(dataTPHList)
                observeData()
            }
        }

        binding.listItemTerupload.setOnClickListener {
            if (currentArchiveState != 1) {
                currentArchiveState = 1
                updateMenuState()
                binding.tableHeader.headerNoList.visibility = View.VISIBLE
                binding.tableHeader.headerCheckBoxPanen.visibility = View.GONE
                tphAdapter.updateArchiveState(1)
                dataTPHList.clear()
                tphAdapter.updateData(dataTPHList)
                observeData()
            }
        }
    }

    private fun observeData() {
        loadingDialog.show()
        tphViewModel.loadDataAllTPH(currentArchiveState)
        tphViewModel.dataTPHAll.observe(this) { data ->
            if (data != null) {
                dataTPHList.clear()

                data.forEach { record ->
                    record?.let { safeRecord ->
                        val recordMap = mutableMapOf<String, Any>()
                        recordMap[KEY_ID] = safeRecord.id ?: 0
                        recordMap[KEY_TANGGAL] = safeRecord.tanggal ?: ""
                        recordMap[KEY_ESTATE] = safeRecord.estate ?: ""
                        recordMap[KEY_USER_INPUT] = safeRecord.user_input ?: ""
                        recordMap[KEY_ESTATE_ID] = safeRecord.id_estate ?: 0
                        recordMap[KEY_AFDELING] = safeRecord.afdeling ?: ""
                        recordMap[KEY_AFDELING_ID] = safeRecord.id_afdeling ?: 0
                        recordMap[KEY_BLOK] = safeRecord.blok ?: ""
                        recordMap[KEY_BLOK_ID] = safeRecord.id_blok ?: 0
                        recordMap[KEY_ANCAK] = safeRecord.ancak ?: ""
                        recordMap[KEY_ANCAK_ID] = safeRecord.id_ancak ?: 0
                        recordMap[KEY_TPH] = safeRecord.tph ?: ""
                        recordMap[KEY_TPH_ID] = safeRecord.id_tph ?: 0
                        recordMap[KEY_LAT] = safeRecord.latitude ?: ""
                        recordMap[KEY_LON] = safeRecord.longitude ?: ""
                        recordMap[KEY_APP_VERSION] = safeRecord.app_version ?: ""
                        dataTPHList.add(recordMap)
                    }
                }

                // Update RecyclerView with new data
                tphAdapter.updateData(dataTPHList)

                tphViewModel.countDataArchive()
                tphViewModel.countDataNonArchive()

                tphViewModel.resultCountDataArchive.observe(this) { count ->
                    binding.counterItemTerupload.text = count.toString()
                }

                tphViewModel.resultCountDataNonArchive.observe(this) { count ->
                    binding.counterItemTersimpan.text = count.toString()
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                }, 1000)
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                    Toast.makeText(this, getString(R.string.toast_failed_load_data), Toast.LENGTH_SHORT).show()
                }, 1000)
            }
        }
    }

    private fun initViewModel() {
        tphViewModel = ViewModelProvider(
            this,
            TPHViewModel.Factory(application, TPHRepository(this))
        )[TPHViewModel::class.java]
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        AlertDialogUtility.withTwoActions(
            this,
            "Batal",
            "Keluar",
            "Apakah anda yakin untuk keluar dari aplikasi MobilePro?",
            "warning.json"
        ) {
            finish()
        }
    }

    override fun onDestroy() {
        tphAdapter.clearAll()
        binding.tableHeader.headerCheckBoxPanen.setOnCheckedChangeListener(null)
        super.onDestroy()
    }
}