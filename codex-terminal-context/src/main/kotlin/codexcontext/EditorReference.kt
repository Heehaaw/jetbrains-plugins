package codexcontext

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

internal data class LineRange(val start: Int, val end: Int) {
    init {
        require(start > 0) { "Start line must be positive" }
        require(end >= start) { "End line must not precede start line" }
    }
}

internal data class EditorReference(val file: Path, val lineRange: LineRange?)

internal fun captureEditorReference(editor: Editor, file: VirtualFile): EditorReference {
    val selection = editor.selectionModel
    val lineRange = if (selection.hasSelection()) {
        selectionLineRange(
            selectionStart = selection.selectionStart,
            selectionEnd = selection.selectionEnd,
            zeroBasedLineAtOffset = editor.document::getLineNumber,
        )
    } else {
        null
    }

    return EditorReference(Path.of(file.path), lineRange)
}

internal fun EditorReference.format(workingDirectory: Path): String =
    formatReference(workingDirectory, file, lineRange)

internal fun selectionLineRange(
    selectionStart: Int,
    selectionEnd: Int,
    zeroBasedLineAtOffset: (Int) -> Int,
): LineRange {
    require(selectionEnd > selectionStart) { "Selection must not be empty" }

    val startLine = zeroBasedLineAtOffset(selectionStart) + 1

    // Selection end offsets are exclusive, including when the caret is at the next line's start.
    val endLine = zeroBasedLineAtOffset(selectionEnd - 1) + 1
    return LineRange(startLine, endLine)
}

internal fun formatReference(workingDirectory: Path, file: Path, lineRange: LineRange?): String {
    val normalizedWorkingDirectory = workingDirectory.toAbsolutePath().normalize()
    val normalizedFile = file.toAbsolutePath().normalize()
    val displayedPath = if (normalizedFile.startsWith(normalizedWorkingDirectory)) {
        normalizedWorkingDirectory.relativize(normalizedFile)
    } else {
        normalizedFile
    }

    return buildString {
        append(displayedPath.toString().replace('\\', '/'))
        lineRange?.let { range ->
            append(':').append(range.start)
            if (range.end != range.start) {
                append('-').append(range.end)
            }
        }
        append(' ')
    }
}
