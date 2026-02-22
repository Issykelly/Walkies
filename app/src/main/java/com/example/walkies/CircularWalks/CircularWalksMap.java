package com.example.walkies.CircularWalks;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkies.R;
import com.example.walkies.Tamagotchi.Tamagotchi;
import com.example.walkies.walkModel;
import com.example.walkies.walksAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

public class CircularWalksMap extends AppCompatActivity
        implements OnMapReadyCallback, CircularWalksContract.View {

    private GoogleMap map;
    private CircularWalksPresenter presenter;
    private Polyline polyline;
    private RecyclerView rv;
    private walksAdapter adapter;
    private SupportMapFragment mapFragment;
    private ImageButton backBtn;
    private boolean mapReady = false;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private int retryCount = 0;
    private FusedLocationProviderClient client;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_circular_walks_map);

        rv = findViewById(R.id.idRVWalks);
        rv.setLayoutManager(new LinearLayoutManager(this));

        presenter = new CircularWalksPresenter(this);

        mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        client = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        getLocation();

        double forcedLat = getIntent().getDoubleExtra("force_walk_lat", Double.NaN);
        double forcedLon = getIntent().getDoubleExtra("force_walk_lon", Double.NaN);

        if (!Double.isNaN(forcedLat) && !Double.isNaN(forcedLon)) {
            presenter.startForcedWalk(new LatLng(forcedLat, forcedLon));
        }

        backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, Tamagotchi.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap g) {
        mapReady = true;
        map = g;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

        presenter.onMapReady();
    }

    public boolean isMapReady() {
        return mapReady;
    }


    // ---------------- LOCATION UPDATE FROM SERVICE / CLIENT ----------------
    // Call this from your LocationCallback
    public void updateLocation(Location location){
        presenter.onLocationReceived(location);
    }

    // ---------------- VIEW METHODS ----------------

    @Override
    public void showWalks(List<walkModel> walks) {

        if (adapter == null) {

            adapter = new walksAdapter(
                    this,
                    new ArrayList<>(walks),
                    new walksAdapter.OnWalkClickListener() {

                        @Override
                        public void onWalkClick(walkModel walk) {
                            presenter.onWalkSelected(walk);
                        }

                        @Override
                        public void onRouteButtonClick(walkModel walk) {
                            presenter.onRouteRequested(walk);
                        }
                    });

            rv.setAdapter(adapter);

        } else {
            adapter.updateData(walks);
        }
    }

    @Override
    public void showMarkers(List<walkModel> walks) {

        if (map == null) return;

        map.clear();

        for (walkModel walk : walks) {
            map.addMarker(new MarkerOptions()
                    .position(new LatLng(
                            walk.getWalkLatitude(),
                            walk.getWalkLongitude()))
                    .title(walk.getWalkName()));
        }
    }

    @Override
    public void showForcedWalkMarker(LatLng pendingForcedWalkDest) {
        if (map == null) return;

        map.clear();
        map.addMarker(new MarkerOptions()
                .position(pendingForcedWalkDest)
                .title("Mystery Location"));
    }

    @Override
    public void showRoute(List<LatLng> points) {

        runOnUiThread(() -> {
            if (map == null) return;

            if (polyline == null)
                polyline = map.addPolyline(
                        new PolylineOptions()
                                .width(12f)
                                .color(0xFF2196F3)
                                .addAll(points)
                );
            else
                polyline.setPoints(points);
        });
    }

    public void moveCamera(LatLng latLng, float zoom) {
        if (map != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        }
    }

    @Override
    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void toggleWalkList(boolean show) {
        findViewById(R.id.idRVWalks)
                .setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    @Override
    public Context getContext() {
        return this;
    }

    public void getLocation() {
        client.getLastLocation().addOnSuccessListener(location -> {
            if (location != null)
                updateLocation(location);
        });
    }



    // ---------------- LIFECYCLE ----------------

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause();
    }
}