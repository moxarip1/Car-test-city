package com.example.game

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class BuildingSpec(
    val cx: Float,
    val cz: Float,
    val halfW: Float,
    val halfD: Float,
    val height: Float,
    val accentCyan: Boolean
)

data class BoostPadSpec(
    val cx: Float,
    val cz: Float,
    val halfW: Float = 5.5f,
    val halfD: Float = 4.5f
)

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

data class SkidSegment(
    var active: Boolean = false,
    var x1: Float = 0f,
    var z1: Float = 0f,
    var x2: Float = 0f,
    var z2: Float = 0f,
    var y: Float = 0.03f,
    var life: Float = 1.0f
)

object CityLayout {
    const val ISLAND_HALF_SIZE = 145f
    const val WATER_LEVEL = -1.6f
    const val ROUNDABOUT_MONUMENT_R = 13.5f

    const val CANAL_MIN_X = 44f
    const val CANAL_MAX_X = 96f
    const val CANAL_MIN_Z = 36f
    const val CANAL_MAX_Z = 68f

    const val HELIPAD_MIN_X = 102f
    const val HELIPAD_MAX_X = 134f
    const val HELIPAD_MIN_Z = 42f
    const val HELIPAD_MAX_Z = 74f
    const val HELIPAD_HEIGHT = 7.2f
    const val HELIPAD_CX = 118f
    const val HELIPAD_CZ = 58f

    val buildings = listOf(
        BuildingSpec(-54f, -54f, 11f, 11f, 44f, accentCyan = true),
        BuildingSpec(-86f, -54f, 12f, 11f, 58f, accentCyan = false),
        BuildingSpec(-54f, -88f, 11f, 12f, 52f, accentCyan = false),
        BuildingSpec(-86f, -88f, 13f, 13f, 68f, accentCyan = true),
        BuildingSpec(-54f, 54f, 11f, 11f, 38f, accentCyan = false),
        BuildingSpec(-86f, 54f, 12f, 12f, 48f, accentCyan = true),
        BuildingSpec(-54f, 88f, 11f, 11f, 42f, accentCyan = true),
        BuildingSpec(-86f, 88f, 12f, 12f, 34f, accentCyan = false),
        BuildingSpec(94f, -56f, 10f, 12f, 46f, accentCyan = true),
        BuildingSpec(94f, -92f, 11f, 11f, 54f, accentCyan = false)
    )

    val boostPads = listOf(
        BoostPadSpec(57f, -96f),
        BoostPadSpec(57f, -68f),
        BoostPadSpec(57f, -40f),
        BoostPadSpec(-115f, 0f, halfW = 4.5f, halfD = 5.5f)
    )

    fun queryGroundHeight(x: Float, y: Float, z: Float): Float {
        if (x in 110f..126f && z in 6f..42f) {
            val t = ((z - 6f) / 36f).coerceIn(0f, 1f)
            return t * HELIPAD_HEIGHT
        }
        if (x in HELIPAD_MIN_X..HELIPAD_MAX_X && z in HELIPAD_MIN_Z..HELIPAD_MAX_Z) {
            if (y >= HELIPAD_HEIGHT - 1.5f) return HELIPAD_HEIGHT
        }
        if (x in 53f..65f) {
            if (z in 20f..36f) return ((z - 20f) / 16f) * 4.8f
            if (z in 68f..84f) return ((84f - z) / 16f) * 4.8f
        }
        if (x in 75f..87f) {
            if (z in 20f..36f) return ((z - 20f) / 16f) * 4.8f
            if (z in 68f..84f) return ((84f - z) / 16f) * 4.8f
        }
        if (x in CANAL_MIN_X..CANAL_MAX_X && z in CANAL_MIN_Z..CANAL_MAX_Z) {
            return WATER_LEVEL
        }
        if (abs(x) > ISLAND_HALF_SIZE || abs(z) > ISLAND_HALF_SIZE) {
            return WATER_LEVEL
        }
        return 0f
    }
}

