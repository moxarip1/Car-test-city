package com.example.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class SmokeParticle(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var age: Float = 0f,
    var maxAge: Float = 0.9f,
    var size: Float = 0.6f
)

data class SparkParticle(
    var active: Boolean = false,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var age: Float = 0f,
    var maxAge: Float = 0.36f
)

data class SkidSegment(
    var active: Boolean = false,
    var x1: Float = 0f,
    var z1: Float = 0f,
    var x2: Float = 0f,
    var z2: Float = 0f,
    var y: Float = 0.03f,
    var life: Float = 1.0f
)

data class TailTrailSegment(
    var active: Boolean = false,
    var x1: Float = 0f,
    var y1: Float = 0f,
    var z1: Float = 0f,
    var x2: Float = 0f,
    var y2: Float = 0f,
    var z2: Float = 0f,
    var life: Float = 1.0f
)

class GameWorldPhysics(
    private val audioEngine: CarAudioEngine,
    initialState: GameState = GameState.PLAYING
) {
    // High-level game state & persistent settings/records
    var gameState: GameState = initialState
    var settings: GameSettings = GameSettings()
    var bestDriftScore = 0
    var bestTotalScore = 0
    var topSpeedKmh = 0
    var fps = 60
    var onRecordsChanged: ((SavedRecords) -> Unit)? = null

    // Car spatial state (starts on Downtown North-South Boulevard facing the Grand Central Roundabout)
    var carX = if (initialState == GameState.SPLASH || initialState == GameState.MENU) CityLayout.SHOWROOM_X else 0f
    var carY = if (initialState == GameState.SPLASH || initialState == GameState.MENU) CityLayout.SHOWROOM_Y else 0.05f
    var carZ = if (initialState == GameState.SPLASH || initialState == GameState.MENU) CityLayout.SHOWROOM_Z else -290f
    var yaw = if (initialState == GameState.SPLASH || initialState == GameState.MENU) CityLayout.SHOWROOM_YAW else 0f
    var vx = 0f
    var vy = 0f
    var vz = 0f

    // Visual & suspension dynamics
    var chassisPitch = 0f
    var chassisRoll = 0f
    var suspensionY = 0f
    private var suspensionVy = 0f
    var frontSteerAngle = 0f
    var wheelSpinAngle = 0f
    var backfireTimer = 0f
    var boostTimer = 0f
    var nitroCharge = 100f
    var isNitroActive = false
    var cameraFov = 60f
    private var driftBlend = 0f

    // 6-Speed Automatic Gearbox state
    var currentGear = 1
    var rpmNormalized = 0.14f

    // Configuration
    var carType = CarType.EV_HYPER_GT
    var paintIndex = CarType.EV_HYPER_GT.defaultPaintIndex
    var paintFinish = PaintFinish.GLOSSY
    var cameraMode = CameraMode.CHASE
    var timeOfDay = TimeOfDay.SUNSET
    var isMuted = false

    // Scoring, Radars, Drift Zones & Stunt banners
    var activeDriftScore = 0f
    var driftMultiplier = 1
    var totalScore = 0
    var bestTrapSpeedKmh = 0
    var lastTrapSpeedKmh = 0
    var isNewRecordBanner = false
    private var driftEndGrace = 0f
    private var wasAirborne = false
    private var airtimeDuration = 0f
    private var helipadRewardCooldown = 0f
    private var bannerTimer = 0f
    private var currentBanner = StuntBannerType.NONE
    private var boostCooldown = 0f
    private val ringCooldowns = FloatArray(CityLayout.boostRings.size)
    private val trapCooldowns = FloatArray(CityLayout.speedTraps.size)
    private var sparkCooldown = 0f

    // Fixed-size pools for zero-GC instanced particles, sparks, skid marks, and night trails
    val smokeParticles = Array(128) { SmokeParticle() }
    private var smokeCursor = 0
    val sparkParticles = Array(96) { SparkParticle() }
    private var sparkCursor = 0
    val skidSegments = Array(260) { SkidSegment() }
    private var skidCursor = 0
    val tailTrails = Array(160) { TailTrailSegment() }
    private var tailCursor = 0

    private var prevRearLeftX = 0f
    private var prevRearLeftZ = 0f
    private var prevRearRightX = 0f
    private var prevRearRightZ = 0f
    private var hadSkidLastFrame = false

    private var prevTailLX = 0f
    private var prevTailLY = 0f
    private var prevTailLZ = 0f
    private var prevTailRX = 0f
    private var prevTailRY = 0f
    private var prevTailRZ = 0f
    private var hadTailLastFrame = false

    // Rock-solid exponential smoothing camera state (horizon locked, zero jitter)
    var camPosX = carX - sin(yaw) * 7.2f
    var camPosY = carY + 2.4f
    var camPosZ = carZ - cos(yaw) * 7.2f
    var camLookX = carX
    var camLookY = carY + 1.0f
    var camLookZ = carZ
    var orbitAngle = 0.6f
    var worldTime = 0f

    fun setRecords(records: SavedRecords) {
        bestDriftScore = records.bestDriftScore
        bestTotalScore = records.bestTotalScore
        topSpeedKmh = records.topSpeedKmh
    }

    fun applySettings(newSettings: GameSettings) {
        settings = newSettings
        isMuted = newSettings.isMuted
        audioEngine.applySettings(
            master = newSettings.masterVolume,
            engine = newSettings.engineVolume,
            sfx = newSettings.sfxVolume,
            isMuted = newSettings.isMuted
        )
    }

    fun switchCar(newType: CarType) {
        if (carType != newType) {
            carType = newType
            paintIndex = newType.defaultPaintIndex
            suspensionVy -= 1.2f
        }
    }

    fun snapToShowroom() {
        clearEffectsAndMomentum()
        carX = CityLayout.SHOWROOM_X
        carY = CityLayout.SHOWROOM_Y
        carZ = CityLayout.SHOWROOM_Z
        yaw = CityLayout.SHOWROOM_YAW
        frontSteerAngle = 0.22f
        orbitAngle = 0.72f
        val radius = 6.8f
        camPosX = carX + sin(orbitAngle) * radius
        camPosY = carY + 2.05f
        camPosZ = carZ + cos(orbitAngle) * radius
        camLookX = carX - 0.3f
        camLookY = carY + 0.85f
        camLookZ = carZ
    }

    fun teleportTo(location: SpawnLocation) {
        clearEffectsAndMomentum()
        val pt = CityLayout.spawnPoint(location)
        carX = pt[0]
        carY = pt[1]
        carZ = pt[2]
        yaw = pt[3]
        val fwdX = sin(yaw)
        val fwdZ = cos(yaw)
        camPosX = carX - fwdX * 7.6f
        camPosY = carY + 2.75f
        camPosZ = carZ - fwdZ * 7.6f
        camLookX = carX + fwdX * 4.2f
        camLookY = carY + 1.10f
        camLookZ = carZ + fwdZ * 4.2f
        cameraFov = 58f
    }

    private fun clearEffectsAndMomentum() {
        vx = 0f; vy = 0f; vz = 0f
        chassisPitch = 0f; chassisRoll = 0f
        suspensionY = 0f; suspensionVy = 0f
        boostTimer = 0f
        backfireTimer = 0f
        isNitroActive = false
        hadSkidLastFrame = false
        hadTailLastFrame = false
        activeDriftScore = 0f
        driftMultiplier = 1
        currentBanner = StuntBannerType.NONE
        bannerTimer = 0f
        for (p in smokeParticles) p.active = false
        for (sp in sparkParticles) sp.active = false
        for (t in tailTrails) t.active = false
    }

    fun resetCar() {
        val candidates = arrayOf(
            floatArrayOf(0f, -290f, 0f, 0.25f),                            // Downtown North-South
            floatArrayOf(0f, 92f, 3.14159f, 0.25f),                        // Downtown South
            floatArrayOf(580f, -540f, 0f, 0.25f),                          // Marina Coastal Straightaway
            floatArrayOf(580f, 180f, 0f, 0.25f),                           // Marina South
            floatArrayOf(-1180f, 480f, 0f, 0.25f),                         // Airport Runway Start
            floatArrayOf(CityLayout.CANYON_FEEDER_X, -220f, PI.toFloat(), 0.25f), // Canyon Entry
            floatArrayOf(CityLayout.canyonRoadX(-1050f), -1050f, PI.toFloat(), CityLayout.canyonRoadY(-1050f) + 0.35f) // Canyon Mid
        )
        var best = candidates[0]
        var bestDist = Float.MAX_VALUE
        for (c in candidates) {
            val d = hypot(carX - c[0], carZ - c[1])
            if (d < bestDist) {
                bestDist = d
                best = c
            }
        }
        carX = best[0]
        carZ = best[1]
        yaw = best[2]
        carY = best[3]
        vx = 0f; vy = 0f; vz = 0f
        chassisPitch = 0f; chassisRoll = 0f
        suspensionY = 0f; suspensionVy = 0f
        boostTimer = 0f
        isNitroActive = false
        hadSkidLastFrame = false
        hadTailLastFrame = false
    }

    fun step(dtRaw: Float, input: ControlState): HudTelemetry {
        val dt = dtRaw.coerceIn(0.001f, 0.045f)
        audioEngine.setActiveDriving(gameState == GameState.PLAYING)

        if (gameState == GameState.SPLASH || gameState == GameState.MENU) {
            worldTime += dt
            carX = CityLayout.SHOWROOM_X
            carY = CityLayout.SHOWROOM_Y
            carZ = CityLayout.SHOWROOM_Z
            yaw = CityLayout.SHOWROOM_YAW
            vx = 0f; vy = 0f; vz = 0f
            chassisPitch = 0f; chassisRoll = 0f
            frontSteerAngle = 0.22f
            wheelSpinAngle = 0f
            isNitroActive = false
            boostTimer = 0f

            val suspStiff = 120f / carType.suspensionSoftness
            val suspAcc = -suspStiff * suspensionY - 13f * suspensionVy
            suspensionVy += suspAcc * dt
            suspensionY = (suspensionY + suspensionVy * dt).coerceIn(-0.18f, 0.16f)

            orbitAngle += dt * 0.30f
            val radius = 6.8f
            val targetCamX = carX + sin(orbitAngle) * radius
            val targetCamY = carY + 2.05f
            val targetCamZ = carZ + cos(orbitAngle) * radius
            if (hypot(camPosX - targetCamX, camPosZ - targetCamZ) > 35f) {
                camPosX = targetCamX
                camPosY = targetCamY
                camPosZ = targetCamZ
            } else {
                camPosX = expLerp(camPosX, targetCamX, 5.5f, dt)
                camPosY = expLerp(camPosY, targetCamY, 5.5f, dt)
                camPosZ = expLerp(camPosZ, targetCamZ, 5.5f, dt)
            }
            camLookX = expLerp(camLookX, carX - 0.35f, 8.0f, dt)
            camLookY = expLerp(camLookY, carY + 0.85f, 8.0f, dt)
            camLookZ = expLerp(camLookZ, carZ, 8.0f, dt)
            cameraFov = expLerp(cameraFov, 48f, 6.0f, dt)

            return buildTelemetry(
                speedAbs = 0f,
                inDriftZone = false,
                isDrifting = false,
                airborneNow = false,
                isReversing = false
            )
        }

        if (gameState == GameState.PAUSED) {
            val speedAbs = hypot(vx, vz)
            return buildTelemetry(
                speedAbs = speedAbs,
                inDriftZone = false,
                isDrifting = false,
                airborneNow = false,
                isReversing = false
            )
        }

        worldTime += dt
        if (helipadRewardCooldown > 0f) helipadRewardCooldown = max(0f, helipadRewardCooldown - dt)
        if (boostCooldown > 0f) boostCooldown = max(0f, boostCooldown - dt)
        if (backfireTimer > 0f) backfireTimer = max(0f, backfireTimer - dt)
        if (sparkCooldown > 0f) sparkCooldown = max(0f, sparkCooldown - dt)
        for (i in ringCooldowns.indices) if (ringCooldowns[i] > 0f) ringCooldowns[i] = max(0f, ringCooldowns[i] - dt)
        for (i in trapCooldowns.indices) if (trapCooldowns[i] > 0f) trapCooldowns[i] = max(0f, trapCooldowns[i] - dt)
        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) {
                currentBanner = StuntBannerType.NONE
                isNewRecordBanner = false
            }
        }

        val forwardX = sin(yaw)
        val forwardZ = cos(yaw)
        val rightX = cos(yaw)
        val rightZ = -sin(yaw)

        var vForward = vx * forwardX + vz * forwardZ
        var vRight = vx * rightX + vz * rightZ

        val groundY = CityLayout.queryGroundHeight(carX, carY, carZ)
        val isGrounded = carY <= groundY + 0.18f

        val digitalSteer = (if (input.steerLeft) 1f else 0f) - (if (input.steerRight) 1f else 0f)
        val combinedSteer = (digitalSteer + input.analogSteer).coerceIn(-1f, 1f)
        val steerInput = combinedSteer * settings.steeringSensitivity.coerceIn(0.5f, 1.5f)
        val targetSteer = steerInput.coerceIn(-1.2f, 1.2f) * 0.48f
        frontSteerAngle += (targetSteer - frontSteerAngle) * min(1f, dt * 13f)

        val targetDriftBlend = if (input.handbrake) 1f else 0f
        driftBlend += (targetDriftBlend - driftBlend) * min(1f, dt * 8.5f)

        // Nitro Boost management
        val wantsNitro = input.nitro && nitroCharge > 2.0f
        isNitroActive = wantsNitro
        if (isNitroActive) {
            nitroCharge = max(0f, nitroCharge - 26f * dt)
        } else {
            // Gentle passive regen at high speed (> 140 km/h)
            if (abs(vForward) > 39f) {
                nitroCharge = min(100f, nitroCharge + 6.5f * dt)
            }
        }

        var targetPitch = 0f
        val topMult = when {
            isNitroActive && boostTimer > 0f -> 1.34f
            isNitroActive -> 1.24f
            boostTimer > 0f -> 1.28f
            else -> 1.0f
        }
        val accelMult = when {
            isNitroActive && boostTimer > 0f -> 1.65f
            isNitroActive -> 1.42f
            boostTimer > 0f -> 1.48f
            else -> 1.0f
        }

        if (isGrounded) {
            val maxSpd = carType.topSpeed * topMult
            if (input.gas || isNitroActive) {
                if (vForward < maxSpd) {
                    vForward = min(maxSpd, vForward + carType.accel * accelMult * dt)
                }
                targetPitch -= (if (isNitroActive) 0.052f else 0.032f) * carType.suspensionSoftness
            }
            if (input.brake) {
                if (vForward > 1.5f) {
                    vForward = max(0f, vForward - 64f * dt)
                    targetPitch += 0.055f * carType.suspensionSoftness
                } else {
                    vForward = max(-carType.reverseSpeed, vForward - 30f * dt)
                }
            }
            if (!input.gas && !input.brake && !isNitroActive) {
                val drag = if (abs(vForward) > 0.5f) 6.5f * dt else 18f * dt
                if (vForward > 0f) vForward = max(0f, vForward - drag)
                else if (vForward < 0f) vForward = min(0f, vForward + drag)
            }
            if (input.handbrake) {
                vForward *= max(0f, 1f - 0.95f * dt)
                targetPitch += 0.024f
            }
        } else if (isNitroActive) {
            // Slight mid-air nitro thrust
            vForward = min(carType.topSpeed * topMult, vForward + 18f * dt)
        }

        if (boostTimer > 0f) {
            boostTimer = max(0f, boostTimer - dt)
            if (isGrounded) {
                vForward = min(carType.topSpeed * topMult, vForward + 36f * dt)
            }
        }

        // Ground boost pads
        if (isGrounded && boostCooldown <= 0f) {
            for (pad in CityLayout.boostPads) {
                if (abs(carX - pad.cx) < pad.halfW && abs(carZ - pad.cz) < pad.halfD) {
                    boostTimer = 1.45f
                    boostCooldown = 1.2f
                    nitroCharge = min(100f, nitroCharge + 20f)
                    vForward = max(vForward + 20f, carType.topSpeed * 0.94f)
                    if (carType.hasBackfireFlames) {
                        backfireTimer = 0.45f
                        audioEngine.triggerBackfire()
                    }
                    audioEngine.triggerBoost()
                    showBanner(StuntBannerType.TURBO_BOOST, 1.5f)
                    break
                }
            }
        }

        // Floating Aerial Boost Rings
        for (i in CityLayout.boostRings.indices) {
            if (ringCooldowns[i] > 0f) continue
            val ring = CityLayout.boostRings[i]
            val dist = sqrt((carX - ring.cx) * (carX - ring.cx) + (carY + 0.6f - ring.cy) * (carY + 0.6f - ring.cy) + (carZ - ring.cz) * (carZ - ring.cz))
            if (dist <= ring.radius) {
                ringCooldowns[i] = 4.0f
                boostTimer = 1.8f
                nitroCharge = min(100f, nitroCharge + 35f)
                totalScore += 1500
                vForward = max(vForward + 18f, carType.topSpeed * 1.08f)
                audioEngine.triggerBoost()
                showBanner(StuntBannerType.BOOST_RING, 1.8f)
                break
            }
        }

        val speedAbs = hypot(vForward, vRight)
        val steerFactor = (abs(vForward) / 9.5f).coerceIn(0f, 1f)
        val reverseSign = if (vForward >= -0.2f) 1f else -1f
        val driftTurnBoost = 1.0f + driftBlend * 0.38f

        // Smart Mobile Drift Assist: counter-steer stabilization prevents spin-outs when sliding steeply
        val driftAssistYaw = if (settings.smartDriftAssist && isGrounded && input.handbrake && abs(vRight) > 6f && abs(combinedSteer) < 0.1f) {
            val slipSign = if (vRight > 0f) -1f else 1f
            slipSign * min(1.0f, abs(vRight) / 28f) * 1.15f
        } else 0f

        if (isGrounded && speedAbs > 0.4f) {
            yaw += (steerInput * carType.turnRate * steerFactor * reverseSign * driftTurnBoost + driftAssistYaw) * dt
        }

        if (isGrounded) {
            val grip = carType.lateralGrip + (carType.driftGrip - carType.lateralGrip) * driftBlend
            val lateralDecay = min(1f, grip * dt)
            vRight *= (1f - lateralDecay)
        }

        val newFwdX = sin(yaw)
        val newFwdZ = cos(yaw)
        val newRightX = cos(yaw)
        val newRightZ = -sin(yaw)

        vx = newFwdX * vForward + newRightX * vRight
        vz = newFwdZ * vForward + newRightZ * vRight

        val targetRoll = -steerInput * (abs(vForward) / carType.topSpeed).coerceIn(0f, 1f) * 0.095f * carType.suspensionSoftness
        chassisPitch += (targetPitch - chassisPitch) * min(1f, dt * 8.5f)
        chassisRoll += (targetRoll - chassisRoll) * min(1f, dt * 8.5f)

        // Spring-damper suspension bounce
        val suspStiff = 120f / carType.suspensionSoftness
        val suspDamp = 13f
        val suspAcc = -suspStiff * suspensionY - suspDamp * suspensionVy
        suspensionVy += suspAcc * dt
        suspensionY = (suspensionY + suspensionVy * dt).coerceIn(-0.18f, 0.16f)

        val prevY = carY
        carX += vx * dt
        carZ += vz * dt
        resolveWorldCollisionsAndSparks()

        val newGroundY = CityLayout.queryGroundHeight(carX, carY, carZ)
        if (isGrounded) {
            val slopeVy = (newGroundY - prevY) / dt
            if (newGroundY > prevY + 0.005f) {
                vy = slopeVy.coerceIn(0f, 26f)
                carY = newGroundY
                suspensionVy -= 0.7f * dt * 60f * 0.14f
            } else if (newGroundY < prevY - 0.18f) {
                // Keep vertical momentum launching off ramp lips or canyon drops
                carY = prevY + vy * dt
            } else {
                carY = newGroundY
                vy = 0f
            }
        } else {
            vy -= 22.5f * dt
            carY += vy * dt
            if (carY <= newGroundY) {
                val impact = -vy
                carY = newGroundY
                vy = 0f
                if (impact > 3.5f) {
                    chassisPitch += 0.065f
                    suspensionVy -= (impact * 0.14f).coerceIn(0.4f, 2.6f)
                }
            }
        }

        if (carY < CityLayout.WATER_LEVEL + 0.35f) {
            resetCar()
        }

        // Speed Trap Radars
        for (i in CityLayout.speedTraps.indices) {
            if (trapCooldowns[i] > 0f) continue
            val trap = CityLayout.speedTraps[i]
            if (abs(carX - trap.cx) <= trap.halfW && abs(carZ - trap.cz) <= trap.halfD && speedAbs > 15f) {
                trapCooldowns[i] = 4.5f
                val kmh = (speedAbs * 3.6f).toInt()
                lastTrapSpeedKmh = kmh
                isNewRecordBanner = kmh > bestTrapSpeedKmh
                if (isNewRecordBanner) {
                    bestTrapSpeedKmh = kmh
                    totalScore += 1200 + kmh * 3
                } else {
                    totalScore += kmh * 2
                }
                showBanner(StuntBannerType.SPEED_TRAP, 2.4f)
                break
            }
        }

        // Airtime stunt scoring
        val airborneNow = carY > newGroundY + 0.55f
        if (airborneNow) {
            airtimeDuration += dt
            wasAirborne = true
        } else {
            if (wasAirborne && airtimeDuration > 0.55f) {
                totalScore += (airtimeDuration * 650).toInt()
                showBanner(StuntBannerType.AIRTIME, 1.5f)
            }
            wasAirborne = false
            airtimeDuration = 0f
        }

        // Rooftop Helipad landing bonus
        if (helipadRewardCooldown <= 0f &&
            carY >= CityLayout.HELIPAD_HEIGHT - 0.35f &&
            hypot(carX - CityLayout.HELIPAD_CX, carZ - CityLayout.HELIPAD_CZ) < 16f
        ) {
            totalScore += 5000
            helipadRewardCooldown = 12f
            showBanner(StuntBannerType.HELIPAD_LANDING, 2.2f)
        }

        wheelSpinAngle += (vForward / 0.36f) * dt

        if (carType.hasBackfireFlames && !input.gas && vForward > 38f && Random.nextFloat() < dt * 2.5f) {
            backfireTimer = 0.22f
            audioEngine.triggerBackfire()
        }

        // Check Drift Zones (2x multiplier)
        var inDriftZone = false
        for (dz in CityLayout.driftZones) {
            if (hypot(carX - dz.cx, carZ - dz.cz) <= dz.radius) {
                inDriftZone = true
                break
            }
        }

        var recordsDirty = false
        val currentSpeedKmh = (speedAbs * 3.6f).toInt()
        if (currentSpeedKmh > topSpeedKmh) {
            topSpeedKmh = currentSpeedKmh
            recordsDirty = true
        }

        val lateralSpeed = abs(vRight)
        val isDrifting = isGrounded && speedAbs > 10f && (lateralSpeed > 3.8f || (input.handbrake && abs(combinedSteer) > 0.1f))
        if (isDrifting) {
            driftEndGrace = 0.9f
            val zoneBonus = if (inDriftZone) 2.0f else 1.0f
            val pointsDelta = (lateralSpeed * 14f + speedAbs * 2.5f) * zoneBonus * dt
            activeDriftScore += pointsDelta
            nitroCharge = min(100f, nitroCharge + 22f * dt)
            driftMultiplier = when {
                activeDriftScore > 2500f -> 5
                activeDriftScore > 1200f -> 4
                activeDriftScore > 500f -> 3
                activeDriftScore > 180f -> 2
                else -> 1
            } * (if (inDriftZone) 2 else 1)
            val comboScore = (activeDriftScore * driftMultiplier).toInt()
            if (comboScore > bestDriftScore) {
                bestDriftScore = comboScore
                recordsDirty = true
            }
            if (settings.graphicsPreset.particlesEnabled) {
                spawnDriftEffects(newFwdX, newFwdZ, newRightX, newRightZ, groundY)
            }
        } else {
            hadSkidLastFrame = false
            if (driftEndGrace > 0f) {
                driftEndGrace -= dt
                if (driftEndGrace <= 0f && activeDriftScore > 15f) {
                    val comboScore = (activeDriftScore * driftMultiplier).toInt()
                    if (comboScore > bestDriftScore) {
                        bestDriftScore = comboScore
                        recordsDirty = true
                    }
                    totalScore += comboScore
                    activeDriftScore = 0f
                    driftMultiplier = 1
                }
            }
        }

        if (totalScore > bestTotalScore) {
            bestTotalScore = totalScore
            recordsDirty = true
        }

        if (recordsDirty) {
            onRecordsChanged?.invoke(
                SavedRecords(
                    bestDriftScore = bestDriftScore,
                    bestTotalScore = bestTotalScore,
                    topSpeedKmh = topSpeedKmh
                )
            )
        }

        // Night Tail-Light Trail recording when fast or in night mode
        if (timeOfDay == TimeOfDay.NIGHT && settings.graphicsPreset.headlightsAllowed && speedAbs > 16f) {
            recordTailTrail(newFwdX, newFwdZ, newRightX, newRightZ)
        } else {
            hadTailLastFrame = false
        }

        updateGearbox(vForward, input.gas || isNitroActive)
        updateParticlesAndSkids(dt)
        updateCamera(dt, newFwdX, newFwdZ, newRightX, newRightZ, speedAbs)

        val driftNorm = if (isDrifting) (lateralSpeed / 18f).coerceIn(0.2f, 1f) else 0f
        audioEngine.update(carType, vForward, currentGear, rpmNormalized, input.gas || isNitroActive, driftNorm, isNitroActive)

        return buildTelemetry(
            speedAbs = speedAbs,
            inDriftZone = inDriftZone,
            isDrifting = isDrifting,
            airborneNow = airborneNow,
            isReversing = vForward < -0.5f && input.brake
        )
    }

    private fun buildTelemetry(
        speedAbs: Float,
        inDriftZone: Boolean,
        isDrifting: Boolean,
        airborneNow: Boolean,
        isReversing: Boolean
    ): HudTelemetry {
        val district = CityLayout.getDistrict(carX, carZ)
        return HudTelemetry(
            gameState = gameState,
            speedKmh = (speedAbs * 3.6f).toInt(),
            gear = currentGear,
            rpmNormalized = rpmNormalized,
            nitroCharge = nitroCharge,
            isNitroActive = isNitroActive,
            activeDriftScore = activeDriftScore.toInt(),
            driftMultiplier = driftMultiplier,
            totalScore = totalScore,
            bestTrapSpeedKmh = bestTrapSpeedKmh,
            lastTrapSpeedKmh = lastTrapSpeedKmh,
            bestDriftScore = bestDriftScore,
            topSpeedKmh = topSpeedKmh,
            isNewRecord = isNewRecordBanner,
            inDriftZone = inDriftZone,
            isDrifting = isDrifting,
            isBoosting = boostTimer > 0f || isNitroActive,
            isAirborne = airborneNow,
            isReversing = isReversing,
            bannerType = currentBanner,
            carType = carType,
            paintIndex = paintIndex,
            paintFinish = paintFinish,
            cameraMode = cameraMode,
            timeOfDay = timeOfDay,
            district = district,
            carX = carX,
            carZ = carZ,
            carYaw = yaw,
            fps = fps,
            isMuted = isMuted || settings.isMuted
        )
    }

    private fun updateGearbox(vForward: Float, throttleOn: Boolean) {
        if (vForward < -0.5f) {
            currentGear = 0 // Reverse
            rpmNormalized = (0.18f + (abs(vForward) / carType.reverseSpeed) * 0.65f).coerceIn(0.15f, 0.95f)
            return
        }
        val top = carType.topSpeed * 1.18f
        val normSpeed = (abs(vForward) / top).coerceIn(0f, 1.15f)
        val thresholds = floatArrayOf(0f, 0.16f, 0.34f, 0.54f, 0.74f, 0.92f, 1.25f)
        var gear = 1
        for (g in 1..6) {
            if (normSpeed >= thresholds[g - 1]) gear = g
        }
        currentGear = gear
        val gearMin = thresholds[gear - 1]
        val gearMax = thresholds[gear]
        val inGearRatio = ((normSpeed - gearMin) / (gearMax - gearMin)).coerceIn(0f, 1f)
        val targetRpm = (0.20f + inGearRatio * 0.78f + if (throttleOn) 0.05f else -0.06f).coerceIn(0.12f, 1.0f)
        rpmNormalized += (targetRpm - rpmNormalized) * 0.25f
    }

    private fun showBanner(type: StuntBannerType, duration: Float) {
        currentBanner = type
        bannerTimer = duration
        if (type != StuntBannerType.SPEED_TRAP) {
            isNewRecordBanner = false
        }
    }

    /**
     * Wall-Slide Collision Physics: deflects velocity tangentially along barriers/buildings
     * with orange sparks instead of stopping dead or clipping.
     */
    private fun resolveWorldCollisionsAndSparks() {
        val carRadius = 1.95f

        // 1. Grand Central Roundabout Monument
        val distCenter = hypot(carX, carZ)
        val minCenterR = CityLayout.ROUNDABOUT_MONUMENT_R + carRadius
        if (distCenter < minCenterR && distCenter > 0.001f) {
            val nx = carX / distCenter
            val nz = carZ / distCenter
            carX = nx * minCenterR
            carZ = nz * minCenterR
            applyWallSlide(nx, nz)
        }

        // 2. Skyscraper AABBs (only evaluate within 60m broadphase for O(1) cost)
        for (b in CityLayout.buildings) {
            if (abs(carX - b.cx) > b.halfW + 8f || abs(carZ - b.cz) > b.halfD + 8f) continue
            resolveBoxWallSlide(b.cx, b.cz, b.halfW, b.halfD, b.height, carRadius)
        }

        // 3. Rooftop Helipad support tower
        resolveBoxWallSlide(
            CityLayout.HELIPAD_CX,
            CityLayout.HELIPAD_CZ,
            CityLayout.HELIPAD_HALF_SIZE,
            CityLayout.HELIPAD_HALF_SIZE,
            CityLayout.HELIPAD_HEIGHT - 0.8f,
            carRadius
        )

        // 4. Canyon Mountain Touge guardrails (keep car drifting cleanly on elevated ribbons!)
        if (carX < -440f && carZ in CityLayout.CANYON_END_Z..CityLayout.CANYON_START_Z && carY > 1.2f) {
            val summitX = CityLayout.canyonRoadX(CityLayout.CANYON_END_Z)
            val distSummit = hypot(carX - summitX, carZ - CityLayout.CANYON_END_Z)
            val onSummit = distSummit <= CityLayout.CANYON_SUMMIT_PLATEAU_R
            if (!onSummit) {
                val roadX = CityLayout.canyonRoadX(carZ)
                val dxdz = CityLayout.canyonRoadSlopeX(carZ)
                val halfBandX = (CityLayout.CANYON_ROAD_HALF_W - 1.5f) * sqrt(1f + dxdz * dxdz)
                val dx = carX - roadX
                if (abs(dx) > halfBandX) {
                    val sign = if (dx > 0f) 1f else -1f
                    carX = roadX + sign * halfBandX
                    // Normal pointing back into canyon road
                    val invLen = 1f / sqrt(1f + dxdz * dxdz)
                    val nx = -sign * invLen
                    val nz = sign * dxdz * invLen
                    applyWallSlide(nx, nz)
                }
            }
        }

        // 5. Island perimeter seawall guardrails (keeps high-speed runs crisp along coastline)
        val wallWest = -CityLayout.WORLD_HALF_SIZE + 8f
        val wallEast = CityLayout.EAST_COAST_X - 8f
        val wallNorth = -CityLayout.WORLD_HALF_SIZE + 8f
        val wallSouth = CityLayout.WORLD_HALF_SIZE - 8f
        if (carX < wallWest) { carX = wallWest; applyWallSlide(1f, 0f) }
        if (carX > wallEast && carZ !in (CityLayout.MARINA_CANAL_MIN_Z - 4f)..(CityLayout.MARINA_CANAL_MAX_Z + 4f)) {
            carX = wallEast; applyWallSlide(-1f, 0f)
        }
        if (carZ < wallNorth) { carZ = wallNorth; applyWallSlide(0f, 1f) }
        if (carZ > wallSouth) { carZ = wallSouth; applyWallSlide(0f, -1f) }
    }

    private fun resolveBoxWallSlide(
        cx: Float, cz: Float, halfW: Float, halfD: Float, height: Float, carRadius: Float
    ) {
        if (carY >= height) return
        val dx = carX - cx
        val dz = carZ - cz
        val overlapX = (halfW + carRadius) - abs(dx)
        val overlapZ = (halfD + carRadius) - abs(dz)
        if (overlapX > 0f && overlapZ > 0f) {
            if (overlapX < overlapZ) {
                val sx = if (dx >= 0f) 1f else -1f
                carX += sx * overlapX
                applyWallSlide(sx, 0f)
            } else {
                val sz = if (dz >= 0f) 1f else -1f
                carZ += sz * overlapZ
                applyWallSlide(0f, sz)
            }
        }
    }

    private fun applyWallSlide(nx: Float, nz: Float) {
        val vn = vx * nx + vz * nz
        if (vn < 0f) {
            // Tangential slide preservation (90% tangential speed preserved, crisp bounce off normal)
            vx -= 1.25f * vn * nx
            vz -= 1.25f * vn * nz
            vx *= 0.92f
            vz *= 0.92f
            if (settings.graphicsPreset.particlesEnabled && abs(vn) > 3.5f && sparkCooldown <= 0f) {
                sparkCooldown = 0.045f
                spawnWallSparks(carX - nx * 1.4f, carY + 0.55f, carZ - nz * 1.4f, nx, nz)
            }
        }
    }

    private fun spawnWallSparks(sx: Float, sy: Float, sz: Float, nx: Float, nz: Float) {
        repeat(7) {
            val sp = sparkParticles[sparkCursor]
            sparkCursor = (sparkCursor + 1) % sparkParticles.size
            sp.active = true
            sp.x = sx
            sp.y = sy
            sp.z = sz
            sp.vx = vx * 0.35f + nx * (4f + Random.nextFloat() * 7f) + (Random.nextFloat() - 0.5f) * 8f
            sp.vy = 2.5f + Random.nextFloat() * 5.5f
            sp.vz = vz * 0.35f + nz * (4f + Random.nextFloat() * 7f) + (Random.nextFloat() - 0.5f) * 8f
            sp.age = 0f
            sp.maxAge = 0.24f + Random.nextFloat() * 0.18f
        }
    }

    private fun recordTailTrail(fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float) {
        val tailOffset = -2.36f
        val trackHalf = 0.62f
        val ty = carY + suspensionY + 0.68f
        val tlx = carX + fwdX * tailOffset - rightX * trackHalf
        val tlz = carZ + fwdZ * tailOffset - rightZ * trackHalf
        val trx = carX + fwdX * tailOffset + rightX * trackHalf
        val trz = carZ + fwdZ * tailOffset + rightZ * trackHalf

        if (hadTailLastFrame) {
            val d = hypot(tlx - prevTailLX, tlz - prevTailLZ)
            if (d in 0.15f..6.0f) {
                val segL = tailTrails[tailCursor]
                tailCursor = (tailCursor + 1) % tailTrails.size
                segL.active = true; segL.x1 = prevTailLX; segL.y1 = prevTailLY; segL.z1 = prevTailLZ
                segL.x2 = tlx; segL.y2 = ty; segL.z2 = tlz; segL.life = 1.0f

                val segR = tailTrails[tailCursor]
                tailCursor = (tailCursor + 1) % tailTrails.size
                segR.active = true; segR.x1 = prevTailRX; segR.y1 = prevTailRY; segR.z1 = prevTailRZ
                segR.x2 = trx; segR.y2 = ty; segR.z2 = trz; segR.life = 1.0f
            }
        }
        prevTailLX = tlx; prevTailLY = ty; prevTailLZ = tlz
        prevTailRX = trx; prevTailRY = ty; prevTailRZ = trz
        hadTailLastFrame = true
    }

    private fun spawnDriftEffects(
        fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float, groundY: Float
    ) {
        val rearOffset = -1.38f
        val trackHalf = 0.94f
        val rlx = carX + fwdX * rearOffset - rightX * trackHalf
        val rlz = carZ + fwdZ * rearOffset - rightZ * trackHalf
        val rrx = carX + fwdX * rearOffset + rightX * trackHalf
        val rrz = carZ + fwdZ * rearOffset + rightZ * trackHalf

        if (hadSkidLastFrame) {
            val dL = hypot(rlx - prevRearLeftX, rlz - prevRearLeftZ)
            if (dL in 0.1f..3.5f) {
                val segL = skidSegments[skidCursor]
                skidCursor = (skidCursor + 1) % skidSegments.size
                segL.active = true; segL.x1 = prevRearLeftX; segL.z1 = prevRearLeftZ
                segL.x2 = rlx; segL.z2 = rlz; segL.y = groundY + 0.04f; segL.life = 1.0f

                val segR = skidSegments[skidCursor]
                skidCursor = (skidCursor + 1) % skidSegments.size
                segR.active = true; segR.x1 = prevRearRightX; segR.z1 = prevRearRightZ
                segR.x2 = rrx; segR.z2 = rrz; segR.y = groundY + 0.04f; segR.life = 1.0f
            }
        }
        prevRearLeftX = rlx; prevRearLeftZ = rlz
        prevRearRightX = rrx; prevRearRightZ = rrz
        hadSkidLastFrame = true

        for (side in -1..1 step 2) {
            val p = smokeParticles[smokeCursor]
            smokeCursor = (smokeCursor + 1) % smokeParticles.size
            p.active = true
            p.x = if (side < 0) rlx else rrx
            p.y = groundY + 0.25f
            p.z = if (side < 0) rlz else rrz
            p.vx = (Random.nextFloat() - 0.5f) * 1.4f
            p.vy = 0.85f + Random.nextFloat() * 1.1f
            p.vz = (Random.nextFloat() - 0.5f) * 1.4f
            p.age = 0f
            p.maxAge = 0.7f + Random.nextFloat() * 0.35f
            p.size = 0.48f
        }
    }

    private fun updateParticlesAndSkids(dt: Float) {
        for (p in smokeParticles) {
            if (!p.active) continue
            p.age += dt
            if (p.age >= p.maxAge) { p.active = false; continue }
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt
            p.size += 1.75f * dt
        }
        for (sp in sparkParticles) {
            if (!sp.active) continue
            sp.age += dt
            if (sp.age >= sp.maxAge) { sp.active = false; continue }
            sp.vy -= 18f * dt
            sp.x += sp.vx * dt; sp.y += sp.vy * dt; sp.z += sp.vz * dt
        }
        for (s in skidSegments) {
            if (!s.active) continue
            s.life -= dt * 0.11f
            if (s.life <= 0f) s.active = false
        }
        for (t in tailTrails) {
            if (!t.active) continue
            t.life -= dt * 1.85f
            if (t.life <= 0f) t.active = false
        }
    }

    /**
     * Rock-solid exponential smoothing camera (horizon locked, velocity look-ahead, zero jitter).
     */
    private fun expLerp(current: Float, target: Float, stiffness: Float, dt: Float): Float {
        val alpha = 1f - exp(-stiffness * dt)
        return current + (target - current) * alpha
    }

    private fun updateCamera(
        dt: Float, fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float, speedAbs: Float
    ) {
        val speedRatio = (speedAbs / 90f).coerceIn(0f, 1.35f)
        val highSpeedFov = if (speedAbs > 50f) ((speedAbs - 50f) * 0.22f).coerceIn(0f, 12f) else 0f
        val boostFov = if (isNitroActive || boostTimer > 0f) 7.5f else 0f
        val targetFov = when (cameraMode) {
            CameraMode.CHASE -> 58f + speedRatio * 10f + highSpeedFov + boostFov
            CameraMode.SIDE_ACTION -> 52f + speedRatio * 7f
            CameraMode.HOOD -> 68f + speedRatio * 9f + highSpeedFov + boostFov
            CameraMode.ORBIT -> 52f
        }
        cameraFov = expLerp(cameraFov, targetFov, 7.0f, dt)

        // Velocity look-ahead blends slightly into drift/turn vector
        val velLeadX = (vx * 0.08f).coerceIn(-4.5f, 4.5f)
        val velLeadZ = (vz * 0.08f).coerceIn(-4.5f, 4.5f)

        when (cameraMode) {
            CameraMode.CHASE -> {
                val dist = 7.4f + speedRatio * 1.6f
                val tx = carX - fwdX * dist
                val ty = carY + 2.65f + speedRatio * 0.35f
                val tz = carZ - fwdZ * dist
                camPosX = expLerp(camPosX, tx, 10.5f, dt)
                camPosY = expLerp(camPosY, ty, 8.5f, dt)
                camPosZ = expLerp(camPosZ, tz, 10.5f, dt)

                camLookX = expLerp(camLookX, carX + fwdX * 4.2f + velLeadX, 14f, dt)
                camLookY = expLerp(camLookY, carY + 1.10f, 12f, dt)
                camLookZ = expLerp(camLookZ, carZ + fwdZ * 4.2f + velLeadZ, 14f, dt)
            }

            CameraMode.SIDE_ACTION -> {
                val sideDist = 6.9f
                val tx = carX + rightX * sideDist + fwdX * 0.9f
                val ty = carY + 1.60f
                val tz = carZ + rightZ * sideDist + fwdZ * 0.9f
                camPosX = expLerp(camPosX, tx, 9.5f, dt)
                camPosY = expLerp(camPosY, ty, 8.0f, dt)
                camPosZ = expLerp(camPosZ, tz, 9.5f, dt)

                camLookX = expLerp(camLookX, carX + fwdX * 0.6f, 14f, dt)
                camLookY = expLerp(camLookY, carY + 0.72f, 12f, dt)
                camLookZ = expLerp(camLookZ, carZ + fwdZ * 0.6f, 14f, dt)
            }

            CameraMode.HOOD -> {
                camPosX = carX + fwdX * 0.55f
                camPosY = carY + 1.18f + suspensionY * 0.5f
                camPosZ = carZ + fwdZ * 0.55f
                camLookX = carX + fwdX * 20f + velLeadX * 0.5f
                camLookY = carY + 1.05f - chassisPitch * 6f
                camLookZ = carZ + fwdZ * 20f + velLeadZ * 0.5f
            }

            CameraMode.ORBIT -> {
                orbitAngle += dt * 0.55f
                val radius = 7.4f
                camPosX = carX + sin(orbitAngle) * radius
                camPosY = carY + 2.45f
                camPosZ = carZ + cos(orbitAngle) * radius
                camLookX = carX
                camLookY = carY + 0.85f
                camLookZ = carZ
            }
        }
    }
}
