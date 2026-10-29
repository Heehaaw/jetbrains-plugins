package codexcontext

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class ReferenceFormatterTest {
    @Test
    fun Should_formatRelativeFileReference_When_fileIsInsideWorkingDirectory() {
        val reference = formatReference(
            workingDirectory = Path.of("/workspace/project"),
            file = Path.of("/workspace/project/src/Example.cs"),
            lineRange = null,
        )

        assertEquals("src/Example.cs ", reference)
    }

    @Test
    fun Should_formatSelectedLineRange_When_selectionIsPresent() {
        val reference = formatReference(
            workingDirectory = Path.of("/workspace/project"),
            file = Path.of("/workspace/project/src/Example.cs"),
            lineRange = LineRange(12, 18),
        )

        assertEquals("src/Example.cs:12-18 ", reference)
    }

    @Test
    fun Should_useAbsolutePath_When_fileIsOutsideWorkingDirectory() {
        val reference = formatReference(
            workingDirectory = Path.of("/workspace/project"),
            file = Path.of("/workspace/shared/Example.cs"),
            lineRange = LineRange(7, 7),
        )

        assertEquals("/workspace/shared/Example.cs:7 ", reference)
    }
}
