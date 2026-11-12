package t3context

import org.junit.Assert.assertEquals
import org.junit.Test

class PasteFailureTest {
    @Test
    fun Should_askToStartT3Code_When_appIsNotRunning() {
        val message = describePasteFailure("0:96: execution error: T3 Code is not running. (1000)\n", "Rider")

        assertEquals("Start T3 Code and open the target thread, then retry.", message)
    }

    @Test
    fun Should_pointToAccessibilitySettings_When_keystrokesAreRefused() {
        val stderr = "execution error: System Events got an error: osascript is not allowed to send keystrokes. (1002)"

        val message = describePasteFailure(stderr, "Rider")

        assertEquals("Allow Rider under System Settings → Privacy & Security → Accessibility, then retry.", message)
    }

    @Test
    fun Should_pointToAutomationSettings_When_appleEventsAreRefused() {
        val stderr = "execution error: Not authorized to send Apple events to System Events. (-1743)"

        val message = describePasteFailure(stderr, "Rider")

        assertEquals("Allow Rider to control System Events under System Settings → Privacy & Security → Automation, then retry.", message)
    }

    @Test
    fun Should_includeRawError_When_errorIsUnknown() {
        val message = describePasteFailure("execution error: Something else. (-42)", "Rider")

        assertEquals("Could not paste into T3 Code: execution error: Something else. (-42)", message)
    }
}
