package com.example.game.gl

import com.example.game.CityLayout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class WorldMeshes(
    val staticWorldMesh: GpuMesh,
    val buildingInstanced: InstancedGpuMesh,
    val palmInstanced: InstancedGpuMesh,
    val lampInstanced: InstancedGpuMesh,
    val barrierInstanced: InstancedGpuMesh,
    val yachtInstanced: InstancedGpuMesh,
    val canyonRockInstanced: InstancedGpuMesh,
    val billboardInstanced: InstancedGpuMesh,
    val boostRingInstanced: InstancedGpuMesh,
    val speedTrapInstanced: InstancedGpuMesh,
    val oceanMesh: GpuMesh,
    val skyDomeMesh: GpuMesh
) {
    fun release() {
        staticWorldMesh.release()
        buildingInstanced.release()
        palmInstanced.release()
        lampInstanced.release()
        barrierInstanced.release()
        yachtInstanced.release()
        canyonRockInstanced.release()
        billboardInstanced.release()
        boostRingInstanced.release()
        speedTrapInstanced.release()
        oceanMesh.release()
        skyDomeMesh.release()
    }
}

object WorldMeshLibrary {

    fun buildWorld(): WorldMeshes {
        return WorldMeshes(
            staticWorldMesh = buildCityMesh(),
            buildingInstanced = buildInstancedSkyscrapers(),
            palmInstanced = buildInstancedPalms(),
            lampInstanced = buildInstancedStreetlamps(),
            barrierInstanced = buildInstancedBarriers(),
            yachtInstanced = buildInstancedYachts(),
            canyonRockInstanced = buildInstancedCanyonRocks(),
            billboardInstanced = buildInstancedBillboards(),
            boostRingInstanced = buildInstancedBoostRings(),
            speedTrapInstanced = buildInstancedSpeedTraps(),
            oceanMesh = buildOceanMesh(),
            skyDomeMesh = buildSkyDomeMesh()
        )
    }

