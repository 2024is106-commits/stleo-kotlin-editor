package com.steo.steotexteditor.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.steo.steotexteditor.R

class HomeFragment : Fragment() {

    private lateinit var viewModel: EditorViewModel
    private lateinit var adapter: RecentFilesAdapter
    private lateinit var rvRecentFiles: RecyclerView
    private lateinit var emptyRecentFiles: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        viewModel = ViewModelProvider(requireActivity()).get(EditorViewModel::class.java)

        rvRecentFiles = root.findViewById(R.id.rvRecentFiles)
        emptyRecentFiles = root.findViewById(R.id.tvNoRecentFiles)
        rvRecentFiles.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecentFilesAdapter(emptyList()) { file ->
            val bundle = Bundle().apply {
                putLong("file_id", file.id)
            }
            findNavController().navigate(R.id.nav_edit, bundle)
        }
        rvRecentFiles.adapter = adapter

        viewModel.recentFiles.observe(viewLifecycleOwner) { files ->
            adapter.submitList(files)
            val isEmpty = files.isNullOrEmpty()
            rvRecentFiles.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptyRecentFiles.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        root.findViewById<View>(R.id.cardNewFile).setOnClickListener {
            showFileTypeChooser()
        }

        root.findViewById<View>(R.id.cardNewProject).setOnClickListener {
            showFileTypeChooser()
        }

        updateHeadline(root)

        return root
    }

    private fun updateHeadline(root: View) {
        val prefs = requireContext().getSharedPreferences("steo_prefs", Context.MODE_PRIVATE)
        val coderName = prefs.getString("coder_name", "LEO")
        val headline = root.findViewById<TextView>(R.id.tvHomeHeadline)
        headline?.text = "Ready to write your first line of code ${coderName?.uppercase()}?"
    }

    fun refreshCoderName() {
        view?.let { updateHeadline(it) }
    }

    private fun showFileTypeChooser() {
        val labels = arrayOf("Markdown file (.md)", "Kotlin file (.kt)", "Plain text file (.txt)")
        val extensions = arrayOf("md", "kt", "txt")
        val title = TextView(requireContext()).apply {
            text = "What file are you creating?"
            setTextColor(Color.BLACK)
            textSize = 20f
            typeface = try {
                ResourcesCompat.getFont(requireContext(), R.font.silkscreen) ?: Typeface.DEFAULT_BOLD
            } catch (_: Exception) {
                Typeface.DEFAULT_BOLD
            }
            setPadding(48, 36, 48, 12)
        }
        AlertDialog.Builder(requireContext())
            .setCustomTitle(title)
            .setItems(labels) { _, which ->
                val bundle = Bundle().apply {
                    putLong("file_id", -1L)
                    putString("file_extension", extensions[which])
                }
                findNavController().navigate(R.id.nav_edit, bundle)
            }
            .show()
    }
}
