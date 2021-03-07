#pragma once

#include <oboe/Oboe.h>

constexpr double kDefaultLatency = 120; //ms

inline double millsNow() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(steady_clock::now().time_since_epoch()).count();
}

inline int64_t millsToBytes(int64_t mills, const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    const int64_t kBytesPerMillisecond = oboeStream->getBytesPerFrame() * oboeStream->getSampleRate() / 1000;

    int64_t bytes = mills * kBytesPerMillisecond;
    bytes = bytes / oboeStream->getBytesPerFrame() * oboeStream->getBytesPerFrame(); // align value

    return bytes;
}

inline int64_t millsToSamples(int64_t mills, const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    return millsToBytes(mills, oboeStream) / oboeStream->getBytesPerSample();
}

inline int64_t millsToFrames(int64_t mills, const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    return millsToBytes(mills, oboeStream) / oboeStream->getBytesPerFrame();
}

inline int64_t bytesToMills(int64_t bytes, const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    const int64_t kBytesPerMillisecond = oboeStream->getBytesPerFrame() * oboeStream->getSampleRate() / 1000;

    int64_t mills = bytes / kBytesPerMillisecond;
    return mills;
}
