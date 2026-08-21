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
            val diffBlocks = versions.zipWithNext().map { (previous, current) ->
                val previousContent = viewModel.reconstructVersion(file.id, previous.versionNumber).orEmpty()
                val currentContent = viewModel.reconstructVersion(file.id, current.versionNumber).orEmpty()
                DiffBlock(
                    previous = previous,
                    current = current,
                    rows = buildLineDiff(previousContent, currentContent)
                )
            }
            activity?.runOnUiThread {
                if (versions.size < 2) {
                    content.addView(emptyText("NO DIFFS TO SHOW YET.\nTRY SAVING YOUR CODE"))
                    return@runOnUiThread
                }

                diffBlocks.forEach { block ->
                    addDiffBlock(content, block.previous, block.current, block.rows)
                }
            }
        }
    }

    private fun addDiffBlock(
        parent: LinearLayout,
        previous: VersionEntity,
        current: VersionEntity,
        rows: List<DiffRow>
    ) {
        parent.addView(TextView(requireContext()).apply {
            text = "DIFF - V${previous.versionNumber} -> V${current.versionNumber}"
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_off_white))
            typeface = loadSilkscreen()
            textSize = 16f
            letterSpacing = 0.076f
            setPadding(0, 0, 0, 12)
        })

        if (rows.isEmpty()) {
            parent.addView(TextView(requireContext()).apply {
                text = "NO LINE CHANGES"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.line_number_gray))
                typeface = Typeface.create("Consolas", Typeface.NORMAL)
                textSize = 12f
                setPadding(0, 0, 0, 18)
            })
            return
        }

        rows.forEach { row -> parent.addView(diffRowView(row)) }
    }

    private fun diffRowView(row: DiffRow): TextView {
        val color = when (row.kind) {
            DiffKind.ADDED -> ColorPalette.added
            DiffKind.REMOVED -> ColorPalette.removed
            DiffKind.MODIFIED -> ColorPalette.modified
        }
        return TextView(requireContext()).apply {
            text = row.label
            setTextColor(color)
            typeface = Typeface.create("Consolas", Typeface.NORMAL)
            textSize = 14f
            setPadding(0, 2, 0, 2)
        }
    }

    private fun buildLineDiff(previousContent: String, currentContent: String): List<DiffRow> {
        val previousLines = previousContent.lines()
        val currentLines = currentContent.lines()
        val lcs = buildLcsTable(previousLines, currentLines)
        val rows = mutableListOf<DiffRow>()
        var oldIndex = 0
        var newIndex = 0

        while (oldIndex < previousLines.size || newIndex < currentLines.size) {
            if (
                oldIndex < previousLines.size &&
                newIndex < currentLines.size &&
                previousLines[oldIndex] == currentLines[newIndex]
            ) {
                oldIndex++
                newIndex++
                continue
            }

            val removedStart = oldIndex
            val addedStart = newIndex
            while (
                oldIndex < previousLines.size &&
                newIndex < currentLines.size &&
                previousLines[oldIndex] != currentLines[newIndex]
            ) {
                if (lcs[oldIndex + 1][newIndex] >= lcs[oldIndex][newIndex + 1]) {
                    oldIndex++
                } else {
                    newIndex++
                }
            }

            while (
                oldIndex < previousLines.size &&
                (newIndex >= currentLines.size || lcs[oldIndex + 1][newIndex] >= lcs[oldIndex][newIndex + 1]) &&
                (newIndex >= currentLines.size || previousLines[oldIndex] != currentLines[newIndex])
            ) {
                oldIndex++
            }

            while (
                newIndex < currentLines.size &&
                (oldIndex >= previousLines.size || lcs[oldIndex][newIndex + 1] > lcs[oldIndex + 1][newIndex]) &&
                (oldIndex >= previousLines.size || previousLines[oldIndex] != currentLines[newIndex])
            ) {
                newIndex++
            }

            appendChangedRows(
                rows = rows,
                previousLines = previousLines,
                currentLines = currentLines,
                removedStart = removedStart,
                removedEnd = oldIndex,
                addedStart = addedStart,
                addedEnd = newIndex
            )
        }

        return rows
    }

    private fun appendChangedRows(
        rows: MutableList<DiffRow>,
        previousLines: List<String>,
        currentLines: List<String>,
        removedStart: Int,
        removedEnd: Int,
        addedStart: Int,
        addedEnd: Int
    ) {
        val removedCount = removedEnd - removedStart
        val addedCount = addedEnd - addedStart
        val modifiedCount = minOf(removedCount, addedCount)

        for (offset in 0 until modifiedCount) {
            rows.add(
                DiffRow(
                    DiffKind.MODIFIED,
                    "M L${removedStart + offset + 1}->L${addedStart + offset + 1}: " +
                        "${previousLines[removedStart + offset]}  =>  ${currentLines[addedStart + offset]}"
                )
            )
        }

        for (index in removedStart + modifiedCount until removedEnd) {
            rows.add(DiffRow(DiffKind.REMOVED, "- L${index + 1}: ${previousLines[index]}"))
        }

        for (index in addedStart + modifiedCount until addedEnd) {
            rows.add(DiffRow(DiffKind.ADDED, "+ L${index + 1}: ${currentLines[index]}"))
        }
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
            gravity = android.view.Gravity.CENTER
            textSize = 12f
            letterSpacing = 0.076f
        }
    }

    private fun loadSilkscreen(): Typeface {
        return try {
            androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.silkscreen) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private object ColorPalette {
        val added = android.graphics.Color.parseColor("#00FF3B")
        val removed = android.graphics.Color.parseColor("#FF3045")
        val modified = android.graphics.Color.parseColor("#FFD84D")
    }

    private enum class DiffKind {
        ADDED,
        REMOVED,
        MODIFIED
    }

    private data class DiffRow(
        val kind: DiffKind,
        val label: String
    )

    private data class DiffBlock(
        val previous: VersionEntity,
        val current: VersionEntity,
        val rows: List<DiffRow>
    )
}
