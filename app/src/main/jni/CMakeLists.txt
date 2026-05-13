cmake_minimum_required(VERSION 3.10)

project(yolov8ncnn)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

message(STATUS "ANDROID_ABI = ${ANDROID_ABI}")

# =========================
# OpenCV Mobile 手动配置
# =========================
set(OPENCV_MOBILE_DIR "${CMAKE_CURRENT_LIST_DIR}/opencv-mobile-4.12.0-android")
set(OPENCV_INCLUDE_DIR "${OPENCV_MOBILE_DIR}/sdk/native/jni/include")
set(OPENCV_STATIC_LIB_DIR "${OPENCV_MOBILE_DIR}/sdk/native/staticlibs/${ANDROID_ABI}")
set(OPENCV_3RDPARTY_LIB_DIR "${OPENCV_MOBILE_DIR}/sdk/native/3rdparty/libs/${ANDROID_ABI}")

message(STATUS "OPENCV_INCLUDE_DIR = ${OPENCV_INCLUDE_DIR}")
message(STATUS "OPENCV_STATIC_LIB_DIR = ${OPENCV_STATIC_LIB_DIR}")
message(STATUS "OPENCV_3RDPARTY_LIB_DIR = ${OPENCV_3RDPARTY_LIB_DIR}")

if(NOT EXISTS "${OPENCV_INCLUDE_DIR}/opencv2/core/core.hpp")
    message(FATAL_ERROR "OpenCV include not found: ${OPENCV_INCLUDE_DIR}")
endif()

if(NOT EXISTS "${OPENCV_STATIC_LIB_DIR}/libopencv_core.a")
    message(FATAL_ERROR "libopencv_core.a not found: ${OPENCV_STATIC_LIB_DIR}")
endif()

if(NOT EXISTS "${OPENCV_STATIC_LIB_DIR}/libopencv_imgproc.a")
    message(FATAL_ERROR "libopencv_imgproc.a not found: ${OPENCV_STATIC_LIB_DIR}")
endif()

file(GLOB OPENCV_3RDPARTY_LIBS "${OPENCV_3RDPARTY_LIB_DIR}/*.a")

set(OpenCV_LIBS
        "${OPENCV_STATIC_LIB_DIR}/libopencv_imgproc.a"
        "${OPENCV_STATIC_LIB_DIR}/libopencv_core.a"
        ${OPENCV_3RDPARTY_LIBS}
        )

find_package(OpenMP)

# =========================
# ncnn 手动配置
# 不再使用 find_package(ncnn)
# =========================
set(NCNN_ROOT "${CMAKE_CURRENT_LIST_DIR}/ncnn-20260113-android-vulkan/${ANDROID_ABI}")
set(NCNN_INCLUDE_DIR "${NCNN_ROOT}/include")
set(NCNN_INCLUDE_NCNN_DIR "${NCNN_ROOT}/include/ncnn")
set(NCNN_LIB_DIR "${NCNN_ROOT}/lib")
set(NCNN_CMAKE_DIR "${NCNN_ROOT}/lib/cmake/ncnn")

message(STATUS "NCNN_ROOT = ${NCNN_ROOT}")
message(STATUS "NCNN_INCLUDE_DIR = ${NCNN_INCLUDE_DIR}")
message(STATUS "NCNN_LIB_DIR = ${NCNN_LIB_DIR}")
message(STATUS "NCNN_CMAKE_DIR = ${NCNN_CMAKE_DIR}")

if(EXISTS "${NCNN_LIB_DIR}/libncnn.a")
    set(NCNN_MAIN_LIB "${NCNN_LIB_DIR}/libncnn.a")
elseif(EXISTS "${NCNN_CMAKE_DIR}/libncnn.a")
    set(NCNN_MAIN_LIB "${NCNN_CMAKE_DIR}/libncnn.a")
else()
    message(FATAL_ERROR "libncnn.a not found. Please check ncnn path: ${NCNN_ROOT}")
endif()

if(NOT EXISTS "${NCNN_INCLUDE_DIR}/ncnn/net.h" AND NOT EXISTS "${NCNN_INCLUDE_NCNN_DIR}/net.h")
    message(FATAL_ERROR "ncnn headers not found. Please check ncnn include path: ${NCNN_INCLUDE_DIR}")
endif()

file(GLOB NCNN_LIBS_IN_LIB_DIR "${NCNN_LIB_DIR}/*.a")
file(GLOB NCNN_LIBS_IN_CMAKE_DIR "${NCNN_CMAKE_DIR}/*.a")

set(NCNN_LIBS
        ${NCNN_LIBS_IN_LIB_DIR}
        ${NCNN_LIBS_IN_CMAKE_DIR}
        )

message(STATUS "NCNN_MAIN_LIB = ${NCNN_MAIN_LIB}")
message(STATUS "NCNN_LIBS = ${NCNN_LIBS}")

# =========================
# 编译 yolov8ncnn
# =========================
add_library(
        yolov8ncnn
        SHARED
        yolov8ncnn.cpp
        yolov8.cpp
        yolov8_det.cpp
        yolov8_seg.cpp
        yolov8_pose.cpp
        yolov8_cls.cpp
        yolov8_obb.cpp
        ndkcamera.cpp
)

target_include_directories(
        yolov8ncnn
        PRIVATE
        ${OPENCV_INCLUDE_DIR}
        ${NCNN_INCLUDE_DIR}
        ${NCNN_INCLUDE_NCNN_DIR}
)

target_link_libraries(
        yolov8ncnn
        -Wl,--start-group
        ${NCNN_LIBS}
        ${OpenCV_LIBS}
        -Wl,--end-group

        camera2ndk
        mediandk
        android
        log
        jnigraphics
        vulkan
        z
        c++_shared
)

if(OpenMP_CXX_FOUND)
    target_link_libraries(yolov8ncnn OpenMP::OpenMP_CXX)
endif()