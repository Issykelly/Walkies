package com.example.walkies;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CircularWalksMap extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "WalkiesDebug";

    // Routing
    private static final float PROGRESS_THRESHOLD = 15f;
    private static final float ARRIVAL_THRESHOLD = 50f;
    private static final float OFF_ROUTE_THRESHOLD = 25f;
    private static final long RECALC_COOLDOWN_MS = 15_000;

    private List<LatLng> fullRoutePoints = new ArrayList<>();
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback locationCallback;
    private Polyline currentPolyline;
    private LatLng startPoint;
    private walkModel activeWalk;
    private boolean isReturning = false;
    private boolean isForcedWalk = false;
    private boolean shouldZoomToRoute = true;
    private boolean isRecalculating = false;
    private long lastRecalcTime = 0;
    private List<LatLng> points;

    // Map
    private GoogleMap mMap;
    private GeoApiContext mGeoApiContext;
    private final List<Marker> walkMarkers = new ArrayList<>();

    // Database
    private FirebaseFirestore db;
    private final ArrayList<walkModel> walkList = new ArrayList<>();

    // Permissions
    private static final int PERMISSION_ID = 44;

    // UI
    private View hintContainer;
    private RecyclerView walksRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_walks_map);
        EdgeToEdge.enable(this);

        db = FirebaseFirestore.getInstance();
        walksRV = findViewById(R.id.idRVWalks);
        hintContainer = findViewById(R.id.hint_container);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mGeoApiContext = new GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_key))
                .build();

        if (getIntent().getBooleanExtra("is_fresh_launch", false)) {
            clearRouteProgress();
        } else {
            loadRouteState();
        }

        updateUIVisibility();
        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        getLastLocation();

        double forcedLat = getIntent().getDoubleExtra("force_walk_lat", Double.NaN);
        double forcedLon = getIntent().getDoubleExtra("force_walk_lon", Double.NaN);
        if (!Double.isNaN(forcedLat) && !Double.isNaN(forcedLon)) {
            startForcedWalkAt(new LatLng(forcedLat, forcedLon));
        }
    }

    private void fetchWalks(double userLat, double userLon) {
        db.collection("CircularWalks")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        walkList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            GeoPoint walkLocation = document.getGeoPoint("location");
                            if (walkLocation != null) {
                                float[] results = new float[1];
                                Location.distanceBetween(userLat, userLon,
                                        walkLocation.getLatitude(), walkLocation.getLongitude(), results);
                                double distanceInMiles = results[0] / 1609.34;
                                walkList.add(new walkModel(name, distanceInMiles, walkLocation.getLongitude(), walkLocation.getLatitude(), null ));
                            }
                        }
                        walkList.sort(Comparator.comparingDouble(walkModel::getWalkDistance));


                        walksAdapter adapter = new walksAdapter(this, walkList, new walksAdapter.OnWalkClickListener() {
                            @Override
                            public void onWalkClick(walkModel walk) {
                                LatLng walkLoc = new LatLng(walk.getWalkLatitude(), walk.getWalkLongitude());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(walkLoc, 15f));
                            }
                            @Override
                            public void onRouteButtonClick(walkModel walk) {
                                startRoute(walk);
                            }
                        });
                        walksRV.setLayoutManager(new LinearLayoutManager(this));
                        walksRV.setAdapter(adapter);
                        updateMapMarkers();
                    }
                });
    }


    private void updateUIVisibility() {
        if (activeWalk != null) {
            walksRV.setVisibility(View.GONE);
            hintContainer.setVisibility(View.VISIBLE);
        } else {
            walksRV.setVisibility(View.VISIBLE);
            hintContainer.setVisibility(View.GONE);
        }
    }

    private void moveCameraToUser(Location location) {
        if (mMap != null && location != null) {
            LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 15f));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        enableUserLocation();
        updateMapMarkers();
        restoreRouteProgress();

        mMap.setOnMarkerClickListener(marker -> {
            walkModel walk = (walkModel) marker.getTag();
            if (walk == null) return false;

            LatLng loc = new LatLng(walk.getWalkLatitude(), walk.getWalkLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f));

            RecyclerView.Adapter rvAdapter = walksRV.getAdapter();
            if (rvAdapter instanceof walksAdapter) {
                walksAdapter adapter = (walksAdapter) rvAdapter;
                adapter.setSelectedWalk(walk);
                int index = walkList.indexOf(walk);
                if (index != -1) walksRV.scrollToPosition(index);
            }

            return true;
        });
    }

    // ---------------- DATABASE ----------------

    private void fetchRoute(LatLng origin, LatLng dest, boolean zoomToRoute) {
        DirectionsApi.newRequest(mGeoApiContext)
                .mode(TravelMode.WALKING)
                .origin(new com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(new com.google.maps.model.LatLng(dest.latitude, dest.longitude))
                .setCallback(new PendingResult.Callback<>() {
                    @Override
                    public void onResult(DirectionsResult result) {
                        if (result.routes == null || result.routes.length == 0) return;

                        // Decode polyline points
                        List<com.google.maps.model.LatLng> decoded = result.routes[0].overviewPolyline.decodePath();
                        fullRoutePoints.clear();
                        for (com.google.maps.model.LatLng p : decoded) {
                            fullRoutePoints.add(new LatLng(p.lat, p.lng));
                        }

                        runOnUiThread(() -> {
                            // Draw or update polyline
                            if (currentPolyline == null) {
                                currentPolyline = mMap.addPolyline(
                                        new PolylineOptions()
                                                .color(Color.BLUE)
                                                .width(12)
                                                .addAll(fullRoutePoints)
                                );
                            } else {
                                currentPolyline.setPoints(fullRoutePoints);
                            }
                            saveRouteProgress();
                        });
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.e(TAG, "Directions API failed", e);
                    }
                });
    }

    // ---------------- PERMISSIONS ----------------

    @SuppressLint("MissingPermission")
    private void enableUserLocation() {
        if (mMap != null && checkPermissions()) mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
            enableUserLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationTracking() {

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000
        ).setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location loc = locationResult.getLastLocation();
                if (loc != null) {

                    if (shouldZoomToRoute) {
                        moveCameraToUser(loc);
                        shouldZoomToRoute = false;
                    }

                    updateLiveRoute(loc);
                    checkProximity(loc);
                }
            }
        };

        mFusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    private void checkProximity(Location loc) {
        if (activeWalk == null) return;

        float[] dist = new float[1];
        Location.distanceBetween(loc.getLatitude(), loc.getLongitude(),
                activeWalk.getWalkLatitude(), activeWalk.getWalkLongitude(), dist);

        if (dist[0] < ARRIVAL_THRESHOLD) {
            if (isForcedWalk) {
                completeForcedWalk();
                return;
            }
            if (!isReturning) {
                isReturning = true;
                Toast.makeText(this, "Reached the pin! Now heading back.", Toast.LENGTH_LONG).show();
                fetchRoute(new LatLng(loc.getLatitude(), loc.getLongitude()), startPoint, false);
                activeWalk = new walkModel(activeWalk.getWalkName(), 0,startPoint.longitude, startPoint.latitude, null);
            } else completeRegularWalk();
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    // ---------------- ROUTE LOGIC ----------------

    private void loadRouteState() {
        SharedPreferences prefs = getSharedPreferences("WalkProgress", MODE_PRIVATE);
        String name = prefs.getString("activeWalkName", null);
        if (name != null) {
            float lat = prefs.getFloat("activeWalkLat", 0);
            float lon = prefs.getFloat("activeWalkLon", 0);
            activeWalk = new walkModel(name, 0, lon, lat, null);
            startPoint = new LatLng(prefs.getFloat("startLat", 0), prefs.getFloat("startLon", 0));
            isReturning = prefs.getBoolean("isReturning", false);
        }
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
                    Location location = task.getResult();
                    if (location != null) {
                        fetchWalks(location.getLatitude(), location.getLongitude());
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on your location...", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        } else requestPermissions();
    }

    // ---------------- LIVE ROUTE UPDATE & RECALC ----------------

    private void updateLiveRoute(Location userLocation) {
        if (activeWalk == null || fullRoutePoints.isEmpty()) return;

        LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
        LatLng snappedPoint = snapToRoute(userLatLng, fullRoutePoints);

        if (distanceBetween(userLatLng, snappedPoint) > OFF_ROUTE_THRESHOLD) {
            recalculateRoute(userLatLng);
            return;
        }

        trimPassedRoutePoints(snappedPoint);

        if (currentPolyline != null) currentPolyline.setPoints(fullRoutePoints);
        saveRouteProgress();
    }

    // Snap the user's location to the closest point on the route
    private LatLng snapToRoute(LatLng user, List<LatLng> route) {
        LatLng closest = null;
        double minDist = Double.MAX_VALUE;

        for (int i = 0; i < route.size() - 1; i++) {
            LatLng a = route.get(i);
            LatLng b = route.get(i + 1);
            LatLng candidate = closestPointOnSegment(user, a, b);
            double dist = distanceBetween(user, candidate);
            if (dist < minDist) {
                minDist = dist;
                closest = candidate;
            }
        }

        return closest != null ? closest : route.get(0);
    }

    // Find the closest point on a line segment AB to point P
    private LatLng closestPointOnSegment(LatLng p, LatLng a, LatLng b) {
        double dx = b.longitude - a.longitude;
        double dy = b.latitude - a.latitude;

        if (dx == 0 && dy == 0) return a;

        double t = ((p.longitude - a.longitude) * dx + (p.latitude - a.latitude) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        return new LatLng(a.latitude + t * dy, a.longitude + t * dx);
    }

    // Trim the points in the route that the user has already passed
    private void trimPassedRoutePoints(LatLng snappedPoint) {
        long now = System.currentTimeMillis();
        if (now - lastRecalcTime < RECALC_COOLDOWN_MS) return;
        while (!fullRoutePoints.isEmpty() && distanceBetween(fullRoutePoints.get(0), snappedPoint) < PROGRESS_THRESHOLD) {
            lastRecalcTime = now;
            fullRoutePoints.remove(0);
        }
    }


    private void recalculateRoute(LatLng userLatLng) {
        if (activeWalk == null || isRecalculating) return;

        long now = System.currentTimeMillis();
        if (now - lastRecalcTime < RECALC_COOLDOWN_MS) return;

        isRecalculating = true;
        lastRecalcTime = now;

        LatLng target = isReturning ? startPoint
                : new LatLng(activeWalk.getWalkLatitude(), activeWalk.getWalkLongitude());

        Toast.makeText(this, "Off-route detected — recalculating...", Toast.LENGTH_SHORT).show();

        DirectionsApi.newRequest(mGeoApiContext)
                .mode(TravelMode.WALKING)
                .origin(new com.google.maps.model.LatLng(userLatLng.latitude, userLatLng.longitude))
                .destination(new com.google.maps.model.LatLng(target.latitude, target.longitude))
                .setCallback(new PendingResult.Callback<>() {
                    @Override
                    public void onResult(DirectionsResult result) {
                        if (result.routes == null || result.routes.length == 0) {
                            isRecalculating = false;
                            return;
                        }

                        List<com.google.maps.model.LatLng> decoded = result.routes[0].overviewPolyline.decodePath();
                        fullRoutePoints.clear();
                        for (com.google.maps.model.LatLng p : decoded) {
                            fullRoutePoints.add(new LatLng(p.lat, p.lng));
                        }

                        runOnUiThread(() -> {
                            if (currentPolyline == null) {
                                currentPolyline = mMap.addPolyline(
                                        new PolylineOptions().color(Color.BLUE).width(12).addAll(fullRoutePoints)
                                );
                            } else currentPolyline.setPoints(fullRoutePoints);
                            saveRouteProgress();
                            isRecalculating = false;
                        });
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.e(TAG, "Route recalculation failed", e);
                        isRecalculating = false;
                    }
                });
    }
    private double distanceBetween(LatLng a, LatLng b) {
        float[] res = new float[1];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, res);
        return res[0];
    }

    @SuppressLint("MissingPermission")
    private void startRoute(walkModel walk) {
        mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                activeWalk = walk;
                isReturning = false;
                startPoint = new LatLng(location.getLatitude(), location.getLongitude());
                LatLng dest = new LatLng(walk.getWalkLatitude(), walk.getWalkLongitude());
                fetchRoute(startPoint, dest, true);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 15f));
                updateUIVisibility();
                updateMapMarkers();
            }
        });
    }

    private void saveRouteProgress() {
        if (activeWalk == null) return;

        SharedPreferences.Editor editor = getSharedPreferences("WalkProgress", MODE_PRIVATE).edit();
        editor.putString("activeWalkName", activeWalk.getWalkName());
        editor.putFloat("activeWalkLat", (float) activeWalk.getWalkLatitude());
        editor.putFloat("activeWalkLon", (float) activeWalk.getWalkLongitude());
        editor.putFloat("startLat", (float) startPoint.latitude);
        editor.putFloat("startLon", (float) startPoint.longitude);
        editor.putBoolean("isReturning", isReturning);

        if (currentPolyline != null) {
            points = currentPolyline.getPoints();
            StringBuilder sb = new StringBuilder();
            for (LatLng p : points) sb.append(p.latitude).append(",").append(p.longitude).append(";");
            editor.putString("polylinePoints", sb.toString());
        }

        editor.apply();
    }

    private void restoreRouteProgress() {
        SharedPreferences prefs = getSharedPreferences("WalkProgress", MODE_PRIVATE);
        String polylineStr = prefs.getString("polylinePoints", null);
        if (polylineStr != null) {
            fullRoutePoints.clear();
            String[] coords = polylineStr.split(";");
            for (String c : coords) {
                if (!c.isEmpty()) {
                    String[] latlon = c.split(",");
                    fullRoutePoints.add(new LatLng(Double.parseDouble(latlon[0]), Double.parseDouble(latlon[1])));
                }
            }
            if (!fullRoutePoints.isEmpty()) {
                currentPolyline = mMap.addPolyline(new PolylineOptions()
                        .color(Color.BLUE).width(12).addAll(fullRoutePoints));
            }
        }
    }

    private void clearRouteProgress() {
        SharedPreferences.Editor editor = getSharedPreferences("WalkProgress", MODE_PRIVATE).edit();
        editor.clear().apply();
    }

    // ---------------- FORCED WALK ----------------

    @SuppressLint("MissingPermission")
    private void startForcedWalkAt(LatLng dest) {
        isForcedWalk = true;

        walksRV.setVisibility(View.GONE);
        hintContainer.setVisibility(View.VISIBLE);

        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        mFusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && mMap != null) {

                    // Set starting point first
                    startPoint = new LatLng(location.getLatitude(), location.getLongitude());

                    // Initialize forced walk
                    activeWalk = new walkModel("Forced Walk", 0, dest.latitude, dest.longitude, null);
                    isReturning = false;

                    // Update UI safely
                    updateUIVisibility();

                    // Fetch route
                    fetchRoute(startPoint, dest, true);

                    // Stop location updates after first fix
                    mFusedLocationClient.removeLocationUpdates(this);
                }
            }
        }, Looper.getMainLooper());
    }

    private void completeForcedWalk() {
        isForcedWalk = false;
        Toast.makeText(this, "Forced walk completed!", Toast.LENGTH_LONG).show();
        clearRouteProgress();
        activeWalk = null;
        fullRoutePoints.clear();
        if (currentPolyline != null) currentPolyline.remove();
        updateUIVisibility();

        SharedPreferences tomagatchiPrefs = getSharedPreferences("WalkiesPrefs", MODE_PRIVATE);
        tomagatchiPrefs.edit()
                .putInt("walked", 100)
                .putLong("last_save_time", System.currentTimeMillis() / 1000)
                .apply();

        Intent intent = new Intent(CircularWalksMap.this, Tomagatchi.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void completeRegularWalk() {
        Toast.makeText(this, "Walk completed!", Toast.LENGTH_LONG).show();
        clearRouteProgress();
        activeWalk = null;
        fullRoutePoints.clear();
        if (currentPolyline != null) currentPolyline.remove();
        updateUIVisibility();

        SharedPreferences tomagatchiPrefs = getSharedPreferences("WalkiesPrefs", MODE_PRIVATE);
        tomagatchiPrefs.edit()
                .putInt("walked", 100)
                .putLong("last_save_time", System.currentTimeMillis() / 1000)
                .apply();

        Intent intent = new Intent(CircularWalksMap.this, Tomagatchi.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // ---------------- MAP MARKERS ----------------

    private void updateMapMarkers() {
        for (Marker marker : walkMarkers) marker.remove();
        walkMarkers.clear();
        if (mMap == null) return;
        if (isForcedWalk && activeWalk != null) {
            LatLng dest = new LatLng(activeWalk.getWalkLongitude(), activeWalk.getWalkLatitude());
            Marker marker = mMap.addMarker(new MarkerOptions().position(dest).title(activeWalk.getWalkName()));
            marker.setTag(activeWalk);
            walkMarkers.add(marker);
        } else{
            for (walkModel walk : walkList) {
                LatLng loc = new LatLng(walk.getWalkLatitude(), walk.getWalkLongitude());
                Marker marker = mMap.addMarker(new MarkerOptions().position(loc).title(walk.getWalkName()));
                marker.setTag(walk);
                walkMarkers.add(marker);
            }
        }
    }

    private void stopLocationTracking() {
        if (locationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationTracking();
    }


}