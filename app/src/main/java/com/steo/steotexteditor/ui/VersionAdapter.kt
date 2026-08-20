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
        private val tvAdded: TextView = v.findViewById(R.id.tvAdded)
        private val tvRemoved: TextView = v.findViewById(R.id.tvRemoved)

        fun bind(version: VersionEntity, isLatest: Boolean) {
            tvBadge.text = "V${version.versionNumber}"
            tvLatest.isVisible = isLatest
            latestAccent.isVisible = isLatest
            tvLabel.text = if (version.label.isBlank()) "Updated main.kt" else version.label
            tvTimestamp.text = formatRelativeTime(version.createdAt)

            val added = if (version.versionNumber == 1) {
                version.patchText?.lines()?.count { it.isNotBlank() } ?: 0
            } else {
                countDiffLines(version.patchText, '+')
            }
            val removed = countDiffLines(version.patchText, '-')
            tvAdded.text = "+$added LINES"
            tvRemoved.text = "-$removed LINES"
            tvAdded.isVisible = added > 0
            tvRemoved.isVisible = removed > 0
        }

        private fun countDiffLines(patchText: String?, marker: Char): Int {
            if (patchText.isNullOrBlank()) return 0
            return patchText.lines().count { line ->
                line.firstOrNull() == marker && !line.startsWith("$marker$marker$marker")
            }
        }

        private fun formatRelativeTime(createdAt: Long): String {
            val elapsedMillis = System.currentTimeMillis() - createdAt
            val minutes = elapsedMillis / 60000L
            val hours = minutes / 60L
            val days = hours / 24L
            return when {
                minutes < 1 -> "NOW"
                minutes < 60 -> "${minutes}M AGO"
                hours < 24 -> "${hours}H AGO"
                days < 7 -> "${days}D AGO"
                else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(createdAt))
            }
        }
    }
}
