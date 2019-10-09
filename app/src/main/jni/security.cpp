//
// Created by lijun on 2019/8/10.
//
#include <jni.h>
#include <random>
#include <android/log.h>
#include <openssl/pem.h>
#include <openssl/rsa.h>
#include <openssl/evp.h>
#include <openssl/aes.h>
#include <openssl/conf.h>
#include <openssl/err.h>
#include <util.h>

#define ivLen 16

const char* TAG = "LogUtils-JNI";
unsigned char sessionKey[32];
const char* publicKey = "-----BEGIN PUBLIC KEY-----\n"
                        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+/rmkYkZ1YcsHLjQgGgJuZ1LU\n"
                        "YdGTy6o8LI74SvU3Tvv2xao4Wwkfsib9UdH8oyyszZU+sw+thjgy26bU7EeeUrOW\n"
                        "ZuBcRhtksH1LgcoRtlAgPIKLtQXbt7onnUOIKgzoqA1x0x2anT3E6tsSKwvmGw1n\n"
                        "uP/tW7so4cjB2Bqv2QIDAQAB\n"
                        "-----END PUBLIC KEY-----";

extern "C"
JNIEXPORT jint JNICALL Java_com_sjk_tpay_Security_getVersion(JNIEnv *env, jclass type) {
    return 2;
}

extern "C"
    JNIEXPORT jstring JNICALL Java_com_sjk_tpay_Security_getABI(JNIEnv *env, jclass type) {
    return env->NewStringUTF(ABI);
}

extern "C"
JNIEXPORT jbyteArray JNICALL Java_com_sjk_tpay_Security_initRequest(JNIEnv *env, jclass type) {
    //随机生成sessionKey
    srand((unsigned)time(nullptr));
    for(int i = 0; i < 32; i++) {
        sessionKey[i] = (unsigned char) (rand() % 0xFF);
    }
    char source[100];
    char* hexStr = bin2hex(sessionKey, 32);
    sprintf(source, "{\"sessionKey\": \"%s\"}", hexStr);
    //__android_log_print(ANDROID_LOG_DEBUG, TAG , "SessionKey: %s", hexStr);
    free(hexStr);

    //初始化RSA
    BIO *bufIO = BIO_new_mem_buf((void*)publicKey, -1);
    RSA *rsa = PEM_read_bio_RSA_PUBKEY(bufIO, nullptr, 0, nullptr);
    BIO_free(bufIO);

    size_t rsa_len = RSA_size(rsa);

    auto *encryptedMsg = (unsigned char *)malloc(rsa_len);
    memset(encryptedMsg, 0, rsa_len);

    //加密
    int status = RSA_public_encrypt(strlen(source), reinterpret_cast<const unsigned char*>(source), encryptedMsg, rsa, RSA_PKCS1_PADDING);
    RSA_free(rsa);
    CRYPTO_cleanup_all_ex_data();

    if (status < 0){
        __android_log_print(ANDROID_LOG_ERROR, TAG , "RSA encrypt failed!");
    }

    auto *by = (jbyte*)encryptedMsg;
    jbyteArray result = env->NewByteArray(rsa_len);
    env->SetByteArrayRegion(result, 0, rsa_len, by);
    free(encryptedMsg);

    return result;
}

