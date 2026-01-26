package com.example.walkies;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CircularWalksMap extends AppCompatActivity {

    private static final String TAG = "WalkiesDebug";
    private FirebaseFirestore db;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final int PERMISSION_ID = 44;
    private RecyclerView walksRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_walks_map);
        EdgeToEdge.enable(this);

        db = FirebaseFirestore.getInstance();
        walksRV = findViewById(R.id.idRVWalks);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Log.d(TAG, "!!! ACTIVITY STARTED !!!");
        getLastLocation();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            fetchWalks(location.getLatitude(), location.getLongitude());
                        }
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

    private void fetchWalks(double userLat, double userLon) {
        Log.d(TAG, "Fetching walks for Location: " + userLat + ", " + userLon);
        db.collection("CircularWalks")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            ArrayList<walkModel> walkList = new ArrayList<>();
                            
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

                            // Sort the list by distance
                            Collections.sort(walkList, new Comparator<walkModel>() {
                                @Override
                                public int compare(walkModel o1, walkModel o2) {
                                    return Double.compare(o1.getWalkDistance(), o2.getWalkDistance());
                                }
                            });

                            // Setup RecyclerView with the adapter
                            walksAdapter adapter = new walksAdapter(CircularWalksMap.this, walkList);
                            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(CircularWalksMap.this, LinearLayoutManager.VERTICAL, false);
                            walksRV.setLayoutManager(linearLayoutManager);
                            walksRV.setAdapter(adapter);

                            Log.d(TAG, "--- Sorted Walks by Distance ---");
                            for (walkModel walk : walkList) {
                                Log.d(TAG, String.format("Walk: %s | Distance: %.2f miles", walk.getWalkName(), walk.getWalkDistance()));
                            }
                            
                        } else {
                            Log.e(TAG, "Firestore connection: FAILED", task.getException());
                        }
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdates(1)
                .build();

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location mLastLocation = locationResult.getLastLocation();
                if (mLastLocation != null) {
                    fetchWalks(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                }
            }
        }, Looper.myLooper());
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED 
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        }
    }
}
