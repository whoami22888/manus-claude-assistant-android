cmake_minimum_required(VERSION 3.18)

project(PersonalAssistantNative CXX)

# Build the shared_core logic as an in-tree static library so that no
# external prebuilt .so is required.
add_library(assistant_core STATIC
            shared_core/core_logic.cpp)

target_include_directories(assistant_core PUBLIC
                           shared_core/include)

# JNI bridge — the shared library loaded by the Android runtime
add_library(personalassistant_native SHARED
            jni_bridge.cpp)

target_include_directories(personalassistant_native PRIVATE
                           shared_core/include)

# Link against Android system libs and the in-tree static core
target_link_libraries(personalassistant_native
                      android
                      log
                      assistant_core)

