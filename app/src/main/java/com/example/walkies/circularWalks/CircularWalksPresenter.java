package com.example.walkies.circularWalks;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.walkies.tamagotchi.Tamagotchi;
import com.example.walkies.walkModel;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class CircularWalksPresenter implements CircularWalksContract.Presenter {

    private final CircularWalksContract.View view;
    protected CircularWalksContract.Model model;

    // Walk state
    // ---------------------------------------------------------
    private walkModel activeWalk;
    private LatLng startPoint;
    private Location lastLocation;
    private Location previousLocation;

    private final List<LatLng> fullRoutePoints = new ArrayList<>();

    private boolean isReturning = false;
    private boolean isForcedWalk = false;
    private LatLng pendingForcedWalkDest;

    private boolean initialZoom = false;
    private boolean mapReady = false;
    private boolean walksFetched = false;

    private LatLng pendingCameraTarget = null;
    private float pendingCameraZoom = 12f;

    // movement thresholds
    // ---------------------------------------------------------
    private static final float PROGRESS_THRESHOLD = 15f;
    private static final float ARRIVAL_THRESHOLD = 40f;
    private static final float OFF_ROUTE_THRESHOLD = 25f;

    // route recalculation cooldown
    // ---------------------------------------------------------
    private static final long RECALC_COOLDOWN_MS = 15000;
    private long lastRecalcTime = 0;
    private boolean isRecalculating = false;

    // loop
    // ---------------------------------------------------------
    private final Handler routeHandler = new Handler(Looper.getMainLooper());
    private boolean loopRunning = false;

    public CircularWalksPresenter(CircularWalksContract.View v) {
        this(v, new CircularWalksModel(v.getContext()));
    }

    public CircularWalksPresenter(CircularWalksContract.View v, CircularWalksContract.Model m) {
        view = v;
        model = m;
    }

    // Map Ready
    // ---------------------------------------------------------
    @Override
    public void onMapReady() {
        mapReady = true;

        if (isForcedWalk && activeWalk != null) {
            renderPendingMarkerIfNeeded();
            pendingCameraTarget = new LatLng(activeWalk.getWalkLatitude(), activeWalk.getWalkLongitude());
            pendingCameraZoom = 15f;
        } else if (lastLocation != null) {
            pendingCameraTarget = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        }

        if (pendingCameraTarget != null) {
            view.moveCamera(pendingCameraTarget, pendingCameraZoom);
            initialZoom = true;
            pendingCameraTarget = null;
        }
    }

    private void renderPendingMarkerIfNeeded() {
        if (mapReady && activeWalk != null) {
            view.showForcedWalkMarker(new LatLng(activeWalk.getWalkLatitude(), activeWalk.getWalkLongitude()));
        }
    }

    // Location Updates
    // ---------------------------------------------------------
    @Override
    public void onLocationReceived(Location loc) {
        if (loc == null) return;

        lastLocation = loc;
        Log.d("CircularWalksPresenter", "Location received: " + loc.getLatitude() + ", " + loc.getLongitude());

        if (!initialZoom && mapReady) {
            view.moveCamera(new LatLng(loc.getLatitude(), loc.getLongitude()), 12f);
            initialZoom = true;
        } else if (!initialZoom) {
            pendingCameraTarget = new LatLng(loc.getLatitude(), loc.getLongitude());
        }

        // Start forced walk if pending
        // ---------------------------------------------------------
        if (pendingForcedWalkDest != null) beginForcedWalk(pendingForcedWalkDest);

        // Fetch walks once
        // ---------------------------------------------------------
        if (activeWalk == null && !walksFetched) {
            walksFetched = true;
            Log.d("CircularWalksPresenter", "Fetching walks for location...");
            model.fetchWalks(loc.getLatitude(), loc.getLongitude(), walks -> {
                if (walks == null || walks.isEmpty()) walksFetched = false;
                view.showWalks(walks);
                if (!isForcedWalk) view.showMarkers(walks);
            });
        }
    }

    // Walk Selection
    // ---------------------------------------------------------
    @Override
    public void onWalkSelected(walkModel walk) {
        view.moveCamera(new LatLng(walk.getWalkLatitude(), walk.getWalkLongitude()), 15f);
    }

    @Override
    public void onRouteRequested(walkModel walk) {
        if (lastLocation == null) {
            view.showMessage("Waiting for GPS...");
            return;
        }

        activeWalk = walk;
        isReturning = false;
        startPoint = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

        view.toggleWalkList(false);

        LatLng dest = new LatLng(walk.getWalkLatitude(), walk.getWalkLongitude());

        model.fetchRoute(startPoint, dest, points -> {
            fullRoutePoints.clear();
            fullRoutePoints.addAll(points);
            view.showRoute(fullRoutePoints);
            startRouteLoop();
        });

        view.showHint();
    }

    // Main Route Loop
    // ---------------------------------------------------------
    private final Runnable routeLoop = new Runnable() {
        @Override
        public void run() {
            if (!loopRunning || lastLocation == null || activeWalk == null) {
                scheduleNext();
                return;
            }

            preTrimRoute();
            updateLiveRoute(lastLocation);
            checkProximity(lastLocation);

            previousLocation = new Location(lastLocation);
            scheduleNext();
        }
    };

    private void startRouteLoop() {
        if (loopRunning) return;
        loopRunning = true;
        routeHandler.post(routeLoop);
    }

    private void stopRouteLoop() {
        loopRunning = false;
        routeHandler.removeCallbacks(routeLoop);
    }

    private void scheduleNext() {
        routeHandler.postDelayed(routeLoop, 1500);
    }

    // Routing Logic
    // ---------------------------------------------------------
    private void updateLiveRoute(Location userLocation) {
        if (fullRoutePoints.isEmpty()) return;

        LatLng user = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
        LatLng snapped = snapToRoute(user, fullRoutePoints);

        if (distanceBetween(user, snapped) > OFF_ROUTE_THRESHOLD) {
            recalculateRoute(user);
            return;
        }

        view.showRoute(fullRoutePoints);
    }

    private LatLng snapToRoute(LatLng user, List<LatLng> route) {
        if (route.isEmpty()) return user;
        LatLng closest = null;
        double min = Double.MAX_VALUE;

        int step = Math.max(1, route.size() / 50);
        for (int i = 0; i < route.size() - 1; i += step) {
            LatLng a = route.get(i);
            LatLng b = route.get(Math.min(i + 1, route.size() - 1));
            LatLng p = closestPointOnSegment(user, a, b);
            double d = distanceBetween(user, p);
            if (d < min) {
                min = d;
                closest = p;
            }
        }

        return closest != null ? closest : route.get(0);
    }

    private LatLng closestPointOnSegment(LatLng p, LatLng a, LatLng b) {
        double dx = b.longitude - a.longitude;
        double dy = b.latitude - a.latitude;
        if (dx == 0 && dy == 0) return a;

        double t = ((p.longitude - a.longitude) * dx + (p.latitude - a.latitude) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        return new LatLng(a.latitude + t * dy, a.longitude + t * dx);
    }

    private void preTrimRoute() {
        if (fullRoutePoints.size() < 2 || lastLocation == null) return;

        int trimCount = 0;
        LatLng user = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        int maxTrim = Math.min(fullRoutePoints.size() - 1, 5);

        for (int i = 1; i <= maxTrim; i++) {
            if (distanceBetween(user, fullRoutePoints.get(i)) < PROGRESS_THRESHOLD) {
                trimCount = i;
            } else break;
        }

        if (trimCount > 0) {
            fullRoutePoints.subList(0, trimCount).clear();
            view.showRoute(fullRoutePoints);
        }
    }

    private void recalculateRoute(LatLng user) {
        if (isRecalculating) return;
        long now = System.currentTimeMillis();
        if (now - lastRecalcTime < RECALC_COOLDOWN_MS) return;

        isRecalculating = true;
        lastRecalcTime = now;

        LatLng target = isReturning ? startPoint :
                new LatLng(activeWalk.getWalkLatitude(), activeWalk.getWalkLongitude());

        model.fetchRoute(user, target, pts -> {
            fullRoutePoints.clear();
            fullRoutePoints.addAll(pts);
            view.showRoute(fullRoutePoints);
            view.showMessage("Off-route — recalculating...");
            isRecalculating = false;
        });
    }

    private double distanceBetween(LatLng a, LatLng b) {
        float[] r = new float[1];
        android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r);
        return r[0];
    }

    // Proximity
    // ---------------------------------------------------------
    private void checkProximity(Location loc) {
        if (activeWalk == null) return;

        LatLng target = isReturning ? startPoint :
                new LatLng(activeWalk.getWalkLatitude(), activeWalk.getWalkLongitude());

        float[] distance = new float[1];
        android.location.Location.distanceBetween(
                loc.getLatitude(), loc.getLongitude(),
                target.latitude, target.longitude,
                distance
        );

        Log.d("CircularWalksPresenter", "Distance to target: " + distance[0] + " meters");

        if (distance[0] < ARRIVAL_THRESHOLD) {
            Log.d("CircularWalksPresenter", "Reached target!");

            if (isForcedWalk) {
                completeForcedWalk();
            } else if (!isReturning) {
                isReturning = true;
                view.showMessage("Reached pin — returning.");

                LatLng userLoc = new LatLng(loc.getLatitude(), loc.getLongitude());
                LatLng returnTarget = startPoint;

                model.fetchRoute(userLoc, returnTarget, pts -> {
                    fullRoutePoints.clear();
                    fullRoutePoints.addAll(pts);
                    view.showRoute(fullRoutePoints);
                });
            } else {
                completeRegularWalk();
            }
        }
    }

    // Walk Completion
    // ---------------------------------------------------------
    public void completeRegularWalk() { finishWalk("Walk completed!"); }
    public void completeForcedWalk() { finishWalk("Forced walk completed!"); }

    private void finishWalk(String msg) {
        view.showMessage(msg);

        fullRoutePoints.clear();
        isReturning = false;
        isForcedWalk = false;

        stopRouteLoop();
        view.showRoute(fullRoutePoints);

        int earnedXp = 0;
        if (activeWalk != null) {
            double maxDistanceMeters = 1609.34;
            double distanceRatio = Math.min(activeWalk.getWalkDistance() / maxDistanceMeters, 1.0);
            earnedXp = (int) Math.round(distanceRatio * 200);
        }

        activeWalk = null;

        Context ctx = view.getContext();
        var p = ctx.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE);
        int currentXp = p.getInt("xp", 0);
        int currentCoins = p.getInt("coins", 0);

        p.edit()
                .putInt("walked", 100)
                .putLong("last_save_time", System.currentTimeMillis() / 1000)
                .putInt("coins", currentCoins + 75)
                .putInt("lifetime_coins", p.getInt("lifetime_coins", 0) + 75)
                .putInt("xp", currentXp + earnedXp)
                .putInt("lifetime_xp", p.getInt("lifetime_xp", 0) + earnedXp)
                .putInt("lifetime_circular", p.getInt("lifetime_circular", 0) + 1)
                .apply();

        Intent i = new Intent(ctx, Tamagotchi.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(i);

        view.toggleWalkList(true);
    }

    // Forced Walk
    // ---------------------------------------------------------
    public void startForcedWalk(LatLng dest) {
        isForcedWalk = true;
        pendingForcedWalkDest = dest;

        if (lastLocation != null) {
            renderPendingMarkerIfNeeded();
            beginForcedWalk(dest);
        } else view.showMessage("Waiting for GPS...");
    }

    private void beginForcedWalk(LatLng dest) {
        activeWalk = new walkModel("Forced Walk", 0, dest.longitude, dest.latitude, null);
        startPoint = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        isReturning = false;

        view.toggleWalkList(false);

        model.fetchRoute(startPoint, dest, pts -> {
            fullRoutePoints.clear();
            fullRoutePoints.addAll(pts);
            view.showRoute(fullRoutePoints);
            startRouteLoop();
        });

        pendingForcedWalkDest = null;
    }

    // Lifecycle
    // ---------------------------------------------------------
    @Override
    public void onResume() { startRouteLoop(); }

    @Override
    public void onPause() { stopRouteLoop(); }
}
