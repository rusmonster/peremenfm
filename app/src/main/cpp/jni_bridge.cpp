/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <codecvt>
#include <oboe/Oboe.h>
#include "OboeEngine.h"
#include "logging_macros.h"

#define JNI_METHOD_NAME_(NAME) Java_fm_peremen_android_PlaybackEngine_##NAME

// Given a (UTF-16) jstring return a new UTF-8 native string.
static std::string StdStringFromJstring(JNIEnv* jni, jstring const& j_string)
{
    std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> convert;

    const jchar* jchars = jni->GetStringChars(j_string, NULL);
    size_t len = jni->GetStringLength(j_string);

    auto str = convert.to_bytes(std::u16string(reinterpret_cast<char16_t const*>(jchars), len));
    jni->ReleaseStringChars(j_string, jchars);
    return str;
}

extern "C" {

/**
 * Creates the audio engine
 *
 * @return a pointer to the audio engine. This should be passed to other methods
 */
JNIEXPORT jlong JNICALL
JNI_METHOD_NAME_(native_1createEngine)(
        JNIEnv *env,
        jclass /*unused*/) {
    // We use std::nothrow so `new` returns a nullptr if the engine creation fails
    OboeEngine *engine = new(std::nothrow) OboeEngine();
    if (engine == nullptr) {
        LOGE("Could not instantiate OboeEngine");
        return 0;
    }
    auto result = engine->start();
    if (result != oboe::Result::OK) {
        LOGE("Opening and starting stream failed. Returned %d", result);
        engine->stop();
        delete engine;
        return 0;
    }
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
JNI_METHOD_NAME_(native_1deleteEngine)(
        JNIEnv *env,
        jclass,
        jlong engineHandle) {

    OboeEngine *engine = reinterpret_cast<OboeEngine *>(engineHandle);
    engine->stop();
    delete engine;
}

JNIEXPORT jlong JNICALL
JNI_METHOD_NAME_(native_1getCurrentPositionMillis)(
        JNIEnv *env,
        jclass,
        jlong engineHandle) {

    OboeEngine *engine = reinterpret_cast<OboeEngine*>(engineHandle);
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return static_cast<jlong>(-1);
    }
    return static_cast<jlong>(engine->getCurrentPositionMills());
}

JNIEXPORT jlong JNICALL
JNI_METHOD_NAME_(native_1getTotalPatchMills)(
        JNIEnv *env,
        jclass,
        jlong engineHandle) {

    OboeEngine *engine = reinterpret_cast<OboeEngine*>(engineHandle);
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return static_cast<jlong>(-1);
    }
    return static_cast<jlong>(engine->getTotalPatchMills());
}

JNIEXPORT jdouble JNICALL
JNI_METHOD_NAME_(native_1getCurrentOutputLatencyMillis)(
        JNIEnv *env,
        jclass,
        jlong engineHandle) {

    OboeEngine *engine = reinterpret_cast<OboeEngine*>(engineHandle);
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return static_cast<jdouble>(-1.0);
    }
    return static_cast<jdouble>(engine->getCurrentOutputLatencyMillis());
}

JNIEXPORT void JNICALL
JNI_METHOD_NAME_(native_1setDefaultStreamValues)(
        JNIEnv *env,
        jclass type,
        jint sampleRate,
        jint channelCount,
        jint framesPerBurst) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) sampleRate;
    oboe::DefaultStreamValues::ChannelCount = (int32_t) channelCount;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) framesPerBurst;
}

JNIEXPORT void JNICALL
JNI_METHOD_NAME_(native_1prepare)(
        JNIEnv *env,
        jclass type,
        jlong engineHandle,
        jstring jfilePath) {
    std::string filePath = StdStringFromJstring(env, jfilePath);
    LOGD("prepare: %s", filePath.c_str());

    OboeEngine *engine = reinterpret_cast<OboeEngine*>(engineHandle);
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }

    engine->prepare(filePath);
}

JNIEXPORT void JNICALL
JNI_METHOD_NAME_(native_1play)(
        JNIEnv *env,
        jclass type,
        jlong engineHandle,
        jlong offset,
        jlong size) {
    LOGD("play: %ld", offset);

    OboeEngine *engine = reinterpret_cast<OboeEngine*>(engineHandle);
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->play(offset, size);
}

JNIEXPORT void JNICALL
JNI_METHOD_NAME_(native_1setPlaybackShift)(
        JNIEnv *env,
        jclass type,
        jlong engineHandle,
        jlong playbackShift) {

    OboeEngine *engine = reinterpret_cast<OboeEngine*>(engineHandle);
    if (engine == nullptr) {
        LOGE("Engine is null, you must call createEngine before calling this method");
        return;
    }
    engine->setPlaybackShift(playbackShift);
}

} // extern "C"
