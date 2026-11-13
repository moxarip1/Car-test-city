package com.example.game

import androidx.compose.ui.graphics.Color
import java.util.concurrent.atomic.AtomicReference

enum class GameState {
    SPLASH,
    MENU,
    PLAYING,
    PAUSED
}

enum class SpawnLocation {
    DOWNTOWN,
    MARINA,
    CANYON,
    AIRPORT
}

enum class GraphicsPreset(
    val shadowsEnabled: Boolean,
    val shadowRes: Int,
    val drawDistance: Float,
    val fogDensity: Float,
    val particlesEnabled: Boolean,
    val headlightsAllowed: Boolean,
    val reflectionsEnabled: Boolean
) {
    LOW(
        shadowsEnabled = false,
        shadowRes = 512,
        drawDistance = 1200f,
        fogDensity = 0.00145f,
        particlesEnabled = false,
        headlightsAllowed = false,
        reflectionsEnabled = false
    ),
    MEDIUM(
        shadowsEnabled = true,
        shadowRes = 512,
        drawDistance = 2200f,
        fogDensity = 0.00095f,
        particlesEnabled = true,
        headlightsAllowed = false,
        reflectionsEnabled = true
    ),
    HIGH(
        shadowsEnabled = true,
        shadowRes = 1024,
        drawDistance = 3500f,
        fogDensity = 0.00068f,
        particlesEnabled = true,
        headlightsAllowed = true,
        reflectionsEnabled = true
    ),
    ULTRA(
        shadowsEnabled = true,
        shadowRes = 2048,
        drawDistance = 4500f,
        fogDensity = 0.00048f,
        particlesEnabled = true,
        headlightsAllowed = true,
        reflectionsEnabled = true
    )
}

enum class ResolutionPreset(val scale: Float, val useNativeBuffer: Boolean) {
    LOW_50(0.50f, false),
    BALANCED_75(0.75f, false),
    HIGH_100(0.90f, false),
    NATIVE(1.0f, true)
}

enum class SteeringMode {
    BUTTONS,
    VIRTUAL_WHEEL
}

enum class SpeedUnit {
    KMH,
    MPH
}

data class GameSettings(
    val graphicsPreset: GraphicsPreset = GraphicsPreset.HIGH,
    val resolutionPreset: ResolutionPreset = ResolutionPreset.HIGH_100,
    val showFps: Boolean = false,
    val masterVolume: Float = 0.85f,
    val engineVolume: Float = 0.90f,
    val sfxVolume: Float = 0.85f,
    val isMuted: Boolean = false,
    val steeringMode: SteeringMode = SteeringMode.BUTTONS,
    val steeringSensitivity: Float = 1.0f,
    val smartDriftAssist: Boolean = true,
    val speedUnit: SpeedUnit = SpeedUnit.KMH
)

data class SavedRecords(
    val bestDriftScore: Int = 0,
    val bestTotalScore: Int = 0,
    val topSpeedKmh: Int = 0
)

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
    val defaultPaintIndex: Int,
    val statSpeed: Float,
    val statAccel: Float,
    val statDrift: Float
) {
    EV_HYPER_GT(
        displayName = "EV Hyper-GT",
        accel = 64f,
        topSpeed = 98f,
        reverseSpeed = 30f,
        lateralGrip = 12.6f,
        driftGrip = 4.1f,
        turnRate = 2.75f,
        suspensionSoftness = 0.72f,
        hasBackfireFlames = false,
        accentR = 0.0f,
        accentG = 0.94f,
        accentB = 1.0f,
        caliperR = 0.0f,
        caliperG = 0.88f,
        caliperB = 1.0f,
        defaultPaintIndex = 3,
        statSpeed = 0.88f,
        statAccel = 0.98f,
        statDrift = 0.84f
    ),
    V12_APEX_ROADSTER(
        displayName = "V12 Apex",
        accel = 52f,
        topSpeed = 108f,
        reverseSpeed = 28f,
        lateralGrip = 8.9f,
        driftGrip = 2.1f,
        turnRate = 2.90f,
        suspensionSoftness = 0.88f,
        hasBackfireFlames = true,
        accentR = 1.0f,
        accentG = 0.34f,
        accentB = 0.16f,
        caliperR = 0.95f,
        caliperG = 0.12f,
        caliperB = 0.15f,
        defaultPaintIndex = 4,
        statSpeed = 0.98f,
        statAccel = 0.86f,
        statDrift = 0.96f
    ),
    VIP_EXECUTIVE_COUPE(
        displayName = "VIP Coupe",
        accel = 44f,
        topSpeed = 90f,
        reverseSpeed = 26f,
        lateralGrip = 10.2f,
        driftGrip = 3.0f,
        turnRate = 2.40f,
        suspensionSoftness = 1.35f,
        hasBackfireFlames = false,
        accentR = 0.98f,
        accentG = 0.76f,
        accentB = 0.22f,
        caliperR = 0.96f,
        caliperG = 0.74f,
        caliperB = 0.20f,
        defaultPaintIndex = 0,
        statSpeed = 0.80f,
        statAccel = 0.76f,
        statDrift = 0.78f
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

enum class TimeOfDay {
    SUNSET,
    DAY,
    NIGHT
}

enum class DistrictZone {
    DOWNTOWN,
    MARINA,
    CANYON,
    AIRPORT
}

data class ControlState(
    val gas: Boolean = false,
    val brake: Boolean = false,
    val steerLeft: Boolean = false,
    val steerRight: Boolean = false,
    val analogSteer: Float = 0f,
    val handbrake: Boolean = false,
    val nitro: Boolean = false
)

enum class StuntBannerType {
    NONE,
    AIRTIME,
    HELIPAD_LANDING,
    TURBO_BOOST,
    BOOST_RING,
    SPEED_TRAP,
    DRIFT_ZONE
}

data class HudTelemetry(
    val gameState: GameState = GameState.SPLASH,
    val speedKmh: Int = 0,
    val gear: Int = 1,
    val rpmNormalized: Float = 0.15f,
    val nitroCharge: Float = 100f,
    val isNitroActive: Boolean = false,
    val activeDriftScore: Int = 0,
    val driftMultiplier: Int = 1,
    val totalScore: Int = 0,
    val bestTrapSpeedKmh: Int = 0,
    val lastTrapSpeedKmh: Int = 0,
    val bestDriftScore: Int = 0,
    val topSpeedKmh: Int = 0,
    val isNewRecord: Boolean = false,
    val inDriftZone: Boolean = false,
    val isDrifting: Boolean = false,
    val isBoosting: Boolean = false,
    val isAirborne: Boolean = false,
    val isReversing: Boolean = false,
    val bannerType: StuntBannerType = StuntBannerType.NONE,
    val carType: CarType = CarType.EV_HYPER_GT,
    val paintIndex: Int = 3,
    val paintFinish: PaintFinish = PaintFinish.GLOSSY,
    val cameraMode: CameraMode = CameraMode.CHASE,
    val timeOfDay: TimeOfDay = TimeOfDay.SUNSET,
    val district: DistrictZone = DistrictZone.DOWNTOWN,
    val carX: Float = 0f,
    val carZ: Float = 0f,
    val carYaw: Float = 0f,
    val fps: Int = 60,
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
            analogSteer = t.analogSteer,
            handbrake = t.handbrake || k.handbrake,
            nitro = t.nitro || k.nitro
        )
    }
}
