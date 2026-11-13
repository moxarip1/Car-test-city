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
    private var masterVolume = 0.85f

    @Volatile
    private var engineVolume = 0.90f

    @Volatile
    private var sfxVolume = 0.85f

    @Volatile
    private var activeDriving = false

    @Volatile
    private var carType = CarType.EV_HYPER_GT

    @Volatile
    private var rpmNorm = 0.12f

    @Volatile
    private var speedMetersPerSec = 0f

    @Volatile
    private var throttle = 0f

    @Volatile
    private var driftIntensity = 0f

    @Volatile
    private var boostTimer = 0f

    @Volatile
    private var backfirePulse = 0f

    @Volatile
    private var bovPulse = 0f

    @Volatile
    private var nitroActive = false

    private var prevThrottle = false
    private var prevGear = 1
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

    fun applySettings(master: Float, engine: Float, sfx: Float, isMuted: Boolean) {
        masterVolume = master.coerceIn(0f, 1f)
        engineVolume = engine.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        muted = isMuted
    }

    fun setActiveDriving(active: Boolean) {
        activeDriving = active
    }

    fun triggerBoost() {
        boostTimer = 1.0f
    }

    fun triggerBackfire() {
        backfirePulse = 1.0f
    }

    fun update(
        type: CarType,
        speedMps: Float,
        gear: Int,
        rpmNormalized: Float,
        gasPressed: Boolean,
        driftAmount: Float,
        isNitro: Boolean
    ) {
        carType = type
        speedMetersPerSec = abs(speedMps)
        rpmNorm = rpmNormalized.coerceIn(0f, 1.05f)
        throttle = if (gasPressed) 1f else 0f
        driftIntensity = driftAmount.coerceIn(0f, 1f)
        nitroActive = isNitro

        if (gear > prevGear && type == CarType.V12_APEX_ROADSTER) {
            backfirePulse = 0.85f
        }
        if (prevThrottle && !gasPressed && rpmNormalized > 0.52f && type != CarType.EV_HYPER_GT) {
            bovPulse = 1.0f
        }
        prevGear = gear
        prevThrottle = gasPressed
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
        var phaseTurbo = 0.0
        var phaseSqueal = 0.0
        var phaseBoost = 0.0
        var lpState = 0.0
        var hpPrev = 0.0
        var windLp = 0.0
        var smoothRpm = 0.12
        var smoothThrottle = 0.0
        var smoothDrift = 0.0
        var smoothWind = 0.0

        val twoPi = 2.0 * PI

        try {
            audioTrack.play()
            while (running) {
                if (muted || !activeDriving || masterVolume <= 0.005f) {
                    pcmBuffer.fill(0)
                    audioTrack.write(pcmBuffer, 0, chunkSamples)
                    Thread.sleep(16)
                    continue
                }

                val activeCar = carType
                val mGain = masterVolume.toDouble()
                val eGain = engineVolume.toDouble()
                val sGain = sfxVolume.toDouble()
                val targetRpm = rpmNorm.toDouble()
                val targetThr = throttle.toDouble()
                val targetDft = driftIntensity.toDouble()
                val targetWind = ((speedMetersPerSec - 55.5f) / 55f).coerceIn(0f, 1f).toDouble()
                val localBoost = boostTimer
                if (boostTimer > 0f) boostTimer = (boostTimer - 0.023f).coerceAtLeast(0f)
                val localBackfire = backfirePulse
                if (backfirePulse > 0f) backfirePulse = (backfirePulse - 0.055f).coerceAtLeast(0f)
                val localBov = bovPulse
                if (bovPulse > 0f) bovPulse = (bovPulse - 0.032f).coerceAtLeast(0f)
                val isNitro = nitroActive

                for (i in 0 until chunkSamples) {
                    smoothRpm += (targetRpm - smoothRpm) * 0.0045
                    smoothThrottle += (targetThr - smoothThrottle) * 0.005
                    smoothDrift += (targetDft - smoothDrift) * 0.005
                    smoothWind += (targetWind - smoothWind) * 0.002

                    var engineSample = 0.0

                    when (activeCar) {
                        CarType.EV_HYPER_GT -> {
                            val baseFreq = 120.0 + smoothRpm * 640.0 + (if (isNitro) 110.0 else 0.0)
                            val invFreq = 460.0 + smoothRpm * 1350.0
                            phaseMain = (phaseMain + twoPi * baseFreq / sampleRate) % twoPi
                            phaseHarm1 = (phaseHarm1 + twoPi * (baseFreq * 1.995) / sampleRate) % twoPi
                            phaseHarm2 = (phaseHarm2 + twoPi * invFreq / sampleRate) % twoPi

                            val turbine = sin(phaseMain) * 0.45 + sin(phaseHarm1) * 0.25
                            val inverter = sin(phaseHarm2) * (0.04 + smoothThrottle * 0.09 + smoothRpm * 0.08)
                            val presence = 0.14 + smoothRpm * 0.26 + smoothThrottle * 0.14
                            engineSample += (turbine + inverter) * presence
                        }

                        CarType.V12_APEX_ROADSTER -> {
                            val baseFreq = 44.0 + smoothRpm * 185.0 + (if (isNitro) 25.0 else 0.0)
                            phaseMain = (phaseMain + twoPi * baseFreq / sampleRate) % twoPi
                            phaseHarm1 = (phaseHarm1 + twoPi * (baseFreq * 2.0) / sampleRate) % twoPi
                            phaseHarm2 = (phaseHarm2 + twoPi * (baseFreq * 3.0) / sampleRate) % twoPi

                            val saw = (phaseMain / PI) - 1.0
                            val roar = saw * 0.36 + sin(phaseHarm1) * 0.32 + sin(phaseHarm2) * 0.24 + sin(phaseMain) * 0.22
                            lpState += (roar - lpState) * (0.14 + smoothThrottle * 0.22 + smoothRpm * 0.14)
                            val pop = if (localBackfire > 0.04f) {
                                (Random.nextDouble() * 2.0 - 1.0) * localBackfire * 0.50
                            } else 0.0

                            val turboFreq = 1650.0 + smoothRpm * 1450.0
                            phaseTurbo = (phaseTurbo + twoPi * turboFreq / sampleRate) % twoPi
                            val turboWhine = sin(phaseTurbo) * smoothThrottle * smoothRpm * 0.065

                            engineSample += lpState * (0.28 + smoothThrottle * 0.22 + smoothRpm * 0.20) + pop + turboWhine
                        }

                        CarType.VIP_EXECUTIVE_COUPE -> {
                            val baseFreq = 38.0 + smoothRpm * 128.0
                            phaseMain = (phaseMain + twoPi * baseFreq / sampleRate) % twoPi
                            phaseHarm1 = (phaseHarm1 + twoPi * (baseFreq * 2.0) / sampleRate) % twoPi
                            phaseHarm2 = (phaseHarm2 + twoPi * (baseFreq * 0.5) / sampleRate) % twoPi

                            val raw = sin(phaseMain) * 0.54 + sin(phaseHarm1) * 0.22 + sin(phaseHarm2) * 0.26
                            lpState += (raw - lpState) * 0.085
                            engineSample += lpState * (0.25 + smoothThrottle * 0.17 + smoothRpm * 0.15)
                        }
                    }

                    var sfxSample = 0.0
                    val noise = Random.nextDouble() * 2.0 - 1.0
                    val highNoise = noise - hpPrev * 0.85
                    hpPrev = noise

                    if (localBov > 0.02f) {
                        sfxSample += highNoise * localBov * 0.18
                    }

                    if (smoothDrift > 0.03) {
                        val squealFreq = 920.0 + smoothRpm * 240.0
                        phaseSqueal = (phaseSqueal + twoPi * squealFreq / sampleRate) % twoPi
                        val tireWave = (sin(phaseSqueal) * 0.45 + highNoise * 0.35)
                        sfxSample += tireWave * smoothDrift * 0.22
                    }

                    if (smoothWind > 0.01) {
                        windLp += (noise - windLp) * 0.12
                        sfxSample += windLp * smoothWind * 0.14
                    }

                    if (localBoost > 0.02f || isNitro) {
                        val bFreq = if (isNitro) 340.0 + smoothRpm * 220.0 else 260.0 + (1.0 - localBoost) * 680.0
                        phaseBoost = (phaseBoost + twoPi * bFreq / sampleRate) % twoPi
                        val amp = if (isNitro) 0.15 else localBoost * 0.24
                        sfxSample += (sin(phaseBoost) * 0.65 + windLp * 0.35) * amp
                    }

                    val mixed = (engineSample * eGain + sfxSample * sGain) * mGain
                    val clamped = (mixed * 11200.0).coerceIn(-32000.0, 32000.0)
                    pcmBuffer[i] = clamped.toInt().toShort()
                }
                audioTrack.write(pcmBuffer, 0, chunkSamples)
            }
            audioTrack.stop()
        } catch (_: Throwable) {
        } finally {
            try {
                audioTrack.release()
            } catch (_: Throwable) {
            }
        }
    }
}
