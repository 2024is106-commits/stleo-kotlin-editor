package com.steo.steotexteditor.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.widget.TextView as AndroidTextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.getSpans
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.steo.steotexteditor.R
import com.steo.steotexteditor.data.db.FileEntity
import com.steo.steotexteditor.databinding.FragmentEditorBinding
import com.steo.steotexteditor.util.FileHelper
import com.steo.steotexteditor.util.UndoRedoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.steo.steotexteditor.ui.RecentFilesAdapter
import java.util.Locale

import io.noties.markwon.Markwon

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EditorViewModel by viewModels({ requireActivity() })
    private var currentFile: FileEntity? = null
    private var currentFileId: Long = -1
    private var isDirty = false
    private val highlightHandler = Handler(Looper.getMainLooper())
    private var highlightRunnable: Runnable? = null
    private var highlightJob: Job? = null
    private val kotlinSyntaxHighlighter = KotlinSyntaxHighlighter()
    private val markdownSyntaxHighlighter = MarkdownSyntaxHighlighter()
    private var undoRedoManager: UndoRedoManager? = null
    private var isProgrammaticTextChange = false

    private lateinit var searchBarContainer: View
    private lateinit var searchInput: EditText
    private lateinit var replaceInput: EditText
    private lateinit var prevMatchButton: ImageButton
    private lateinit var nextMatchButton: ImageButton
    private lateinit var replaceCurrentButton: ImageButton
    private lateinit var replaceAllButton: ImageButton
    private lateinit var closeSearchButton: ImageButton
    private var isSearchVisible = false
    private val searchMatchRanges = mutableListOf<IntRange>()
    private var currentSearchMatchIndex = -1
    private var backPressCallback: OnBackPressedCallback? = null

    // Markdown preview support
    private var isPreviewMode = false
    private var currentFileExtension: String = ""
    private var pendingNewFileExtension: String = "kt"
    private lateinit var markwon: Markwon

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.openFile(uri) { file, content ->
                activity?.runOnUiThread {
                    if (file != null && content != null) {
                        currentFile = file
                        currentFileId = file.id
                        currentFileExtension = resolveFileType(file)
                        setEditorText(content)
                        isDirty = false
                        updateToolbarTitle()
                        viewModel.updateEditorSession(file, content, hasUnsavedChanges = false)
                        scheduleHighlighting()
                    } else {
                        Toast.makeText(requireContext(), "Failed to open file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get file ID from arguments
        currentFileId = arguments?.getLong("file_id") ?: -1
        pendingNewFileExtension = arguments?.getString("file_extension") ?: "txt"
        
        setupDrawer()
        setupSearchBar()
        setupBars()
        setupTextWatcher()
        setupBackPressHandling()
        observeEditorSession()
        undoRedoManager = UndoRedoManager(binding.editorView)
        // Initialize Markwon for Markdown preview (core only)
        markwon = Markwon.builder(requireContext()).build()

        loadFile()
    }

    private fun observeEditorSession() {
        viewModel.sessionState.observe(viewLifecycleOwner) { state ->
            val file = currentFile ?: return@observe
            if (state.currentFileId != file.id || state.currentFileContent == binding.editorView.text?.toString()) {
                return@observe
            }

            val scrollX = binding.editorView.scrollX
            val scrollY = binding.editorView.scrollY
            currentFileExtension = state.currentFileType
            setEditorText(state.currentFileContent)
            isDirty = state.hasUnsavedChanges
            updateToolbarTitle()
            scheduleHighlighting()
            binding.editorView.post {
                binding.editorView.scrollTo(scrollX, scrollY)
            }
        }
    }

    private fun showRecoveryDialog(draft: FileHelper.RecoveryDraft, onDiscard: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Recover Unsaved Work")
            .setMessage("It looks like the app closed unexpectedly while editing ${draft.fileName}. Would you like to restore or discard your unsaved content?")
            .setPositiveButton("Restore") { _, _ ->
                restoreDraft(draft)
                FileHelper.clearCrashRecovery(requireContext())
            }
            .setNegativeButton("Discard") { _, _ ->
                viewModel.discardRecoveryDraft()
                onDiscard()
            }
            .setCancelable(false)
            .show()
    }

    private fun restoreDraft(draft: FileHelper.RecoveryDraft) {
        val restoredFile = FileEntity(
            id = draft.fileId,
            name = draft.fileName,
            path = draft.path,
            fileType = draft.fileType,
            lastModified = System.currentTimeMillis(),
            isReadOnly = false
        )
        currentFile = restoredFile
        currentFileId = restoredFile.id
        currentFileExtension = restoredFile.fileType
        setEditorText(draft.content)
        isDirty = true
        updateToolbarTitle()
        scheduleHighlighting()
        viewModel.updateEditorSession(restoredFile, draft.content, hasUnsavedChanges = true)
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private fun setupDrawer() {
        drawerLayout = requireActivity().findViewById(R.id.drawerLayout)
        navigationView = requireActivity().findViewById(R.id.navigationView)

        // Ensure the header is inflated
        if (navigationView.headerCount == 0) return
        val header = navigationView.getHeaderView(0)
        val rv = header.findViewById<RecyclerView>(R.id.rvRecentFiles) ?: return
        val btnNewFile = header.findViewById<Button>(R.id.btnNewFile) ?: return
        val btnOpenFile = header.findViewById<Button>(R.id.btnOpenFile) ?: return
        val btnCloseDrawer = header.findViewById<ImageButton>(R.id.btnCloseDrawer)
        val tvEmpty = header.findViewById<TextView>(R.id.tvEmptyRecent) ?: return

        val adapter = RecentFilesAdapter(emptyList()) { file ->
            // Click to open file
            viewModel.loadFile(file.id) { f, content ->
                activity?.runOnUiThread {
                    if (f != null) {
                        currentFile = f
                        currentFileId = f.id
                        currentFileExtension = resolveFileType(f)
                        setEditorText(content ?: "")
                        isDirty = false
                        updateToolbarTitle()
                        viewModel.updateEditorSession(f, content.orEmpty(), hasUnsavedChanges = false)
                        viewModel.setCurrentFile(f)
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                }
            }
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        viewModel.recentFiles.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rv.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rv.visibility = View.VISIBLE
                adapter.submitList(list)
            }
        }

        btnNewFile.setOnClickListener {
            newFile()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        btnOpenFile.setOnClickListener {
            openFile()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        btnCloseDrawer?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupSearchBar() {
        val root = binding.root
        
        // Use findViewById since we removed the ID from the include tag to avoid ID override
        val container = root.findViewById<View>(R.id.searchBarContainer) ?: return
        searchBarContainer = container
        searchInput = root.findViewById(R.id.etSearch) ?: return
        replaceInput = root.findViewById(R.id.etReplace) ?: return
        prevMatchButton = root.findViewById(R.id.btnPrev) ?: return
        nextMatchButton = root.findViewById(R.id.btnNext) ?: return
        replaceCurrentButton = root.findViewById(R.id.btnReplace) ?: return
        replaceAllButton = root.findViewById(R.id.btnReplaceAll) ?: return
        closeSearchButton = root.findViewById(R.id.btnCloseSearch) ?: return

        searchBarContainer.visibility = View.GONE

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                refreshSearchMatches()
            }
        })

        prevMatchButton.setOnClickListener { moveToPreviousMatch() }
        nextMatchButton.setOnClickListener { moveToNextMatch() }
        replaceCurrentButton.setOnClickListener { replaceCurrentMatch() }
        replaceAllButton.setOnClickListener { replaceAllMatches() }
        closeSearchButton.setOnClickListener { hideSearchBar() }
    }

    private fun setupBackPressHandling() {
        backPressCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    isSearchVisible -> hideSearchBar()
                    ::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.START) ->
                        drawerLayout.closeDrawer(GravityCompat.START)
                    else -> {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback!!)
    }

    private fun setupBars() {
        binding.btnNavToggle.setOnClickListener {
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.nav_home)
        }
        
        binding.btnSave.setOnClickListener {
            saveFile()
        }

        binding.btnOverflow.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(requireContext(), binding.btnOverflow)
            popup.menuInflater.inflate(R.menu.editor_menu, popup.menu)

            // Show preview menu only for markdown files
            val previewItem = popup.menu.findItem(R.id.action_preview)
            previewItem?.isVisible = canPreviewMarkdown()
            // tint icon if currently in preview mode
            previewItem?.icon?.setTint(if (isPreviewMode) Color.parseColor("#7B2FBE") else Color.parseColor("#DCDCF0"))

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save_as -> { saveAsFile(); true }
                    R.id.action_versions -> {
                        showVersions(); true
                    }
                    R.id.action_preview -> {
                        togglePreview()
                        true
                    }
                    R.id.action_delete -> { deleteFile(); true }
                    else -> false
                }
            }
            popup.show()
        }
        
        binding.btnUndoQuick.setOnClickListener { performUndo() }
        binding.btnRedoQuick.setOnClickListener { performRedo() }
        binding.btnSearchQuick.setOnClickListener { toggleSearchBar() }
        binding.btnVersionsQuick.setOnClickListener { showVersions() }
        binding.btnLockQuick.setOnClickListener { 
            currentFile?.let {
                val newState = !it.isReadOnly
                binding.editorView.isEnabled = !newState
                val color = if (newState) Color.parseColor("#7B2FBE") else Color.parseColor("#5A5A7A")
                binding.btnLockQuick.setColorFilter(color)
                currentFile = it.copy(isReadOnly = newState)
                updateToolbarTitle()
            }
        }

        binding.etFileName.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                val newName = binding.etFileName.text.toString().removeSuffix("*").trim()
                if (newName.isNotEmpty() && currentFile != null) {
                    val previousName = currentFile!!.name
                    val isUnsavedFile = currentFile!!.id == 0L
                    currentFile = currentFile!!.copy(name = newName)
                    isDirty = true
                    updateToolbarTitle()
                    if (isUnsavedFile && previousName != newName) {
                        Toast.makeText(requireContext(), "File renamed", Toast.LENGTH_SHORT).show()
                        currentFile?.let { viewModel.recordActivity(it, "File renamed") }
                    }
                }
                v.clearFocus()
                true
            } else {
                false
            }
        }    }

    private fun setupTextWatcher() {
        binding.editorView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticTextChange) return
                if (!isDirty) {
                    isDirty = true
                    updateToolbarTitle()
                }
                viewModel.updateEditorSession(
                    file = currentFile,
                    content = s?.toString() ?: "",
                    hasUnsavedChanges = true
                )
                scheduleHighlighting()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        scheduleHighlighting()
    }

    private fun scheduleHighlighting() {
        highlightRunnable?.let { highlightHandler.removeCallbacks(it) }
        highlightRunnable = Runnable { applySyntaxHighlighting() }
        highlightHandler.postDelayed(highlightRunnable!!, 300)
    }

    private fun applySyntaxHighlighting() {
        val sourceText = binding.editorView.text?.toString() ?: return
        val syntaxType = detectSyntaxType()

        highlightJob?.cancel()
        highlightJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val spans = when (syntaxType) {
                SyntaxType.MARKDOWN -> markdownSyntaxHighlighter.buildSpans(sourceText)
                SyntaxType.KOTLIN -> kotlinSyntaxHighlighter.buildSpans(sourceText)
                SyntaxType.PLAIN_TEXT -> emptyList()
            }

            withContext(Dispatchers.Main) {  
                if (_binding == null) return@withContext
                val editable = binding.editorView.text ?: return@withContext

                if (editable.toString() != sourceText) {
                    scheduleHighlighting()
                    return@withContext
                }

                clearHighlightSpans(editable)
                applyHighlightSpans(editable, spans)
            }
        }
    }

    private fun detectSyntaxType(): SyntaxType {
        val fileName = currentFile?.name?.lowercase().orEmpty()
        return when {
            fileName.endsWith(".md") -> SyntaxType.MARKDOWN
            fileName.endsWith(".txt") -> SyntaxType.PLAIN_TEXT
            fileName.isBlank() || !fileName.contains('.') -> SyntaxType.KOTLIN
            fileName.endsWith(".kt") -> SyntaxType.KOTLIN
            else -> SyntaxType.KOTLIN
        }
    }

    private fun clearHighlightSpans(editable: Editable) {
        editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
            .forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, StyleSpan::class.java)
            .forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, BackgroundColorSpan::class.java)
            .forEach { editable.removeSpan(it) }
        editable.getSpans(0, editable.length, TypefaceSpan::class.java)
            .forEach { editable.removeSpan(it) }
    }

    private fun applyHighlightSpans(editable: Editable, spans: List<SyntaxHighlightSpan>) {
        spans.forEach { span ->
            if (span.start < 0 || span.end > editable.length || span.start >= span.end) {
                return@forEach
            }

            span.foregroundColor?.let {
                editable.setSpan(
                    ForegroundColorSpan(it),
                    span.start,
                    span.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            span.style?.let {
                editable.setSpan(
                    StyleSpan(it),
                    span.start,
                    span.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            span.backgroundColor?.let {
                editable.setSpan(
                    BackgroundColorSpan(it),
                    span.start,
                    span.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            if (span.monospace) {
                editable.setSpan(
                    TypefaceSpan("Consolas"),
                    span.start,
                    span.end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun updateToolbarTitle() {
        val title = currentFile?.name ?: "Untitled"
        val displayTitle = if (isDirty) "$title*" else title
        if (binding.etFileName.text.toString() != displayTitle) {
            binding.etFileName.setText(displayTitle)
        }
        binding.statusDot.visibility = if (isDirty) View.VISIBLE else View.INVISIBLE
        viewModel.setCurrentFile(currentFile)
    }

    private fun setEditorText(content: String) {
        isProgrammaticTextChange = true
        binding.editorView.setText(content)
        isProgrammaticTextChange = false
    }

    private fun resolveFileType(file: FileEntity?): String {
        val stateType = viewModel.sessionState.value?.currentFileType.orEmpty()
        val fileType = file?.fileType.orEmpty()
        val fileName = file?.name.orEmpty()
        return when {
            fileType.isNotBlank() -> fileType.lowercase(Locale.getDefault())
            fileName.contains('.') -> fileName.substringAfterLast('.', "txt").lowercase(Locale.getDefault())
            stateType.isNotBlank() -> stateType.lowercase(Locale.getDefault())
            currentFileExtension.isNotBlank() -> currentFileExtension.lowercase(Locale.getDefault())
            else -> "txt"
        }
    }

    private fun canPreviewMarkdown(): Boolean = resolveFileType(currentFile) == "md"

    private fun loadFile() {
        if (currentFileId == -1L) {
            viewModel.consumeRecoveryDraftIfNeeded()?.let { draft ->
                showRecoveryDialog(draft) {
                    showFileTypeChooser { clearEditor(it) }
                }
                return
            }

            val activeFile = viewModel.currentFile.value
            if (activeFile != null) {
                resumeEditorSession(activeFile)
                return
            }

            if (arguments?.containsKey("file_extension") != true) {
                showFileTypeChooser { clearEditor(it) }
                return
            }

            // New file
            currentFile = FileEntity(
                id = 0,
                name = defaultUntitledName(pendingNewFileExtension),
                path = "",
                fileType = pendingNewFileExtension,
                lastModified = System.currentTimeMillis(),
                isReadOnly = false
            )
            setEditorText("")
            currentFileExtension = pendingNewFileExtension
            updateToolbarTitle()
            viewModel.updateEditorSession(currentFile, "", hasUnsavedChanges = false)
            scheduleHighlighting()
            return
        }
        
        viewModel.loadFile(currentFileId) { file, content ->
            activity?.runOnUiThread {
                if (file != null && content != null) {
                    currentFile = file
                    setEditorText(content)
                    isDirty = false
                    currentFileExtension = resolveFileType(file)
                    updateToolbarTitle()
                    viewModel.updateEditorSession(file, content, hasUnsavedChanges = false)
                } else {
                    Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
        }
    }

    private fun newFile() {
        if (isDirty) {
            showUnsavedChangesDialog(object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> saveFile()
                        DialogInterface.BUTTON_NEGATIVE -> {
                            showFileTypeChooser { clearEditor(it) }
                        }
                        DialogInterface.BUTTON_NEUTRAL -> {
                            // Cancel
                        }
                    }
                }
            })
        } else {
            showFileTypeChooser { clearEditor(it) }
        }
    }

    private fun resumeEditorSession(file: FileEntity) {
        currentFile = file
        currentFileId = file.id
        currentFileExtension = resolveFileType(file)

        if (file.id > 0L) {
            val session = viewModel.sessionState.value
            val inMemoryContent = session?.currentFileContent
            if (inMemoryContent != null) {
                setEditorText(inMemoryContent)
                isDirty = session.hasUnsavedChanges
                updateToolbarTitle()
                scheduleHighlighting()
                return
            }

            viewModel.loadFile(file.id) { loadedFile, content ->
                activity?.runOnUiThread {
                    currentFile = loadedFile ?: file
                    currentFileExtension = resolveFileType(currentFile)
                    val restoredContent = content ?: viewModel.sessionState.value?.currentFileContent.orEmpty()
                    setEditorText(restoredContent)
                    isDirty = false
                    updateToolbarTitle()
                    viewModel.updateEditorSession(currentFile, restoredContent, hasUnsavedChanges = false)
                    scheduleHighlighting()
                }
            }
        } else {
            val session = viewModel.sessionState.value
            setEditorText(session?.currentFileContent.orEmpty())
            isDirty = session?.hasUnsavedChanges == true
            updateToolbarTitle()
            scheduleHighlighting()
        }
    }

    private fun clearEditor(extension: String = pendingNewFileExtension) {
        pendingNewFileExtension = extension
        setEditorText("")
        currentFile = FileEntity(
            id = 0,
            name = defaultUntitledName(extension),
            path = "",
            fileType = extension,
            lastModified = System.currentTimeMillis(),
            isReadOnly = false
        )
        currentFileId = -1
        currentFileExtension = extension
        isDirty = false
        updateToolbarTitle()
        viewModel.updateEditorSession(currentFile, "", hasUnsavedChanges = false)
        scheduleHighlighting()
    }

    private fun showFileTypeChooser(onSelected: (String) -> Unit) {
        val labels = arrayOf("Markdown file (.md)", "Kotlin file (.kt)", "Plain text file (.txt)")
        val extensions = arrayOf("md", "kt", "txt")
        val title = android.widget.TextView(requireContext()).apply {
            text = "What file are you creating?"
            setTextColor(Color.BLACK)
            textSize = 14f
            typeface = try {
                ResourcesCompat.getFont(requireContext(), R.font.silkscreen) ?: Typeface.DEFAULT
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
            setPadding(48, 36, 48, 12)
        }
        AlertDialog.Builder(requireContext())
            .setCustomTitle(title)
            .setItems(labels) { _, which -> onSelected(extensions[which]) }
            .setOnCancelListener { onSelected("txt") }
            .show()
    }

    private fun defaultUntitledName(extension: String): String = "Untitled.${extension.ifBlank { "txt" }}"

    private fun loadSilkscreen(): Typeface {
        return try {
            ResourcesCompat.getFont(requireContext(), R.font.silkscreen) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun showUnsavedChangesDialog(listener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save before continuing?")
            .setPositiveButton("Save", listener)
            .setNegativeButton("Don't Save", listener)
            .setNeutralButton("Cancel", listener)
            .show()
    }

    private fun openFile() {
        if (isDirty) {
            showUnsavedChangesDialog(object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            saveFile()
                            openFileLauncher.launch("*/*")
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            openFileLauncher.launch("*/*")
                        }
                        DialogInterface.BUTTON_NEUTRAL -> {
                            // Cancel
                        }
                    }
                }
            })
        } else {
            openFileLauncher.launch("*/*")
        }
    }

    private fun saveFile() {
        val content = binding.editorView.text.toString()
        val file = currentFile ?: return
        
        if (file.id == 0L) {
            // New file - prompt for filename
            saveAsFile()
        } else {
            // Existing file - save with version
            viewModel.saveFile(file, content) { fileId ->
                activity?.runOnUiThread {
                    currentFileId = fileId
                    isDirty = false
                    updateToolbarTitle()
                    FileHelper.clearCrashRecovery(requireContext())
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveAsFile() {
        val content = binding.editorView.text.toString()
        
        val input = EditText(requireContext())
        input.setText(currentFile?.name ?: defaultUntitledName(currentFileExtension))
        input.typeface = loadSilkscreen()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Save As")
            .setMessage("Enter file name:")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                var fileName = input.text.toString()
                if (fileName.isNotEmpty()) {
                    // If no extension, keep the selected file type.
                    if (!fileName.contains(".")) {
                        fileName += ".${currentFileExtension.ifBlank { "txt" }}"
                    }

                    viewModel.createNewFile(fileName, content) { fileId ->
                        activity?.runOnUiThread {
                            currentFileId = fileId
                            currentFileExtension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
                            isDirty = false
                            updateToolbarTitle()
                            viewModel.loadFile(fileId) { f, _ ->
                                activity?.runOnUiThread {
                                    currentFile = f
                                    updateToolbarTitle()
                                    scheduleHighlighting()
                                }
                            }
                            FileHelper.clearCrashRecovery(requireContext())
                            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVersions() {
        val file = currentFile ?: return
        // Navigate to VersionsFragment passing file id
        val bundle = android.os.Bundle()
        bundle.putLong("file_id", file.id)
        try {
            val navController = androidx.navigation.Navigation.findNavController(requireView())
            navController.navigate(R.id.versionsFragment, bundle)
        } catch (e: Exception) {
            // fallback: toast
            Toast.makeText(requireContext(), "Unable to open versions", Toast.LENGTH_SHORT).show()
        }
    }

    // Called from MainActivity when bottom Run button is pressed
    fun handleRunAction() {
        val content = binding.editorView.text.toString()
        // If file not saved yet, ask for name and save as .md
        if (currentFile == null || currentFile?.id == 0L) {
            val input = EditText(requireContext())
            input.setText(currentFile?.name ?: "untitled.md")
            input.typeface = loadSilkscreen()
            AlertDialog.Builder(requireContext())
                .setTitle("Save file before running")
                .setMessage("Enter filename (will be saved as Markdown .md):")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    var name = input.text.toString().ifBlank { "untitled.md" }
                    if (!name.endsWith(".md")) name += ".md"
                    viewModel.createNewFile(name, content) { fileId ->
                        activity?.runOnUiThread {
                            currentFileId = fileId
                            viewModel.loadFile(fileId) { f, c ->
                                activity?.runOnUiThread {
                                    if (f != null) {
                                        currentFile = f
                                        currentFileExtension = resolveFileType(f)
                                        setEditorText(c ?: "")
                                        isDirty = false
                                        updateToolbarTitle()
                                        viewModel.updateEditorSession(f, c.orEmpty(), hasUnsavedChanges = false)
                                        // show preview
                                        if (canPreviewMarkdown()) {
                                            if (!isPreviewMode) togglePreview()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // If file exists but has unsaved changes, prompt to save
        if (isDirty) {
            AlertDialog.Builder(requireContext())
                .setTitle("Save changes?")
                .setMessage("Save changes before running preview?")
                .setPositiveButton("Save") { _, _ ->
                    // call viewModel.saveFile directly so we can act on completion
                    val file = currentFile ?: return@setPositiveButton
                    val contentToSave = binding.editorView.text.toString()
                    viewModel.saveFile(file, contentToSave) { fileId ->
                        activity?.runOnUiThread {
                            currentFileId = fileId
                            // reload file metadata
                            viewModel.loadFile(fileId) { f, c ->
                                activity?.runOnUiThread {
                                    if (f != null) {
                                        currentFile = f
                                        currentFileExtension = resolveFileType(f)
                                        setEditorText(c ?: "")
                                        isDirty = false
                                        updateToolbarTitle()
                                        viewModel.updateEditorSession(f, c.orEmpty(), hasUnsavedChanges = false)
                                        if (canPreviewMarkdown()) {
                                            if (!isPreviewMode) togglePreview()
                                        } else {
                                            Toast.makeText(requireContext(), "Preview available only for Markdown files", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // No unsaved changes; just show preview if file is Markdown
        if (canPreviewMarkdown()) {
            if (!isPreviewMode) togglePreview()
        } else {
            Toast.makeText(requireContext(), "Preview available only for Markdown files", Toast.LENGTH_SHORT).show()
        }
    }

    // Make togglePreview public so MainActivity can trigger preview display
    fun togglePreview() {
        isPreviewMode = !isPreviewMode
        val editor = binding.editorView
        val previewScroll = binding.root.findViewById<android.widget.ScrollView>(R.id.scrollPreview)
        val previewTv = binding.root.findViewById<TextView>(R.id.previewTextView)

        if (isPreviewMode) {
            // switch to preview
            editor.visibility = View.GONE
            previewScroll.visibility = View.VISIBLE
            updatePreview()
        } else {
            // back to edit
            previewScroll.visibility = View.GONE
            editor.visibility = View.VISIBLE
        }
    }

    private fun updatePreview() {
        val previewTv = binding.root.findViewById<TextView>(R.id.previewTextView)
        val content = binding.editorView.text?.toString() ?: ""
        // Render markdown into previewTextView
        try {
            markwon.setMarkdown(previewTv, content)
        } catch (_: Exception) {
            previewTv.text = content
        }
    }

    private fun deleteFile() {
        val file = currentFile ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete File")
            .setMessage("Are you sure you want to delete ${file.name}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFile(file) {
                    Toast.makeText(requireContext(), "File deleted", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performUndo() {
        try {
            if (!binding.editorView.onTextContextMenuItem(android.R.id.undo)) {
                undoRedoManager?.undo()
            }
        } catch (_: Exception) {
            undoRedoManager?.undo()
        }
    }

    private fun performRedo() {
        try {
            if (!binding.editorView.onTextContextMenuItem(android.R.id.redo)) {
                undoRedoManager?.redo()
            }
        } catch (_: Exception) {
            undoRedoManager?.redo()
        }
    }

    private fun toggleSearchBar() {
        if (isSearchVisible) {
            hideSearchBar()
        } else {
            showSearchBar()
        }
    }

    private fun showSearchBar() {
        isSearchVisible = true
        searchBarContainer.visibility = View.VISIBLE
        searchBarContainer.post {
            searchBarContainer.translationY = -searchBarContainer.height.toFloat()
            searchBarContainer.animate()
                .translationY(0f)
                .setDuration(300)
                .start()
        }
        searchInput.requestFocus()
    }

    private fun hideSearchBar() {
        isSearchVisible = false
        searchBarContainer.animate()
            .translationY(-searchBarContainer.height.toFloat())
            .setDuration(300)
            .withEndAction { 
                searchBarContainer.visibility = View.GONE 
                clearSearchHighlights()
            }
            .start()
        binding.editorView.requestFocus()
    }

    private fun refreshSearchMatches() {
        val query = searchInput.text.toString()
        val text = binding.editorView.text.toString()
        searchMatchRanges.clear()
        
        if (query.isNotEmpty()) {
            var index = text.indexOf(query, 0, true)
            while (index >= 0) {
                searchMatchRanges.add(index until (index + query.length))
                index = text.indexOf(query, index + 1, true)
            }
        }
        
        if (searchMatchRanges.isEmpty()) {
            currentSearchMatchIndex = -1
        } else if (currentSearchMatchIndex !in searchMatchRanges.indices) {
            currentSearchMatchIndex = 0
        }
        
        highlightMatches()
    }

    private fun highlightMatches() {
        val editable = binding.editorView.text ?: return
        clearSearchHighlights()

        if (searchMatchRanges.isEmpty()) return

        searchMatchRanges.forEachIndexed { index, range ->
            val color = if (index == currentSearchMatchIndex) {
                Color.parseColor("#5A5A8C") // Brighter for current match
            } else {
                Color.parseColor("#3A3A5C")
            }
            editable.setSpan(
                BackgroundColorSpan(color),
                range.first,
                range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (currentSearchMatchIndex != -1) {
            scrollToMatch(currentSearchMatchIndex)
        }
    }

    private fun clearSearchHighlights() {
        val editable = binding.editorView.text ?: return
        val spans = editable.getSpans(0, editable.length, BackgroundColorSpan::class.java)
        for (span in spans) {
            val color = span.backgroundColor
            if (color == Color.parseColor("#3A3A5C") || color == Color.parseColor("#5A5A8C")) {
                editable.removeSpan(span)
            }
        }
    }

    private fun scrollToMatch(index: Int) {
        if (index in searchMatchRanges.indices) {
            val range = searchMatchRanges[index]
            binding.editorView.setSelection(range.first, range.last + 1)
            // Optional: ensure it's visible if the editor is large
        }
    }

    private fun moveToPreviousMatch() {
        if (searchMatchRanges.isEmpty()) return
        currentSearchMatchIndex = if (currentSearchMatchIndex <= 0) {
            searchMatchRanges.size - 1
        } else {
            currentSearchMatchIndex - 1
        }
        scrollToMatch(currentSearchMatchIndex)
    }

    private fun moveToNextMatch() {
        if (searchMatchRanges.isEmpty()) return
        currentSearchMatchIndex = if (currentSearchMatchIndex >= searchMatchRanges.size - 1) {
            0
        } else {
            currentSearchMatchIndex + 1
        }
        scrollToMatch(currentSearchMatchIndex)
    }

    private fun replaceCurrentMatch() {
        if (currentSearchMatchIndex in searchMatchRanges.indices) {
            val range = searchMatchRanges[currentSearchMatchIndex]
            val replacement = replaceInput.text.toString()
            binding.editorView.text?.replace(range.first, range.last + 1, replacement)
            refreshSearchMatches()
        }
    }

    private fun replaceAllMatches() {
        val query = searchInput.text.toString()
        if (query.isEmpty()) return
        val replacement = replaceInput.text.toString()
        val text = binding.editorView.text.toString()
        val newText = text.replace(query, replacement, false) // ignoreCase false as requested
        binding.editorView.setText(newText)
        refreshSearchMatches()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        highlightRunnable?.let { highlightHandler.removeCallbacks(it) }
        highlightJob?.cancel()
        super.onDestroyView()
        _binding = null
    }

    private enum class SyntaxType {
        KOTLIN,
        MARKDOWN,
        PLAIN_TEXT
    }
}