class GameWorldPhysics(private val audioEngine: CarAudioEngine) {
    // Car spatial state
    var carX = 0f
    var carY = 0.05f
    var carZ = -62f
    var yaw = 0f
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
    var cameraFov = 60f
    private var driftBlend = 0f

    // Configuration
    var carType = CarType.EV_HYPER_GT
    var paintIndex = CarType.EV_HYPER_GT.defaultPaintIndex
    var paintFinish = PaintFinish.GLOSSY
    var cameraMode = CameraMode.CHASE
    var isMuted = false

    // Scoring & Stunt banners
    var activeDriftScore = 0f
    var driftMultiplier = 1
    var totalScore = 0
    private var driftEndGrace = 0f
    private var wasAirborne = false
    private var airtimeDuration = 0f
    private var helipadRewardCooldown = 0f
    private var bannerTimer = 0f
    private var currentBanner = StuntBannerType.NONE
    private var boostCooldown = 0f

    // Fixed-size pools for zero-GC instanced particles & skid marks
    val smokeParticles = Array(128) { SmokeParticle() }
    private var smokeCursor = 0
    val skidSegments = Array(260) { SkidSegment() }
    private var skidCursor = 0
    private var prevRearLeftX = 0f
    private var prevRearLeftZ = 0f
    private var prevRearRightX = 0f
    private var prevRearRightZ = 0f
    private var hadSkidLastFrame = false

    // Critically-damped spring camera state
    var camPosX = 0f
    var camPosY = 3.8f
    var camPosZ = -71f
    private var camVelX = 0f
    private var camVelY = 0f
    private var camVelZ = 0f
    var camLookX = 0f
    var camLookY = 1.2f
    var camLookZ = -60f
    private var lookVelX = 0f
    private var lookVelY = 0f
    private var lookVelZ = 0f
    var orbitAngle = 0.6f
    var worldTime = 0f

    fun switchCar(newType: CarType) {
        if (carType != newType) {
            carType = newType
            paintIndex = newType.defaultPaintIndex
            suspensionVy -= 1.2f
        }
    }

    fun resetCar() {
        val candidates = arrayOf(
            floatArrayOf(0f, -62f, 0f),
            floatArrayOf(0f, 62f, 3.14159f),
            floatArrayOf(-62f, 0f, 1.5708f),
            floatArrayOf(57f, -104f, 0f),
            floatArrayOf(59f, 16f, 0f)
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
        carY = 0.25f
        carZ = best[1]
        yaw = best[2]
        vx = 0f; vy = 0f; vz = 0f
        chassisPitch = 0f; chassisRoll = 0f
        suspensionY = 0f; suspensionVy = 0f
        boostTimer = 0f
        hadSkidLastFrame = false
        camVelX = 0f; camVelY = 0f; camVelZ = 0f
        lookVelX = 0f; lookVelY = 0f; lookVelZ = 0f
    }

    fun step(dtRaw: Float, input: ControlState): HudTelemetry {
        val dt = dtRaw.coerceIn(0.001f, 0.045f)
        worldTime += dt
        if (helipadRewardCooldown > 0f) helipadRewardCooldown = max(0f, helipadRewardCooldown - dt)
        if (boostCooldown > 0f) boostCooldown = max(0f, boostCooldown - dt)
        if (backfireTimer > 0f) backfireTimer = max(0f, backfireTimer - dt)
        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) currentBanner = StuntBannerType.NONE
        }

        val forwardX = sin(yaw)
        val forwardZ = cos(yaw)
        val rightX = cos(yaw)
        val rightZ = -sin(yaw)

        var vForward = vx * forwardX + vz * forwardZ
        var vRight = vx * rightX + vz * rightZ

        val groundY = CityLayout.queryGroundHeight(carX, carY, carZ)
        val isGrounded = carY <= groundY + 0.14f

        val steerInput = (if (input.steerLeft) 1f else 0f) - (if (input.steerRight) 1f else 0f)
        val targetSteer = steerInput * 0.48f
        frontSteerAngle += (targetSteer - frontSteerAngle) * min(1f, dt * 13f)

        // Progressive drift blend when holding handbrake
        val targetDriftBlend = if (input.handbrake) 1f else 0f
        driftBlend += (targetDriftBlend - driftBlend) * min(1f, dt * 8f)

