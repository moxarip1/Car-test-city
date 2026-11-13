package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.game.CameraMode
import com.example.game.CarAudioEngine
import com.example.game.CarType
import com.example.game.ControlState
import com.example.game.GamePreferencesStore
import com.example.game.GameSettings
import com.example.game.GameState
import com.example.game.HudTelemetry
import com.example.game.LUXURY_PAINT_PRESETS
import com.example.game.PaintFinish
import com.example.game.SavedRecords
import com.example.game.SharedGameInput
import com.example.game.SpawnLocation
import com.example.game.TimeOfDay
import com.example.game.gl.HypercarGlRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsStore = GamePreferencesStore(application)
    val sharedInput = SharedGameInput()
    val audioEngine = CarAudioEngine()

    private val initialSettings = prefsStore.loadSettings()
    private val initialCar = prefsStore.loadCarType()
    private val initialPaint = prefsStore.loadPaintIndex(initialCar.defaultPaintIndex)
    private val initialFinish = prefsStore.loadPaintFinish()
    private val initialTod = prefsStore.loadTimeOfDay()
    private val initialRecords = prefsStore.loadRecords()

    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private val _records = MutableStateFlow(initialRecords)
    val records: StateFlow<SavedRecords> = _records.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _telemetry = MutableStateFlow(
        HudTelemetry(
            gameState = GameState.SPLASH,
            carType = initialCar,
            paintIndex = initialPaint,
            paintFinish = initialFinish,
            timeOfDay = initialTod,
            bestDriftScore = initialRecords.bestDriftScore,
            topSpeedKmh = initialRecords.topSpeedKmh,
            isMuted = initialSettings.isMuted
        )
    )
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

    init {
        audioEngine.applySettings(
            master = initialSettings.masterVolume,
            engine = initialSettings.engineVolume,
            sfx = initialSettings.sfxVolume,
            isMuted = initialSettings.isMuted
        )
        renderer.postAction { physics ->
            physics.carType = initialCar
            physics.paintIndex = initialPaint
            physics.paintFinish = initialFinish
            physics.timeOfDay = initialTod
            physics.setRecords(initialRecords)
            physics.applySettings(initialSettings)
            physics.onRecordsChanged = { updated ->
                _records.value = updated
                prefsStore.saveRecords(updated)
            }
        }
    }

    fun finishSplash() {
        if (_telemetry.value.gameState != GameState.SPLASH) return
        _telemetry.value = _telemetry.value.copy(gameState = GameState.MENU)
        renderer.postAction { physics ->
            physics.gameState = GameState.MENU
            physics.snapToShowroom()
        }
    }

    fun launchGame(spawn: SpawnLocation) {
        onFirstUserTouch()
        clearAllControlInputs()
        _isSettingsOpen.value = false
        _telemetry.value = _telemetry.value.copy(gameState = GameState.PLAYING)
        renderer.postAction { physics ->
            physics.gameState = GameState.PLAYING
            physics.teleportTo(spawn)
        }
    }

    fun pauseGame() {
        if (_telemetry.value.gameState != GameState.PLAYING) return
        clearAllControlInputs()
        _telemetry.value = _telemetry.value.copy(gameState = GameState.PAUSED)
        renderer.postAction { physics ->
            physics.gameState = GameState.PAUSED
        }
    }

    fun resumeGame() {
        if (_telemetry.value.gameState != GameState.PAUSED) return
        _isSettingsOpen.value = false
        _telemetry.value = _telemetry.value.copy(gameState = GameState.PLAYING)
        renderer.postAction { physics ->
            physics.gameState = GameState.PLAYING
        }
    }

    fun exitToMenu() {
        clearAllControlInputs()
        _isSettingsOpen.value = false
        _telemetry.value = _telemetry.value.copy(gameState = GameState.MENU)
        renderer.postAction { physics ->
            physics.gameState = GameState.MENU
            physics.snapToShowroom()
        }
    }

    fun teleportTo(spawn: SpawnLocation) {
        clearAllControlInputs()
        _isSettingsOpen.value = false
        _telemetry.value = _telemetry.value.copy(gameState = GameState.PLAYING)
        renderer.postAction { physics ->
            physics.gameState = GameState.PLAYING
            physics.teleportTo(spawn)
        }
    }

    fun openSettings() {
        onFirstUserTouch()
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun updateSettings(updated: GameSettings) {
        _settings.value = updated
        prefsStore.saveSettings(updated)
        audioEngine.applySettings(
            master = updated.masterVolume,
            engine = updated.engineVolume,
            sfx = updated.sfxVolume,
            isMuted = updated.isMuted
        )
        _telemetry.value = _telemetry.value.copy(isMuted = updated.isMuted)
        renderer.postAction { physics ->
            physics.applySettings(updated)
        }
    }

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

    fun setTouchAnalogSteer(steer: Float) {
        onFirstUserTouch()
        updateTouch { it.copy(analogSteer = steer.coerceIn(-1f, 1f)) }
    }

    fun setTouchHandbrake(pressed: Boolean) {
        if (pressed) onFirstUserTouch()
        updateTouch { it.copy(handbrake = pressed) }
    }

    fun setTouchNitro(pressed: Boolean) {
        if (pressed) onFirstUserTouch()
        updateTouch { it.copy(nitro = pressed) }
    }

    fun setKeyGas(pressed: Boolean) = updateKeyboard { it.copy(gas = pressed) }
    fun setKeyBrake(pressed: Boolean) = updateKeyboard { it.copy(brake = pressed) }
    fun setKeySteerLeft(pressed: Boolean) = updateKeyboard { it.copy(steerLeft = pressed) }
    fun setKeySteerRight(pressed: Boolean) = updateKeyboard { it.copy(steerRight = pressed) }
    fun setKeyHandbrake(pressed: Boolean) = updateKeyboard { it.copy(handbrake = pressed) }
    fun setKeyNitro(pressed: Boolean) = updateKeyboard { it.copy(nitro = pressed) }

    private fun clearAllControlInputs() {
        sharedInput.touchControls.set(ControlState())
        sharedInput.keyboardControls.set(ControlState())
    }

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
        prefsStore.saveCarType(type)
        prefsStore.savePaintIndex(type.defaultPaintIndex)
        _telemetry.value = _telemetry.value.copy(carType = type, paintIndex = type.defaultPaintIndex)
        renderer.postAction { it.switchCar(type) }
    }

    fun selectPaintColor(index: Int) {
        onFirstUserTouch()
        val safeIdx = index.coerceIn(LUXURY_PAINT_PRESETS.indices)
        prefsStore.savePaintIndex(safeIdx)
        _telemetry.value = _telemetry.value.copy(paintIndex = safeIdx)
        renderer.postAction { it.paintIndex = safeIdx }
    }

    fun cyclePaintFinish() {
        onFirstUserTouch()
        val finishes = PaintFinish.entries
        val next = finishes[(_telemetry.value.paintFinish.ordinal + 1) % finishes.size]
        prefsStore.savePaintFinish(next)
        _telemetry.value = _telemetry.value.copy(paintFinish = next)
        renderer.postAction { it.paintFinish = next }
    }

    fun cycleCameraMode() {
        onFirstUserTouch()
        val modes = CameraMode.entries
        val next = modes[(_telemetry.value.cameraMode.ordinal + 1) % modes.size]
        _telemetry.value = _telemetry.value.copy(cameraMode = next)
        renderer.postAction { it.cameraMode = next }
    }

    fun cycleTimeOfDay() {
        onFirstUserTouch()
        val times = TimeOfDay.entries
        val next = times[(_telemetry.value.timeOfDay.ordinal + 1) % times.size]
        prefsStore.saveTimeOfDay(next)
        _telemetry.value = _telemetry.value.copy(timeOfDay = next)
        renderer.postAction { it.timeOfDay = next }
    }

    fun toggleMute() {
        onFirstUserTouch()
        val nextMuted = !_settings.value.isMuted
        updateSettings(_settings.value.copy(isMuted = nextMuted))
    }

    fun resetCar() {
        onFirstUserTouch()
        if (_telemetry.value.gameState == GameState.PAUSED) {
            _telemetry.value = _telemetry.value.copy(gameState = GameState.PLAYING)
        }
        renderer.postAction {
            it.gameState = GameState.PLAYING
            it.resetCar()
        }
    }

    override fun onCleared() {
        audioEngine.stop()
        super.onCleared()
    }
}
