package com.steo.steotexteditor.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val lineNumbersPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5A5A7A")
        textSize = 35f
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create("Consolas", Typeface.NORMAL)
    }

    private val rect = Rect()
    private val paddingLeftValue = 70 // Reduced space for line numbers

    init {
        // Add left padding to make room for line numbers
        setPadding(paddingLeftValue, paddingTop, paddingRight, paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        val lineCount = lineCount
        val lineNumbersSpacing = 15 // Slightly reduced spacing

        for (i in 0 until lineCount) {
            val baseline = getLineBounds(i, rect)
            val lineNumber = (i + 1).toString()
            canvas.drawText(
                lineNumber,
                (paddingLeftValue - lineNumbersSpacing).toFloat(),
                baseline.toFloat(),
                lineNumbersPaint
            )
        }
        super.onDraw(canvas)
    }
}
