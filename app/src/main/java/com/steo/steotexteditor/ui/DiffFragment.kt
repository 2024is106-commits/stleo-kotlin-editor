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
import com.steo.steotexteditor.data.db.VersionEntity
import kotlinx.coroutines.launch

class DiffFragment : Fragment() {
    private lateinit var viewModel: EditorViewModel

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

        loadDiffs(content)
        return root
    }

    private fun loadDiffs(content: LinearLayout) {
        val file = viewModel.currentFile.value
        if (file == null) {
            content.addView(emptyText("NO FILE OPEN.\nOPEN OR CREATE A FILE IN THE EDITOR TO VIEW ITS HISTORY."))
            return
        }

        if (file.id <= 0L) {
            content.addView(emptyText("NO DIFFS TO SHOW YET.\nTRY SAVING YOUR CODE"))
            return
        }

        lifecycleScope.launch {
            val versions = viewModel.getVersionsForFile(file.id)
            activity?.runOnUiThread {
                if (versions.size < 2) {
                    content.addView(emptyText("NO DIFFS TO SHOW YET.\nTRY SAVING YOUR CODE"))
                    return@runOnUiThread
                }

                versions.drop(1).forEach { version ->
                    addDiffBlock(content, version)
                }
            }
        }
    }

    private fun addDiffBlock(parent: LinearLayout, version: VersionEntity) {
        parent.addView(TextView(requireContext()).apply {
            text = "DIFF - V${version.versionNumber - 1} -> V${version.versionNumber}"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_off_white))
            typeface = loadSilkscreen()
            textSize = 16f
            letterSpacing = 0.08f
            setPadding(0, 0, 0, 12)
        })

        version.patchText.orEmpty().lines().forEach { line ->
            if (line.isBlank() || line.startsWith("---") || line.startsWith("+++") || line.startsWith("@@")) return@forEach
            val color = when {
                line.startsWith("+") -> ColorPalette.added
                line.startsWith("-") -> ColorPalette.removed
                else -> ContextCompat.getColor(requireContext(), R.color.text_off_white)
            }
            parent.addView(TextView(requireContext()).apply {
                text = line
                setTextColor(color)
                typeface = Typeface.MONOSPACE
                textSize = 14f
                setPadding(0, 2, 0, 2)
            })
        }
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

    private object ColorPalette {
        val added = android.graphics.Color.parseColor("#00FF3B")
        val removed = android.graphics.Color.parseColor("#FF3045")
    }
}
