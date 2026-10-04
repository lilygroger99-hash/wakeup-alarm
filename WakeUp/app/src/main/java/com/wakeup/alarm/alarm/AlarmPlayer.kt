package com.wakeup.alarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.wakeup.alarm.util.SoundUtils
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays the looping alarm sound and vibration. Created by [AlarmService] when an alarm rings and released when it
 * is stopped, snoozed or times out.
 *
 * Sound fallback chain (a missing sound never results in a silent alarm):
 *   chosen sound -> default alarm -> default notification -> default ringtone -> generated beep pattern.
 */
class AlarmPlayer(private val context: Context) {

    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private var mediaPlayer: MediaPlayer? = null
    private var beepTrack: AudioTrack? = null
    private var focusRequest: AudioFocusRequest? = null
    private var released = false

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun start(soundUri: String, vibrate: Boolean) {
        if (released) return
        requestAudioFocus()
        startSound(soundUri)
        if (vibrate) startVibration()
    }

    fun release() {
        released = true
        stopSound()
        stopVibration()
        abandonAudioFocus()
    }

    // ---- sound -------------------------------------------------------------------------------------------------

    private fun startSound(soundUri: String) {
        for (uri in SoundUtils.playbackCandidates(soundUri)) {
            if (tryPlay(uri)) return
        }
        Log.w(TAG, "No playable system sound found; using generated beep")
        startBeep()
    }

    private fun tryPlay(uri: Uri): Boolean {
        val player = MediaPlayer()
        return try {
            player.setAudioAttributes(audioAttributes)
            player.setDataSource(context, uri)
            player.isLooping = true
            player.setOnErrorListener { failed, what, extra ->
                Log.w(TAG, "MediaPlayer error what=$what extra=$extra; switching to beep")
                if (mediaPlayer === failed) {
                    mediaPlayer = null
                    runCatching { failed.release() }
                    if (!released) startBeep()
                }
                true
            }
            player.prepare()
            player.start()
            mediaPlayer = player
            true
        } catch (e: Exception) {
            // Missing file, revoked permission, bad format, ...: try the next candidate.
            Log.w(TAG, "Cannot play $uri", e)
            runCatching { player.release() }
            false
        }
    }

    private fun startBeep() {
        if (beepTrack != null) return
        try {
            val pcm = buildBeepPattern()
            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setLoopPoints(0, pcm.size, -1)
            track.play()
            beepTrack = track
        } catch (e: Exception) {
            Log.e(TAG, "Fallback beep failed", e)
        }
    }

    /** Four short 880 Hz beeps followed by a pause, 1.6 s in total. Generated in code: no audio files needed. */
    private fun buildBeepPattern(): ShortArray {
        val total = (SAMPLE_RATE * 1.6).toInt()
        val pcm = ShortArray(total)
        val beepSamples = (SAMPLE_RATE * 0.15).toInt()
        val fade = (SAMPLE_RATE * 0.005).toInt()
        for (beep in 0 until 4) {
            val start = (SAMPLE_RATE * 0.25 * beep).toInt()
            for (i in 0 until beepSamples) {
                val envelope = when {
                    i < fade -> i / fade.toDouble()
                    i > beepSamples - fade -> (beepSamples - i) / fade.toDouble()
                    else -> 1.0
                }
                val sample = sin(2.0 * PI * 880.0 * i / SAMPLE_RATE) * envelope * 0.6
                pcm[start + i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return pcm
    }

    private fun stopSound() {
        mediaPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
        beepTrack?.let { track ->
            runCatching { track.stop() }
            runCatching { track.release() }
        }
        beepTrack = null
    }

    // ---- vibration ---------------------------------------------------------------------------------------------

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        try {
            val vibrator = vibrator()?.takeIf { it.hasVibrator() } ?: return
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0)
            // The AudioAttributes overload marks this as an ALARM vibration so it is not muted like touch feedback.
            vibrator.vibrate(effect, audioAttributes)
        } catch (e: Exception) {
            Log.w(TAG, "Vibration unavailable", e)
        }
    }

    private fun stopVibration() {
        runCatching { vibrator()?.cancel() }
    }

    // ---- audio focus -------------------------------------------------------------------------------------------

    private fun requestAudioFocus() {
        try {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener { }
                .build()
            audioManager?.requestAudioFocus(request)
            focusRequest = request
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed", e)
        }
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { request -> runCatching { audioManager?.abandonAudioFocusRequest(request) } }
        focusRequest = null
    }

    private companion object {
        const val TAG = "AlarmPlayer"
        const val SAMPLE_RATE = 44_100
    }
}
