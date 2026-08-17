package com.steo.steotexteditor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.steo.steotexteditor.R

class DiffBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_FILE_ID = "file_id"
        private const val ARG_VERSION = "version"

        fun newInstance(fileId: Long, version: Int): DiffBottomSheet {
            val b = Bundle()
            b.putLong(ARG_FILE_ID, fileId)
            b.putInt(ARG_VERSION, version)
            val f = DiffBottomSheet()
            f.arguments = b
            return f
        }
    }

    private var fileId: Long = -1
    private var version: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileId = arguments?.getLong(ARG_FILE_ID) ?: -1
        version = arguments?.getInt(ARG_VERSION) ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.sheet_diff, container, false)
        val tv = root.findViewById<TextView>(R.id.tvDiff)

        if (fileId == -1L || version == -1) {
            tv.text = "Invalid version"
            return root
        }

        // Obtain EditorViewModel from activity
        val vm = androidx.lifecycle.ViewModelProvider(requireActivity()).get(EditorViewModel::class.java)

        lifecycleScope.launch {
            // find latest version number
            val versions = vm.getVersionsForFile(fileId)
            val latest = versions.maxOfOrNull { it.versionNumber } ?: version
            val from = version
            val to = latest
            val diffLines = vm.getDiffBetweenVersions(fileId, from, to)
            val diffText = if (diffLines.isEmpty()) "No diff available" else diffLines.joinToString("\n")
            activity?.runOnUiThread {
                tv.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
                tv.text = diffText
            }
        }

        return root
    }
}
