package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.game.GameSettings
import com.example.game.GraphicsPreset
import com.example.game.ResolutionPreset
import com.example.game.SpeedUnit
import com.example.game.SteeringMode
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.DriftAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.PearlText
import com.example.ui.theme.SunsetGold
import kotlin.math.roundToInt

private enum class SettingsTab {
    GRAPHICS,
    AUDIO,
    CONTROLS
}

@Composable
fun SettingsModal(
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(SettingsTab.GRAPHICS) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xA6050812))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .testTag("settings_modal"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, SunsetGold.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .heightIn(max = 390.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            color = SunsetGold,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Row(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                                .testTag("settings_close_button")
                                .clickable { onClose() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.menu_close),
                                tint = PearlText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.menu_close),
                                color = PearlText,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // 3 Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsTabChip(
                            label = stringResource(R.string.settings_tab_graphics),
                            icon = Icons.Default.DisplaySettings,
                            selected = activeTab == SettingsTab.GRAPHICS,
                            tag = "tab_graphics",
                            modifier = Modifier.weight(1f),
                            onClick = { activeTab = SettingsTab.GRAPHICS }
                        )
                        SettingsTabChip(
                            label = stringResource(R.string.settings_tab_audio),
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            selected = activeTab == SettingsTab.AUDIO,
                            tag = "tab_audio",
                            modifier = Modifier.weight(1f),
                            onClick = { activeTab = SettingsTab.AUDIO }
                        )
                        SettingsTabChip(
                            label = stringResource(R.string.settings_tab_controls),
                            icon = Icons.Default.SportsEsports,
                            selected = activeTab == SettingsTab.CONTROLS,
                            tag = "tab_controls",
                            modifier = Modifier.weight(1f),
                            onClick = { activeTab = SettingsTab.CONTROLS }
                        )
                    }

                    // Scrollable Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeTab) {
                            SettingsTab.GRAPHICS -> GraphicsSettingsTab(
                                settings = settings,
                                onUpdateSettings = onUpdateSettings
                            )

                            SettingsTab.AUDIO -> AudioSettingsTab(
                                settings = settings,
                                onUpdateSettings = onUpdateSettings
                            )

                            SettingsTab.CONTROLS -> ControlsSettingsTab(
                                settings = settings,
                                onUpdateSettings = onUpdateSettings
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphicsSettingsTab(
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingSectionLabel(stringResource(R.string.graphics_preset_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                GraphicsPreset.LOW to stringResource(R.string.preset_low),
                GraphicsPreset.MEDIUM to stringResource(R.string.preset_medium),
                GraphicsPreset.HIGH to stringResource(R.string.preset_high),
                GraphicsPreset.ULTRA to stringResource(R.string.preset_ultra)
            )
            presets.forEach { (preset, label) ->
                OptionSelectChip(
                    label = label,
                    selected = settings.graphicsPreset == preset,
                    accent = SunsetGold,
                    tag = "gfx_preset_${preset.name.lowercase()}",
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdateSettings(settings.copy(graphicsPreset = preset)) }
                )
            }
        }

        SettingSectionLabel(stringResource(R.string.resolution_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val resolutions = listOf(
                ResolutionPreset.LOW_50 to stringResource(R.string.res_50),
                ResolutionPreset.BALANCED_75 to stringResource(R.string.res_75),
                ResolutionPreset.HIGH_100 to stringResource(R.string.res_100),
                ResolutionPreset.NATIVE to stringResource(R.string.res_native)
            )
            resolutions.forEach { (res, label) ->
                OptionSelectChip(
                    label = label,
                    selected = settings.resolutionPreset == res,
                    accent = ElectricCyan,
                    tag = "res_preset_${res.name.lowercase()}",
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdateSettings(settings.copy(resolutionPreset = res)) }
                )
            }
        }

        SettingSwitchRow(
            label = stringResource(R.string.show_fps_label),
            checked = settings.showFps,
            tag = "settings_fps_switch",
            onCheckedChange = { onUpdateSettings(settings.copy(showFps = it)) }
        )
    }
}

