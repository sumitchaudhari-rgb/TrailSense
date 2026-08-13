package com.trailsense.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
fun SafePointsPanelCompose(
    shelterText: String,
    shelterDesc: String,
    waterText: String,
    waterDesc: String,
    exitText: String,
    exitDesc: String,
    onCloseClick: () -> Unit,
    onRouteShelterClick: () -> Unit,
    onRouteWaterClick: () -> Unit,
    onRouteExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚨 Nearest Safe Points",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCloseClick, modifier = Modifier.size(32.dp)) {
                    Text(text = "✖", color = TextSecondary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Shelter Item
            SafePointCardItem(
                title = shelterText.ifEmpty { "🛖 Shelter: --" },
                subtitle = shelterDesc.ifEmpty { "Emergency trail shelter" },
                accentColor = ShelterGreen,
                onRouteClick = onRouteShelterClick
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Water Item
            SafePointCardItem(
                title = waterText.ifEmpty { "💧 Water: --" },
                subtitle = waterDesc.ifEmpty { "Potable drinking water point" },
                accentColor = WaterBlue,
                onRouteClick = onRouteWaterClick
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Exit Item
            SafePointCardItem(
                title = exitText.ifEmpty { "🚪 Exit: --" },
                subtitle = exitDesc.ifEmpty { "Trailhead emergency exit point" },
                accentColor = ExitAmber,
                onRouteClick = onRouteExitClick
            )
        }
    }
}

@Composable
private fun SafePointCardItem(
    title: String,
    subtitle: String,
    accentColor: Color,
    onRouteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Button(
            onClick = onRouteClick,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(text = "Route 🗺️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
