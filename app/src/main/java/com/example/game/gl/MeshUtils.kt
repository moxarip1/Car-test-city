package com.example.game.gl

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class GpuMesh(
    val vaoId: Int,
    val vboId: Int,
    val vertexCount: Int
) {
    fun draw(mode: Int = GLES30.GL_TRIANGLES) {
        if (vertexCount <= 0) return
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(mode, 0, vertexCount)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
    }
}

class InstancedGpuMesh(
    val vaoId: Int,
    val vboId: Int,
    val instanceVboId: Int,
    val vertexCount: Int,
    val instanceCount: Int
) {
    fun drawInstanced() {
        if (vertexCount <= 0 || instanceCount <= 0) return
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, vertexCount, instanceCount)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteBuffers(2, intArrayOf(vboId, instanceVboId), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
    }
}

/**
 * Vertex stride: 12 floats:
 * pos(3), normal(3), uv(2), color(3), matType(1)
 * matType:
 *  0 = standard lit (concrete, palm, sand)
 *  1 = asphalt road (procedural lane lines)
 *  2 = skyscraper glass (procedural lit windows + sky reflection)
 *  3 = metallic gold / chrome trim
 *  4 = neon emissive (LED strips, boost pads, tunnel rings)
 *  5 = tinted car canopy glass
 *  6 = car primary paint (uses live uniform paint color + glossy/matte/chrome)
 */
class MeshBuilder(initialFloatCapacity: Int = 16384) {
    private var data = FloatArray(initialFloatCapacity)
    private var count = 0

