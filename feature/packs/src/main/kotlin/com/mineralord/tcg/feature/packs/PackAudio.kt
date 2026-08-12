package com.mineralord.tcg.feature.packs

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Efectos de la apertura de sobres (síntesis procedural, sin ficheros). */
enum class Sfx { WHOOSH, SPARKLE, RARE, GRAB, TEAR, OPEN, IMPACT, HIGH }

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
                    Sfx.GRAB -> grab()
                    Sfx.TEAR -> tear()
                    Sfx.OPEN -> open()
                    Sfx.IMPACT -> impact()
                    Sfx.HIGH -> highChime()
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

    /** Agarre: click grave muy corto con un toque de ruido (contacto físico con el plástico). */
    private fun grab(): ShortArray {
        val n = (sr * 0.09f).toInt()
        val buf = FloatArray(n)
        val rnd = Random(7)
        for (i in 0 until n) {
            val tt = i.toFloat() / sr
            val env = exp(-tt * 42f)
            val tone = sin(2.0 * PI * 190f * tt).toFloat()
            val noise = (rnd.nextFloat() * 2f - 1f) * 0.25f
            buf[i] = (tone + noise) * env * 0.5f
        }
        return toPcm(buf)
    }

    /** Rasgado: ruido con "crackle" (modulación de amplitud irregular) que barre — plástico rompiéndose. */
    private fun tear(): ShortArray {
        val n = (sr * 0.34f).toInt()
        val buf = FloatArray(n)
        val rnd = Random(23)
        var lp = 0f
        var crackle = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val env = (1f - t) * (if (t < 0.05f) t / 0.05f else 1f)
            val cutoff = 0.75f
            val noise = rnd.nextFloat() * 2f - 1f
            lp += cutoff * (noise - lp)
            // Crackle: pulsos aleatorios que dan la textura de fibras rompiéndose.
            if (rnd.nextFloat() < 0.06f) crackle = rnd.nextFloat()
            crackle *= 0.82f
            buf[i] = (lp * (0.5f + crackle)) * env * 0.7f
        }
        return toPcm(buf)
    }

    /** Apertura: hinchazón aérea (ruido paso-bajo que sube + florecer senoidal). */
    private fun open(): ShortArray {
        val n = (sr * 0.5f).toInt()
        val buf = FloatArray(n)
        val rnd = Random(41)
        var lp = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val env = if (t < 0.4f) t / 0.4f else 1f - (t - 0.4f) / 0.6f
            val cutoff = 0.12f + 0.5f * t
            val noise = rnd.nextFloat() * 2f - 1f
            lp += cutoff * (noise - lp)
            val bloom = sin(2.0 * PI * (330f + 220f * t) * (i.toFloat() / sr)).toFloat() * 0.15f
            buf[i] = (lp * 0.5f + bloom) * env * 0.5f
        }
        return toPcm(buf)
    }

    /** Impacto de asentamiento: golpe grave corto (la carta aterriza). */
    private fun impact(): ShortArray {
        val n = (sr * 0.16f).toInt()
        val buf = FloatArray(n)
        for (i in 0 until n) {
            val tt = i.toFloat() / sr
            val env = exp(-tt * 26f)
            val f = 92f * (1f + 2f * exp(-tt * 60f)) // pitch-drop
            buf[i] = sin(2.0 * PI * f * tt).toFloat() * env * 0.55f
        }
        return toPcm(buf)
    }

    /** Carillón de alta rareza: parciales altos con decaimiento largo y trémolo (brillo etéreo). */
    private fun highChime(): ShortArray {
        val partials = floatArrayOf(1174.7f, 1567.98f, 2093f, 2637f, 3136f)
        val n = (sr * 1.1f).toInt()
        val buf = FloatArray(n)
        for (k in partials.indices) {
            val s = (k * 0.045f * sr).toInt()
            for (i in s until n) {
                val tt = (i - s).toFloat() / sr
                val env = exp(-tt * 3.2f) * (1f - exp(-tt * 120f))
                val trem = 1f + 0.12f * sin(2.0 * PI * 6.5f * tt).toFloat()
                buf[i] += sin(2.0 * PI * partials[k] * tt).toFloat() * env * trem * 0.14f
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
