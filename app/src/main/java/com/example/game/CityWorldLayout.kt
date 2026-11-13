package com.example.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class BuildingSpec(
    val cx: Float,
    val cz: Float,
    val halfW: Float,
    val halfD: Float,
    val height: Float,
    val scale: Float,
    val tintR: Float,
    val tintG: Float,
    val tintB: Float
)

data class BoostPadSpec(
    val cx: Float,
    val cz: Float,
    val halfW: Float = 6.5f,
    val halfD: Float = 5.5f
)

data class BoostRingSpec(
    val cx: Float,
    val cy: Float,
    val cz: Float,
    val radius: Float = 7.5f,
    val yawRad: Float = 0f
)

data class SpeedTrapSpec(
    val id: String,
    val cx: Float,
    val cz: Float,
    val halfW: Float = 18f,
    val halfD: Float = 8f,
    val yawRad: Float = 0f
)

data class DriftZoneSpec(
    val cx: Float,
    val cz: Float,
    val radius: Float
)

data class RampSpec(
    val minX: Float,
    val maxX: Float,
    val startZ: Float,
    val endZ: Float,
    val baseY: Float,
    val topY: Float
)

object CityLayout {
    const val WORLD_HALF_SIZE = 1920f
    const val EAST_COAST_X = 840f
    const val WATER_LEVEL = -1.8f
    const val ROUNDABOUT_MONUMENT_R = 15.5f

    const val MARINA_CANAL_MIN_X = 520f
    const val MARINA_CANAL_MAX_X = 845f
    const val MARINA_CANAL_MIN_Z = 40f
    const val MARINA_CANAL_MAX_Z = 84f

    const val HELIPAD_CX = -820f
    const val HELIPAD_CZ = 920f
    const val HELIPAD_HALF_SIZE = 22f
    const val HELIPAD_HEIGHT = 10.5f

    // Illuminated Coastal Showroom Podium overlooking Downtown skyline & Marina bay
    const val SHOWROOM_X = 710f
    const val SHOWROOM_Y = 1.65f
    const val SHOWROOM_Z = -180f
    const val SHOWROOM_YAW = -2.25f

    const val CANYON_FEEDER_X = -760f
    const val CANYON_START_Z = -280f
    const val CANYON_END_Z = -1680f
    const val CANYON_ROAD_HALF_W = 19f
    const val CANYON_SUMMIT_PLATEAU_R = 64f

    fun spawnPoint(location: SpawnLocation): FloatArray = when (location) {
        SpawnLocation.DOWNTOWN -> floatArrayOf(0f, 0.25f, -290f, 0f)
        SpawnLocation.MARINA -> floatArrayOf(580f, 0.25f, -560f, 0f)
        SpawnLocation.CANYON -> floatArrayOf(CANYON_FEEDER_X, 0.25f, -210f, PI.toFloat())
        SpawnLocation.AIRPORT -> floatArrayOf(-1180f, 0.25f, 470f, 0f)
    }

    val ramps = listOf(
        // Marina canal twin launch ramps (North->South and South->North)
        RampSpec(562f, 578f, 20f, 40f, 0f, 6.2f),
        RampSpec(562f, 578f, 104f, 84f, 0f, 6.2f),
        RampSpec(582f, 598f, 20f, 40f, 0f, 6.2f),
        RampSpec(582f, 598f, 104f, 84f, 0f, 6.2f),
        // Airport Runway mega hydraulic launch ramps
        RampSpec(-1204f, -1156f, 940f, 1010f, 0f, 12.0f),
        RampSpec(-1204f, -1156f, 1340f, 1270f, 0f, 12.0f),
        // Stunt park medium ramps
        RampSpec(-990f, -954f, 620f, 682f, 0f, 9.8f),
        RampSpec(-990f, -954f, 1180f, 1118f, 0f, 9.8f),
        // Rooftop Helipad wide approach ramp
        RampSpec(HELIPAD_CX - 12f, HELIPAD_CX + 12f, 838f, HELIPAD_CZ - HELIPAD_HALF_SIZE, 0f, HELIPAD_HEIGHT)
    )

    val boostPads = listOf(
        // Marina acceleration tunnel pads
        BoostPadSpec(580f, -490f),
        BoostPadSpec(580f, -385f),
        BoostPadSpec(580f, -280f),
        // Airport drag runway start & mid pads
        BoostPadSpec(-1180f, 560f),
        BoostPadSpec(-1180f, 820f),
        BoostPadSpec(-1180f, 1460f),
        // Downtown East-West & North-South expressway pads
        BoostPadSpec(0f, -240f),
        BoostPadSpec(0f, 240f),
        BoostPadSpec(-240f, 0f, halfW = 5.5f, halfD = 6.5f),
        BoostPadSpec(240f, 0f, halfW = 5.5f, halfD = 6.5f)
    )

