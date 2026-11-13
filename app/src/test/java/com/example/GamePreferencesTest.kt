package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.CarType
import com.example.game.GamePreferencesStore
import com.example.game.GameSettings
import com.example.game.GraphicsPreset
import com.example.game.PaintFinish
import com.example.game.ResolutionPreset
import com.example.game.SavedRecords
import com.example.game.SpeedUnit
import com.example.game.SteeringMode
import com.example.game.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GamePreferencesTest {

    @Test
    fun preferencesStorePersistsSettingsGarageAndRecords() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = GamePreferencesStore(context)

        val customSettings = GameSettings(
            graphicsPreset = GraphicsPreset.ULTRA,
            resolutionPreset = ResolutionPreset.BALANCED_75,
            showFps = true,
            masterVolume = 0.65f,
            engineVolume = 0.72f,
            sfxVolume = 0.50f,
            isMuted = false,
            steeringMode = SteeringMode.VIRTUAL_WHEEL,
            steeringSensitivity = 1.35f,
            smartDriftAssist = false,
            speedUnit = SpeedUnit.MPH
        )
        store.saveSettings(customSettings)
        store.saveCarType(CarType.V12_APEX_ROADSTER)
        store.savePaintIndex(5)
        store.savePaintFinish(PaintFinish.CHROME)
        store.saveTimeOfDay(TimeOfDay.NIGHT)
        store.saveRecords(SavedRecords(bestDriftScore = 8400, bestTotalScore = 24500, topSpeedKmh = 382))

        val reloadedStore = GamePreferencesStore(context)
        val loaded = reloadedStore.loadSettings()
        assertEquals(GraphicsPreset.ULTRA, loaded.graphicsPreset)
        assertEquals(ResolutionPreset.BALANCED_75, loaded.resolutionPreset)
        assertTrue(loaded.showFps)
        assertEquals(0.65f, loaded.masterVolume, 0.01f)
        assertEquals(0.72f, loaded.engineVolume, 0.01f)
        assertEquals(0.50f, loaded.sfxVolume, 0.01f)
        assertFalse(loaded.isMuted)
        assertEquals(SteeringMode.VIRTUAL_WHEEL, loaded.steeringMode)
        assertEquals(1.35f, loaded.steeringSensitivity, 0.01f)
        assertFalse(loaded.smartDriftAssist)
        assertEquals(SpeedUnit.MPH, loaded.speedUnit)

        assertEquals(CarType.V12_APEX_ROADSTER, reloadedStore.loadCarType())
        assertEquals(5, reloadedStore.loadPaintIndex(0))
        assertEquals(PaintFinish.CHROME, reloadedStore.loadPaintFinish())
        assertEquals(TimeOfDay.NIGHT, reloadedStore.loadTimeOfDay())

        val loadedRecords = reloadedStore.loadRecords()
        assertEquals(8400, loadedRecords.bestDriftScore)
        assertEquals(24500, loadedRecords.bestTotalScore)
        assertEquals(382, loadedRecords.topSpeedKmh)
    }
}
