# Undo/Redo and Search/Replace Implementation

This plan details the steps to implement Undo/Redo and Search/Replace functionality, including UI updates, new layouts, and logic implementation in `EditorFragment`.

## User Review Required

> [!IMPORTANT]
> The Undo/Redo logic for devices below API 23 will use a custom `UndoRedoManager` with a 500ms debounce. For API 23+, we will leverage the native `EditText.undo()` and `EditText.redo()` methods.

## Proposed Changes

### Resources

#### [NEW] [ic_baseline_undo_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_undo_24.xml)
#### [NEW] [ic_baseline_redo_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_redo_24.xml)
#### [NEW] [ic_baseline_search_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_search_24.xml)
#### [NEW] [ic_baseline_chevron_left_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_chevron_left_24.xml) (Previous)
#### [NEW] [ic_baseline_chevron_right_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_chevron_right_24.xml) (Next)
#### [NEW] [ic_baseline_replace_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_replace_24.xml) (Replace Current)
#### [NEW] [ic_baseline_replace_all_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_replace_all_24.xml) (Replace All)

#### [MODIFY] [editor_menu.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/menu/editor_menu.xml)
* Add Undo, Redo, and Search menu items.

#### [NEW] [layout_search_bar.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/layout/layout_search_bar.xml)
* Implement the search and replace UI with the specified styling (#1A1A2E background, #E8E8F0 text).

#### [MODIFY] [fragment_editor.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/layout/fragment_editor.xml)
* Include the search bar layout below the toolbar.

### Logic

#### [NEW] [UndoRedoManager.kt](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/java/com/steo/steotexteditor/util/UndoRedoManager.kt)
* Implement stack-based undo/redo with debouncing.

#### [MODIFY] [EditorFragment.kt](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/java/com/steo/steotexteditor/ui/EditorFragment.kt)
* Integrate Undo/Redo logic (native vs custom manager).
* Implement Search logic using `Spannable` and `BackgroundColorSpan`.
* Implement Navigation between matches.
* Implement Replace and Replace All logic.
* Add animations for search bar visibility.

## Verification Plan

### Automated Tests
* N/A (UI and interaction focused)

### Manual Verification
* Verify Undo/Redo works via toolbar buttons.
* Verify Search highlights all matches and navigation works.
* Verify Replace and Replace All correctly update the text.
* Verify UI matches the dark theme specification.
