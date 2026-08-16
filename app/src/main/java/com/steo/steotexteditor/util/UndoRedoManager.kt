package com.steo.steotexteditor.util

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.util.Stack

class UndoRedoManager(private val editText: EditText) {

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var isUndoingOrRedoing = false

    private val handler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null
    private var lastSnapshotText: String = editText.text.toString()

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (!isUndoingOrRedoing) {
                scheduleSnapshot(s.toString())
            }
        }
    }

    init {
        editText.addTextChangedListener(textWatcher)
    }

    private fun scheduleSnapshot(currentText: String) {
        debounceRunnable?.let { handler.removeCallbacks(it) }
        debounceRunnable = Runnable {
            if (currentText != lastSnapshotText) {
                pushToUndo(lastSnapshotText)
                lastSnapshotText = currentText
                redoStack.clear()
            }
        }
        handler.postDelayed(debounceRunnable!!, 500)
    }

    private fun pushToUndo(text: String) {
        if (undoStack.isEmpty() || undoStack.peek() != text) {
            undoStack.push(text)
            if (undoStack.size > 50) {
                undoStack.removeAt(0)
            }
        }
    }

    fun undo() {
        debounceRunnable?.let { 
            handler.removeCallbacks(it)
            it.run() // Commit pending snapshot before undoing
        }
        
        if (undoStack.isNotEmpty()) {
            isUndoingOrRedoing = true
            val currentText = editText.text.toString()
            redoStack.push(currentText)
            
            val previousText = undoStack.pop()
            editText.setText(previousText)
            editText.setSelection(previousText.length)
            lastSnapshotText = previousText
            isUndoingOrRedoing = false
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            isUndoingOrRedoing = true
            val currentText = editText.text.toString()
            undoStack.push(currentText)
            
            val nextText = redoStack.pop()
            editText.setText(nextText)
            editText.setSelection(nextText.length)
            lastSnapshotText = nextText
            isUndoingOrRedoing = false
        }
    }
    
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        lastSnapshotText = editText.text.toString()
    }
}
