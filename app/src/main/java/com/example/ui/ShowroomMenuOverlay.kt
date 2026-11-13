package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CarType
import com.example.game.GameSettings
import com.example.game.HudTelemetry
import com.example.game.LUXURY_PAINT_PRESETS
import com.example.game.PaintFinish
import com.example.game.SavedRecords
import com.example.game.SpawnLocation
import com.example.game.SpeedUnit
import com.example.game.TimeOfDay
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

private enum class ShowroomPanel {
    MAIN,
    SPAWN_SELECT,
    GARAGE
}

@Composable
fun ShowroomMenuOverlay(
    telemetry: HudTelemetry,
    settings: GameSettings,
    records: SavedRecords,
    onLaunchGame: (SpawnLocation) -> Unit,
    onSelectCar: (CarType) -> Unit,
    onSelectPaint: (Int) -> Unit,
    onCycleFinish: () -> Unit,
    onCycleTimeOfDay: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activePanel by remember { mutableStateOf(ShowroomPanel.MAIN) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("showroom_menu_overlay")
        ) {
            // Top bar: Title + Saved Records Badge + Time-of-Day toggle
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SunsetGold.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.app_title_ar),
                            color = SunsetGold,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = telemetry.carType.displayName,
                            color = ElectricCyan,
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RecordsBadge(records = records, speedUnit = settings.speedUnit)

                    val (todIcon, todLabel, todColor) = when (telemetry.timeOfDay) {
                        TimeOfDay.SUNSET -> Triple(Icons.Default.WbTwilight, stringResource(R.string.tod_sunset), DriftAmber)
                        TimeOfDay.DAY -> Triple(Icons.Default.WbSunny, stringResource(R.string.tod_day), SunsetGold)
                        TimeOfDay.NIGHT -> Triple(Icons.Default.Brightness2, stringResource(R.string.tod_night), ElectricCyan)
                    }
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, todColor.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("menu_tod_toggle")
                            .clickable { onCycleTimeOfDay() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = todIcon,
                                contentDescription = todLabel,
                                tint = todColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = todLabel,
                                color = PearlText,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Side Dock Panel keeping the orbiting 3D hypercar framed cleanly in center
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(top = 64.dp)
                    .widthIn(min = 270.dp, max = 350.dp)
            ) {
                AnimatedContent(targetState = activePanel, label = "showroom_panel") { panel ->
                    when (panel) {
                        ShowroomPanel.MAIN -> MainMenuButtonsCard(
                            onPlayClick = { activePanel = ShowroomPanel.SPAWN_SELECT },
                            onGarageClick = { activePanel = ShowroomPanel.GARAGE },
                            onSettingsClick = onOpenSettings
                        )

                        ShowroomPanel.SPAWN_SELECT -> SpawnLocationPickerCard(
                            onSelectSpawn = onLaunchGame,
                            onBack = { activePanel = ShowroomPanel.MAIN }
                        )

                        ShowroomPanel.GARAGE -> GaragePanelCard(
                            telemetry = telemetry,
                            onSelectCar = onSelectCar,
                            onSelectPaint = onSelectPaint,
                            onCycleFinish = onCycleFinish,
                            onBack = { activePanel = ShowroomPanel.MAIN }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsBadge(
    records: SavedRecords,
    speedUnit: SpeedUnit
) {
    val topVal = if (speedUnit == SpeedUnit.MPH) {
        (records.topSpeedKmh * 0.621371f).roundToInt()
    } else {
        records.topSpeedKmh
    }
    val unitText = stringResource(
        if (speedUnit == SpeedUnit.MPH) R.string.speed_unit_mph else R.string.speed_unit_kmh
    )

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.testTag("menu_records_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = DriftAmber,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.best_drift_label),
                        color = MutedSlate,
                        fontFamily = CairoFontFamily,
                        fontSize = 10.sp
                    )
                    Text(
                        text = records.bestDriftScore.toString(),
                        color = SunsetGold,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(HairlineBorder)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.top_speed_label),
                        color = MutedSlate,
                        fontFamily = CairoFontFamily,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "$topVal $unitText",
                        color = ElectricCyan,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MainMenuButtonsCard(
    onPlayClick: () -> Unit,
    onGarageClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Primary Hero Button: ابدأ القيادة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(SunsetGold, DriftAmber)))
                    .testTag("menu_play_button")
                    .clickable { onPlayClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF0A0F1C),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.menu_play),
                        color = Color(0xFF0A0F1C),
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            MenuSecondaryButton(
                label = stringResource(R.string.menu_garage),
                icon = Icons.Default.Garage,
                accent = ElectricCyan,
                tag = "menu_garage_button",
                onClick = onGarageClick
            )

            MenuSecondaryButton(
                label = stringResource(R.string.menu_settings),
                icon = Icons.Default.Settings,
                accent = PearlText,
                tag = "menu_settings_button",
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun MenuSecondaryButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = PearlText,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SpawnLocationPickerCard(
    onSelectSpawn: (SpawnLocation) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, SunsetGold.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("spawn_selector_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PanelHeaderRow(
                title = stringResource(R.string.spawn_choose_title),
                onBack = onBack,
                backTag = "spawn_back_button"
            )

            SpawnOptionRow(
                label = stringResource(R.string.spawn_downtown),
                icon = Icons.Default.LocationCity,
                accent = SunsetGold,
                tag = "spawn_downtown_btn",
                onClick = { onSelectSpawn(SpawnLocation.DOWNTOWN) }
            )
            SpawnOptionRow(
                label = stringResource(R.string.spawn_marina),
                icon = Icons.Default.Waves,
                accent = ElectricCyan,
                tag = "spawn_marina_btn",
                onClick = { onSelectSpawn(SpawnLocation.MARINA) }
            )
            SpawnOptionRow(
                label = stringResource(R.string.spawn_canyon),
                icon = Icons.Default.Landscape,
                accent = DriftAmber,
                tag = "spawn_canyon_btn",
                onClick = { onSelectSpawn(SpawnLocation.CANYON) }
            )
            SpawnOptionRow(
                label = stringResource(R.string.spawn_airport),
                icon = Icons.Default.FlightTakeoff,
                accent = Color(0xFF4ADE80),
                tag = "spawn_airport_btn",
                onClick = { onSelectSpawn(SpawnLocation.AIRPORT) }
            )
        }
    }
}

@Composable
private fun SpawnOptionRow(
    label: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.48f), RoundedCornerShape(14.dp))
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                color = PearlText,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun GaragePanelCard(
    telemetry: HudTelemetry,
    onSelectCar: (CarType) -> Unit,
    onSelectPaint: (Int) -> Unit,
    onCycleFinish: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("garage_panel_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelHeaderRow(
                title = stringResource(R.string.menu_garage),
                onBack = onBack,
                backTag = "garage_back_button"
            )

            // 3 Hypercar Switcher Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GarageCarTile(
                    label = stringResource(R.string.car_ev_hyper_gt),
                    selected = telemetry.carType == CarType.EV_HYPER_GT,
                    accent = ElectricCyan,
                    icon = Icons.Default.ElectricBolt,
                    tag = "garage_car_ev",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectCar(CarType.EV_HYPER_GT) }
                )
                GarageCarTile(
                    label = stringResource(R.string.car_v12_apex),
                    selected = telemetry.carType == CarType.V12_APEX_ROADSTER,
                    accent = DriftAmber,
                    icon = Icons.Default.LocalFireDepartment,
                    tag = "garage_car_v12",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectCar(CarType.V12_APEX_ROADSTER) }
                )
                GarageCarTile(
                    label = stringResource(R.string.car_vip_coupe),
                    selected = telemetry.carType == CarType.VIP_EXECUTIVE_COUPE,
                    accent = SunsetGold,
                    icon = Icons.Default.DirectionsCar,
                    tag = "garage_car_vip",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectCar(CarType.VIP_EXECUTIVE_COUPE) }
                )
            }

            // Animated Car Performance Stat Bars
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatProgressBar(
                    label = stringResource(R.string.stat_top_speed),
                    value = telemetry.carType.statSpeed,
                    color = ElectricCyan
                )
                StatProgressBar(
                    label = stringResource(R.string.stat_acceleration),
                    value = telemetry.carType.statAccel,
                    color = SunsetGold
                )
                StatProgressBar(
                    label = stringResource(R.string.stat_drift_control),
                    value = telemetry.carType.statDrift,
                    color = DriftAmber
                )
            }

            // Paint Swatches + Finish Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LUXURY_PAINT_PRESETS.forEachIndexed { idx, preset ->
                        val selected = telemetry.paintIndex == idx
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(preset.composeColor)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) SunsetGold else HairlineBorder,
                                    shape = CircleShape
                                )
                                .testTag("garage_paint_${preset.id}")
                                .clickable { onSelectPaint(idx) }
                        )
                    }
                }

                val finishLabel = when (telemetry.paintFinish) {
                    PaintFinish.GLOSSY -> stringResource(R.string.finish_glossy)
                    PaintFinish.MATTE -> stringResource(R.string.finish_matte)
                    PaintFinish.CHROME -> stringResource(R.string.finish_chrome)
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SunsetGold.copy(alpha = 0.22f))
                        .border(1.dp, SunsetGold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                        .testTag("garage_finish_toggle")
                        .clickable { onCycleFinish() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
    }
}

@Composable
private fun PanelHeaderRow(
    title: String,
    onBack: () -> Unit,
    backTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = SunsetGold,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .testTag(backTag)
                .clickable { onBack() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.menu_back),
                tint = PearlText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.menu_back),
                color = PearlText,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun GarageCarTile(
    label: String,
    selected: Boolean,
    accent: Color,
    icon: ImageVector,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (selected) accent else HairlineBorder, RoundedCornerShape(12.dp))
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else MutedSlate,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (selected) PearlText else MutedSlate,
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun StatProgressBar(
    label: String,
    value: Float,
    color: Color
) {
    val animated by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 360),
        label = label
    )
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = PearlText.copy(alpha = 0.85f),
                fontFamily = CairoFontFamily,
                fontSize = 11.sp
            )
            Text(
                text = "${(animated * 100f).roundToInt()}",
                color = color,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
