package com.nullbrowser.privacy

import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricLock(
    private val activity: FragmentActivity,
    private val onAuthenticated: () -> Unit,
    private val onPanicWipe: (String) -> Unit
) {
    private var failedAttempts = 0
    private val maxAttempts = 5
    private val handler = Handler(Looper.getMainLooper())
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
        val executor: Executor = { command -> handler.post(command) }
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    handleFailure()
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
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
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
