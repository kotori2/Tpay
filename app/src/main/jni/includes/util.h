//
// Created by lijun on 2019/8/10.
//
#include <jni.h>
#ifndef TPAY_MASTER_UTIL_H
#define TPAY_MASTER_UTIL_H

#endif //TPAY_MASTER_UTIL_H

#if defined(__arm64__) || defined(__aarch64__)
#define ABI "arm64-v8a"
#elif defined(__arm__)
#define ABI "armeabi-v7"
#elif defined(__i386__)
#define ABI "x86"
#elif defined(_x86_64)
#define ABI "x86_64"
#else
#define ABI "unknown"
#endif

char* bin2hex(unsigned char *old, const size_t oldlen);
void genRandomBytes(char inp[], int len);