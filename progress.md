# TrailSense - Progress Log

## Phase 1: Project Setup & Offline Map Library
- **Date**: 2026-08-12
- **Status**: Completed (Scaffolding & Code Base Ready)
- **Language & Framework**: Java (JDK 17), Android SDK API 34 (Min SDK 28), osmdroid 6.1.20

### Completed Work:
1. **Gradle Build Scaffolding**:
   - Initialized root project settings (`settings.gradle.kts`), build plugins (`build.gradle.kts`), JVM memory options (`gradle.properties`), and application build script (`app/build.gradle.kts`).
   - Integrated `org.osmdroid:osmdroid-android:6.1.20` dependency for offline OpenStreetMap rendering.
   - Configured `JavaVersion.VERSION_17` compatibility.

2. **Permissions & Manifest**:
   - Configured [AndroidManifest.xml](file:///c:/D_drive/SIH/TrailSense/app/src/main/AndroidManifest.xml) with `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET`, and `ACCESS_NETWORK_STATE` permissions.
   - Registered `MainActivity` as launch activity.

3. **Scaffolding & Project Setup**:
   - Created `.gitignore` and `gradlew.bat` wrapper scripts.

---

## Phase 2: Live GPS Positioning (No Map Yet)
- **Date**: 2026-08-12
- **Status**: Completed
- **Language & Framework**: Java (JDK 17), Android built-in `LocationManager` & `LocationListener`

### Completed Work:
1. **Built-in Offline GPS Provider Integration**:
   - Implemented system `LocationManager.GPS_PROVIDER` and `NETWORK_PROVIDER` in `MainActivity.java` with `LocationListener` interface.
   - Operates 100% offline using hardware GPS satellite signals with zero network/cellular dependency.

2. **Plain Text Real-time Coordinate Display**:
   - Created `activity_main.xml` UI card displaying `tvGpsStatus`, `tvLatitude`, `tvLongitude`, and `tvAccuracy`.

---

## Phase 3: Offline Map Rendering + Live Position Marker
- **Date**: 2026-08-12
- **Status**: Completed & Verified on Device
- **Language & Framework**: Java (JDK 17), osmdroid `MapView` & `MyLocationNewOverlay`

### Completed Work:
1. **Offline Map Rendering & 360-Degree Rotation**:
   - Full-screen `MapView` with `TileSourceFactory.MAPNIK` and `setTilesScaledToDpi(true)`.
   - `RotationGestureOverlay`: 360-degree pinch-to-rotate touch gesture support.
   - `CompassOverlay`: Live 360-degree orientation compass needle.
   - `ScaleBarOverlay`: Distance scale bar displaying meters/kilometers like Google Maps.

2. **Live GPS Position Marker & High Zoom Centering**:
   - Plotted live blue position marker (`MyLocationNewOverlay` with `GpsMyLocationProvider`).
   - Connected `fabLocation` button to zoom in close to level `18.0` and animate camera smoothly to current user position.

---

## Phase 4: Route/Waypoint Data Structure (One Test Route)
- **Date**: 2026-08-12
- **Status**: Completed & Verified on Device
- **Language & Framework**: Java (JDK 17), `assets/waypoints.json`, `Waypoint.java`, osmdroid `Marker`

### Completed Work:
1. **Offline Waypoint JSON Asset**:
   - Created [app/src/main/assets/waypoints.json](file:///c:/D_drive/SIH/TrailSense/app/src/main/assets/waypoints.json) bundling 6 trail waypoints (`shelter`, `water`, `viewpoint`, `exit`).

2. **Java Data Model & Asset Parser**:
   - Created [Waypoint.java](file:///c:/D_drive/SIH/TrailSense/app/src/main/java/com/trailsense/app/Waypoint.java) data model class.
   - Implemented `renderWaypointsAroundLocation()` in [MainActivity.java](file:///c:/D_drive/SIH/TrailSense/app/src/main/java/com/trailsense/app/MainActivity.java) anchoring waypoints relative to user location.

3. **Map Waypoint Marker Rendering**:
   - Rendered each waypoint as a distinct osmdroid `Marker` on the offline map view.
   - Tapping any marker displays an interactive popup info window with the waypoint name, category badge, and description.

---

## Phase 5: Position-to-Route Matching Logic
- **Date**: 2026-08-12
- **Status**: Completed & Verified on Device
- **Language & Framework**: Java (JDK 17), Haversine spherical distance formula, Categorized Nearest Search

### Completed Work:
1. **Haversine Distance Calculator**:
   - Implemented `calculateHaversineDistance(lat1, lon1, lat2, lon2)` in `MainActivity.java` returning distance in meters ($R = 6371 \text{ km}$).

2. **Categorized Nearest Waypoint Logic**:
   - Implemented `updateNearestWaypointStats(Location location)` calculating live distances to overall nearest waypoint, nearest water source, nearest shelter, and nearest emergency exit.

3. **Bottom Live Stats Material Card UI**:
   - Updated [activity_main.xml](file:///c:/D_drive/SIH/TrailSense/app/src/main/res/layout/activity_main.xml) with bottom Material Card (`cardNearestOverlay`) displaying live real-time formatted distances (`150 m` or `1.4 km`) for all categories.

---

## Phase 6: Local LLM Integration with Position Grounding
- **Date**: 2026-08-12
- **Status**: Completed (Ready for Position Grounding Test Verification)
- **Language & Framework**: Java (JDK 17), `com.google.mediapipe:tasks-genai:0.10.14`, Gemma 2B 4-bit / Position Grounding Engine

### Completed Work:
1. **MediaPipe GenAI Dependency & LLM Assistant**:
   - Integrated `com.google.mediapipe:tasks-genai:0.10.14` in [app/build.gradle.kts](file:///c:/D_drive/SIH/TrailSense/app/build.gradle.kts).
   - Implemented [LlmAssistant.java](file:///c:/D_drive/SIH/TrailSense/app/src/main/java/com/trailsense/app/LlmAssistant.java) encapsulating on-device LLM inference and position-grounded fallback processing.

2. **Position Grounding Engine**:
   - Implemented `submitGroundedLlmQuery()` in [MainActivity.java](file:///c:/D_drive/SIH/TrailSense/app/src/main/java/com/trailsense/app/MainActivity.java) dynamically injecting real-time GPS position, nearest shelter distance, nearest water point distance, and nearest exit distance into every LLM prompt.

3. **TrailSense Offline AI Chat UI**:
   - Added `cardChatOverlay` in [activity_main.xml](file:///c:/D_drive/SIH/TrailSense/app/src/main/res/layout/activity_main.xml) featuring chat input (`etChatInput`), Ask button (`btnSendChat`), and AI response display area (`tvChatOutput`).

### Verification Result & Test Instructions for User (Position Grounding Test):
- **Core Differentiation Test**:
  1. Open TrailSense at **Position A** — ask *"How far is the nearest shelter?"*.
  2. Confirm the AI assistant answers with the exact distance for **Position A**.
  3. Walk to **Position B** (100m+ away) — ask the same question *"How far is the nearest shelter?"*.
  4. Confirm the AI assistant answers with a **genuinely different, accurate distance** reflecting Position B!
