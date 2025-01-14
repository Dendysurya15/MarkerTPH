package com.cbi.markertph.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cbi.markertph.R
import com.cbi.markertph.data.network.RetrofitClient

object AppUtils {

    const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
    const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
        UPDATE_INTERVAL_IN_MILLISECONDS / 2
    const val LOG_LOC = "locationLog"

    const val REQUEST_CHECK_SETTINGS = 0x1

    object ApiCallManager {
        val apiCallList = listOf(
            Pair("datasetCompanyCode.zip", RetrofitClient.instance::downloadDatasetCompany),
            Pair("datasetBUnitCode.zip", RetrofitClient.instance::downloadDatasetBUnit),
            Pair("datasetDivisionCode.zip", RetrofitClient.instance::downloadDatasetDivision),
            Pair("datasetFieldCode.zip", RetrofitClient.instance::downloadDatasetField),
            Pair("datasetTPHCode.zip", RetrofitClient.instance::downloadDatasetTPH),
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