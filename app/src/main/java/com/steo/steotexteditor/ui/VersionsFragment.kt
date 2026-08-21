package com.steo.steotexteditor.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.steo.steotexteditor.R
import com.steo.steotexteditor.databinding.FragmentVersionsBinding
import com.steo.steotexteditor.data.db.VersionEntity
import kotlinx.coroutines.launch

class VersionsFragment : Fragment() {

    private var _binding: FragmentVersionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditorViewModel by viewModels({ requireActivity() })
    private lateinit var adapter: VersionAdapter
    private var fileId: Long = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentVersionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentFile = viewModel.currentFile.value
        fileId = currentFile?.id ?: arguments?.getLong("file_id") ?: -1

        adapter = VersionAdapter(emptyList(), onClick = { version ->
            previewVersion(version)
        }, onLongClick = { version ->
            AlertDialog.Builder(requireContext())
                .setTitle("Restore Version")
                .setMessage("Restore version ${version.versionNumber}?")
                .setPositiveButton("Restore") { _, _ ->
                    viewModel.restoreVersion(fileId, version.versionNumber) { success ->
                        activity?.runOnUiThread {
                            if (success) {
                                Toast.makeText(requireContext(), "Version restored", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to restore version", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        })

        binding.rvVersions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVersions.adapter = adapter

        loadVersions()
    }

    private fun loadVersions() {
        if (viewModel.currentFile.value == null) {
            showEmptyState("NO FILE OPEN.\nOPEN OR CREATE A FILE IN THE EDITOR TO VIEW ITS HISTORY.")
            binding.tvRevisionCount.text = "0 REVISIONS"
            return
        }

        if (fileId <= 0L) {
            showEmptyState("NO SAVED SNAPSHOTS YET.\nSAVE THIS FILE TO VIEW ITS HISTORY.")
            binding.tvRevisionCount.text = "0 REVISIONS"
            return
        }
        lifecycleScope.launch {
            val versions = viewModel.getVersionsForFile(fileId)
            val items = versions.mapIndexed { index, version ->
                val previousContent = if (index == 0) "" else viewModel.reconstructVersion(fileId, versions[index - 1].versionNumber).orEmpty()
                val currentContent = viewModel.reconstructVersion(fileId, version.versionNumber).orEmpty()
                val stats = countLineChanges(previousContent, currentContent)
                VersionListItem(version, stats.added, stats.removed)
            }
            activity?.runOnUiThread {
                adapter.submitList(items.asReversed())
                binding.tvRevisionCount.text = "${versions.size} ${if (versions.size == 1) "REVISION" else "REVISIONS"}"
                if (versions.isEmpty()) {
                    showEmptyState("NO SAVED SNAPSHOTS YET.\nSAVE THIS FILE TO VIEW ITS HISTORY.")
                } else {
                    binding.rvVersions.visibility = View.VISIBLE
                    binding.tvVersionsEmpty.visibility = View.GONE
                }
            }
        }
    }

    private fun previewVersion(version: VersionEntity) {
        lifecycleScope.launch {
            val content = viewModel.reconstructVersion(fileId, version.versionNumber).orEmpty()
            activity?.runOnUiThread {
                val preview = TextView(requireContext()).apply {
                    text = content.ifBlank { "EMPTY VERSION" }
                    setTextColor(android.graphics.Color.parseColor("#E8E8F0"))
                    typeface = android.graphics.Typeface.create("Consolas", android.graphics.Typeface.NORMAL)
                    setPadding(32, 24, 32, 24)
                }
                val scroll = ScrollView(requireContext()).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#111118"))
                    addView(preview)
                }
                val fileName = viewModel.currentFile.value?.name.orEmpty()
                AlertDialog.Builder(requireContext())
                    .setTitle("Preview v${version.versionNumber}${if (fileName.isNotBlank()) " - $fileName" else ""}")
                    .setView(scroll)
                    .setPositiveButton("Close", null)
                    .show()
            }
        }
    }

    private fun showEmptyState(message: String) {
        binding.rvVersions.visibility = View.GONE
        binding.tvVersionsEmpty.text = message
        binding.tvVersionsEmpty.visibility = View.VISIBLE
    }

    private fun countLineChanges(previousContent: String, currentContent: String): LineStats {
        val previousLines = previousContent.lines().filterNot { it.isEmpty() }
        val currentLines = currentContent.lines().filterNot { it.isEmpty() }
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

        var oldIndex = 0
        var newIndex = 0
        var added = 0
        var removed = 0
        while (oldIndex < previousLines.size || newIndex < currentLines.size) {
            when {
                oldIndex < previousLines.size &&
                    newIndex < currentLines.size &&
                    previousLines[oldIndex] == currentLines[newIndex] -> {
                    oldIndex++
                    newIndex++
                }
                newIndex < currentLines.size &&
                    (oldIndex >= previousLines.size || table[oldIndex][newIndex + 1] >= table[oldIndex + 1][newIndex]) -> {
                    added++
                    newIndex++
                }
                oldIndex < previousLines.size -> {
                    removed++
                    oldIndex++
                }
            }
        }
        return LineStats(added, removed)
    }

    private data class LineStats(val added: Int, val removed: Int)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
