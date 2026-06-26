cmake_minimum_required(VERSION 3.22.1)
project("nullbrowser")

add_library(
    nullbrowser
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    nullbrowser
    ${log-lib}
)
