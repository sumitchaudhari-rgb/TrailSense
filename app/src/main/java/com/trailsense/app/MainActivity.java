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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

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
    private FloatingActionButton fabLocation;

    private TextView tvGpsStatus;
    private TextView tvLatitude;
    private TextView tvLongitude;

    // Phase 5 Position-to-Route Matching UI Views
    private TextView tvNearestWaypoint;
    private TextView tvNearestWater;
    private TextView tvNearestShelter;
    private TextView tvNearestExit;

    private LocationManager locationManager;
    private Location lastKnownLocation;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private List<Waypoint> waypointList = new ArrayList<>();
    private List<Marker> waypointMarkers = new ArrayList<>();
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

        // Phase 5 UI Views
        tvNearestWaypoint = findViewById(R.id.tvNearestWaypoint);
        tvNearestWater = findViewById(R.id.tvNearestWater);
        tvNearestShelter = findViewById(R.id.tvNearestShelter);
        tvNearestExit = findViewById(R.id.tvNearestExit);

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
                marker.setSnippet("[" + wp.getCategory().toUpperCase() + "]\n" + wp.getDescription());
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                
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

        if (nearestOverall != null) {
            tvNearestWaypoint.setText(String.format("📍 Nearest: %s (%s)",
                    nearestOverall.getName(), formatDistance(minDistanceOverall)));
        }

        if (nearestWater != null) {
            tvNearestWater.setText(String.format("💧 Nearest Water: %s (%s)",
                    nearestWater.getName(), formatDistance(minDistanceWater)));
        }

        if (nearestShelter != null) {
            tvNearestShelter.setText(String.format("🛖 Nearest Shelter: %s (%s)",
                    nearestShelter.getName(), formatDistance(minDistanceShelter)));
        }

        if (nearestExit != null) {
            tvNearestExit.setText(String.format("🚪 Nearest Exit: %s (%s)",
                    nearestExit.getName(), formatDistance(minDistanceExit)));
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
        tvGpsStatus.setTextColor(Color.parseColor("#2E7D32"));

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
}
