package t3context

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class ReferenceFormatterTest {
    @Test
    fun Should_formatAbsoluteFileReference_When_noLinesAreSelected() {
        val file = Path.of("/workspace/project/src/Example.cs")

        val reference = formatReference(file, lineRange = null)

        assertEquals("@/workspace/project/src/Example.cs ", reference)
    }

    @Test
    fun Should_formatSelectedLineRange_When_selectionIsPresent() {
        val file = Path.of("/workspace/project/src/Example.cs")

        val reference = formatReference(file, LineRange(12, 18))

        assertEquals("@/workspace/project/src/Example.cs#L12-18 ", reference)
    }

    @Test
    fun Should_formatSingleLine_When_selectionCoversOneLine() {
        val file = Path.of("/workspace/shared/Example.cs")

        val reference = formatReference(file, LineRange(7, 7))

        assertEquals("@/workspace/shared/Example.cs#L7 ", reference)
    }

    @Test
    fun Should_normalizePath_When_pathContainsRelativeSegments() {
        val file = Path.of("/workspace/project/src/../lib/Example.cs")

        val reference = formatReference(file, lineRange = null)

        assertEquals("@/workspace/project/lib/Example.cs ", reference)
    }
}
