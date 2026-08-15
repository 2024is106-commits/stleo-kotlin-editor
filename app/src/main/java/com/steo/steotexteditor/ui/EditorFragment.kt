package com.steo.steotexteditor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.steo.steotexteditor.R
import com.steo.steotexteditor.data.db.FileEntity
import com.steo.steotexteditor.data.repository.FileRepository
import com.steo.steotexteditor.databinding.FragmentEditorBinding
import kotlinx.coroutines.launch

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fileRepository: FileRepository
    private var currentFile: FileEntity? = null
    private var currentFileId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        fileRepository = FileRepository(requireContext())
        
        // Get file ID from arguments
        currentFileId = arguments?.getLong("file_id") ?: -1
        
        setupToolbar()
        loadFile()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            // Handle back navigation
            parentFragmentManager.popBackStack()
        }
        
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> {
                    saveFile()
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
            return
        }
        
        lifecycleScope.launch {
            val file = fileRepository.getFileById(currentFileId)
            if (file != null) {
                currentFile = file
                val content = com.steo.steotexteditor.util.FileHelper.readFile(file.path)
                binding.editorView.setText(content)
            } else {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun saveFile() {
        val content = binding.editorView.text.toString()
        val file = currentFile ?: return
        
        lifecycleScope.launch {
            val fileId: Long
            if (file.id == 0L) {
                // New file - create it
                val newFile = file.copy(
                    name = file.name.ifEmpty { "Untitled" },
                    path = com.steo.steotexteditor.util.FileHelper.getStorageDir(requireContext())
                        .resolve(file.name.ifEmpty { "untitled.txt" }).absolutePath,
                    lastModified = System.currentTimeMillis()
                )
                fileId = fileRepository.saveFileWithVersion(newFile, content, "Initial save")
            } else {
                // Existing file - save with version
                fileId = fileRepository.saveFileWithVersion(file, content, "Manual save")
            }
            
            currentFileId = fileId
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showVersions() {
        // This would typically open a dialog or fragment showing version history
        Toast.makeText(context, "Version history not implemented", Toast.LENGTH_SHORT).show()
    }

    private fun deleteFile() {
        val file = currentFile ?: return
        
        lifecycleScope.launch {
            // Delete from database
            fileRepository.deleteFile(file)
            // Delete from disk
            com.steo.steotexteditor.util.FileHelper.deleteFile(file.path)
            
            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
