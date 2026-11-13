package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.game.CameraMode
import com.example.game.CarAudioEngine
import com.example.game.CarType
import com.example.game.ControlState
import com.example.game.HudTelemetry
import com.example.game.LUXURY_PAINT_PRESETS
import com.example.game.PaintFinish
import com.example.game.SharedGameInput
import com.example.game.gl.HypercarGlRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    val sharedInput = SharedGameInput()
    val audioEngine = CarAudioEngine()

    private val _telemetry = MutableStateFlow(HudTelemetry())
    val telemetry: StateFlow<HudTelemetry> = _telemetry.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private var engagedFullscreenOnFirstTouch = false

    val renderer = HypercarGlRenderer(
        sharedInput = sharedInput,
        audioEngine = audioEngine,
        onTelemetryUpdate = { snapshot ->
            _telemetry.value = snapshot
        }
    )

    fun onFirstUserTouch() {
        if (!engagedFullscreenOnFirstTouch) {
            engagedFullscreenOnFirstTouch = true
            _isFullscreen.value = true
        }
    }

    fun toggleFullscreen() {
        engagedFullscreenOnFirstTouch = true
        _isFullscreen.value = !_isFullscreen.value
    }

    fun setTouchGas(pressed: Boolean) {
        if (pressed) onFirstUserTouch()
        updateTouch { it.copy(gas = pressed) }
    }

    fun setTouchBrake(pressed: Boolean) {
        if (pressed) onFirstUserTouch()
        updateTouch { it.copy(brake = pressed) }
    }

    fun setTouchSteerLeft(pressed: Boolean) {
        if (pressed) onFirstUserTouch()
        updateTouch { it.copy(steerLeft = pressed) }
    }

    fun setTouchSteerRight(pressed: Boolean) {
        if (pressed) onFirstUserTouch()
        updateTouch { it.copy(steerRight = pressed) }
    }

    fun setTouchHandbrake(pressed: Boolean) {
        if (pressed) onFirstUserTouch()
        updateTouch { it.copy(handbrake = pressed) }
    }

    fun setKeyGas(pressed: Boolean) = updateKeyboard { it.copy(gas = pressed) }
    fun setKeyBrake(pressed: Boolean) = updateKeyboard { it.copy(brake = pressed) }
    fun setKeySteerLeft(pressed: Boolean) = updateKeyboard { it.copy(steerLeft = pressed) }
    fun setKeySteerRight(pressed: Boolean) = updateKeyboard { it.copy(steerRight = pressed) }
    fun setKeyHandbrake(pressed: Boolean) = updateKeyboard { it.copy(handbrake = pressed) }

    private inline fun updateTouch(crossinline transform: (ControlState) -> ControlState) {
        while (true) {
            val cur = sharedInput.touchControls.get()
            if (sharedInput.touchControls.compareAndSet(cur, transform(cur))) break
        }
    }

    private inline fun updateKeyboard(crossinline transform: (ControlState) -> ControlState) {
        while (true) {
            val cur = sharedInput.keyboardControls.get()
            if (sharedInput.keyboardControls.compareAndSet(cur, transform(cur))) break
        }
    }

    fun selectCar(type: CarType) {
        onFirstUserTouch()
        renderer.postAction { it.switchCar(type) }
    }

    fun selectPaintColor(index: Int) {
        onFirstUserTouch()
        val safeIdx = index.coerceIn(LUXURY_PAINT_PRESETS.indices)
        renderer.postAction { it.paintIndex = safeIdx }
    }

    fun cyclePaintFinish() {
        onFirstUserTouch()
        val finishes = PaintFinish.entries
        val next = finishes[(_telemetry.value.paintFinish.ordinal + 1) % finishes.size]
        renderer.postAction { it.paintFinish = next }
    }

    fun cycleCameraMode() {
        onFirstUserTouch()
        val modes = CameraMode.entries
        val next = modes[(_telemetry.value.cameraMode.ordinal + 1) % modes.size]
        renderer.postAction { it.cameraMode = next }
    }

    fun toggleMute() {
        onFirstUserTouch()
        val nextMuted = !_telemetry.value.isMuted
        audioEngine.setMuted(nextMuted)
        renderer.postAction { it.isMuted = nextMuted }
    }

    fun resetCar() {
        onFirstUserTouch()
        renderer.postAction { it.resetCar() }
    }

    override fun onCleared() {
        audioEngine.stop()
        super.onCleared()
    }
}
