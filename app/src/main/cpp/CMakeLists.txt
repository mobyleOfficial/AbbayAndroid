cmake_minimum_required(VERSION 3.22.1)
project(abbay_rnnoise C)

# RNNoise with its built-in model, plus a small JNI bridge. The only native code in the project.
add_library(abbay_rnnoise SHARED
        abbay_rnnoise_jni.c
        rnnoise/src/denoise.c
        rnnoise/src/rnn.c
        rnnoise/src/rnn_data.c
        rnnoise/src/pitch.c
        rnnoise/src/kiss_fft.c
        rnnoise/src/celt_lpc.c)

target_include_directories(abbay_rnnoise PRIVATE rnnoise/include rnnoise/src)
# -O3 also in debug builds: playback must keep up (CPU budget). No -ffast-math: RNNoise refuses it (NaN safety).
target_compile_options(abbay_rnnoise PRIVATE -O3 -Wno-unused-parameter)
target_link_libraries(abbay_rnnoise m)
# 16 KB page alignment for Android 15+ devices.
target_link_options(abbay_rnnoise PRIVATE "-Wl,-z,max-page-size=16384")
