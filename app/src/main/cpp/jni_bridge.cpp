#include <jni.h>
#include <string>
#include <android/log.h>
#include "../../../../shared_core/include/core_logic.h" // Adjust path relative to jni_bridge.cpp

#define LOG_TAG "AssistantCoreJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global reference to the JVM and MainActivity instance for callbacks
JavaVM* g_jvm = nullptr;
jobject g_mainActivityInstance = nullptr;
jmethodID g_speakMethodId = nullptr;
jmethodID g_startListeningMethodId = nullptr;
jmethodID g_stopListeningMethodId = nullptr;
jmethodID g_uploadFileMethodId = nullptr;
jmethodID g_listFilesMethodId = nullptr; // New method ID
jmethodID g_downloadFileMethodId = nullptr; // New method ID

// Helper function to get JNIEnv
JNIEnv* getJniEnv() {
    JNIEnv* env = nullptr;
    if (g_jvm == nullptr) {
        LOGE("JVM not initialized!");
        return nullptr;
    }
    // Try to get the environment for the current thread
    int getEnvStat = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        LOGI("getJniEnv: Thread not attached, attempting to attach");
        if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("getJniEnv: Failed to attach current thread");
            return nullptr;
        }
        LOGI("getJniEnv: Thread attached successfully");
        // Mark that we might need to detach later if needed, depends on thread model
    } else if (getEnvStat == JNI_EVERSION) {
        LOGE("getJniEnv: JNI version not supported");
        return nullptr;
    } else if (getEnvStat != JNI_OK) {
         LOGE("getJniEnv: Failed to get JNI environment for unknown reason");
         return nullptr;
    }
    return env;
}

// Store JVM on load
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    LOGI("JNI_OnLoad: JVM stored.");
    return JNI_VERSION_1_6;
}

// --- Callback Implementations (C++ side, calling Kotlin) ---

void speak_callback_impl(const std::string& text) {
    JNIEnv* env = getJniEnv();
    if (!env || !g_mainActivityInstance || !g_speakMethodId) {
        LOGE("Speak callback failed: JNI Env=%p, instance=%p or method ID=%p is null", env, g_mainActivityInstance, g_speakMethodId);
        return;
    }
    jstring jtext = env->NewStringUTF(text.c_str());
    if (!jtext) { // Check if string creation failed
        LOGE("Speak callback failed: Could not create Java string");
        return;
    }
    env->CallVoidMethod(g_mainActivityInstance, g_speakMethodId, jtext);
    env->DeleteLocalRef(jtext);
    // Consider detaching if AttachCurrentThread was used and this callback runs on a background thread
    // g_jvm->DetachCurrentThread(); 
}

void start_listening_callback_impl() {
    JNIEnv* env = getJniEnv();
     if (!env || !g_mainActivityInstance || !g_startListeningMethodId) {
        LOGE("Start listening callback failed: JNI Env=%p, instance=%p or method ID=%p is null", env, g_mainActivityInstance, g_startListeningMethodId);
        return;
    }
    env->CallVoidMethod(g_mainActivityInstance, g_startListeningMethodId);
    // Consider detaching
    // g_jvm->DetachCurrentThread(); 
}

void stop_listening_callback_impl() {
    JNIEnv* env = getJniEnv();
     if (!env || !g_mainActivityInstance || !g_stopListeningMethodId) {
        LOGE("Stop listening callback failed: JNI Env=%p, instance=%p or method ID=%p is null", env, g_mainActivityInstance, g_stopListeningMethodId);
        return;
    }
    env->CallVoidMethod(g_mainActivityInstance, g_stopListeningMethodId);
    // Consider detaching
    // g_jvm->DetachCurrentThread(); 
}

void upload_file_callback_impl(const std::string& localFilePath) {
     JNIEnv* env = getJniEnv();
    if (!env || !g_mainActivityInstance || !g_uploadFileMethodId) {
        LOGE("Upload file callback failed: JNI Env=%p, instance=%p or method ID=%p is null", env, g_mainActivityInstance, g_uploadFileMethodId);
        return;
    }
    jstring jlocalFilePath = env->NewStringUTF(localFilePath.c_str());
     if (!jlocalFilePath) { // Check if string creation failed
        LOGE("Upload file callback failed: Could not create Java string");
        return;
    }
    env->CallVoidMethod(g_mainActivityInstance, g_uploadFileMethodId, jlocalFilePath);
    env->DeleteLocalRef(jlocalFilePath);
    // Consider detaching
    // g_jvm->DetachCurrentThread(); 
}

