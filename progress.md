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
   - Implemented system `LocationManager.GPS_PROVIDER` in `MainActivity.java` with `LocationListener` interface.
   - Operates 100% offline using hardware GPS satellite signals with zero network/cellular dependency.

2. **Plain Text Real-time Coordinate Display**:
   - Created `activity_main.xml` UI card displaying `tvGpsStatus`, `tvLatitude`, `tvLongitude`, and `tvAccuracy`.

---

## Phase 3: Offline Map Rendering + Live Position Marker
- **Date**: 2026-08-12
- **Status**: Completed (Awaiting Offline Test Confirmation)
- **Language & Framework**: Java (JDK 17), osmdroid `MapView` & `MyLocationNewOverlay`

### Completed Work:
1. **Offline Map Rendering**:
   - Re-introduced `org.osmdroid.views.MapView` filling full screen in [activity_main.xml](file:///c:/D_drive/SIH/TrailSense/app/src/main/res/layout/activity_main.xml).
   - Configured tile source (`TileSourceFactory.MAPNIK`), multi-touch controls, and zoom level `15.0` in [MainActivity.java](file:///c:/D_drive/SIH/TrailSense/app/src/main/java/com/trailsense/app/MainActivity.java).

2. **Live GPS Position Marker**:
   - Initialized osmdroid `MyLocationNewOverlay` with `GpsMyLocationProvider`.
   - Plotted live position marker on the offline map view.
   - Integrated `LocationManager.GPS_PROVIDER` updates to dynamically update position marker and map camera center in real time.

3. **UI Overlay Controls**:
   - Added top Material Card view showing live GPS status, latitude, and longitude.
   - Added `FloatingActionButton` (`fabLocation`) at bottom-right to re-center map view camera directly onto the live position marker.

### Verification Result & Test Instructions for User:
- **Offline Map & Marker Test**:
  1. Enable **Airplane Mode** (Mobile Data & Wi-Fi OFF).
  2. Keep **Location / GPS** toggled ON in system settings.
  3. Launch TrailSense — osmdroid map renders and blue position marker appears at current GPS location.
  4. Walk or simulate GPS movement — position marker updates on map in real time with zero network connectivity.

### Next Phase Pick-up (Phase 4):
- Phase 4: Route/Waypoint Data Structure (One Test Route) - Design JSON schema for waypoints, bundle sample JSON asset with 5-8 waypoints, parse JSON on startup, and render categorized markers on offline map.