    val boostRings = listOf(
        BoostRingSpec(570f, 11.5f, 62f, radius = 8.5f, yawRad = 0f),
        BoostRingSpec(590f, 11.5f, 62f, radius = 8.5f, yawRad = 0f),
        BoostRingSpec(-1180f, 18.5f, 1045f, radius = 9.5f, yawRad = 0f),
        BoostRingSpec(-1180f, 18.5f, 1235f, radius = 9.5f, yawRad = 0f),
        BoostRingSpec(-972f, 15.5f, 718f, radius = 8.5f, yawRad = 0f),
        BoostRingSpec(0f, 4.5f, -410f, radius = 8.0f, yawRad = 0f)
    )

    val speedTraps = listOf(
        SpeedTrapSpec("downtown_n", 0f, -310f, halfW = 20f, halfD = 8f, yawRad = 0f),
        SpeedTrapSpec("downtown_e", 310f, 0f, halfW = 8f, halfD = 20f, yawRad = (PI * 0.5).toFloat()),
        SpeedTrapSpec("marina_tunnel", 580f, -210f, halfW = 22f, halfD = 8f, yawRad = 0f),
        SpeedTrapSpec("marina_south", 580f, 480f, halfW = 22f, halfD = 8f, yawRad = 0f),
        SpeedTrapSpec("airport_mid", -1180f, 900f, halfW = 36f, halfD = 10f, yawRad = 0f),
        SpeedTrapSpec("airport_end", -1180f, 1540f, halfW = 36f, halfD = 10f, yawRad = 0f),
        SpeedTrapSpec("canyon_entry", CANYON_FEEDER_X, -220f, halfW = 22f, halfD = 8f, yawRad = 0f),
        SpeedTrapSpec("canyon_crest", canyonRoadX(-1180f), -1180f, halfW = 28f, halfD = 14f, yawRad = 0.4f)
    )

    val driftZones = listOf(
        DriftZoneSpec(0f, 0f, 62f),              // Downtown Grand Roundabout
        DriftZoneSpec(-880f, -980f, 340f),      // Canyon Mountain Touge Hairpins
        DriftZoneSpec(-900f, 1340f, 110f),      // Airport Drift Skidpad
        DriftZoneSpec(580f, 760f, 105f)         // Marina Waterfront Drift Plaza
    )

