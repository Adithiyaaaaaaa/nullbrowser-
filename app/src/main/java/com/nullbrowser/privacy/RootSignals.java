package com.nullbrowser.privacy;

import java.io.File;

final class RootSignals {
    private static final String[] ROOT_PATHS = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
    };

    private RootSignals() {
    }

    static boolean looksRooted() {
        for (String path : ROOT_PATHS) {
            if (new File(path).exists()) {
                return true;
            }
        }
        String tags = android.os.Build.TAGS;
        return tags != null && tags.contains("test-keys");
    }
}
