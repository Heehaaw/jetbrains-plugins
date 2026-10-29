package codexcontext

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.terminal.TerminalIcons

private const val NOTIFICATION_GROUP_ID = "CodexTerminalContext"
private const val NOTIFICATION_TITLE = "Codex Terminal Context"
private const val TERMINAL_TOOL_WINDOW_ID = "Terminal"

class InsertCodexReferenceAction : AnAction() {
    init {
        templatePresentation.icon = TerminalIcons.Agents.Codex
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR)
            ?: FileEditorManager.getInstance(project).selectedTextEditor
            ?: return notify(project, "Open a file in the editor, then retry.")
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: FileDocumentManager.getInstance().getFile(editor.document)
            ?: return notify(project, "The current editor is not backed by a file.")
        val reference = captureEditorReference(editor, file)

        // Codex resolves file references from disk, so keep its view aligned with the IDE editor.
        FileDocumentManager.getInstance().saveDocument(editor.document)

        event.coroutineScope.launch {
            // Terminal creation and view mutation require EDT after the action read context has unwound.
            withContext(Dispatchers.EDT) {
                val target = findOrLaunchTarget(project) ?: return@withContext
                target.insertReference(reference.format(target.workingDirectory))
                target.focus(project)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.setEnabledAndVisible(event.project != null)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

private suspend fun findOrLaunchTarget(project: Project): CodexTerminalSession? {
    val sessions = findLiveCodexSessions(project)
    if (sessions.isNotEmpty()) {
        return selectTarget(project, sessions)
    }

    if (!launchCodexTerminal(project)) {
        notify(project, "Codex executable was not found in PATH or ~/.local/bin.")
        return null
    }

    return awaitLiveCodexSession(project)
        ?: run {
            notify(project, "The IDE could not start a Codex terminal within 10 seconds.")
            null
        }
}

private fun selectTarget(project: Project, sessions: List<CodexTerminalSession>): CodexTerminalSession? {
    val selectedContent = ToolWindowManager.getInstance(project)
        .getToolWindow(TERMINAL_TOOL_WINDOW_ID)
        ?.contentManager
        ?.selectedContent

    return sessions.firstOrNull { session -> session.content === selectedContent }
        ?: sessions.singleOrNull()
        ?: run {
            notify(project, "Select the Codex terminal that should receive the reference, then retry.")
            null
        }
}

internal fun notify(project: Project, message: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(NOTIFICATION_GROUP_ID)
        .createNotification(NOTIFICATION_TITLE, message, NotificationType.INFORMATION)
        .notify(project)
}
