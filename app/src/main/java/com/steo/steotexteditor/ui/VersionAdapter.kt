package com.steo.steotexteditor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.steo.steotexteditor.R
import com.steo.steotexteditor.data.db.VersionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VersionAdapter(
    initial: List<VersionEntity>,
    private val onClick: (VersionEntity) -> Unit,
    private val onLongClick: (VersionEntity) -> Unit
) : ListAdapter<VersionEntity, VersionAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VersionEntity>() {
            override fun areItemsTheSame(oldItem: VersionEntity, newItem: VersionEntity): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: VersionEntity, newItem: VersionEntity): Boolean = oldItem == newItem
        }
    }

    init { submitList(initial) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_version, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val version = getItem(position)
        holder.bind(version)
        holder.itemView.setOnClickListener { onClick(version) }
        holder.itemView.setOnLongClickListener { onLongClick(version); true }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvBadge: TextView = v.findViewById(R.id.tvBadge)
        private val tvLabel: TextView = v.findViewById(R.id.tvLabel)
        private val tvTimestamp: TextView = v.findViewById(R.id.tvTimestamp)

        fun bind(version: VersionEntity) {
            tvBadge.text = "v${version.versionNumber}"
            tvLabel.text = if (version.label.isBlank()) "Version ${version.versionNumber}" else version.label
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            tvTimestamp.text = sdf.format(Date(version.createdAt))
        }
    }
}