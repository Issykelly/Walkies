package com.example.walkies;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MysteryWalks extends AppCompatActivity {

    private static final int PERMISSION_ID = 44;
    private static final float ARRIVAL_THRESHOLD = 50f;
    private boolean hasLeftStart = false;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private final ArrayList<walkModel> walkList = new ArrayList<>();
    private RecyclerView walksRV;

    private walkModel activeWalk;
    private Location startLocation;

    private boolean hasReachedMystery = false;
    private boolean isTracking = false;

    // Hint UI
    private Group hintGroup;
    private TextView hintContainer, hintNumber, distanceContainer;
    private Button next, prev, giveup;
    private String[] hints;
    private int currentHint = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mystery_walks);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        walksRV = findViewById(R.id.idRVWalks);
        hintGroup = findViewById(R.id.hintingGroup);
        hintContainer = findViewById(R.id.hint_container);
        hintNumber = findViewById(R.id.hint_number);
        distanceContainer = findViewById(R.id.distance);
        giveup = findViewById(R.id.giveup);
        next = findViewById(R.id.next);
        prev = findViewById(R.id.prev);

        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MysteryWalks.this, Tomagatchi.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        setupHintButtons();
        checkLocationReady();
        fetchWalks();

        // Check if we should clear progress (fresh launch from Tomagatchi)
        // as if the map is loading from the tomadatchi screen, we don't wanna load into a walk
        if (getIntent().getBooleanExtra("is_fresh_launch", false)) {
            freshLaunch();
        }
    }

    // fetch walks
    // -----------------------------------------------------------------------------------------

    private void fetchWalks() {
        db.collection("MysteryWalks")
                .get()
                .addOnSuccessListener(query -> {

                    walkList.clear();

                    for (QueryDocumentSnapshot doc : query) {

                        String walkNo = String.valueOf(doc.getLong("WalkNo"));
                        GeoPoint geo = doc.getGeoPoint("Location");

                        if (geo == null) continue;

                        String[] walkHints = new String[]{
                                doc.getString("HintOne"),
                                doc.getString("HintTwo"),
                                doc.getString("HintThree")
                        };

                        walkList.add(new walkModel(
                                walkNo,
                                0,
                                geo.getLongitude(),
                                geo.getLatitude(),
                                walkHints
                        ));
                    }

                    setupRecycler();
                });
    }

    private void setupRecycler() {

        walksAdapter adapter = new walksAdapter(this, walkList,
                new walksAdapter.OnWalkClickListener() {

                    @Override
                    public void onWalkClick(walkModel walk) { }

                    @Override
                    public void onRouteButtonClick(walkModel walk) {
                        beginWalk(walk);
                    }
                });

        walksRV.setLayoutManager(new LinearLayoutManager(this));
        walksRV.setAdapter(adapter);
    }

    // start walk
    // -----------------------------------------------------------------------------------------

    private void beginWalk(walkModel walk) {

        activeWalk = walk;
        hasReachedMystery = false;
        hasLeftStart = false;

        hints = walk.getHints();
        currentHint = 0;

        if (hints != null && hints[0] != null) {
            hintGroup.setVisibility(View.VISIBLE);
            hintContainer.setText(hints[0]);
            hintNumber.setText("Hint 1/3");
        }

        walksRV.setVisibility(View.GONE);

        giveup.setOnClickListener(v -> {
            Intent intent = new Intent(this, CircularWalksMap.class);
            intent.putExtra("force_walk_lat", walk.getWalkLatitude()); // destination latitude
            intent.putExtra("force_walk_lon", walk.getWalkLongitude()); // destination longitude
            startActivity(intent);
            finish();
        });

        updateInitialDistance();

        startTracking();
    }

    @SuppressLint("MissingPermission")
    private void updateInitialDistance() {
        if (!checkPermissions() || activeWalk == null) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        float[] distanceResult = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(),
                                location.getLongitude(),
                                activeWalk.getWalkLatitude(),
                                activeWalk.getWalkLongitude(),
                                distanceResult
                        );
                        int rounded = Math.round(distanceResult[0]);
                        distanceContainer.setText("current distance: " + rounded + " metres");
                    }
                });
    }

    // tracking
    // -----------------------------------------------------------------------------------------

    private void startTracking() {

        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {

                Location loc = result.getLastLocation();
                if (loc == null || activeWalk == null) return;

                if (!isTracking) {
                    startLocation = loc;
                    isTracking = true;
                }

                checkProximity(loc);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (activeWalk != null) {
                    startTracking();  // resume tracking after permission granted
                }

            } else {
                Toast.makeText(this,
                        "Location permission required for Walks",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    private void stopTracking() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        isTracking = false;
    }

    // proximity
    // -----------------------------------------------------------------------------------------

    private void checkProximity(Location currentLoc) {

        if (currentLoc == null || activeWalk == null) return;

        float[] distanceResult = new float[1];

        if (!hasReachedMystery) {

            Location.distanceBetween(
                    currentLoc.getLatitude(),
                    currentLoc.getLongitude(),
                    activeWalk.getWalkLatitude(),
                    activeWalk.getWalkLongitude(),
                    distanceResult
            );

            int rounded = Math.round(distanceResult[0]);
            distanceContainer.setText("current distance: " + rounded + " metres");

            if (distanceResult[0] < ARRIVAL_THRESHOLD) {
                hasReachedMystery = true;
                completeWalk();
            }

        }
    }

    private void completeWalk() {
        stopTracking();

        SharedPreferences prefs =
                getSharedPreferences("WalkiesPrefs", MODE_PRIVATE);

        prefs.edit()
                .putInt("walked", 100)
                .putLong("last_save_time", System.currentTimeMillis() / 1000)
                .apply();

        startActivity(new Intent(this, Tomagatchi.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private void setupHintButtons() {

        next.setOnClickListener(v -> {
            if (hints != null &&
                    currentHint < hints.length - 1 &&
                    hints[currentHint + 1] != null) {

                currentHint++;
                hintContainer.setText(hints[currentHint]);
                hintNumber.setText("Hint " +
                        (currentHint + 1) + "/3");
            }
        });

        prev.setOnClickListener(v -> {
            if (hints != null && currentHint > 0) {

                currentHint--;
                hintContainer.setText(hints[currentHint]);
                hintNumber.setText("Hint " +
                        (currentHint + 1) + "/3");
            }
        });
    }

    // permission
    // -----------------------------------------------------------------------------------------

    private void checkLocationReady() {
        if (!checkPermissions()) requestPermissions();
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID);
    }

    private void freshLaunch(){
        hintGroup.setVisibility(View.GONE);
        walksRV.setVisibility(View.VISIBLE);
        hasLeftStart = false;
        activeWalk = null;
        hasReachedMystery = false;
        isTracking = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (activeWalk != null && !isTracking && checkPermissions()) {
            startTracking();
        }
    }
}