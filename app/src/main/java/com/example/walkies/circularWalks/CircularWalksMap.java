package com.example.walkies.circularWalks;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkies.R;
import com.example.walkies.tamagotchi.Tamagotchi;
import com.example.walkies.walkModel;
import com.example.walkies.walksAdapter;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;

public class CircularWalksMap extends AppCompatActivity
        implements OnMapReadyCallback, CircularWalksContract.View {

    private GoogleMap map;
    private CircularWalksPresenter presenter;
    private Polyline polyline;
    private RecyclerView rv;
    private walksAdapter adapter;
    private FrameLayout hintContainer;

    private FusedLocationProviderClient client;
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location loc : locationResult.getLocations()) {
                presenter.onLocationReceived(loc);
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        int orientation = getResources().getInteger(R.integer.app_orientation);
        setRequestedOrientation(orientation);

        super.setContentView(R.layout.activity_circular_walks_map);

        rv = findViewById(R.id.idRVWalks);
        hintContainer = findViewById(R.id.hint_container);
        rv.setLayoutManager(new LinearLayoutManager(this));

        presenter = new CircularWalksPresenter(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        client = LocationServices.getFusedLocationProviderClient(this);
        Intent intent = getIntent();
        if (intent.hasExtra("force_walk_lat") && intent.hasExtra("force_walk_lon")) {
            double lat = intent.getDoubleExtra("force_walk_lat", 0);
            double lon = intent.getDoubleExtra("force_walk_lon", 0);
            presenter.setForcedWalk(lat, lon);
        }

        ImageButton backBtn = findViewById(R.id.backButton);
        backBtn.setOnClickListener(v -> {
            Intent backIntent = new Intent(this, Tamagotchi.class);
            backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(backIntent);
            finish();
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        try {
            client.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    presenter.onLocationReceived(location);
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateDistanceMeters(5)
                .build();

        client.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        client.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (presenter != null) {
            presenter.onDestroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap g) {
        map = g;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }

        presenter.onMapReady();
    }

    @Override
    public void showWalks(List<walkModel> walks) {
        if (adapter == null) {
            adapter = new walksAdapter(new ArrayList<>(walks),
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
            map.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .position(new LatLng(walk.getWalkLatitude(), walk.getWalkLongitude()))
                    .title(walk.getWalkName()));
        }
    }

    @Override
    public void showForcedWalkMarker(LatLng pendingForcedWalkDest) {
        if (map == null) return;
        map.clear();
        map.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .position(pendingForcedWalkDest)
                .title("Mystery Location"));
    }

    @Override
    public void showRoute(List<LatLng> points) {
        runOnUiThread(() -> {
            if (map == null) return;

            if (polyline == null) {
                polyline = map.addPolyline(new com.google.android.gms.maps.model.PolylineOptions()
                        .width(12f)
                        .color(0xFF2196F3)
                        .addAll(points)
                );
            } else {
                polyline.setPoints(points);
            }
        });
    }

    @Override
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
        rv.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        if (show) {
            hintContainer.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void showHint() {
        rv.setVisibility(android.view.View.GONE);
        hintContainer.setVisibility(android.view.View.VISIBLE);
        TextView tvHint = findViewById(R.id.hint);
        if (tvHint != null) {
            tvHint.setText(R.string.look_around);
        }
    }

    @Override
    public void showLocationError() {
        runOnUiThread(() -> {
            android.view.View overlay = findViewById(R.id.location_error_overlay);
            if (overlay != null) {
                overlay.setVisibility(android.view.View.VISIBLE);
                findViewById(R.id.error_ok_button).setOnClickListener(v -> {
                    Intent backIntent = new Intent(this, Tamagotchi.class);
                    backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(backIntent);
                    finish();
                });
            }
        });
    }
}
