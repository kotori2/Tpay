# 设置构建本地库所需的最小版本
cmake_minimum_required(VERSION 3.4.1)
# 编译出一个动态库 HelloCPP，源文件只有 src/main/cpp/HelloCPP.cpp
add_library( # 设置库的名称，即 SO 文件的名称
             security
             # STATIC：静态库，是目标文件的归档文件，在链接其它目标的时候使用。
             # SHARED：动态库，会被动态链接，在运行时被加载。
             # MODULE：模块库，是不会被链接到其它目标中的插件，但是可能会在运行时使用dlopen-系列的函数动态链接。
             SHARED
             # 要编译的源代码文件
             src/main/native/security.cpp src/main/native/rsa.cpp )
# 指定的是头文件的目录
# include_directories(src/main/cpp/include/)
# 添加 NDK API，Android NDK 提供了一套实用的原生 API 和库，通过将 NDK 库包含到项目的 CMakeLists.txt 脚本文件中。
find_library( # 定位 Android 特定的日志支持库并将其路径存储在 log-lib 中
              log-lib
              # Specifies the name of the NDK library that
              # CMake needs to locate.
              log )
# 为了确保您的原生库可以在 log 库中调用函数，您需要使用 CMake 构建脚本中的 target_link_libraries() 命令关联库。
target_link_libraries( # Specifies the target library.
                       security
                       # Links the log library to the target library.
                       ${log-lib} )