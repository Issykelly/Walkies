package com.example.walkies.mysteryWalks;

import android.location.Location;
import com.example.walkies.walkModel;
import java.util.ArrayList;
import java.util.List;

public class MysteryWalksPresenter implements MysteryWalksContract.Presenter {

    private static final float THRESHOLD = 30f;

    private final MysteryWalksContract.View view;
    private final MysteryWalksContract.Model model;

    private walkModel active;
    private String[] hints;
    private int hintIndex = 0;
    private boolean reached = false;

    private enum TrackingMode { NONE, LIST, WALK }
    private TrackingMode trackingMode = TrackingMode.NONE;

    private List<walkModel> walkList = new ArrayList<>();

    public MysteryWalksPresenter(MysteryWalksContract.View view, MysteryWalksContract.Model model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void init(boolean fresh) {
        trackingMode = TrackingMode.NONE;
        model.stopTracking();
        model.reset();
        resetActiveWalk();
        walkList.clear();
        
        view.showWalks(new ArrayList<>()); 
        view.showHints(false);
        view.showWalkList(true);
        view.showDistance(-1);

        model.loadWalks(walks -> {
            List<walkModel> loaded = new ArrayList<>();
            if (walks != null) {
                for (walkModel w : walks) {
                    loaded.add(new walkModel(
                            w.getWalkName(),
                            0,
                            w.getWalkLongitude(),
                            w.getWalkLatitude(),
                            w.getHints()
                    ));
                }
            }
            walkList = loaded;
            view.showWalks(new ArrayList<>(walkList));

            if (view.hasLocationPermission()) {
                startLocationUpdates();
            } else {
                view.requestLocationPermission();
            }
        });
    }

    private void startLocationUpdates() {
        model.getLastLocation(location -> {
            if (location != null && active == null) {
                updateWalkDistances(location);
            }
        });

        if (active == null) {
            if (trackingMode != TrackingMode.LIST) {
                model.stopTracking();
                model.startTracking(loc -> {
                    if (loc != null && active == null) {
                        updateWalkDistances(loc);
                    }
                });
                trackingMode = TrackingMode.LIST;
            }
        } else {
            if (trackingMode != TrackingMode.WALK) {
                model.stopTracking();
                model.startTracking(this::location);
                trackingMode = TrackingMode.WALK;
            }
        }
    }

    private void updateWalkDistances(Location location) {
        if (location == null) return;
        
        List<walkModel> updatedList = new ArrayList<>();
        for (walkModel w : walkList) {
            walkModel fresh = new walkModel(
                    w.getWalkName(),
                    w.getWalkDistance(),
                    w.getWalkLongitude(),
                    w.getWalkLatitude(),
                    w.getHints()
            );
            fresh.updateDistance(location);
            updatedList.add(fresh);
        }
        walkList = updatedList;
        view.showWalks(new ArrayList<>(walkList));
    }

    @Override
    public void walkSelected(walkModel w) {
        model.stopTracking();
        trackingMode = TrackingMode.NONE;

        resetActiveWalk();

        active = new walkModel(
                w.getWalkName(),
                0,
                w.getWalkLongitude(),
                w.getWalkLatitude(),
                w.getHints()
        );

        hints = active.getHints();
        hintIndex = 0;
        reached = false;

        model.setInitialDistance(0);
        model.setMaxHint(0);

        view.showWalkList(false);

        if (hints != null && hints[0] != null) {
            view.showHints(true);
            view.showHint(hints[0], 1);
            model.setMaxHint(1);
        }

        view.showDistance(-1);

        // Update active walk distance immediately
        model.getLastLocation(loc -> {
            if (loc != null && active != null) {
                int d = dist(loc);
                view.showDistance(d);
                model.setInitialDistance(d);
            }
        });

        startLocationUpdates();
    }

    private void resetActiveWalk() {
        if (active != null) active.reset();
        active = null;
        hints = null;
        hintIndex = 0;
        reached = false;
    }

    private void location(Location l) {
        if (l == null || active == null || reached) return;

        int d = dist(l);
        view.showDistance(d);

        if (d < THRESHOLD) {
            reached = true;
            model.stopTracking();
            model.saveCompletion();
            resetActiveWalk();
            model.reset();
            view.closeActivity();
        }
    }

    private int dist(Location l) {
        if (l == null || active == null) return -1;
        float[] r = new float[1];
        android.location.Location.distanceBetween(
                l.getLatitude(), l.getLongitude(),
                active.getWalkLatitude(), active.getWalkLongitude(),
                r);
        return Math.round(r[0]);
    }

    @Override
    public void nextHint() {
        if (hints == null || hintIndex >= hints.length - 1) return;
        if (hints[hintIndex + 1] != null) {
            hintIndex++;
            view.showHint(hints[hintIndex], hintIndex + 1);
            model.setMaxHint(hintIndex + 1);
        }
    }

    @Override
    public void prevHint() {
        if (hints == null || hintIndex <= 0) return;
        hintIndex--;
        view.showHint(hints[hintIndex], hintIndex + 1);
    }

    @Override
    public void giveUp() {
        if (active != null) {
            view.openMap(active.getWalkLatitude(), active.getWalkLongitude());
            resetActiveWalk();
            model.reset();
        }
    }

    @Override
    public void permissionResult(boolean granted) {
        if (granted) {
            startLocationUpdates();
        } else {
            view.showMessage("Location permission required for Walks");
        }
    }

    @Override
    public void resume() {
        if (view.hasLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    public void pause() {
        model.stopTracking();
        trackingMode = TrackingMode.NONE;
    }
}
