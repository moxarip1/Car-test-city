package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.game.CameraMode
import com.example.game.CarType
import com.example.game.GameSettings
import com.example.game.HudTelemetry
import com.example.game.LUXURY_PAINT_PRESETS
import com.example.game.PaintFinish
import com.example.game.SpeedUnit
import com.example.game.SteeringMode
import com.example.game.StuntBannerType
import com.example.game.TimeOfDay
import com.example.ui.theme.BrakeCrimson
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.DriftAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.PearlText
import com.example.ui.theme.SunsetGold
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow

@Composable
fun GameHudOverlay(
    telemetryFlow: StateFlow<HudTelemetry>,
    settingsFlow: StateFlow<GameSettings>,
    isFullscreenFlow: StateFlow<Boolean>,
    onPauseGame: () -> Unit,
    onSelectCar: (CarType) -> Unit,
    onSelectPaint: (Int) -> Unit,
    onCycleFinish: () -> Unit,
    onCycleCamera: () -> Unit,
    onCycleTimeOfDay: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleMute: () -> Unit,
    onResetCar: () -> Unit,
    onTouchGas: (Boolean) -> Unit,
    onTouchBrake: (Boolean) -> Unit,
    onTouchSteerLeft: (Boolean) -> Unit,
    onTouchSteerRight: (Boolean) -> Unit,
    onTouchAnalogSteer: (Float) -> Unit,
    onTouchHandbrake: (Boolean) -> Unit,
    onTouchNitro: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry by telemetryFlow.collectAsStateWithLifecycle()
    val settings by settingsFlow.collectAsStateWithLifecycle()
    val isFullscreen by isFullscreenFlow.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 740.dp

        // Immersive Nitro / High-Speed Edge Lines
        if (telemetry.isNitroActive || telemetry.isBoosting || telemetry.speedKmh > 255) {
            SpeedLinesOverlay(isNitro = telemetry.isNitroActive)
        }

        // Arabic RTL Top Bar + Speedometer, Stunt Banners & Mini-Map Radar
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TopBarRow(
                        telemetry = telemetry,
                        showFps = settings.showFps,
                        isFullscreen = isFullscreen,
                        isCompact = isCompactWidth,
                        onPauseGame = onPauseGame,
                        onSelectCar = onSelectCar,
                        onSelectPaint = onSelectPaint,
                        onCycleFinish = onCycleFinish,
                        onCycleCamera = onCycleCamera,
                        onCycleTimeOfDay = onCycleTimeOfDay,
                        onToggleFullscreen = onToggleFullscreen,
                        onToggleMute = onToggleMute,
                        onResetCar = onResetCar
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        SpeedometerCard(telemetry = telemetry, speedUnit = settings.speedUnit)

                        StuntEventBanner(telemetry = telemetry, speedUnit = settings.speedUnit)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            DriftScoreCard(telemetry = telemetry)
                            MiniMapRadar(
                                carX = telemetry.carX,
                                carZ = telemetry.carZ,
                                carYaw = telemetry.carYaw,
                                district = telemetry.district,
                                radarSize = if (isCompactWidth) 78.dp else 94.dp
                            )
                        }
                    }
                }
            }
        }

        // Ergonomic Landscape Bottom Pedals (Physical Left = Steering, Physical Right = Nitro + Drift + Brake + Gas)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (settings.steeringMode == SteeringMode.VIRTUAL_WHEEL) {
                    VirtualSteeringWheel(
                        size = if (isCompactWidth) 136.dp else 156.dp,
                        onAnalogSteerChanged = onTouchAnalogSteer
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SteerTouchButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDesc = stringResource(R.string.steer_left_desc),
                            tag = "steer_left_button",
                            size = if (isCompactWidth) 74.dp else 84.dp,
                            onPressedChanged = onTouchSteerLeft
                        )
                        SteerTouchButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDesc = stringResource(R.string.steer_right_desc),
                            tag = "steer_right_button",
                            size = if (isCompactWidth) 74.dp else 84.dp,
                            onPressedChanged = onTouchSteerRight
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    NitroPedalButton(
                        label = stringResource(R.string.pedal_nitro),
                        charge = telemetry.nitroCharge,
                        isActive = telemetry.isNitroActive,
                        width = if (isCompactWidth) 66.dp else 76.dp,
                        height = if (isCompactWidth) 66.dp else 74.dp,
                        onPressedChanged = onTouchNitro
                    )
                    PedalTouchButton(
                        label = stringResource(R.string.pedal_drift),
                        accent = DriftAmber,
                        tag = "handbrake_pedal",
                        width = if (isCompactWidth) 64.dp else 74.dp,
                        height = if (isCompactWidth) 66.dp else 74.dp,
                        onPressedChanged = onTouchHandbrake
                    )
                    PedalTouchButton(
                        label = stringResource(R.string.pedal_brake),
                        accent = BrakeCrimson,
                        tag = "brake_pedal",
                        width = if (isCompactWidth) 66.dp else 76.dp,
                        height = if (isCompactWidth) 72.dp else 82.dp,
                        onPressedChanged = onTouchBrake
                    )
                    PedalTouchButton(
                        label = stringResource(R.string.pedal_gas),
                        accent = ElectricCyan,
                        tag = "gas_pedal",
                        width = if (isCompactWidth) 74.dp else 86.dp,
                        height = if (isCompactWidth) 88.dp else 102.dp,
                        onPressedChanged = onTouchGas
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedLinesOverlay(isNitro: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val lineCol = if (isNitro) ElectricCyan.copy(alpha = 0.28f) else SunsetGold.copy(alpha = 0.20f)
        val stroke = if (isNitro) 3.dp.toPx() else 2.dp.toPx()
        for (i in 0..7) {
            val yRatio = (i + 0.5f) / 8f
            val y = h * yRatio
            val len = w * (0.06f + (i % 3) * 0.03f)
            drawLine(lineCol, Offset(0f, y), Offset(len, y + (yRatio - 0.5f) * 28f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(lineCol, Offset(w, y), Offset(w - len, y + (yRatio - 0.5f) * 28f), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun TopBarRow(
    telemetry: HudTelemetry,
    showFps: Boolean,
    isFullscreen: Boolean,
    isCompact: Boolean,
    onPauseGame: () -> Unit,
    onSelectCar: (CarType) -> Unit,
    onSelectPaint: (Int) -> Unit,
    onCycleFinish: () -> Unit,
    onCycleCamera: () -> Unit,
    onCycleTimeOfDay: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleMute: () -> Unit,
    onResetCar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPillContainer {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPillButton(
                    icon = Icons.Default.Pause,
                    label = stringResource(R.string.pause_button),
                    accent = SunsetGold,
                    tag = "pause_game_button",
                    onClick = onPauseGame
                )
                if (showFps) {
                    Text(
                        text = "${telemetry.fps} ${stringResource(R.string.fps_label)}",
                        color = ElectricCyan,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .testTag("fps_counter_badge")
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
        GlassPillContainer {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarSwitchChip(
                    label = stringResource(R.string.car_ev_hyper_gt),
                    selected = telemetry.carType == CarType.EV_HYPER_GT,
                    accent = ElectricCyan,
                    icon = Icons.Default.ElectricBolt,
                    tag = "car_switch_ev",
                    onClick = { onSelectCar(CarType.EV_HYPER_GT) }
                )
                CarSwitchChip(
                    label = stringResource(R.string.car_v12_apex),
                    selected = telemetry.carType == CarType.V12_APEX_ROADSTER,
                    accent = DriftAmber,
                    icon = Icons.Default.LocalFireDepartment,
                    tag = "car_switch_v12",
                    onClick = { onSelectCar(CarType.V12_APEX_ROADSTER) }
                )
                CarSwitchChip(
                    label = stringResource(R.string.car_vip_coupe),
                    selected = telemetry.carType == CarType.VIP_EXECUTIVE_COUPE,
                    accent = SunsetGold,
                    icon = Icons.Default.DirectionsCar,
                    tag = "car_switch_vip",
                    onClick = { onSelectCar(CarType.VIP_EXECUTIVE_COUPE) }
                )
            }
        }

        GlassPillContainer {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LUXURY_PAINT_PRESETS.forEachIndexed { index, preset ->
                    val selected = telemetry.paintIndex == index
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 22.dp else 25.dp)
                            .clip(CircleShape)
                            .background(preset.composeColor)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) SunsetGold else HairlineBorder,
                                shape = CircleShape
                            )
                            .testTag("paint_swatch_${preset.id}")
                            .clickable { onSelectPaint(index) }
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                val finishLabel = when (telemetry.paintFinish) {
                    PaintFinish.GLOSSY -> stringResource(R.string.finish_glossy)
                    PaintFinish.MATTE -> stringResource(R.string.finish_matte)
                    PaintFinish.CHROME -> stringResource(R.string.finish_chrome)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(SunsetGold.copy(alpha = 0.22f))
                        .border(1.dp, SunsetGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .testTag("paint_finish_toggle")
                        .clickable { onCycleFinish() }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = finishLabel,
                        color = SunsetGold,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        GlassPillContainer {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (todIcon, todLabel, todAccent) = when (telemetry.timeOfDay) {
                    TimeOfDay.SUNSET -> Triple(Icons.Default.WbTwilight, stringResource(R.string.tod_sunset), DriftAmber)
                    TimeOfDay.DAY -> Triple(Icons.Default.WbSunny, stringResource(R.string.tod_day), SunsetGold)
                    TimeOfDay.NIGHT -> Triple(Icons.Default.Brightness2, stringResource(R.string.tod_night), ElectricCyan)
                }
                ActionPillButton(
                    icon = todIcon,
                    label = todLabel,
                    accent = todAccent,
                    tag = "tod_toggle_button",
                    onClick = onCycleTimeOfDay
                )

                val camLabel = when (telemetry.cameraMode) {
                    CameraMode.CHASE -> stringResource(R.string.cam_chase)
                    CameraMode.SIDE_ACTION -> stringResource(R.string.cam_side)
                    CameraMode.HOOD -> stringResource(R.string.cam_hood)
                    CameraMode.ORBIT -> stringResource(R.string.cam_orbit)
                }
                ActionPillButton(
                    icon = Icons.Default.Videocam,
                    label = camLabel,
                    accent = ElectricCyan,
                    tag = "camera_toggle_button",
                    onClick = onCycleCamera
                )
                ActionIconButton(
                    icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDesc = stringResource(if (isFullscreen) R.string.action_exit_fullscreen else R.string.action_fullscreen),
                    tint = if (isFullscreen) ElectricCyan else PearlText,
                    tag = "fullscreen_toggle_button",
                    onClick = onToggleFullscreen
                )
                ActionIconButton(
                    icon = if (telemetry.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDesc = stringResource(if (telemetry.isMuted) R.string.action_unmute else R.string.action_mute),
                    tint = if (telemetry.isMuted) BrakeCrimson else PearlText,
                    tag = "mute_toggle_button",
                    onClick = onToggleMute
                )
                ActionPillButton(
                    icon = Icons.Default.Refresh,
                    label = stringResource(R.string.action_reset_car),
                    accent = SunsetGold,
                    tag = "reset_car_button",
                    onClick = onResetCar
                )
            }
        }
    }
}

@Composable
private fun GlassPillContainer(content: @Composable () -> Unit) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder)
    ) {
        Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)) {
            content()
        }
    }
}

@Composable
private fun CarSwitchChip(
    label: String,
    selected: Boolean,
    accent: Color,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    val bg = if (selected) accent.copy(alpha = 0.24f) else Color.Transparent
    val borderCol = if (selected) accent else Color.Transparent
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else MutedSlate,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = if (selected) PearlText else MutedSlate,
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ActionPillButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = PearlText,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDesc: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .testTag(tag)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun SpeedometerCard(
    telemetry: HudTelemetry,
    speedUnit: SpeedUnit
) {
    val displaySpeed = if (speedUnit == SpeedUnit.MPH) {
        (telemetry.speedKmh * 0.621371f).roundToInt()
    } else {
        telemetry.speedKmh
    }
    val unitLabel = stringResource(
        if (speedUnit == SpeedUnit.MPH) R.string.speed_unit_mph else R.string.speed_unit_kmh
    )
    val accent = when {
        telemetry.isNitroActive || telemetry.isBoosting -> ElectricCyan
        telemetry.speedKmh > 220 -> SunsetGold
        else -> PearlText
    }
    val rpmColor = when {
        telemetry.rpmNormalized > 0.85f -> BrakeCrimson
        telemetry.rpmNormalized > 0.62f -> DriftAmber
        else -> ElectricCyan
    }
    val gearText = if (telemetry.gear == 0) "R" else "G${telemetry.gear}"

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (telemetry.isNitroActive || telemetry.isBoosting) ElectricCyan else HairlineBorder
        ),
        modifier = Modifier.testTag("speedometer_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = SunsetGold,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(bottom = 3.dp)
                )
                Text(
                    text = displaySpeed.toString(),
                    color = accent,
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 27.sp
                )
                Text(
                    text = unitLabel,
                    color = MutedSlate,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rpmColor.copy(alpha = 0.22f))
                        .border(1.dp, rpmColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = gearText,
                        color = rpmColor,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Live 6-speed RPM Tachometer Bar
            Box(
                modifier = Modifier
                    .width(142.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(telemetry.rpmNormalized.coerceIn(0.05f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(ElectricCyan, DriftAmber, rpmColor)))
                )
            }
        }
    }
}

@Composable
private fun DriftScoreCard(telemetry: HudTelemetry) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (telemetry.isDrifting) DriftAmber else HairlineBorder
        ),
        modifier = Modifier.testTag("drift_score_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.total_score_label),
                    color = MutedSlate,
                    fontFamily = CairoFontFamily,
                    fontSize = 11.sp
                )
                Text(
                    text = telemetry.totalScore.toString(),
                    color = SunsetGold,
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            if (telemetry.bestTrapSpeedKmh > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = stringResource(R.string.radar_speed_trap),
                        color = MutedSlate,
                        fontFamily = CairoFontFamily,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${telemetry.bestTrapSpeedKmh}",
                        color = ElectricCyan,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            if (telemetry.activeDriftScore > 0 || telemetry.isDrifting) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.drift_score_label),
                        color = DriftAmber,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "+${telemetry.activeDriftScore}",
                        color = PearlText,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DriftAmber)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "x${telemetry.driftMultiplier}",
                            color = ObsidianSurface,
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StuntEventBanner(
    telemetry: HudTelemetry,
    speedUnit: SpeedUnit
) {
    val trapVal = if (speedUnit == SpeedUnit.MPH) {
        (telemetry.lastTrapSpeedKmh * 0.621371f).roundToInt()
    } else {
        telemetry.lastTrapSpeedKmh
    }
    val unitStr = stringResource(
        if (speedUnit == SpeedUnit.MPH) R.string.speed_unit_mph else R.string.speed_unit_kmh
    )
    val bannerText = when (telemetry.bannerType) {
        StuntBannerType.NONE -> if (telemetry.inDriftZone && telemetry.isDrifting) {
            stringResource(R.string.drift_zone_bonus)
        } else null
        StuntBannerType.AIRTIME -> stringResource(R.string.stunt_airtime)
        StuntBannerType.HELIPAD_LANDING -> stringResource(R.string.stunt_helipad)
        StuntBannerType.TURBO_BOOST -> stringResource(R.string.stunt_boost)
        StuntBannerType.BOOST_RING -> stringResource(R.string.stunt_ring)
        StuntBannerType.SPEED_TRAP -> "${stringResource(R.string.radar_speed_trap)}: $trapVal $unitStr"
        StuntBannerType.DRIFT_ZONE -> stringResource(R.string.drift_zone_bonus)
    }
    AnimatedVisibility(
        visible = bannerText != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        if (bannerText != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Surface(
                    color = if (telemetry.bannerType == StuntBannerType.SPEED_TRAP) ElectricCyan else SunsetGold.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = bannerText,
                        color = Color(0xFF0A0F1C),
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
                if (telemetry.bannerType == StuntBannerType.SPEED_TRAP && telemetry.isNewRecord) {
                    Surface(
                        color = BrakeCrimson,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.new_record_badge),
                            color = PearlText,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SteerTouchButton(
    icon: ImageVector,
    contentDesc: String,
    tag: String,
    size: Dp,
    onPressedChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val bg = if (pressed) ElectricCyan.copy(alpha = 0.42f) else ObsidianCard
    val borderCol = if (pressed) ElectricCyan else HairlineBorder

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, borderCol, CircleShape)
            .testTag(tag)
            .semantics { contentDescription = contentDesc }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyDown = event.changes.any { it.pressed }
                        if (anyDown != pressed) {
                            pressed = anyDown
                            onPressedChanged(anyDown)
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (pressed) PearlText else SunsetGold,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

@Composable
private fun NitroPedalButton(
    label: String,
    charge: Float,
    isActive: Boolean,
    width: Dp,
    height: Dp,
    onPressedChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val ratio = (charge / 100f).coerceIn(0f, 1f)
    val accent = if (isActive || pressed) Color(0xFF38BDF8) else ElectricCyan

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(18.dp))
            .background(if (pressed || isActive) accent.copy(alpha = 0.36f) else ObsidianCard)
            .border(1.5.dp, if (pressed || isActive) accent else accent.copy(alpha = 0.52f), RoundedCornerShape(18.dp))
            .testTag("nitro_pedal")
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyDown = event.changes.any { it.pressed }
                        if (anyDown != pressed) {
                            pressed = anyDown
                            onPressedChanged(anyDown)
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                color = if (pressed || isActive) PearlText else accent,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent)
                )
            }
        }
    }
}

@Composable
private fun PedalTouchButton(
    label: String,
    accent: Color,
    tag: String,
    width: Dp,
    height: Dp,
    onPressedChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val brush = if (pressed) {
        Brush.verticalGradient(listOf(accent.copy(alpha = 0.65f), accent.copy(alpha = 0.25f)))
    } else {
        Brush.verticalGradient(listOf(ObsidianCard, ObsidianSurface))
    }

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(18.dp))
            .background(brush)
            .border(1.5.dp, if (pressed) accent else accent.copy(alpha = 0.52f), RoundedCornerShape(18.dp))
            .testTag(tag)
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyDown = event.changes.any { it.pressed }
                        if (anyDown != pressed) {
                            pressed = anyDown
                            onPressedChanged(anyDown)
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (pressed) PearlText else accent,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
