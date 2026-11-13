package com.example.game

import androidx.compose.ui.graphics.Color
import java.util.concurrent.atomic.AtomicReference

enum class CarType(
    val displayName: String,
    val accel: Float,
    val topSpeed: Float,
    val reverseSpeed: Float,
    val lateralGrip: Float,
    val driftGrip: Float,
    val turnRate: Float,
    val suspensionSoftness: Float,
    val hasBackfireFlames: Boolean,
    val accentR: Float,
    val accentG: Float,
    val accentB: Float,
    val caliperR: Float,
    val caliperG: Float,
    val caliperB: Float,
    val defaultPaintIndex: Int
) {
    EV_HYPER_GT(
        displayName = "EV Hyper-GT",
        accel = 58f,
        topSpeed = 90f,
        reverseSpeed = 28f,
        lateralGrip = 12.2f,
        driftGrip = 4.2f,
        turnRate = 2.75f,
        suspensionSoftness = 0.72f,
        hasBackfireFlames = false,
        accentR = 0.0f,
        accentG = 0.94f,
        accentB = 1.0f,
        caliperR = 0.0f,
        caliperG = 0.88f,
        caliperB = 1.0f,
        defaultPaintIndex = 3
    ),
    V12_APEX_ROADSTER(
        displayName = "V12 Apex",
        accel = 47f,
        topSpeed = 100f,
        reverseSpeed = 26f,
        lateralGrip = 8.6f,
        driftGrip = 2.05f,
        turnRate = 2.90f,
        suspensionSoftness = 0.88f,
        hasBackfireFlames = true,
        accentR = 1.0f,
        accentG = 0.34f,
        accentB = 0.16f,
        caliperR = 0.95f,
        caliperG = 0.12f,
        caliperB = 0.15f,
        defaultPaintIndex = 4
    ),
    VIP_EXECUTIVE_COUPE(
        displayName = "VIP Coupe",
        accel = 40f,
        topSpeed = 82f,
        reverseSpeed = 24f,
        lateralGrip = 9.8f,
        driftGrip = 3.1f,
        turnRate = 2.35f,
        suspensionSoftness = 1.38f,
        hasBackfireFlames = false,
        accentR = 0.98f,
        accentG = 0.76f,
        accentB = 0.22f,
        caliperR = 0.96f,
        caliperG = 0.74f,
        caliperB = 0.20f,
        defaultPaintIndex = 0
    )
}

enum class PaintFinish(
    val roughness: Float,
    val metalness: Float,
    val clearcoat: Float,
    val clearcoatRoughness: Float
) {
    GLOSSY(roughness = 0.15f, metalness = 0.85f, clearcoat = 1.0f, clearcoatRoughness = 0.06f),
    MATTE(roughness = 0.56f, metalness = 0.38f, clearcoat = 0.08f, clearcoatRoughness = 0.45f),
    CHROME(roughness = 0.05f, metalness = 0.98f, clearcoat = 1.0f, clearcoatRoughness = 0.03f)
}

data class PaintPreset(
    val id: String,
    val composeColor: Color,
    val r: Float,
    val g: Float,
    val b: Float
)

val LUXURY_PAINT_PRESETS = listOf(
    PaintPreset("obsidian", Color(0xFF141923), 0.08f, 0.10f, 0.14f),
    PaintPreset("gold", Color(0xFFF3B33D), 0.95f, 0.70f, 0.24f),
    PaintPreset("cyan", Color(0xFF00D2FF), 0.02f, 0.80f, 0.98f),
    PaintPreset("pearl", Color(0xFFEDF2F7), 0.92f, 0.94f, 0.97f),
    PaintPreset("crimson", Color(0xFFD91E36), 0.85f, 0.10f, 0.20f),
    PaintPreset("emerald", Color(0xFF0E9F6E), 0.05f, 0.62f, 0.43f),
    PaintPreset("sapphire", Color(0xFF2563EB), 0.14f, 0.38f, 0.92f)
)

enum class CameraMode {
    CHASE,
    SIDE_ACTION,
    HOOD,
    ORBIT
}

data class ControlState(
    val gas: Boolean = false,
    val brake: Boolean = false,
    val steerLeft: Boolean = false,
    val steerRight: Boolean = false,
    val handbrake: Boolean = false
)

enum class StuntBannerType {
    NONE,
    AIRTIME,
    HELIPAD_LANDING,
    TURBO_BOOST
}

data class HudTelemetry(
    val speedKmh: Int = 0,
    val activeDriftScore: Int = 0,
    val driftMultiplier: Int = 1,
    val totalScore: Int = 0,
    val isDrifting: Boolean = false,
    val isBoosting: Boolean = false,
    val isAirborne: Boolean = false,
    val isReversing: Boolean = false,
    val bannerType: StuntBannerType = StuntBannerType.NONE,
    val carType: CarType = CarType.EV_HYPER_GT,
    val paintIndex: Int = 3,
    val paintFinish: PaintFinish = PaintFinish.GLOSSY,
    val cameraMode: CameraMode = CameraMode.CHASE,
    val isMuted: Boolean = false
)

class SharedGameInput {
    val touchControls = AtomicReference(ControlState())
    val keyboardControls = AtomicReference(ControlState())

    fun combined(): ControlState {
        val t = touchControls.get()
        val k = keyboardControls.get()
        return ControlState(
            gas = t.gas || k.gas,
            brake = t.brake || k.brake,
            steerLeft = t.steerLeft || k.steerLeft,
            steerRight = t.steerRight || k.steerRight,
            handbrake = t.handbrake || k.handbrake
        )
    }
}
