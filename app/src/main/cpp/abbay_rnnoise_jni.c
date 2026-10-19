// JNI bridge to RNNoise for com.mobyle.abbay.infra.dsp.nativelib.RnnNoise.
#include <jni.h>
#include <stdint.h>
#include "rnnoise.h"

JNIEXPORT jlong JNICALL
Java_com_mobyle_abbay_infra_dsp_nativelib_RnnNoise_nativeCreate(JNIEnv *env, jclass type) {
    return (jlong) (intptr_t) rnnoise_create(NULL);
}

JNIEXPORT void JNICALL
Java_com_mobyle_abbay_infra_dsp_nativelib_RnnNoise_nativeDestroy(JNIEnv *env, jclass type, jlong handle) {
    if (handle != 0) rnnoise_destroy((DenoiseState *) (intptr_t) handle);
}

JNIEXPORT jint JNICALL
Java_com_mobyle_abbay_infra_dsp_nativelib_RnnNoise_nativeFrameSize(JNIEnv *env, jclass type) {
    return rnnoise_get_frame_size();
}

// Denoises one frame of 480 samples at 48 kHz, in the 16-bit scale RNNoise expects; returns the voice probability.
JNIEXPORT jfloat JNICALL
Java_com_mobyle_abbay_infra_dsp_nativelib_RnnNoise_nativeProcess(JNIEnv *env, jclass type, jlong handle, jfloatArray input,
                                                          jfloatArray output) {
    if (handle == 0) return 0.0f;
    float in[480];
    float out[480];
    (*env)->GetFloatArrayRegion(env, input, 0, 480, in);
    float probability = rnnoise_process_frame((DenoiseState *) (intptr_t) handle, out, in);
    (*env)->SetFloatArrayRegion(env, output, 0, 480, out);
    return probability;
}