    private fun buildCityMesh(): GpuMesh {
        val b = MeshBuilder(196608)

        val ws = CityLayout.WORLD_HALF_SIZE
        val ec = CityLayout.EAST_COAST_X
        val cmx = CityLayout.MARINA_CANAL_MIN_X

        // 1. 4000x4000 Coastal Peninsula Terrain Slabs (carved cleanly around Marina Canal)
        b.addBox((-ws + cmx) * 0.5f, -0.9f, 0f, ws + cmx, 1.8f, ws * 2f, 0.19f, 0.22f, 0.27f, 0f)
        b.addBox((cmx + ec) * 0.5f, -0.9f, (-ws + CityLayout.MARINA_CANAL_MIN_Z) * 0.5f, ec - cmx, 1.8f, ws + CityLayout.MARINA_CANAL_MIN_Z, 0.19f, 0.22f, 0.27f, 0f)
        b.addBox((cmx + ec) * 0.5f, -0.9f, (CityLayout.MARINA_CANAL_MAX_Z + ws) * 0.5f, ec - cmx, 1.8f, ws - CityLayout.MARINA_CANAL_MAX_Z, 0.19f, 0.22f, 0.27f, 0f)

        // Sandy Coastal Beach Rim & Boardwalk
        b.addBox(0f - (ws - ec) * 0.5f, -0.4f, -ws - 3f, ws + ec + 12f, 1.0f, 6f, 0.85f, 0.72f, 0.50f, 0f)
        b.addBox(0f - (ws - ec) * 0.5f, -0.4f, ws + 3f, ws + ec + 12f, 1.0f, 6f, 0.85f, 0.72f, 0.50f, 0f)
        b.addBox(-ws - 3f, -0.4f, 0f, 6f, 1.0f, ws * 2f, 0.85f, 0.72f, 0.50f, 0f)
        b.addBox(ec + 3f, -0.4f, 0f, 6f, 1.0f, ws * 2f, 0.85f, 0.72f, 0.50f, 0f)

        // 2. Multi-Lane Expressways & District Highways (matType = 1 samples procedural asphalt)
        // North-South Main Expressway (32m wide, 3.7km long)
        b.addQuad(-16f, 0.02f, -1880f, 0f, 0f, 16f, 0.02f, -1880f, 1f, 0f, 16f, 0.02f, 1880f, 1f, 120f, -16f, 0.02f, 1880f, 0f, 120f, 1f, 1f, 1f, 1f)
        // East-West Main Expressway (Downtown -> Marina Boardwalk & Airport/Canyon hubs)
        b.addQuad(-1880f, 0.025f, -16f, 0f, 0f, 800f, 0.025f, -16f, 0f, 90f, 800f, 0.025f, 16f, 1f, 90f, -1880f, 0.025f, 16f, 1f, 0f, 1f, 1f, 1f, 1f)

        // Downtown Outer Ring Boulevards (±360m)
        b.addQuad(-374f, 0.022f, -374f, 0f, 0f, 374f, 0.022f, -374f, 0f, 26f, 374f, 0.022f, -346f, 1f, 26f, -374f, 0.022f, -346f, 1f, 0f, 1f, 1f, 1f, 1f)
        b.addQuad(-374f, 0.022f, 346f, 0f, 0f, 374f, 0.022f, 346f, 0f, 26f, 374f, 0.022f, 374f, 1f, 26f, -374f, 0.022f, 374f, 1f, 0f, 1f, 1f, 1f, 1f)
        b.addQuad(-374f, 0.022f, -346f, 0f, 0f, -346f, 0.022f, -346f, 1f, 0f, -346f, 0.022f, 346f, 1f, 24f, -374f, 0.022f, 346f, 0f, 24f, 1f, 1f, 1f, 1f)
        b.addQuad(346f, 0.022f, -346f, 0f, 0f, 374f, 0.022f, -346f, 1f, 0f, 374f, 0.022f, 346f, 1f, 24f, 346f, 0.022f, 346f, 0f, 24f, 1f, 1f, 1f, 1f)

        // Coastal Marina Straightaway Highway (40m wide, 3.3km long)
        b.addQuad(560f, 0.028f, -1650f, 0f, 0f, 600f, 0.028f, -1650f, 1f, 0f, 600f, 0.028f, 40f, 1f, 65f, 560f, 0.028f, 40f, 0f, 65f, 1f, 1f, 1f, 1f)
        b.addQuad(560f, 0.028f, 84f, 0f, 0f, 600f, 0.028f, 84f, 1f, 0f, 600f, 0.028f, 1650f, 1f, 60f, 560f, 0.028f, 1650f, 0f, 60f, 1f, 1f, 1f, 1f)

        // Airport Feeder, Massive Drag Runway (280m wide x 1260m long), & Stunt Park Apron
        b.addQuad(-1196f, 0.026f, 0f, 0f, 0f, -1164f, 0.026f, 0f, 1f, 0f, -1164f, 0.026f, 420f, 1f, 16f, -1196f, 0.026f, 420f, 0f, 16f, 1f, 1f, 1f, 1f)
        b.addQuad(-1320f, 0.03f, 420f, 0f, 0f, -1040f, 0.03f, 420f, 1f, 0f, -1040f, 0.03f, 1680f, 1f, 42f, -1320f, 0.03f, 1680f, 0f, 42f, 1f, 1f, 1f, 1f)
        b.addQuad(-1040f, 0.024f, 540f, 0f, 0f, -760f, 0.024f, 540f, 1f, 0f, -760f, 0.024f, 1520f, 1f, 30f, -1040f, 0.024f, 1520f, 0f, 30f, 0.9f, 0.9f, 0.9f, 1f)
        // Runway threshold piano keys & bright cyan drag centerline
        for (kx in -1290..-1070 step 28) {
            b.addBox(kx.toFloat(), 0.045f, 445f, 10f, 0.02f, 36f, 0.95f, 0.96f, 0.98f, 0f)
            b.addBox(kx.toFloat(), 0.045f, 1655f, 10f, 0.02f, 36f, 0.95f, 0.96f, 0.98f, 0f)
        }
        // Airport Drift Skidpad Glowing Ring at (-900, 1340)
        addGlowCircle(b, -900f, 0.05f, 1340f, 75f, 78f, 32, 1f, 0.72f, 0.16f)

        // Canyon Feeder Road (x = -760 from z=0 to z=-280)
        b.addQuad(
            CityLayout.CANYON_FEEDER_X - 18f, 0.028f, -280f, 0f, 0f,
            CityLayout.CANYON_FEEDER_X + 18f, 0.028f, -280f, 1f, 0f,
            CityLayout.CANYON_FEEDER_X + 18f, 0.028f, 0f, 1f, 12f,
            CityLayout.CANYON_FEEDER_X - 18f, 0.028f, 0f, 0f, 12f,
            1f, 1f, 1f, 1f
        )

        // 3. Elevated Canyon Mountain Touge Winding Road + Guardrails + Viaduct Pillars
        val zStart = CityLayout.CANYON_START_Z
        val zEnd = CityLayout.CANYON_END_Z
        val segs = 70
        val dz = (zEnd - zStart) / segs
        for (i in 0 until segs) {
            val z0 = zStart + i * dz
            val z1 = zStart + (i + 1) * dz
            val x0 = CityLayout.canyonRoadX(z0)
            val y0 = CityLayout.canyonRoadY(z0)
            val s0 = CityLayout.canyonRoadSlopeX(z0)
            val hw0 = CityLayout.CANYON_ROAD_HALF_W * sqrt(1f + s0 * s0)

            val x1 = CityLayout.canyonRoadX(z1)
            val y1 = CityLayout.canyonRoadY(z1)
            val s1 = CityLayout.canyonRoadSlopeX(z1)
            val hw1 = CityLayout.CANYON_ROAD_HALF_W * sqrt(1f + s1 * s1)

            // Main asphalt deck (note z1 < z0, order vertices CCW facing +Y)
            b.addQuad(
                x0 - hw0, y0 + 0.04f, z0, 0f, i * 0.8f,
                x1 - hw1, y1 + 0.04f, z1, 0f, (i + 1) * 0.8f,
                x1 + hw1, y1 + 0.04f, z1, 1f, (i + 1) * 0.8f,
                x0 + hw0, y0 + 0.04f, z0, 1f, i * 0.8f,
                1f, 1f, 1f, 1f
            )
            // Under-deck stone slab & viaduct support piers
            b.addQuad(
                x0 + hw0, y0 - 1.2f, z0, 0f, 0f,
                x1 + hw1, y1 - 1.2f, z1, 1f, 0f,
                x1 + hw1, y1 + 0.04f, z1, 1f, 1f,
                x0 + hw0, y0 + 0.04f, z0, 0f, 1f,
                0.42f, 0.34f, 0.28f, 0f
            )
            b.addQuad(
                x0 - hw0, y0 + 0.04f, z0, 0f, 1f,
                x1 - hw1, y1 + 0.04f, z1, 1f, 1f,
                x1 - hw1, y1 - 1.2f, z1, 1f, 0f,
                x0 - hw0, y0 - 1.2f, z0, 0f, 0f,
                0.42f, 0.34f, 0.28f, 0f
            )
            // Left & right red/white racing guardrails along hairpins (matType = 10)
            b.addQuad(
                x0 - hw0, y0 + 0.04f, z0, 0f, 0f,
                x1 - hw1, y1 + 0.04f, z1, 2f, 0f,
                x1 - hw1, y1 + 1.05f, z1, 2f, 1f,
                x0 - hw0, y0 + 1.05f, z0, 0f, 1f,
                1f, 1f, 1f, 10f
            )
            b.addQuad(
                x0 + hw0, y0 + 1.05f, z0, 0f, 1f,
                x1 + hw1, y1 + 1.05f, z1, 2f, 1f,
                x1 + hw1, y1 + 0.04f, z1, 2f, 0f,
                x0 + hw0, y0 + 0.04f, z0, 0f, 0f,
                1f, 1f, 1f, 10f
            )
            if (i % 4 == 0 && y0 > 2.0f) {
                b.addBox(x0 - hw0 * 0.65f, y0 * 0.5f, z0, 3.5f, y0, 3.5f, 0.36f, 0.30f, 0.26f, 0f)
                b.addBox(x0 + hw0 * 0.65f, y0 * 0.5f, z0, 3.5f, y0, 3.5f, 0.36f, 0.30f, 0.26f, 0f)
            }
        }
        // Canyon Summit Overlook Plateau
        val sumX = CityLayout.canyonRoadX(zEnd)
        val sumY = CityLayout.canyonRoadY(zEnd)
        b.addCylinderY(sumX, 0f, zEnd, CityLayout.CANYON_SUMMIT_PLATEAU_R, CityLayout.CANYON_SUMMIT_PLATEAU_R, sumY, 24, 0.25f, 0.26f, 0.30f, 0f)
        addGlowCircle(b, sumX, sumY + 0.08f, zEnd, 42f, 44f, 28, 0.0f, 0.94f, 1.0f)

        // 4. Grand Central Drift Roundabout + Racing Curbs + Monument
        val ringSegs = 48
        val rInner = 16.5f
        val rOuter = 48f
        for (i in 0 until ringSegs) {
            val a0 = (i.toFloat() / ringSegs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / ringSegs) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            b.addQuad(
                c0 * rInner, 0.032f, s0 * rInner, 0f, i * 0.5f,
                c0 * rOuter, 0.032f, s0 * rOuter, 1f, i * 0.5f,
                c1 * rOuter, 0.032f, s1 * rOuter, 1f, (i + 1) * 0.5f,
                c1 * rInner, 0.032f, s1 * rInner, 0f, (i + 1) * 0.5f,
                1f, 1f, 1f, 1f
            )
            b.addQuad(
                c0 * 15.2f, 0.16f, s0 * 15.2f, 0f, i * 0.5f,
                c0 * 16.6f, 0.06f, s0 * 16.6f, 1f, i * 0.5f,
                c1 * 16.6f, 0.06f, s1 * 16.6f, 1f, (i + 1) * 0.5f,
                c1 * 15.2f, 0.16f, s1 * 15.2f, 0f, (i + 1) * 0.5f,
                1f, 1f, 1f, 10f
            )
        }
        b.addCylinderY(0f, 0f, 0f, 15.0f, 13.8f, 1.6f, 24, 0.88f, 0.84f, 0.76f, 0f)
        b.addCylinderY(0f, 1.6f, 0f, 6.2f, 4.4f, 3.6f, 16, 0.16f, 0.19f, 0.26f, 3f)
        b.addCylinderY(0f, 5.2f, 0f, 2.5f, 0.35f, 19.0f, 12, 0.96f, 0.74f, 0.26f, 3f)
        b.addBox(0f, 24.4f, 0f, 1.4f, 1.4f, 1.4f, 0f, 0.95f, 1f, 4f)

        // 5. Marina Glowing Neon Acceleration Tunnel (x = 580, z = -520..-240)
        for (zPos in -520..-240 step 20) {
            val zf = zPos.toFloat()
            b.addBox(564f, 5.0f, zf, 1.4f, 10.0f, 1.8f, 0.14f, 0.16f, 0.22f, 0f)
            b.addBox(596f, 5.0f, zf, 1.4f, 10.0f, 1.8f, 0.14f, 0.16f, 0.22f, 0f)
            b.addBox(580f, 10.0f, zf, 33.4f, 1.4f, 1.8f, 0.14f, 0.16f, 0.22f, 0f)
            val isCyan = (zPos / 20) % 2 == 0
            val nr = if (isCyan) 0.0f else 1.0f
            val ng = if (isCyan) 0.95f else 0.74f
            val nb = if (isCyan) 1.0f else 0.22f
            b.addBox(565f, 5.0f, zf, 0.4f, 9.0f, 0.5f, nr, ng, nb, 4f)
            b.addBox(595f, 5.0f, zf, 0.4f, 9.0f, 0.5f, nr, ng, nb, 4f)
            b.addBox(580f, 9.2f, zf, 29.6f, 0.4f, 0.5f, nr, ng, nb, 4f)
        }

        // 6. All Boost Pads
        for (pad in CityLayout.boostPads) {
            b.addBox(pad.cx, 0.06f, pad.cz, pad.halfW * 1.8f, 0.08f, pad.halfD * 1.8f, 0.05f, 0.12f, 0.20f, 0f)
            b.addBox(pad.cx, 0.11f, pad.cz - 1.8f, pad.halfW * 1.5f, 0.04f, 1.1f, 0.0f, 0.95f, 1.0f, 4f)
            b.addBox(pad.cx, 0.11f, pad.cz, pad.halfW * 1.5f, 0.04f, 1.1f, 1.0f, 0.76f, 0.22f, 4f)
            b.addBox(pad.cx, 0.11f, pad.cz + 1.8f, pad.halfW * 1.5f, 0.04f, 1.1f, 0.0f, 0.95f, 1.0f, 4f)
        }

        // 7. All Hydraulic & Stunt Ramps
        CityLayout.ramps.forEachIndexed { idx, r ->
            val cyanAccent = idx % 2 == 0
            b.addRamp(
                r.minX, r.maxX, r.startZ, r.endZ, r.baseY, r.topY,
                0.20f, 0.23f, 0.30f,
                if (cyanAccent) 0f else 1f,
                if (cyanAccent) 0.95f else 0.74f,
                if (cyanAccent) 1f else 0.22f
            )
        }

        // 8. Rooftop Helipad Landing Platform in Stunt Park
        val hx = CityLayout.HELIPAD_CX
        val hz = CityLayout.HELIPAD_CZ
        val hy = CityLayout.HELIPAD_HEIGHT
        val hs = CityLayout.HELIPAD_HALF_SIZE
        b.addBox(hx, hy * 0.5f, hz, hs * 2f, hy, hs * 2f, 0.18f, 0.21f, 0.28f, 0f)
        addGlowCircle(b, hx, hy + 0.07f, hz, 14.5f, 15.8f, 28, 0.98f, 0.76f, 0.24f)
        b.addBox(hx - 4.4f, hy + 0.07f, hz, 1.9f, 0.06f, 13.0f, 0f, 0.95f, 1f, 4f)
        b.addBox(hx + 4.4f, hy + 0.07f, hz, 1.9f, 0.06f, 13.0f, 0f, 0.95f, 1f, 4f)
        b.addBox(hx, hy + 0.07f, hz, 7.6f, 0.06f, 1.9f, 0f, 0.95f, 1f, 4f)

        // 9. Illuminated Coastal Showroom Podium (used during Splash & Main Menu)
        val sx = CityLayout.SHOWROOM_X
        val sz = CityLayout.SHOWROOM_Z
        val sy = CityLayout.SHOWROOM_Y
        b.addCylinderY(sx, 0f, sz, 9.5f, 8.8f, 0.7f, 32, 0.14f, 0.17f, 0.24f, 0f)
        b.addCylinderY(sx, 0.7f, sz, 7.8f, 7.4f, sy - 0.7f, 32, 0.22f, 0.25f, 0.34f, 3f)
        addGlowCircle(b, sx, sy + 0.04f, sz, 6.8f, 7.35f, 32, 0.0f, 0.94f, 1.0f)
        addGlowCircle(b, sx, 0.74f, sz, 8.2f, 8.65f, 32, 0.98f, 0.76f, 0.24f)

        return b.build()
    }

