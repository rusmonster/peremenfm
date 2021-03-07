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

static constexpr int64_t kHardSyncThresholdMills = 200;
static constexpr int64_t kSoftSyncThresholdMills = 2;
static constexpr int64_t kSoftSyncIntervalFrames = 20;

SoundGenerator::SoundGenerator(std::shared_ptr<oboe::AudioStream> oboeStream)
        : mStream(std::move(oboeStream)) {}

void SoundGenerator::renderAudio(float *audioData, int32_t numFrames) {
    if (!mIsPlaying) {
        memset(audioData, 0, numFrames * mStream->getBytesPerFrame());
        mEmptyFramesWritten += numFrames;
        return;
    }

    if (mJustStarted.exchange(false)) { // first render after call play()
        auto latencyResult = mStream->calculateLatencyMillis();
        double startLatency = latencyResult ? latencyResult.value() : kDefaultLatency;
        LOGD("startLatency: %f", latencyResult.value());
        double startDelay = millsNow() - mStartTimestamp;
        LOGD("startDelay: %f", startDelay);
        int64_t millsToSkip = startDelay + startLatency;
        LOGD("millsToSkip: %ld", millsToSkip);
        int64_t samplesToSkip = millsToSamples(millsToSkip, mStream);
        LOGD("samplesToSkip: %ld", samplesToSkip);
        mPosition += samplesToSkip;
        mMillsSkippedOnStart = millsToSkip;
    }

    int64_t millsSinceStart = millsNow() - mStartTimestamp;
    int64_t estimatedOffsetMills = (mStartOffsetMills + millsSinceStart) % mSizeMills;
    int64_t synchronizationOffsetMills = estimatedOffsetMills - getCurrentPositionMills();
    int64_t synchronizationPatchBytes = 0;

    if (abs(synchronizationOffsetMills) > kHardSyncThresholdMills) {
        LOGD("synchronization: hard shift: %ld", synchronizationOffsetMills);
        int64_t patchBytes = millsToBytes(synchronizationOffsetMills, mStream);
        mPosition += patchBytes;
        mTotalPatchBytes += patchBytes;
    } else if (abs(synchronizationOffsetMills) > kSoftSyncThresholdMills) {
        // soft adjust
        int64_t synchronizationPatchSamples = synchronizationOffsetMills > 0 ? 1 : -1;
        synchronizationPatchBytes = synchronizationPatchSamples * mStream->getBytesPerSample();
    }

    static int k = 0;
    if (++k % 100 == 0) {
        LOGD("synchronizationOffsetMills: %ld", synchronizationOffsetMills);
    }

    auto buffer = reinterpret_cast<int16_t*>(audioData);
    auto source = reinterpret_cast<int16_t*>(mBuffer.get());

    int channelCount = mStream->getChannelCount();

    for (int j = 0; j < numFrames; ++j) {
        for (int i = 0; i < channelCount; ++i) {
            buffer[(j * channelCount) + i] = source[mPosition];
            mPosition = ++mPosition % mSizeSamples;
        }

        if (synchronizationPatchBytes != 0 && j % kSoftSyncIntervalFrames == 0) {
            LOGD("synchronization: soft shift: %ld", synchronizationPatchBytes);
            mPosition = (mPosition + synchronizationPatchBytes) % mSizeSamples;
            mTotalPatchBytes += synchronizationPatchBytes;
        }
    }
}

int64_t SoundGenerator::getCurrentPositionMills() {
    auto latencyResult = mStream->calculateLatencyMillis();
    int64_t latencyMills = latencyResult ? latencyResult.value() : kDefaultLatency;
    int64_t latencyFrames = millsToFrames(latencyMills, mStream);

    int64_t audioFramesWritten = mStream->getFramesWritten() - getEmptyFrameWritten() - latencyFrames;
    int64_t writtenMills = audioFramesWritten * 1000 / mStream->getSampleRate();
    int64_t patchMills = bytesToMills(mTotalPatchBytes, mStream);
    int64_t playedMills = mStartOffsetMills + writtenMills + getMillsSkippedOnStart() + patchMills;
    int64_t currentPositionMills = (mSizeMills > 0) ? playedMills % mSizeMills : playedMills;

    //int64_t millsSinceStart = millsNow() - mStartTimestamp;
    // The idea is that (millsSinceStart == millsSkippedOnStart) at the moment when (writtenMills == 0)
    //LOGD("writtenMills: %ld; fakeFrames: %06ld; millsSinceStart: %ld; skippedOnStart: %ld", writtenMills, getEmptyFrameWritten(), millsSinceStart, getMillsSkippedOnStart());

    return currentPositionMills;
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

void SoundGenerator::play(int64_t offsetMills, int64_t sizeMills) {
    mStartTimestamp = millsNow();
    mStartOffsetMills = offsetMills;
    mSizeMills = sizeMills;

    int64_t offsetSamples = millsToSamples(offsetMills, mStream);
    int64_t sizeSamples = millsToSamples(sizeMills, mStream);

    mStartPosition = offsetSamples;
    mPosition = offsetSamples;
    mSizeSamples = sizeSamples;

    mJustStarted = true;
    mIsPlaying = true;
}
