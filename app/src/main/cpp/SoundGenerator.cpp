/*
 * Copyright 2018 The Android Open Source Project
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

#include "SoundGenerator.h"
#include "logging_macros.h"
#include "utils.h"

SoundGenerator::SoundGenerator(std::shared_ptr<oboe::AudioStream> oboeStream)
        : mStream(std::move(oboeStream)) {}

void SoundGenerator::renderAudio(float *audioData, int32_t numFrames) {
    if (!mIsPlaying) {
        memset(audioData, 0, numFrames * mStream->getBytesPerFrame());
        mFakeFramesWritten += numFrames;
        return;
    }

    int64_t startPlayTimestamp = mStartPlayTimestamp.exchange(0);
    if (startPlayTimestamp > 0) {// first render after call play()
        auto latencyResult = mStream->calculateLatencyMillis();
        double startLatency = latencyResult ? latencyResult.value() : kDefaultLatency;
        LOGD("startLatency: %f", latencyResult.value());
        double startDelay = millsNow() - startPlayTimestamp;
        LOGD("startDelay: %f", startDelay);
        int64_t millsToSkip = startDelay + startLatency;
        LOGD("millsToSkip: %ld", millsToSkip);
        int64_t samplesToSkip = millsToSamples(millsToSkip, mStream);
        LOGD("samplesToSkip: %ld", samplesToSkip);
        mPosition += samplesToSkip;
        mMillsSkippedOnStartStartDelay = millsToSkip;
    }

    auto buffer = reinterpret_cast<int16_t*>(audioData);
    auto source = reinterpret_cast<int16_t*>(mBuffer.get());

    int channelCount = mStream->getChannelCount();

    for (int j = 0; j < numFrames; ++j) {
        for (int i = 0; i < channelCount; ++i) {
            buffer[(j * channelCount) + i] = source[mPosition]; // TODO: memcpy?
            mPosition = ++mPosition % mSize;
        }
    }
}

void SoundGenerator::prepare(const std::string& filePath) {
    FILE *fp = fopen(filePath.c_str(), "r");

    fseek(fp, 0L, SEEK_END);
    long size = ftell(fp);

    auto buf = std::make_unique<char[]>(size);
    mBuffer.swap(buf);

    fseek(fp, 0L, SEEK_SET);
    fread(mBuffer.get(), 1, size, fp);

    fclose(fp);
}

void SoundGenerator::play(int64_t offsetSamples, int64_t sizeSamples) {
    mSize = sizeSamples;
    mPosition = offsetSamples;
    mStartPlayTimestamp = millsNow();
    mIsPlaying = true;
}
