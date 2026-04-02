package com.example.walkies.mysteryWalks;

import com.example.walkies.walkModel;
import com.example.walkies.tamagotchi.TamagotchiRepository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;

public class MysteryWalksModel implements MysteryWalksContract.Model {

    private static final String TAG = "MysteryWalksModel";

    private final com.google.firebase.firestore.FirebaseFirestore db;
    private final com.google.android.gms.location.FusedLocationProviderClient client;
    private final TamagotchiRepository repository;

    private com.google.android.gms.location.LocationCallback trackingCallback;

    private int maxHint = 0;
    private int initialDistance = 0;

    public MysteryWalksModel(android.content.Context ctx) {

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        repository = new TamagotchiRepository(ctx.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE));

        client = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(ctx);
    }

    @Override
    public void setInitialDistance(int distance) {
        initialDistance = distance;
    }

    @Override
    public void setMaxHint(int hint) {
        if (hint > maxHint)
            maxHint = hint;
    }

    @Override
    public void loadWalks(Callback<java.util.List<walkModel>> cb) {
        String userCity = repository.getCity();
        DocumentReference cityRef = db.collection("Cities").document(userCity);
        
        db.collection("MysteryWalks")
                .whereEqualTo("city", cityRef)
                .get(Source.DEFAULT) 
                .addOnSuccessListener(q -> {

                    java.util.List<walkModel> list = new java.util.ArrayList<>();
                    boolean isFromCache = q.getMetadata().isFromCache();

                    for (var d : q) {
                        try {
                            var geo = d.getGeoPoint("Location");
                            if (geo == null) geo = d.getGeoPoint("location");
                            if (geo == null) continue;

                            String name = d.getString("name");
                            if (name == null) {
                                Object walkNo = d.get("WalkNo");
                                name = walkNo != null ? String.valueOf(walkNo) : d.getId();
                            }

                            list.add(new walkModel(
                                    name,
                                    0,
                                    geo.getLongitude(),
                                    geo.getLatitude(),
                                    new String[]{
                                            d.getString("HintOne") != null ? d.getString("HintOne") : d.getString("Hint1"),
                                            d.getString("HintTwo") != null ? d.getString("HintTwo") : d.getString("Hint2"),
                                            d.getString("HintThree") != null ? d.getString("HintThree") : d.getString("Hint3")
                                    }));

                        } catch (Exception e) {
                            Log.e(TAG, "Parse error " + d.getId(), e);
                        }
                    }

                    cb.call(list, isFromCache);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Load walks error", e);
                    cb.call(new java.util.ArrayList<>(), false);
                });
    }

    @Override
    @android.annotation.SuppressLint("MissingPermission")
    public void startTracking(LocationCallback cb) {

        var req = new com.google.android.gms.location.LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        trackingCallback = new com.google.android.gms.location.LocationCallback() {

            @Override
            public void onLocationResult(
                    @NonNull com.google.android.gms.location.LocationResult r) {

                if (r.getLastLocation() != null)
                    cb.call(r.getLastLocation());
            }
        };

        try {

            client.requestLocationUpdates(
                    req,
                    trackingCallback,
                    android.os.Looper.getMainLooper());

        } catch (Exception e) {
            Log.e(TAG, "Location update error", e);
        }
    }

    @Override
    public void stopTracking() {

        if (trackingCallback != null) {

            client.removeLocationUpdates(trackingCallback);

            trackingCallback = null;
        }
    }

    @Override
    @android.annotation.SuppressLint("MissingPermission")
    public void getLastLocation(LocationCallback cb) {

        try {

            client.getLastLocation()
                    .addOnSuccessListener(lastLoc -> {
                        if (lastLoc != null)
                            cb.call(lastLoc);
                    });

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(freshLoc -> {
                        if (freshLoc != null)
                            cb.call(freshLoc);
                    });

        } catch (Exception e) {

            Log.e(TAG, "Location fetch error", e);

            cb.call(null);
        }
    }

    @Override
    public void saveCompletion() {

        double maxDistanceMeters = 1609.34;

        double distanceRatio = Math.min(initialDistance / maxDistanceMeters, 1.0);

        int baseXp = (int) Math.round(distanceRatio * 200);

        double hintMultiplier;

        switch (maxHint) {
            case 1: hintMultiplier = 1.0; break;
            case 2: hintMultiplier = 0.75; break;
            default: hintMultiplier = 0.50;
        }

        int earnedXp = (int) Math.round(baseXp * hintMultiplier);

        repository.addWalkRewards(earnedXp, 75, false);

        updateFirestoreStats();
    }

    private void updateFirestoreStats() {
        String username = repository.getUsername().toLowerCase();
        if (!username.isEmpty()) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> updates = new HashMap<>();
            updates.put("lifetimeXP", repository.getLifetimeXP());
            updates.put("monthlyXP", repository.getMonthlyXP());
            updates.put("level", repository.getLevel());
            updates.put("city", repository.getCity());

            db.collection("Users").document(username)
                    .update(updates)
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating Firestore stats", e));
        }
    }

    @Override
    public void reset() {
        initialDistance = 0;
        maxHint = 0;
    }

    @Override
    public void shutdown() {
        stopTracking();
    }

}
