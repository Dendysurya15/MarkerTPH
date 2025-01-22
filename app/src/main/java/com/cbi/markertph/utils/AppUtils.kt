package com.cbi.markertph.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.PermissionRequest
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.cbi.markertph.R
import com.cbi.markertph.data.network.RetrofitClient
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.text.SimpleDateFormat
import java.util.Locale

object AppUtils {

    const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
        UPDATE_INTERVAL_IN_MILLISECONDS / 2
    const val LOG_LOC = "locationLog"

    const val REQUEST_CHECK_SETTINGS = 0x1


    object ApiCallManager {
//        noted : field ditengah merupakan nama db yg ada di server, harus sama !
        val apiCallList = listOf(
            Pair("datasetRegional.zip", RetrofitClient.instance::downloadDatasetRegionalJson),
            Pair("datasetWilayah.zip", RetrofitClient.instance::downloadDatasetWilayahJson),
            Pair("datasetDept.zip", RetrofitClient.instance::downloadDatasetDeptJson),
            Pair("datasetDivisi.zip", RetrofitClient.instance::downloadDatasetDivisiJson),
            Pair("datasetBlok.zip", RetrofitClient.instance::downloadDatasetBlokJson),
            Pair("datasetTPH.zip", RetrofitClient.instance::downloadDatasetTPHNewJson),
        )
    }
    /**
     * Gets the current app version from BuildConfig or string resources.
     * @param context The context used to retrieve the string resource.
     * @return The app version as a string.
     */


    fun getAppVersion(context: Context): String {
        return "${context.stringXML(R.string.version_word)} ${context.getString(R.string.app_version)}"
    }

    // Function to format the date
    fun formatDateToIndonesian(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) // Indonesian format

            val date = inputFormat.parse(dateString) // Parse input date
            outputFormat.format(date!!)             // Format to Indonesian date
        } catch (e: Exception) {
            e.printStackTrace()
            "Invalid Date" // Fallback in case of error
        }
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun Context.vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    fun Fragment.stringXML(field: Int): String {
        return requireContext().getString(field)
    }

    fun Context.stringXML(field: Int): String {
        return getString(field)
    }
}