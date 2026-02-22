package com.example.walkies.MysteryWalks;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import com.example.walkies.CircularWalks.CircularWalksMap;
import com.example.walkies.R;
import com.example.walkies.Tamagotchi.Tamagotchi;
import com.example.walkies.walkModel;
import com.example.walkies.walksAdapter;

public class MysteryWalks extends AppCompatActivity
        implements MysteryWalksContract.View {

    private static final int PERMISSION_ID = 44;

    private MysteryWalksPresenter presenter;

    private RecyclerView rv;
    private Group hintGroup;
    private TextView hintTxt, hintNum, dist;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
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

        // Default UI state (important)
        showHints(false);
        showWalkList(true);

        presenter.init(getIntent().getBooleanExtra("is_fresh_launch", false));
    }

    // ---------- VIEW METHODS ----------

    @Override
    public void showWalks(java.util.List<walkModel> walks) {

        walksAdapter ad = new walksAdapter(this, walks,
                new walksAdapter.OnWalkClickListener() {
                    public void onWalkClick(walkModel w) {}

                    public void onRouteButtonClick(walkModel w) {
                        presenter.walkSelected(w);
                    }
                });

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(ad);
    }

    @Override
    public void showHint(String t, int i) {
        hintTxt.setText(t);
        hintNum.setText("Hint " + i + "/3");
    }

    @Override
    public void showHints(boolean v) {
        hintGroup.setVisibility(v ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showDistance(int m) {
        dist.setText("current distance: " + m + " metres");
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

    @Override protected void onResume() {
        super.onResume();
        presenter.resume();
    }

    @Override protected void onPause() {
        super.onPause();
        presenter.pause();
    }
}
