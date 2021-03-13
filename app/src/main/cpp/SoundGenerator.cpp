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
static constexpr int64_t kSoftSyncIntervalFrames = 50;

SoundGenerator::SoundGenerator(std::shared_ptr<oboe::AudioStream> oboeStream)
        : mStream(std::move(oboeStream)) {}

void SoundGenerator::renderAudio(int16_t *audioData, int32_t numFrames) {
    if (!mIsPlaying) {
        memset(audioData, 0, numFrames * mStream->getBytesPerFrame());
        mEmptyFramesWritten += numFrames;
        return;
    }

    int64_t millsSinceStart = millsNow() - mStartTimestamp;
    int64_t estimatedOffsetMills = (mStartOffsetMills + millsSinceStart + mPlaybackShiftMills) % mSizeMills;

    int64_t synchronizationOffsetMillsForward = (estimatedOffsetMills - getCurrentPositionMills()) % mSizeMills;
    int64_t synchronizationOffsetMillsBackward = (synchronizationOffsetMillsForward + mSizeMills) % mSizeMills;

    // (estimatedOffsetMills - getCurrentPositionMills()) could be big negative as almost -mSizeMills when passing zero position.
    // So we add extra mSizeMills here to avoid unnecessary hard synchronizations.
    int64_t synchronizationOffsetMills = abs(synchronizationOffsetMillsForward) < abs(synchronizationOffsetMillsBackward)
            ? synchronizationOffsetMillsForward
            : synchronizationOffsetMillsBackward;

    int64_t synchronizationPatchSamples = 0;

//    static int k = 0;
//    if (k++ % 100 == 0) {
//        LOGD("synchronizationOffsetMills: %ld", synchronizationOffsetMills);
//    }

    bool isJustStarted = mIsJustStarted.exchange(false);
    if (isJustStarted) {
        mSizeSamples = millsToSamples(mSizeMills, mStream);
        mPositionSamples = millsToSamples(mStartOffsetMills, mStream);
    }

    if (isJustStarted || abs(synchronizationOffsetMills) > kHardSyncThresholdMills) {
        LOGD("synchronization: hard shift: %ld", synchronizationOffsetMills);
        int64_t patchSamples = millsToSamples(synchronizationOffsetMills, mStream);
        updatePosition(mPositionSamples + patchSamples);
        mTotalPatchSamples += patchSamples;
    } else if (abs(synchronizationOffsetMills) > kSoftSyncThresholdMills) {
        // soft adjust
        synchronizationPatchSamples = synchronizationOffsetMills > 0 ? 1 : -1;
    }

    auto source = reinterpret_cast<int16_t*>(mBuffer.get());

    int channelCount = mStream->getChannelCount();

    for (int j = 0; j < numFrames; ++j) {
        for (int i = 0; i < channelCount; ++i) {
            audioData[(j * channelCount) + i] = source[mPositionSamples];
            updatePosition(mPositionSamples + 1);
        }

        if (synchronizationPatchSamples != 0 && j % kSoftSyncIntervalFrames == 0) {
            updatePosition(mPositionSamples + synchronizationPatchSamples);
            mTotalPatchSamples += synchronizationPatchSamples;
        }
    }
}

int64_t SoundGenerator::getCurrentPositionMills() {
    auto latencyResult = mStream->calculateLatencyMillis();
    double latencyMills = latencyResult ? latencyResult.value() : kDefaultLatency;
    int64_t latencyFrames = millsToFrames(latencyMills, mStream);

    int64_t audioFramesWritten = mStream->getFramesWritten() - mEmptyFramesWritten - latencyFrames;
    int64_t writtenMills = audioFramesWritten * 1000 / mStream->getSampleRate();
    int64_t patchMills = samplesToMills(mTotalPatchSamples, mStream);
    int64_t playedMills = mStartOffsetMills + writtenMills + patchMills;
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

    mIsJustStarted = true;
    mIsPlaying = true;
}

void SoundGenerator::setPlaybackShift(int64_t playbackShiftMills) {
    LOGD("setPlaybackShift: %ld; old: %ld; diff: %ld", playbackShiftMills, mPlaybackShiftMills.load(), playbackShiftMills - mPlaybackShiftMills);
    mPlaybackShiftMills = playbackShiftMills;
}

int64_t SoundGenerator::getTotalPatchMills() {
    return samplesToMills(mTotalPatchSamples, mStream);
}

void SoundGenerator::updatePosition(int64_t positionSamples) {
    mPositionSamples = positionSamples % mSizeSamples;
    if (mPositionSamples < 0) {
        mPositionSamples += mSizeSamples;
    }
}
