package com.example.walkies.circularWalks;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.example.walkies.R;
import com.example.walkies.walkModel;
import com.google.android.gms.maps.model.LatLng;
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

public class CircularWalksModel implements CircularWalksContract.Model {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final GeoApiContext geoApiContext;

    public CircularWalksModel(Context ctx) {
        geoApiContext = new GeoApiContext.Builder()
                .apiKey(ctx.getString(R.string.google_maps_key))
                .build();
    }

    @Override
    public void fetchWalks(double lat, double lon, WalksCallback cb) {
        db.collection("CircularWalks").get().addOnSuccessListener(res -> {

            List<walkModel> list = new ArrayList<>();

            for (QueryDocumentSnapshot doc : res) {
                String name = doc.getString("name");
                GeoPoint geo = doc.getGeoPoint("location");
                if (geo == null) continue;

                float[] r = new float[1];
                Location.distanceBetween(lat, lon, geo.getLatitude(), geo.getLongitude(), r);

                list.add(new walkModel(
                        name,
                        r[0] / 1609.34,
                        geo.getLongitude(),
                        geo.getLatitude(),
                        null
                ));
            }

            list.sort(Comparator.comparingDouble(walkModel::getWalkDistance));
            cb.onLoaded(list);
        });
    }

    @Override
    public void fetchRoute(LatLng origin, LatLng dest, RouteCallback cb) {

        DirectionsApi.newRequest(geoApiContext)
                .mode(TravelMode.WALKING)
                .origin(new com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(new com.google.maps.model.LatLng(dest.latitude, dest.longitude))
                .setCallback(new PendingResult.Callback<>() {

                    @Override
                    public void onResult(DirectionsResult result) {

                        List<LatLng> list = new ArrayList<>();

                        if (result.routes != null && result.routes.length > 0) {
                            for (var p : result.routes[0].overviewPolyline.decodePath())
                                list.add(new LatLng(p.lat, p.lng));
                        }

                        cb.onLoaded(list);
                    }

                    @Override public void onFailure(Throwable e) {
                        Log.e("Directions", String.valueOf(e));
                    }
                });
    }
}
