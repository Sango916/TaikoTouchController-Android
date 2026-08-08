package com.example

import android.content.Context

/**
 * No-op Audio Player stub.
 * Audio playback functionality has been removed to ensure zero latency overhead
 * and eliminate native SoundPool/MediaExtractor logs.
 */
class TaikoAudioPlayer(context: Context? = null) {
    fun playDon(volume: Float = 1.0f) {}
    fun playKat(volume: Float = 1.0f) {}
    fun release() {}
}
