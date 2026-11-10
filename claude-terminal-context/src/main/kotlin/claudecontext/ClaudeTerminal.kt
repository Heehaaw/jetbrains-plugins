package claudecontext

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import com.intellij.terminal.frontend.view.TerminalView
import com.intellij.terminal.frontend.view.TerminalViewSessionState
import com.intellij.ui.content.Content
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.plugins.terminal.TerminalIcons
import org.jetbrains.plugins.terminal.session.TerminalStartupOptions
import org.jetbrains.plugins.terminal.startup.TerminalProcessType
import java.nio.file.InvalidPathException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

private const val TERMINAL_TOOL_WINDOW_ID = "Terminal"
private const val CLAUDE_AGENT_KEY = "claude"
private const val CLAUDE_TAB_NAME = "Claude"
private const val SESSION_START_TIMEOUT_MILLIS = 10_000L
private const val SESSION_POLL_INTERVAL_MILLIS = 100L
private val supportedClaudeExecutables = setOf("claude", "claude.exe", "claude.cmd")

internal class ClaudeTerminalSession(
    private val tab: TerminalToolWindowTab,
    val workingDirectory: Path,
) {
    val content: Content
        get() = tab.content

    fun insertReference(reference: String) {
        tab.view.createSendTextBuilder().useBracketedPasteMode().send(reference)
    }

    fun focus(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TERMINAL_TOOL_WINDOW_ID) ?: return
        if (toolWindow.contentManager.selectedContent !== tab.content) {
            toolWindow.contentManager.setSelectedContent(tab.content, true)
        }

        toolWindow.activate({ tab.view.preferredFocusableComponent.requestFocusInWindow() }, true)
    }
}

internal fun findLiveClaudeSessions(project: Project): List<ClaudeTerminalSession> =
    TerminalToolWindowTabsManager.getInstance(project).tabs.mapNotNull { tab -> createClaudeSession(project, tab) }

internal fun launchClaudeTerminal(project: Project): Boolean {
    val executable = findClaudeExecutable() ?: return false
    val tab = TerminalToolWindowTabsManager.getInstance(project)
        .createTabBuilder()
        .shellCommand(listOf(executable))
        .processType(TerminalProcessType.NON_SHELL)
        .tabName(CLAUDE_TAB_NAME)
        .requestFocus(true)
        // This dedicated tab has no shell to return to after Claude exits.
        .closeOnProcessTermination(true)
        .apply {
            project.basePath?.let(::workingDirectory)
        }
        .createTab()

    tab.content.icon = TerminalIcons.Agents.ClaudeCode
    return true
}

internal suspend fun awaitLiveClaudeSession(project: Project): ClaudeTerminalSession? =
    withTimeoutOrNull(SESSION_START_TIMEOUT_MILLIS) {
        var session: ClaudeTerminalSession?
        do {
            session = findLiveClaudeSessions(project).firstOrNull()

            // Terminal process startup completes asynchronously after the tab is created.
            if (session == null) {
                delay(SESSION_POLL_INTERVAL_MILLIS)
            }
        } while (session == null)

        session
    }

private fun findClaudeExecutable(): String? {
    PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(CLAUDE_AGENT_KEY)?.let { executable ->
        return executable.absolutePath
    }

    val knownLocation = Path.of(System.getProperty("user.home"), ".local", "bin", CLAUDE_AGENT_KEY)
    return knownLocation.takeIf { path -> Files.isRegularFile(path) && Files.isExecutable(path) }?.toString()
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun createClaudeSession(project: Project, tab: TerminalToolWindowTab): ClaudeTerminalSession? {
    val view = tab.view
    if (view.sessionState.value !is TerminalViewSessionState.Running) {
        return null
    }

    val startupOptions = view.startupOptionsDeferred
    if (!startupOptions.isCompleted || startupOptions.isCancelled) {
        return null
    }

    return try {
        val options = startupOptions.getCompleted()
        if (!isClaudeCommand(options.shellCommand)) {
            null
        } else {
            ClaudeTerminalSession(tab, resolveWorkingDirectory(project, view, options))
        }
    } catch (_: IllegalStateException) {
        null
    }
}

private fun resolveWorkingDirectory(
    project: Project,
    view: TerminalView,
    startupOptions: TerminalStartupOptions,
): Path = normalizedPath(view.getCurrentDirectory())
    ?: normalizedPath(startupOptions.workingDirectory)
    ?: normalizedPath(project.basePath)
    ?: Path.of(".").toAbsolutePath().normalize()

private fun normalizedPath(value: String?): Path? {
    if (value.isNullOrBlank()) {
        return null
    }

    return try {
        Path.of(value).toAbsolutePath().normalize()
    } catch (_: InvalidPathException) {
        null
    }
}

internal fun isClaudeCommand(command: List<String>?): Boolean {
    val executable = command
        ?.firstOrNull()
        ?.replace('\\', '/')
        ?.substringAfterLast('/')
        ?.lowercase(Locale.ROOT)
        ?: return false

    return executable in supportedClaudeExecutables
}
