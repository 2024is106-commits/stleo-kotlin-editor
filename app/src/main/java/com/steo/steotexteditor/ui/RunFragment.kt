package com.steo.steotexteditor.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.steo.steotexteditor.R
import io.noties.markwon.Markwon
import java.util.Locale

class RunFragment : Fragment() {
    private lateinit var viewModel: EditorViewModel
    private lateinit var markwon: Markwon
    private lateinit var fileName: TextView
    private lateinit var outputText: TextView
    private var currentFileId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(requireActivity()).get(EditorViewModel::class.java)
        markwon = Markwon.builder(requireContext()).build()
        val root = inflater.inflate(R.layout.fragment_run, container, false)
        fileName = root.findViewById(R.id.tvRunFileName)
        outputText = root.findViewById(R.id.tvRunPlaceholder)

        root.findViewById<View>(R.id.btnRunBackToEditor).setOnClickListener {
            val bundle = Bundle().apply {
                if (currentFileId > 0L) {
                    putLong("file_id", currentFileId)
                }
            }
            findNavController().navigate(R.id.nav_edit, bundle)
        }

        viewModel.sessionState.observe(viewLifecycleOwner) { state ->
            renderRunState(state)
        }

        return root
    }

    private fun renderRunState(state: EditorSessionState) {
        currentFileId = state.currentFileId
        fileName.text = state.currentFileName.ifBlank { "Untitled.txt" }

        val fileType = state.currentFileType.lowercase(Locale.getDefault())
        val hasOpenFile = state.currentFileId > 0L || state.currentFileContent.isNotBlank()
        if (!hasOpenFile) {
            outputText.gravity = Gravity.CENTER
            outputText.text = "NO FILE SELECTED"
            return
        }

        outputText.gravity = Gravity.START or Gravity.TOP
        when (fileType) {
            "md", "markdown" -> markwon.setMarkdown(
                outputText,
                state.currentFileContent.ifBlank { "NO MARKDOWN CONTENT TO PREVIEW" }
            )
            "kt", "kts" -> outputText.text = "KOTLIN RUN OUTPUT\n\n${state.currentFileContent.ifBlank { "NO CONTENT TO RUN" }}"
            else -> outputText.text = state.currentFileContent.ifBlank { "NO RUN OUTPUT TO SHOW YET" }
        }
    }
}
