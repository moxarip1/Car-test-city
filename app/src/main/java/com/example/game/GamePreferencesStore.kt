package com.example.game

import android.content.Context
import android.content.SharedPreferences

class GamePreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): GameSettings {
        val gfx = enumOrDefault(prefs.getString(KEY_GRAPHICS_PRESET, null), GraphicsPreset.HIGH)
        val res = enumOrDefault(prefs.getString(KEY_RESOLUTION_PRESET, null), ResolutionPreset.HIGH_100)
        val steerMode = enumOrDefault(prefs.getString(KEY_STEERING_MODE, null), SteeringMode.BUTTONS)
        val unit = enumOrDefault(prefs.getString(KEY_SPEED_UNIT, null), SpeedUnit.KMH)
        return GameSettings(
            graphicsPreset = gfx,
            resolutionPreset = res,
            showFps = prefs.getBoolean(KEY_SHOW_FPS, false),
            masterVolume = prefs.getFloat(KEY_MASTER_VOL, 0.85f).coerceIn(0f, 1f),
            engineVolume = prefs.getFloat(KEY_ENGINE_VOL, 0.90f).coerceIn(0f, 1f),
            sfxVolume = prefs.getFloat(KEY_SFX_VOL, 0.85f).coerceIn(0f, 1f),
            isMuted = prefs.getBoolean(KEY_MUTED, false),
            steeringMode = steerMode,
            steeringSensitivity = prefs.getFloat(KEY_STEER_SENSITIVITY, 1.0f).coerceIn(0.5f, 1.5f),
            smartDriftAssist = prefs.getBoolean(KEY_DRIFT_ASSIST, true),
            speedUnit = unit
        )
    }

    fun saveSettings(s: GameSettings) {
        prefs.edit()
            .putString(KEY_GRAPHICS_PRESET, s.graphicsPreset.name)
            .putString(KEY_RESOLUTION_PRESET, s.resolutionPreset.name)
            .putBoolean(KEY_SHOW_FPS, s.showFps)
            .putFloat(KEY_MASTER_VOL, s.masterVolume)
            .putFloat(KEY_ENGINE_VOL, s.engineVolume)
            .putFloat(KEY_SFX_VOL, s.sfxVolume)
            .putBoolean(KEY_MUTED, s.isMuted)
            .putString(KEY_STEERING_MODE, s.steeringMode.name)
            .putFloat(KEY_STEER_SENSITIVITY, s.steeringSensitivity)
            .putBoolean(KEY_DRIFT_ASSIST, s.smartDriftAssist)
            .putString(KEY_SPEED_UNIT, s.speedUnit.name)
            .apply()
    }

    fun loadCarType(): CarType =
        enumOrDefault(prefs.getString(KEY_CAR_TYPE, null), CarType.EV_HYPER_GT)

    fun saveCarType(type: CarType) {
        prefs.edit().putString(KEY_CAR_TYPE, type.name).apply()
    }

    fun loadPaintIndex(defaultIndex: Int): Int =
        prefs.getInt(KEY_PAINT_INDEX, defaultIndex).coerceIn(LUXURY_PAINT_PRESETS.indices)

    fun savePaintIndex(index: Int) {
        prefs.edit().putInt(KEY_PAINT_INDEX, index).apply()
    }

    fun loadPaintFinish(): PaintFinish =
        enumOrDefault(prefs.getString(KEY_PAINT_FINISH, null), PaintFinish.GLOSSY)

    fun savePaintFinish(finish: PaintFinish) {
        prefs.edit().putString(KEY_PAINT_FINISH, finish.name).apply()
    }

    fun loadTimeOfDay(): TimeOfDay =
        enumOrDefault(prefs.getString(KEY_TIME_OF_DAY, null), TimeOfDay.SUNSET)

    fun saveTimeOfDay(tod: TimeOfDay) {
        prefs.edit().putString(KEY_TIME_OF_DAY, tod.name).apply()
    }

    fun loadRecords(): SavedRecords {
        return SavedRecords(
            bestDriftScore = prefs.getInt(KEY_BEST_DRIFT, 0),
            bestTotalScore = prefs.getInt(KEY_BEST_TOTAL, 0),
            topSpeedKmh = prefs.getInt(KEY_TOP_SPEED, 0)
        )
    }

    fun saveRecords(r: SavedRecords) {
        prefs.edit()
            .putInt(KEY_BEST_DRIFT, r.bestDriftScore)
            .putInt(KEY_BEST_TOTAL, r.bestTotalScore)
            .putInt(KEY_TOP_SPEED, r.topSpeedKmh)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
        if (name == null) return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }

    companion object {
        private const val PREFS_NAME = "hypercar_resort_prefs"
        private const val KEY_GRAPHICS_PRESET = "graphics_preset"
        private const val KEY_RESOLUTION_PRESET = "resolution_preset"
        private const val KEY_SHOW_FPS = "show_fps"
        private const val KEY_MASTER_VOL = "master_vol"
        private const val KEY_ENGINE_VOL = "engine_vol"
        private const val KEY_SFX_VOL = "sfx_vol"
        private const val KEY_MUTED = "muted"
        private const val KEY_STEERING_MODE = "steering_mode"
        private const val KEY_STEER_SENSITIVITY = "steer_sensitivity"
        private const val KEY_DRIFT_ASSIST = "drift_assist"
        private const val KEY_SPEED_UNIT = "speed_unit"
        private const val KEY_CAR_TYPE = "car_type"
        private const val KEY_PAINT_INDEX = "paint_index"
        private const val KEY_PAINT_FINISH = "paint_finish"
        private const val KEY_TIME_OF_DAY = "time_of_day"
        private const val KEY_BEST_DRIFT = "best_drift_score"
        private const val KEY_BEST_TOTAL = "best_total_score"
        private const val KEY_TOP_SPEED = "top_speed_kmh"
    }
}
