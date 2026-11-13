package com.example.game.gl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.EGLExt
import android.opengl.GLSurfaceView
import android.view.TextureView

/**
 * Hardware-accelerated OpenGL ES 3.0 TextureView backed by EGL14.
 * Avoids SurfaceView SurfaceSyncGroup synchronization stalls inside Jetpack Compose AndroidView.
 */
class GlTextureView(
    context: Context,
    private val renderer: GLSurfaceView.Renderer
) : TextureView(context), TextureView.SurfaceTextureListener {

    private var renderThread: RenderThread? = null

    init {
        isOpaque = true
        surfaceTextureListener = this
    }

    fun onResume() {
        renderThread?.setPaused(false)
    }

    fun onPause() {
        renderThread?.setPaused(true)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        renderThread = RenderThread(surface, width, height, renderer).apply { start() }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderThread?.onResize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderThread?.requestExitAndWait()
        renderThread = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // No-op: driven continuously by RenderThread at 60 FPS vsync
    }

    private class RenderThread(
        private val surfaceTexture: SurfaceTexture,
        initialWidth: Int,
        initialHeight: Int,
        private val renderer: GLSurfaceView.Renderer
    ) : Thread("HypercarGLThread") {

        private val lock = Object()
        private var shouldExit = false
        private var paused = false
        private var width = initialWidth
        private var height = initialHeight
        private var sizeChanged = true

        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

        fun setPaused(pause: Boolean) {
            synchronized(lock) {
                paused = pause
                lock.notifyAll()
            }
        }

        fun onResize(w: Int, h: Int) {
            synchronized(lock) {
                width = w
                height = h
                sizeChanged = true
                lock.notifyAll()
            }
        }

        fun requestExitAndWait() {
            synchronized(lock) {
                shouldExit = true
                lock.notifyAll()
            }
            try {
                join(800)
            } catch (_: InterruptedException) {
            }
        }

        override fun run() {
            if (!initEgl()) return
            try {
                renderer.onSurfaceCreated(null, null)
                while (true) {
                    var w: Int
                    var h: Int
                    var resized: Boolean
                    synchronized(lock) {
                        while (paused && !shouldExit) {
                            lock.wait()
                        }
                        if (shouldExit) return
                        w = width
                        h = height
                        resized = sizeChanged
                        sizeChanged = false
                    }

                    if (resized && w > 0 && h > 0) {
                        renderer.onSurfaceChanged(null, w, h)
                    }

                    renderer.onDrawFrame(null)
                    EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                }
            } finally {
                destroyEgl()
            }
        }

        private fun initEgl(): Boolean {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

            val attribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
                attribs[1] = EGL14.EGL_OPENGL_ES2_BIT
                if (!EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
                    return false
                }
            }
            val config = configs[0] ?: return false

            val ctxAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) return false

            val surfAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surfaceTexture, surfAttribs, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) return false

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false
            EGL14.eglSwapInterval(eglDisplay, 1)
            return true
        }

        private fun destroyEgl() {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    eglDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
                )
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                }
                EGL14.eglTerminate(eglDisplay)
            }
            eglSurface = EGL14.EGL_NO_SURFACE
            eglContext = EGL14.EGL_NO_CONTEXT
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }
}
