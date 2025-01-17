#include <sys/socket.h>
#include <android/hardware_buffer.h>
#include <android/log.h>
#include <jni.h>
#include <unistd.h>
#include <android/hardware_buffer_jni.h>

/* Part of implementation has been taken from
twaik */

JNIEXPORT jlong JNICALL
Java_com_winlator_xserver_extensions_DRI3Extension_hardwareBufferFromSocket(JNIEnv *env, jclass obj, jint fd) {
    AHardwareBuffer *ahb;
    
    uint8_t buf = 1;
    write(fd, &buf, 1);
    
    AHardwareBuffer_recvHandleFromUnixSocket(fd, &ahb);
    
    return (jlong)ahb;
}

JNIEXPORT jobject JNICALL
Java_com_winlator_xserver_extensions_DRI3Extension_hardwareBufferToBuffer(JNIEnv *env, jclass obj, jlong hardwareBufferPtr) {
    AHardwareBuffer *ahb = (AHardwareBuffer *)hardwareBufferPtr;
    AHardwareBuffer_Desc desc;
    void *addr;
    
    AHardwareBuffer_describe(ahb, &desc);
    
    jlong size = desc.width * desc.height * 4;
    
    AHardwareBuffer_lock(ahb, AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN, -1, NULL, &addr);
    
    jobject buffer = (*env)->NewDirectByteBuffer(env, addr, size);
    
    return buffer;
}

JNIEXPORT jint JNICALL
Java_com_winlator_xserver_extensions_DRI3Extension_getHardwareBufferStride(JNIEnv *env, jclass obj, jlong hardwareBufferPtr) {
    AHardwareBuffer *ahb = (AHardwareBuffer *)hardwareBufferPtr;
    AHardwareBuffer_Desc desc;
    
    AHardwareBuffer_describe(ahb, &desc);
    
    int stride = desc.stride;
    
    return (jint)stride;
}

JNIEXPORT jint JNICALL
Java_com_winlator_xserver_extensions_DRI3Extension_getHardwareBufferHeight(JNIEnv *env, jclass obj, jlong hardwareBufferPtr) {
    AHardwareBuffer *ahb = (AHardwareBuffer *)hardwareBufferPtr;
    AHardwareBuffer_Desc desc;
    
    AHardwareBuffer_describe(ahb, &desc);
    
    int height = desc.height;
    
    return (jint)height;
}

JNIEXPORT jint JNICALL
Java_com_winlator_xserver_extensions_DRI3Extension_getHardwareBufferWidth(JNIEnv *env, jclass obj, jlong hardwareBufferPtr) {
    AHardwareBuffer *ahb = (AHardwareBuffer *)hardwareBufferPtr;
    AHardwareBuffer_Desc desc;
    
    AHardwareBuffer_describe(ahb, &desc);
    
    int width = desc.width;
    
    return (jint)width;
}