package fm.peremen.android;
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

import android.content.Context;
import android.media.AudioManager;

public class PlaybackEngine {

    static long mEngineHandle = 0;

    static {
        System.loadLibrary("peremenfm");
    }

    static boolean create(){

        if (mEngineHandle == 0){
            mEngineHandle = native_createEngine();
        }
        return (mEngineHandle != 0);
    }

    static void setDefaultStreamValues(Context context, int defaultSampleRate, int defaultChannelCount) {
        AudioManager myAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        String framesPerBurstStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int defaultFramesPerBurst = Integer.parseInt(framesPerBurstStr);

        native_setDefaultStreamValues(defaultSampleRate, defaultChannelCount, defaultFramesPerBurst);
    }

    static void delete(){
        if (mEngineHandle != 0){
            native_deleteEngine(mEngineHandle);
        }
        mEngineHandle = 0;
    }

    static void prepare(String filePath) {
        if (mEngineHandle == 0) return;
        native_prepare(mEngineHandle, filePath);
    }

    static void play(long offset, long size) {
        if (mEngineHandle == 0) return;
        native_play(mEngineHandle, offset, size);
    }

    static void setPlaybackShift(long playbackShift) {
        if (mEngineHandle == 0) return;
        native_setPlaybackShift(mEngineHandle, playbackShift);
    }

    static long getCurrentPositionMillis(){
        if (mEngineHandle == 0) return 0;
        return native_getCurrentPositionMillis(mEngineHandle);
    }

    static long getTotalPathMills(){
        if (mEngineHandle == 0) return 0;
        return native_getTotalPatchMills(mEngineHandle);
    }

    static double getCurrentOutputLatencyMillis(){
        if (mEngineHandle == 0) return 0;
        return native_getCurrentOutputLatencyMillis(mEngineHandle);
    }

    private static native long native_createEngine();
    private static native void native_deleteEngine(long engineHandle);
    private static native long native_getCurrentPositionMillis(long engineHandle);
    private static native long native_getTotalPatchMills(long engineHandle);
    private static native double native_getCurrentOutputLatencyMillis(long engineHandle);
    private static native void native_setDefaultStreamValues(int sampleRate, int channelCount, int framesPerBurst);
    private static native void native_prepare(long engineHandle, String filePath);
    private static native void native_play(long engineHandle, long offset, long size);
    private static native void native_setPlaybackShift(long engineHandle, long playbackShift);
}
