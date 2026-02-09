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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.maps.model.Marker;

import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CircularWalksMap extends AppCompatActivity implements OnMapReadyCallback {

    // logcat debug
    // ---------------------------------------------------------------------------------------
    private static final String TAG = "WalkiesDebug";

    // routing logic variables
    // ---------------------------------------------------------------------------------------
    private static final float PROGRESS_THRESHOLD = 15f; // meters

    private List<LatLng> fullRoutePoints = new ArrayList<>();
    private FusedLocationProviderClient mFusedLocationClient;
    private final ArrayList<walkModel> walkList = new ArrayList<>();
    private Polyline currentPolyline;
    private walkModel activeWalk;
    private LatLng startPoint;
    private boolean isReturning = false;
    private static final float ARRIVAL_THRESHOLD = 50.0f;
    private static final float OFF_ROUTE_THRESHOLD = 45f;
    private static final long RECALC_COOLDOWN_MS = 15_000;
    private long lastRecalcTime = 0;
    private boolean isRecalculating = false;
    List<LatLng> points;
    private boolean initialZoomDone = false;
    private GoogleMap mMap;
    private GeoApiContext mGeoApiContext;
    private final List<Marker> walkMarkers = new ArrayList<>();
    // database variables
    // ---------------------------------------------------------------------------------------
    private FirebaseFirestore db;
    // permissions variables
    // ---------------------------------------------------------------------------------------
    private static final int PERMISSION_ID = 44;
    // ui variables
    // ---------------------------------------------------------------------------------------
    private View hintContainer;
    private RecyclerView walksRV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_walks_map);
        EdgeToEdge.enable(this);

        //fetch database
        db = FirebaseFirestore.getInstance();

        //find containers
        walksRV = findViewById(R.id.idRVWalks);
        hintContainer = findViewById(R.id.hint_container);

        //location services
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mGeoApiContext = new GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_key))
                .build();

        // Check if we should clear progress (fresh launch from Tomagatchi)
        // as if the map is loading from the tomadatchi screen, we don't wanna load into a walk
        if (getIntent().getBooleanExtra("is_fresh_launch", false)) {
            clearRouteProgress();
        } else {
            // go straight into previous walk
            loadRouteState();
        }

        updateUIVisibility();

        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> {
            clearRouteProgress();
            Intent intent = new Intent(CircularWalksMap.this, Tomagatchi.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Log.d(TAG, "!!! ACTIVITY STARTED !!!");
        getLastLocation();
        startLocationTracking();
    }

    // ui management
    // ---------------------------------------------------------------------------------------
    private void updateUIVisibility() {
        if (activeWalk != null) {
            walksRV.setVisibility(View.GONE);
            hintContainer.setVisibility(View.VISIBLE);
        } else {
            walksRV.setVisibility(View.VISIBLE);
            hintContainer.setVisibility(View.GONE);
        }
    }
    // map management
    // ---------------------------------------------------------------------------------------
    private void moveCameraToUser(Location location) {
        if (mMap != null && location != null) {
            LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 15f));
            initialZoomDone = true;
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

            LatLng loc = new LatLng(
                    walk.getWalkLatitude(),
                    walk.getWalkLongitude()
            );
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f));

            RecyclerView.Adapter rvAdapter = walksRV.getAdapter();
            if (rvAdapter instanceof walksAdapter) {
                walksAdapter adapter = (walksAdapter) rvAdapter;
                adapter.setSelectedWalk(walk);

                int index = walkList.indexOf(walk);
                if (index != -1) {
                    walksRV.scrollToPosition(index);
                }
            }

            return true;
        });



    }

    // database connection & fetching
    // ----------------------------------------------------------------------------------------

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
                                walkList.add(new walkModel(name, distanceInMiles, walkLocation.getLongitude(), walkLocation.getLatitude()));
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

    // permissions
    // ----------------------------------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void enableUserLocation() {
        if (mMap != null && checkPermissions()) {
            mMap.setMyLocationEnabled(true);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
            enableUserLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationTracking() {
        // Request updates every 5 seconds, but allow faster updates if available
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        mFusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location lastLoc = locationResult.getLastLocation();
                if (lastLoc != null) {
                    if (!initialZoomDone) {
                        moveCameraToUser(lastLoc);
                    }

                    // Logic to update route as user moves
                    updateLiveRoute(lastLoc);
                    checkProximity(lastLoc);
                }
            }
        }, Looper.myLooper());
    }

    private void checkProximity(Location currentLoc) {
        if (activeWalk == null) return;

        float[] dist = new float[1];
        if (!isReturning) {
            Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(),
                    activeWalk.getWalkLatitude(), activeWalk.getWalkLongitude(), dist);

            if (dist[0] < ARRIVAL_THRESHOLD) {
                isReturning = true;
                Toast.makeText(this, "Reached the pin! Now heading back.", Toast.LENGTH_LONG).show();
                fetchRoute(new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude()), startPoint);
            }
        } else {
            Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(),
                    startPoint.latitude, startPoint.longitude, dist);

            if (dist[0] < ARRIVAL_THRESHOLD) {
                activeWalk = null;
                isReturning = false;
                if (currentPolyline != null) currentPolyline.remove();
                clearRouteProgress(); // Clears persistent state
                updateUIVisibility();
                Toast.makeText(this, "Welcome back! Walk completed.", Toast.LENGTH_LONG).show();

                // Update Tomagatchi stats: set walked to full (100)
                // Also reset last_save_time so stats don't drop for the duration of the walk
                SharedPreferences tomagatchiPrefs = getSharedPreferences("WalkiesPrefs", MODE_PRIVATE);
                tomagatchiPrefs.edit()
                        .putInt("walked", 100)
                        .putLong("last_save_time", System.currentTimeMillis() / 1000)
                        .apply();

                activeWalk = null;
                updateMapMarkers();

                // Return to Tomagatchi activity
                Intent intent = new Intent(CircularWalksMap.this, Tomagatchi.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    // checks if the device is using its location services
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // checks if the user is able to use location services
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // route logic
    // -----------------------------------------------------------------------------------------

    private void loadRouteState() {
        SharedPreferences prefs = getSharedPreferences("WalkProgress", MODE_PRIVATE);
        String name = prefs.getString("activeWalkName", null);
        if (name != null) {
            float lat = prefs.getFloat("activeWalkLat", 0);
            float lon = prefs.getFloat("activeWalkLon", 0);
            activeWalk = new walkModel(name, 0, lon, lat);
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
                        moveCameraToUser(location);
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }
    private void updateLiveRoute(Location currentLoc) {
        if (activeWalk == null || fullRoutePoints.isEmpty()) return;

        LatLng user = new LatLng(
                currentLoc.getLatitude(),
                currentLoc.getLongitude()
        );

        LatLng snapped = snapToRoute(user, fullRoutePoints);

        if (distanceBetween(user, snapped) > OFF_ROUTE_THRESHOLD) {
            recalculateRoute(user);
            return;
        }

        int trimIndex = -1;

        for (int i = 0; i < fullRoutePoints.size(); i++) {
            double d = distanceBetween(user, fullRoutePoints.get(i));
            if (d < PROGRESS_THRESHOLD) {
                trimIndex = i;
            } else {
                break;
            }
        }

        if (trimIndex > 0) {
            fullRoutePoints = new ArrayList<>(
                    fullRoutePoints.subList(trimIndex, fullRoutePoints.size())
            );

            if (currentPolyline != null) {
                currentPolyline.setPoints(fullRoutePoints);
            }
        }

    }

    private double distanceBetween(LatLng a, LatLng b) {
        float[] res = new float[1];
        Location.distanceBetween(
                a.latitude, a.longitude,
                b.latitude, b.longitude,
                res
        );
        return res[0];
    }

    private LatLng snapToRoute(LatLng user, List<LatLng> route) {
        LatLng closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < route.size() - 1; i++) {
            LatLng a = route.get(i);
            LatLng b = route.get(i + 1);

            LatLng snapped = closestPointOnSegment(user, a, b);
            double dist = distanceBetween(snapped, user);

            if (dist < minDistance) {
                minDistance = dist;
                closestPoint = snapped;
            }
        }
        return closestPoint != null ? closestPoint : user;
    }

    private void recalculateRoute(LatLng userLocation) {
        if (activeWalk == null || isRecalculating) return;

        long now = System.currentTimeMillis();
        if (now - lastRecalcTime < RECALC_COOLDOWN_MS) return;

        isRecalculating = true;
        lastRecalcTime = now;

        LatLng target = isReturning
                ? startPoint
                : new LatLng(activeWalk.getWalkLatitude(), activeWalk.getWalkLongitude());

        Toast.makeText(this, "Off route — recalculating…", Toast.LENGTH_SHORT).show();

        DirectionsApi.newRequest(mGeoApiContext)
                .mode(TravelMode.WALKING)
                .origin(new com.google.maps.model.LatLng(
                        userLocation.latitude, userLocation.longitude))
                .destination(new com.google.maps.model.LatLng(
                        target.latitude, target.longitude))
                .setCallback(new com.google.maps.PendingResult.Callback<>() {
                    @Override
                    public void onResult(DirectionsResult result) {
                        if (result.routes == null || result.routes.length == 0) {
                            isRecalculating = false;
                            return;
                        }

                        List<com.google.maps.model.LatLng> decoded =
                                result.routes[0].overviewPolyline.decodePath();

                        fullRoutePoints.clear();
                        for (com.google.maps.model.LatLng p : decoded) {
                            fullRoutePoints.add(new LatLng(p.lat, p.lng));
                        }

                        runOnUiThread(() -> {
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

    private LatLng closestPointOnSegment(LatLng p, LatLng a, LatLng b) {
        double dx = b.latitude - a.latitude;
        double dy = b.longitude - a.longitude;

        if (dx == 0 && dy == 0) return a;

        double t = ((p.latitude - a.latitude) * dx +
                (p.longitude - a.longitude) * dy) /
                (dx * dx + dy * dy);

        t = Math.max(0, Math.min(1, t));

        return new LatLng(
                a.latitude + t * dx,
                a.longitude + t * dy
        );
    }

    @SuppressLint("MissingPermission")
    private void startRoute(walkModel walk) {
        mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                activeWalk = walk;
                isReturning = false;
                startPoint = new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                );

                LatLng dest = new LatLng(
                        walk.getWalkLatitude(),
                        walk.getWalkLongitude()
                );

                fetchRoute(startPoint, dest);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 15f));

                updateUIVisibility();

                updateMapMarkers();
            }
        });
    }


    private void fetchRoute(LatLng origin, LatLng dest) {
        DirectionsApi.newRequest(mGeoApiContext)
                .mode(TravelMode.WALKING)
                .origin(new com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(new com.google.maps.model.LatLng(dest.latitude, dest.longitude))
                .setCallback(new com.google.maps.PendingResult.Callback<>() {
                    @Override
                    public void onResult(DirectionsResult result) {
                        if (result.routes == null || result.routes.length == 0) return;

                        List<com.google.maps.model.LatLng> decoded =
                                result.routes[0].overviewPolyline.decodePath();

                        fullRoutePoints.clear();
                        for (com.google.maps.model.LatLng p : decoded) {
                            fullRoutePoints.add(new LatLng(p.lat, p.lng));
                        }

                        runOnUiThread(() -> {
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
            for (LatLng p : points) {
                sb.append(p.latitude).append(",").append(p.longitude).append(";");
            }
            editor.putString("polylinePoints", sb.toString());
        }
        editor.apply();
    }

    private void clearRouteProgress() {
        getSharedPreferences("WalkProgress", MODE_PRIVATE).edit().clear().apply();
        activeWalk = null;
        isReturning = false;
        initialZoomDone = false;
        if (currentPolyline != null) {
            currentPolyline.remove();
            currentPolyline = null;
        }
        updateUIVisibility();
    }

    private void restoreRouteProgress() {
        if (mMap == null) return;

        SharedPreferences prefs = getSharedPreferences("WalkProgress", MODE_PRIVATE);
        String ptsStr = prefs.getString("polylinePoints", null);

        if (ptsStr != null) {
            points = new ArrayList<>();
            fullRoutePoints = new ArrayList<>(points);
            String[] pairs = ptsStr.split(";");
            for (String pair : pairs) {
                String[] latLon = pair.split(",");
                if (latLon.length == 2) {
                    points.add(new LatLng(Double.parseDouble(latLon[0]), Double.parseDouble(latLon[1])));
                }
            }

            currentPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(Color.BLUE)
                    .width(12));
        }
    }

    private void updateMapMarkers() {
        if (mMap == null) return;

        // Remove old markers only (NOT map.clear)
        for (Marker marker : walkMarkers) {
            marker.remove();
        }
        walkMarkers.clear();

        for (walkModel walk : walkList) {

            if (activeWalk != null && !walk.equals(activeWalk)) {
                continue;
            }

            LatLng loc = new LatLng(
                    walk.getWalkLatitude(),
                    walk.getWalkLongitude()
            );

            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(loc)
                            .title(walk.getWalkName())
            );

            marker.setTag(walk);
            walkMarkers.add(marker);
        }
    }


    // on pause & resume logic
    // -----------------------------------------------------------------------------------------

    @Override
    protected void onPause() {
        super.onPause();
        saveRouteProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            restoreRouteProgress();
        }
        getLastLocation();
    }
}
