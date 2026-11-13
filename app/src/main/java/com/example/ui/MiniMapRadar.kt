package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CityLayout
import com.example.game.DistrictZone
import com.example.ui.theme.BrakeCrimson
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.DriftAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PearlText
import com.example.ui.theme.SunsetGold

@Composable
fun MiniMapRadar(
    carX: Float,
    carZ: Float,
    carYaw: Float,
    district: DistrictZone,
    radarSize: Dp = 94.dp,
    modifier: Modifier = Modifier
) {
    val districtLabel = when (district) {
        DistrictZone.DOWNTOWN -> stringResource(R.string.district_downtown)
        DistrictZone.MARINA -> stringResource(R.string.district_marina)
        DistrictZone.CANYON -> stringResource(R.string.district_canyon)
        DistrictZone.AIRPORT -> stringResource(R.string.district_airport)
    }

    Column(
        modifier = modifier.testTag("minimap_radar"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(radarSize)
                .clip(CircleShape)
                .background(ObsidianCard)
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(radarSize)) {
                val r = size.minDimension * 0.5f
                val cx = r
                val cy = r
                val radarRangeMeters = 950f
                val scale = r / radarRangeMeters

                fun toRadar(wx: Float, wz: Float): Offset {
                    // World +X = West/Left in yaw=0 forward(+Z); screen +X is East(-X world), +Y is South(-Z world)
                    // Keeping consistent intuitive North-up radar where +Z is Up (-Y screen) and +X is Left (-X screen) or Right:
                    // Let's map world (+Z = North/Up, -X = Right/East? Wait: in our coord frame, yaw=0 faces +Z, steerLeft increments yaw -> +X is Left!)
                    // Wait: if +X is Left and +Z is Up, then:
                    // screenX = cx - (wx - carX) * scale
                    // screenY = cy - (wz - carZ) * scale
                    return Offset(
                        x = cx - (wx - carX) * scale,
                        y = cy - (wz - carZ) * scale
                    )
                }

                // Ocean east of Marina coastline (wx > 840)
                val coastX = cx - (CityLayout.EAST_COAST_X - carX) * scale
                if (coastX > 0f) {
                    drawRect(
                        color = Color(0xFF0A4D68).copy(alpha = 0.65f),
                        topLeft = Offset(0f, 0f),
                        size = Size(coastX.coerceAtMost(size.width), size.height)
                    )
                }

                // Subtle radar range rings
                drawCircle(
                    color = HairlineBorder,
                    radius = r * 0.55f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Highways: North-South (x=0), East-West (z=0), Marina (x=580), Downtown Ring (±360)
                val roadColor = Color(0xFF94A3B8).copy(alpha = 0.65f)
                val roadStroke = 3.2.dp.toPx()

                drawLine(roadColor, toRadar(0f, -1880f), toRadar(0f, 1880f), strokeWidth = roadStroke)
                drawLine(roadColor, toRadar(-1880f, 0f), toRadar(800f, 0f), strokeWidth = roadStroke)
                drawLine(roadColor, toRadar(580f, -1650f), toRadar(580f, 1650f), strokeWidth = roadStroke)
                drawLine(roadColor, toRadar(-360f, -360f), toRadar(360f, -360f), strokeWidth = 2.dp.toPx())
                drawLine(roadColor, toRadar(-360f, 360f), toRadar(360f, 360f), strokeWidth = 2.dp.toPx())
                drawLine(roadColor, toRadar(-360f, -360f), toRadar(-360f, 360f), strokeWidth = 2.dp.toPx())
                drawLine(roadColor, toRadar(360f, -360f), toRadar(360f, 360f), strokeWidth = 2.dp.toPx())

                // Airport Runway rectangle (x in -1320..-1040, z in 420..1680)
                val rwA = toRadar(-1040f, 1680f)
                val rwB = toRadar(-1320f, 420f)
                drawRect(
                    color = Color(0xFFCBD5E1).copy(alpha = 0.35f),
                    topLeft = Offset(minOf(rwA.x, rwB.x), minOf(rwA.y, rwB.y)),
                    size = Size(kotlin.math.abs(rwB.x - rwA.x), kotlin.math.abs(rwB.y - rwA.y))
                )

                // Canyon Touge winding polyline
                var prevCanyon = toRadar(CityLayout.CANYON_FEEDER_X, -280f)
                for (zStep in -380 downTo -1680 step 100) {
                    val cur = toRadar(CityLayout.canyonRoadX(zStep.toFloat()), zStep.toFloat())
                    drawLine(DriftAmber.copy(alpha = 0.72f), prevCanyon, cur, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    prevCanyon = cur
                }

                // Grand Central Roundabout ring
                val centerPt = toRadar(0f, 0f)
                if ((centerPt - Offset(cx, cy)).getDistance() < r * 1.2f) {
                    drawCircle(
                        color = SunsetGold.copy(alpha = 0.8f),
                        radius = 48f * scale,
                        center = centerPt,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Stunt Ramps & Aerial Boost Rings (Gold dots)
                for (ring in CityLayout.boostRings) {
                    val pt = toRadar(ring.cx, ring.cz)
                    if ((pt - Offset(cx, cy)).getDistance() <= r - 4f) {
                        drawCircle(color = SunsetGold, radius = 3.2.dp.toPx(), center = pt)
                    }
                }

                // Speed Trap Radar Gates (Crimson dots)
                for (trap in CityLayout.speedTraps) {
                    val pt = toRadar(trap.cx, trap.cz)
                    if ((pt - Offset(cx, cy)).getDistance() <= r - 4f) {
                        drawCircle(color = BrakeCrimson, radius = 2.8.dp.toPx(), center = pt)
                    }
                }

                // Player Directional Arrow (yaw=0 points Up -> 0 deg; yaw > 0 rotates left/CCW in screen coords)
                val yawDeg = -Math.toDegrees(carYaw.toDouble()).toFloat()
                rotate(degrees = yawDeg, pivot = Offset(cx, cy)) {
                    val arrow = Path().apply {
                        moveTo(cx, cy - 8.5.dp.toPx())
                        lineTo(cx + 6.dp.toPx(), cy + 6.5.dp.toPx())
                        lineTo(cx, cy + 3.dp.toPx())
                        lineTo(cx - 6.dp.toPx(), cy + 6.5.dp.toPx())
                        close()
                    }
                    drawPath(arrow, color = ElectricCyan)
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = districtLabel,
                color = PearlText,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}
