package com.example.walkies.mysteryWalks;

import com.example.walkies.walkModel;

public class MysteryWalksModel implements MysteryWalksContract.Model {

    private final android.content.Context ctx;
    private final com.google.firebase.firestore.FirebaseFirestore db;
    private final com.google.android.gms.location.FusedLocationProviderClient client;
    private com.google.android.gms.location.LocationCallback callback;

    public MysteryWalksModel(android.content.Context ctx){
        this.ctx = ctx;
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        client = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(ctx);
    }

    @Override
    public void loadWalks(Callback<java.util.List<walkModel>> cb){

        db.collection("MysteryWalks").get().addOnSuccessListener(q->{

            java.util.List<walkModel> list=new java.util.ArrayList<>();

            for(var d:q){
                var geo=d.getGeoPoint("Location");
                if(geo==null) continue;

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
    public void startTracking(LocationCallback cb){

        var req=new com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        callback=new com.google.android.gms.location.LocationCallback(){
            @Override public void onLocationResult(
                    com.google.android.gms.location.LocationResult r){
                cb.call(r.getLastLocation());
            }
        };

        client.requestLocationUpdates(req,callback,
                android.os.Looper.getMainLooper());
    }

    @Override
    public void stopTracking(){
        if(callback!=null)
            client.removeLocationUpdates(callback);
    }

    @Override
    @android.annotation.SuppressLint("MissingPermission")
    public void getLastLocation(LocationCallback cb){
        client.getLastLocation().addOnSuccessListener(cb::call);
    }

    @Override
    public void saveCompletion(){
        var p=ctx.getSharedPreferences("WalkiesPrefs",
                android.content.Context.MODE_PRIVATE);

        p.edit()
                .putInt("walked",100)
                .putLong("last_save_time",System.currentTimeMillis()/1000)
                .apply();
    }
}