extern "C"
JNIEXPORT jbyteArray JNICALL Java_com_sjk_tpay_Security_getRequest(JNIEnv *env, jclass type, jstring input) {
    jboolean isCopy;
    const char* src = env->GetStringUTFChars(input, &isCopy);
    int srcLen = env->GetStringUTFLength(input);
    auto* ciphertext = (unsigned char *)malloc(srcLen * sizeof(unsigned char));

    //int ivLen = 16;
    char iv[ivLen];
    genRandomBytes(iv, ivLen);
    //__android_log_print(ANDROID_LOG_DEBUG, TAG , "IV: %s", bin2hex(reinterpret_cast<unsigned char*>(iv), ivLen));//TODO:RELEASE MEMORY

    int status;

    //初始化AES
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    status = EVP_EncryptInit_ex(ctx, EVP_aes_256_cbc(), nullptr, sessionKey, reinterpret_cast<const unsigned char*>(iv));
    if(status != 1){
        __android_log_print(ANDROID_LOG_ERROR, TAG , "EVP_EncryptInit_ex failed!");
        EVP_CIPHER_CTX_free(ctx);
        return nullptr;
    }

    int cipherLen = 0;
    int len;
    status = EVP_EncryptUpdate(ctx, ciphertext, &len, reinterpret_cast<const unsigned char*>(src), srcLen);
    if(status != 1){
        __android_log_print(ANDROID_LOG_ERROR, TAG , "EVP_EncryptUpdate failed!");
        EVP_CIPHER_CTX_free(ctx);
        return nullptr;
    }
    cipherLen = len;

    status = EVP_EncryptFinal_ex(ctx, ciphertext + len, &len);
    if(status != 1){
        __android_log_print(ANDROID_LOG_ERROR, TAG , "EVP_EncryptFinal_ex failed!");
        EVP_CIPHER_CTX_free(ctx);
        return nullptr;
    }
    cipherLen += len;

    env->ReleaseStringUTFChars(input, src);
    EVP_CIPHER_CTX_free(ctx);
    CRYPTO_cleanup_all_ex_data();

    int resultLen = cipherLen + ivLen;//IV
    auto* resultChar = (unsigned char*)malloc(resultLen * sizeof(unsigned char));
    memcpy(resultChar, iv, ivLen);
    memcpy(resultChar + ivLen, ciphertext, cipherLen);

    auto *by = (jbyte*)resultChar;
    jbyteArray result = env->NewByteArray(resultLen);
    env->SetByteArrayRegion(result, 0, resultLen, by);
    free(resultChar);
    free(ciphertext);
    return result;
}

extern "C"
JNIEXPORT jstring JNICALL Java_com_sjk_tpay_Security_getResponse(JNIEnv *env, jclass type, jbyteArray input) {
    char *cipherText = nullptr;
    jbyte *bytes = env->GetByteArrayElements(input, 0);
    int cipherLen = env->GetArrayLength(input);

    if(cipherLen <= 0){
        return nullptr;
    }

    //将输入数组复制到C
    cipherText = new char[cipherLen];
    memset(cipherText, 0, cipherLen);
    memcpy(cipherText, bytes, cipherLen);
    cipherLen -= ivLen;

    char iv[ivLen];
    memcpy(iv, cipherText, ivLen);
    auto* plainText = (unsigned char*)malloc(sizeof(unsigned char*) * cipherLen);
    memset(plainText, 0, cipherLen);

    int status;

    //初始化AES
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    status = EVP_DecryptInit_ex(ctx, EVP_aes_256_cbc(), nullptr, sessionKey, reinterpret_cast<const unsigned char*>(iv));
    if(status != 1){
        __android_log_print(ANDROID_LOG_ERROR, TAG , "EVP_DecryptInit_ex failed!");
        EVP_CIPHER_CTX_free(ctx);
        return nullptr;
    }

    //喂数据
    int plainTextLen;
    int len;
    status = EVP_DecryptUpdate(ctx, plainText, &len, reinterpret_cast<const unsigned char*>(cipherText + ivLen), cipherLen);
    if(status != 1){
        __android_log_print(ANDROID_LOG_ERROR, TAG , "EVP_DecryptUpdate failed!");
        EVP_CIPHER_CTX_free(ctx);
        return nullptr;
    }
    plainTextLen = len;

    //结束
    status = EVP_DecryptFinal_ex(ctx, plainText + len, &len);
    if(status != 1){
        __android_log_print(ANDROID_LOG_ERROR, TAG , "EVP_DecryptFinal_ex failed!");
        EVP_CIPHER_CTX_free(ctx);
        return nullptr;
    }
    plainTextLen += len;

    EVP_CIPHER_CTX_free(ctx);
    CRYPTO_cleanup_all_ex_data();

    //__android_log_print(ANDROID_LOG_DEBUG, TAG , "AES decrypt: %s", plainText);
    //转换成jstring
    jclass     jstrObj   = env->FindClass("java/lang/String");
    jmethodID  methodId  = env->GetMethodID(jstrObj, "<init>", "([BLjava/lang/String;)V");
    jbyteArray byteArray = env->NewByteArray(plainTextLen);
    jstring    encode    = env->NewStringUTF("utf-8");
    env->SetByteArrayRegion(byteArray, 0, plainTextLen, (jbyte*)plainText);

    free(cipherText);
    return (jstring)env->NewObject(jstrObj, methodId, byteArray, encode);
}