    // Instanced skyscraper towers (unit tower base 24x80x24 scaled by spec.scale)
    val buildings: List<BuildingSpec> = buildList {
        val slots = listOf(
            // Inner Downtown Core ring
            floatArrayOf(-88f, -88f, 1.15f, 0f),
            floatArrayOf(-150f, -88f, 1.35f, 1f),
            floatArrayOf(-88f, -150f, 1.25f, 2f),
            floatArrayOf(-150f, -150f, 1.60f, 0f),
            floatArrayOf(88f, -88f, 1.20f, 1f),
            floatArrayOf(150f, -88f, 1.45f, 0f),
            floatArrayOf(88f, -150f, 1.10f, 2f),
            floatArrayOf(150f, -150f, 1.55f, 1f),
            floatArrayOf(-88f, 88f, 1.05f, 2f),
            floatArrayOf(-150f, 88f, 1.30f, 0f),
            floatArrayOf(-88f, 150f, 1.40f, 1f),
            floatArrayOf(-150f, 150f, 1.50f, 2f),
            floatArrayOf(88f, 88f, 1.25f, 0f),
            floatArrayOf(150f, 88f, 1.48f, 1f),
            floatArrayOf(88f, 150f, 1.18f, 2f),
            floatArrayOf(150f, 150f, 1.65f, 0f),
            // Mid Downtown Skyline
            floatArrayOf(-245f, -95f, 1.42f, 1f),
            floatArrayOf(-245f, -245f, 1.78f, 0f),
            floatArrayOf(-95f, -245f, 1.38f, 2f),
            floatArrayOf(95f, -245f, 1.52f, 0f),
            floatArrayOf(245f, -245f, 1.85f, 1f),
            floatArrayOf(245f, -95f, 1.32f, 2f),
            floatArrayOf(-245f, 95f, 1.28f, 0f),
            floatArrayOf(-245f, 245f, 1.62f, 1f),
            floatArrayOf(-95f, 245f, 1.35f, 2f),
            floatArrayOf(95f, 245f, 1.48f, 0f),
            floatArrayOf(245f, 245f, 1.72f, 1f),
            floatArrayOf(245f, 95f, 1.26f, 2f),
            // Outer Metropolis Skyline & Marina Resort Towers
            floatArrayOf(-430f, -120f, 1.30f, 0f),
            floatArrayOf(-430f, 120f, 1.22f, 1f),
            floatArrayOf(430f, -140f, 1.40f, 2f),
            floatArrayOf(430f, 140f, 1.35f, 0f),
            floatArrayOf(655f, -680f, 1.12f, 1f),
            floatArrayOf(655f, -380f, 1.25f, 0f),
            floatArrayOf(655f, 320f, 1.18f, 2f),
            floatArrayOf(655f, 640f, 1.08f, 1f)
        )
        for (s in slots) {
            val scale = s[2]
            val accentMode = s[3].toInt()
            val (r, g, b) = when (accentMode) {
                0 -> Triple(0.0f, 0.94f, 1.0f)    // Cyan neon crown
                1 -> Triple(1.0f, 0.76f, 0.22f)   // Sunset gold crown
                else -> Triple(1.0f, 0.20f, 0.65f) // Magenta neon crown
            }
            add(
                BuildingSpec(
                    cx = s[0],
                    cz = s[1],
                    halfW = 12f * scale,
                    halfD = 12f * scale,
                    height = 76f * scale,
                    scale = scale,
                    tintR = r,
                    tintG = g,
                    tintB = b
                )
            )
        }
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun canyonRoadX(z: Float): Float {
        val t = ((CANYON_START_Z - z) / (CANYON_START_Z - CANYON_END_Z)).coerceIn(0f, 1f)
        return CANYON_FEEDER_X - 240f * t + sin(t * 3.0f * PI.toFloat()) * 250f
    }

    fun canyonRoadSlopeX(z: Float): Float {
        val span = CANYON_START_Z - CANYON_END_Z
        val t = ((CANYON_START_Z - z) / span).coerceIn(0f, 1f)
        val dtDz = -1f / span
        val dxDt = -240f + cos(t * 3.0f * PI.toFloat()) * 250f * 3.0f * PI.toFloat()
        return dxDt * dtDz
    }

    fun canyonRoadY(z: Float): Float {
        val t = ((CANYON_START_Z - z) / (CANYON_START_Z - CANYON_END_Z)).coerceIn(0f, 1f)
        return 44f * smoothstep(0.03f, 0.72f, t) - 10f * smoothstep(0.80f, 1.0f, t)
    }

    fun getDistrict(x: Float, z: Float): DistrictZone {
        return when {
            x >= 460f -> DistrictZone.MARINA
            x <= -460f && z <= -180f -> DistrictZone.CANYON
            x <= -460f && z >= 180f -> DistrictZone.AIRPORT
            else -> DistrictZone.DOWNTOWN
        }
    }

    fun queryGroundHeight(x: Float, y: Float, z: Float): Float {
        // 1. Check Canyon Mountain Touge elevated road & summit plateau
        if (x < -420f && z in (CANYON_END_Z - 75f)..CANYON_START_Z) {
            val summitX = canyonRoadX(CANYON_END_Z)
            val summitY = canyonRoadY(CANYON_END_Z)
            if (hypot(x - summitX, z - CANYON_END_Z) <= CANYON_SUMMIT_PLATEAU_R) {
                if (y >= summitY - 4f) return summitY
            }
            if (z in CANYON_END_Z..CANYON_START_Z) {
                val roadX = canyonRoadX(z)
                val roadY = canyonRoadY(z)
                val dxdz = canyonRoadSlopeX(z)
                val halfBandX = CANYON_ROAD_HALF_W * sqrt(1f + dxdz * dxdz)
                if (abs(x - roadX) <= halfBandX && y >= roadY - 4.5f) {
                    return roadY
                }
            }
        }

        // 2. Rooftop Helipad platform
        if (x in (HELIPAD_CX - HELIPAD_HALF_SIZE)..(HELIPAD_CX + HELIPAD_HALF_SIZE) &&
            z in (HELIPAD_CZ - HELIPAD_HALF_SIZE)..(HELIPAD_CZ + HELIPAD_HALF_SIZE)
        ) {
            if (y >= HELIPAD_HEIGHT - 2.0f) return HELIPAD_HEIGHT
        }

        // 3. All hydraulic & stunt ramps
        for (ramp in ramps) {
            if (x in ramp.minX..ramp.maxX) {
                val minZ = minOf(ramp.startZ, ramp.endZ)
                val maxZ = maxOf(ramp.startZ, ramp.endZ)
                if (z in minZ..maxZ) {
                    val t = ((z - ramp.startZ) / (ramp.endZ - ramp.startZ)).coerceIn(0f, 1f)
                    val rampY = ramp.baseY + t * (ramp.topY - ramp.baseY)
                    if (y >= rampY - 2.5f) return rampY
                }
            }
        }

        // 4. Marina Canal water cut-out
        if (x in MARINA_CANAL_MIN_X..MARINA_CANAL_MAX_X && z in MARINA_CANAL_MIN_Z..MARINA_CANAL_MAX_Z) {
            return WATER_LEVEL
        }

        // 5. Ocean boundaries (beyond island west/north/south or beyond East Coast Marina bay)
        if (x < -WORLD_HALF_SIZE || x > EAST_COAST_X || abs(z) > WORLD_HALF_SIZE) {
            return WATER_LEVEL
        }

        return 0f
    }
}
