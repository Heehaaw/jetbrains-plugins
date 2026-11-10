package claudecontext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaudeCommandMatcherTest {
    @Test
    fun Should_recognizeClaudeSession_When_commandUsesSupportedExecutable() {
        assertTrue(isClaudeCommand(listOf("/Users/example/.local/bin/claude")))
        assertTrue(isClaudeCommand(listOf("C:\\Tools\\claude.exe", "--continue")))
        assertTrue(isClaudeCommand(listOf("claude.cmd")))
    }

    @Test
    fun Should_rejectTerminalSession_When_commandIsNotClaude() {
        assertFalse(isClaudeCommand(listOf("codex")))
        assertFalse(isClaudeCommand(listOf("/bin/zsh", "-c", "claude")))
        assertFalse(isClaudeCommand(null))
        assertFalse(isClaudeCommand(emptyList()))
    }
}
