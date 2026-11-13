package com.example

import com.example.game.CarAudioEngine
import com.example.game.CarType
import com.example.game.CityLayout
import com.example.game.ControlState
import com.example.game.GameWorldPhysics
import com.example.game.StuntBannerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePhysicsTest {

    @Test
    fun accelerationAndInstantCarSwitchWork() {
        val physics = GameWorldPhysics(CarAudioEngine())
        val gas = ControlState(gas = true)

        repeat(60) {
            physics.step(1f / 60f, gas)
        }
        assertTrue("EV Hyper-GT should accelerate rapidly", physics.vz > 35f)

        physics.switchCar(CarType.V12_APEX_ROADSTER)
        val afterSwitch = physics.step(1f / 60f, gas)
        assertEquals(CarType.V12_APEX_ROADSTER, afterSwitch.carType)
        assertTrue("Speed should be preserved on live car switch", afterSwitch.speedKmh > 100)
    }

    @Test
    fun boostPadAndStuntRampAndHelipadPhysicsWork() {
        val physics = GameWorldPhysics(CarAudioEngine())
        // Place car at acceleration tunnel boost pad
        physics.carX = 57f
        physics.carZ = -96f
        val boostHud = physics.step(1f / 60f, ControlState(gas = true))
        assertTrue("Boost pad should activate turbo boost", boostHud.isBoosting)
        assertEquals(StuntBannerType.TURBO_BOOST, boostHud.bannerType)

        // Test stunt ramp launch over marina canal
        physics.carX = 59f
        physics.carZ = 34f
        physics.carY = CityLayout.queryGroundHeight(59f, 5f, 34f)
        physics.yaw = 0f
        physics.vx = 0f
        physics.vz = 55f
        repeat(10) {
            physics.step(1f / 60f, ControlState(gas = true))
        }
        assertTrue("Car should launch airborne off marina stunt ramp", physics.carY > 4.5f)

        // Test helipad landing bonus
        physics.carX = CityLayout.HELIPAD_CX
        physics.carZ = CityLayout.HELIPAD_CZ
        physics.carY = CityLayout.HELIPAD_HEIGHT
        val heliHud = physics.step(1f / 60f, ControlState())
        assertTrue("Helipad landing should award +5000 points", heliHud.totalScore >= 5000)
        assertEquals(StuntBannerType.HELIPAD_LANDING, heliHud.bannerType)
    }

    @Test
    fun resetCarRecoversFromOcean() {
        val physics = GameWorldPhysics(CarAudioEngine())
        physics.carX = 220f
        physics.carZ = 220f
        physics.carY = -3.0f
        physics.step(1f / 60f, ControlState())
        assertTrue("Car should recover onto island road", physics.carY >= 0f)
        assertTrue("Car should be back inside island bounds", kotlin.math.abs(physics.carX) < 130f)
    }

    @Test
    fun sideActionCameraAndReverseLightsWork() {
        val physics = GameWorldPhysics(CarAudioEngine())
        physics.cameraMode = com.example.game.CameraMode.SIDE_ACTION
        val revState = ControlState(brake = true)
        var lastHud = physics.step(1f / 60f, revState)
        repeat(25) {
            lastHud = physics.step(1f / 60f, revState)
        }
        assertEquals(com.example.game.CameraMode.SIDE_ACTION, lastHud.cameraMode)
        assertTrue("Reversing should activate reverse lights", lastHud.isReversing)
        assertTrue(
            "Side-action camera should position laterally off car flank",
            kotlin.math.abs(physics.camPosX - physics.carX) > 4.0f
        )
    }
}
