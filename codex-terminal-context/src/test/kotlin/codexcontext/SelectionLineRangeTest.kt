package codexcontext

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionLineRangeTest {
    @Test
    fun Should_excludeNextLine_When_selectionEndsAtItsStart() {
        val text = "first\nsecond\nthird"

        val range = selectionLineRange(0, 13) { offset -> text.lineAtOffset(offset) }

        assertEquals(LineRange(1, 2), range)
    }

    @Test
    fun Should_useOneBasedLine_When_selectionIsOnSingleLine() {
        val text = "first\nsecond\nthird"

        val range = selectionLineRange(7, 10) { offset -> text.lineAtOffset(offset) }

        assertEquals(LineRange(2, 2), range)
    }
}

private fun String.lineAtOffset(offset: Int): Int = take(offset).count { character -> character == '\n' }
