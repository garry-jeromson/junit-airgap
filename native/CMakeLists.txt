cmake_minimum_required(VERSION 3.10)
project(junit-airgap-agent)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Default to Release build type if not specified
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE "Debug" CACHE STRING "Build type (Debug or Release)" FORCE)
endif()

# Compiler flags for Release and Debug builds
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    # Release: optimize for speed, strip debug symbols
    set(CMAKE_CXX_FLAGS_RELEASE "-O3 -DNDEBUG")
    message(STATUS "Building in RELEASE mode - optimizations enabled, debug logging disabled")
elseif(CMAKE_BUILD_TYPE STREQUAL "Debug")
    # Debug: keep debug symbols, enable debug logging
    set(CMAKE_CXX_FLAGS_DEBUG "-g -DDEBUG")
    message(STATUS "Building in DEBUG mode - debug symbols and logging enabled")
endif()

# Find JNI (includes JVMTI headers)
find_package(JNI REQUIRED)

# Include directories
include_directories(
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Source files
set(AGENT_SOURCES
    src/agent.cpp
    src/socket_interceptor.cpp
    src/dns_interceptor.cpp
)

# Create shared library (agent)
add_library(junit-airgap-agent SHARED ${AGENT_SOURCES})

# Link against JNI
target_link_libraries(junit-airgap-agent ${JNI_LIBRARIES})

# Platform-specific settings
if(APPLE)
    # macOS: Create .dylib
    set_target_properties(junit-airgap-agent PROPERTIES
        SUFFIX ".dylib"
        PREFIX "lib"
    )
elseif(UNIX)
    # Linux: Create .so
    set_target_properties(junit-airgap-agent PROPERTIES
        SUFFIX ".so"
        PREFIX "lib"
    )
elseif(WIN32)
    # Windows: Create .dll
    set_target_properties(junit-airgap-agent PROPERTIES
        SUFFIX ".dll"
        PREFIX ""
    )
endif()

# Enable position-independent code
set_target_properties(junit-airgap-agent PROPERTIES
    POSITION_INDEPENDENT_CODE ON
)

# Install target
install(TARGETS junit-airgap-agent
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
)

# Print configuration
message(STATUS "Building JVMTI agent for junit-airgap")
message(STATUS "  CMAKE_BUILD_TYPE: ${CMAKE_BUILD_TYPE}")
message(STATUS "  CMAKE_SYSTEM_NAME: ${CMAKE_SYSTEM_NAME}")
message(STATUS "  CMAKE_SYSTEM_PROCESSOR: ${CMAKE_SYSTEM_PROCESSOR}")
message(STATUS "  JNI_INCLUDE_DIRS: ${JNI_INCLUDE_DIRS}")
message(STATUS "  JNI_LIBRARIES: ${JNI_LIBRARIES}")
