package codexcontext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexCommandMatcherTest {
    @Test
    fun Should_recognizeCodexSession_When_commandUsesSupportedExecutable() {
        assertTrue(isCodexCommand(listOf("/Users/example/.local/bin/codex")))
        assertTrue(isCodexCommand(listOf("C:\\Tools\\codex.exe", "--model", "gpt-5")))
        assertTrue(isCodexCommand(listOf("codex.cmd")))
    }

    @Test
    fun Should_rejectTerminalSession_When_commandIsNotCodex() {
        assertFalse(isCodexCommand(listOf("claude")))
        assertFalse(isCodexCommand(emptyList()))
    }
}
