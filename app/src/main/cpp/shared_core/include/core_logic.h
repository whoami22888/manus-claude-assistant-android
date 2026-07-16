#pragma once

#include <functional>
#include <string>

namespace AssistantCore {

/**
 * Rule-based assistant core built in-tree.
 *
 * Provides the same command set as AssistantEngine.kt so that both the
 * pure-Kotlin path and the JNI/native path produce consistent responses.
 * All six callback slots allow the JNI bridge to forward events back into
 * the Android/Kotlin layer.
 */
class CoreLogic {
public:
    CoreLogic();
    ~CoreLogic();

    // Lifecycle
    void initialize();

    // Core processing
    std::string process_input(const std::string& input);
    void on_voice_recognized(const std::string& text);

    // Callback setters (called by the JNI bridge after initialize())
    void set_speak_callback(std::function<void(const std::string&)> callback);
    void set_start_listening_callback(std::function<void()> callback);
    void set_stop_listening_callback(std::function<void()> callback);
    void set_upload_file_callback(std::function<void(const std::string&)> callback);
    void set_list_files_callback(std::function<void(const std::string&)> callback);
    void set_download_file_callback(
        std::function<void(const std::string&, const std::string&)> callback);

private:
    std::function<void(const std::string&)>              speak_callback_;
    std::function<void()>                                start_listening_callback_;
    std::function<void()>                                stop_listening_callback_;
    std::function<void(const std::string&)>              upload_file_callback_;
    std::function<void(const std::string&)>              list_files_callback_;
    std::function<void(const std::string&, const std::string&)> download_file_callback_;
};

} // namespace AssistantCore
