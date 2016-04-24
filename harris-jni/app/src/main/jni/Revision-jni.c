#include <jni.h>

JNIEXPORT jstring JNICALL
Java_org_v4vision_reVision_harrisjava_JavaActivity_getMsgFromJni(JNIEnv *env, jobject instance) {

    // TODO


    return (*env)->NewStringUTF(env, "Hello from other side");
}

JNIEXPORT jint JNICALL
Java_org_v4vision_reVision_harrisjava_JavaActivity_grayscale(JNIEnv *env, jobject instance,
                                                             jbyteArray frame_) {
    jbyte *frame = (*env)->GetByteArrayElements(env, frame_, 0);

    // TODO
    jint count = (*env)->GetArrayLength(env,frame_);

    (*env)->ReleaseByteArrayElements(env, frame_, frame, 0);

    return count;
}