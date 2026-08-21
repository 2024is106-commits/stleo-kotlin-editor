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
            activity?.runOnUiThread {
                adapter.submitList(versions)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
