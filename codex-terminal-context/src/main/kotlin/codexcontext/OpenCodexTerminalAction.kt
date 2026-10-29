package codexcontext

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.plugins.terminal.TerminalIcons

class OpenCodexTerminalAction : DumbAwareAction() {
    init {
        templatePresentation.icon = TerminalIcons.Agents.Codex
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!launchCodexTerminal(project)) {
            notify(project, "Codex executable was not found in PATH or ~/.local/bin.")
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.setEnabledAndVisible(event.project != null)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
