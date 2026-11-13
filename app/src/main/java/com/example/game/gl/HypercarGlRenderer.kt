package com.example.game.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.CarAudioEngine
import com.example.game.CarType
import com.example.game.GameWorldPhysics
import com.example.game.HudTelemetry
import com.example.game.LUXURY_PAINT_PRESETS
import com.example.game.SharedGameInput
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class HypercarGlRenderer(
    private val sharedInput: SharedGameInput,
    private val audioEngine: CarAudioEngine,
    private val onTelemetryUpdate: (HudTelemetry) -> Unit
) : GLSurfaceView.Renderer {

    private val physics = GameWorldPhysics(audioEngine)
    private val commandQueue = ConcurrentLinkedQueue<() -> Unit>()

    private var lastFrameNanos = 0L
    private var hudPostAccum = 0f
    private var viewWidth = 1280
    private var viewHeight = 720

    // Shader programs
    private var shadowProg = 0
    private var instancedShadowProg = 0
    private var skyProg = 0
    private var oceanProg = 0
    private var worldProg = 0
    private var instancedWorldProg = 0
    private var particleProg = 0

    // Cached uniform handles
    private class WorldUniforms(prog: Int) {
        val uShadowMap = GLES30.glGetUniformLocation(prog, "uShadowMap")
        val uAsphaltTex = GLES30.glGetUniformLocation(prog, "uAsphaltTex")
        val uSkyscraperTex = GLES30.glGetUniformLocation(prog, "uSkyscraperTex")
        val uCurbTex = GLES30.glGetUniformLocation(prog, "uCurbTex")
        val uViewProj = GLES30.glGetUniformLocation(prog, "uViewProj")
        val uCamPos = GLES30.glGetUniformLocation(prog, "uCamPos")
        val uSunDir = GLES30.glGetUniformLocation(prog, "uSunDir")
        val uTime = GLES30.glGetUniformLocation(prog, "uTime")
        val uPaintColor = GLES30.glGetUniformLocation(prog, "uPaintColor")
        val uPaintRoughness = GLES30.glGetUniformLocation(prog, "uPaintRoughness")
        val uPaintMetalness = GLES30.glGetUniformLocation(prog, "uPaintMetalness")
        val uPaintClearcoat = GLES30.glGetUniformLocation(prog, "uPaintClearcoat")
        val uPaintClearcoatRoughness = GLES30.glGetUniformLocation(prog, "uPaintClearcoatRoughness")
        val uBrakeGlow = GLES30.glGetUniformLocation(prog, "uBrakeGlow")
        val uReverseGlow = GLES30.glGetUniformLocation(prog, "uReverseGlow")
        val uAccentColor = GLES30.glGetUniformLocation(prog, "uAccentColor")
        val uCaliperColor = GLES30.glGetUniformLocation(prog, "uCaliperColor")
        val uModel = GLES30.glGetUniformLocation(prog, "uModel")
        val uLightMvp = GLES30.glGetUniformLocation(prog, "uLightMvp")
        val uLightVp = GLES30.glGetUniformLocation(prog, "uLightVp")
    }

    private lateinit var worldU: WorldUniforms
    private lateinit var instWorldU: WorldUniforms
    private var uShadowLightMvp = -1
    private var uInstShadowLightVp = -1
    private var uSkyViewProj = -1
    private var uSkySunDir = -1
    private var uOceanWaterTex = -1
    private var uOceanViewProj = -1
    private var uOceanCamPos = -1
    private var uOceanSunDir = -1
    private var uOceanTime = -1
    private var uParticleViewProj = -1

    // Shadow map FBO (1024x1024 fast soft PCF)
    private var shadowFbo = 0
    private var shadowDepthTex = 0
    private val shadowRes = 1024

    // Procedural Textures & Meshes
    private lateinit var textures: ProceduralTextures
    private lateinit var worldMeshes: WorldMeshes
    private lateinit var carMeshes: Map<CarType, CarMeshSet>

    // Dynamic particle/skid/halo buffer: pos(3) + rgba(4) + uv(2) = 9 floats
    private val maxDynFloats = 4096 * 9
    private val dynData = FloatArray(maxDynFloats)
    private val dynFloatBuf: FloatBuffer = ByteBuffer.allocateDirect(maxDynFloats * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private var dynVao = 0
    private var dynVbo = 0

    // Matrices
    private val projMat = FloatArray(16)
    private val viewMat = FloatArray(16)
    private val viewProjMat = FloatArray(16)
    private val skyViewMat = FloatArray(16)
    private val skyViewProjMat = FloatArray(16)
    private val lightProjMat = FloatArray(16)
    private val lightViewMat = FloatArray(16)
    private val lightVpMat = FloatArray(16)
    private val lightMvpMat = FloatArray(16)
    private val modelMat = FloatArray(16)
    private val wheelMat = FloatArray(16)

    // Golden-hour sunset sun direction
    private val sunDir = floatArrayOf(-0.62f, 0.34f, 0.70f)

    fun postAction(action: (GameWorldPhysics) -> Unit) {
        commandQueue.offer { action(physics) }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        shadowProg = Shaders.compileProgram(Shaders.SHADOW_VS, Shaders.SHADOW_FS)
        instancedShadowProg = Shaders.compileProgram(Shaders.INSTANCED_SHADOW_VS, Shaders.SHADOW_FS)
        skyProg = Shaders.compileProgram(Shaders.SKY_VS, Shaders.SKY_FS)
        oceanProg = Shaders.compileProgram(Shaders.OCEAN_VS, Shaders.OCEAN_FS)
        worldProg = Shaders.compileProgram(Shaders.WORLD_VS, Shaders.WORLD_FS)
        instancedWorldProg = Shaders.compileProgram(Shaders.INSTANCED_WORLD_VS, Shaders.WORLD_FS)
        particleProg = Shaders.compileProgram(Shaders.PARTICLE_VS, Shaders.PARTICLE_FS)

        worldU = WorldUniforms(worldProg)
        instWorldU = WorldUniforms(instancedWorldProg)
        uShadowLightMvp = GLES30.glGetUniformLocation(shadowProg, "uLightMvp")
        uInstShadowLightVp = GLES30.glGetUniformLocation(instancedShadowProg, "uLightVp")
        uSkyViewProj = GLES30.glGetUniformLocation(skyProg, "uViewProj")
        uSkySunDir = GLES30.glGetUniformLocation(skyProg, "uSunDir")
        uOceanWaterTex = GLES30.glGetUniformLocation(oceanProg, "uWaterTex")
        uOceanViewProj = GLES30.glGetUniformLocation(oceanProg, "uViewProj")
        uOceanCamPos = GLES30.glGetUniformLocation(oceanProg, "uCamPos")
        uOceanSunDir = GLES30.glGetUniformLocation(oceanProg, "uSunDir")
        uOceanTime = GLES30.glGetUniformLocation(oceanProg, "uTime")
        uParticleViewProj = GLES30.glGetUniformLocation(particleProg, "uViewProj")

        initShadowFbo()
        initDynamicBuffer()

        textures = ProceduralTextures.createAll()
        worldMeshes = WorldMeshLibrary.buildWorld()
        carMeshes = CarMeshLibrary.buildAllCars()

        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width.coerceAtLeast(1)
        viewHeight = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.045f)
        lastFrameNanos = now

        while (true) {
            val cmd = commandQueue.poll() ?: break
            cmd()
        }

        val input = sharedInput.combined()
        val telemetry = physics.step(dt, input)

        hudPostAccum += dt
        if (hudPostAccum >= 0.025f) {
            hudPostAccum = 0f
            onTelemetryUpdate(telemetry)
        }

        renderShadowPass()

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
        GLES30.glClearColor(0.10f, 0.12f, 0.22f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val aspect = viewWidth.toFloat() / viewHeight.toFloat()
        Matrix.perspectiveM(projMat, 0, physics.cameraFov, aspect, 0.35f, 850f)
        Matrix.setLookAtM(
            viewMat, 0,
            physics.camPosX, physics.camPosY, physics.camPosZ,
            physics.camLookX, physics.camLookY, physics.camLookZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjMat, 0, projMat, 0, viewMat, 0)

        renderSkyDome()
        renderOcean()
        renderWorldAndCar(
            braking = input.brake || input.handbrake,
            reversing = telemetry.isReversing
        )
        renderSkidsSmokeAndHalos(braking = input.brake || input.handbrake)
    }

    private fun renderShadowPass() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glViewport(0, 0, shadowRes, shadowRes)
        GLES30.glColorMask(false, false, false, false)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)

        val cx = physics.carX
        val cz = physics.carZ
        Matrix.orthoM(lightProjMat, 0, -90f, 90f, -90f, 90f, 10f, 250f)
        Matrix.setLookAtM(
            lightViewMat, 0,
            cx + sunDir[0] * 120f, sunDir[1] * 120f + 20f, cz + sunDir[2] * 120f,
            cx, 0f, cz,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(lightVpMat, 0, lightProjMat, 0, lightViewMat, 0)

        GLES30.glUseProgram(shadowProg)
        Matrix.setIdentityM(modelMat, 0)
        Matrix.multiplyMM(lightMvpMat, 0, lightVpMat, 0, modelMat, 0)
        GLES30.glUniformMatrix4fv(uShadowLightMvp, 1, false, lightMvpMat, 0)
        worldMeshes.staticWorldMesh.draw()

        buildCarBodyMatrix(modelMat)
        Matrix.multiplyMM(lightMvpMat, 0, lightVpMat, 0, modelMat, 0)
        GLES30.glUniformMatrix4fv(uShadowLightMvp, 1, false, lightMvpMat, 0)
        carMeshes[physics.carType]?.bodyMesh?.draw()

        GLES30.glUseProgram(instancedShadowProg)
        GLES30.glUniformMatrix4fv(uInstShadowLightVp, 1, false, lightVpMat, 0)
        worldMeshes.palmInstanced.drawInstanced()
        worldMeshes.lampInstanced.drawInstanced()
        worldMeshes.barrierInstanced.drawInstanced()

        GLES30.glColorMask(true, true, true, true)
    }

    private fun renderSkyDome() {
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glUseProgram(skyProg)

        System.arraycopy(viewMat, 0, skyViewMat, 0, 16)
        skyViewMat[12] = 0f; skyViewMat[13] = 0f; skyViewMat[14] = 0f
        Matrix.multiplyMM(skyViewProjMat, 0, projMat, 0, skyViewMat, 0)

        GLES30.glUniformMatrix4fv(uSkyViewProj, 1, false, skyViewProjMat, 0)
        GLES30.glUniform3fv(uSkySunDir, 1, sunDir, 0)
        worldMeshes.skyDomeMesh.draw()

        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glDepthFunc(GLES30.GL_LESS)
    }

    private fun renderOcean() {
        GLES30.glUseProgram(oceanProg)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.waterTexId)
        GLES30.glUniform1i(uOceanWaterTex, 0)

        GLES30.glUniformMatrix4fv(uOceanViewProj, 1, false, viewProjMat, 0)
        GLES30.glUniform3f(uOceanCamPos, physics.camPosX, physics.camPosY, physics.camPosZ)
        GLES30.glUniform3fv(uOceanSunDir, 1, sunDir, 0)
        GLES30.glUniform1f(uOceanTime, physics.worldTime)
        worldMeshes.oceanMesh.draw()
    }

    private fun bindWorldCommonUniforms(prog: Int, u: WorldUniforms, braking: Boolean, reversing: Boolean) {
        GLES30.glUseProgram(prog)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowDepthTex)
        GLES30.glUniform1i(u.uShadowMap, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.asphaltTexId)
        GLES30.glUniform1i(u.uAsphaltTex, 1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.skyscraperTexId)
        GLES30.glUniform1i(u.uSkyscraperTex, 2)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE3)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.curbTexId)
        GLES30.glUniform1i(u.uCurbTex, 3)

        GLES30.glUniformMatrix4fv(u.uViewProj, 1, false, viewProjMat, 0)
        GLES30.glUniform3f(u.uCamPos, physics.camPosX, physics.camPosY, physics.camPosZ)
        GLES30.glUniform3fv(u.uSunDir, 1, sunDir, 0)
        GLES30.glUniform1f(u.uTime, physics.worldTime)

        val paint = LUXURY_PAINT_PRESETS[physics.paintIndex.coerceIn(LUXURY_PAINT_PRESETS.indices)]
        val finish = physics.paintFinish
        GLES30.glUniform3f(u.uPaintColor, paint.r, paint.g, paint.b)
        GLES30.glUniform1f(u.uPaintRoughness, finish.roughness)
        GLES30.glUniform1f(u.uPaintMetalness, finish.metalness)
        GLES30.glUniform1f(u.uPaintClearcoat, finish.clearcoat)
        GLES30.glUniform1f(u.uPaintClearcoatRoughness, finish.clearcoatRoughness)
        GLES30.glUniform1f(u.uBrakeGlow, if (braking) 1f else 0f)
        GLES30.glUniform1f(u.uReverseGlow, if (reversing) 1f else 0f)
        GLES30.glUniform3f(u.uAccentColor, physics.carType.accentR, physics.carType.accentG, physics.carType.accentB)
        GLES30.glUniform3f(u.uCaliperColor, physics.carType.caliperR, physics.carType.caliperG, physics.carType.caliperB)
    }

    private fun renderWorldAndCar(braking: Boolean, reversing: Boolean) {
        bindWorldCommonUniforms(worldProg, worldU, braking, reversing)
        GLES30.glUniformMatrix4fv(worldU.uLightMvp, 1, false, lightVpMat, 0)

        Matrix.setIdentityM(modelMat, 0)
        GLES30.glUniformMatrix4fv(worldU.uModel, 1, false, modelMat, 0)
        worldMeshes.staticWorldMesh.draw()

        val carSet = carMeshes[physics.carType] ?: return

        buildCarBodyMatrix(modelMat)
        GLES30.glUniformMatrix4fv(worldU.uModel, 1, false, modelMat, 0)
        carSet.bodyMesh.draw()

        if (physics.carType.hasBackfireFlames && (physics.backfireTimer > 0f || physics.boostTimer > 0f)) {
            GLES30.glDisable(GLES30.GL_CULL_FACE)
            carSet.flameMesh?.draw()
            GLES30.glEnable(GLES30.GL_CULL_FACE)
        }

        val wheelOffsets = arrayOf(
            floatArrayOf(0.97f, 0.36f, 1.44f, 1f),
            floatArrayOf(-0.97f, 0.36f, 1.44f, 1f),
            floatArrayOf(0.99f, 0.36f, -1.38f, 0f),
            floatArrayOf(-0.99f, 0.36f, -1.38f, 0f)
        )
        val spinDeg = Math.toDegrees(physics.wheelSpinAngle.toDouble()).toFloat()
        val steerDeg = Math.toDegrees(physics.frontSteerAngle.toDouble()).toFloat()
        val yawDeg = Math.toDegrees(physics.yaw.toDouble()).toFloat()

        for (wo in wheelOffsets) {
            Matrix.setIdentityM(wheelMat, 0)
            Matrix.translateM(wheelMat, 0, physics.carX, physics.carY, physics.carZ)
            Matrix.rotateM(wheelMat, 0, yawDeg, 0f, 1f, 0f)
            Matrix.translateM(wheelMat, 0, wo[0], wo[1], wo[2])
            if (wo[3] > 0.5f) {
                Matrix.rotateM(wheelMat, 0, steerDeg, 0f, 1f, 0f)
            }
            GLES30.glUniformMatrix4fv(worldU.uModel, 1, false, wheelMat, 0)
            carSet.caliperMesh.draw()

            Matrix.rotateM(wheelMat, 0, spinDeg, 1f, 0f, 0f)
            GLES30.glUniformMatrix4fv(worldU.uModel, 1, false, wheelMat, 0)
            carSet.wheelMesh.draw()
        }

        bindWorldCommonUniforms(instancedWorldProg, instWorldU, braking, reversing)
        GLES30.glUniformMatrix4fv(instWorldU.uLightVp, 1, false, lightVpMat, 0)
        worldMeshes.palmInstanced.drawInstanced()
        worldMeshes.lampInstanced.drawInstanced()
        worldMeshes.barrierInstanced.drawInstanced()
    }

    private fun buildCarBodyMatrix(out: FloatArray) {
        Matrix.setIdentityM(out, 0)
        Matrix.translateM(out, 0, physics.carX, physics.carY + physics.suspensionY, physics.carZ)
        val yawDeg = Math.toDegrees(physics.yaw.toDouble()).toFloat()
        val pitchDeg = Math.toDegrees(physics.chassisPitch.toDouble()).toFloat()
        val rollDeg = Math.toDegrees(physics.chassisRoll.toDouble()).toFloat()
        Matrix.rotateM(out, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(out, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.rotateM(out, 0, -rollDeg, 0f, 0f, 1f)
    }

    private fun renderSkidsSmokeAndHalos(braking: Boolean) {
        val camDx = physics.camPosX - physics.carX
        val camDz = physics.camPosZ - physics.carZ
        val camLen = hypot(camDx, camDz).coerceAtLeast(0.001f)
        val rightX = -camDz / camLen
        val rightZ = camDx / camLen

        var floatCount = 0
        for (s in physics.skidSegments) {
            if (!s.active) continue
            if (floatCount + 6 * 9 > maxDynFloats) break
            val dx = s.x2 - s.x1
            val dz = s.z2 - s.z1
            val len = hypot(dx, dz).coerceAtLeast(0.001f)
            val nx = (-dz / len) * 0.17f
            val nz = (dx / len) * 0.17f
            val alpha = (s.life * 0.58f).coerceIn(0f, 0.62f)
            floatCount = pushQuad(
                floatCount,
                s.x1 - nx, s.y, s.z1 - nz,
                s.x1 + nx, s.y, s.z1 + nz,
                s.x2 + nx, s.y, s.z2 + nz,
                s.x2 - nx, s.y, s.z2 - nz,
                0.04f, 0.04f, 0.05f, alpha,
                solid = true
            )
        }

        for (p in physics.smokeParticles) {
            if (!p.active) continue
            if (floatCount + 6 * 9 > maxDynFloats) break
            val t = (p.age / p.maxAge).coerceIn(0f, 1f)
            val alpha = (1f - t) * 0.42f
            val hs = p.size * 0.5f
            floatCount = pushQuad(
                floatCount,
                p.x - rightX * hs, p.y - hs * 0.65f, p.z - rightZ * hs,
                p.x + rightX * hs, p.y - hs * 0.65f, p.z + rightZ * hs,
                p.x + rightX * hs, p.y + hs * 1.15f, p.z + rightZ * hs,
                p.x - rightX * hs, p.y + hs * 1.15f, p.z - rightZ * hs,
                0.92f, 0.88f, 0.85f, alpha,
                solid = false
            )
        }

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glUseProgram(particleProg)
        GLES30.glUniformMatrix4fv(uParticleViewProj, 1, false, viewProjMat, 0)

        if (floatCount > 0) {
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            drawDynamicBatch(floatCount)
        }

        var haloCount = 0
        val fwdX = sin(physics.yaw)
        val fwdZ = cos(physics.yaw)
        val carRightX = cos(physics.yaw)
        val carRightZ = -sin(physics.yaw)
        val bodyY = physics.carY + physics.suspensionY

        for (side in floatArrayOf(-0.68f, 0.68f)) {
            val hx = physics.carX + fwdX * 2.34f + carRightX * side
            val hy = bodyY + 0.52f
            val hz = physics.carZ + fwdZ * 2.34f + carRightZ * side
            val hs = 0.55f
            haloCount = pushQuad(
                haloCount,
                hx - rightX * hs, hy - hs, hz - rightZ * hs,
                hx + rightX * hs, hy - hs, hz + rightZ * hs,
                hx + rightX * hs, hy + hs, hz + rightZ * hs,
                hx - rightX * hs, hy + hs, hz - rightZ * hs,
                0.78f, 0.94f, 1.0f, 0.34f,
                solid = false
            )
        }
        val tailAlpha = if (braking) 0.55f else 0.22f
        val tailSize = if (braking) 0.72f else 0.45f
        for (side in floatArrayOf(-0.60f, 0.60f)) {
            val tx = physics.carX - fwdX * 2.38f + carRightX * side
            val ty = bodyY + 0.68f
            val tz = physics.carZ - fwdZ * 2.38f + carRightZ * side
            haloCount = pushQuad(
                haloCount,
                tx - rightX * tailSize, ty - tailSize, tz - rightZ * tailSize,
                tx + rightX * tailSize, ty - tailSize, tz + rightZ * tailSize,
                tx + rightX * tailSize, ty + tailSize, tz + rightZ * tailSize,
                tx - rightX * tailSize, ty + tailSize, tz - rightZ * tailSize,
                1.0f, 0.14f, 0.16f, tailAlpha,
                solid = false
            )
        }

        if (haloCount > 0) {
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
            drawDynamicBatch(haloCount)
        }

        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun drawDynamicBatch(floatCount: Int) {
        dynFloatBuf.clear()
        dynFloatBuf.put(dynData, 0, floatCount)
        dynFloatBuf.position(0)
        GLES30.glBindVertexArray(dynVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, dynVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, floatCount * 4, dynFloatBuf)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, floatCount / 9)
        GLES30.glBindVertexArray(0)
    }

    private fun pushQuad(
        offset: Int,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        r: Float, g: Float, b: Float, a: Float,
        solid: Boolean
    ): Int {
        val u0 = if (solid) -1f else 0f
        val u1 = if (solid) -1f else 1f
        val v0 = if (solid) -1f else 0f
        val v1 = if (solid) -1f else 1f
        var o = offset
        o = pushVert(o, x1, y1, z1, r, g, b, a, u0, v0)
        o = pushVert(o, x2, y2, z2, r, g, b, a, u1, v0)
        o = pushVert(o, x3, y3, z3, r, g, b, a, u1, v1)
        o = pushVert(o, x1, y1, z1, r, g, b, a, u0, v0)
        o = pushVert(o, x3, y3, z3, r, g, b, a, u1, v1)
        o = pushVert(o, x4, y4, z4, r, g, b, a, u0, v1)
        return o
    }

    private fun pushVert(
        o: Int, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float, u: Float, v: Float
    ): Int {
        dynData[o] = x; dynData[o + 1] = y; dynData[o + 2] = z
        dynData[o + 3] = r; dynData[o + 4] = g; dynData[o + 5] = b; dynData[o + 6] = a
        dynData[o + 7] = u; dynData[o + 8] = v
        return o + 9
    }

    private fun initShadowFbo() {
        val fbos = IntArray(1)
        val texs = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        GLES30.glGenTextures(1, texs, 0)
        shadowFbo = fbos[0]
        shadowDepthTex = texs[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowDepthTex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
            shadowRes, shadowRes, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, shadowDepthTex, 0
        )
        GLES30.glDrawBuffers(1, intArrayOf(GLES30.GL_NONE), 0)
        GLES30.glReadBuffer(GLES30.GL_NONE)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun initDynamicBuffer() {
        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        dynVao = vaos[0]
        dynVbo = vbos[0]

        GLES30.glBindVertexArray(dynVao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, dynVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, maxDynFloats * 4, null, GLES30.GL_DYNAMIC_DRAW)

        val stride = 9 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 7 * 4)

        GLES30.glBindVertexArray(0)
    }
}
