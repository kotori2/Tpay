//
// Created by lijun on 2019/8/10.
//
#include <jni.h>
#include <malloc.h>
#include <random>

char hexconvtab[] = "0123456789abcdef";
char* bin2hex(unsigned char *old, const size_t oldlen)
{
    char *result = (char*) malloc(oldlen * 2 + 1);
    size_t i, j;

    for (i = j = 0; i < oldlen; i++) {
        result[j++] = hexconvtab[old[i] >> 4];
        result[j++] = hexconvtab[old[i] & 15];
    }
    result[j] = '\0';
    return result;
}

void genRandomBytes(char inp[], int len){
    std::random_device rd;
    std::default_random_engine gen = std::default_random_engine(rd());
    std::uniform_int_distribution<int> dis(0, 0xFF);
    for(int i = 0; i < len; i++){
        inp[i] = (char)dis(gen);
    }
}