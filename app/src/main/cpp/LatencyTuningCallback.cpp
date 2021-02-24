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

#include "LatencyTuningCallback.h"

oboe::DataCallbackResult LatencyTuningCallback::onAudioReady(
     oboe::AudioStream *oboeStream, void *audioData, int32_t numFrames) {
    if (oboeStream != mStream) {
        mStream = oboeStream;
        mLatencyTuner = std::make_unique<oboe::LatencyTuner>(*oboeStream);
    }
    if (mBufferTuneEnabled
            && mLatencyTuner
            && oboeStream->getAudioApi() == oboe::AudioApi::AAudio) {
        mLatencyTuner->tune();
    }

    auto result = DefaultDataCallback::onAudioReady(oboeStream, audioData, numFrames);
    return result;
}
