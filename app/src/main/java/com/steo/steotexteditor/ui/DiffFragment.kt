package com.steo.steotexteditor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.steo.steotexteditor.R

class DiffFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = FrameLayout(requireContext())
        root.setBackgroundResource(R.drawable.starfield_background)
        val tv = TextView(requireContext())
        tv.text = "No diffs/logs to show yet"
        tv.setTextColor(resources.getColor(R.color.text_off_white, requireContext().theme))
        tv.typeface = resources.getFont(R.font.silkscreen)
        tv.gravity = android.view.Gravity.CENTER
        root.addView(tv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        return root
    }
}
