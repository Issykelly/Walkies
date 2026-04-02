package com.example.walkies.mysteryWalks;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkies.circularWalks.CircularWalksMap;
import com.example.walkies.R;
import com.example.walkies.tamagotchi.Tamagotchi;
import com.example.walkies.walkModel;
import com.example.walkies.walksAdapter;

import java.util.ArrayList;
import java.util.List;

public class MysteryWalks extends AppCompatActivity
        implements MysteryWalksContract.View {

    private static final int PERMISSION_ID = 44;

    private MysteryWalksContract.Presenter presenter;

    private RecyclerView rv;
    private walksAdapter adapter;
    private Group hintGroup;
    private TextView hintTxt, hintNum, dist;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        int orientation = getResources().getInteger(R.integer.app_orientation);
        setRequestedOrientation(orientation);

        setContentView(R.layout.activity_mystery_walks);

        presenter = new MysteryWalksPresenter(
                this,
                new MysteryWalksModel(this));

        rv = findViewById(R.id.idRVWalks);
        hintGroup = findViewById(R.id.hintingGroup);
        hintTxt = findViewById(R.id.hint_container);
        hintNum = findViewById(R.id.hint_number);
        dist = findViewById(R.id.distance);

        ImageButton back = findViewById(R.id.backButton);

        findViewById(R.id.next).setOnClickListener(v -> presenter.nextHint());
        findViewById(R.id.prev).setOnClickListener(v -> presenter.prevHint());
        findViewById(R.id.giveup).setOnClickListener(v -> presenter.giveUp());

        back.setOnClickListener(v -> openTamagotchi());

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new walksAdapter(new ArrayList<>(),
                new walksAdapter.OnWalkClickListener() {
                    @Override
                    public void onWalkClick(walkModel w) {}

                    @Override
                    public void onRouteButtonClick(walkModel w) {
                        presenter.walkSelected(w);
                    }
                });
        rv.setAdapter(adapter);

        clearIndicators();
        
        showHints(false);
        showWalkList(true);

        presenter.init(getIntent().getBooleanExtra("is_fresh_launch", false));
    }

    private void clearIndicators() {
        dist.setText(R.string.distance_label);
        hintTxt.setText(R.string.hint);
        hintNum.setText(getString(R.string.hint_1, 1));
    }

    @Override
    public void showWalks(List<walkModel> walks) {
        adapter.updateData(walks);
    }

    @Override
    public void showHint(String t, int i) {
        hintTxt.setText(t);
        hintNum.setText(getString(R.string.hint_1, i));
    }

    @Override
    public void showHints(boolean v) {
        hintGroup.setVisibility(v ? View.VISIBLE : View.GONE);
        if (!v) {
            clearIndicators();
        }
    }

    @Override
    public void showDistance(int m) {
        if (m < 0)
            dist.setText(R.string.distance_label);
        else
            dist.setText(getString(R.string.current_distance, m));
    }

    @Override
    public void showMessage(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showWalkList(boolean s) {
        rv.setVisibility(s ? View.VISIBLE : View.GONE);
    }

    @Override
    public void openMap(double lat, double lon) {
        Intent i = new Intent(this, CircularWalksMap.class);
        i.putExtra("force_walk_lat", lat);
        i.putExtra("force_walk_lon", lon);
        startActivity(i);
        finish();
    }

    @Override
    public void openTamagotchi() {
        Intent i = new Intent(this, Tamagotchi.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    @Override
    public void closeActivity() {
        finish();
    }

    @Override
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID);
    }

    @Override
    public void onRequestPermissionsResult(
            int r, @NonNull String[] p, @NonNull int[] g) {

        super.onRequestPermissionsResult(r, p, g);

        if (r == PERMISSION_ID)
            presenter.permissionResult(
                    g.length > 0 &&
                            g[0] == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.resume();
    }


    @Override protected void onPause() {
        super.onPause();
        presenter.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.onDestroy();
        }
    }

    @Override
    public void showLocationError() {
        Log.d("MysteryWalks", "showLocationError called");
        runOnUiThread(() -> {
            View overlay = findViewById(R.id.location_error_overlay);
            if (overlay != null) {
                overlay.setVisibility(View.VISIBLE);
                findViewById(R.id.error_ok_button).setOnClickListener(v -> openTamagotchi());
            } else {
                Log.e("MysteryWalks", "location_error_overlay not found!");
            }
        });
    }
}
