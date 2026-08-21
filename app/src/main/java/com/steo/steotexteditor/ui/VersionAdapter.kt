package com.steo.steotexteditor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
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
        val latestVersionNumber = currentList.maxOfOrNull { it.versionNumber }
        holder.bind(version, latestVersionNumber == version.versionNumber)
        holder.itemView.setOnClickListener { onClick(version) }
        holder.itemView.setOnLongClickListener { onLongClick(version); true }
        holder.btnDiff.setOnClickListener { onClick(version) }
        holder.btnRestore.setOnClickListener { onLongClick(version) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val btnDiff: TextView = v.findViewById(R.id.btnDiff)
        val btnRestore: TextView = v.findViewById(R.id.btnRestore)
        private val latestAccent: View = v.findViewById(R.id.latestAccent)
        private val tvBadge: TextView = v.findViewById(R.id.tvBadge)
        private val tvLabel: TextView = v.findViewById(R.id.tvLabel)
        private val tvTimestamp: TextView = v.findViewById(R.id.tvTimestamp)
        private val tvLatest: TextView = v.findViewById(R.id.tvLatest)
        private val diffStatsRow: View = v.findViewById(R.id.diffStatsRow)
        private val tvAdded: TextView = v.findViewById(R.id.tvAdded)
        private val tvRemoved: TextView = v.findViewById(R.id.tvRemoved)

        fun bind(version: VersionEntity, isLatest: Boolean) {
            tvBadge.text = "V${version.versionNumber}"
            tvLatest.isVisible = isLatest
            latestAccent.isVisible = isLatest
            tvLabel.text = "Version ${version.versionNumber}"
            tvTimestamp.text = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date(version.createdAt))
            btnDiff.text = "PREVIEW"

            diffStatsRow.isVisible = false
            tvAdded.isVisible = false
            tvRemoved.isVisible = false
        }
    }
}
