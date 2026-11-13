package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class CarAudioEngine {
    @Volatile
    private var running = false

    @Volatile
    private var muted = false

    @Volatile
    private var carType = CarType.EV_HYPER_GT

    @Volatile
    private var speedNorm = 0f

    @Volatile
    private var throttle = 0f

    @Volatile
    private var driftIntensity = 0f

    @Volatile
    private var boostTimer = 0f

    @Volatile
    private var backfirePulse = 0f

    private var workerThread: Thread? = null

    fun start() {
        if (running) return
        running = true
        workerThread = thread(start = true, isDaemon = true, name = "HypercarAudioSynth") {
            runSynthLoop()
        }
    }

    fun stop() {
        running = false
        workerThread?.join(300)
        workerThread = null
    }

    fun setMuted(isMuted: Boolean) {
        muted = isMuted
    }

    fun triggerBoost() {
        boostTimer = 1.0f
    }

    fun triggerBackfire() {
        backfirePulse = 1.0f
    }

    fun update(
        type: CarType,
        speedMetersPerSec: Float,
        gasPressed: Boolean,
        driftAmount: Float
    ) {
        carType = type
        speedNorm = (abs(speedMetersPerSec) / type.topSpeed).coerceIn(0f, 1.35f)
        throttle = if (gasPressed) 1f else 0f
        driftIntensity = driftAmount.coerceIn(0f, 1f)
    }

    private fun runSynthLoop() {
        val sampleRate = 22050
        val minBufBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) {
            return
        }

        val chunkSamples = 512
        val pcmBuffer = ShortArray(chunkSamples)

        var phaseMain = 0.0
        var phaseHarm1 = 0.0
        var phaseHarm2 = 0.0
        var phaseSqueal = 0.0
        var phaseBoost = 0.0
        var lpState = 0.0
        var hpPrev = 0.0
        var smoothSpeed = 0.0
        var smoothThrottle = 0.0
        var smoothDrift = 0.0

        val twoPi = 2.0 * PI

        try {
            audioTrack.play()
            while (running) {
                if (muted) {
                    pcmBuffer.fill(0)
                    audioTrack.write(pcmBuffer, 0, chunkSamples)
                    Thread.sleep(16)
                    continue
                }

                val activeCar = carType
                val targetSpd = speedNorm.toDouble()
                val targetThr = throttle.toDouble()
                val targetDft = driftIntensity.toDouble()
                val localBoost = boostTimer
                if (boostTimer > 0f) boostTimer = (boostTimer - 0.023f).coerceAtLeast(0f)
                val localBackfire = backfirePulse
                if (backfirePulse > 0f) backfirePulse = (backfirePulse - 0.06f).coerceAtLeast(0f)

                for (i in 0 until chunkSamples) {
                    smoothSpeed += (targetSpd - smoothSpeed) * 0.0025
                    smoothThrottle += (targetThr - smoothThrottle) * 0.004
                    smoothDrift += (targetDft - smoothDrift) * 0.005

                    var sample = 0.0

                    when (activeCar) {
                        CarType.EV_HYPER_GT -> {
                            // Futuristic dual-harmonic electric turbine
                            val baseFreq = 115.0 + smoothSpeed * 620.0 + smoothThrottle * 35.0
                            val invFreq = 420.0 + smoothSpeed * 1250.0
                            phaseMain = (phaseMain + twoPi * baseFreq / sampleRate) % twoPi
                            phaseHarm1 = (phaseHarm1 + twoPi * (baseFreq * 1.995) / sampleRate) % twoPi
                            phaseHarm2 = (phaseHarm2 + twoPi * invFreq / sampleRate) % twoPi

                            val turbine = sin(phaseMain) * 0.45 + sin(phaseHarm1) * 0.25
                            val inverter = sin(phaseHarm2) * (0.04 + smoothThrottle * 0.08 + smoothSpeed * 0.08)
                            val presence = 0.14 + smoothSpeed * 0.26 + smoothThrottle * 0.14
                            sample += (turbine + inverter) * presence
                        }

                        CarType.V12_APEX_ROADSTER -> {
                            // Aggressive V12 multi-harmonic combustion rumble + backfire pop
                            val gearCycle = (smoothSpeed * 4.5) % 1.0
                            val rpmFactor = 0.22 + smoothSpeed * 0.65 + gearCycle * 0.18 + smoothThrottle * 0.12
                            val baseFreq = 42.0 + rpmFactor * 165.0
                            phaseMain = (phaseMain + twoPi * baseFreq / sampleRate) % twoPi
                            phaseHarm1 = (phaseHarm1 + twoPi * (baseFreq * 2.0) / sampleRate) % twoPi
                            phaseHarm2 = (phaseHarm2 + twoPi * (baseFreq * 3.0) / sampleRate) % twoPi

                            // Sawtooth-like aggressive V12 firing waveform
                            val saw = (phaseMain / PI) - 1.0
                            val roar = saw * 0.35 + sin(phaseHarm1) * 0.32 + sin(phaseHarm2) * 0.22 + sin(phaseMain) * 0.25
                            lpState += (roar - lpState) * (0.14 + smoothThrottle * 0.20 + smoothSpeed * 0.12)
                            val pop = if (localBackfire > 0.05f) {
                                (Random.nextDouble() * 2.0 - 1.0) * localBackfire * 0.45
                            } else 0.0
                            sample += lpState * (0.28 + smoothThrottle * 0.22 + smoothSpeed * 0.18) + pop
                        }

                        CarType.VIP_EXECUTIVE_COUPE -> {
                            // Smooth velvet luxury V8 purr
                            val baseFreq = 38.0 + smoothSpeed * 112.0 + smoothThrottle * 18.0
                            phaseMain = (phaseMain + twoPi * baseFreq / sampleRate) % twoPi
                            phaseHarm1 = (phaseHarm1 + twoPi * (baseFreq * 2.0) / sampleRate) % twoPi
                            phaseHarm2 = (phaseHarm2 + twoPi * (baseFreq * 0.5) / sampleRate) % twoPi

                            val raw = sin(phaseMain) * 0.55 + sin(phaseHarm1) * 0.22 + sin(phaseHarm2) * 0.25
                            lpState += (raw - lpState) * 0.08
                            sample += lpState * (0.25 + smoothThrottle * 0.16 + smoothSpeed * 0.14)
                        }
                    }

                    // Tire screech during drift
                    if (smoothDrift > 0.03) {
                        val squealFreq = 920.0 + smoothSpeed * 260.0
                        phaseSqueal = (phaseSqueal + twoPi * squealFreq / sampleRate) % twoPi
                        val noise = Random.nextDouble() * 2.0 - 1.0
                        val highNoise = noise - hpPrev * 0.85
                        hpPrev = noise
                        val tireWave = (sin(phaseSqueal) * 0.45 + highNoise * 0.35)
                        sample += tireWave * smoothDrift * 0.22
                    }

                    // Boost pad whoosh
                    if (localBoost > 0.02f) {
                        val bFreq = 260.0 + (1.0 - localBoost) * 680.0
                        phaseBoost = (phaseBoost + twoPi * bFreq / sampleRate) % twoPi
                        sample += sin(phaseBoost) * localBoost * 0.24
                    }

                    val clamped = (sample * 11000.0).coerceIn(-32000.0, 32000.0)
                    pcmBuffer[i] = clamped.toInt().toShort()
                }
                audioTrack.write(pcmBuffer, 0, chunkSamples)
            }
            audioTrack.stop()
        } catch (_: Throwable) {
            // Audio hardware unavailable on headless test runners
        } finally {
            try {
                audioTrack.release()
            } catch (_: Throwable) {
            }
        }
    }
}
