package t3context

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

internal fun EditorReference.format(): String = formatReference(file, lineRange)

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

// T3 Code threads may run in another worktree or directory than the IDE project, so references stay absolute.
internal fun formatReference(file: Path, lineRange: LineRange?): String {
    val absolutePath = file.toAbsolutePath().normalize()

    return buildString {
        append('@')
        append(absolutePath.toString().replace('\\', '/'))
        lineRange?.let { range ->
            append("#L").append(range.start)
            if (range.end != range.start) {
                append('-').append(range.end)
            }
        }
        append(' ')
    }
}
