package com.example.game.gl

import com.example.game.CityLayout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class WorldMeshes(
    val staticWorldMesh: GpuMesh,
    val palmInstanced: InstancedGpuMesh,
    val lampInstanced: InstancedGpuMesh,
    val barrierInstanced: InstancedGpuMesh,
    val oceanMesh: GpuMesh,
    val skyDomeMesh: GpuMesh
) {
    fun release() {
        staticWorldMesh.release()
        palmInstanced.release()
        lampInstanced.release()
        barrierInstanced.release()
        oceanMesh.release()
        skyDomeMesh.release()
    }
}

object WorldMeshLibrary {

    fun buildWorld(): WorldMeshes {
        return WorldMeshes(
            staticWorldMesh = buildCityMesh(),
            palmInstanced = buildInstancedPalms(),
            lampInstanced = buildInstancedStreetlamps(),
            barrierInstanced = buildInstancedBarriers(),
            oceanMesh = buildOceanMesh(),
            skyDomeMesh = buildSkyDomeMesh()
        )
    }

    private fun buildCityMesh(): GpuMesh {
        val b = MeshBuilder(131072)

        // 1. Coastal Resort Island Ground & Seawall Perimeter
        val s = CityLayout.ISLAND_HALF_SIZE
        b.addBox((-s + 44f) * 0.5f, -0.8f, 0f, s + 44f, 1.6f, s * 2f, 0.20f, 0.23f, 0.28f, 0f)
        b.addBox((96f + s) * 0.5f, -0.8f, 0f, s - 96f, 1.6f, s * 2f, 0.20f, 0.23f, 0.28f, 0f)
        b.addBox(70f, -0.8f, (-s + 36f) * 0.5f, 52f, 1.6f, s + 36f, 0.20f, 0.23f, 0.28f, 0f)
        b.addBox(70f, -0.8f, (68f + s) * 0.5f, 52f, 1.6f, s - 68f, 0.20f, 0.23f, 0.28f, 0f)

        val rim = s + 2.5f
        b.addBox(0f, -0.35f, -rim, s * 2f + 10f, 0.9f, 5f, 0.85f, 0.72f, 0.50f, 0f)
        b.addBox(0f, -0.35f, rim, s * 2f + 10f, 0.9f, 5f, 0.85f, 0.72f, 0.50f, 0f)
        b.addBox(-rim, -0.35f, 0f, 5f, 0.9f, s * 2f, 0.85f, 0.72f, 0.50f, 0f)
        b.addBox(rim, -0.35f, 0f, 5f, 0.9f, s * 2f, 0.85f, 0.72f, 0.50f, 0f)

        // 2. Procedural Textured Asphalt Boulevards (matType = 1 samples uAsphaltTex)
        // North-South Boulevard
        b.addQuad(-11f, 0.02f, -135f, 0f, 0f, 11f, 0.02f, -135f, 1f, 0f, 11f, 0.02f, 135f, 1f, 12f, -11f, 0.02f, 135f, 0f, 12f, 1f, 1f, 1f, 1f)
        // East-West Boulevard (UV rotated 90 deg so lane markings run along X)
        b.addQuad(-135f, 0.02f, -11f, 0f, 0f, 135f, 0.02f, -11f, 0f, 12f, 135f, 0.02f, 11f, 1f, 12f, -135f, 0.02f, 11f, 1f, 0f, 1f, 1f, 1f, 1f)

        // Outer Coastal Ring Road
        b.addQuad(-126f, 0.02f, -126f, 0f, 0f, 126f, 0.02f, -126f, 0f, 12f, 126f, 0.02f, -108f, 1f, 12f, -126f, 0.02f, -108f, 1f, 0f, 1f, 1f, 1f, 1f)
        b.addQuad(-126f, 0.02f, 108f, 0f, 0f, 126f, 0.02f, 108f, 0f, 12f, 126f, 0.02f, 126f, 1f, 12f, -126f, 0.02f, 126f, 1f, 0f, 1f, 1f, 1f, 1f)
        b.addQuad(-126f, 0.02f, -108f, 0f, 0f, -108f, 0.02f, -108f, 1f, 0f, -108f, 0.02f, 108f, 1f, 10f, -126f, 0.02f, 108f, 0f, 10f, 1f, 1f, 1f, 1f)
        b.addQuad(108f, 0.02f, -108f, 0f, 0f, 126f, 0.02f, -108f, 1f, 0f, 126f, 0.02f, 108f, 1f, 10f, 108f, 0.02f, 108f, 0f, 10f, 1f, 1f, 1f, 1f)

        // Hypercar Test Straight Road
        b.addQuad(46f, 0.025f, -115f, 0f, 0f, 68f, 0.025f, -115f, 1f, 0f, 68f, 0.025f, 20f, 1f, 6f, 46f, 0.025f, 20f, 0f, 6f, 1f, 1f, 1f, 1f)

        // Crosswalks approaching Grand Central Roundabout
        for (cx in -9..9 step 3) {
            b.addBox(cx.toFloat(), 0.032f, -42.5f, 1.3f, 0.01f, 3.6f, 0.94f, 0.95f, 0.98f, 0f)
            b.addBox(cx.toFloat(), 0.032f, 42.5f, 1.3f, 0.01f, 3.6f, 0.94f, 0.95f, 0.98f, 0f)
        }
        for (cz in -9..9 step 3) {
            b.addBox(-42.5f, 0.032f, cz.toFloat(), 3.6f, 0.01f, 1.3f, 0.94f, 0.95f, 0.98f, 0f)
            b.addBox(42.5f, 0.032f, cz.toFloat(), 3.6f, 0.01f, 1.3f, 0.94f, 0.95f, 0.98f, 0f)
        }

        // 3. Grand Central Drift Roundabout + Red/White Racing Curbs (matType = 10)
        val ringSegs = 44
        val rInner = 14.5f
        val rOuter = 40f
        for (i in 0 until ringSegs) {
            val a0 = (i.toFloat() / ringSegs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / ringSegs) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            b.addQuad(
                c0 * rInner, 0.03f, s0 * rInner, 0f, i * 0.5f,
                c0 * rOuter, 0.03f, s0 * rOuter, 1f, i * 0.5f,
                c1 * rOuter, 0.03f, s1 * rOuter, 1f, (i + 1) * 0.5f,
                c1 * rInner, 0.03f, s1 * rInner, 0f, (i + 1) * 0.5f,
                1f, 1f, 1f, 1f
            )
            // Inner red/white racing curb around monument
            b.addQuad(
                c0 * 13.4f, 0.14f, s0 * 13.4f, 0f, i * 0.5f,
                c0 * 14.6f, 0.06f, s0 * 14.6f, 1f, i * 0.5f,
                c1 * 14.6f, 0.06f, s1 * 14.6f, 1f, (i + 1) * 0.5f,
                c1 * 13.4f, 0.14f, s1 * 13.4f, 0f, (i + 1) * 0.5f,
                1f, 1f, 1f, 10f
            )
        }
        b.addCylinderY(0f, 0f, 0f, 13.2f, 12.2f, 1.4f, 24, 0.88f, 0.84f, 0.76f, 0f)
        b.addCylinderY(0f, 1.4f, 0f, 5.5f, 4.0f, 3.2f, 16, 0.16f, 0.19f, 0.26f, 3f)
        b.addCylinderY(0f, 4.6f, 0f, 2.2f, 0.25f, 16.0f, 12, 0.96f, 0.74f, 0.26f, 3f)
        b.addBox(0f, 20.8f, 0f, 1.1f, 1.1f, 1.1f, 0f, 0.95f, 1f, 4f)

        // 4. Downtown Financial District Glass Skyscrapers (matType = 2 uses uSkyscraperTex)
        for (bld in CityLayout.buildings) {
            b.addBox(bld.cx, 2.5f, bld.cz, bld.halfW * 2.0f, 5.0f, bld.halfD * 2.0f, 0.16f, 0.18f, 0.24f, 0f)
            b.addBox(
                bld.cx, 5.0f + (bld.height - 5.0f) * 0.5f, bld.cz,
                bld.halfW * 1.82f, bld.height - 5.0f, bld.halfD * 1.82f,
                1f, 1f, 1f, 2f, uvScale = 1.0f
            )
            val cr = if (bld.accentCyan) 0.0f else 0.98f
            val cg = if (bld.accentCyan) 0.92f else 0.72f
            val cb = if (bld.accentCyan) 1.0f else 0.24f
            b.addBox(bld.cx, bld.height + 0.35f, bld.cz, bld.halfW * 1.86f, 0.7f, bld.halfD * 1.86f, cr, cg, cb, 4f)
        }

        // 5. Glowing Neon Acceleration Tunnel
        for (zPos in -110..-30 step 10) {
            val zf = zPos.toFloat()
            b.addBox(46.5f, 4.2f, zf, 1.1f, 8.4f, 1.4f, 0.14f, 0.16f, 0.22f, 0f)
            b.addBox(67.5f, 4.2f, zf, 1.1f, 8.4f, 1.4f, 0.14f, 0.16f, 0.22f, 0f)
            b.addBox(57f, 8.4f, zf, 22.1f, 1.1f, 1.4f, 0.14f, 0.16f, 0.22f, 0f)
            val isCyan = (zPos / 10) % 2 == 0
            val nr = if (isCyan) 0.0f else 1.0f
            val ng = if (isCyan) 0.95f else 0.74f
            val nb = if (isCyan) 1.0f else 0.22f
            b.addBox(47.2f, 4.2f, zf, 0.35f, 7.6f, 0.45f, nr, ng, nb, 4f)
            b.addBox(66.8f, 4.2f, zf, 0.35f, 7.6f, 0.45f, nr, ng, nb, 4f)
            b.addBox(57f, 7.8f, zf, 19.6f, 0.35f, 0.45f, nr, ng, nb, 4f)
        }

        // 6. Speed Boost Pads
        for (pad in CityLayout.boostPads) {
            b.addBox(pad.cx, 0.06f, pad.cz, pad.halfW * 1.8f, 0.08f, pad.halfD * 1.8f, 0.05f, 0.12f, 0.20f, 0f)
            b.addBox(pad.cx, 0.11f, pad.cz - 1.8f, pad.halfW * 1.5f, 0.04f, 1.1f, 0.0f, 0.95f, 1.0f, 4f)
            b.addBox(pad.cx, 0.11f, pad.cz, pad.halfW * 1.5f, 0.04f, 1.1f, 1.0f, 0.76f, 0.22f, 4f)
            b.addBox(pad.cx, 0.11f, pad.cz + 1.8f, pad.halfW * 1.5f, 0.04f, 1.1f, 0.0f, 0.95f, 1.0f, 4f)
        }

        // 7. Marina Canal Dual Hydraulic Stunt Ramps
        b.addRamp(53f, 65f, 20f, 36f, 0f, 4.8f, 0.22f, 0.25f, 0.32f, 0f, 0f, 0.95f, 1f)
        b.addRamp(53f, 65f, 84f, 68f, 0f, 4.8f, 0.22f, 0.25f, 0.32f, 0f, 1f, 0.74f, 0.22f)
        b.addRamp(75f, 87f, 20f, 36f, 0f, 4.8f, 0.22f, 0.25f, 0.32f, 0f, 0f, 0.95f, 1f)
        b.addRamp(75f, 87f, 84f, 68f, 0f, 4.8f, 0.22f, 0.25f, 0.32f, 0f, 1f, 0.74f, 0.22f)

        // 8. Rooftop Helipad Landing Zone & Approach Ramp
        b.addBox(CityLayout.HELIPAD_CX, CityLayout.HELIPAD_HEIGHT * 0.5f, CityLayout.HELIPAD_CZ, 32f, CityLayout.HELIPAD_HEIGHT, 32f, 0.18f, 0.21f, 0.28f, 0f)
        b.addRamp(110f, 126f, 6f, 42f, 0f, CityLayout.HELIPAD_HEIGHT, 0.20f, 0.22f, 0.28f, 1f, 1f, 0.78f, 0.24f)
        val hy = CityLayout.HELIPAD_HEIGHT + 0.06f
        val hx = CityLayout.HELIPAD_CX
        val hz = CityLayout.HELIPAD_CZ
        for (i in 0 until 28) {
            val a0 = (i / 28f) * 2f * PI.toFloat()
            val a1 = ((i + 1) / 28f) * 2f * PI.toFloat()
            b.addQuad(
                hx + cos(a0) * 10.5f, hy, hz + sin(a0) * 10.5f, 0f, 0f,
                hx + cos(a0) * 11.4f, hy, hz + sin(a0) * 11.4f, 1f, 0f,
                hx + cos(a1) * 11.4f, hy, hz + sin(a1) * 11.4f, 1f, 1f,
                hx + cos(a1) * 10.5f, hy, hz + sin(a1) * 10.5f, 0f, 1f,
                0.98f, 0.76f, 0.24f, 4f
            )
        }
        b.addBox(hx - 3.6f, hy, hz, 1.6f, 0.06f, 10.5f, 0f, 0.95f, 1f, 4f)
        b.addBox(hx + 3.6f, hy, hz, 1.6f, 0.06f, 10.5f, 0f, 0.95f, 1f, 4f)
        b.addBox(hx, hy, hz, 6.2f, 0.06f, 1.6f, 0f, 0.95f, 1f, 4f)

        return b.build()
    }

