package com.trailsense.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trailsense.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreenCompose(
    chatMessages: List<ChatMessage>,
    isThinking: Boolean,
    isListening: Boolean,
    onSendQuery: (String) -> Unit,
    onMicClick: () -> Unit,
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showErrorHint by remember { mutableStateOf(false) }

    // Scroll state for the chat message history box
    val chatScrollState = rememberScrollState()

    // Auto-scroll to bottom whenever new messages arrive
    LaunchedEffect(chatMessages.size, isThinking) {
        chatScrollState.animateScrollTo(chatScrollState.maxValue)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // ── Header Row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💬 ", fontSize = 14.sp)
                Text(
                    text = "TrailSense guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = TagGreenBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Offline AI",
                        color = TagGreenText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Suggestion Chips (horizontal scroll) ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = { showErrorHint = false; onChipClick("How far is the nearest shelter?") },
                    label = { Text("Nearest shelter?", color = TextPrimary, fontSize = 11.sp) },
                    border = BorderStroke(1.dp, CardBorder)
                )
                SuggestionChip(
                    onClick = { showErrorHint = false; onChipClick("Where can I find drinking water?") },
                    label = { Text("Water source?", color = TextPrimary, fontSize = 11.sp) },
                    border = BorderStroke(1.dp, CardBorder)
                )
                SuggestionChip(
                    onClick = { showErrorHint = false; onChipClick("Where is the nearest emergency exit?") },
                    label = { Text("Emergency exit?", color = TextPrimary, fontSize = 11.sp) },
                    border = BorderStroke(1.dp, CardBorder)
                )
                SuggestionChip(
                    onClick = { showErrorHint = false; onChipClick("Is this trail safe to continue?") },
                    label = { Text("Trail safe?", color = TextPrimary, fontSize = 11.sp) },
                    border = BorderStroke(1.dp, CardBorder)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // ── Chat History + Arrow Scroll Buttons ───────────────────────
            val scope = rememberCoroutineScope()
            val scrollAmount = 300  // pixels to scroll per button tap

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chat message box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 120.dp, max = 260.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF8F9FA))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(chatScrollState)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (chatMessages.isEmpty()) {
                            AiChatBubble(text = "Hello. I'm your offline trail guide. Ask about shelters, water points, or emergency exits.")
                        } else {
                            chatMessages.forEach { msg ->
                                if (msg.isUser) UserChatBubble(text = msg.text)
                                else AiChatBubble(text = msg.text)
                            }
                        }

                        if (isThinking) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = WaterBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generating answer...",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // ▲ / ▼ Arrow scroll buttons stacked vertically
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ▲ Scroll Up button
                    FilledIconButton(
                        onClick = {
                            scope.launch {
                                chatScrollState.animateScrollTo(
                                    (chatScrollState.value - scrollAmount).coerceAtLeast(0)
                                )
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = ShelterGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("▲", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // ▼ Scroll Down button
                    FilledIconButton(
                        onClick = {
                            scope.launch {
                                chatScrollState.animateScrollTo(
                                    (chatScrollState.value + scrollAmount)
                                        .coerceAtMost(chatScrollState.maxValue)
                                )
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = ShelterGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("▼", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            // ── Error Hint ─────────────────────────────────────────────────
            if (showErrorHint) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please enter a question before tapping Ask.",
                    color = MedicalRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Input Row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        if (showErrorHint && it.isNotBlank()) showErrorHint = false
                    },
                    placeholder = {
                        Text("Ask about the trail ahead", color = TextSecondary, fontSize = 12.sp)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        focusedBorderColor = WaterBlue,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Mic button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isListening) MedicalRed else Color(0xFFF0F0F0),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Text(text = if (isListening) "🔴" else "🎙️", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Ask button
                Button(
                    onClick = {
                        if (inputText.isBlank()) {
                            showErrorHint = true
                        } else {
                            showErrorHint = false
                            onSendQuery(inputText)
                            inputText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShelterGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Ask", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UserChatBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            color = UserBubbleBg,
            shape = RoundedCornerShape(12.dp, 12.dp, 2.dp, 12.dp),
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Text(
                text = text,
                color = UserBubbleText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun AiChatBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            color = AiBubbleBg,
            shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp),
            border = BorderStroke(1.dp, AiBubbleBorder),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                color = AiBubbleText,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
