package com.steo.steotexteditor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.steo.steotexteditor.R
import com.steo.steotexteditor.data.db.FileEntity
import java.util.*
import java.util.concurrent.TimeUnit

class RecentFilesAdapter(
    private var items: List<FileEntity>,
    private val onClick: (FileEntity) -> Unit
) : RecyclerView.Adapter<RecentFilesAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvRelativeTime: TextView = view.findViewById(R.id.tvRelativeTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.nav_recent_item, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = items[position]
        holder.tvFileName.text = file.name
        // Badge: determine extension
        val ext = file.name.substringAfterLast('.', "").lowercase(Locale.getDefault())
        val badge = when (ext) {
            "kt" -> "KT"
            "md" -> "MD"
            else -> if (ext.isNotEmpty()) ext.take(2).uppercase(Locale.getDefault()) else "•"
        }
        holder.tvBadge.text = badge

        holder.tvRelativeTime.text = formatRelativeTime(file.lastModified)

        holder.itemView.setOnClickListener { onClick(file) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<FileEntity>) {
        items = list
        notifyDataSetChanged()
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        if (days >= 30) {
            val months = days / 30
            return "${months}M AGO"
        }
        if (days > 0) return "${days}D AGO"
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        if (hours > 0) return "${hours}H AGO"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        if (minutes > 0) return "${minutes}M AGO"
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        return "${seconds}s AGO"
    }
}
