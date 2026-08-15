package com.steo.steotexteditor.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.steo.steotexteditor.R
import com.steo.steotexteditor.data.db.FileEntity
import com.steo.steotexteditor.data.db.VersionEntity
import com.steo.steotexteditor.databinding.FragmentEditorBinding
import com.steo.steotexteditor.util.FileHelper
import kotlinx.coroutines.launch

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EditorViewModel by viewModels()
    private var currentFile: FileEntity? = null
    private var currentFileId: Long = -1
    private var isDirty = false

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.openFile(uri) { file, content ->
                activity?.runOnUiThread {
                    if (file != null && content != null) {
                        currentFile = file
                        currentFileId = file.id
                        binding.editorView.setText(content)
                        isDirty = false
                        updateToolbarTitle()
                    } else {
                        Toast.makeText(requireContext(), "Failed to open file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get file ID from arguments
        currentFileId = arguments?.getLong("file_id") ?: -1
        
        setupToolbar()
        setupTextWatcher()
        loadFile()
        
        // Restore crash recovery content if available
        val crashRecoveryContent = FileHelper.readCrashRecovery(requireContext())
        if (crashRecoveryContent != null) {
            binding.editorView.setText(crashRecoveryContent)
            isDirty = true
            updateToolbarTitle()
            FileHelper.clearCrashRecovery(requireContext())
        }
    }

    private fun setupToolbar() {
        binding.toolbar.overflowIcon?.setTint(Color.parseColor("#DCDCF0"))
        
        binding.toolbar.setNavigationOnClickListener {
            // Handle back navigation
            parentFragmentManager.popBackStack()
        }
        
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_new -> {
                    newFile()
                    true
                }
                R.id.action_open -> {
                    openFile()
                    true
                }
                R.id.action_save -> {
                    saveFile()
                    true
                }
                R.id.action_save_as -> {
                    saveAsFile()
                    true
                }
                R.id.action_versions -> {
                    showVersions()
                    true
                }
                R.id.action_delete -> {
                    deleteFile()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTextWatcher() {
        binding.editorView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isDirty) {
                    isDirty = true
                    updateToolbarTitle()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateToolbarTitle() {
        val title = currentFile?.name ?: "Untitled"
        if (isDirty) {
            binding.toolbar.title = "$title*"
        } else {
            binding.toolbar.title = title
        }
    }

    private fun loadFile() {
        if (currentFileId == -1L) {
            // New file
            currentFile = FileEntity(
                id = 0,
                name = "Untitled",
                path = "",
                lastModified = System.currentTimeMillis(),
                isReadOnly = false
            )
            binding.editorView.setText("")
            updateToolbarTitle()
            return
        }
        
        viewModel.loadFile(currentFileId) { file, content ->
            activity?.runOnUiThread {
                if (file != null && content != null) {
                    currentFile = file
                    binding.editorView.setText(content)
                    isDirty = false
                    updateToolbarTitle()
                } else {
                    Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun newFile() {
        if (isDirty) {
            showUnsavedChangesDialog(object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> saveFile()
                        DialogInterface.BUTTON_NEGATIVE -> {
                            // Don't save
                            clearEditor()
                        }
                        DialogInterface.BUTTON_NEUTRAL -> {
                            // Cancel
                        }
                    }
                }
            })
        } else {
            clearEditor()
        }
    }

    private fun clearEditor() {
        binding.editorView.setText("")
        currentFile = FileEntity(
            id = 0,
            name = "Untitled",
            path = "",
            lastModified = System.currentTimeMillis(),
            isReadOnly = false
        )
        currentFileId = -1
        isDirty = false
        updateToolbarTitle()
    }

    private fun showUnsavedChangesDialog(listener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save before continuing?")
            .setPositiveButton("Save", listener)
            .setNegativeButton("Don't Save", listener)
            .setNeutralButton("Cancel", listener)
            .show()
    }

    private fun openFile() {
        if (isDirty) {
            showUnsavedChangesDialog(object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            saveFile()
                            openFileLauncher.launch("*/*")
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            openFileLauncher.launch("*/*")
                        }
                        DialogInterface.BUTTON_NEUTRAL -> {
                            // Cancel
                        }
                    }
                }
            })
        } else {
            openFileLauncher.launch("*/*")
        }
    }

    private fun saveFile() {
        val content = binding.editorView.text.toString()
        val file = currentFile ?: return
        
        if (file.id == 0L) {
            // New file - create it
            viewModel.createNewFile(file.name, content) { fileId ->
                activity?.runOnUiThread {
                    currentFileId = fileId
                    isDirty = false
                    updateToolbarTitle()
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Existing file - save with version
            viewModel.saveFile(file, content) { fileId ->
                activity?.runOnUiThread {
                    currentFileId = fileId
                    isDirty = false
                    updateToolbarTitle()
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveAsFile() {
        val content = binding.editorView.text.toString()
        
        val input = EditText(requireContext())
        input.setText(currentFile?.name ?: "Untitled")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Save As")
            .setMessage("Enter file name:")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val fileName = input.text.toString()
                if (fileName.isNotEmpty()) {
                    viewModel.createNewFile(fileName, content) { fileId ->
                        activity?.runOnUiThread {
                            currentFileId = fileId
                            isDirty = false
                            updateToolbarTitle()
                            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVersions() {
        val file = currentFile ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val versions = viewModel.getVersionsForFile(file.id)
            if (versions.isEmpty()) {
                Toast.makeText(requireContext(), "No versions available", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val versionNumbers = versions.map { it.versionNumber.toString() }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Select Version")
                .setItems(versionNumbers.map { "Version $it" }.toTypedArray()) { _, which ->
                    val selectedVersion = versions[which]
                    viewModel.restoreVersion(file.id, selectedVersion.versionNumber) { success ->
                        if (success) {
                            loadFile()
                            Toast.makeText(requireContext(), "Version restored", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed to restore version", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
        }
    }

    private fun deleteFile() {
        val file = currentFile ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFile(file) {
                    Toast.makeText(requireContext(), "File deleted", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onStop() {
        super.onStop()
        // Save crash recovery content
        val content = binding.editorView.text.toString()
        if (content.isNotEmpty()) {
            FileHelper.saveCrashRecovery(requireContext(), content)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
