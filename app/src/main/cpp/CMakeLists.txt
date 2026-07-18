cmake_minimum_required(VERSION 3.31.5)

project(assistantcore)

add_library(
    assistantcore
    SHARED
    native_command_processor.cpp
)

find_library(log-lib log)

target_link_libraries(
    assistantcore
    ${log-lib}
)
