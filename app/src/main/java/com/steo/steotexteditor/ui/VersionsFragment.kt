package com.steo.steotexteditor.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        fileId = arguments?.getLong("file_id") ?: -1

        adapter = VersionAdapter(emptyList(), onClick = { version ->
            // Confirm restore
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
        }, onLongClick = { version ->
            // Show diff bottom sheet
            val sheet = DiffBottomSheet.newInstance(fileId, version.versionNumber)
            sheet.show(childFragmentManager, "diff")
        })

        binding.rvVersions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVersions.adapter = adapter

        loadVersions()
    }

    private fun loadVersions() {
        if (fileId == -1L) return
        lifecycleScope.launch {
            val versions = viewModel.getVersionsForFile(fileId)
            activity?.runOnUiThread {
                adapter.submitList(versions)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
