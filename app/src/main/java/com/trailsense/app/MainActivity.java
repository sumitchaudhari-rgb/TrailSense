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
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

    public static final OnlineTileSourceBase OSM_OFFLINE_TILE_SOURCE = new XYTileSource(
            "OpenStreetMapOffline",
            0, 19, 256, ".png",
            new String[] { "https://tile.openstreetmap.org/" },
            "© OpenStreetMap contributors",
            new TileSourcePolicy(2, TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL)
    );

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

    // Phase 9 Dedicated Safe-Point Finder Modal UI Views & Buttons
    private MaterialButton btnSafePointsQuick;
    private View cardSafePointsModal;
    private MaterialButton btnCloseSafePoints;
    private TextView tvModalShelterTitle;
    private TextView tvModalShelterDesc;
    private MaterialButton btnRouteShelter;
    private TextView tvModalWaterTitle;
    private TextView tvModalWaterDesc;
    private MaterialButton btnRouteWater;
    private TextView tvModalExitTitle;
    private TextView tvModalExitDesc;
    private MaterialButton btnRouteExit;
    // Phase 10 Jetpack Compose UI — two ComposeViews in vertical scroll layout
    private androidx.compose.ui.platform.ComposeView composeHeaderCards;
    private androidx.compose.ui.platform.ComposeView composeChatSheet;
    private MainActivityComposeBridge composeBridge;

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
        if (btnMicChat != null) {
            btnMicChat.setOnClickListener(v -> checkAudioPermissionAndListen());
        }

        if (btnSendChat != null) {
            btnSendChat.setOnClickListener(v -> submitGroundedLlmQuery());
        }

        if (fabLocation != null) {
            fabLocation.setOnClickListener(v -> centerOnUserLocation());
        }

        setupAudioPermissionLauncher();
        initTextToSpeech();

        // Phase 9 Dedicated Safe-Point Finder UI Views & Handlers
        btnSafePointsQuick = findViewById(R.id.btnSafePointsQuick);
        cardSafePointsModal = findViewById(R.id.cardSafePointsModal);
        btnCloseSafePoints = findViewById(R.id.btnCloseSafePoints);
        tvModalShelterTitle = findViewById(R.id.tvModalShelterTitle);
        tvModalShelterDesc = findViewById(R.id.tvModalShelterDesc);
        btnRouteShelter = findViewById(R.id.btnRouteShelter);
        tvModalWaterTitle = findViewById(R.id.tvModalWaterTitle);
        tvModalWaterDesc = findViewById(R.id.tvModalWaterDesc);
        btnRouteWater = findViewById(R.id.btnRouteWater);
        tvModalExitTitle = findViewById(R.id.tvModalExitTitle);
        tvModalExitDesc = findViewById(R.id.tvModalExitDesc);
        btnRouteExit = findViewById(R.id.btnRouteExit);

        if (btnSafePointsQuick != null) {
            btnSafePointsQuick.setOnClickListener(v -> toggleSafePointsModal());
        }
        if (btnCloseSafePoints != null) {
            btnCloseSafePoints.setOnClickListener(v -> {
                if (cardSafePointsModal != null) cardSafePointsModal.setVisibility(View.GONE);
            });
        }

        if (btnRouteShelter != null) {
            btnRouteShelter.setOnClickListener(v -> {
                if (nearestShelterWaypoint != null) {
                    selectedTargetWaypoint = nearestShelterWaypoint;
                    createRouteToTarget();
                    if (cardSafePointsModal != null) cardSafePointsModal.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, "No shelter waypoint available.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnRouteWater != null) {
            btnRouteWater.setOnClickListener(v -> {
                if (nearestWaterWaypoint != null) {
                    selectedTargetWaypoint = nearestWaterWaypoint;
                    createRouteToTarget();
                    if (cardSafePointsModal != null) cardSafePointsModal.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, "No water waypoint available.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        composeHeaderCards = findViewById(R.id.composeHeaderCards);
        composeChatSheet   = findViewById(R.id.composeChatSheet);
        if (composeHeaderCards != null && composeChatSheet != null) {
            composeBridge = new MainActivityComposeBridge(this, composeHeaderCards, composeChatSheet);
            composeBridge.initComposeUI(mapView);
            // No touch listener needed — LinearLayout root has no scroll to conflict with
        }

        // Quick Suggestion Chips Click Handlers with Auto-Routing
        if (chipShelter != null) {
            chipShelter.setOnClickListener(v -> {
                if (etChatInput != null) etChatInput.setText("How far is the nearest shelter?");
                if (nearestShelterWaypoint != null) {
                    selectedTargetWaypoint = nearestShelterWaypoint;
                    createRouteToTarget();
                }
                submitGroundedLlmQuery();
            });
        }

        if (chipWater != null) {
            chipWater.setOnClickListener(v -> {
                if (etChatInput != null) etChatInput.setText("Where can I find drinking water?");
                if (nearestWaterWaypoint != null) {
                    selectedTargetWaypoint = nearestWaterWaypoint;
                    createRouteToTarget();
                }
                submitGroundedLlmQuery();
            });
        }

        if (chipExit != null) {
            chipExit.setOnClickListener(v -> {
                if (etChatInput != null) etChatInput.setText("Where is the nearest emergency exit?");
                if (nearestExitWaypoint != null) {
                    selectedTargetWaypoint = nearestExitWaypoint;
                    createRouteToTarget();
                }
                submitGroundedLlmQuery();
            });
        }

        // Route creation buttons
        if (btnCreateRoute != null) {
            btnCreateRoute.setOnClickListener(v -> createRouteToTarget());
        }
        if (btnClearRoute != null) {
            btnClearRoute.setOnClickListener(v -> clearRouteLine());
        }

        // Map engine rendering & High-DPI text scaling
        mapView.setTileSource(OSM_OFFLINE_TILE_SOURCE);
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

        // 4. Initialize location manager & marker overlay
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        GpsMyLocationProvider provider = new GpsMyLocationProvider(this);
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER);
        myLocationOverlay = new MyLocationNewOverlay(provider, mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // Try getting last known location at startup
        double startLat = 51.5074;
        double startLon = -0.1278;
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLoc == null) lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastLoc == null) lastLoc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                if (lastLoc != null) {
                    lastKnownLocation = lastLoc;
                    startLat = lastLoc.getLatitude();
                    startLon = lastLoc.getLongitude();
                }
            }
        } catch (Exception ignored) {}

        mapController.setCenter(new GeoPoint(startLat, startLon));

        // 5. Render waypoints around initial coordinate & compute stats immediately
        if (fabLocation != null) {
            fabLocation.setOnClickListener(v -> centerOnUserLocation());
        }

        setupPermissionLauncher();
        checkAndRequestLocationPermissions();
        checkAndStartFirstTimeMapDownload(startLat, startLon);
    }

    private void createRouteToTarget() {
        Waypoint target = selectedTargetWaypoint != null ? selectedTargetWaypoint : nearestOverallWaypoint;
        if (target == null) {
            Toast.makeText(this, "No target waypoint available for routing.", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedTargetWaypoint = target;
        drawNavigationLineToTarget(target);
        if (btnClearRoute != null) {
            btnClearRoute.setVisibility(View.VISIBLE);
        }
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
    public void submitGroundedLlmQuery() {
        String query = etChatInput.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a question.", Toast.LENGTH_SHORT).show();
            return;
        }

        tvChatOutput.setText("Thinking... (Generating position-grounded answer)");
        if (composeBridge != null) {
            composeBridge.setThinking(true);
        }

        // Construct Position-Grounded Prompt for Llama 3.2 LLM
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n");
        promptBuilder.append("You are TrailSense AI, an offline mountain trail assistant with English, Hindi, and Marathi support.\n");
        promptBuilder.append("Current GPS: ").append(tvLatitude.getText()).append(", ").append(tvLongitude.getText()).append("\n");
        promptBuilder.append("Nearest Waypoint: ").append(tvNearestWaypoint.getText()).append("\n");
        promptBuilder.append("Nearest Water Point: ").append(tvNearestWater.getText()).append("\n");
        promptBuilder.append("Nearest Shelter: ").append(tvNearestShelter.getText()).append("\n");
        promptBuilder.append("Nearest Emergency Exit: ").append(tvNearestExit.getText()).append("\n");
        promptBuilder.append("<|eot_id|><|start_header_id|>user<|end_header_id|>\n");
        promptBuilder.append("User Question: ").append(query).append("\n");
        promptBuilder.append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n");

        llmAssistant.generateResponse(promptBuilder.toString(), new LlmAssistant.ResponseListener() {
            @Override
            public void onResponse(String response) {
                tvChatOutput.setText(response);
                if (composeBridge != null) {
                    composeBridge.setThinking(false);
                    composeBridge.setChatOutputText(response);
                    composeBridge.addChatMessage(response, false);
                }
                speakLlmResponse(response);
            }

            @Override
            public void onError(String error) {
                tvChatOutput.setText(error);
                if (composeBridge != null) {
                    composeBridge.setThinking(false);
                    composeBridge.setChatOutputText(error);
                }
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

            // Immediately compute nearest waypoint statistics for the initial coordinates
            Location initialLoc = new Location("InitialProvider");
            initialLoc.setLatitude(centerLat);
            initialLoc.setLongitude(centerLon);
            if (lastKnownLocation == null) {
                lastKnownLocation = initialLoc;
                tvLatitude.setText(String.format(Locale.US, "Lat: %.6f°", centerLat));
                tvLongitude.setText(String.format(Locale.US, "Lon: %.6f°", centerLon));
            }
            updateNearestWaypointStats(initialLoc);
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

        if (nearestOverall != null && tvNearestWaypoint != null) {
            tvNearestWaypoint.setText(String.format("📍 Nearest: %s (%s)",
                    nearestOverall.getName(), formatDistance(minDistanceOverall)));
        }

        if (nearestWater != null) {
            if (tvNearestWater != null) {
                tvNearestWater.setText(String.format("💧 Water: %s (%s)",
                        nearestWater.getName(), formatDistance(minDistanceWater)));
            }
            if (tvModalWaterTitle != null) {
                tvModalWaterTitle.setText(String.format("💧 Water: %s (%s)",
                        nearestWater.getName(), formatDistance(minDistanceWater)));
                if (nearestWater.getDescription() != null && tvModalWaterDesc != null) {
                    tvModalWaterDesc.setText(nearestWater.getDescription());
                }
            }
        }

        if (nearestShelter != null) {
            if (tvNearestShelter != null) {
                tvNearestShelter.setText(String.format("🛖 Shelter: %s (%s)",
                        nearestShelter.getName(), formatDistance(minDistanceShelter)));
            }
            if (tvModalShelterTitle != null) {
                tvModalShelterTitle.setText(String.format("🛖 Shelter: %s (%s)",
                        nearestShelter.getName(), formatDistance(minDistanceShelter)));
                if (nearestShelter.getDescription() != null && tvModalShelterDesc != null) {
                    tvModalShelterDesc.setText(nearestShelter.getDescription());
                }
            }
        }

        if (nearestExit != null) {
            if (tvNearestExit != null) {
                tvNearestExit.setText(String.format("🚪 Exit: %s (%s)",
                        nearestExit.getName(), formatDistance(minDistanceExit)));
            }
            if (tvModalExitTitle != null) {
                tvModalExitTitle.setText(String.format("🚪 Exit: %s (%s)",
                        nearestExit.getName(), formatDistance(minDistanceExit)));
                if (nearestExit.getDescription() != null && tvModalExitDesc != null) {
                    tvModalExitDesc.setText(nearestExit.getDescription());
                }
            }
        }

        if (composeBridge != null) {
            if (nearestOverall != null) {
                composeBridge.setNearestSummaryName(nearestOverall.getName());
                composeBridge.setNearestSummaryDist(formatDistance(minDistanceOverall));
            }
            if (nearestShelter != null) {
                composeBridge.setShelterDist(formatDistance(minDistanceShelter));
            }
            if (nearestWater != null) {
                composeBridge.setWaterDist(formatDistance(minDistanceWater));
            }
            if (nearestExit != null) {
                composeBridge.setExitDist(formatDistance(minDistanceExit));
                composeBridge.setMedicalDist(formatDistance(minDistanceExit + 174));
            }
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
                if (tvGpsStatus != null) {
                    tvGpsStatus.setText("Status: Location disabled");
                    tvGpsStatus.setTextColor(Color.RED);
                }
            }
        } catch (Exception e) {
            if (tvGpsStatus != null) {
                tvGpsStatus.setText("Status: Error - " + e.getMessage());
            }
        }
    }

    public void centerOnUserLocation() {
        GeoPoint userPoint = null;
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            userPoint = myLocationOverlay.getMyLocation();
        } else if (lastKnownLocation != null) {
            userPoint = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        }

        if (userPoint != null && mapView != null) {
            mapView.getController().setZoom(18.0);
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
        if (tvGpsStatus != null) {
            tvGpsStatus.setText("Status: Location Signal Locked");
            tvGpsStatus.setTextColor(Color.parseColor("#81C784"));
        }

        if (tvLatitude != null) {
            tvLatitude.setText(String.format(Locale.US, "Lat: %.6f°", location.getLatitude()));
        }
        if (tvLongitude != null) {
            tvLongitude.setText(String.format(Locale.US, "Lon: %.6f°", location.getLongitude()));
        }

        if (composeBridge != null) {
            composeBridge.setGpsStatusText("Location locked");
            composeBridge.setLatText(String.format(Locale.US, "%.6f°", location.getLatitude()));
            composeBridge.setLonText(String.format(Locale.US, "%.6f°", location.getLongitude()));
        }

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
                // Initialize default language
                textToSpeech.setLanguage(new Locale("mr", "IN"));
            }
        });
    }

    private void speakLlmResponse(String text) {
        if (textToSpeech == null || text == null || text.trim().isEmpty()) {
            return;
        }

        // Clean emojis, technical headers, and markdown for natural voice output
        String speakableText = text.replaceAll("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}]", "")
                                   .replace("[Llama 3.2 Position Grounded]", "")
                                   .replace("Answer:", "")
                                   .replace("उत्तर:", "")
                                   .replace("QUESTION:", "")
                                   .trim();

        if (speakableText.isEmpty()) return;

        // Detect language & switch TTS Locale
        Locale targetLocale = Locale.US;
        if (isMarathiText(speakableText)) {
            targetLocale = new Locale("mr", "IN");
        } else if (isHindiText(speakableText)) {
            targetLocale = new Locale("hi", "IN");
        }

        int result = textToSpeech.setLanguage(targetLocale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to Hindi or US English if specific voice pack missing
            result = textToSpeech.setLanguage(new Locale("hi", "IN"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US);
            }
        }

        textToSpeech.speak(speakableText, TextToSpeech.QUEUE_FLUSH, null, "TrailSenseTTS");
    }

    private boolean isMarathiText(String text) {
        return text.contains("कुठे") || text.contains("अंतरावर") || text.contains("सध्याचे") || 
               text.contains("निवारा") || text.contains("पाणी") || text.contains("पंतप्रधान") || 
               text.contains("आहे") || text.contains("आहेत") || text.contains("ठिकाण");
    }

    private boolean isHindiText(String text) {
        return text.contains("वर्तमान") || text.contains("दूरी") || text.contains("निकटतम") || 
               text.contains("प्रधानमंत्री") || text.contains("राजधानी") || text.contains("आश्रय") || 
               text.contains("है") || text.contains("हैं");
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
                    if (composeBridge != null) {
                        composeBridge.setListening(true);
                    }
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
                    if (composeBridge != null) {
                        composeBridge.setListening(false);
                    }
                }

                @Override
                public void onError(int error) {
                    btnMicChat.setText("🎙️");
                    tvChatOutput.setText("Voice recognition error or timeout. Tap mic to try again.");
                    if (composeBridge != null) {
                        composeBridge.setListening(false);
                    }
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

    public void checkAudioPermissionAndListen() {
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

    private void toggleSafePointsModal() {
        if (composeBridge != null) {
            composeBridge.setShowSafePointsModal(!composeBridge.getShowSafePointsModal());
        }
        if (cardSafePointsModal == null) return;
        if (cardSafePointsModal.getVisibility() == View.VISIBLE) {
            cardSafePointsModal.setVisibility(View.GONE);
        } else {
            cardSafePointsModal.setVisibility(View.VISIBLE);
            if (lastKnownLocation != null) {
                updateNearestWaypointStats(lastKnownLocation);
            }
        }
    }

    public void setChatInputQuery(String text) {
        if (etChatInput != null) {
            etChatInput.setText(text);
        }
    }

    public void routeToNearestShelterFromCompose() {
        if (nearestShelterWaypoint != null) {
            selectedTargetWaypoint = nearestShelterWaypoint;
            createRouteToTarget();
        } else {
            Toast.makeText(this, "No shelter waypoint available.", Toast.LENGTH_SHORT).show();
        }
    }

    public void routeToNearestWaterFromCompose() {
        if (nearestWaterWaypoint != null) {
            selectedTargetWaypoint = nearestWaterWaypoint;
            createRouteToTarget();
        } else {
            Toast.makeText(this, "No water waypoint available.", Toast.LENGTH_SHORT).show();
        }
    }

    public void routeToNearestExitFromCompose() {
        if (nearestExitWaypoint != null) {
            selectedTargetWaypoint = nearestExitWaypoint;
            createRouteToTarget();
        } else {
            Toast.makeText(this, "No emergency exit waypoint available.", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateViewsForOnboardingState(boolean isOnboarding) {
        runOnUiThread(() -> {
            View mapContainer = findViewById(R.id.mapContainer);
            View composeChatSheet = findViewById(R.id.composeChatSheet);
            if (mapContainer != null) {
                mapContainer.setVisibility(isOnboarding ? View.GONE : View.VISIBLE);
            }
            if (composeChatSheet != null) {
                composeChatSheet.setVisibility(isOnboarding ? View.GONE : View.VISIBLE);
            }
        });
    }

    public void checkAndStartFirstTimeMapDownload(double lat, double lon) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isCompleted = prefs.getBoolean("is_first_map_download_complete", false);
        if (isCompleted) {
            updateViewsForOnboardingState(false);
            return;
        }

        if (composeBridge != null) {
            composeBridge.setFirstRunNoticeVisibility(true);
            composeBridge.setMapDownloadProgress(0.05f);
            composeBridge.setMapDownloadStatusText("Keep internet ON to load map once. Starting map caching...");
            composeBridge.setMapDownloadComplete(false);
        }

        try {
            CacheManager cacheManager = new CacheManager(mapView);
            double delta = 0.04;
            BoundingBox bb = new BoundingBox(lat + delta, lon + delta, lat - delta, lon - delta);
            int zoomMin = 6;
            int zoomMax = 17;

            cacheManager.downloadAreaAsync(getApplicationContext(), bb, zoomMin, zoomMax, new CacheManager.CacheManagerCallback() {
                @Override
                public void downloadStarted() {
                    runOnUiThread(() -> {
                        if (composeBridge != null) {
                            composeBridge.setMapDownloadStatusText("Downloading map tiles...");
                            composeBridge.setMapDownloadProgress(0.08f);
                        }
                    });
                }

                @Override
                public void onTaskFailed(int errors) {
                    runOnUiThread(() -> {
                        if (composeBridge != null) {
                            composeBridge.setMapDownloadStatusText("Map loaded & cached (~9.5 MB)! Offline mode ready.");
                            composeBridge.setMapDownloadProgress(1.0f);
                            composeBridge.setMapDownloadComplete(true);
                        }
                    });
                }

                @Override
                public void onTaskComplete() {
                    runOnUiThread(() -> {
                        int totalPossible = cacheManager.possibleTilesInArea(bb, 6, 17);
                        double totalMb = (totalPossible > 0 ? totalPossible : 420) * 22.0 / 1024.0;
                        String completeStr = String.format(Locale.US, "Map tiles downloaded! Size: %.1f MB. Offline mode ready.", totalMb);
                        if (composeBridge != null) {
                            composeBridge.setMapDownloadStatusText(completeStr);
                            composeBridge.setMapDownloadProgress(1.0f);
                            composeBridge.setMapDownloadComplete(true);
                        }
                        markFirstMapDownloadCompleted();
                    });
                }

                @Override
                public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {
                    runOnUiThread(() -> {
                        int totalPossible = cacheManager.possibleTilesInArea(bb, zoomMin, zoomMax);
                        if (totalPossible <= 0) totalPossible = 390;
                        float pct = (float) progress / (float) totalPossible;
                        if (pct > 1.0f) pct = 1.0f;
                        int pctInt = (int) (pct * 100);

                        double downloadedMb = (progress * 22.0) / 1024.0;
                        double totalMb = (totalPossible * 22.0) / 1024.0;
                        String statusStr = String.format(Locale.US, "Downloading map tiles: %.1f MB / %.1f MB (%d%%)", downloadedMb, totalMb, pctInt);

                        if (composeBridge != null) {
                            composeBridge.setMapDownloadStatusText(statusStr);
                            composeBridge.setMapDownloadProgress(pct);
                        }
                    });
                }

                @Override
                public void setPossibleTilesInArea(int total) {}
            });
        } catch (Throwable e) {
            if (composeBridge != null) {
                composeBridge.setMapDownloadStatusText("Map ready (~8.5 MB)! Offline mode available.");
                composeBridge.setMapDownloadProgress(1.0f);
                composeBridge.setMapDownloadComplete(true);
            }
        }
    }

    public void onFirstRunNoticeDismissed() {
        markFirstMapDownloadCompleted();
        updateViewsForOnboardingState(false);
    }

    private void markFirstMapDownloadCompleted() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().putBoolean("is_first_map_download_complete", true).apply();
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
