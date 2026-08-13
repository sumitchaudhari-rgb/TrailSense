package com.trailsense.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trailsense.app.ui.theme.*

@Composable
fun TopHeaderCardsCompose(
    gpsStatusText: String,
    latText: String,
    lonText: String,
    nearestSummaryName: String,
    nearestSummaryDist: String,
    waterDist: String,
    shelterDist: String,
    exitDist: String,
    medicalDist: String,
    onWaterCardClick: () -> Unit,
    onShelterCardClick: () -> Unit,
    onExitCardClick: () -> Unit,
    onMedicalCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Status Bar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🟢 ", fontSize = 12.sp)
                Text(
                    text = gpsStatusText.ifEmpty { "Location locked" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Offline mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }
        }

        // 2. Lat/Long Readout Card (2 Columns)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Latitude", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = latText.ifEmpty { "20.036104°" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Longitude", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lonText.ifEmpty { "73.802842°" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        // 3. Nearest Summary & 2x2 Quick Access Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📍 ", fontSize = 13.sp)
                    Text(
                        text = "Nearest: " + nearestSummaryName.ifEmpty { "Trailhead shelter" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = nearestSummaryDist.ifEmpty { "205 m" },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2x2 Grid Row 1 (Water | Shelter)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryQuickCard(
                        icon = "💧",
                        label = "Water",
                        distance = waterDist.ifEmpty { "235 m" },
                        accentColor = WaterBlue,
                        onClick = onWaterCardClick,
                        modifier = Modifier.weight(1f)
                    )
                    CategoryQuickCard(
                        icon = "🛖",
                        label = "Shelter",
                        distance = shelterDist.ifEmpty { "205 m" },
                        accentColor = ShelterGreen,
                        onClick = onShelterCardClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2x2 Grid Row 2 (Exit | Medical)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryQuickCard(
                        icon = "🚪",
                        label = "Exit",
                        distance = exitDist.ifEmpty { "438 m" },
                        accentColor = ExitAmber,
                        onClick = onExitCardClick,
                        modifier = Modifier.weight(1f)
                    )
                    CategoryQuickCard(
                        icon = "🧰",
                        label = "Medical",
                        distance = medicalDist.ifEmpty { "612 m" },
                        accentColor = MedicalRed,
                        onClick = onMedicalCardClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryQuickCard(
    icon: String,
    label: String,
    distance: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = distance,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
