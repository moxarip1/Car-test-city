package com.example

import com.example.game.CameraMode
import com.example.game.CarAudioEngine
import com.example.game.CarType
import com.example.game.CityLayout
import com.example.game.ControlState
import com.example.game.DistrictZone
import com.example.game.GameSettings
import com.example.game.GameState
import com.example.game.GameWorldPhysics
import com.example.game.SpawnLocation
import com.example.game.StuntBannerType
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePhysicsTest {

    @Test
    fun accelerationGearboxAndLiveCarSwitchWork() {
        val physics = GameWorldPhysics(CarAudioEngine())
        val gas = ControlState(gas = true)

        var lastHud = physics.step(1f / 60f, gas)
        repeat(150) {
            lastHud = physics.step(1f / 60f, gas)
        }
        assertTrue("EV Hyper-GT should accelerate rapidly", physics.vz > 55f)
        assertTrue("6-speed gearbox should upshift cleanly", lastHud.gear >= 3)

        physics.switchCar(CarType.V12_APEX_ROADSTER)
        val afterSwitch = physics.step(1f / 60f, gas)
        assertEquals(CarType.V12_APEX_ROADSTER, afterSwitch.carType)
        assertTrue("Speed should be preserved on live car switch", afterSwitch.speedKmh > 180)
    }

    @Test
    fun nitroBoostAndDriftRechargeWork() {
        val physics = GameWorldPhysics(CarAudioEngine())
        val initialNitro = physics.nitroCharge
        val nitroHud = physics.step(0.1f, ControlState(gas = true, nitro = true))
        assertTrue("Nitro should activate when charged", nitroHud.isNitroActive)
        assertTrue("Nitro should drain while active", nitroHud.nitroCharge < initialNitro)

        // Drifting recharges nitro
        physics.nitroCharge = 40f
        physics.carX = 0f
        physics.carZ = -180f
        physics.vx = 18f
        physics.vz = 40f
        val driftHud = physics.step(0.1f, ControlState(gas = true, steerLeft = true, handbrake = true))
        assertTrue("Drifting should recharge nitro gauge", driftHud.nitroCharge > 40f)
    }

    @Test
    fun boostPadStuntRampBoostRingAndHelipadWork() {
        val physics = GameWorldPhysics(CarAudioEngine())

        // 1. Marina acceleration tunnel boost pad
        physics.carX = 580f
        physics.carZ = -490f
        val boostHud = physics.step(1f / 60f, ControlState(gas = true))
        assertTrue("Boost pad should activate turbo boost", boostHud.isBoosting)
        assertEquals(StuntBannerType.TURBO_BOOST, boostHud.bannerType)
        assertEquals(DistrictZone.MARINA, boostHud.district)

        // 2. Stunt ramp launch over Marina canal
        physics.carX = 570f
        physics.carZ = 38f
        physics.carY = CityLayout.queryGroundHeight(570f, 8f, 38f)
        physics.yaw = 0f
        physics.vx = 0f
        physics.vz = 68f
        repeat(10) {
            physics.step(1f / 60f, ControlState(gas = true))
        }
        assertTrue("Car should launch airborne off Marina stunt ramp", physics.carY > 5.5f)

        // 3. Aerial boost ring fly-through
        val ring = CityLayout.boostRings[2] // Airport runway aerial ring
        physics.carX = ring.cx
        physics.carY = ring.cy - 0.5f
        physics.carZ = ring.cz
        val ringHud = physics.step(1f / 60f, ControlState(gas = true))
        assertTrue("Aerial boost ring should award +1500 points", ringHud.totalScore >= 1500)
        assertEquals(StuntBannerType.BOOST_RING, ringHud.bannerType)

        // 4. Rooftop Helipad landing bonus in Stunt Park
        physics.carX = CityLayout.HELIPAD_CX
        physics.carZ = CityLayout.HELIPAD_CZ
        physics.carY = CityLayout.HELIPAD_HEIGHT
        val heliHud = physics.step(1f / 60f, ControlState())
        assertTrue("Helipad landing should award +5000 points", heliHud.totalScore >= 5000)
        assertEquals(StuntBannerType.HELIPAD_LANDING, heliHud.bannerType)
        assertEquals(DistrictZone.AIRPORT, heliHud.district)
    }

    @Test
    fun canyonHeightfieldSpeedTrapAndWallSlideSparksWork() {
        val physics = GameWorldPhysics(CarAudioEngine())

        // Elevated Canyon Mountain Touge check
        val midZ = -1100f
        val midX = CityLayout.canyonRoadX(midZ)
        val midY = CityLayout.queryGroundHeight(midX, 45f, midZ)
        assertTrue("Canyon Mountain Touge should climb above 25m elevation", midY > 25f)
        assertEquals(DistrictZone.CANYON, CityLayout.getDistrict(midX, midZ))

        // Speed trap radar record check
        val trap = CityLayout.speedTraps[0]
        physics.carX = trap.cx
        physics.carZ = trap.cz
        physics.carY = 0f
        physics.vz = 85f // 306 km/h
        val trapHud = physics.step(1f / 60f, ControlState(gas = true))
        assertEquals(StuntBannerType.SPEED_TRAP, trapHud.bannerType)
        assertTrue("First radar trap pass should set New Record", trapHud.isNewRecord)
        assertTrue("Best trap speed should exceed 250 km/h", trapHud.bestTrapSpeedKmh > 250)

        // Wall-slide tangential deflection + spark emission on building scrape
        val b = CityLayout.buildings[0]
        physics.carX = b.cx + b.halfW + 1.0f
        physics.carZ = b.cz
        physics.carY = 0.2f
        physics.vx = -22f
        physics.vz = 40f
        physics.step(1f / 60f, ControlState())
        assertTrue("Wall slide should preserve tangential Z velocity", physics.vz > 25f)
        assertTrue("Wall scrape should emit orange spark particles", physics.sparkParticles.any { it.active })
    }

    @Test
    fun resetCarRecoversFromOceanAndSideCameraReverseWork() {
        val physics = GameWorldPhysics(CarAudioEngine())
        physics.carX = 2400f
        physics.carZ = 2400f
        physics.carY = -3.0f
        physics.step(1f / 60f, ControlState())
        assertTrue("Car should recover onto drivable road", physics.carY >= 0f)
        assertTrue("Car should be back inside peninsula bounds", abs(physics.carX) < CityLayout.WORLD_HALF_SIZE)

        physics.cameraMode = CameraMode.SIDE_ACTION
        val revState = ControlState(brake = true)
        var lastHud = physics.step(1f / 60f, revState)
        repeat(25) {
            lastHud = physics.step(1f / 60f, revState)
        }
        assertEquals(CameraMode.SIDE_ACTION, lastHud.cameraMode)
        assertTrue("Reversing should activate reverse lights", lastHud.isReversing)
        assertEquals("Reversing should engage R gear (0)", 0, lastHud.gear)
        assertTrue(
            "Side-action camera should position laterally off car flank",
            abs(physics.camPosX - physics.carX) > 4.0f
        )
    }

    @Test
    fun showroomPauseTeleportAndAnalogSteeringWork() {
        val physics = GameWorldPhysics(CarAudioEngine(), initialState = GameState.MENU)
        val menuHud = physics.step(1f / 60f, ControlState(gas = true, nitro = true))
        assertEquals(GameState.MENU, menuHud.gameState)
        assertEquals(CityLayout.SHOWROOM_X, physics.carX, 0.01f)
        assertEquals(CityLayout.SHOWROOM_Z, physics.carZ, 0.01f)
        assertEquals(0, menuHud.speedKmh)

        // Teleport to Marina & drive, then Pause should freeze motion
        physics.gameState = GameState.PLAYING
        physics.teleportTo(SpawnLocation.MARINA)
        assertEquals(DistrictZone.MARINA, CityLayout.getDistrict(physics.carX, physics.carZ))

        repeat(30) {
            physics.step(1f / 60f, ControlState(gas = true))
        }
        val beforePauseX = physics.carX
        val beforePauseZ = physics.carZ
        physics.gameState = GameState.PAUSED
        repeat(20) {
            physics.step(1f / 60f, ControlState(gas = true, steerLeft = true))
        }
        assertEquals("Paused game must freeze car X", beforePauseX, physics.carX, 0.0001f)
        assertEquals("Paused game must freeze car Z", beforePauseZ, physics.carZ, 0.0001f)

        // Resume + Analog steering wheel sensitivity check
        physics.gameState = GameState.PLAYING
        physics.applySettings(GameSettings(steeringSensitivity = 1.4f))
        val initialYaw = physics.yaw
        repeat(15) {
            physics.step(1f / 60f, ControlState(gas = true, analogSteer = 0.85f))
        }
        assertTrue("Analog wheel steering left should increase yaw", physics.yaw > initialYaw + 0.1f)
    }
}
