package com.cbi.markertph

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cbi.markertph.data.network.RetrofitClient
import com.cbi.markertph.data.repository.TPHRepository
import com.cbi.markertph.ui.theme.MarkerTPHTheme
import com.cbi.markertph.ui.view.HomeActivity
import com.cbi.markertph.ui.view.HomePage
import com.cbi.markertph.ui.view.UploadDataActivity
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.AppUtils
import com.cbi.markertph.utils.AppUtils.stringXML
import com.cbi.markertph.utils.LoadingDialog
import com.google.gson.Gson
import com.jaredrummler.materialspinner.BuildConfig
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var showingSplash = true
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var tphViewModel: TPHViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash_screen)
        initViewModel()
        setAppVersion()
        
        lifecycleScope.launch {
            delay(1500)
            checkDataAndFetch()
        }
    }

    private fun checkDataAndFetch() {
        lifecycleScope.launch {
            try {
                // Check all tables in background
                val needsFetch = withContext(Dispatchers.IO) {
                    val hasCompanyCode = tphViewModel.getCompanyCodeCount() > 0
                    val hasBUnitCode = tphViewModel.getBUnitCodeCount() > 0
                    val hasDivisionCode = tphViewModel.getDivisionCodeCount() > 0
                    val hasFieldCode = tphViewModel.getFieldCodeCount() > 0
                    val hasTPH = tphViewModel.getTPHCount() > 0

//                    Log.d("DataCheck", """
//                    CompanyCode: $hasCompanyCode
//                    BUnitCode: $hasBUnitCode
//                    DivisionCode: $hasDivisionCode
//                    FieldCode: $hasFieldCode
//                    TPH: $hasTPH
//                """.trimIndent())

                    !hasCompanyCode || !hasBUnitCode || !hasDivisionCode ||
                            !hasFieldCode || !hasTPH
                }

                if (needsFetch) {
                    Log.d("DataCheck", "Some tables are empty, fetching data...")
                    showMainContent()
                } else {
                    Log.d("DataCheck", "All tables have data, proceeding to HomePage")
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Log.e("DataCheck", "Error checking data", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error checking database: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setAppVersion() {
        val versionTextView: TextView = findViewById(R.id.version_app)
        val appVersion = AppUtils.getAppVersion(this) // Use AppUtils here
        versionTextView.text = appVersion
    }

    private fun initViewModel() {
        tphViewModel = ViewModelProvider(
            this,
            TPHViewModel.Factory(application, TPHRepository(this))
        )[TPHViewModel::class.java]
    }

    private fun logErrorToFile(errorMessage: String) {
        try {
            val filename = "error_log_${System.currentTimeMillis()}.txt"
            val file = File(getExternalFilesDir(null), filename)

            file.writeText("""
            Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Android Version: ${Build.VERSION.RELEASE}
            App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            
            Error Details:
            $errorMessage
        """.trimIndent())

            Log.e("MainActivity", "Error logged to file: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to write error log: ${e.message}")
        }
    }

    private fun showMainContent() {
        if (!showingSplash) return
        showingSplash = false

        loadingDialog = LoadingDialog(this)
        loadingDialog.show()

        // Create a job for progress updates
        val progressJob = lifecycleScope.launch(Dispatchers.Main) {
            var dots = 1
            while (true) {
                loadingDialog.setMessage("${stringXML(R.string.fetching_data)}${".".repeat(dots)}")
                dots = if (dots >= 3) 1 else dots + 1
                delay(500) // Update every 500ms
            }
        }

        lifecycleScope.launch {
            try {
                // Fetch data
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.fetchRawData().execute()
                }

                if (response.isSuccessful) {
                    val fetchResponse = response.body()
                    if (fetchResponse?.statusCode == 1) {
                        val allData = fetchResponse.data

                        // Process all data in background
                        withContext(Dispatchers.Default) {
                            // Process in sequence but in background
                            allData?.companyCode?.forEach { companyCodeModel ->
                                tphViewModel.insertCompanyCode(companyCodeModel)
                            }

                            allData?.bUnitCode?.forEach { bUnitCodeModel ->
                                tphViewModel.insertBUnitCode(bUnitCodeModel)
                            }

                            allData?.divisionCode?.forEach { divisionCodeModel ->
                                tphViewModel.insertDivisionCode(divisionCodeModel)
                            }

                            allData?.fieldCode?.forEach { fieldCodeModel ->
                                tphViewModel.insertFieldCode(fieldCodeModel)
                            }

                            allData?.tph?.let { tphList ->
                                Log.d("MainActivity", "Processing ${tphList.size} TPH records")
                                tphViewModel.insertTPHBatch(tphList)
                            }
                        }

                        // Cancel progress updates
                        progressJob.cancel()

                        // Navigate after all processing is done
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                            finish()
                        }
                    } else {
                        progressJob.cancel()
                        withContext(Dispatchers.Main) {
                            loadingDialog.dismiss()
                            Toast.makeText(
                                this@MainActivity,
                                fetchResponse?.message ?: "Failed to fetch data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    progressJob.cancel()
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            this@MainActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {

                logErrorToFile("Exception Details:\n" +
                        "Message: ${e.message}\n" +
                        "Stack Trace:\n${e.stackTraceToString()}")
                progressJob.cancel()

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@MainActivity,
                        "Exception: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
