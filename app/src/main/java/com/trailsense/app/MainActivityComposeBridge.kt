package com.trailsense.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.trailsense.app.ui.ChatMessage
import com.trailsense.app.ui.TrailSenseHeaderCards
import com.trailsense.app.ui.TrailSenseChatSheet
import com.trailsense.app.ui.TrailSenseAppScreen
import org.osmdroid.views.MapView

class MainActivityComposeBridge(
    private val activity: MainActivity,
    private val headerCardsView: ComposeView,
    private val chatSheetView: ComposeView
) {
    var gpsStatusText by mutableStateOf("Location locked")
    var latText by mutableStateOf("--°")
    var lonText by mutableStateOf("--°")

    var nearestSummaryName by mutableStateOf("Calculating...")
    var nearestSummaryDist by mutableStateOf("--")

    var waterDist by mutableStateOf("--")
    var shelterDist by mutableStateOf("--")
    var exitDist by mutableStateOf("--")
    var medicalDist by mutableStateOf("--")

    val chatMessages = mutableStateListOf<ChatMessage>()
    var chatOutputText by mutableStateOf("")
    var isThinking by mutableStateOf(false)
    var isListening by mutableStateOf(false)
    var showSafePointsModal by mutableStateOf(false)

    fun addChatMessage(text: String, isUser: Boolean) {
        chatMessages.add(ChatMessage(text = text, isUser = isUser))
    }

    fun initComposeUI(mapView: MapView?) {
        // 1. Header cards (Status, Lat/Lon, Nearest + 2x2 grid)
        headerCardsView.setContent {
            TrailSenseHeaderCards(
                gpsStatusText = gpsStatusText,
                latText = latText,
                lonText = lonText,
                nearestSummaryName = nearestSummaryName,
                nearestSummaryDist = nearestSummaryDist,
                waterDist = waterDist,
                shelterDist = shelterDist,
                exitDist = exitDist,
                medicalDist = medicalDist,
                onWaterCardClick = {
                    activity.routeToNearestWaterFromCompose()
                },
                onShelterCardClick = {
                    activity.routeToNearestShelterFromCompose()
                },
                onExitCardClick = {
                    activity.routeToNearestExitFromCompose()
                },
                onMedicalCardClick = {
                    activity.routeToNearestExitFromCompose()
                }
            )
        }

        // 2. Chat / AI guide sheet
        chatSheetView.setContent {
            TrailSenseChatSheet(
                chatMessages = chatMessages,
                isThinking = isThinking,
                isListening = isListening,
                onSendQuery = { query ->
                    addChatMessage(query, isUser = true)
                    activity.setChatInputQuery(query)
                    activity.submitGroundedLlmQuery()
                },
                onMicClick = {
                    activity.checkAudioPermissionAndListen()
                },
                onChipClick = { promptText ->
                    addChatMessage(promptText, isUser = true)
                    activity.setChatInputQuery(promptText)
                    activity.submitGroundedLlmQuery()
                }
            )
        }
    }
}
