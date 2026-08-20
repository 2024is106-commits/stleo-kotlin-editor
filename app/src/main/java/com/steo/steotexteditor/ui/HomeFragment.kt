package com.steo.steotexteditor.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
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

        updateGreeting(root)

        return root
    }

    private fun updateGreeting(root: View) {
        val prefs = requireContext().getSharedPreferences("steo_prefs", Context.MODE_PRIVATE)
        val coderName = prefs.getString("coder_name", "LEO")
        val greetingTv = root.findViewById<TextView>(R.id.tvGreeting)
        greetingTv?.text = "STARTING SOMETHING NEW TODAY,\n${coderName?.uppercase()}?"
    }

    private fun showFileTypeChooser() {
        val labels = arrayOf("Markdown file (.md)", "Kotlin file (.kt)", "Plain text file (.txt)")
        val extensions = arrayOf("md", "kt", "txt")
        AlertDialog.Builder(requireContext())
            .setTitle("What file are you creating?")
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