    private fun buildInstancedPalms(): InstancedGpuMesh {
        val b = MeshBuilder(4096)
        b.addCylinderY(0f, 0f, 0f, 1.35f, 1.15f, 0.45f, 8, 0.78f, 0.72f, 0.62f, 0f)
        b.addCylinderY(0f, 0.45f, 0f, 0.36f, 0.28f, 3.2f, 7, 0.46f, 0.30f, 0.18f, 0f)
        b.addCylinderY(0.15f, 3.65f, 0.1f, 0.28f, 0.20f, 3.2f, 7, 0.50f, 0.33f, 0.20f, 0f)
        val topX = 0.28f; val topY = 6.85f; val topZ = 0.16f
        for (i in 0 until 6) {
            val a = i * (PI.toFloat() / 3f)
            val dx = cos(a); val dz = sin(a)
            val sx = -dz * 0.48f; val sz = dx * 0.48f
            val midX = topX + dx * 2.1f; val midZ = topZ + dz * 2.1f
            val tipX = topX + dx * 3.8f; val tipZ = topZ + dz * 3.8f
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

        val coords = listOf(
            -15f to -55f, 15f to -55f, -15f to -85f, 15f to -85f, -15f to -115f, 15f to -115f,
            -15f to 55f, 15f to 55f, -15f to 85f, 15f to 85f, -15f to 115f, 15f to 115f,
            -55f to -15f, -55f to 15f, -85f to -15f, -85f to 15f, -115f to -15f, -115f to 15f,
            35f to -15f, 35f to 15f, 92f to -15f, 92f to 15f,
            -32f to -32f, 32f to -32f, -32f to 32f, 32f to 32f
        )
        val inst = FloatArray(coords.size * 8)
        coords.forEachIndexed { idx, (px, pz) ->
            val o = idx * 8
            inst[o] = px; inst[o + 1] = 0f; inst[o + 2] = pz
            inst[o + 3] = 0.92f + (idx % 3) * 0.12f
            inst[o + 4] = (idx * 1.1f)
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildInstancedStreetlamps(): InstancedGpuMesh {
        val b = MeshBuilder(1024)
        // Sleek brushed-metal pole + cantilevered arm + glowing warm LED bulb
        b.addBox(0f, 3.0f, 0f, 0.18f, 6.0f, 0.18f, 0.65f, 0.68f, 0.74f, 3f)
        b.addBox(0.55f, 5.95f, 0f, 1.25f, 0.14f, 0.22f, 0.65f, 0.68f, 0.74f, 3f)
        b.addBox(1.05f, 5.82f, 0f, 0.42f, 0.12f, 0.26f, 1.0f, 0.88f, 0.62f, 4f)

        val entries = mutableListOf<FloatArray>()
        for (z in -100..100 step 32) {
            if (z in -38..38) continue
            entries.add(floatArrayOf(-12.5f, z.toFloat(), 0f))
            entries.add(floatArrayOf(12.5f, z.toFloat(), PI.toFloat()))
        }
        for (x in -100..100 step 32) {
            if (x in -38..38) continue
            entries.add(floatArrayOf(x.toFloat(), -12.5f, -PI.toFloat() * 0.5f))
            entries.add(floatArrayOf(x.toFloat(), 12.5f, PI.toFloat() * 0.5f))
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
        // Red/white racing barrier segment (matType = 10 uses uCurbTex)
        b.addBox(0f, 0.42f, 0f, 5.6f, 0.84f, 0.55f, 1f, 1f, 1f, 10f)

        val entries = mutableListOf<FloatArray>()
        // Waterfront seawall barriers & canal edge safety curbs
        for (x in -132..132 step 16) {
            if (x in -14..14) continue
            entries.add(floatArrayOf(x.toFloat(), -141f, 0f))
            entries.add(floatArrayOf(x.toFloat(), 141f, 0f))
        }
        for (z in -132..132 step 16) {
            if (z in -14..14) continue
            entries.add(floatArrayOf(-141f, z.toFloat(), PI.toFloat() * 0.5f))
            entries.add(floatArrayOf(141f, z.toFloat(), PI.toFloat() * 0.5f))
        }
        // Canal safety barriers (skip ramp lanes)
        entries.add(floatArrayOf(47f, 35.5f, 0f))
        entries.add(floatArrayOf(93f, 35.5f, 0f))
        entries.add(floatArrayOf(47f, 68.5f, 0f))
        entries.add(floatArrayOf(93f, 68.5f, 0f))

        val inst = FloatArray(entries.size * 8)
        entries.forEachIndexed { idx, e ->
            val o = idx * 8
            inst[o] = e[0]; inst[o + 1] = 0f; inst[o + 2] = e[1]
            inst[o + 3] = 1f; inst[o + 4] = e[2]
            inst[o + 5] = 1f; inst[o + 6] = 1f; inst[o + 7] = 1f
        }
        return b.buildInstanced(inst)
    }

    private fun buildOceanMesh(): GpuMesh {
        val b = MeshBuilder(16384)
        val extent = 740f
        val step = 37f
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
