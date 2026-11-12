package t3context

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil

internal const val T3_CODE_BUNDLE_ID = "com.t3tools.t3code"
private const val PASTE_TIMEOUT_MILLIS = 5_000

// T3 Code exposes no API for appending to the open thread's draft, so the reference is pasted into
// whatever has focus in its window. Activation is asynchronous; wait up to ~2 s before pasting.
private val pasteScript = """
    if application id "$T3_CODE_BUNDLE_ID" is not running then error "T3 Code is not running." number 1000
    tell application id "$T3_CODE_BUNDLE_ID" to activate
    tell application "System Events"
        set t3Process to first application process whose bundle identifier is "$T3_CODE_BUNDLE_ID"
        repeat 40 times
            if frontmost of t3Process then exit repeat
            delay 0.05
        end repeat
        if not (frontmost of t3Process) then error "T3 Code did not come to the front." number 1001
        keystroke "v" using command down
    end tell
""".trimIndent()

private val appleScriptErrorNumber = Regex("""\((-?\d+)\)\s*$""")

/** Activates T3 Code and pastes the clipboard; returns a user-facing failure message, or `null` on success. */
internal fun pasteClipboardIntoT3Code(ideName: String): String? {
    val command = GeneralCommandLine("osascript")
        .withParameters(pasteScript.lines().flatMap { line -> listOf("-e", line) })

    val output = try {
        ExecUtil.execAndGetOutput(command, PASTE_TIMEOUT_MILLIS)
    } catch (exception: ExecutionException) {
        return "Could not run osascript: ${exception.message}"
    }

    return when {
        output.isTimeout -> "T3 Code did not respond within ${PASTE_TIMEOUT_MILLIS / 1000} seconds."
        output.exitCode == 0 -> null
        else -> describePasteFailure(output.stderr, ideName)
    }
}

internal fun describePasteFailure(stderr: String, ideName: String): String {
    val errorNumber = appleScriptErrorNumber.find(stderr.trim())?.groupValues?.get(1)?.toInt()
    return when (errorNumber) {
        1000 -> "Start T3 Code and open the target thread, then retry."
        1001 -> "T3 Code did not come to the front. Retry."
        // 1002: keystrokes refused; -1719: assistive access refused.
        1002, -1719 -> "Allow $ideName under System Settings → Privacy & Security → Accessibility, then retry."
        -1743 -> "Allow $ideName to control System Events under System Settings → Privacy & Security → Automation, then retry."
        else -> "Could not paste into T3 Code: ${stderr.trim().ifEmpty { "unknown osascript error" }}"
    }
}
