package com.example.game.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.CarAudioEngine
import com.example.game.CarType
import com.example.game.GameState
import com.example.game.GameWorldPhysics
import com.example.game.HudTelemetry
import com.example.game.LUXURY_PAINT_PRESETS
import com.example.game.SharedGameInput
import com.example.game.TimeOfDay
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

    private val physics = GameWorldPhysics(audioEngine, initialState = GameState.SPLASH)
    private val commandQueue = ConcurrentLinkedQueue<() -> Unit>()

    private var lastFrameNanos = 0L
    private var hudPostAccum = 0f
    private var fpsFrameCount = 0
    private var fpsTimerAccum = 0f
    private var viewWidth = 1280
    private var viewHeight = 720

    private var shadowProg = 0
    private var instancedShadowProg = 0
    private var skyProg = 0
    private var oceanProg = 0
    private var worldProg = 0
    private var instancedWorldProg = 0
    private var particleProg = 0

    private class WorldUniforms(prog: Int) {
        val uShadowMap = GLES30.glGetUniformLocation(prog, "uShadowMap")
        val uAsphaltTex = GLES30.glGetUniformLocation(prog, "uAsphaltTex")
        val uSkyscraperTex = GLES30.glGetUniformLocation(prog, "uSkyscraperTex")
        val uCurbTex = GLES30.glGetUniformLocation(prog, "uCurbTex")
        val uViewProj = GLES30.glGetUniformLocation(prog, "uViewProj")
        val uCamPos = GLES30.glGetUniformLocation(prog, "uCamPos")
        val uSunDir = GLES30.glGetUniformLocation(prog, "uSunDir")
        val uTime = GLES30.glGetUniformLocation(prog, "uTime")
        val uSkyHorizon = GLES30.glGetUniformLocation(prog, "uSkyHorizon")
        val uSkyZenith = GLES30.glGetUniformLocation(prog, "uSkyZenith")
        val uSunCol = GLES30.glGetUniformLocation(prog, "uSunCol")
        val uFogColor = GLES30.glGetUniformLocation(prog, "uFogColor")
        val uFogDensity = GLES30.glGetUniformLocation(prog, "uFogDensity")
        val uShadowEnabled = GLES30.glGetUniformLocation(prog, "uShadowEnabled")
        val uShadowTexel = GLES30.glGetUniformLocation(prog, "uShadowTexel")
        val uReflectionsEnabled = GLES30.glGetUniformLocation(prog, "uReflectionsEnabled")
        val uAmbientScale = GLES30.glGetUniformLocation(prog, "uAmbientScale")
        val uNightFactor = GLES30.glGetUniformLocation(prog, "uNightFactor")
        val uCarPos = GLES30.glGetUniformLocation(prog, "uCarPos")
        val uCarFwd = GLES30.glGetUniformLocation(prog, "uCarFwd")
        val uHeadlightOn = GLES30.glGetUniformLocation(prog, "uHeadlightOn")
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
    private var uSkyHorizon = -1
    private var uSkyMid = -1
    private var uSkyZenith = -1
    private var uSkySunCol = -1
    private var uSkyNight = -1
    private var uOceanWaterTex = -1
    private var uOceanViewProj = -1
    private var uOceanCamPos = -1
    private var uOceanSunDir = -1
    private var uOceanSkyHorizon = -1
    private var uOceanSkyZenith = -1
    private var uOceanSunCol = -1
    private var uOceanFogCol = -1
    private var uOceanFogDensity = -1
    private var uOceanNight = -1
    private var uOceanTime = -1
    private var uParticleViewProj = -1

    private var shadowFbo = 0
    private var shadowDepthTex = 0
    private var currentShadowRes = 1024

    private var sceneFbo = 0
    private var sceneColorTex = 0
    private var sceneDepthRbo = 0
    private var sceneW = 0
    private var sceneH = 0

    private lateinit var textures: ProceduralTextures
    private lateinit var worldMeshes: WorldMeshes
    private lateinit var carMeshes: Map<CarType, CarMeshSet>

    private val maxDynFloats = 4096 * 9
    private val dynData = FloatArray(maxDynFloats)
    private val dynFloatBuf: FloatBuffer = ByteBuffer.allocateDirect(maxDynFloats * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private var dynVao = 0
    private var dynVbo = 0

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

    // Lighting parameters resolved each frame from TimeOfDay
    private val sunDir = floatArrayOf(-0.62f, 0.34f, 0.70f)
    private val skyHorizon = floatArrayOf(1.0f, 0.54f, 0.24f)
    private val skyMid = floatArrayOf(0.46f, 0.23f, 0.42f)
    private val skyZenith = floatArrayOf(0.05f, 0.09f, 0.24f)
    private val sunCol = floatArrayOf(1.0f, 0.78f, 0.50f)
    private val fogCol = floatArrayOf(0.95f, 0.56f, 0.34f)
    private var ambientScale = 1.0f
    private var nightFactor = 0.0f

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
        uSkyHorizon = GLES30.glGetUniformLocation(skyProg, "uSkyHorizon")
        uSkyMid = GLES30.glGetUniformLocation(skyProg, "uSkyMid")
        uSkyZenith = GLES30.glGetUniformLocation(skyProg, "uSkyZenith")
        uSkySunCol = GLES30.glGetUniformLocation(skyProg, "uSunCol")
        uSkyNight = GLES30.glGetUniformLocation(skyProg, "uNightFactor")

        uOceanWaterTex = GLES30.glGetUniformLocation(oceanProg, "uWaterTex")
        uOceanViewProj = GLES30.glGetUniformLocation(oceanProg, "uViewProj")
        uOceanCamPos = GLES30.glGetUniformLocation(oceanProg, "uCamPos")
        uOceanSunDir = GLES30.glGetUniformLocation(oceanProg, "uSunDir")
        uOceanSkyHorizon = GLES30.glGetUniformLocation(oceanProg, "uSkyHorizon")
        uOceanSkyZenith = GLES30.glGetUniformLocation(oceanProg, "uSkyZenith")
        uOceanSunCol = GLES30.glGetUniformLocation(oceanProg, "uSunCol")
        uOceanFogCol = GLES30.glGetUniformLocation(oceanProg, "uFogColor")
        uOceanFogDensity = GLES30.glGetUniformLocation(oceanProg, "uFogDensity")
        uOceanNight = GLES30.glGetUniformLocation(oceanProg, "uNightFactor")
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
        sceneW = 0
        sceneH = 0
        GLES30.glViewport(0, 0, viewWidth, viewHeight)
    }

    private fun updateAtmosphereUniforms() {
        when (physics.timeOfDay) {
            TimeOfDay.SUNSET -> {
                sunDir[0] = -0.62f; sunDir[1] = 0.34f; sunDir[2] = 0.70f
                skyHorizon[0] = 1.0f; skyHorizon[1] = 0.54f; skyHorizon[2] = 0.24f
                skyMid[0] = 0.46f; skyMid[1] = 0.23f; skyMid[2] = 0.42f
                skyZenith[0] = 0.05f; skyZenith[1] = 0.09f; skyZenith[2] = 0.24f
                sunCol[0] = 1.45f; sunCol[1] = 1.13f; sunCol[2] = 0.72f
                fogCol[0] = 0.95f; fogCol[1] = 0.56f; fogCol[2] = 0.34f
                ambientScale = 1.0f
                nightFactor = 0.0f
            }
            TimeOfDay.DAY -> {
                sunDir[0] = -0.35f; sunDir[1] = 0.82f; sunDir[2] = 0.45f
                skyHorizon[0] = 0.68f; skyHorizon[1] = 0.86f; skyHorizon[2] = 0.98f
                skyMid[0] = 0.32f; skyMid[1] = 0.64f; skyMid[2] = 0.94f
                skyZenith[0] = 0.12f; skyZenith[1] = 0.38f; skyZenith[2] = 0.78f
                sunCol[0] = 1.55f; sunCol[1] = 1.50f; sunCol[2] = 1.40f
                fogCol[0] = 0.72f; fogCol[1] = 0.86f; fogCol[2] = 0.96f
                ambientScale = 1.30f
                nightFactor = 0.0f
            }
            TimeOfDay.NIGHT -> {
                sunDir[0] = 0.40f; sunDir[1] = 0.52f; sunDir[2] = -0.75f
                skyHorizon[0] = 0.06f; skyHorizon[1] = 0.08f; skyHorizon[2] = 0.19f
                skyMid[0] = 0.03f; skyMid[1] = 0.05f; skyMid[2] = 0.13f
                skyZenith[0] = 0.01f; skyZenith[1] = 0.02f; skyZenith[2] = 0.06f
                sunCol[0] = 0.35f; sunCol[1] = 0.45f; sunCol[2] = 0.72f
                fogCol[0] = 0.04f; fogCol[1] = 0.06f; fogCol[2] = 0.15f
                ambientScale = 0.52f
                nightFactor = 1.0f
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val rawDt = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.0005f, 0.25f)
        val dt = rawDt.coerceIn(0.001f, 0.045f)
        lastFrameNanos = now

        fpsFrameCount++
        fpsTimerAccum += rawDt
        if (fpsTimerAccum >= 0.5f) {
            physics.fps = (fpsFrameCount / fpsTimerAccum).toInt().coerceIn(1, 240)
            fpsFrameCount = 0
            fpsTimerAccum = 0f
        }

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

        updateAtmosphereUniforms()
        val gfx = physics.settings.graphicsPreset
        if (gfx.shadowsEnabled && physics.timeOfDay != TimeOfDay.NIGHT) {
            ensureShadowResolution(gfx.shadowRes)
            renderShadowPass()
        }

        val resPreset = physics.settings.resolutionPreset
        val useOffscreen = !resPreset.useNativeBuffer
        if (useOffscreen) {
            val targetW = (viewWidth * resPreset.scale).toInt().coerceAtLeast(320)
            val targetH = (viewHeight * resPreset.scale).toInt().coerceAtLeast(180)
            ensureSceneFbo(targetW, targetH)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, sceneFbo)
            GLES30.glViewport(0, 0, sceneW, sceneH)
        } else {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glViewport(0, 0, viewWidth, viewHeight)
        }

        GLES30.glClearColor(fogCol[0], fogCol[1], fogCol[2], 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val aspect = viewWidth.toFloat() / viewHeight.toFloat()
        val farPlane = gfx.drawDistance.coerceAtLeast(1200f)
        Matrix.perspectiveM(projMat, 0, physics.cameraFov, aspect, 0.35f, farPlane)
        Matrix.setLookAtM(
            viewMat, 0,
            physics.camPosX, physics.camPosY, physics.camPosZ,
            physics.camLookX, physics.camLookY, physics.camLookZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProjMat, 0, projMat, 0, viewMat, 0)

        val brakingNow = physics.gameState == GameState.PLAYING && (input.brake || input.handbrake)
        renderSkyDome()
        renderOcean()
        renderWorldAndCar(
            braking = brakingNow,
            reversing = telemetry.isReversing
        )
        renderDynamicEffects(braking = brakingNow)

        if (useOffscreen && sceneFbo != 0) {
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, sceneFbo)
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, 0)
            GLES30.glBlitFramebuffer(
                0, 0, sceneW, sceneH,
                0, 0, viewWidth, viewHeight,
                GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_LINEAR
            )
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }
    }

    private fun renderShadowPass() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFbo)
        GLES30.glViewport(0, 0, currentShadowRes, currentShadowRes)
        GLES30.glColorMask(false, false, false, false)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)

        val cx = physics.carX
        val cz = physics.carZ
        Matrix.orthoM(lightProjMat, 0, -110f, 110f, -110f, 110f, 10f, 300f)
        Matrix.setLookAtM(
            lightViewMat, 0,
            cx + sunDir[0] * 140f, sunDir[1] * 140f + 25f, cz + sunDir[2] * 140f,
            cx, physics.carY, cz,
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
        worldMeshes.buildingInstanced.drawInstanced()
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
        GLES30.glUniform3fv(uSkyHorizon, 1, skyHorizon, 0)
        GLES30.glUniform3fv(uSkyMid, 1, skyMid, 0)
        GLES30.glUniform3fv(uSkyZenith, 1, skyZenith, 0)
        GLES30.glUniform3fv(uSkySunCol, 1, sunCol, 0)
        GLES30.glUniform1f(uSkyNight, nightFactor)
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
        GLES30.glUniform3fv(uOceanSkyHorizon, 1, skyHorizon, 0)
        GLES30.glUniform3fv(uOceanSkyZenith, 1, skyZenith, 0)
        GLES30.glUniform3fv(uOceanSunCol, 1, sunCol, 0)
        GLES30.glUniform3fv(uOceanFogCol, 1, fogCol, 0)
        GLES30.glUniform1f(uOceanFogDensity, physics.settings.graphicsPreset.fogDensity)
        GLES30.glUniform1f(uOceanNight, nightFactor)
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

        val gfx = physics.settings.graphicsPreset
        val shadowsOn = gfx.shadowsEnabled && physics.timeOfDay != TimeOfDay.NIGHT
        val headlightsOn = physics.timeOfDay == TimeOfDay.NIGHT && gfx.headlightsAllowed && physics.gameState != GameState.PAUSED

        GLES30.glUniform3fv(u.uSkyHorizon, 1, skyHorizon, 0)
        GLES30.glUniform3fv(u.uSkyZenith, 1, skyZenith, 0)
        GLES30.glUniform3fv(u.uSunCol, 1, sunCol, 0)
        GLES30.glUniform3fv(u.uFogColor, 1, fogCol, 0)
        GLES30.glUniform1f(u.uFogDensity, gfx.fogDensity)
        GLES30.glUniform1f(u.uShadowEnabled, if (shadowsOn) 1f else 0f)
        GLES30.glUniform1f(u.uShadowTexel, 1.0f / currentShadowRes.toFloat())
        GLES30.glUniform1f(u.uReflectionsEnabled, if (gfx.reflectionsEnabled) 1f else 0f)
        GLES30.glUniform1f(u.uAmbientScale, ambientScale)
        GLES30.glUniform1f(u.uNightFactor, nightFactor)
        GLES30.glUniform3f(u.uCarPos, physics.carX, physics.carY, physics.carZ)
        GLES30.glUniform3f(u.uCarFwd, sin(physics.yaw), 0f, cos(physics.yaw))
        GLES30.glUniform1f(u.uHeadlightOn, if (headlightsOn) 1f else 0f)

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

        if (physics.isNitroActive) {
            GLES30.glDisable(GLES30.GL_CULL_FACE)
            carSet.nitroFlameMesh.draw()
            GLES30.glEnable(GLES30.GL_CULL_FACE)
        } else if (physics.carType.hasBackfireFlames && (physics.backfireTimer > 0f || physics.boostTimer > 0f)) {
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
        worldMeshes.buildingInstanced.drawInstanced()
        worldMeshes.palmInstanced.drawInstanced()
        worldMeshes.lampInstanced.drawInstanced()
        worldMeshes.barrierInstanced.drawInstanced()
        worldMeshes.yachtInstanced.drawInstanced()
        worldMeshes.canyonRockInstanced.drawInstanced()
        worldMeshes.billboardInstanced.drawInstanced()
        worldMeshes.speedTrapInstanced.drawInstanced()

        GLES30.glDisable(GLES30.GL_CULL_FACE)
        worldMeshes.boostRingInstanced.drawInstanced()
        GLES30.glEnable(GLES30.GL_CULL_FACE)
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

    private fun renderDynamicEffects(braking: Boolean) {
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
            val nx = (-dz / len) * 0.18f
            val nz = (dx / len) * 0.18f
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

        // Additive glow batch: Headlight/tail-light halos, night light trails, wall-scrape sparks
        var glowCount = 0
        val fwdX = sin(physics.yaw)
        val fwdZ = cos(physics.yaw)
        val carRightX = cos(physics.yaw)
        val carRightZ = -sin(physics.yaw)
        val bodyY = physics.carY + physics.suspensionY

        val headAlpha = if (physics.timeOfDay == TimeOfDay.NIGHT) 0.56f else 0.34f
        val headSize = if (physics.timeOfDay == TimeOfDay.NIGHT) 0.82f else 0.55f
        for (side in floatArrayOf(-0.68f, 0.68f)) {
            val hx = physics.carX + fwdX * 2.34f + carRightX * side
            val hy = bodyY + 0.52f
            val hz = physics.carZ + fwdZ * 2.34f + carRightZ * side
            glowCount = pushQuad(
                glowCount,
                hx - rightX * headSize, hy - headSize, hz - rightZ * headSize,
                hx + rightX * headSize, hy - headSize, hz + rightZ * headSize,
                hx + rightX * headSize, hy + headSize, hz + rightZ * headSize,
                hx - rightX * headSize, hy + headSize, hz - rightZ * headSize,
                0.78f, 0.94f, 1.0f, headAlpha,
                solid = false
            )
        }
        val tailAlpha = if (braking) 0.62f else if (physics.timeOfDay == TimeOfDay.NIGHT) 0.38f else 0.22f
        val tailSize = if (braking) 0.78f else 0.48f
        for (side in floatArrayOf(-0.60f, 0.60f)) {
            val tx = physics.carX - fwdX * 2.38f + carRightX * side
            val ty = bodyY + 0.68f
            val tz = physics.carZ - fwdZ * 2.38f + carRightZ * side
            glowCount = pushQuad(
                glowCount,
                tx - rightX * tailSize, ty - tailSize, tz - rightZ * tailSize,
                tx + rightX * tailSize, ty - tailSize, tz + rightZ * tailSize,
                tx + rightX * tailSize, ty + tailSize, tz + rightZ * tailSize,
                tx - rightX * tailSize, ty + tailSize, tz - rightZ * tailSize,
                1.0f, 0.14f, 0.16f, tailAlpha,
                solid = false
            )
        }

        // Glowing red night tail-light trails
        for (t in physics.tailTrails) {
            if (!t.active) continue
            if (glowCount + 6 * 9 > maxDynFloats) break
            val w = 0.08f
            glowCount = pushQuad(
                glowCount,
                t.x1, t.y1 - w, t.z1,
                t.x2, t.y2 - w, t.z2,
                t.x2, t.y2 + w, t.z2,
                t.x1, t.y1 + w, t.z1,
                1.0f, 0.10f, 0.14f, t.life * 0.72f,
                solid = true
            )
        }

        // Glowing orange wall-slide sparks
        for (sp in physics.sparkParticles) {
            if (!sp.active) continue
            if (glowCount + 6 * 9 > maxDynFloats) break
            val life = 1f - (sp.age / sp.maxAge).coerceIn(0f, 1f)
            val hs = 0.24f
            glowCount = pushQuad(
                glowCount,
                sp.x - rightX * hs, sp.y - hs, sp.z - rightZ * hs,
                sp.x + rightX * hs, sp.y - hs, sp.z + rightZ * hs,
                sp.x + rightX * hs, sp.y + hs, sp.z + rightZ * hs,
                sp.x - rightX * hs, sp.y + hs, sp.z - rightZ * hs,
                1.0f, 0.62f, 0.14f, life * 0.95f,
                solid = false
            )
        }

        if (glowCount > 0) {
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
            drawDynamicBatch(glowCount)
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
            currentShadowRes, currentShadowRes, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null
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

    private fun ensureShadowResolution(targetRes: Int) {
        if (targetRes == currentShadowRes || shadowDepthTex == 0) return
        currentShadowRes = targetRes
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowDepthTex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_DEPTH_COMPONENT24,
            currentShadowRes, currentShadowRes, 0, GLES30.GL_DEPTH_COMPONENT, GLES30.GL_UNSIGNED_INT, null
        )
    }

    private fun ensureSceneFbo(targetW: Int, targetH: Int) {
        if (sceneFbo != 0 && sceneW == targetW && sceneH == targetH) return
        sceneW = targetW
        sceneH = targetH

        if (sceneFbo == 0) {
            val fbos = IntArray(1)
            val texs = IntArray(1)
            val rbos = IntArray(1)
            GLES30.glGenFramebuffers(1, fbos, 0)
            GLES30.glGenTextures(1, texs, 0)
            GLES30.glGenRenderbuffers(1, rbos, 0)
            sceneFbo = fbos[0]
            sceneColorTex = texs[0]
            sceneDepthRbo = rbos[0]
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneColorTex)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8,
            sceneW, sceneH, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, sceneDepthRbo)
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, sceneW, sceneH)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, sceneFbo)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, sceneColorTex, 0
        )
        GLES30.glFramebufferRenderbuffer(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_RENDERBUFFER, sceneDepthRbo
        )
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
