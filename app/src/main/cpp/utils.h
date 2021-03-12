#pragma once

#include <oboe/Oboe.h>

constexpr double kDefaultLatency = 120; //ms

inline double millsNow() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(steady_clock::now().time_since_epoch()).count();
}

inline double bytesPerMillisecond(const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    return oboeStream->getBytesPerFrame() * oboeStream->getSampleRate() / 1000.0;
}

inline int64_t millsToBytes(double mills, const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    int64_t bytes = mills * bytesPerMillisecond(oboeStream);
    bytes = bytes / oboeStream->getBytesPerFrame() * oboeStream->getBytesPerFrame(); // align value

    return bytes;
}

inline int64_t millsToSamples(double mills, const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    return millsToBytes(mills, oboeStream) / oboeStream->getBytesPerSample();
}

inline int64_t millsToFrames(double mills, const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    return millsToBytes(mills, oboeStream) / oboeStream->getBytesPerFrame();
}

inline double samplesToMills(int64_t samples, const std::shared_ptr<oboe::AudioStream>& oboeStream) {
    int64_t bytes = samples * oboeStream->getBytesPerSample();
    double mills = bytes / bytesPerMillisecond(oboeStream);
    return mills;
}
