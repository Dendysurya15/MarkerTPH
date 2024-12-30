package com.cbi.markertph.ui.view.ui.dashboard

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
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private lateinit var tphViewModel: TPHViewModel
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private val dataTPHList = mutableListOf<Map<String, Any>>()
    private lateinit var tphAdapter: TPHAdapter

    private var currentArchiveState = 0 // 0 for Tersimpan, 1 for Terupload

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.listItemTersimpan.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.greendarkerbutton))
            // Change stroke color
            ((background as LayerDrawable).getDrawable(1) as GradientDrawable).setStroke(
                4, // direct pixel value
                ContextCompat.getColor(requireContext(), R.color.greendarkerbutton)
            )
        }

        binding.listItemTerupload.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            // Change stroke color
            ((background as LayerDrawable).getDrawable(1) as GradientDrawable).setStroke(
                4, // direct pixel value
                ContextCompat.getColor(requireContext(), R.color.white)
            )
        }

        initViewModel()
        setupRecyclerView()
        setupMenuListeners()
        adjustBottomMargin()

        loadingDialog = LoadingDialog(requireContext())



        observeData()

        return root
    }

    private fun updateMenuState() {
        binding.listItemTersimpan.apply {
            setTextColor(ContextCompat.getColor(requireContext(),
                if (currentArchiveState == 0) R.color.greendarkerbutton else R.color.black))
            ((background as LayerDrawable).getDrawable(1) as GradientDrawable).setStroke(
                4,
                ContextCompat.getColor(requireContext(),
                    if (currentArchiveState == 0) R.color.greendarkerbutton else R.color.white)
            )
        }

        binding.listItemTerupload.apply {
            setTextColor(ContextCompat.getColor(requireContext(),
                if (currentArchiveState == 1) R.color.greendarkerbutton else R.color.black))
            ((background as LayerDrawable).getDrawable(1) as GradientDrawable).setStroke(
                4,
                ContextCompat.getColor(requireContext(),
                    if (currentArchiveState == 1) R.color.greendarkerbutton else R.color.white)
            )
        }
    }

    private fun adjustBottomMargin() {
        // Get reference to the bottom navigation
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)

        // Wait for the bottom navigation to be laid out
        bottomNav.post {
            val bottomNavHeight = bottomNav.height

            // Get the content layout
            val contentLayout = binding.contentLayout

            // Create margin
            val params = contentLayout.layoutParams as ConstraintLayout.LayoutParams
            params.bottomMargin = bottomNavHeight
            contentLayout.layoutParams = params
        }
    }

    private fun setupRecyclerView() {
        tphAdapter = TPHAdapter()
        binding.rvTableData.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tphAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupMenuListeners() {
        binding.listItemTersimpan.setOnClickListener {
            if (currentArchiveState != 0) {
                currentArchiveState = 0
                updateMenuState()


                // Clear existing data while loading
                dataTPHList.clear()
                tphAdapter.updateData(dataTPHList)

                observeData()
            }
        }

        binding.listItemTerupload.setOnClickListener {
            if (currentArchiveState != 1) {
                currentArchiveState = 1
                updateMenuState()

                // Clear existing data while loading
                dataTPHList.clear()
                tphAdapter.updateData(dataTPHList)

                observeData()
            }
        }
    }


    private fun observeData() {
        loadingDialog.show()
        tphViewModel.loadDataAllTPH(currentArchiveState)
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

                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                }, 1000) // 1000ms = 1 second
            } else {
                loadingDialog.dismiss()
                // Optionally show error message
                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                    Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
                }, 1000)
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