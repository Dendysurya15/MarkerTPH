package com.cbi.markertph.ui.adapter

import android.view.LayoutInflater
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
    private val selectedItems = mutableSetOf<Int>()  // Track selected positions
    class TPHViewHolder(private val binding: TableItemRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Map<String, Any>, isSelected: Boolean, onCheckedChange: (Boolean) -> Unit) {
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

            binding.checkBoxPanen.isChecked = isSelected
            binding.checkBoxPanen.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(isChecked)
            }
        }
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

    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: TPHViewHolder, position: Int) {
        holder.bind(tphList[position], selectedItems.contains(position)) { isChecked ->
            if (isChecked) {
                selectedItems.add(position)
            } else {
                selectedItems.remove(position)
            }
            notifySelectedItemsChanged()
        }
    }
    override fun getItemCount() = tphList.size

    fun updateData(newData: List<Map<String, Any>>) {
        tphList.clear()
        tphList.addAll(newData)
        notifyDataSetChanged()
    }

    private var onSelectionChangedListener: ((Int) -> Unit)? = null

    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChangedListener = listener
    }

    private fun notifySelectedItemsChanged() {
        onSelectionChangedListener?.invoke(selectedItems.size)
    }
}