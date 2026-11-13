package com.example.game.gl

import com.example.game.CarType

class CarMeshSet(
    val bodyMesh: GpuMesh,
    val wheelMesh: GpuMesh,
    val caliperMesh: GpuMesh,
    val flameMesh: GpuMesh?
) {
    fun release() {
        bodyMesh.release()
        wheelMesh.release()
        caliperMesh.release()
        flameMesh?.release()
    }
}

object CarMeshLibrary {

    fun buildAllCars(): Map<CarType, CarMeshSet> = mapOf(
        CarType.EV_HYPER_GT to buildEvHyperGt(),
        CarType.V12_APEX_ROADSTER to buildV12ApexRoadster(),
        CarType.VIP_EXECUTIVE_COUPE to buildVipExecutiveCoupe()
    )

    private fun addWheelArchFlares(b: MeshBuilder) {
        // Sculpted carbon-composite arch eyebrows framing all 4 wheel wells
        for (zPos in floatArrayOf(1.44f, -1.38f)) {
            for (side in floatArrayOf(-1f, 1f)) {
                val xArch = side * 1.01f
                b.addBox(xArch, 0.72f, zPos, 0.14f, 0.08f, 0.62f, 0.09f, 0.10f, 0.12f, 0f)
                b.addBox(xArch, 0.56f, zPos + 0.34f, 0.14f, 0.28f, 0.10f, 0.09f, 0.10f, 0.12f, 0f)
                b.addBox(xArch, 0.56f, zPos - 0.34f, 0.14f, 0.28f, 0.10f, 0.09f, 0.10f, 0.12f, 0f)
            }
        }
    }

    private fun buildCaliperMesh(): GpuMesh {
        val cb = MeshBuilder(1024)
        // Metallic brake rotor disc (matType = 3) + painted brake caliper housing (matType = 11)
        cb.addBox(0f, 0f, 0f, 0.06f, 0.42f, 0.42f, 0.62f, 0.65f, 0.70f, 3f)
        cb.addBox(0f, 0.08f, -0.15f, 0.12f, 0.18f, 0.16f, 1f, 1f, 1f, 11f)
        return cb.build()
    }

    private fun buildEvHyperGt(): CarMeshSet {
        val b = MeshBuilder()

        val chassisSlices = listOf(
            -2.32f to octProfile(0.84f, 0.34f, 0.88f),
            -1.95f to octProfile(1.00f, 0.30f, 0.96f),
            -0.80f to octProfile(1.03f, 0.28f, 0.90f),
            0.75f to octProfile(1.01f, 0.26f, 0.86f),
            1.85f to octProfile(0.95f, 0.24f, 0.72f),
            2.34f to octProfile(0.72f, 0.22f, 0.44f)
        )
        b.addLoftedProfile(chassisSlices, 1f, 1f, 1f, 6f)

        val canopySlices = listOf(
            -1.45f to octProfile(0.58f, 0.86f, 1.00f),
            -0.55f to octProfile(0.74f, 0.84f, 1.24f),
            0.35f to octProfile(0.70f, 0.82f, 1.20f),
            1.15f to octProfile(0.56f, 0.72f, 0.90f)
        )
        b.addLoftedProfile(canopySlices, 0.05f, 0.07f, 0.10f, 5f)

        addWheelArchFlares(b)

        // Carbon fiber splitter, skirts & diffuser
        b.addBox(0f, 0.18f, 2.26f, 1.98f, 0.07f, 0.48f, 0.09f, 0.10f, 0.12f, 0f)
        b.addBox(-0.98f, 0.20f, 0f, 0.14f, 0.08f, 2.35f, 0.09f, 0.10f, 0.12f, 0f)
        b.addBox(0.98f, 0.20f, 0f, 0.14f, 0.08f, 2.35f, 0.09f, 0.10f, 0.12f, 0f)
        b.addBox(0f, 0.24f, -2.28f, 1.72f, 0.18f, 0.24f, 0.08f, 0.09f, 0.11f, 0f)

        // Active rear spoiler
        b.addBox(-0.48f, 0.95f, -2.08f, 0.07f, 0.28f, 0.24f, 0.10f, 0.11f, 0.14f, 0f)
        b.addBox(0.48f, 0.95f, -2.08f, 0.07f, 0.28f, 0.24f, 0.10f, 0.11f, 0.14f, 0f)
        b.addBox(0f, 1.10f, -2.14f, 1.92f, 0.06f, 0.34f, 0.12f, 0.14f, 0.18f, 6f)

        // Cyan LED accent blades (matType = 8)
        b.addBox(0f, 0.44f, 2.34f, 1.48f, 0.055f, 0.08f, 0f, 0.95f, 1f, 8f)
        b.addBox(-1.02f, 0.24f, 0f, 0.04f, 0.04f, 1.9f, 0f, 0.95f, 1f, 8f)
        b.addBox(1.02f, 0.24f, 0f, 0.04f, 0.04f, 1.9f, 0f, 0.95f, 1f, 8f)

        // Front headlight LEDs (matType = 4)
        b.addBox(-0.68f, 0.50f, 2.24f, 0.38f, 0.06f, 0.14f, 0.88f, 0.98f, 1.0f, 4f)
        b.addBox(0.68f, 0.50f, 2.24f, 0.38f, 0.06f, 0.14f, 0.88f, 0.98f, 1.0f, 4f)

        // Full-width reactive brake light bar (matType = 7) & reverse lights (matType = 9)
        b.addBox(0f, 0.68f, -2.33f, 1.66f, 0.085f, 0.07f, 1f, 0.1f, 0.1f, 7f)
        b.addBox(-0.46f, 0.56f, -2.33f, 0.26f, 0.055f, 0.07f, 1f, 1f, 1f, 9f)
        b.addBox(0.46f, 0.56f, -2.33f, 0.26f, 0.055f, 0.07f, 1f, 1f, 1f, 9f)

        val wb = MeshBuilder(4096)
        wb.addWheelX(radius = 0.36f, width = 0.30f, rimR = 0.72f, rimG = 0.94f, rimB = 1.0f)

        return CarMeshSet(b.build(), wb.build(), buildCaliperMesh(), null)
    }