    private fun ensureCapacity(extraFloats: Int) {
        if (count + extraFloats > data.size) {
            data = data.copyOf(maxOf(data.size * 2, count + extraFloats))
        }
    }

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Float, g: Float, b: Float,
        matType: Float
    ) {
        ensureCapacity(12)
        val i = count
        data[i] = x
        data[i + 1] = y
        data[i + 2] = z
        data[i + 3] = nx
        data[i + 4] = ny
        data[i + 5] = nz
        data[i + 6] = u
        data[i + 7] = v
        data[i + 8] = r
        data[i + 9] = g
        data[i + 10] = b
        data[i + 11] = matType
        count += 12
    }

    fun addTriangle(
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        r: Float, g: Float, b: Float, matType: Float
    ) {
        val ux = x2 - x1
        val uy = y2 - y1
        val uz = z2 - z1
        val vx = x3 - x1
        val vy = y3 - y1
        val vz = z3 - z1
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = hypot(hypot(nx, ny), nz).coerceAtLeast(1e-5f)
        nx /= len; ny /= len; nz /= len
        addVertex(x1, y1, z1, nx, ny, nz, u1, v1, r, g, b, matType)
        addVertex(x2, y2, z2, nx, ny, nz, u2, v2, r, g, b, matType)
        addVertex(x3, y3, z3, nx, ny, nz, u3, v3, r, g, b, matType)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        x4: Float, y4: Float, z4: Float, u4: Float, v4: Float,
        r: Float, g: Float, b: Float, matType: Float
    ) {
        addTriangle(x1, y1, z1, u1, v1, x2, y2, z2, u2, v2, x3, y3, z3, u3, v3, r, g, b, matType)
        addTriangle(x1, y1, z1, u1, v1, x3, y3, z3, u3, v3, x4, y4, z4, u4, v4, r, g, b, matType)
    }

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        w: Float, h: Float, d: Float,
        r: Float, g: Float, b: Float,
        matType: Float,
        uvScale: Float = 1f
    ) {
        val hw = w * 0.5f
        val hh = h * 0.5f
        val hd = d * 0.5f
        val x0 = cx - hw; val x1 = cx + hw
        val y0 = cy - hh; val y1 = cy + hh
        val z0 = cz - hd; val z1 = cz + hd

        // Front (+Z)
        addQuad(x0, y0, z1, 0f, 0f, x1, y0, z1, w * uvScale, 0f, x1, y1, z1, w * uvScale, h * uvScale, x0, y1, z1, 0f, h * uvScale, r, g, b, matType)
        // Back (-Z)
        addQuad(x1, y0, z0, 0f, 0f, x0, y0, z0, w * uvScale, 0f, x0, y1, z0, w * uvScale, h * uvScale, x1, y1, z0, 0f, h * uvScale, r, g, b, matType)
        // Right (+X)
        addQuad(x1, y0, z1, 0f, 0f, x1, y0, z0, d * uvScale, 0f, x1, y1, z0, d * uvScale, h * uvScale, x1, y1, z1, 0f, h * uvScale, r, g, b, matType)
        // Left (-X)
        addQuad(x0, y0, z0, 0f, 0f, x0, y0, z1, d * uvScale, 0f, x0, y1, z1, d * uvScale, h * uvScale, x0, y1, z0, 0f, h * uvScale, r, g, b, matType)
        // Top (+Y)
        addQuad(x0, y1, z1, 0f, 0f, x1, y1, z1, w * uvScale, 0f, x1, y1, z0, w * uvScale, d * uvScale, x0, y1, z0, 0f, d * uvScale, r, g, b, matType)
        // Bottom (-Y)
        addQuad(x0, y0, z0, 0f, 0f, x1, y0, z0, w * uvScale, 0f, x1, y0, z1, w * uvScale, d * uvScale, x0, y0, z1, 0f, d * uvScale, r, g, b, matType)
    }

    /**
     * Sculpted loft between cross-section slices along Z.
     * Each slice is List<Pair<Float, Float>> (x, y) around the perimeter in CCW order.
     */
    fun addLoftedProfile(
        slices: List<Pair<Float, List<Pair<Float, Float>>>>,
        r: Float, g: Float, b: Float,
        matType: Float
    ) {
        if (slices.size < 2) return
        for (s in 0 until slices.size - 1) {
            val z0 = slices[s].first
            val p0 = slices[s].second
            val z1 = slices[s + 1].first
            val p1 = slices[s + 1].second
            val n = p0.size
            for (i in 0 until n) {
                val j = (i + 1) % n
                val (ax, ay) = p0[i]
                val (bx, by) = p0[j]
                val (cx, cy) = p1[j]
                val (dx, dy) = p1[i]
                addQuad(
                    ax, ay, z0, 0f, 0f,
                    bx, by, z0, 1f, 0f,
                    cx, cy, z1, 1f, 1f,
                    dx, dy, z1, 0f, 1f,
                    r, g, b, matType
                )
            }
        }
        // Cap ends
        val front = slices.first()
        val fPts = front.second
        var fcx = 0f; var fcy = 0f
        for (p in fPts) { fcx += p.first; fcy += p.second }
        fcx /= fPts.size; fcy /= fPts.size
        for (i in fPts.indices) {
            val j = (i + 1) % fPts.size
            addTriangle(
                fcx, fcy, front.first, 0.5f, 0.5f,
                fPts[j].first, fPts[j].second, front.first, 1f, 0f,
                fPts[i].first, fPts[i].second, front.first, 0f, 0f,
                r, g, b, matType
            )
        }
        val back = slices.last()
        val bPts = back.second
        var bcx = 0f; var bcy = 0f
        for (p in bPts) { bcx += p.first; bcy += p.second }
        bcx /= bPts.size; bcy /= bPts.size
        for (i in bPts.indices) {
            val j = (i + 1) % bPts.size
            addTriangle(
                bcx, bcy, back.first, 0.5f, 0.5f,
                bPts[i].first, bPts[i].second, back.first, 0f, 0f,
                bPts[j].first, bPts[j].second, back.first, 1f, 0f,
                r, g, b, matType
            )
        }
    }

    fun addCylinderY(
        cx: Float, cyBottom: Float, cz: Float,
        radiusBottom: Float, radiusTop: Float, height: Float,
        segments: Int,
        r: Float, g: Float, b: Float,
        matType: Float
    ) {
        val cyTop = cyBottom + height
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * PI.toFloat())
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            addQuad(
                cx + c0 * radiusBottom, cyBottom, cz + s0 * radiusBottom, 0f, 0f,
                cx + c1 * radiusBottom, cyBottom, cz + s1 * radiusBottom, 1f, 0f,
                cx + c1 * radiusTop, cyTop, cz + s1 * radiusTop, 1f, 1f,
                cx + c0 * radiusTop, cyTop, cz + s0 * radiusTop, 0f, 1f,
                r, g, b, matType
            )
            addTriangle(
                cx, cyTop, cz, 0.5f, 0.5f,
                cx + c0 * radiusTop, cyTop, cz + s0 * radiusTop, 0f, 0f,
                cx + c1 * radiusTop, cyTop, cz + s1 * radiusTop, 1f, 0f,
                r, g, b, matType
            )
        }
    }

    fun addWheelX(
        radius: Float,
        width: Float,
        segments: Int = 20,
        rimR: Float = 0.85f,
        rimG: Float = 0.87f,
        rimB: Float = 0.90f
    ) {
        val hw = width * 0.5f
        val rimRadius = radius * 0.68f
        // Tire tread
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * PI.toFloat())
            val y0 = cos(a0) * radius; val z0 = sin(a0) * radius
            val y1 = cos(a1) * radius; val z1 = sin(a1) * radius
            addQuad(
                -hw, y0, z0, 0f, 0f,
                hw, y0, z0, 1f, 0f,
                hw, y1, z1, 1f, 1f,
                -hw, y1, z1, 0f, 1f,
                0.08f, 0.08f, 0.09f, 0f
            )
            // Outer & inner sidewalls
            val iy0 = cos(a0) * rimRadius; val iz0 = sin(a0) * rimRadius
            val iy1 = cos(a1) * rimRadius; val iz1 = sin(a1) * rimRadius
            addQuad(
                hw, iy0, iz0, 0f, 0f,
                hw, y0, z0, 1f, 0f,
                hw, y1, z1, 1f, 1f,
                hw, iy1, iz1, 0f, 1f,
                0.10f, 0.10f, 0.12f, 0f
            )
            addQuad(
                -hw, y0, z0, 0f, 0f,
                -hw, iy0, iz0, 1f, 0f,
                -hw, iy1, iz1, 1f, 1f,
                -hw, y1, z1, 0f, 1f,
                0.10f, 0.10f, 0.12f, 0f
            )
            // Rim inner drum
            addTriangle(
                hw * 0.85f, 0f, 0f, 0.5f, 0.5f,
                hw * 0.85f, iy0, iz0, 0f, 0f,
                hw * 0.85f, iy1, iz1, 1f, 0f,
                0.18f, 0.20f, 0.24f, 3f
            )
            addTriangle(
                -hw * 0.85f, 0f, 0f, 0.5f, 0.5f,
                -hw * 0.85f, iy1, iz1, 1f, 0f,
                -hw * 0.85f, iy0, iz0, 0f, 0f,
                0.18f, 0.20f, 0.24f, 3f
            )
        }
        // 5 metallic alloy spokes on both faces so wheel rotation is super readable!
        for (s in 0 until 5) {
            val angle = s * (2f * PI.toFloat() / 5f)
            val cy = cos(angle) * (rimRadius * 0.52f)
            val cz = sin(angle) * (rimRadius * 0.52f)
            val ty = -sin(angle) * 0.032f
            val tz = cos(angle) * 0.032f
            val ey = cos(angle) * rimRadius * 0.96f
            val ez = sin(angle) * rimRadius * 0.96f
            for (side in floatArrayOf(hw * 0.92f, -hw * 0.92f)) {
                addQuad(
                    side, -ty, -tz, 0f, 0f,
                    side, ty, tz, 1f, 0f,
                    side, ey + ty, ez + tz, 1f, 1f,
                    side, ey - ty, ez - tz, 0f, 1f,
                    rimR, rimG, rimB, 3f
                )
            }
            // Center hub cap
            addBox(0f, cy * 0.05f, cz * 0.05f, width * 0.96f, 0.09f, 0.09f, rimR, rimG, rimB, 3f)
        }
    }

    fun addRamp(
        minX: Float, maxX: Float,
        zStart: Float, zEnd: Float,
        yStart: Float, yEnd: Float,
        r: Float, g: Float, b: Float,
        matType: Float,
        stripeR: Float = 0f, stripeG: Float = 0.9f, stripeB: Float = 1f
    ) {
        // Top sloped deck
        addQuad(
            minX, yStart, zStart, 0f, 0f,
            maxX, yStart, zStart, 1f, 0f,
            maxX, yEnd, zEnd, 1f, 1f,
            minX, yEnd, zEnd, 0f, 1f,
            r, g, b, matType
        )
        // Side cheeks
        addTriangle(minX, yStart, zStart, 0f, 0f, minX, yEnd, zEnd, 1f, 1f, minX, 0f, zEnd, 1f, 0f, 0.16f, 0.18f, 0.24f, 0f)
        addTriangle(maxX, yStart, zStart, 0f, 0f, maxX, 0f, zEnd, 1f, 0f, maxX, yEnd, zEnd, 1f, 1f, 0.16f, 0.18f, 0.24f, 0f)
        // Back vertical face
        addQuad(
            minX, 0f, zEnd, 0f, 0f,
            maxX, 0f, zEnd, 1f, 0f,
            maxX, yEnd, zEnd, 1f, 1f,
            minX, yEnd, zEnd, 0f, 1f,
            0.14f, 0.16f, 0.22f, 0f
        )
        // Glowing neon side rails
        addQuad(
            minX, yStart + 0.06f, zStart, 0f, 0f,
            minX + 0.35f, yStart + 0.06f, zStart, 1f, 0f,
            minX + 0.35f, yEnd + 0.06f, zEnd, 1f, 1f,
            minX, yEnd + 0.06f, zEnd, 0f, 1f,
            stripeR, stripeG, stripeB, 4f
        )
        addQuad(
            maxX - 0.35f, yStart + 0.06f, zStart, 0f, 0f,
            maxX, yStart + 0.06f, zStart, 1f, 0f,
            maxX, yEnd + 0.06f, zEnd, 1f, 1f,
            maxX - 0.35f, yEnd + 0.06f, zEnd, 0f, 1f,
            stripeR, stripeG, stripeB, 4f
        )
    }

    fun build(): GpuMesh {
        val vertexCount = count / 12
        val byteBuf = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder())
        val floatBuf: FloatBuffer = byteBuf.asFloatBuffer()
        floatBuf.put(data, 0, count)
        floatBuf.position(0)

        val vao = IntArray(1)
        val vbo = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)

        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, count * 4, floatBuf, GLES30.GL_STATIC_DRAW)

        val strideBytes = 12 * 4
        // aPos
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
        // aNormal
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)
        // aUv
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, strideBytes, 6 * 4)
        // aColor
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 3, GLES30.GL_FLOAT, false, strideBytes, 8 * 4)
        // aMatType
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, strideBytes, 11 * 4)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)

        return GpuMesh(vao[0], vbo[0], vertexCount)
    }

    /**
     * Builds an InstancedGpuMesh where each instance is 8 floats:
     * (x, y, z, scale, yawRad, tintR, tintG, tintB)
     */
    fun buildInstanced(instanceFloats: FloatArray): InstancedGpuMesh {
        val vertexCount = count / 12
        val instanceCount = instanceFloats.size / 8

        val byteBuf = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder())
        val floatBuf = byteBuf.asFloatBuffer().apply {
            put(data, 0, count)
            position(0)
        }

        val instBuf = ByteBuffer.allocateDirect(instanceFloats.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(instanceFloats)
                position(0)
            }

        val vao = IntArray(1)
        val vbos = IntArray(2)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(2, vbos, 0)

        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, count * 4, floatBuf, GLES30.GL_STATIC_DRAW)

        val strideBytes = 12 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, strideBytes, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 3, GLES30.GL_FLOAT, false, strideBytes, 8 * 4)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, strideBytes, 11 * 4)

        // Instance buffer
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[1])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, instanceFloats.size * 4, instBuf, GLES30.GL_STATIC_DRAW)
        val instStride = 8 * 4
        GLES30.glEnableVertexAttribArray(5)
        GLES30.glVertexAttribPointer(5, 4, GLES30.GL_FLOAT, false, instStride, 0)
        GLES30.glVertexAttribDivisor(5, 1)
        GLES30.glEnableVertexAttribArray(6)
        GLES30.glVertexAttribPointer(6, 4, GLES30.GL_FLOAT, false, instStride, 4 * 4)
        GLES30.glVertexAttribDivisor(6, 1)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)

        return InstancedGpuMesh(vao[0], vbos[0], vbos[1], vertexCount, instanceCount)
    }
}
