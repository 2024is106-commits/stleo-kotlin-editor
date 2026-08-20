package com.steo.steotexteditor.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.steo.steotexteditor.R

class RunFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = FrameLayout(requireContext())
        root.setBackgroundResource(R.drawable.starfield_background)
        val tv = TextView(requireContext())
        tv.text = "No run output to show yet"
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.line_number_gray))
        tv.typeface = Typeface.MONOSPACE
        tv.gravity = android.view.Gravity.CENTER
        tv.textSize = 12f
        tv.letterSpacing = 0.08f
        val padding = (24 * resources.displayMetrics.density).toInt()
        tv.setPadding(padding, padding, padding, padding)
        root.addView(tv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        return root
    }
}
