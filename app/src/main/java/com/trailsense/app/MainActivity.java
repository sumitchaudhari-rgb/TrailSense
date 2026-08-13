package com.trailsense.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay compassOverlay;
    private RotationGestureOverlay rotationGestureOverlay;
    private ScaleBarOverlay scaleBarOverlay;
    private Polyline navigationPolyline;
    private FloatingActionButton fabLocation;

    private TextView tvGpsStatus;
    private TextView tvLatitude;
    private TextView tvLongitude;

    // Phase 5 Position-to-Route Matching UI Views & Buttons
    private TextView tvNearestWaypoint;
    private TextView tvNearestWater;
    private TextView tvNearestShelter;
    private TextView tvNearestExit;
    private MaterialButton btnCreateRoute;
    private MaterialButton btnClearRoute;

    // Phase 6 On-Device LLM Chat UI Views & Chips
    private TextView tvChatOutput;
    private EditText etChatInput;
    private Button btnSendChat;
    private Chip chipShelter;
    private Chip chipWater;
    private Chip chipExit;
    private LlmAssistant llmAssistant;

    // Phase 8 Voice Input/Output (SpeechRecognizer & TextToSpeech)
    private MaterialButton btnMicChat;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private ActivityResultLauncher<String> audioPermissionRequest;

    private LocationManager locationManager;
    private Location lastKnownLocation;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private List<Waypoint> waypointList = new ArrayList<>();
    private List<Marker> waypointMarkers = new ArrayList<>();
    private Waypoint selectedTargetWaypoint = null;
    private Waypoint nearestOverallWaypoint = null;
    private Waypoint nearestShelterWaypoint = null;
    private Waypoint nearestWaterWaypoint = null;
    private Waypoint nearestExitWaypoint = null;
    private boolean waypointsAnchoredToGPS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize osmdroid configuration before layout inflation
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        fabLocation = findViewById(R.id.fabLocation);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);

        // Phase 5 UI Views & Route Buttons
        tvNearestWaypoint = findViewById(R.id.tvNearestWaypoint);
        tvNearestWater = findViewById(R.id.tvNearestWater);
        tvNearestShelter = findViewById(R.id.tvNearestShelter);
        tvNearestExit = findViewById(R.id.tvNearestExit);
        btnCreateRoute = findViewById(R.id.btnCreateRoute);
        btnClearRoute = findViewById(R.id.btnClearRoute);

        // Phase 6 UI Views & On-Device LLM Engine
        tvChatOutput = findViewById(R.id.tvChatOutput);
        etChatInput = findViewById(R.id.etChatInput);
        btnSendChat = findViewById(R.id.btnSendChat);
        chipShelter = findViewById(R.id.chipShelter);
        chipWater = findViewById(R.id.chipWater);
        chipExit = findViewById(R.id.chipExit);
        
        llmAssistant = new LlmAssistant(this);

        // Phase 8 Voice Input/Output UI Views & Listeners
        btnMicChat = findViewById(R.id.btnMicChat);
        btnMicChat.setOnClickListener(v -> checkAudioPermissionAndListen());

        btnSendChat.setOnClickListener(v -> submitGroundedLlmQuery());

        setupAudioPermissionLauncher();
        initTextToSpeech();

        // Quick Suggestion Chips Click Handlers with Auto-Routing
        chipShelter.setOnClickListener(v -> {
            etChatInput.setText("How far is the nearest shelter?");
            if (nearestShelterWaypoint != null) {
                selectedTargetWaypoint = nearestShelterWaypoint;
                createRouteToTarget();
            }
            submitGroundedLlmQuery();
        });

        chipWater.setOnClickListener(v -> {
            etChatInput.setText("Where can I find drinking water?");
            if (nearestWaterWaypoint != null) {
                selectedTargetWaypoint = nearestWaterWaypoint;
                createRouteToTarget();
            }
            submitGroundedLlmQuery();
        });

        chipExit.setOnClickListener(v -> {
            etChatInput.setText("Where is the nearest emergency exit?");
            if (nearestExitWaypoint != null) {
                selectedTargetWaypoint = nearestExitWaypoint;
                createRouteToTarget();
            }
            submitGroundedLlmQuery();
        });

        // Route creation buttons
        btnCreateRoute.setOnClickListener(v -> createRouteToTarget());
        btnClearRoute.setOnClickListener(v -> clearRouteLine());

        // Map engine rendering & High-DPI text scaling
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(16.0);
        GeoPoint startPoint = new GeoPoint(51.5074, -0.1278); // Default fallback center
        mapController.setCenter(startPoint);

        // 1. Enable 360-degree touch gesture rotation
        rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);

        // 2. Enable 360-degree compass orientation needle overlay
        compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        // 3. Add Google Maps-style Distance Scale Bar (meters / kilometers)
        scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setCentred(false);
        scaleBarOverlay.setScaleBarOffset(20, 60);
        mapView.getOverlays().add(scaleBarOverlay);

        // 4. Initialize location marker overlay
        GpsMyLocationProvider provider = new GpsMyLocationProvider(this);
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER);
        myLocationOverlay = new MyLocationNewOverlay(provider, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // 5. Render waypoints around initial coordinate
        renderWaypointsAroundLocation(51.5074, -0.1278);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        fabLocation.setOnClickListener(v -> centerOnUserLocation());

        setupPermissionLauncher();
        checkAndRequestLocationPermissions();
    }

    private void createRouteToTarget() {
        Waypoint target = selectedTargetWaypoint != null ? selectedTargetWaypoint : nearestOverallWaypoint;
        if (target == null) {
            Toast.makeText(this, "No target waypoint available for routing.", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedTargetWaypoint = target;
        drawNavigationLineToTarget(target);
        btnClearRoute.setVisibility(View.VISIBLE);
        Toast.makeText(this, "🎯 Route created to " + target.getName(), Toast.LENGTH_SHORT).show();
    }

    private void clearRouteLine() {
        if (navigationPolyline != null) {
            mapView.getOverlays().remove(navigationPolyline);
            navigationPolyline = null;
            mapView.invalidate();
        }
        selectedTargetWaypoint = null;
        btnClearRoute.setVisibility(View.GONE);
        Toast.makeText(this, "Route cleared.", Toast.LENGTH_SHORT).show();
    }

    private void drawNavigationLineToTarget(Waypoint target) {
        GeoPoint userGeo = null;
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            userGeo = myLocationOverlay.getMyLocation();
        } else if (lastKnownLocation != null) {
            userGeo = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        }

        if (userGeo == null) return;

        List<GeoPoint> linePoints = new ArrayList<>();
        linePoints.add(userGeo);
        linePoints.add(new GeoPoint(target.getLatitude(), target.getLongitude()));

        if (navigationPolyline != null) {
            mapView.getOverlays().remove(navigationPolyline);
        }

        navigationPolyline = new Polyline(mapView);
        navigationPolyline.setPoints(linePoints);
        navigationPolyline.getOutlinePaint().setColor(Color.parseColor("#1976D2")); // Vibrant trail blue line
        navigationPolyline.getOutlinePaint().setStrokeWidth(14.0f);
        
        // Insert polyline at index 0 so it renders under markers and overlay clicks
        mapView.getOverlays().add(0, navigationPolyline);
        mapView.invalidate();
    }

    /**
     * Phase 6: Ground user query with real-time GPS position facts & pass to LLM
     */
    private void submitGroundedLlmQuery() {
        String query = etChatInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a question.", Toast.LENGTH_SHORT).show();
            return;
        }

        tvChatOutput.setText("Thinking... (Generating position-grounded answer)");

        // Construct Position-Grounded Prompt for LLM
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("System: You are TrailSense AI, an offline mountain trail assistant.\n");
        promptBuilder.append("Current GPS: ").append(tvLatitude.getText()).append(", ").append(tvLongitude.getText()).append("\n");
        promptBuilder.append("Nearest Waypoint: ").append(tvNearestWaypoint.getText()).append("\n");
        promptBuilder.append("Nearest Water Point: ").append(tvNearestWater.getText()).append("\n");
        promptBuilder.append("Nearest Shelter: ").append(tvNearestShelter.getText()).append("\n");
        promptBuilder.append("Nearest Emergency Exit: ").append(tvNearestExit.getText()).append("\n");
        promptBuilder.append("User Question: ").append(query).append("\n");

        llmAssistant.generateResponse(promptBuilder.toString(), new LlmAssistant.ResponseListener() {
            @Override
            public void onResponse(String response) {
                tvChatOutput.setText(response);
                speakLlmResponse(response);
            }

            @Override
            public void onError(String error) {
                tvChatOutput.setText(error);
                speakLlmResponse(error);
            }
        });
    }

    private void renderWaypointsAroundLocation(double centerLat, double centerLon) {
        try {
            InputStream is = getAssets().open("waypoints.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(jsonStr);

            // Relative offset deltas around user's location (~100m to ~450m radius)
            double[][] offsets = {
                { 0.0015,  0.0012}, // Shelter (NE ~150m)
                {-0.0012, -0.0018}, // Water (SW ~180m)
                { 0.0025, -0.0010}, // Viewpoint (NW ~250m)
                { 0.0035,  0.0020}, // Exit (NE ~350m)
                {-0.0028,  0.0025}, // Water (SE ~300m)
                {-0.0040, -0.0030}  // Shelter (SW ~450m)
            };

            clearWaypointMarkers();
            waypointList.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                
                double lat = centerLat + offsets[i % offsets.length][0];
                double lon = centerLon + offsets[i % offsets.length][1];

                Waypoint wp = new Waypoint(
                        obj.optString("id"),
                        obj.optString("name"),
                        obj.optString("category"),
                        lat,
                        lon,
                        obj.optString("description")
                );
                waypointList.add(wp);

                GeoPoint pt = new GeoPoint(wp.getLatitude(), wp.getLongitude());

                // Create osmdroid marker for each waypoint
                Marker marker = new Marker(mapView);
                marker.setPosition(pt);
                marker.setTitle("📍 " + wp.getName());
                marker.setSnippet("[" + wp.getCategory().toUpperCase() + "] Route created!");
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                
                // Tapping marker pin draws route immediately and shows info popup
                marker.setOnMarkerClickListener((selectedMarker, map) -> {
                    selectedMarker.showInfoWindow();
                    selectedTargetWaypoint = wp;
                    createRouteToTarget();
                    return true;
                });

                waypointMarkers.add(marker);
                mapView.getOverlays().add(marker);
            }

            mapView.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load waypoints: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearWaypointMarkers() {
        for (Marker marker : waypointMarkers) {
            mapView.getOverlays().remove(marker);
        }
        waypointMarkers.clear();
    }

    /**
     * Phase 5 Step 2: Calculate spherical distance between two coordinates using Haversine formula
     * @return Distance in meters
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String formatDistance(double meters) {
        if (meters < 1000) {
            return String.format(Locale.US, "%.0f m", meters);
        } else {
            return String.format(Locale.US, "%.1f km", meters / 1000.0);
        }
    }

    /**
     * Phase 5 Step 3 & 4: Calculate real-time nearest waypoint statistics and update UI
     */
    private void updateNearestWaypointStats(@NonNull Location location) {
        if (waypointList.isEmpty()) return;

        Waypoint nearestOverall = null;
        double minDistanceOverall = Double.MAX_VALUE;

        Waypoint nearestWater = null;
        double minDistanceWater = Double.MAX_VALUE;

        Waypoint nearestShelter = null;
        double minDistanceShelter = Double.MAX_VALUE;

        Waypoint nearestExit = null;
        double minDistanceExit = Double.MAX_VALUE;

        for (Waypoint wp : waypointList) {
            double dist = calculateHaversineDistance(
                    location.getLatitude(), location.getLongitude(),
                    wp.getLatitude(), wp.getLongitude()
            );

            if (dist < minDistanceOverall) {
                minDistanceOverall = dist;
                nearestOverall = wp;
            }

            if ("water".equalsIgnoreCase(wp.getCategory()) && dist < minDistanceWater) {
                minDistanceWater = dist;
                nearestWater = wp;
            }

            if ("shelter".equalsIgnoreCase(wp.getCategory()) && dist < minDistanceShelter) {
                minDistanceShelter = dist;
                nearestShelter = wp;
            }

            if ("exit".equalsIgnoreCase(wp.getCategory()) && dist < minDistanceExit) {
                minDistanceExit = dist;
                nearestExit = wp;
            }
        }

        this.nearestOverallWaypoint = nearestOverall;
        this.nearestShelterWaypoint = nearestShelter;
        this.nearestWaterWaypoint = nearestWater;
        this.nearestExitWaypoint = nearestExit;

        if (nearestOverall != null) {
            tvNearestWaypoint.setText(String.format("📍 Nearest: %s (%s)",
                    nearestOverall.getName(), formatDistance(minDistanceOverall)));
        }

        if (nearestWater != null) {
            tvNearestWater.setText(String.format("💧 Water: %s (%s)",
                    nearestWater.getName(), formatDistance(minDistanceWater)));
        }

        if (nearestShelter != null) {
            tvNearestShelter.setText(String.format("🛖 Shelter: %s (%s)",
                    nearestShelter.getName(), formatDistance(minDistanceShelter)));
        }

        if (nearestExit != null) {
            tvNearestExit.setText(String.format("🚪 Exit: %s (%s)",
                    nearestExit.getName(), formatDistance(minDistanceExit)));
        }

        if (selectedTargetWaypoint != null && navigationPolyline != null) {
            drawNavigationLineToTarget(selectedTargetWaypoint);
        }
    }

    private void setupPermissionLauncher() {
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean fineLocationGranted = permissions.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = permissions.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if ((fineLocationGranted != null && fineLocationGranted) ||
                        (coarseLocationGranted != null && coarseLocationGranted)) {
                        Toast.makeText(this, "Location permission granted.", Toast.LENGTH_SHORT).show();
                        startLocationUpdates();
                    } else {
                        tvGpsStatus.setText("Status: Permission denied");
                        tvGpsStatus.setTextColor(Color.RED);
                    }
                }
        );
    }

    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            if (myLocationOverlay != null) {
                myLocationOverlay.enableMyLocation();
                myLocationOverlay.enableFollowLocation();
            }

            tvGpsStatus.setText("Status: Acquiring location...");
            tvGpsStatus.setTextColor(Color.parseColor("#D32F2F"));

            boolean providerEnabled = false;

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000L,
                        1.0f,
                        this
                );
                providerEnabled = true;
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        1.0f,
                        this
                );
                providerEnabled = true;
            }

            Location lastKnown = null;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (lastKnown == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            if (lastKnown != null) {
                onLocationChanged(lastKnown);
            }

            if (!providerEnabled) {
                tvGpsStatus.setText("Status: Location disabled");
                tvGpsStatus.setTextColor(Color.RED);
            }
        } catch (Exception e) {
            tvGpsStatus.setText("Status: Error - " + e.getMessage());
        }
    }

    private void centerOnUserLocation() {
        GeoPoint userPoint = null;
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            userPoint = myLocationOverlay.getMyLocation();
        } else if (lastKnownLocation != null) {
            userPoint = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        }

        if (userPoint != null && mapView != null) {
            mapView.getController().setZoom(16.0);
            mapView.getController().animateTo(userPoint);
            if (myLocationOverlay != null) {
                myLocationOverlay.enableFollowLocation();
            }
        } else {
            Toast.makeText(this, "Current location not available yet.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lastKnownLocation = location;
        tvGpsStatus.setText("Status: Location Signal Locked");
        tvGpsStatus.setTextColor(Color.parseColor("#81C784"));

        tvLatitude.setText(String.format(Locale.US, "Lat: %.6f°", location.getLatitude()));
        tvLongitude.setText(String.format(Locale.US, "Lon: %.6f°", location.getLongitude()));

        // Dynamically re-anchor 6 trail waypoints around the user's real location
        if (!waypointsAnchoredToGPS) {
            waypointsAnchoredToGPS = true;
            renderWaypointsAroundLocation(location.getLatitude(), location.getLongitude());
        }

        // Phase 5: Update nearest waypoint distances in real time
        updateNearestWaypointStats(location);

        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        if (mapView != null) {
            if (myLocationOverlay != null && myLocationOverlay.isFollowLocationEnabled()) {
                mapView.getController().setCenter(currentPoint);
            }
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        startLocationUpdates();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            tvGpsStatus.setText("Status: Location disabled");
            tvGpsStatus.setTextColor(Color.RED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
        }
        if (compassOverlay != null) {
            compassOverlay.enableCompass();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
        if (compassOverlay != null) {
            compassOverlay.disableCompass();
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    // ==========================================
    // Phase 8: Voice Input & Output Methods
    // ==========================================

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }

    private void speakLlmResponse(String text) {
        if (textToSpeech != null && text != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TrailSenseTTS");
        }
    }

    private void setupAudioPermissionLauncher() {
        audioPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startVoiceRecognition();
                    } else {
                        Toast.makeText(this, "Microphone permission required for voice Q&A.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    tvChatOutput.setText("🎙️ Listening... Speak your trail question clearly.");
                    btnMicChat.setText("🔴");
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    btnMicChat.setText("🎙️");
                }

                @Override
                public void onError(int error) {
                    btnMicChat.setText("🎙️");
                    tvChatOutput.setText("Voice recognition error or timeout. Tap mic to try again.");
                }

                @Override
                public void onResults(Bundle results) {
                    btnMicChat.setText("🎙️");
                    if (results != null) {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            String recognizedText = matches.get(0);
                            etChatInput.setText(recognizedText);
                            Toast.makeText(MainActivity.this, "Voice: \"" + recognizedText + "\"", Toast.LENGTH_SHORT).show();
                            submitGroundedLlmQuery(); // Feeds voice directly into position-grounded LLM pipeline
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private void checkAudioPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition();
        } else {
            audioPermissionRequest.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceRecognition() {
        if (speechRecognizer == null) {
            initSpeechRecognizer();
        }

        if (speechRecognizer != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true); // Prefer offline speech recognition
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask TrailSense AI...");
            try {
                speechRecognizer.startListening(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Voice recognition error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Speech recognition unavailable on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
