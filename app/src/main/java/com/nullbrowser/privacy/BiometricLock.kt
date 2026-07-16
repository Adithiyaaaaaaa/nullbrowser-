package com.nullbrowser.privacy

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricLock(
    private val activity: FragmentActivity,
    private val onAuthenticated: () -> Unit,
    private val onPanicWipe: (String) -> Unit
) {
    private var failedAttempts = 0
    private val maxAttempts = 5
    private val lockTimeout = 30000L // 30 seconds
    private var lastBackgroundTime = 0L

    fun onPause() {
        lastBackgroundTime = System.currentTimeMillis()
    }

    fun onResume() {
        if (lastBackgroundTime != 0L && (System.currentTimeMillis() - lastBackgroundTime) > lockTimeout) {
            showLock()
        }
    }

    private fun showLock() {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onPanicWipe("Panic wipe triggered by user.")
                    } else {
                        handleFailure()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    failedAttempts = 0
                    onAuthenticated()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    handleFailure()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("NullBrowser Locked")
            .setSubtitle("Authenticate to continue session")
            .setNegativeButtonText("Panic Wipe")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun handleFailure() {
        failedAttempts++
        if (failedAttempts >= maxAttempts) {
            onPanicWipe("Max biometric attempts reached. Panic wipe triggered.")
        } else {
            showLock()
        }
    }
}
