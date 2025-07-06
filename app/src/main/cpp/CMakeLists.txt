cmake_minimum_required(VERSION 3.18)

project(PersonalAssistantNative CXX)

# Find the prebuilt shared core library
# Assuming the shared core library is placed in a known location relative to this CMakeLists.txt
# Adjust the path as necessary based on the actual project structure
set(SHARED_CORE_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../../shared_core/build)
find_library(ASSISTANT_CORE_LIB
             NAMES AssistantCore
             PATHS ${SHARED_CORE_LIB_DIR}
             NO_DEFAULT_PATH)

if(NOT ASSISTANT_CORE_LIB)
    message(FATAL_ERROR "Shared core library (libAssistantCore.so) not found in ${SHARED_CORE_LIB_DIR}")
endif()

# Add the native library for Android
add_library(personalassistant_native SHARED
            jni_bridge.cpp)

# Include directories
include_directories(${CMAKE_SOURCE_DIR}/../../../../shared_core/include)

# Link against the Android log library and the prebuilt shared core library
target_link_libraries(personalassistant_native
                      android
                      log
                      ${ASSISTANT_CORE_LIB})