@Composable
private fun AudioSettingsTab(
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VolumeSliderRow(
            label = stringResource(R.string.audio_master),
            value = settings.masterVolume,
            accent = SunsetGold,
            tag = "slider_master_volume",
            onValueChange = { onUpdateSettings(settings.copy(masterVolume = it)) }
        )
        VolumeSliderRow(
            label = stringResource(R.string.audio_engine),
            value = settings.engineVolume,
            accent = DriftAmber,
            tag = "slider_engine_volume",
            onValueChange = { onUpdateSettings(settings.copy(engineVolume = it)) }
        )
        VolumeSliderRow(
            label = stringResource(R.string.audio_sfx),
            value = settings.sfxVolume,
            accent = ElectricCyan,
            tag = "slider_sfx_volume",
            onValueChange = { onUpdateSettings(settings.copy(sfxVolume = it)) }
        )
        SettingSwitchRow(
            label = stringResource(R.string.audio_mute_all),
            checked = settings.isMuted,
            tag = "settings_mute_switch",
            onCheckedChange = { onUpdateSettings(settings.copy(isMuted = it)) }
        )
    }
}

@Composable
private fun ControlsSettingsTab(
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingSectionLabel(stringResource(R.string.steering_mode_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OptionSelectChip(
                label = stringResource(R.string.steer_mode_buttons),
                selected = settings.steeringMode == SteeringMode.BUTTONS,
                accent = ElectricCyan,
                tag = "steer_mode_buttons",
                modifier = Modifier.weight(1f),
                onClick = { onUpdateSettings(settings.copy(steeringMode = SteeringMode.BUTTONS)) }
            )
            OptionSelectChip(
                label = stringResource(R.string.steer_mode_wheel),
                selected = settings.steeringMode == SteeringMode.VIRTUAL_WHEEL,
                accent = SunsetGold,
                tag = "steer_mode_wheel",
                modifier = Modifier.weight(1f),
                onClick = { onUpdateSettings(settings.copy(steeringMode = SteeringMode.VIRTUAL_WHEEL)) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.steering_sensitivity_label),
                    color = PearlText,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${String.format("%.2f", settings.steeringSensitivity)}x",
                    color = SunsetGold,
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Slider(
                value = settings.steeringSensitivity,
                onValueChange = { onUpdateSettings(settings.copy(steeringSensitivity = it)) },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = SunsetGold,
                    activeTrackColor = SunsetGold
                ),
                modifier = Modifier.testTag("slider_steering_sensitivity")
            )
        }

        SettingSwitchRow(
            label = stringResource(R.string.drift_assist_label),
            checked = settings.smartDriftAssist,
            tag = "settings_drift_assist_switch",
            onCheckedChange = { onUpdateSettings(settings.copy(smartDriftAssist = it)) }
        )

        SettingSectionLabel(stringResource(R.string.speed_unit_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OptionSelectChip(
                label = stringResource(R.string.speed_unit_kmh),
                selected = settings.speedUnit == SpeedUnit.KMH,
                accent = ElectricCyan,
                tag = "speed_unit_kmh",
                modifier = Modifier.weight(1f),
                onClick = { onUpdateSettings(settings.copy(speedUnit = SpeedUnit.KMH)) }
            )
            OptionSelectChip(
                label = stringResource(R.string.speed_unit_mph),
                selected = settings.speedUnit == SpeedUnit.MPH,
                accent = SunsetGold,
                tag = "speed_unit_mph",
                modifier = Modifier.weight(1f),
                onClick = { onUpdateSettings(settings.copy(speedUnit = SpeedUnit.MPH)) }
            )
        }
    }
}

@Composable
private fun SettingsTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = if (selected) SunsetGold else MutedSlate
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) SunsetGold.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (selected) SunsetGold else HairlineBorder, RoundedCornerShape(14.dp))
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            color = if (selected) PearlText else MutedSlate,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingSectionLabel(text: String) {
    Text(
        text = text,
        color = MutedSlate,
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}

@Composable
private fun OptionSelectChip(
    label: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (selected) accent else HairlineBorder, RoundedCornerShape(12.dp))
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) PearlText else MutedSlate,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun VolumeSliderRow(
    label: String,
    value: Float,
    accent: Color,
    tag: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = PearlText,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "${(value * 100f).roundToInt()}%",
                color = accent,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = PearlText,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A0F1C),
                checkedTrackColor = SunsetGold
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
