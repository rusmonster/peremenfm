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

#ifndef SAMPLES_SOUNDGENERATOR_H
#define SAMPLES_SOUNDGENERATOR_H


#include <oboe/AudioStream.h>
#include "IRenderableAudio.h"

class SoundGenerator : public IRenderableAudio {
public:
    SoundGenerator(std::shared_ptr<oboe::AudioStream> oboeStream);

    void prepare(const std::string& filePath);
    void play(int64_t offsetMills, int64_t sizeMills);
    void setPlaybackShift(int64_t playbackShiftMills);

    void renderAudio(int16_t *audioData, int32_t numFrames) override;

    int64_t getTotalPatchMills();
    int64_t getCurrentPositionMills();

private:
    void updatePosition(int64_t positionSamples);

private:
    const std::shared_ptr<oboe::AudioStream> mStream;
    std::unique_ptr<char[]> mBuffer;

    int64_t mSizeSamples {0};
    int64_t mPositionSamples {0};

    std::atomic<double> mStartTimestamp {0};
    std::atomic_int64_t mStartOffsetMills {0};
    std::atomic_int64_t mSizeMills {0};

    std::atomic_int64_t mEmptyFramesWritten {0};
    std::atomic_int64_t mTotalPatchSamples {0};
    std::atomic_int64_t mPlaybackShiftMills {0};

    std::atomic_bool mIsJustStarted {false};
    std::atomic_bool mIsPlaying {false};
};

#endif //SAMPLES_SOUNDGENERATOR_H
