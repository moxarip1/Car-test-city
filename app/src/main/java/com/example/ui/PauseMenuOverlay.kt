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
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.game.SpawnLocation
import com.example.ui.theme.BrakeCrimson
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.DriftAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.PearlText
import com.example.ui.theme.SunsetGold

@Composable
fun PauseMenuOverlay(
    onResume: () -> Unit,
    onResetCar: () -> Unit,
    onTeleport: (SpawnLocation) -> Unit,
    onOpenSettings: () -> Unit,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0x99050812))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .testTag("pause_menu_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, SunsetGold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp)
                    .heightIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.pause_title),
                        color = SunsetGold,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Resume Primary CTA
                        Row(
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(SunsetGold, DriftAmber)))
                                .testTag("pause_resume_button")
                                .clickable { onResume() }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF0A0F1C),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.pause_resume),
                                color = Color(0xFF0A0F1C),
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        PauseActionButton(
                            label = stringResource(R.string.pause_reset),
                            icon = Icons.Default.Refresh,
                            accent = ElectricCyan,
                            tag = "pause_reset_button",
                            modifier = Modifier.weight(1f),
                            onClick = onResetCar
                        )
                    }

                    Text(
                        text = stringResource(R.string.pause_teleport_title),
                        color = MutedSlate,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TeleportChip(
                            label = stringResource(R.string.district_downtown),
                            icon = Icons.Default.LocationCity,
                            accent = SunsetGold,
                            tag = "teleport_downtown_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { onTeleport(SpawnLocation.DOWNTOWN) }
                        )
                        TeleportChip(
                            label = stringResource(R.string.district_marina),
                            icon = Icons.Default.Waves,
                            accent = ElectricCyan,
                            tag = "teleport_marina_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { onTeleport(SpawnLocation.MARINA) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TeleportChip(
                            label = stringResource(R.string.district_canyon),
                            icon = Icons.Default.Landscape,
                            accent = DriftAmber,
                            tag = "teleport_canyon_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { onTeleport(SpawnLocation.CANYON) }
                        )
                        TeleportChip(
                            label = stringResource(R.string.district_airport),
                            icon = Icons.Default.FlightTakeoff,
                            accent = Color(0xFF4ADE80),
                            tag = "teleport_airport_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { onTeleport(SpawnLocation.AIRPORT) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PauseActionButton(
                            label = stringResource(R.string.pause_settings),
                            icon = Icons.Default.Settings,
                            accent = PearlText,
                            tag = "pause_settings_button",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenSettings
                        )
                        PauseActionButton(
                            label = stringResource(R.string.pause_main_menu),
                            icon = Icons.Default.Home,
                            accent = BrakeCrimson,
                            tag = "pause_main_menu_button",
                            modifier = Modifier.weight(1f),
                            onClick = onExitToMenu
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseActionButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .testTag(tag)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = PearlText,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun TeleportChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = PearlText,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