    private fun buildV12ApexRoadster(): CarMeshSet {
        val b = MeshBuilder()

        val chassisSlices = listOf(
            -2.36f to octProfile(0.90f, 0.30f, 0.90f),
            -1.75f to octProfile(1.08f, 0.26f, 0.94f),
            -0.50f to octProfile(1.06f, 0.24f, 0.86f),
            0.85f to octProfile(1.04f, 0.23f, 0.84f),
            1.92f to octProfile(0.98f, 0.21f, 0.70f),
            2.38f to octProfile(0.76f, 0.19f, 0.42f)
        )
        b.addLoftedProfile(chassisSlices, 1f, 1f, 1f, 6f)

        val windshieldSlices = listOf(
            0.22f to octProfile(0.72f, 0.76f, 1.12f),
            0.95f to octProfile(0.62f, 0.74f, 0.92f)
        )
        b.addLoftedProfile(windshieldSlices, 0.04f, 0.06f, 0.09f, 5f)
        b.addBox(-0.42f, 0.92f, -0.78f, 0.36f, 0.22f, 0.86f, 1f, 1f, 1f, 6f)
        b.addBox(0.42f, 0.92f, -0.78f, 0.36f, 0.22f, 0.86f, 1f, 1f, 1f, 6f)
        b.addBox(-0.42f, 1.04f, -0.44f, 0.22f, 0.14f, 0.12f, 0.85f, 0.85f, 0.90f, 3f)
        b.addBox(0.42f, 1.04f, -0.44f, 0.22f, 0.14f, 0.12f, 0.85f, 0.85f, 0.90f, 3f)
        b.addBox(0f, 0.76f, -0.05f, 1.24f, 0.14f, 0.92f, 0.14f, 0.09f, 0.08f, 0f)

        addWheelArchFlares(b)

        b.addBox(0f, 0.16f, 2.30f, 2.08f, 0.06f, 0.50f, 0.08f, 0.08f, 0.10f, 0f)
        b.addBox(-1.04f, 0.36f, -0.30f, 0.16f, 0.32f, 1.10f, 0.09f, 0.09f, 0.11f, 0f)
        b.addBox(1.04f, 0.36f, -0.30f, 0.16f, 0.32f, 1.10f, 0.09f, 0.09f, 0.11f, 0f)

        // Swan-neck high GT carbon rear wing
        b.addBox(-0.56f, 1.06f, -2.14f, 0.07f, 0.42f, 0.26f, 0.10f, 0.10f, 0.12f, 0f)
        b.addBox(0.56f, 1.06f, -2.14f, 0.07f, 0.42f, 0.26f, 0.10f, 0.10f, 0.12f, 0f)
        b.addBox(0f, 1.28f, -2.22f, 2.12f, 0.07f, 0.38f, 0.10f, 0.10f, 0.12f, 0f)
        b.addBox(-1.06f, 1.26f, -2.22f, 0.05f, 0.26f, 0.44f, 1f, 1f, 1f, 6f)
        b.addBox(1.06f, 1.26f, -2.22f, 0.05f, 0.26f, 0.44f, 1f, 1f, 1f, 6f)

        b.addBox(-0.24f, 0.34f, -2.38f, 0.15f, 0.15f, 0.20f, 0.85f, 0.85f, 0.88f, 3f)
        b.addBox(0.24f, 0.34f, -2.38f, 0.15f, 0.15f, 0.20f, 0.85f, 0.85f, 0.88f, 3f)

        b.addBox(-0.70f, 0.48f, 2.28f, 0.36f, 0.07f, 0.12f, 1.0f, 0.92f, 0.72f, 4f)
        b.addBox(0.70f, 0.48f, 2.28f, 0.36f, 0.07f, 0.12f, 1.0f, 0.92f, 0.72f, 4f)
        b.addBox(-0.62f, 0.66f, -2.37f, 0.50f, 0.10f, 0.07f, 1f, 0.1f, 0.1f, 7f)
        b.addBox(0.62f, 0.66f, -2.37f, 0.50f, 0.10f, 0.07f, 1f, 0.1f, 0.1f, 7f)
        b.addBox(0f, 0.54f, -2.37f, 0.42f, 0.06f, 0.07f, 1f, 1f, 1f, 9f)

        val wb = MeshBuilder(4096)
        wb.addWheelX(radius = 0.36f, width = 0.32f, rimR = 0.92f, rimG = 0.76f, rimB = 0.34f)

        val fb = MeshBuilder(1024)
        for (exX in floatArrayOf(-0.24f, 0.24f)) {
            fb.addTriangle(exX - 0.09f, 0.34f, -2.44f, 0f, 0f, exX + 0.09f, 0.34f, -2.44f, 1f, 0f, exX, 0.34f, -3.28f, 0.5f, 1f, 1.0f, 0.48f, 0.08f, 4f)
            fb.addTriangle(exX, 0.25f, -2.44f, 0f, 0f, exX, 0.43f, -2.44f, 1f, 0f, exX, 0.34f, -3.28f, 0.5f, 1f, 0.45f, 0.85f, 1.0f, 4f)
        }

        return CarMeshSet(b.build(), wb.build(), buildCaliperMesh(), fb.build())
    }

