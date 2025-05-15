cmake_minimum_required(VERSION 3.28)
project(jniwrapper)

set(CMAKE_CXX_STANDARD 20)

set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -s -O3")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -s -O3")

if(NOT DEFINED ENV{JAVA_HOME})
    message(FATAL_ERROR "JAVA_HOME is not set")
endif()

include_directories($ENV{JAVA_HOME}/include $ENV{JAVA_HOME}/include/linux)

add_library(jniwrapper SHARED jniwrapper.cpp)

find_program(PATCHELF_EXEC patchelf)

if(NOT PATCHELF_EXEC)
    message(FATAL_ERROR "patchelf not found")
endif()

add_custom_command(TARGET jniwrapper POST_BUILD
        COMMAND ${PATCHELF_EXEC} --add-needed libzomdroidlinker.so $<TARGET_FILE:jniwrapper>
        COMMENT "Add zomdroidlinker to DT_NEEDED so box64 can resolve external symbols at load time"
)
