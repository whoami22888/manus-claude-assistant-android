#include "include/core_logic.h"

#include <algorithm>
#include <cctype>
#include <ctime>
#include <string>

namespace AssistantCore {

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

static std::string to_lower(const std::string& s) {
    std::string result = s;
    std::transform(result.begin(), result.end(), result.begin(),
                   [](unsigned char c) { return std::tolower(c); });
    return result;
}

static bool contains_ci(const std::string& haystack, const std::string& needle) {
    return to_lower(haystack).find(to_lower(needle)) != std::string::npos;
}

// ---------------------------------------------------------------------------
// CoreLogic
// ---------------------------------------------------------------------------

CoreLogic::CoreLogic()  = default;
CoreLogic::~CoreLogic() = default;

void CoreLogic::initialize() {
    // Nothing to set up for the rule-based engine; reserved for future use.
}

std::string CoreLogic::process_input(const std::string& input) {
    const std::string lower = to_lower(input);

    if (contains_ci(input, "hello") || contains_ci(input, "hi") ||
        contains_ci(input, "hey")) {
        return "Hello! How can I assist you today?";
    }
    if (contains_ci(input, "what time") || lower == "time") {
        std::time_t now = std::time(nullptr);
        char buf[32];
        std::strftime(buf, sizeof(buf), "%I:%M %p", std::localtime(&now));
        return std::string("The current time is ") + buf + ".";
    }
    if (contains_ci(input, "what date") || contains_ci(input, "today") ||
        lower == "date") {
        std::time_t now = std::time(nullptr);
        char buf[32];
        std::strftime(buf, sizeof(buf), "%B %d, %Y", std::localtime(&now));
        return std::string("Today's date is ") + buf + ".";
    }
    if (contains_ci(input, "upload")) {
        return "File upload is ready. Tap the menu to select a file from your device.";
    }
    if (contains_ci(input, "download")) {
        return "File download is ready. Please specify a path or URL.";
    }
    if (contains_ci(input, "list files") || contains_ci(input, "show files")) {
        return "Local files available: notes.txt, photo.jpg, document.pdf.";
    }
    if (contains_ci(input, "help")) {
        return "I can help with:\n"
               "- Time and date queries\n"
               "- File operations (upload / download / list)\n"
               "- General questions\n"
               "Just type or speak your request!";
    }
    if (contains_ci(input, "bye") || contains_ci(input, "goodbye") ||
        contains_ci(input, "exit")) {
        return "Goodbye! Have a great day!";
    }
    if (contains_ci(input, "thank")) {
        return "You're welcome! Let me know if there's anything else I can do.";
    }

    return "I received: \"" + input +
           "\".\nI'm still learning. Type \"help\" to see what I can do.";
}

void CoreLogic::on_voice_recognized(const std::string& text) {
    std::string response = process_input(text);
    if (speak_callback_) {
        speak_callback_(response);
    }
}

// ---------------------------------------------------------------------------
// Callback setters
// ---------------------------------------------------------------------------

void CoreLogic::set_speak_callback(
    std::function<void(const std::string&)> callback) {
    speak_callback_ = std::move(callback);
}

void CoreLogic::set_start_listening_callback(std::function<void()> callback) {
    start_listening_callback_ = std::move(callback);
}

void CoreLogic::set_stop_listening_callback(std::function<void()> callback) {
    stop_listening_callback_ = std::move(callback);
}

void CoreLogic::set_upload_file_callback(
    std::function<void(const std::string&)> callback) {
    upload_file_callback_ = std::move(callback);
}

void CoreLogic::set_list_files_callback(
    std::function<void(const std::string&)> callback) {
    list_files_callback_ = std::move(callback);
}

void CoreLogic::set_download_file_callback(
    std::function<void(const std::string&, const std::string&)> callback) {
    download_file_callback_ = std::move(callback);
}

} // namespace AssistantCore
