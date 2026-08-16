# Fix App Crash and Implement Undo/Redo & Search/Replace

The app currently crashes at launch due to a `NullPointerException` in `EditorFragment` when accessing the search bar views. This is because the `<include>` tag in the layout overrides the IDs of the included views. Additionally, there are duplicate `UndoRedoManager` classes and some vector drawables have incorrect viewport coordinates.

## Proposed Changes

### [UI Layouts]

#### [MODIFY] [fragment_editor.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/layout/fragment_editor.xml)
- Remove `android:id="@+id/searchBarLayout"` from the `<include>` tag for `layout_search_bar.xml`. This ensures the `android:id="@+id/searchBarContainer"` in the included layout is preserved and can be found by `findViewById`.

### [Resources]

#### [MODIFY] [ic_baseline_undo_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_undo_24.xml)
- Replace with correct Material Design path for "Undo".

#### [MODIFY] [ic_baseline_redo_24.xml](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/res/drawable/ic_baseline_redo_24.xml)
- Replace with correct Material Design path for "Redo".

### [Code Refinement]

#### [DELETE] [UndoRedoManager.kt](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/java/com/steo/steotexteditor/ui/UndoRedoManager.kt)
- Remove the redundant class in the `ui` package. The version in `util` package will be kept and used.

#### [MODIFY] [EditorFragment.kt](file:///C:/Users/nn/OneDrive/Documents/Projects/Kotlin-text%20editor/steo-text-editor/app/src/main/java/com/steo/steotexteditor/ui/EditorFragment.kt)
- Ensure all search bar view lookups are safe.
- Fix the Search/Replace logic to correctly navigate and highlight matches.
- Integrate Undo/Redo menu actions with both native API 23+ support and the `UndoRedoManager` fallback.
- Add animation to search bar toggle.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew assembleDebug` to ensure no compilation errors.

### Manual Verification
- Deploy the app to the device.
- Verify the splash screen shows and transitions to the main editor without crashing.
- Test Search/Replace:
    - Type some text.
    - Search for a word (verify highlighting).
    - Use Next/Prev buttons.
    - Replace one instance and Replace All.
- Test Undo/Redo:
    - Type text, wait for debounce.
    - Undo and Redo using toolbar buttons.
