package com.trailsense.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// MapViewCompose is intentionally a transparent placeholder.
// The actual osmdroid MapView lives in activity_main.xml behind the Compose overlay.
// Passing an already-attached MapView into AndroidView causes:
// IllegalStateException: The specified child already has a parent.
@Composable
fun MapViewCompose(
    mapView: org.osmdroid.views.MapView?,
    modifier: Modifier = Modifier
) {
    // No-op: map is rendered by the XML MapView in activity_main.xml
}
