package com.steo.steotexteditor.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class LogsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val tv = TextView(requireContext())
        tv.text = "Logs Fragment Placeholder"
        tv.gravity = android.view.Gravity.CENTER
        return tv
    }
}
