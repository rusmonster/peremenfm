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

#ifndef OBOE_ENGINE_H
#define OBOE_ENGINE_H

#include <oboe/Oboe.h>

#include "SoundGenerator.h"
#include "LatencyTuningCallback.h"
#include "IRestartable.h"
#include "DefaultErrorCallback.h"

class OboeEngine : public IRestartable {

public:
    OboeEngine();

    virtual ~OboeEngine() = default;

    /**
     * Open and start a stream.
     * @return error or OK
     */
    oboe::Result start();

    /**
     * Stop and close the stream.
     */
    void stop();

    // From IRestartable
    void restart() override;

    /**
     * Calculate the current latency between writing a frame to the output stream and
     * the same frame being presented to the audio hardware.
     *
     * Here's how the calculation works:
     *
     * 1) Get the time a particular frame was presented to the audio hardware
     * @see AudioStream::getTimestamp
     * 2) From this extrapolate the time which the *next* audio frame written to the stream
     * will be presented
     * 3) Assume that the next audio frame is written at the current time
     * 4) currentLatency = nextFramePresentationTime - nextFrameWriteTime
     *
     * @return  Output Latency in Milliseconds
     */
    double getCurrentOutputLatencyMillis();

    int64_t getCurrentPositionMills();
    int64_t getTotalPatchMills() { return mAudioSource->getTotalPatchMills(); }

    void prepare(const std::string& filePath) { mAudioSource->prepare(filePath); }
    void play(int64_t offsetMills, int64_t sizeMills) { mAudioSource->play(offsetMills, sizeMills); }
    void setPlaybackShift(int64_t playbackShiftMills) { mAudioSource->setPlaybackShift(playbackShiftMills); }

private:
    oboe::Result createPlaybackStream();

    std::shared_ptr<oboe::AudioStream> mStream;
    std::unique_ptr<LatencyTuningCallback> mLatencyCallback;
    std::unique_ptr<DefaultErrorCallback> mErrorCallback;
    std::shared_ptr<SoundGenerator> mAudioSource;

    int32_t        mChannelCount = oboe::Unspecified;
    int32_t        mSampleRate = oboe::kUnspecified;

    std::mutex     mLock;
};

#endif //OBOE_ENGINE_H
