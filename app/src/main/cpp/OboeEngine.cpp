/**
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

#include "OboeEngine.h"
#include "SoundGenerator.h"
#include "utils.h"

/**
 * Main audio engine. It is responsible for:
 *
 * - Creating a callback object which is supplied when constructing the audio stream, and will be
 * called when the stream starts
 * - Restarting the stream when user-controllable properties (Audio API, channel count etc) are
 * changed, and when the stream is disconnected (e.g. when headphones are attached)
 * - Calculating the audio latency of the stream
 *
 */
OboeEngine::OboeEngine()
        : mLatencyCallback(std::make_unique<LatencyTuningCallback>()),
        mErrorCallback(std::make_unique<DefaultErrorCallback>(*this)) {
}

double OboeEngine::getCurrentOutputLatencyMillis() {
    std::lock_guard<std::mutex> lock(mLock);
    if (!mStream) return -1.0;

    auto latencyResult = mStream->calculateLatencyMillis();
    return latencyResult ? latencyResult.value() : kDefaultLatency;
}

int64_t OboeEngine::getCurrentPositionMills() {
    std::lock_guard<std::mutex> lock(mLock);
    return mAudioSource ? mAudioSource->getCurrentPositionMills() : -1;
}

void OboeEngine::setChannelCount(int channelCount) {
    mChannelCount = channelCount;
    reopenStream();
}

void OboeEngine::setSampleRate(int sampleRate) {
    mSampleRate = sampleRate;
    reopenStream();
}

oboe::Result OboeEngine::createPlaybackStream() {
    oboe::AudioStreamBuilder builder;
    return builder.setSharingMode(oboe::SharingMode::Exclusive)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setFormat(oboe::AudioFormat::I16)
        ->setDataCallback(mLatencyCallback.get())
        ->setErrorCallback(mErrorCallback.get())
        ->setChannelCount(mChannelCount)
        ->setSampleRate(mSampleRate)
        ->openStream(mStream);
}

void OboeEngine::restart() {
    // The stream will have already been closed by the error callback.
    mLatencyCallback->reset();
    start();
}

oboe::Result OboeEngine::start() {
    std::lock_guard<std::mutex> lock(mLock);

    auto result = createPlaybackStream();
    if (result == oboe::Result::OK){
        mAudioSource =  std::make_shared<SoundGenerator>(mStream);
        mLatencyCallback->setSource(std::dynamic_pointer_cast<IRenderableAudio>(mAudioSource));
        mStream->start();

        LOGD("Stream opened: AudioAPI = %d, channelCount = %d, sampleRate = %d, deviceID = %d",
                mStream->getAudioApi(),
                mStream->getChannelCount(),
                mStream->getSampleRate(),
                mStream->getDeviceId());
    } else {
        LOGE("Error creating playback stream. Error: %s", oboe::convertToText(result));
    }
    return result;
}

void OboeEngine::stop() {
    // Stop, close and delete in case not already closed.
    std::lock_guard<std::mutex> lock(mLock);
    if (mStream) {
        mStream->stop();
        mStream->close();
        mStream.reset();
    }
}

oboe::Result OboeEngine::reopenStream() {
    stop();
    return start();
}

void OboeEngine::prepare(const std::string& filePath) {
    mAudioSource->prepare(filePath);
}

void OboeEngine::play(int64_t offsetMills, int64_t sizeMills) {
    mAudioSource->play(offsetMills, sizeMills);
}
