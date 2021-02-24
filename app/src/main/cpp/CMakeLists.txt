#
# Copyright 2017 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cmake_minimum_required(VERSION 3.4.1)

# App specific sources
set (APP_SOURCES
    jni_bridge.cpp
    OboeEngine.cpp
    SoundGenerator.cpp
    LatencyTuningCallback.cpp
)

# Build the peremenfm library
add_library(peremenfm SHARED
            ${DEBUG_UTILS_SOURCES}
            ${APP_SOURCES}
            )

# Specify the libraries needed for peremenfm
find_package (oboe REQUIRED CONFIG)
target_link_libraries(peremenfm android log oboe::oboe)

#target_link_libraries(peremenfm android log oboe)

# Enable optimization flags: if having problems with source level debugging,
# disable -Ofast ( and debug ), re-enable after done debugging.
target_compile_options(peremenfm PRIVATE -Wall -Werror "$<$<CONFIG:RELEASE>:-Ofast>")
