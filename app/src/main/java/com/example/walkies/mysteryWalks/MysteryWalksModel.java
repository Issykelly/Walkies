package com.example.walkies.mysteryWalks;

import com.example.walkies.walkModel;

public class MysteryWalksModel implements MysteryWalksContract.Model {

    private final android.content.Context ctx;
    private final com.google.firebase.firestore.FirebaseFirestore db;
    private final com.google.android.gms.location.FusedLocationProviderClient client;
    private com.google.android.gms.location.LocationCallback callback;
    public int maxHint = 0;
    public int initialDistance = 0;

    public MysteryWalksModel(android.content.Context ctx) {
        this.ctx = ctx;
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        client = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(ctx);
    }

    public void setInitialDistance(int distance) {
        initialDistance = distance;
    }

    @Override
    public void loadWalks(Callback<java.util.List<walkModel>> cb) {

        db.collection("MysteryWalks").get().addOnSuccessListener(q -> {

            java.util.List<walkModel> list = new java.util.ArrayList<>();

            for (var d : q) {
                var geo = d.getGeoPoint("Location");
                if (geo == null) continue;

                list.add(new walkModel(
                        String.valueOf(d.getLong("WalkNo")),
                        0,
                        geo.getLongitude(),
                        geo.getLatitude(),
                        new String[]{
                                d.getString("HintOne"),
                                d.getString("HintTwo"),
                                d.getString("HintThree")
                        }));
            }

            cb.call(list);
        });
    }

    @Override
    @android.annotation.SuppressLint("MissingPermission")
    public void startTracking(LocationCallback cb) {

        var req = new com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        callback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(
                    com.google.android.gms.location.LocationResult r) {
                cb.call(r.getLastLocation());
            }
        };

        client.requestLocationUpdates(req, callback,
                android.os.Looper.getMainLooper());
    }

    @Override
    public void stopTracking() {
        if (callback != null)
            client.removeLocationUpdates(callback);
    }

    @Override
    @android.annotation.SuppressLint("MissingPermission")
    public void getLastLocation(LocationCallback cb) {
        client.getLastLocation().addOnSuccessListener(cb::call);
    }

    @Override
    public void saveCompletion() {

        double maxDistanceMeters = 1609.34; // 1 miles, might change if walks don't end up being that long
        double distanceRatio = Math.min(initialDistance / maxDistanceMeters, 1.0);

        int baseXp = (int) Math.round(distanceRatio * 200);

        double hintMultiplier;

        // xp is decided based on how many hints used
        switch (maxHint) {
            case 1:
                hintMultiplier = 1.0;
                break;
            case 2:
                hintMultiplier = 0.75;
                break;
            case 3:
                hintMultiplier = 0.50;
                break;
            default:
                hintMultiplier = 0.50;  // safety fallback
        }

        int earnedXp = (int) Math.round(baseXp * hintMultiplier);

        var p = ctx.getSharedPreferences("WalkiesPrefs",
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
}
