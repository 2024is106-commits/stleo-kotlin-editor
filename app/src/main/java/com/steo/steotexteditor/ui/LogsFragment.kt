package com.steo.steotexteditor.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.steo.steotexteditor.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogsFragment : Fragment() {
    private lateinit var viewModel: EditorViewModel
    private val dateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(requireActivity()).get(EditorViewModel::class.java)
        val root = ScrollView(requireContext())
        root.setBackgroundResource(R.drawable.starfield_background)
        val padding = (24 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        root.addView(content, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        loadLogs(content)
        return root
    }

    private fun loadLogs(content: LinearLayout) {
        val file = viewModel.currentFile.value
        if (file == null) {
            content.addView(emptyText("NO FILE OPEN.\nOPEN OR CREATE A FILE IN THE EDITOR TO VIEW ITS HISTORY."))
            return
        }

        lifecycleScope.launch {
            val versions = if (file.id > 0L) viewModel.getVersionsForFile(file.id) else emptyList()
            activity?.runOnUiThread {
                val renameLogs = viewModel.activityLogs.value.orEmpty()
                    .filter { it.fileId == file.id || (it.fileId == 0L && it.fileName == file.name) }

                val rows = versions.map { version ->
                    LogRow("Saved v${version.versionNumber}", version.createdAt)
                } + renameLogs.map { log ->
                    LogRow(log.message, log.timestamp)
                }

                if (rows.isEmpty()) {
                    content.addView(emptyText("NO LOGS TO SHOW YET.\nTRY SAVING YOUR CODE"))
                    return@runOnUiThread
                }

                rows.sortedBy { it.timestamp }.forEach { row ->
                    addLogRow(content, row)
                }
            }
        }
    }

    private fun addLogRow(parent: LinearLayout, row: LogRow) {
        parent.addView(TextView(requireContext()).apply {
            text = "${row.message} - ${dateFormat.format(Date(row.timestamp))}"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_off_white))
            typeface = loadSilkscreen()
            textSize = 14f
            letterSpacing = 0.06f
            setPadding(0, 0, 0, 16)
        })
    }

    private fun emptyText(message: String): TextView {
        return TextView(requireContext()).apply {
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), R.color.line_number_gray))
            typeface = Typeface.MONOSPACE
            gravity = android.view.Gravity.CENTER
            textSize = 12f
            letterSpacing = 0.08f
        }
    }

    private fun loadSilkscreen(): Typeface {
        return try {
            androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.silkscreen) ?: Typeface.DEFAULT_BOLD
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    private data class LogRow(val message: String, val timestamp: Long)
}
