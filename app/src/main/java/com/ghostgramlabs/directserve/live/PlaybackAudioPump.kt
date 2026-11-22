package com.ghostgramlabs.directserve.live

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import com.ghoststream.core.model.LiveAudioStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/** Device-playback PCM source for WebRTC external audio input; never opens a microphone. */
@RequiresApi(Build.VERSION_CODES.Q)
class PlaybackAudioPump(
    private val projection: MediaProjection,
    val sampleRate: Int,
    val channelCount: Int,
    private val onStatus: (LiveAudioStatus, String) -> Unit,
    private val onActivity: () -> Unit,
    private val logger: (String, Throwable?) -> Unit = { _, _ -> },
) {
    @Volatile
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var reusableBuffer: ByteBuffer? = null

    @Volatile
    private var activityReported: Boolean = false

    private val readLock = Any()
    private var recording = false
    private var framesReadTotal = 0L
    private val audioTimestamp = android.media.AudioTimestamp()

    @Volatile
    var lastServedCaptureNs: Long = 0L
        private set

    init {
        require(sampleRate > 0)
        require(channelCount == 1 || channelCount == 2)
    }

    fun start(): Boolean {
        val channelMask = if (channelCount == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) {
            onStatus(LiveAudioStatus.AUDIO_FAILED, "min_buffer_size_invalid_$minBuf")
            return false
        }
        // Keep the AudioRecord buffer snug so we never drag behind WebRTC's
        // 10 ms read cadence by more than a handful of milliseconds.
        val bufferBytes = maxOf(minBuf, bytesPerFrame() * sampleRate * AUDIO_RECORD_BUFFER_MS / 1000)

        val config = runCatching {
            AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
        }.getOrElse {
            logger("playback capture config build failed", it)
            onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_config_build_failed")
            return false
        }

        // RECORD_AUDIO can be denied or revoked at any time; fail this audio path cleanly.
        val record = try {
            AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build(),
                )
                .setBufferSizeInBytes(bufferBytes)
                .build()
        } catch (e: SecurityException) {
            logger("playback AudioRecord build failed: record audio permission denied", e)
            onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_audiorecord_build_failed_permission_denied")
            return false
        } catch (e: Exception) {
            logger("playback AudioRecord build failed", e)
            onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_audiorecord_build_failed")
            return false
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { record.release() }
            onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_audiorecord_uninitialized")
            return false
        }

        audioRecord = record
        recording = false
        framesReadTotal = 0L
        lastServedCaptureNs = 0L
        logger(
            "playback AudioRecord ready sampleRate=$sampleRate channels=$channelCount bufferBytes=$bufferBytes",
            null,
        )
        onStatus(LiveAudioStatus.AUDIO_INITIALIZING, "playback_pump_started")
        return true
    }

    fun stop() {
        val record = audioRecord
        audioRecord = null
        // stop() unblocks the external-input callback before waiting for its read to finish.
        runCatching { record?.stop() }
        synchronized(readLock) {
            runCatching { record?.release() }
            recording = false
            reusableBuffer = null
            lastServedCaptureNs = 0L
            activityReported = false
        }
    }

    /** Stop between viewers so queued samples and frame positions cannot leak into a new call. */
    fun pause() {
        synchronized(readLock) {
            if (recording) runCatching { audioRecord?.stop() }
            recording = false
            framesReadTotal = 0L
            lastServedCaptureNs = 0L
        }
    }

    /**
     * The playback recorder is the only audio clock. Called from WebRTC's external-input
     * callback with its microphone recorder disabled. Reading a 10 ms frame paces that callback;
     * there is no second clock inserting silence or discarding PCM between real samples.
     * Returns -1 on unavailable input, so the caller can pace silence without a busy loop.
     */
    fun overwriteWithDeviceAudio(dst: ByteBuffer, bytesToFill: Int): Int = synchronized(readLock) {
        lastServedCaptureNs = 0L
        val record = audioRecord ?: return@synchronized -1
        val length = bytesToFill.coerceAtMost(dst.capacity())
        if (length <= 0 || length % bytesPerFrame() != 0) return@synchronized -1
        if (!recording) {
            if (runCatching { record.startRecording() }.isFailure) {
                onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_audiorecord_start_failed")
                audioRecord = null
                runCatching { record.release() }
                return@synchronized -1
            }
            recording = true
            framesReadTotal = 0L
        }
        val temp = obtainReusableBuffer(length)
        temp.clear()
        var total = 0
        while (total < length && audioRecord === record) {
            // AudioRecord's ByteBuffer overload writes from offset zero and does not advance
            // position. A slice places each partial read after the samples already captured.
            temp.position(total)
            temp.limit(length)
            val read = runCatching {
                record.read(temp.slice(), length - total, AudioRecord.READ_BLOCKING)
            }.getOrDefault(AudioRecord.ERROR_INVALID_OPERATION)
            if (read <= 0) {
                if (audioRecord === record) {
                    onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_read_error_$read")
                    audioRecord = null
                    runCatching { record.stop() }
                    runCatching { record.release() }
                }
                return@synchronized -1
            }
            total += read
        }
        if (total != length) return@synchronized -1
        val firstFrame = framesReadTotal
        framesReadTotal += total / bytesPerFrame()
        val nowNs = System.nanoTime()
        val timestampAvailable = runCatching {
            record.getTimestamp(audioTimestamp, android.media.AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS
        }.getOrDefault(false)
        val measuredNs = if (timestampAvailable) {
            audioTimestamp.nanoTime + (firstFrame - audioTimestamp.framePosition) * 1_000_000_000L / sampleRate
        } else 0L
        lastServedCaptureNs = if (measuredNs > 0 && nowNs - measuredNs in 0L..MAX_PLAUSIBLE_CAPTURE_AGE_NS) {
            measuredNs
        } else {
            nowNs - total * 1_000_000_000L / (bytesPerFrame().toLong() * sampleRate)
        }
        temp.clear()
        temp.order(ByteOrder.LITTLE_ENDIAN)
        var peak = 0
        for (i in 0 until total step BYTES_PER_SAMPLE) {
            peak = maxOf(peak, abs(temp.getShort(i).toInt()))
        }
        for (i in 0 until total) dst.put(i, temp.get(i))
        if (!activityReported && peak > SILENCE_THRESHOLD) {
            activityReported = true
            onActivity()
        }
        peak
    }

    private fun obtainReusableBuffer(capacity: Int): ByteBuffer {
        val existing = reusableBuffer
        if (existing != null && existing.capacity() >= capacity) return existing
        val fresh = ByteBuffer.allocateDirect(capacity)
        reusableBuffer = fresh
        return fresh
    }

    private fun bytesPerFrame(): Int = BYTES_PER_SAMPLE * channelCount

    companion object {
        private const val BYTES_PER_SAMPLE = 2
        private const val SILENCE_THRESHOLD = 8
        private const val AUDIO_RECORD_BUFFER_MS = 20
        private const val MAX_PLAUSIBLE_CAPTURE_AGE_NS = 2_000_000_000L
    }
}
