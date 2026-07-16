package com.nullbrowser.privacy

import java.io.File
import android.os.Build

object RootSignals {
    private val ROOT_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )

    fun looksRooted(): Boolean {
        for (path in ROOT_PATHS) {
            if (File(path).exists()) return true
        }
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }
}
