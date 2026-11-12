package t3context

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.nio.file.Path

private const val NOTIFICATION_GROUP_ID = "T3CodeContext"
private const val NOTIFICATION_TITLE = "T3 Code Context"

// T3 Code reads the clipboard while handling the synthesized Cmd+V after osascript returns.
private const val CLIPBOARD_RESTORE_DELAY_MILLIS = 500L

class InsertT3CodeReferenceAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR)
            ?: FileEditorManager.getInstance(project).selectedTextEditor
            ?: return notify(project, "Open a file in the editor, then retry.")
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: FileDocumentManager.getInstance().getFile(editor.document)
            ?: return notify(project, "The current editor is not backed by a file.")
        val marker = captureEditorReference(editor, file).format(projectDirectory(project))

        // T3 Code agents resolve file references from disk, so keep their view aligned with the IDE editor.
        FileDocumentManager.getInstance().saveDocument(editor.document)

        val clipboard = CopyPasteManager.getInstance()
        val previousContents = clipboard.contents
        clipboard.setContents(StringSelection(marker))

        event.coroutineScope.launch {
            val failure = withContext(Dispatchers.IO) {
                pasteClipboardIntoT3Code(ApplicationNamesInfo.getInstance().fullProductName)
            }

            if (failure != null) {
                // Keep the reference on the clipboard so it can still be pasted by hand.
                withContext(Dispatchers.EDT) { notify(project, "$failure The reference is on the clipboard.") }
                return@launch
            }

            delay(CLIPBOARD_RESTORE_DELAY_MILLIS)
            withContext(Dispatchers.EDT) {
                // Leave anything copied in the meantime untouched.
                if (previousContents != null && clipboard.getContents<String>(DataFlavor.stringFlavor) == marker) {
                    clipboard.setContents(previousContents)
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.setEnabledAndVisible(event.project != null && SystemInfo.isMac)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

private fun projectDirectory(project: Project): Path =
    project.basePath?.let(Path::of) ?: Path.of(".").toAbsolutePath().normalize()

private fun notify(project: Project, message: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup(NOTIFICATION_GROUP_ID)
        .createNotification(NOTIFICATION_TITLE, message, NotificationType.INFORMATION)
        .notify(project)
}
