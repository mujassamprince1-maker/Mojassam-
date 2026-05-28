package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.PrivoraTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentScreen by viewModel.currentScreen.collectAsState()
            val activeTheme by viewModel.theme.collectAsState()
            val isAppLocked by viewModel.isAppLocked.collectAsState()
            val screenshotProtection by viewModel.screenshotProtection.collectAsState()

            // --- Screenshot Protection Integration ---
            // Native Window FLAG_SECURE manipulation based on user configuration state
            LaunchedEffect(screenshotProtection) {
                if (screenshotProtection) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            PrivoraTheme(themeSelection = activeTheme) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Core navigation routing
                    when (currentScreen) {
                        AppScreen.SPLASH -> SplashScreen(viewModel)
                        AppScreen.ONBOARDING -> OnboardingScreen(viewModel)
                        AppScreen.LOGIN -> LoginScreen(viewModel)
                        AppScreen.OTP -> OtpVerificationScreen(viewModel)
                        AppScreen.DASHBOARD -> DashboardContainer(viewModel)
                        AppScreen.PRIVATE_CHAT -> PrivateChatScreen(viewModel)
                        AppScreen.GROUP_CHAT -> GroupChatScreen(viewModel)
                        AppScreen.CONTACTS -> ContactsScreen(viewModel)
                        AppScreen.CALL_SCREEN -> AudioCallScreen(viewModel)
                        AppScreen.VIDEO_CALL_SCREEN -> VideoCallScreen(viewModel)
                        AppScreen.FINGERPRINT_SETUP -> {} // Handled inline by dynamic preferences selectors
                    }

                    // --- Dynamic Biometric Inactivity Cover ---
                    if (isAppLocked) {
                        BiometricLockOverlay(viewModel)
                    }
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        // Reports activity of user on physical panel to reset auto-locking triggers
        viewModel.reportUserInteraction()
    }

    override fun onPause() {
        super.onPause()
        // Safeguard physical leaving by enabling automatic lockout checks if biometric configured
        if (viewModel.isBiometricLocked.value || viewModel.isPasscodeEnabled.value) {
            viewModel.logAdminAction("SysEngine: Activity paused, validating background lock limits.")
        }
    }
}
