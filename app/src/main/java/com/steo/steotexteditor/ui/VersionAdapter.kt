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
    initial: List<VersionListItem>,
    private val onClick: (VersionEntity) -> Unit,
    private val onLongClick: (VersionEntity) -> Unit
) : ListAdapter<VersionListItem, VersionAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VersionListItem>() {
            override fun areItemsTheSame(oldItem: VersionListItem, newItem: VersionListItem): Boolean =
                oldItem.version.id == newItem.version.id

            override fun areContentsTheSame(oldItem: VersionListItem, newItem: VersionListItem): Boolean =
                oldItem == newItem
        }
    }

    init { submitList(initial) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_version, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val version = item.version
        val latestVersionNumber = currentList.maxOfOrNull { it.version.versionNumber }
        holder.bind(item, latestVersionNumber == version.versionNumber)
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

        fun bind(item: VersionListItem, isLatest: Boolean) {
            val version = item.version
            tvBadge.text = "V${version.versionNumber}"
            tvLatest.isVisible = isLatest
            latestAccent.isVisible = isLatest
            tvLabel.text = version.label.ifBlank { "Version ${version.versionNumber}" }
            tvTimestamp.text = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date(version.createdAt))
            btnDiff.text = "DIFF"

            diffStatsRow.isVisible = item.addedLines > 0 || item.removedLines > 0
            tvAdded.isVisible = item.addedLines > 0
            tvRemoved.isVisible = item.removedLines > 0
            tvAdded.text = "+${item.addedLines} LINES"
            tvRemoved.text = "-${item.removedLines} LINES"
        }
    }
}

data class VersionListItem(
    val version: VersionEntity,
    val addedLines: Int,
    val removedLines: Int
)