        var targetPitch = 0f
        if (isGrounded) {
            if (input.gas) {
                val boostMult = if (boostTimer > 0f) 1.48f else 1.0f
                val maxSpd = carType.topSpeed * (if (boostTimer > 0f) 1.28f else 1.0f)
                if (vForward < maxSpd) {
                    vForward = min(maxSpd, vForward + carType.accel * boostMult * dt)
                }
                targetPitch -= 0.032f * carType.suspensionSoftness
            }
            if (input.brake) {
                if (vForward > 1.5f) {
                    vForward = max(0f, vForward - 60f * dt)
                    targetPitch += 0.050f * carType.suspensionSoftness
                } else {
                    vForward = max(-carType.reverseSpeed, vForward - 28f * dt)
                }
            }
            if (!input.gas && !input.brake) {
                val drag = if (abs(vForward) > 0.5f) 7.2f * dt else 18f * dt
                if (vForward > 0f) vForward = max(0f, vForward - drag)
                else if (vForward < 0f) vForward = min(0f, vForward + drag)
            }
            if (input.handbrake) {
                vForward *= max(0f, 1f - 1.05f * dt)
                targetPitch += 0.025f
            }
        }

        if (boostTimer > 0f) {
            boostTimer = max(0f, boostTimer - dt)
            vForward = min(carType.topSpeed * 1.30f, vForward + 36f * dt)
        }
        if (isGrounded && boostCooldown <= 0f) {
            for (pad in CityLayout.boostPads) {
                if (abs(carX - pad.cx) < pad.halfW && abs(carZ - pad.cz) < pad.halfD) {
                    boostTimer = 1.45f
                    boostCooldown = 1.2f
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

        val speedAbs = hypot(vForward, vRight)
        val steerFactor = (abs(vForward) / 9.5f).coerceIn(0f, 1f)
        val reverseSign = if (vForward >= -0.2f) 1f else -1f
        val driftTurnBoost = 1.0f + driftBlend * 0.40f
        if (isGrounded && speedAbs > 0.4f) {
            yaw += steerInput * carType.turnRate * steerFactor * reverseSign * driftTurnBoost * dt
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

        val targetRoll = -steerInput * (abs(vForward) / carType.topSpeed).coerceIn(0f, 1f) * 0.09f * carType.suspensionSoftness
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
        resolveWorldCollisions()

        val newGroundY = CityLayout.queryGroundHeight(carX, carY, carZ)
        if (isGrounded) {
            val slopeVy = (newGroundY - prevY) / dt
            if (newGroundY > prevY + 0.005f) {
                vy = slopeVy.coerceIn(0f, 24f)
                carY = newGroundY
                suspensionVy -= 0.8f * dt * 60f * 0.15f
            } else if (newGroundY < prevY - 0.18f) {
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

        if (helipadRewardCooldown <= 0f &&
            carY >= CityLayout.HELIPAD_HEIGHT - 0.3f &&
            hypot(carX - CityLayout.HELIPAD_CX, carZ - CityLayout.HELIPAD_CZ) < 12f
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

        val lateralSpeed = abs(vRight)
        val isDrifting = isGrounded && speedAbs > 10f && (lateralSpeed > 4.0f || (input.handbrake && abs(steerInput) > 0.1f))
        if (isDrifting) {
            driftEndGrace = 0.9f
            val pointsDelta = (lateralSpeed * 14f + speedAbs * 2.5f) * dt
            activeDriftScore += pointsDelta
            driftMultiplier = when {
                activeDriftScore > 2500f -> 5
                activeDriftScore > 1200f -> 4
                activeDriftScore > 500f -> 3
                activeDriftScore > 180f -> 2
                else -> 1
            }
            spawnDriftEffects(newFwdX, newFwdZ, newRightX, newRightZ, groundY)
        } else {
            hadSkidLastFrame = false
            if (driftEndGrace > 0f) {
                driftEndGrace -= dt
                if (driftEndGrace <= 0f && activeDriftScore > 15f) {
                    totalScore += (activeDriftScore * driftMultiplier).toInt()
                    activeDriftScore = 0f
                    driftMultiplier = 1
                }
            }
        }

        updateParticlesAndSkids(dt)
        updateCamera(dt, newFwdX, newFwdZ, newRightX, newRightZ, speedAbs)

        val driftNorm = if (isDrifting) (lateralSpeed / 18f).coerceIn(0.2f, 1f) else 0f
        audioEngine.update(carType, vForward, input.gas, driftNorm)

        return HudTelemetry(
            speedKmh = (speedAbs * 3.6f).toInt(),
            activeDriftScore = activeDriftScore.toInt(),
            driftMultiplier = driftMultiplier,
            totalScore = totalScore,
            isDrifting = isDrifting,
            isBoosting = boostTimer > 0f,
            isAirborne = airborneNow,
            isReversing = vForward < -0.5f && input.brake,
            bannerType = currentBanner,
            carType = carType,
            paintIndex = paintIndex,
            paintFinish = paintFinish,
            cameraMode = cameraMode,
            isMuted = isMuted
        )
    }

    private fun showBanner(type: StuntBannerType, duration: Float) {
        currentBanner = type
        bannerTimer = duration
    }

    private fun resolveWorldCollisions() {
        val carRadius = 1.95f
        val distCenter = hypot(carX, carZ)
        val minCenterR = CityLayout.ROUNDABOUT_MONUMENT_R + carRadius
        if (distCenter < minCenterR && distCenter > 0.001f) {
            val nx = carX / distCenter
            val nz = carZ / distCenter
            carX = nx * minCenterR
            carZ = nz * minCenterR
            val vn = vx * nx + vz * nz
            if (vn < 0f) {
                vx -= 1.35f * vn * nx
                vz -= 1.35f * vn * nz
            }
        }
        for (b in CityLayout.buildings) {
            resolveBoxCollision(b.cx, b.cz, b.halfW, b.halfD, b.height, carRadius)
        }
        resolveBoxCollision(
            CityLayout.HELIPAD_CX,
            CityLayout.HELIPAD_CZ,
            16f,
            16f,
            CityLayout.HELIPAD_HEIGHT - 0.8f,
            carRadius
        )
    }

    private fun resolveBoxCollision(
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
                vx = -vx * 0.35f
                vz *= 0.82f
            } else {
                val sz = if (dz >= 0f) 1f else -1f
                carZ += sz * overlapZ
                vz = -vz * 0.35f
                vx *= 0.82f
            }
        }
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
            if (dL in 0.1f..3.0f) {
                val segL = skidSegments[skidCursor]
                skidCursor = (skidCursor + 1) % skidSegments.size
                segL.active = true; segL.x1 = prevRearLeftX; segL.z1 = prevRearLeftZ
                segL.x2 = rlx; segL.z2 = rlz; segL.y = groundY + 0.038f; segL.life = 1.0f

                val segR = skidSegments[skidCursor]
                skidCursor = (skidCursor + 1) % skidSegments.size
                segR.active = true; segR.x1 = prevRearRightX; segR.z1 = prevRearRightZ
                segR.x2 = rrx; segR.z2 = rrz; segR.y = groundY + 0.038f; segR.life = 1.0f
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
        for (s in skidSegments) {
            if (!s.active) continue
            s.life -= dt * 0.11f
            if (s.life <= 0f) s.active = false
        }
    }

    /**
     * Critically-damped spring follow for zero-jitter 60 FPS camera motion.
     */
    private fun dampAxis(current: Float, target: Float, vel: Float, smoothTime: Float, dt: Float): Pair<Float, Float> {
        val omega = 2.0f / smoothTime.coerceAtLeast(0.01f)
        val x = omega * dt
        val expFactor = exp(-x)
        val delta = current - target
        val temp = (vel + omega * delta) * dt
        val newVel = (vel - omega * temp) * expFactor
        val newVal = target + (delta + temp) * expFactor
        return newVal to newVel
    }

    private fun updateCamera(
        dt: Float, fwdX: Float, fwdZ: Float, rightX: Float, rightZ: Float, speedAbs: Float
    ) {
        val speedRatio = (speedAbs / 90f).coerceIn(0f, 1.3f)
        val boostFov = if (boostTimer > 0f) 8.5f else 0f
        val targetFov = when (cameraMode) {
            CameraMode.CHASE -> 58f + speedRatio * 18f + boostFov
            CameraMode.SIDE_ACTION -> 52f + speedRatio * 8f
            CameraMode.HOOD -> 68f + speedRatio * 14f + boostFov
            CameraMode.ORBIT -> 52f
        }
        cameraFov += (targetFov - cameraFov) * min(1f, dt * 6f)

        when (cameraMode) {
            CameraMode.CHASE -> {
                val dist = 7.4f + speedRatio * 1.5f
                val tx = carX - fwdX * dist
                val ty = carY + 2.55f + speedRatio * 0.35f
                val tz = carZ - fwdZ * dist
                val (nx, vxNew) = dampAxis(camPosX, tx, camVelX, 0.14f, dt)
                val (ny, vyNew) = dampAxis(camPosY, ty, camVelY, 0.16f, dt)
                val (nz, vzNew) = dampAxis(camPosZ, tz, camVelZ, 0.14f, dt)
                camPosX = nx; camPosY = ny; camPosZ = nz
                camVelX = vxNew; camVelY = vyNew; camVelZ = vzNew

                val (lx, lvx) = dampAxis(camLookX, carX + fwdX * 3.8f, lookVelX, 0.08f, dt)
                val (ly, lvy) = dampAxis(camLookY, carY + 1.05f, lookVelY, 0.08f, dt)
                val (lz, lvz) = dampAxis(camLookZ, carZ + fwdZ * 3.8f, lookVelZ, 0.08f, dt)
                camLookX = lx; camLookY = ly; camLookZ = lz
                lookVelX = lvx; lookVelY = lvy; lookVelZ = lvz
            }

            CameraMode.SIDE_ACTION -> {
                // Lateral side-tracking cam showcasing car profile, spinning alloy rims, brake calipers & drift angle
                val sideDist = 6.8f
                val tx = carX + rightX * sideDist + fwdX * 0.9f
                val ty = carY + 1.55f
                val tz = carZ + rightZ * sideDist + fwdZ * 0.9f
                val (nx, vxNew) = dampAxis(camPosX, tx, camVelX, 0.16f, dt)
                val (ny, vyNew) = dampAxis(camPosY, ty, camVelY, 0.18f, dt)
                val (nz, vzNew) = dampAxis(camPosZ, tz, camVelZ, 0.16f, dt)
                camPosX = nx; camPosY = ny; camPosZ = nz
                camVelX = vxNew; camVelY = vyNew; camVelZ = vzNew

                val (lx, lvx) = dampAxis(camLookX, carX + fwdX * 0.6f, lookVelX, 0.09f, dt)
                val (ly, lvy) = dampAxis(camLookY, carY + 0.72f, lookVelY, 0.09f, dt)
                val (lz, lvz) = dampAxis(camLookZ, carZ + fwdZ * 0.6f, lookVelZ, 0.09f, dt)
                camLookX = lx; camLookY = ly; camLookZ = lz
                lookVelX = lvx; lookVelY = lvy; lookVelZ = lvz
            }

            CameraMode.HOOD -> {
                camPosX = carX + fwdX * 0.55f
                camPosY = carY + 1.18f + suspensionY * 0.5f
                camPosZ = carZ + fwdZ * 0.55f
                camLookX = carX + fwdX * 18f
                camLookY = carY + 1.05f - chassisPitch * 6f
                camLookZ = carZ + fwdZ * 18f
            }

            CameraMode.ORBIT -> {
                orbitAngle += dt * 0.55f
                val radius = 7.2f
                camPosX = carX + sin(orbitAngle) * radius
                camPosY = carY + 2.4f
                camPosZ = carZ + cos(orbitAngle) * radius
                camLookX = carX
                camLookY = carY + 0.85f
                camLookZ = carZ
            }
        }
    }
}
