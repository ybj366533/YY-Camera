#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring

JNICALL
Java_com_ybj366533_yy_1camera_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
