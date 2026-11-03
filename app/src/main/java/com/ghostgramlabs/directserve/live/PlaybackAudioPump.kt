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
import kotlin.math.min

/**
 * Synchronous wrapper around an [AudioPlaybackCaptureConfiguration]-backed
 * [AudioRecord]. Designed to be driven from WebRTC's audio buffer callback
 * thread: the callback already runs at the microphone's 10 ms cadence, so we
 * simply [overwriteWithDeviceAudio] the mic PCM in place with freshly-read
 * playback PCM. WebRTC then stamps the resulting audio with the mic's own
 * capture clock — which is what keeps audio in sync with the screen-capture
 * video track, because both tracks then share the same underlying timebase.
 *
 * This mirrors the ScreenStream / Stream.io pattern: do not fight WebRTC's
 * internal audio pacing, ride on top of it.
 */
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

    // Playback capture is read on its own thread into this ring, and WebRTC's 10 ms mic callback
    // only ever copies out of it. Reading the capture AudioRecord *inside* the callback with
    // READ_BLOCKING tied two independent audio clocks together: whenever capture delivered late,
    // the callback stalled, audio went out in bursts and was stamped with a mic time newer than
    // its content, so viewers heard sound 0.3-0.5 s behind the picture.
    private val ringLock = Any()
    private var ring = ByteArray(0)
    private var ringRead = 0
    private var ringSize = 0
    private var maxBufferedBytes = 0
    private var prefillBytes = 0
    // Capture delivers in uneven bursts; holding a small cushion (PREFILL_MS) before serving
    // stops nearly every other 10 ms callback from coming up short and being padded with silence.
    private var primed = false

    @Volatile
    private var readerThread: Thread? = null

    /**
     * How old the audio handed out by the last [overwriteWithDeviceAudio] call was, in ns. The
     * caller subtracts it from the capture timestamp so receivers can lip-sync the real content
     * time instead of the moment the mic callback happened to run.
     */
    @Volatile
    var lastServedLatencyNs: Long = 0L
        private set

    // Per-window health counters, logged every STATS_WINDOW_CALLBACKS callbacks (~5 s).
    private var statsCallbacks = 0
    private var statsUnderflows = 0
    private var statsBufferedBytes = 0L
    private var statsDroppedBytes = 0L

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

        val record = runCatching {
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
        }.getOrElse {
            logger("playback AudioRecord build failed", it)
            onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_audiorecord_build_failed")
            return false
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { record.release() }
            onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_audiorecord_uninitialized")
            return false
        }

        runCatching { record.startRecording() }.onFailure {
            logger("playback AudioRecord startRecording failed", it)
            runCatching { record.release() }
            onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_audiorecord_start_failed")
            return false
        }

        audioRecord = record
        synchronized(ringLock) {
            ring = ByteArray(bytesPerFrame() * sampleRate * RING_CAPACITY_MS / 1000)
            ringRead = 0
            ringSize = 0
            maxBufferedBytes = bytesPerFrame() * sampleRate * MAX_BUFFERED_MS / 1000
            prefillBytes = bytesPerFrame() * sampleRate * PREFILL_MS / 1000
            primed = false
        }
        readerThread = Thread({ readLoop(record, bufferBytes) }, "DirectServe-PlaybackCapture").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
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
        runCatching { record?.stop() }
        // stop() unblocks the reader's read(); wait briefly so release() never races it.
        runCatching { readerThread?.join(200) }
        readerThread = null
        runCatching { record?.release() }
        reusableBuffer = null
        activityReported = false
        synchronized(ringLock) { ringSize = 0 }
    }

    private fun readLoop(record: AudioRecord, chunkBytes: Int) {
        val chunk = ByteArray(chunkBytes)
        while (audioRecord === record) {
            val read = runCatching { record.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING) }.getOrElse { err ->
                if (audioRecord === record) {
                    logger("playback AudioRecord read threw", err)
                    onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_read_exception")
                }
                return
            }
            if (read <= 0) {
                if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
                    if (audioRecord === record) {
                        logger("playback AudioRecord read error code=$read", null)
                        onStatus(LiveAudioStatus.AUDIO_FAILED, "playback_read_error_$read")
                    }
                    return
                }
                continue
            }
            writeRing(chunk, read)
        }
    }

    private fun writeRing(src: ByteArray, length: Int) {
        synchronized(ringLock) {
            val capacity = ring.size
            if (capacity == 0) return
            var offset = 0
            var remaining = length
            if (remaining > capacity) {
                offset = remaining - capacity
                remaining = capacity
            }
            // Make room by dropping the oldest audio rather than letting latency grow.
            val overflow = ringSize + remaining - capacity
            if (overflow > 0) {
                ringRead = (ringRead + overflow) % capacity
                ringSize -= overflow
            }
            var write = (ringRead + ringSize) % capacity
            while (remaining > 0) {
                val run = min(remaining, capacity - write)
                System.arraycopy(src, offset, ring, write, run)
                offset += run
                remaining -= run
                write = (write + run) % capacity
                ringSize += run
            }
        }
    }

    /** Copies up to [length] bytes of the freshest buffered audio into [dst]; never blocks. */
    private fun readRing(dst: ByteBuffer, length: Int): Int {
        synchronized(ringLock) {
            val capacity = ring.size
            if (capacity == 0) return 0
            statsCallbacks++
            statsBufferedBytes += ringSize
            if (!primed) {
                if (ringSize < prefillBytes) {
                    statsUnderflows++
                    if (statsCallbacks >= STATS_WINDOW_CALLBACKS) logStats()
                    return 0
                }
                primed = true
            }
            // Keep at most MAX_BUFFERED_MS queued so playback latency stays small and constant.
            val frameBytes = bytesPerFrame()
            val excess = ringSize - maxOf(maxBufferedBytes, length)
            if (excess > 0) {
                val drop = excess - excess % frameBytes
                ringRead = (ringRead + drop) % capacity
                ringSize -= drop
                statsDroppedBytes += drop
            }
            lastServedLatencyNs = ringSize * 1_000_000_000L / (frameBytes.toLong() * sampleRate)
            var count = min(length, ringSize)
            count -= count % frameBytes
            if (count < length) {
                statsUnderflows++
                primed = false // ran dry: rebuild the cushion before serving again
            }
            for (i in 0 until count) {
                dst.put(i, ring[(ringRead + i) % capacity])
            }
            ringRead = (ringRead + count) % capacity
            ringSize -= count
            if (statsCallbacks >= STATS_WINDOW_CALLBACKS) logStats()
            return count
        }
    }

    private fun logStats() {
        val bytesPerMs = bytesPerFrame() * sampleRate / 1000.0
        logger(
            "playback pump window callbacks=$statsCallbacks " +
                "avgBufferedMs=${"%.1f".format(statsBufferedBytes / statsCallbacks / bytesPerMs)} " +
                "underflowPct=${statsUnderflows * 100 / statsCallbacks} " +
                "droppedMs=${"%.0f".format(statsDroppedBytes / bytesPerMs)}",
            null,
        )
        statsCallbacks = 0
        statsUnderflows = 0
        statsBufferedBytes = 0
        statsDroppedBytes = 0
    }

    /**
     * Reads [bytesToFill] bytes of device playback PCM and writes them into
     * [dst] starting at absolute index 0, leaving [dst]'s position and limit
     * untouched (WebRTC is responsible for those). Returns the peak PCM-16
     * sample magnitude observed, or -1 if the pump is not yet producing audio
     * (caller should treat this as "keep mic PCM silent" — do NOT let the
     * untouched mic audio leak out).
     */
    fun overwriteWithDeviceAudio(dst: ByteBuffer, bytesToFill: Int): Int {
        if (audioRecord == null) return -1
        val capacityNeeded = bytesToFill.coerceAtMost(dst.capacity())
        if (capacityNeeded <= 0) return -1
        val temp = obtainReusableBuffer(capacityNeeded)
        temp.clear()
        // Non-blocking: whatever capture has delivered so far; a shortfall is padded with silence
        // below, which keeps WebRTC's capture cadence (and timestamps) steady.
        val readBytes = readRing(temp, capacityNeeded)

        temp.order(ByteOrder.LITTLE_ENDIAN)
        var peak = 0
        var i = 0
        while (i + 1 < readBytes) {
            val sample = temp.getShort(i).toInt()
            peak = maxOf(peak, abs(sample))
            i += 2
        }
        // Overwrite dst[0..readBytes] using absolute positioning so WebRTC's
        // position/limit state is preserved exactly as it handed us the buffer.
        for (idx in 0 until readBytes) {
            dst.put(idx, temp.get(idx))
        }
        // If the mic read was shorter than what WebRTC is expecting, pad the
        // remainder with silence so no stale mic PCM leaks out.
        if (readBytes < bytesToFill) {
            for (idx in readBytes until bytesToFill.coerceAtMost(dst.capacity())) {
                dst.put(idx, 0)
            }
        }

        if (!activityReported && peak > SILENCE_THRESHOLD) {
            activityReported = true
            onActivity()
        }
        return peak
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
        private const val RING_CAPACITY_MS = 400
        private const val PREFILL_MS = 60
        private const val MAX_BUFFERED_MS = 250
        private const val STATS_WINDOW_CALLBACKS = 500
    }
}
