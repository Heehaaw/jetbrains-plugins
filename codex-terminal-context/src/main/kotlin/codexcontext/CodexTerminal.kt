package codexcontext

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
private const val CODEX_AGENT_KEY = "codex"
private const val CODEX_TAB_NAME = "Codex"
private const val SESSION_START_TIMEOUT_MILLIS = 10_000L
private const val SESSION_POLL_INTERVAL_MILLIS = 100L
private val supportedCodexExecutables = setOf("codex", "codex.exe", "codex.cmd")

internal class CodexTerminalSession(
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

internal fun findLiveCodexSessions(project: Project): List<CodexTerminalSession> =
    TerminalToolWindowTabsManager.getInstance(project).tabs.mapNotNull { tab -> createCodexSession(project, tab) }

internal fun launchCodexTerminal(project: Project): Boolean {
    val executable = findCodexExecutable() ?: return false
    val tab = TerminalToolWindowTabsManager.getInstance(project)
        .createTabBuilder()
        // Invocation-level config overrides force embedded mode and block shared-server resumes.
        .shellCommand(listOf(executable))
        .processType(TerminalProcessType.NON_SHELL)
        .tabName(CODEX_TAB_NAME)
        .requestFocus(true)
        // This dedicated tab has no shell to return to after Codex exits.
        .closeOnProcessTermination(true)
        .apply {
            project.basePath?.let(::workingDirectory)
        }
        .createTab()

    tab.content.icon = TerminalIcons.Agents.Codex
    return true
}

internal suspend fun awaitLiveCodexSession(project: Project): CodexTerminalSession? =
    withTimeoutOrNull(SESSION_START_TIMEOUT_MILLIS) {
        var session: CodexTerminalSession?
        do {
            session = findLiveCodexSessions(project).firstOrNull()

            // Terminal process startup completes asynchronously after the tab is created.
            if (session == null) {
                delay(SESSION_POLL_INTERVAL_MILLIS)
            }
        } while (session == null)

        session
    }

private fun findCodexExecutable(): String? {
    PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(CODEX_AGENT_KEY)?.let { executable ->
        return executable.absolutePath
    }

    val knownLocation = Path.of(System.getProperty("user.home"), ".local", "bin", CODEX_AGENT_KEY)
    return knownLocation.takeIf { path -> Files.isRegularFile(path) && Files.isExecutable(path) }?.toString()
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun createCodexSession(project: Project, tab: TerminalToolWindowTab): CodexTerminalSession? {
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
        if (!isCodexCommand(options.shellCommand)) {
            null
        } else {
            CodexTerminalSession(tab, resolveWorkingDirectory(project, view, options))
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

internal fun isCodexCommand(command: List<String>?): Boolean {
    val executable = command
        ?.firstOrNull()
        ?.replace('\\', '/')
        ?.substringAfterLast('/')
        ?.lowercase(Locale.ROOT)
        ?: return false

    return executable in supportedCodexExecutables
}
