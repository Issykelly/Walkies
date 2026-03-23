package com.example.walkies.mysteryWalks;

import com.example.walkies.walkModel;
import android.util.Log;
import android.location.Location;
import com.google.android.gms.location.Priority;

public class MysteryWalksModel implements MysteryWalksContract.Model {

    private static final String TAG = "MysteryWalksModel";

    private final android.content.Context ctx;
    private final com.google.firebase.firestore.FirebaseFirestore db;
    private final com.google.android.gms.location.FusedLocationProviderClient client;

    private com.google.android.gms.location.LocationCallback trackingCallback;

    private int maxHint = 0;
    private int initialDistance = 0;

    public MysteryWalksModel(android.content.Context ctx) {
        this.ctx = ctx;

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

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

        db.collection("MysteryWalks").get()
                .addOnSuccessListener(q -> {

                    java.util.List<walkModel> list = new java.util.ArrayList<>();

                    if (q != null) {

                        for (var d : q) {

                            try {

                                var geo = d.getGeoPoint("Location");

                                if (geo == null)
                                    geo = d.getGeoPoint("location");

                                if (geo == null)
                                    continue;

                                String name = d.getString("name");

                                if (name == null) {
                                    Object walkNo = d.get("WalkNo");
                                    name = walkNo != null
                                            ? String.valueOf(walkNo)
                                            : d.getId();
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
                    }

                    cb.call(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Load walks error", e);
                    cb.call(new java.util.ArrayList<>());
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
                    com.google.android.gms.location.LocationResult r) {

                if (r != null && r.getLastLocation() != null)
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
            case 3: hintMultiplier = 0.50; break;
            default: hintMultiplier = 0.50;
        }

        int earnedXp = (int) Math.round(baseXp * hintMultiplier);

        var p = ctx.getSharedPreferences(
                "WalkiesPrefs",
                android.content.Context.MODE_PRIVATE);

        int currentXp = p.getInt("xp", 0);
        int currentCoins = p.getInt("coins", 0);

        p.edit()
                .putInt("walked", 100)
                .putLong("last_save_time", System.currentTimeMillis() / 1000)
                .putInt("coins", currentCoins + 75)
                .putInt("lifetime_coins", p.getInt("lifetime_coins", 0) + 75)
                .putInt("xp", currentXp + earnedXp)
                .putInt("lifetime_xp", p.getInt("lifetime_xp", 0) + earnedXp)
                .putInt("lifetime_mystery", p.getInt("lifetime_mystery", 0) + 1)
                .apply();
    }



    public void reset() {
        initialDistance = 0;
        maxHint = 0;
    }

}
