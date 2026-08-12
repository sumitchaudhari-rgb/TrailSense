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

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

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

    private LocationManager locationManager;
    private Location lastKnownLocation;
    private ActivityResultLauncher<String[]> locationPermissionRequest;

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

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        fabLocation.setOnClickListener(v -> centerOnUserLocation());

        setupPermissionLauncher();
        checkAndRequestLocationPermissions();
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
            // Set close zoom level (18.0) and animate map camera cleanly to user position
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
        tvGpsStatus.setText("Status: Location Signal Locked");
        tvGpsStatus.setTextColor(Color.parseColor("#2E7D32"));

        tvLatitude.setText(String.format(Locale.US, "Lat: %.6f°", location.getLatitude()));
        tvLongitude.setText(String.format(Locale.US, "Lon: %.6f°", location.getLongitude()));

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
