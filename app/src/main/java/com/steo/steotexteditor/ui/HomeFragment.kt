package com.steo.steotexteditor.ui

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        viewModel = ViewModelProvider(requireActivity()).get(EditorViewModel::class.java)

        val rv = root.findViewById<RecyclerView>(R.id.rvRecentFiles)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecentFilesAdapter(emptyList()) { file ->
            val bundle = Bundle().apply {
                putLong("file_id", file.id)
            }
            findNavController().navigate(R.id.nav_edit, bundle)
        }
        rv.adapter = adapter

        viewModel.recentFiles.observe(viewLifecycleOwner) { files ->
            adapter.submitList(files)
        }

        root.findViewById<View>(R.id.cardNewFile).setOnClickListener {
            // Navigate to editor with a "new file" signal or just navigate
            // If EditorFragment checks for file_id, passing -1 might indicate new
            val bundle = Bundle().apply {
                putLong("file_id", -1L)
            }
            findNavController().navigate(R.id.nav_edit, bundle)
        }

        root.findViewById<View>(R.id.cardNewProject).setOnClickListener {
            // Project functionality placeholder
        }

        updateGreeting(root)

        return root
    }

    private fun updateGreeting(root: View) {
        val prefs = requireContext().getSharedPreferences("steo_prefs", Context.MODE_PRIVATE)
        val coderName = prefs.getString("coder_name", "LEO")
        val greetingTv = root.findViewById<TextView>(R.id.tvGreeting)
        greetingTv?.text = "STARTING SOMETHING NEW TODAY, ${coderName?.uppercase()}?"
    }
}
