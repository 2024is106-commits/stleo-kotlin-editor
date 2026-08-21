package com.steo.steotexteditor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.steo.steotexteditor.R

class RunFragment : Fragment() {
    private lateinit var viewModel: EditorViewModel
    private var currentFileId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(requireActivity()).get(EditorViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_run, container, false)
        val fileName = root.findViewById<TextView>(R.id.tvRunFileName)

        root.findViewById<View>(R.id.btnRunBackToEditor).setOnClickListener {
            val bundle = Bundle().apply {
                if (currentFileId > 0L) {
                    putLong("file_id", currentFileId)
                }
            }
            findNavController().navigate(R.id.nav_edit, bundle)
        }

        viewModel.currentFile.observe(viewLifecycleOwner) { file ->
            currentFileId = file?.id ?: -1L
            fileName.text = file?.name ?: "Untitled.txt"
        }

        return root
    }
}
