package com.cbi.markertph.ui.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_AFDELING
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_BLOK
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_ESTATE
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_LAT
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_LON
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_TANGGAL
import com.cbi.markertph.data.database.DatabaseHelper.Companion.KEY_TPH
import com.cbi.markertph.databinding.TableItemRowBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TPHAdapter : RecyclerView.Adapter<TPHAdapter.TPHViewHolder>() {
    private var tphList = mutableListOf<Map<String, Any>>()
    private var currentArchiveState: Int = 0
    private val selectedItems = mutableSetOf<Int>()  // Track selected positions
    private var selectAllState = false

    private var onSelectionChangeListener: ((Int) -> Unit)? = null

    class TPHViewHolder(private val binding: TableItemRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Map<String, Any>, isSelected: Boolean,archiveState: Int, onCheckedChange: (Boolean) -> Unit) {


            // Format date to Indonesia style
            val dateStr = data[KEY_TANGGAL] as String
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = formatter.parse(dateStr)
            val indonesiaFormatter = SimpleDateFormat("d MMM yyyy HH:mm", Locale("id"))
            binding.tvItemTgl.text = indonesiaFormatter.format(date)

            // Concatenate estate, afdeling, block and TPH
            val estateInfo = "${data[KEY_ESTATE]} (${data[KEY_AFDELING]})\n${data[KEY_BLOK]} - ${data[KEY_TPH]}"
            binding.tvItemEstateAfdeling.text = estateInfo

            // Concatenate latitude and longitude
            val location = "${data[KEY_LAT]}\n${data[KEY_LON]}"
            binding.tvItemLatLon.text = location

            if (archiveState == 1) {
                binding.checkBoxPanen.visibility = View.GONE
                binding.numListTerupload.visibility = View.VISIBLE
                binding.numListTerupload.text = "${position + 1}."
            } else {
                binding.checkBoxPanen.visibility = View.VISIBLE
                binding.numListTerupload.visibility = View.GONE
                binding.checkBoxPanen.isChecked = isSelected
                binding.checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }
            }
        }
    }
    fun isAllSelected(): Boolean {
        return selectAllState
    }

    // Add methods to handle selections
    fun getSelectedItems(): List<Map<String, Any>> {
        return selectedItems.mapNotNull { position -> tphList.getOrNull(position) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TPHViewHolder {
        val binding = TableItemRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TPHViewHolder(binding)
    }


    fun selectAll(select: Boolean) {
        selectAllState = select
        selectedItems.clear()
        if (select) {
            for (i in tphList.indices) {
                if (currentArchiveState != 1) {
                    selectedItems.add(i)
                }
            }
        }
        Handler(Looper.getMainLooper()).post {
            notifyDataSetChanged()
            onSelectionChangeListener?.invoke(selectedItems.size)
        }
    }

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: TPHViewHolder, position: Int) {
        holder.bind(tphList[position], selectedItems.contains(position), currentArchiveState) { isChecked ->
            if (isChecked) {
                selectedItems.add(position)
            } else {
                selectedItems.remove(position)
                selectAllState = false
            }
//            notifySelectedItemsChanged()
            onSelectionChangeListener?.invoke(selectedItems.size)
        }
    }

    fun updateArchiveState(state: Int) {
        currentArchiveState = state
        notifyDataSetChanged()
    }

    override fun getItemCount() = tphList.size

//    fun updateData(newData: List<Map<String, Any>>) {
//        tphList.clear()
//        tphList.addAll(newData)
//        notifyDataSetChanged()
//    }

    private var onSelectionChangedListener: ((Int) -> Unit)? = null

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChangeListener = listener
    }

    private fun notifySelectedItemsChanged() {
        onSelectionChangedListener?.invoke(selectedItems.size)
    }

    fun clearAll() {
        selectedItems.clear()
        tphList.clear()
        selectAllState = false
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

    // Make sure updateData also resets selection state
    fun updateData(newData: List<Map<String, Any>>) {
        tphList.clear()
        selectedItems.clear()
        selectAllState = false
        tphList.addAll(newData)
        notifyDataSetChanged()
        onSelectionChangeListener?.invoke(0)
    }

}