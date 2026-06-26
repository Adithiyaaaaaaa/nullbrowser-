#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_nullbrowser_privacy_NativeLib_getSecureKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = "null_browser_v1_0_0_agent_key";
    return env->NewStringUTF(key.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nullbrowser_privacy_NativeLib_verifySignature(
        JNIEnv* env,
        jobject /* this */,
        jstring signature) {
    // In production, compare with an obfuscated/encrypted hash
    const char *nativeString = env->GetStringUTFChars(signature, 0);
    bool result = std::string(nativeString) == "EXPECTED_SIGNATURE_HASH";
    env->ReleaseStringUTFChars(signature, nativeString);
    return result;
}
