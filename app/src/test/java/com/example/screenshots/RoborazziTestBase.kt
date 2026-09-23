package com.example.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 📸 RoborazziTestBase
 * Base test class for Roborazzi screenshot tests in Yemen Services Directory.
 * Standardizes device configuration (Pixel 8), Native Graphics mode,
 * and handles Light/Dark theming and RTL/LTR directionality.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
abstract class RoborazziTestBase {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Unified capture helper that sets up the layout direction (RTL/LTR),
     * applies the application theme (Light/Dark), waits for UI idle,
     * and captures the root visual tree to the designated screenshots directory.
     */
    fun captureScreen(
        screenshotName: String,
        darkTheme: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Rtl,
        content: @Composable () -> Unit
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme(darkTheme = darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/$screenshotName.png",
            roborazziOptions = RoborazziOptions(
                recordOptions = RoborazziOptions.RecordOptions(
                    resizeScale = 1.0
                ),
                compareOptions = RoborazziOptions.CompareOptions(
                    changeThreshold = 0.01f
                )
            )
        )
    }
}
