package com.cbi.markertph

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
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
import androidx.lifecycle.lifecycleScope
import com.cbi.markertph.ui.theme.MarkerTPHTheme
import com.cbi.markertph.ui.view.HomeActivity
import com.cbi.markertph.ui.view.HomePage
import com.cbi.markertph.utils.AppUtils

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var showingSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash_screen)
        setAppVersion()
        lifecycleScope.launch {
            delay(1500) // Wait for 1.5 seconds
            showMainContent()
        }
    }

    private fun setAppVersion() {
        val versionTextView: TextView = findViewById(R.id.version_app)
        val appVersion = AppUtils.getAppVersion(this) // Use AppUtils here
        versionTextView.text = "$appVersion"
    }

    private fun showMainContent() {
        if (!showingSplash) return
        showingSplash = false

        startActivity(Intent(this, HomePage::class.java))
        finish()

    }
}
