package com.trifork.androidble

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

data class ScanResultItem(
    val name: String?,
    val address: String,
    val serviceUuids: List<ParcelUuid>?
)

class ScanResultAdapter : RecyclerView.Adapter<ViewHolder>() {
    private var items = mutableListOf<ScanResultItem>()


    fun setItems(items: MutableList<ScanResultItem>) {
        if (this.items != items) {
            this.items = items
            Log.d("ScanResultAdapter", "notifyDataSetChanged")
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return VH(
            LayoutInflater.from(
                parent.context
            ).inflate(R.layout.scanresult_view, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val vh = holder as VH
        vh.nameText.text = item.name ?: "null"
        vh.addressText.text = item.address
        vh.servicesText.text = if (item.serviceUuids != null) "${item.serviceUuids}" else "null"
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class VH internal constructor(itemView: View) : ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.name)
        val addressText: TextView = itemView.findViewById(R.id.address)
        val servicesText: TextView = itemView.findViewById(R.id.services)
    }
}