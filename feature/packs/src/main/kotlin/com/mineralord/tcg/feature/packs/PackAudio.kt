package com.mineralord.tcg.feature.packs

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Efectos de la apertura de sobres. */
enum class Sfx { WHOOSH, SPARKLE, RARE }

/**
 * SFX **sintetizados en runtime** con [AudioTrack] (PCM16 mono, 44.1 kHz). No usa
 * ficheros de audio ni assets de terceros: todo se genera por DSP procedural.
 * Cada [play] corre en un hilo de fondo (un solo worker) y libera su track al
 * terminar; [release] apaga el ejecutor.
 */
class PackAudio {
    private val exec = Executors.newSingleThreadExecutor()
    private val sr = 44_100

    fun play(kind: Sfx) {
        exec.execute {
            runCatching {
                val data = when (kind) {
                    Sfx.WHOOSH -> whoosh()
                    Sfx.SPARKLE -> sparkle()
                    Sfx.RARE -> fanfare()
                }
                playPcm(data)
            }
        }
    }

    fun release() {
        runCatching { exec.shutdownNow() }
    }

    private fun playPcm(data: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sr)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
        track.write(data, 0, data.size)
        track.play()
        Thread.sleep(data.size * 1000L / sr + 80)
        runCatching { track.stop() }
        runCatching { track.release() }
    }

    private fun toPcm(buf: FloatArray): ShortArray {
        val out = ShortArray(buf.size)
        for (i in buf.indices) {
            val v = buf[i].coerceIn(-1f, 1f)
            out[i] = (v * 32_000f).toInt().toShort()
        }
        return out
    }

    /** Ráfaga de carta entrando: ruido con paso-bajo que barre hacia abajo. */
    private fun whoosh(): ShortArray {
        val n = (sr * 0.28f).toInt()
        val buf = FloatArray(n)
        val rnd = Random(1)
        var lp = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val env = if (t < 0.08f) t / 0.08f else 1f - (t - 0.08f) / 0.92f
            val cutoff = 0.55f - 0.5f * t
            val noise = rnd.nextFloat() * 2f - 1f
            lp += cutoff * (noise - lp)
            buf[i] = lp * env * 0.6f
        }
        return toPcm(buf)
    }

    /** Chispa: varios pings senoidales agudos con decaimiento rápido. */
    private fun sparkle(): ShortArray {
        val n = (sr * 0.38f).toInt()
        val buf = FloatArray(n)
        val freqs = floatArrayOf(2100f, 2650f, 3200f)
        val starts = floatArrayOf(0f, 0.05f, 0.11f)
        for (k in freqs.indices) {
            val s = (starts[k] * sr).toInt()
            for (i in s until n) {
                val tt = (i - s).toFloat() / sr
                val env = exp(-tt * 17f)
                buf[i] += (sin(2.0 * PI * freqs[k] * tt).toFloat()) * env * 0.3f
            }
        }
        return toPcm(buf)
    }

    /** Fanfarria de rara: arpegio ascendente con envolvente de campana. */
    private fun fanfare(): ShortArray {
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.5f)
        val noteDur = 0.13f
        val n = (sr * (notes.size * noteDur + 0.35f)).toInt()
        val buf = FloatArray(n)
        for (j in notes.indices) {
            val s = (j * noteDur * sr).toInt()
            for (i in s until n) {
                val tt = (i - s).toFloat() / sr
                val env = exp(-tt * 5.5f) * (1f - exp(-tt * 80f))
                val f = notes[j]
                val tone = sin(2.0 * PI * f * tt).toFloat() + 0.3f * sin(2.0 * PI * f * 2 * tt).toFloat()
                buf[i] += tone * env * 0.22f
            }
        }
        return toPcm(buf)
    }
}