    private fun addGlowCircle(
        b: MeshBuilder, cx: Float, y: Float, cz: Float, rIn: Float, rOut: Float, segs: Int, r: Float, g: Float, bl: Float
    ) {
        for (i in 0 until segs) {
            val a0 = (i / segs.toFloat()) * 2f * PI.toFloat()
            val a1 = ((i + 1) / segs.toFloat()) * 2f * PI.toFloat()
            b.addQuad(
                cx + cos(a0) * rIn, y, cz + sin(a0) * rIn, 0f, 0f,
                cx + cos(a0) * rOut, y, cz + sin(a0) * rOut, 1f, 0f,
                cx + cos(a1) * rOut, y, cz + sin(a1) * rOut, 1f, 1f,
                cx + cos(a1) * rIn, y, cz + sin(a1) * rIn, 0f, 1f,
                r, g, bl, 4f
            )
        }
    }

    private fun buildInstancedSkyscrapers(): InstancedGpuMesh {
        val b = MeshBuilder(1024)
        // Unit tower: 24m x 76m x 24m (scaled per spec.scale to match AABB collision boxes!)
        b.addBox(0f, 3.0f, 0f, 24.0f, 6.0f, 24.0f, 0.14f, 0.16f, 0.22f, 0f)
        b.addBox(0f, 40.5f, 0f, 22.0f, 69.0f, 22.0f, 1f, 1f, 1f, 2f, uvScale = 1.0f)
        // White neon crown gets tinted dynamically by aInstYawTint.yzw
        b.addBox(0f, 75.5f, 0f, 22.6f, 1.0f, 22.6f, 1f, 1f, 1f, 4f)
        b.addCylinderY(0f, 76.0f, 0f, 0.6f, 0.15f, 8.0f, 6, 0.85f, 0.88f, 0.94f, 3f)

        val inst = FloatArray(CityLayout.buildings.size * 8)
        CityLayout.buildings.forEachIndexed { idx, s ->
            val o = idx * 8
            inst[o] = s.cx; inst[o + 1] = 0f; inst[o + 2] = s.cz
            inst[o + 3] = s.scale; inst[o + 4] = 0f
            inst[o + 5] = s.tintR; inst[o + 6] = s.tintG; inst[o + 7] = s.tintB
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedPalms(): InstancedGpuMesh {
        val b = MeshBuilder(4096)
        b.addCylinderY(0f, 0f, 0f, 1.35f, 1.15f, 0.45f, 8, 0.78f, 0.72f, 0.62f, 0f)
        b.addCylinderY(0f, 0.45f, 0f, 0.36f, 0.28f, 3.4f, 7, 0.46f, 0.30f, 0.18f, 0f)
        b.addCylinderY(0.15f, 3.85f, 0.1f, 0.28f, 0.20f, 3.4f, 7, 0.50f, 0.33f, 0.20f, 0f)
        val topX = 0.28f; val topY = 7.25f; val topZ = 0.16f
        for (i in 0 until 6) {
            val a = i * (PI.toFloat() / 3f)
            val dx = cos(a); val dz = sin(a)
            val sx = -dz * 0.48f; val sz = dx * 0.48f
            val midX = topX + dx * 2.2f; val midZ = topZ + dz * 2.2f
            val tipX = topX + dx * 4.0f; val tipZ = topZ + dz * 4.0f
            b.addQuad(
                topX - sx * 0.35f, topY, topZ - sz * 0.35f, 0f, 0f,
                topX + sx * 0.35f, topY, topZ + sz * 0.35f, 1f, 0f,
                midX + sx, topY + 0.45f, midZ + sz, 1f, 0.5f,
                midX - sx, topY + 0.45f, midZ - sz, 0f, 0.5f,
                0.14f, 0.54f, 0.22f, 0f
            )
            b.addTriangle(
                midX - sx, topY + 0.45f, midZ - sz, 0f, 0.5f,
                midX + sx, topY + 0.45f, midZ + sz, 1f, 0.5f,
                tipX, topY - 0.85f, tipZ, 0.5f, 1f,
                0.18f, 0.64f, 0.26f, 0f
            )
        }

        val entries = mutableListOf<Pair<Float, Float>>()
        for (z in -1400..1400 step 140) {
            if (z in -55..55) continue
            entries.add(-21f to z.toFloat())
            entries.add(21f to z.toFloat())
        }
        for (x in -1400..440 step 140) {
            if (x in -55..55) continue
            entries.add(x.toFloat() to -21f)
            entries.add(x.toFloat() to 21f)
        }
        // Marina oceanfront palms lining the coastline boardwalk
        for (z in -1300..1300 step 110) {
            if (z in 20..105) continue
            entries.add(608f to z.toFloat())
            entries.add(822f to z.toFloat())
        }

        val inst = FloatArray(entries.size * 8)
        entries.forEachIndexed { idx, (px, pz) ->
            val o = idx * 8
            inst[o] = px; inst[o + 1] = 0f; inst[o + 2] = pz
            inst[o + 3] = 0.95f + (idx % 3) * 0.14f
            inst[o + 4] = idx * 1.15f
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedStreetlamps(): InstancedGpuMesh {
        val b = MeshBuilder(1024)
        b.addBox(0f, 3.2f, 0f, 0.20f, 6.4f, 0.20f, 0.65f, 0.68f, 0.74f, 3f)
        b.addBox(0.60f, 6.35f, 0f, 1.35f, 0.16f, 0.24f, 0.65f, 0.68f, 0.74f, 3f)
        b.addBox(1.12f, 6.22f, 0f, 0.45f, 0.14f, 0.28f, 1.0f, 0.88f, 0.62f, 4f)

        val entries = mutableListOf<FloatArray>()
        for (z in -1500..1500 step 90) {
            if (z in -55..55) continue
            entries.add(floatArrayOf(-17.5f, z.toFloat(), 0f))
            entries.add(floatArrayOf(17.5f, z.toFloat(), PI.toFloat()))
        }
        for (x in -1400..500 step 90) {
            if (x in -55..55) continue
            entries.add(floatArrayOf(x.toFloat(), -17.5f, -PI.toFloat() * 0.5f))
            entries.add(floatArrayOf(x.toFloat(), 17.5f, PI.toFloat() * 0.5f))
        }
        for (z in -1400..1400 step 100) {
            if (z in 15..110) continue
            entries.add(floatArrayOf(558f, z.toFloat(), 0f))
            entries.add(floatArrayOf(602f, z.toFloat(), PI.toFloat()))
        }

        val inst = FloatArray(entries.size * 8)
        entries.forEachIndexed { idx, e ->
            val o = idx * 8
            inst[o] = e[0]; inst[o + 1] = 0f; inst[o + 2] = e[1]
            inst[o + 3] = 1f; inst[o + 4] = e[2]
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedBarriers(): InstancedGpuMesh {
        val b = MeshBuilder(1024)
        b.addBox(0f, 0.5f, 0f, 18.0f, 1.0f, 0.8f, 1f, 1f, 1f, 10f)

        val entries = mutableListOf<FloatArray>()
        val ws = CityLayout.WORLD_HALF_SIZE - 5f
        val ec = CityLayout.EAST_COAST_X - 5f
        var x = -ws + 40f
        while (x < ec - 40f) {
            entries.add(floatArrayOf(x, -ws, 0f))
            entries.add(floatArrayOf(x, ws, 0f))
            x += 90f
        }
        var z = -ws + 40f
        while (z < ws - 40f) {
            entries.add(floatArrayOf(-ws, z, (PI * 0.5).toFloat()))
            if (z !in 25f..100f) {
                entries.add(floatArrayOf(ec, z, (PI * 0.5).toFloat()))
            }
            z += 90f
        }
        val inst = FloatArray(entries.size * 8)
        entries.forEachIndexed { idx, e ->
            val o = idx * 8
            inst[o] = e[0]; inst[o + 1] = 0f; inst[o + 2] = e[1]
            inst[o + 3] = 1f; inst[o + 4] = e[2]
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedYachts(): InstancedGpuMesh {
        val b = MeshBuilder(2048)
        // Sleek sculpted superyacht hull + white bridge deck + cyan underwater accent
        b.addBox(0f, 1.4f, 0f, 7.2f, 2.6f, 26f, 0.94f, 0.96f, 0.98f, 6f)
        b.addBox(0f, 1.6f, 14.5f, 4.4f, 2.2f, 7.0f, 0.94f, 0.96f, 0.98f, 6f)
        b.addBox(0f, 3.6f, -1.5f, 5.4f, 2.2f, 14f, 0.96f, 0.97f, 0.99f, 0f)
        b.addBox(0f, 3.8f, 4.2f, 5.1f, 1.2f, 4.5f, 0.04f, 0.06f, 0.12f, 5f)
        b.addBox(0f, 5.4f, -3.0f, 3.8f, 1.5f, 8.0f, 0.96f, 0.97f, 0.99f, 0f)
        b.addBox(0f, 0.5f, 0f, 7.5f, 0.25f, 26.5f, 0.0f, 0.92f, 1.0f, 4f)

        val slots = listOf(
            floatArrayOf(940f, -720f, 0.2f), floatArrayOf(990f, -480f, -0.3f),
            floatArrayOf(950f, -220f, 0.1f), floatArrayOf(1020f, 80f, -0.15f),
            floatArrayOf(960f, 360f, 0.4f), floatArrayOf(1050f, 680f, -0.2f),
            floatArrayOf(1180f, -600f, 0.6f), floatArrayOf(1240f, -300f, -0.5f),
            floatArrayOf(1150f, -40f, 0.3f), floatArrayOf(1280f, 240f, 0.0f),
            floatArrayOf(1200f, 540f, -0.4f), floatArrayOf(1360f, 820f, 0.2f),
            floatArrayOf(1120f, -940f, 0.1f), floatArrayOf(1260f, 980f, -0.3f)
        )
        val inst = FloatArray(slots.size * 8)
        slots.forEachIndexed { idx, s ->
            val o = idx * 8
            inst[o] = s[0]; inst[o + 1] = CityLayout.WATER_LEVEL + 0.15f; inst[o + 2] = s[1]
            inst[o + 3] = 1.0f + (idx % 3) * 0.18f; inst[o + 4] = s[2]
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedCanyonRocks(): InstancedGpuMesh {
        val b = MeshBuilder(1024)
        b.addCylinderY(0f, 0f, 0f, 24f, 16f, 46f, 7, 0.62f, 0.36f, 0.24f, 0f)
        b.addCylinderY(4f, 46f, -2f, 15f, 8f, 22f, 6, 0.72f, 0.44f, 0.28f, 0f)

        val entries = mutableListOf<FloatArray>()
        for (i in 0 until 14) {
            val z = -340f - i * 95f
            val rx = CityLayout.canyonRoadX(z)
            entries.add(floatArrayOf(rx - 95f - (i % 3) * 28f, z, 0.8f + (i % 3) * 0.25f))
            entries.add(floatArrayOf(rx + 95f + (i % 2) * 32f, z - 30f, 0.75f + ((i + 1) % 3) * 0.22f))
        }
        val inst = FloatArray(entries.size * 8)
        entries.forEachIndexed { idx, e ->
            val o = idx * 8
            inst[o] = e[0]; inst[o + 1] = -0.5f; inst[o + 2] = e[1]
            inst[o + 3] = e[2]; inst[o + 4] = idx * 0.7f
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedBillboards(): InstancedGpuMesh {
        val b = MeshBuilder(1024)
        b.addBox(-5.5f, 5.5f, 0f, 0.7f, 11f, 0.7f, 0.2f, 0.22f, 0.28f, 3f)
        b.addBox(5.5f, 5.5f, 0f, 0.7f, 11f, 0.7f, 0.2f, 0.22f, 0.28f, 3f)
        b.addBox(0f, 9.5f, 0f, 14f, 5.2f, 0.8f, 0.08f, 0.10f, 0.14f, 0f)
        b.addBox(0f, 9.5f, 0f, 12.8f, 4.0f, 0.95f, 1f, 1f, 1f, 4f)

        val entries = listOf(
            floatArrayOf(-28f, -180f, 0.3f, 0f, 0.94f, 1f),
            floatArrayOf(28f, -320f, -0.3f, 1f, 0.76f, 0.22f),
            floatArrayOf(-28f, 190f, 0.2f, 1f, 0.20f, 0.65f),
            floatArrayOf(28f, 340f, -0.25f, 0f, 0.94f, 1f),
            floatArrayOf(552f, -640f, 0.2f, 1f, 0.76f, 0.22f),
            floatArrayOf(608f, -150f, -0.2f, 0f, 0.94f, 1f),
            floatArrayOf(552f, 340f, 0.2f, 1f, 0.20f, 0.65f),
            floatArrayOf(-1032f, 650f, -0.2f, 0f, 0.94f, 1f),
            floatArrayOf(-1032f, 1150f, -0.2f, 1f, 0.76f, 0.22f),
            floatArrayOf(-726f, -140f, -0.2f, 1f, 0.20f, 0.65f)
        )
        val inst = FloatArray(entries.size * 8)
        entries.forEachIndexed { idx, e ->
            val o = idx * 8
            inst[o] = e[0]; inst[o + 1] = 0f; inst[o + 2] = e[1]
            inst[o + 3] = 1.15f; inst[o + 4] = e[2]
            inst[o + 5] = e[3]; inst[o + 6] = e[4]; inst[o + 7] = e[5]
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedBoostRings(): InstancedGpuMesh {
        val b = MeshBuilder(2048)
        val segs = 24
        val rIn = 6.2f
        val rOut = 7.2f
        for (i in 0 until segs) {
            val a0 = (i / segs.toFloat()) * 2f * PI.toFloat()
            val a1 = ((i + 1) / segs.toFloat()) * 2f * PI.toFloat()
            val x0 = cos(a0); val y0 = sin(a0)
            val x1 = cos(a1); val y1 = sin(a1)
            b.addQuad(
                x0 * rIn, y0 * rIn, -0.35f, 0f, 0f,
                x0 * rOut, y0 * rOut, -0.35f, 1f, 0f,
                x1 * rOut, y1 * rOut, 0.35f, 1f, 1f,
                x1 * rIn, y1 * rIn, 0.35f, 0f, 1f,
                0.0f, 0.95f, 1.0f, 4f
            )
            b.addQuad(
                x1 * rIn, y1 * rIn, 0.35f, 0f, 1f,
                x1 * rOut, y1 * rOut, 0.35f, 1f, 1f,
                x0 * rOut, y0 * rOut, -0.35f, 1f, 0f,
                x0 * rIn, y0 * rIn, -0.35f, 0f, 0f,
                1.0f, 0.76f, 0.22f, 4f
            )
        }
        val inst = FloatArray(CityLayout.boostRings.size * 8)
        CityLayout.boostRings.forEachIndexed { idx, ring ->
            val o = idx * 8
            inst[o] = ring.cx; inst[o + 1] = ring.cy; inst[o + 2] = ring.cz
            inst[o + 3] = ring.radius / 7.2f; inst[o + 4] = ring.yawRad
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedSpeedTraps(): InstancedGpuMesh {
        val b = MeshBuilder(1024)
        // Gantry speed-trap radar arch spanning road width
        b.addBox(-17f, 4.5f, 0f, 1.1f, 9f, 1.1f, 0.16f, 0.18f, 0.24f, 3f)
        b.addBox(17f, 4.5f, 0f, 1.1f, 9f, 1.1f, 0.16f, 0.18f, 0.24f, 3f)
        b.addBox(0f, 8.8f, 0f, 35.5f, 1.4f, 1.2f, 0.14f, 0.16f, 0.22f, 0f)
        b.addBox(0f, 8.8f, 0f, 32f, 0.55f, 1.35f, 1.0f, 0.20f, 0.35f, 4f)
        b.addBox(-16.5f, 4.5f, 0f, 0.35f, 7.6f, 1.25f, 0.0f, 0.94f, 1.0f, 4f)
        b.addBox(16.5f, 4.5f, 0f, 0.35f, 7.6f, 1.25f, 0.0f, 0.94f, 1.0f, 4f)

        val inst = FloatArray(CityLayout.speedTraps.size * 8)
        CityLayout.speedTraps.forEachIndexed { idx, trap ->
            val o = idx * 8
            val groundY = if (trap.id == "canyon_crest") CityLayout.canyonRoadY(trap.cz) else 0f
            inst[o] = trap.cx; inst[o + 1] = groundY; inst[o + 2] = trap.cz
            inst[o + 3] = 1f; inst[o + 4] = trap.yawRad
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildOceanMesh(): GpuMesh {
        val b = MeshBuilder(16384)
        val extent = 3100f
        val step = 155f
        val y = CityLayout.WATER_LEVEL
        var x = -extent
        while (x < extent) {
            var z = -extent
            while (z < extent) {
                b.addQuad(
                    x, y, z, 0f, 0f,
                    x + step, y, z, 1f, 0f,
                    x + step, y, z + step, 1f, 1f,
                    x, y, z + step, 0f, 1f,
                    0.06f, 0.58f, 0.62f, 0f
                )
                z += step
            }
            x += step
        }
        return b.build()
    }

    private fun buildSkyDomeMesh(): GpuMesh {
        val b = MeshBuilder(4096)
        val r = 1f
        b.addQuad(-r, -r, r, 0f, 0f, r, -r, r, 1f, 0f, r, r, r, 1f, 1f, -r, r, r, 0f, 1f, 1f, 1f, 1f, 0f)
        b.addQuad(r, -r, -r, 0f, 0f, -r, -r, -r, 1f, 0f, -r, r, -r, 1f, 1f, r, r, -r, 0f, 1f, 1f, 1f, 1f, 0f)
        b.addQuad(-r, -r, -r, 0f, 0f, -r, -r, r, 1f, 0f, -r, r, r, 1f, 1f, -r, r, -r, 0f, 1f, 1f, 1f, 1f, 0f)
        b.addQuad(r, -r, r, 0f, 0f, r, -r, -r, 1f, 0f, r, r, -r, 1f, 1f, r, r, r, 0f, 1f, 1f, 1f, 1f, 0f)
        b.addQuad(-r, r, r, 0f, 0f, r, r, r, 1f, 0f, r, r, -r, 1f, 1f, -r, r, -r, 0f, 1f, 1f, 1f, 1f, 0f)
        b.addQuad(-r, -r, -r, 0f, 0f, r, -r, -r, 1f, 0f, r, -r, r, 1f, 1f, -r, -r, r, 0f, 1f, 1f, 1f, 1f, 0f)
        return b.build()
    }
}
