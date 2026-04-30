package com.example.walkies.circularWalks;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.example.walkies.R;
import com.example.walkies.tamagotchi.TamagotchiRepository;
import com.example.walkies.walkModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.DocumentReference;
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
    private final TamagotchiRepository repository;

    public CircularWalksModel(Context ctx) {
        geoApiContext = new GeoApiContext.Builder()
                .apiKey(ctx.getString(R.string.google_maps_key))
                .build();
        repository = new TamagotchiRepository(ctx);
    }

    @Override
    public void fetchWalks(double lat, double lon, WalksCallback cb) {
        String userCity = repository.getCity();
        DocumentReference cityRef = db.collection("Cities").document(userCity);

        db.collection("CircularWalks")
                .whereEqualTo("city", cityRef)
                .get()
                .addOnSuccessListener(res -> {
                    List<walkModel> list = new ArrayList<>();
                    Log.d("CircularWalksModel", "Fetched " + res.size() + " documents from Firestore for city: " + userCity);

                    for (QueryDocumentSnapshot doc : res) {
                        String name = doc.getString("name");
                        GeoPoint geo = doc.getGeoPoint("location");
                        
                        if (geo == null) {
                            Log.w("CircularWalksModel", "Document " + doc.getId() + " is missing 'location' field");
                            continue;
                        }

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
                })
                .addOnFailureListener(e -> {
                    Log.e("CircularWalksModel", "Error fetching walks: ", e);
                    cb.onLoaded(new ArrayList<>()); 
                });
    }

    private LatLng snapToRoad(LatLng point) {
        try {
            var roadsApi = com.google.maps.RoadsApi.nearestRoads(
                    geoApiContext,
                    new com.google.maps.model.LatLng(point.latitude, point.longitude)
            ).await();

            if (roadsApi != null && roadsApi.length > 0) {
                var snapped = roadsApi[0].location;
                return new LatLng(snapped.lat, snapped.lng);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return point;
    }


    @Override
    public void fetchRoute(LatLng origin, LatLng dest, RouteCallback cb) {
        new Thread(() -> {
            LatLng snappedOrigin = snapToRoad(origin);
            LatLng snappedDest = snapToRoad(dest);

            fetchRouteWithMode(snappedOrigin, snappedDest, TravelMode.WALKING, points -> {

                if (points.isEmpty()) {
                    fetchRouteWithMode(snappedOrigin, snappedDest, TravelMode.DRIVING, drivingPoints -> {

                        if (drivingPoints.isEmpty()) {
                            List<LatLng> fallback = new ArrayList<>();
                            fallback.add(snappedOrigin);
                            fallback.add(snappedDest);

                            cb.onLoaded(fallback);
                        } else {
                            cb.onLoaded(drivingPoints);
                        }
                    });
                } else {
                    cb.onLoaded(points);
                }

            });
        }).start();
    }


    private void fetchRouteWithMode(LatLng origin, LatLng dest, TravelMode mode, RouteCallback cb) {
        DirectionsApi.newRequest(geoApiContext)
                .mode(mode)
                .origin(new com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                .destination(new com.google.maps.model.LatLng(dest.latitude, dest.longitude))
                .setCallback(new PendingResult.Callback<>() {
                    @Override
                    public void onResult(DirectionsResult result) {

                        List<LatLng> list = new ArrayList<>();

                        if (result.routes != null && result.routes.length > 0) {

                            var route = result.routes[0];
                            for (var leg : route.legs) {
                                for (var step : leg.steps) {
                                    var decoded = step.polyline.decodePath();
                                    for (var p : decoded) {
                                        list.add(new LatLng(p.lat, p.lng));
                                    }
                                }
                            }
                        }
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            cb.onLoaded(list);
                        });
                    }


                    @Override
                    public void onFailure(Throwable e) {
                        Log.e("CircularWalksModel", "API Error for mode " + mode + ": " + e.getMessage());
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            cb.onLoaded(new ArrayList<>());
                        });
                    }
                });
    }

    @Override
    public void shutdown() {
        if (geoApiContext != null) {
            new Thread(() -> {
                try {
                    geoApiContext.shutdown();
                } catch (Exception e) {
                    Log.e("CircularWalksModel", "Error shutting down GeoApiContext", e);
                }
            }).start();
        }
    }
}
