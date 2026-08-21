package com.steo.steotexteditor.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.steo.steotexteditor.R
import com.steo.steotexteditor.data.db.VersionEntity
import kotlinx.coroutines.launch

class DiffFragment : Fragment() {
    private lateinit var viewModel: EditorViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(requireActivity()).get(EditorViewModel::class.java)

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.starfield_background)
            setPadding(dp(16), 0, dp(16), dp(16))
        }

        val scroll = ScrollView(requireContext()).apply {
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            background = solid(Color.parseColor("#111515"))
        }
        scroll.addView(
            content,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        loadLatestDiff(root, content)
        return root
    }

    private fun loadLatestDiff(root: LinearLayout, content: LinearLayout) {
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
            if (versions.size < 2) {
                activity?.runOnUiThread {
                    content.addView(emptyText("NO DIFFS TO SHOW YET.\nTRY SAVING YOUR CODE"))
                }
                return@launch
            }

            val previous = versions[versions.lastIndex - 1]
            val current = versions.last()
            val previousContent = viewModel.reconstructVersion(file.id, previous.versionNumber).orEmpty()
            val currentContent = viewModel.reconstructVersion(file.id, current.versionNumber).orEmpty()
            val rows = buildDiffRows(previousContent, currentContent)

            activity?.runOnUiThread {
                addDiffPanel(content, previous, current, rows)
                root.addView(restoreButton(current))
            }
        }
    }

    private fun addDiffPanel(parent: LinearLayout, previous: VersionEntity, current: VersionEntity, rows: List<DiffRow>) {
        parent.addView(TextView(requireContext()).apply {
            text = "DIFF - V${previous.versionNumber} -> V${current.versionNumber}"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_off_white))
            typeface = loadSilkscreen()
            textSize = 14f
            letterSpacing = 0.076f
            setPadding(dp(16), dp(14), dp(16), dp(14))
        })

        if (rows.isEmpty()) {
            parent.addView(emptyText("NO LINE CHANGES"))
            return
        }

        rows.forEach { row ->
            parent.addView(diffRowView(row))
        }
    }

    private fun diffRowView(row: DiffRow): TextView {
        val backgroundColor = when (row.kind) {
            DiffKind.CONTEXT -> Color.TRANSPARENT
            DiffKind.ADDED -> Color.parseColor("#1D2924")
            DiffKind.REMOVED -> Color.parseColor("#312522")
            DiffKind.MODIFIED -> Color.parseColor("#2D2A1D")
        }
        val textColor = when (row.kind) {
            DiffKind.CONTEXT -> Color.parseColor("#C8C8C8")
            DiffKind.ADDED -> Color.parseColor("#D9F0DD")
            DiffKind.REMOVED -> Color.parseColor("#FFB8AE")
            DiffKind.MODIFIED -> ColorPalette.modified
        }
        return TextView(requireContext()).apply {
            text = row.label
            setTextColor(textColor)
            typeface = Typeface.create("Consolas", Typeface.NORMAL)
            textSize = 14f
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(6), dp(12), dp(6))
            setBackgroundColor(backgroundColor)
        }
    }

    private fun restoreButton(version: VersionEntity): TextView {
        return TextView(requireContext()).apply {
            text = "RESTORE THIS VERSION"
            setTextColor(Color.parseColor("#151516"))
            typeface = loadSilkscreen()
            textSize = 14f
            letterSpacing = 0.076f
            gravity = Gravity.CENTER
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_history_24, 0, 0, 0)
            compoundDrawablePadding = dp(8)
            background = solid(Color.parseColor("#E8E8E8"))
            setOnClickListener {
                val fileId = viewModel.currentFile.value?.id ?: return@setOnClickListener
                viewModel.restoreVersion(fileId, version.versionNumber) { success ->
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            if (success) "Version restored" else "Failed to restore version",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                topMargin = dp(12)
            }
        }
    }

    private fun buildDiffRows(previousContent: String, currentContent: String): List<DiffRow> {
        val previousLines = previousContent.lines()
        val currentLines = currentContent.lines()
        val table = buildLcsTable(previousLines, currentLines)
        val operations = mutableListOf<LineOperation>()
        var oldIndex = 0
        var newIndex = 0

        while (oldIndex < previousLines.size || newIndex < currentLines.size) {
            when {
                oldIndex < previousLines.size &&
                    newIndex < currentLines.size &&
                    previousLines[oldIndex] == currentLines[newIndex] -> {
                    operations.add(LineOperation(DiffKind.CONTEXT, oldIndex + 1, newIndex + 1, previousLines[oldIndex]))
                    oldIndex++
                    newIndex++
                }
                newIndex < currentLines.size &&
                    (oldIndex >= previousLines.size || table[oldIndex][newIndex + 1] >= table[oldIndex + 1][newIndex]) -> {
                    operations.add(LineOperation(DiffKind.ADDED, null, newIndex + 1, currentLines[newIndex]))
                    newIndex++
                }
                oldIndex < previousLines.size -> {
                    operations.add(LineOperation(DiffKind.REMOVED, oldIndex + 1, null, previousLines[oldIndex]))
                    oldIndex++
                }
            }
        }

        return combineModifiedRows(operations).map { op ->
            when (op.kind) {
                DiffKind.CONTEXT -> DiffRow(DiffKind.CONTEXT, "${op.newLineNumber.toString().padStart(4)}    ${op.text}")
                DiffKind.ADDED -> DiffRow(DiffKind.ADDED, "+ L${op.newLineNumber}    ${op.text}")
                DiffKind.REMOVED -> DiffRow(DiffKind.REMOVED, "- L${op.oldLineNumber}    ${op.text}")
                DiffKind.MODIFIED -> DiffRow(
                    DiffKind.MODIFIED,
                    "~ L${op.oldLineNumber}->L${op.newLineNumber}    ${op.text}  =>  ${op.newText.orEmpty()}"
                )
            }
        }
    }

    private fun combineModifiedRows(operations: List<LineOperation>): List<LineOperation> {
        val combined = mutableListOf<LineOperation>()
        var index = 0
        while (index < operations.size) {
            val current = operations[index]
            val next = operations.getOrNull(index + 1)
            if (current.kind == DiffKind.REMOVED && next?.kind == DiffKind.ADDED) {
                combined.add(
                    LineOperation(
                        kind = DiffKind.MODIFIED,
                        oldLineNumber = current.oldLineNumber,
                        newLineNumber = next.newLineNumber,
                        text = current.text,
                        newText = next.text
                    )
                )
                index += 2
            } else {
                combined.add(current)
                index++
            }
        }
        return combined
    }

    private fun buildLcsTable(previousLines: List<String>, currentLines: List<String>): Array<IntArray> {
        val table = Array(previousLines.size + 1) { IntArray(currentLines.size + 1) }
        for (oldIndex in previousLines.indices.reversed()) {
            for (newIndex in currentLines.indices.reversed()) {
                table[oldIndex][newIndex] = if (previousLines[oldIndex] == currentLines[newIndex]) {
                    table[oldIndex + 1][newIndex + 1] + 1
                } else {
                    maxOf(table[oldIndex + 1][newIndex], table[oldIndex][newIndex + 1])
                }
            }
        }
        return table
    }

    private fun emptyText(message: String): TextView {
        return TextView(requireContext()).apply {
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), R.color.line_number_gray))
            typeface = loadSilkscreen()
            gravity = Gravity.CENTER
            textSize = 12f
            letterSpacing = 0.076f
            setPadding(dp(24), dp(40), dp(24), dp(40))
        }
    }

    private fun loadSilkscreen(): Typeface {
        return try {
            androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.silkscreen) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun solid(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 0f
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private object ColorPalette {
        val modified = Color.parseColor("#FFD84D")
    }

    private enum class DiffKind {
        CONTEXT,
        ADDED,
        REMOVED,
        MODIFIED
    }

    private data class DiffRow(
        val kind: DiffKind,
        val label: String
    )

    private data class LineOperation(
        val kind: DiffKind,
        val oldLineNumber: Int?,
        val newLineNumber: Int?,
        val text: String,
        val newText: String? = null
    )
}
