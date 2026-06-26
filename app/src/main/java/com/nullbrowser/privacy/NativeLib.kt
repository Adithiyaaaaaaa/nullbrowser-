package com.nullbrowser.privacy

class NativeLib {
    companion object {
        init {
            System.loadLibrary("nullbrowser")
        }
    }

    external fun getSecureKey(): String
    external fun verifySignature(signature: String): Boolean
}
