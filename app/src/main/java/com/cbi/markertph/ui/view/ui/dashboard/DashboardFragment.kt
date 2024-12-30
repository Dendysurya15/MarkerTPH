package com.cbi.markertph.ui.view.ui.dashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING_ID
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
import com.cbi.markertph.data.repository.TPHRepository
import com.cbi.markertph.databinding.FragmentDashboardBinding
import com.cbi.markertph.ui.adapter.TPHAdapter
import com.cbi.markertph.ui.viewModel.LocationViewModel
import com.cbi.markertph.ui.viewModel.TPHViewModel
import com.cbi.markertph.utils.LoadingDialog

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private lateinit var tphViewModel: TPHViewModel
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private val dataTPHList = mutableListOf<Map<String, Any>>()
    private lateinit var tphAdapter: TPHAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()

        loadingDialog = LoadingDialog(requireContext())
        initViewModel()

        // Show the loading dialog
        loadingDialog.show()


        observeData()


        return root
    }

    private fun setupRecyclerView() {
        tphAdapter = TPHAdapter()
        binding.rvTableData.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tphAdapter
            // Optional: Add these optimizations if your item layouts don't change size
            setHasFixedSize(true)
        }
    }

    private fun observeData() {
        tphViewModel.loadDataAllTPH()
        tphViewModel.dataTPHAll.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                dataTPHList.clear()

                data.forEach { record ->
                    val recordMap = mutableMapOf<String, Any>()
                    recordMap[KEY_ID] = record.id
                    recordMap[KEY_TANGGAL] = record.tanggal
                    recordMap[KEY_ESTATE] = record.estate
                    recordMap[KEY_ESTATE_ID] = record.id_estate
                    recordMap[KEY_AFDELING] = record.afdeling
                    recordMap[KEY_AFDELING_ID] = record.id_afdeling
                    recordMap[KEY_BLOK] = record.blok
                    recordMap[KEY_BLOK_ID] = record.id_blok
                    recordMap[KEY_TPH] = record.tph
                    recordMap[KEY_TPH_ID] = record.id_tph
                    recordMap[KEY_LAT] = record.latitude
                    recordMap[KEY_LON] = record.longitude
                    dataTPHList.add(recordMap)
                }

                // Update RecyclerView with new data
                tphAdapter.updateData(dataTPHList)

                // Dismiss loading dialog after data is displayed
                loadingDialog.dismiss()
            } else {
                loadingDialog.dismiss()
                // Optionally show error message
                Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun     initViewModel() {
        tphViewModel = ViewModelProvider(
            this,
            TPHViewModel.Factory(requireActivity().application, TPHRepository(requireActivity()))
        )[TPHViewModel::class.java]

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}