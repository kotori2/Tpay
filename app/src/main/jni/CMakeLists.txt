# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.设置库的名字，名字需要跟你在cpp文件中C++的文件一样。同时会在build文件中生成libnative-lib.so文件
             security

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s). 设置cpp文件的路径
            ${CMAKE_SOURCE_DIR}/security.cpp ${CMAKE_SOURCE_DIR}/util.cpp )
target_include_directories(security PRIVATE ${CMAKE_SOURCE_DIR}/includes)
#OpenSSL
add_library(crypto STATIC IMPORTED)
set_target_properties(crypto
  PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../libs/${ANDROID_ABI}/libcrypto.a)

add_library(ssl STATIC IMPORTED)
set_target_properties(ssl
  PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../libs/${ANDROID_ABI}/libssl.a)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable. 使用是ndk自带的日志库。
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.  //如果奥是用log-lib，需要link到我们自己的库native-lib
                       security

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       crypto
                       ssl
                       z
                    )