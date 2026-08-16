package com.steo.steotexteditor.ui

data class SyntaxHighlightSpan(
    val start: Int,
    val end: Int,
    val foregroundColor: Int? = null,
    val style: Int? = null,
    val backgroundColor: Int? = null,
    val monospace: Boolean = false
)

