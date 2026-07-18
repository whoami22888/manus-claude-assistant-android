#include <jni.h>

#include <algorithm>
#include <cctype>
#include <string>

namespace {

std::string ToLower(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
        return static_cast<char>(std::tolower(ch));
    });
    return value;
}

std::string ProcessCommand(const std::string& input) {
    const std::string lower = ToLower(input);
    if (lower.find("native status") != std::string::npos) {
        return "Native core connected through JNI and ready to process commands.";
    }
    if (lower.find("native help") != std::string::npos) {
        return "Native commands:\n• native status\n• native help\n• native process <command>";
    }
    const std::string token = "native process";
    const auto process_index = lower.find(token);
    if (process_index != std::string::npos) {
        std::string payload = input.substr(process_index + token.size());
        const auto start = payload.find_first_not_of(" \t");
        payload = start == std::string::npos ? "" : payload.substr(start);
        if (payload.empty()) {
            payload = "no command provided";
        }
        return "Native core processed: " + payload;
    }
    return "";
}

}  // namespace

extern "C"
JNIEXPORT jstring JNICALL
Java_com_manus_assistant_NativeCommandProcessor_processCommandNative(
    JNIEnv* env,
    jobject /* thiz */,
    jstring input
) {
    const char* raw_input = env->GetStringUTFChars(input, nullptr);
    std::string result = ProcessCommand(raw_input == nullptr ? "" : raw_input);
    if (raw_input != nullptr) {
        env->ReleaseStringUTFChars(input, raw_input);
    }
    return env->NewStringUTF(result.c_str());
}