// New callback implementation for listing files
void list_files_callback_impl(const std::string& cloudPath) {
     JNIEnv* env = getJniEnv();
    if (!env || !g_mainActivityInstance || !g_listFilesMethodId) {
        LOGE("List files callback failed: JNI Env=%p, instance=%p or method ID=%p is null", env, g_mainActivityInstance, g_listFilesMethodId);
        return;
    }
    jstring jcloudPath = env->NewStringUTF(cloudPath.c_str());
     if (!jcloudPath) {
        LOGE("List files callback failed: Could not create Java string");
        return;
    }
    env->CallVoidMethod(g_mainActivityInstance, g_listFilesMethodId, jcloudPath);
    env->DeleteLocalRef(jcloudPath);
    // Consider detaching
    // g_jvm->DetachCurrentThread(); 
}

// New callback implementation for downloading files
void download_file_callback_impl(const std::string& cloudFilePath, const std::string& localSavePath) {
     JNIEnv* env = getJniEnv();
    if (!env || !g_mainActivityInstance || !g_downloadFileMethodId) {
        LOGE("Download file callback failed: JNI Env=%p, instance=%p or method ID=%p is null", env, g_mainActivityInstance, g_downloadFileMethodId);
        return;
    }
    jstring jcloudFilePath = env->NewStringUTF(cloudFilePath.c_str());
    jstring jlocalSavePath = env->NewStringUTF(localSavePath.c_str());
     if (!jcloudFilePath || !jlocalSavePath) {
        LOGE("Download file callback failed: Could not create Java strings");
        if(jcloudFilePath) env->DeleteLocalRef(jcloudFilePath);
        if(jlocalSavePath) env->DeleteLocalRef(jlocalSavePath);
        return;
    }
    env->CallVoidMethod(g_mainActivityInstance, g_downloadFileMethodId, jcloudFilePath, jlocalSavePath);
    env->DeleteLocalRef(jcloudFilePath);
    env->DeleteLocalRef(jlocalSavePath);
    // Consider detaching
    // g_jvm->DetachCurrentThread(); 
}


