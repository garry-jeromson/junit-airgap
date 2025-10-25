cmake_minimum_required(VERSION 3.10)
project(junit-no-network-agent)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

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
add_library(junit-no-network-agent SHARED ${AGENT_SOURCES})

# Link against JNI
target_link_libraries(junit-no-network-agent ${JNI_LIBRARIES})

# Platform-specific settings
if(APPLE)
    # macOS: Create .dylib
    set_target_properties(junit-no-network-agent PROPERTIES
        SUFFIX ".dylib"
        PREFIX "lib"
    )
elseif(UNIX)
    # Linux: Create .so
    set_target_properties(junit-no-network-agent PROPERTIES
        SUFFIX ".so"
        PREFIX "lib"
    )
elseif(WIN32)
    # Windows: Create .dll
    set_target_properties(junit-no-network-agent PROPERTIES
        SUFFIX ".dll"
        PREFIX ""
    )
endif()

# Enable position-independent code
set_target_properties(junit-no-network-agent PROPERTIES
    POSITION_INDEPENDENT_CODE ON
)

# Install target
install(TARGETS junit-no-network-agent
    LIBRARY DESTINATION lib
    RUNTIME DESTINATION bin
)

# Print configuration
message(STATUS "Building JVMTI agent for junit-no-network")
message(STATUS "  CMAKE_SYSTEM_NAME: ${CMAKE_SYSTEM_NAME}")
message(STATUS "  CMAKE_SYSTEM_PROCESSOR: ${CMAKE_SYSTEM_PROCESSOR}")
message(STATUS "  JNI_INCLUDE_DIRS: ${JNI_INCLUDE_DIRS}")
message(STATUS "  JNI_LIBRARIES: ${JNI_LIBRARIES}")