    private fun buildVipExecutiveCoupe(): CarMeshSet {
        val b = MeshBuilder()

        val chassisSlices = listOf(
            -2.42f to octProfile(0.88f, 0.32f, 0.96f),
            -1.70f to octProfile(1.02f, 0.28f, 1.00f),
            -0.30f to octProfile(1.04f, 0.26f, 0.98f),
            1.15f to octProfile(1.00f, 0.26f, 0.92f),
            2.10f to octProfile(0.92f, 0.24f, 0.82f),
            2.46f to octProfile(0.76f, 0.22f, 0.62f)
        )
        b.addLoftedProfile(chassisSlices, 1f, 1f, 1f, 6f)

        val canopySlices = listOf(
            -1.55f to octProfile(0.64f, 0.94f, 1.18f),
            -0.55f to octProfile(0.76f, 0.94f, 1.38f),
            0.40f to octProfile(0.74f, 0.92f, 1.34f),
            1.05f to octProfile(0.62f, 0.84f, 1.04f)
        )
        b.addLoftedProfile(canopySlices, 0.04f, 0.05f, 0.08f, 5f)

        addWheelArchFlares(b)

        b.addBox(0f, 0.52f, 2.47f, 0.72f, 0.36f, 0.05f, 0.96f, 0.75f, 0.26f, 3f)
        b.addBox(0f, 0.52f, 2.49f, 0.56f, 0.28f, 0.04f, 0.08f, 0.09f, 0.12f, 0f)
        b.addBox(-1.02f, 0.24f, 0f, 0.08f, 0.06f, 2.65f, 0.96f, 0.75f, 0.26f, 3f)
        b.addBox(1.02f, 0.24f, 0f, 0.08f, 0.06f, 2.65f, 0.96f, 0.75f, 0.26f, 3f)
        b.addBox(0f, 0.98f, -2.26f, 1.56f, 0.06f, 0.20f, 0.96f, 0.75f, 0.26f, 3f)

        b.addBox(-0.68f, 0.56f, 2.38f, 0.38f, 0.08f, 0.12f, 1.0f, 0.96f, 0.86f, 4f)
        b.addBox(0.68f, 0.56f, 2.38f, 0.38f, 0.08f, 0.12f, 1.0f, 0.96f, 0.86f, 4f)
        b.addBox(0f, 0.70f, -2.43f, 1.62f, 0.09f, 0.07f, 1f, 0.1f, 0.1f, 7f)
        b.addBox(-0.50f, 0.57f, -2.43f, 0.24f, 0.05f, 0.07f, 1f, 1f, 1f, 9f)
        b.addBox(0.50f, 0.57f, -2.43f, 0.24f, 0.05f, 0.07f, 1f, 1f, 1f, 9f)

        val wb = MeshBuilder(4096)
        wb.addWheelX(radius = 0.36f, width = 0.29f, rimR = 0.96f, rimG = 0.78f, rimB = 0.32f)

        return CarMeshSet(b.build(), wb.build(), buildCaliperMesh(), null)
    }

    private fun octProfile(halfW: Float, yBottom: Float, yTop: Float): List<Pair<Float, Float>> {
        val bevelX = halfW * 0.86f
        val yMidLow = yBottom + (yTop - yBottom) * 0.26f
        val yMidHigh = yBottom + (yTop - yBottom) * 0.74f
        return listOf(
            -bevelX to yBottom,
            bevelX to yBottom,
            halfW to yMidLow,
            halfW to yMidHigh,
            bevelX to yTop,
            -bevelX to yTop,
            -halfW to yMidHigh,
            -halfW to yMidLow
        )
    }
}
