package com.trailsense.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.trailsense.app.ui.theme.TrailSenseTheme

// Header cards composable — rendered in composeHeaderCards ComposeView
@Composable
fun TrailSenseHeaderCards(
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
    onMedicalCardClick: () -> Unit
) {
    TrailSenseTheme {
        Box(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
            TopHeaderCardsCompose(
                gpsStatusText = gpsStatusText,
                latText = latText,
                lonText = lonText,
                nearestSummaryName = nearestSummaryName,
                nearestSummaryDist = nearestSummaryDist,
                waterDist = waterDist,
                shelterDist = shelterDist,
                exitDist = exitDist,
                medicalDist = medicalDist,
                onWaterCardClick = onWaterCardClick,
                onShelterCardClick = onShelterCardClick,
                onExitCardClick = onExitCardClick,
                onMedicalCardClick = onMedicalCardClick
            )
        }
    }
}

// Chat sheet composable — rendered in composeChatSheet ComposeView
@Composable
fun TrailSenseChatSheet(
    chatMessages: List<ChatMessage>,
    isThinking: Boolean,
    isListening: Boolean,
    onSendQuery: (String) -> Unit,
    onMicClick: () -> Unit,
    onChipClick: (String) -> Unit
) {
    TrailSenseTheme {
        Box(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
            ChatScreenCompose(
                chatMessages = chatMessages,
                isThinking = isThinking,
                isListening = isListening,
                onSendQuery = onSendQuery,
                onMicClick = onMicClick,
                onChipClick = onChipClick
            )
        }
    }
}

// Legacy compatibility shim — kept so MainActivityComposeBridge compiles
@Composable
fun TrailSenseAppScreen(
    mapView: org.osmdroid.views.MapView?,
    gpsStatusText: String,
    latText: String,
    lonText: String,
    nearestSummaryName: String,
    nearestSummaryDist: String,
    waterDist: String,
    shelterDist: String,
    exitDist: String,
    medicalDist: String,
    chatMessages: List<ChatMessage>,
    isThinking: Boolean,
    isListening: Boolean,
    onWaterCardClick: () -> Unit,
    onShelterCardClick: () -> Unit,
    onExitCardClick: () -> Unit,
    onMedicalCardClick: () -> Unit,
    onSendQuery: (String) -> Unit,
    onMicClick: () -> Unit,
    onChipClick: (String) -> Unit,
    onLocationFabClick: () -> Unit
) {
    // Not used — split into TrailSenseHeaderCards and TrailSenseChatSheet
}
