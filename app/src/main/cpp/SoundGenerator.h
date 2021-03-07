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
    ~SoundGenerator() = default;

    void prepare(const std::string& filePath);
    void play(int64_t offsetMills, int64_t sizeMills);
    void renderAudio(float *audioData, int32_t numFrames) override;

    int64_t getMillsSkippedOnStart() { return mMillsSkippedOnStart; }
    int64_t getEmptyFrameWritten() { return mEmptyFramesWritten; }
    int64_t getCurrentPositionMills();
private:
    const std::shared_ptr<oboe::AudioStream> mStream;
    std::unique_ptr<char[]> mBuffer;

    std::atomic<double> mStartTimestamp {0};
    std::atomic_int64_t mStartPosition {0};
    std::atomic_int64_t mStartOffsetMills {0};
    std::atomic_int64_t mMillsSkippedOnStart {0};
    std::atomic_int64_t mSizeMills {0};

    std::atomic_int64_t mPosition {0};
    std::atomic_int64_t mSizeSamples {0};
    std::atomic_int64_t mEmptyFramesWritten {0};
    std::atomic_int64_t mTotalPatchBytes {0};

    std::atomic_bool mJustStarted {false};
    std::atomic_bool mIsPlaying {false};
};

#endif //SAMPLES_SOUNDGENERATOR_H