// --- JNI Method Implementations (Kotlin calling C++) ---

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_personalassistant_MainActivity_initializeCore(JNIEnv* env, jobject thiz /* this */) {
    LOGI("Initializing CoreLogic via JNI...");
    AssistantCore::CoreLogic* coreLogic = new AssistantCore::CoreLogic();
    coreLogic->initialize();

    // Store global reference to MainActivity instance for callbacks
    if (g_mainActivityInstance != nullptr) {
        env->DeleteGlobalRef(g_mainActivityInstance); // Delete old ref if re-initializing
        g_mainActivityInstance = nullptr;
    }
    g_mainActivityInstance = env->NewGlobalRef(thiz);
    if(g_mainActivityInstance == nullptr) {
        LOGE("Failed to create global reference for MainActivity instance");
        delete coreLogic;
        return 0;
    }
     
    // Get Method IDs for callbacks
    jclass mainActivityClass = env->GetObjectClass(thiz);
    if (!mainActivityClass) {
        LOGE("Failed to get MainActivity class");
        // Clean up global ref
        env->DeleteGlobalRef(g_mainActivityInstance);
        g_mainActivityInstance = nullptr;
        delete coreLogic;
        return 0;
    }
    g_speakMethodId = env->GetMethodID(mainActivityClass, "speak", "(Ljava/lang/String;)V");
    g_startListeningMethodId = env->GetMethodID(mainActivityClass, "startListening", "()V");
    g_stopListeningMethodId = env->GetMethodID(mainActivityClass, "stopListening", "()V");
    g_uploadFileMethodId = env->GetMethodID(mainActivityClass, "uploadFile", "(Ljava/lang/String;)V");
    g_listFilesMethodId = env->GetMethodID(mainActivityClass, "listFiles", "(Ljava/lang/String;)V"); // Get new method ID
    g_downloadFileMethodId = env->GetMethodID(mainActivityClass, "downloadFile", "(Ljava/lang/String;Ljava/lang/String;)V"); // Get new method ID

    // Check if all methods were found
    if (!g_speakMethodId || !g_startListeningMethodId || !g_stopListeningMethodId || !g_uploadFileMethodId || !g_listFilesMethodId || !g_downloadFileMethodId) {
        LOGE("Failed to get one or more callback method IDs (speak=%p, start=%p, stop=%p, upload=%p, list=%p, download=%p)", 
            g_speakMethodId, g_startListeningMethodId, g_stopListeningMethodId, g_uploadFileMethodId, g_listFilesMethodId, g_downloadFileMethodId);
        // Clean up global ref
        env->DeleteGlobalRef(g_mainActivityInstance);
        g_mainActivityInstance = nullptr;
        delete coreLogic;
        return 0;
    }

    // Set callbacks in CoreLogic
    coreLogic->set_speak_callback(speak_callback_impl);
    coreLogic->set_start_listening_callback(start_listening_callback_impl);
    coreLogic->set_stop_listening_callback(stop_listening_callback_impl);
    coreLogic->set_upload_file_callback(upload_file_callback_impl);
    coreLogic->set_list_files_callback(list_files_callback_impl); // Set new callback
    coreLogic->set_download_file_callback(download_file_callback_impl); // Set new callback

    LOGI("CoreLogic initialized and callbacks set.");
    return reinterpret_cast<jlong>(coreLogic);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_personalassistant_MainActivity_processCoreInput(JNIEnv* env, jobject /* this */, jlong corePtr, jstring inputText) {
    AssistantCore::CoreLogic* coreLogic = reinterpret_cast<AssistantCore::CoreLogic*>(corePtr);
    if (!coreLogic) {
        LOGE("processCoreInput: Invalid corePtr");
        return env->NewStringUTF("Error: Core pointer invalid");
    }

    const char* nativeString = env->GetStringUTFChars(inputText, nullptr);
    if (!nativeString) { // Check if GetStringUTFChars failed
        LOGE("processCoreInput: Failed to get UTF chars from input string");
        return env->NewStringUTF("Error: Could not read input string");
    }
    std::string input_std_string(nativeString);
    env->ReleaseStringUTFChars(inputText, nativeString);

    std::string result = coreLogic->process_input(input_std_string);

    return env->NewStringUTF(result.c_str());
}

// New JNI function implementation: Kotlin calls this when speech is recognized
extern "C" JNIEXPORT void JNICALL
Java_com_example_personalassistant_MainActivity_onCoreVoiceRecognized(JNIEnv *env, jobject /* this */, jlong corePtr, jstring recognizedText) {
    AssistantCore::CoreLogic* coreLogic = reinterpret_cast<AssistantCore::CoreLogic*>(corePtr);
    if (!coreLogic) {
        LOGE("onCoreVoiceRecognized: Invalid corePtr");
        return;
    }

    const char *nativeString = env->GetStringUTFChars(recognizedText, nullptr);
     if (!nativeString) { // Check if GetStringUTFChars failed
        LOGE("onCoreVoiceRecognized: Failed to get UTF chars from recognized text string");
        return;
    }
    std::string recognized_std_string(nativeString);
    env->ReleaseStringUTFChars(recognizedText, nativeString);

    // Call the CoreLogic method
    coreLogic->on_voice_recognized(recognized_std_string);
}


extern "C" JNIEXPORT void JNICALL
Java_com_example_personalassistant_MainActivity_destroyCore(JNIEnv* env, jobject /* this */, jlong corePtr) {
    LOGI("Destroying CoreLogic via JNI...");
    AssistantCore::CoreLogic* coreLogic = reinterpret_cast<AssistantCore::CoreLogic*>(corePtr);
    if (coreLogic) {
        delete coreLogic;
    }
    // Release global reference
    if (g_mainActivityInstance) {
        env->DeleteGlobalRef(g_mainActivityInstance);
        g_mainActivityInstance = nullptr;
        LOGI("MainActivity global reference released.");
    }
    // Reset method IDs
    g_speakMethodId = nullptr;
    g_startListeningMethodId = nullptr;
    g_stopListeningMethodId = nullptr;
    g_uploadFileMethodId = nullptr;
    g_listFilesMethodId = nullptr; // Reset new ID
    g_downloadFileMethodId = nullptr; // Reset new ID
}

