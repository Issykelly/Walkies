package com.example.walkies.circularWalks;

import android.content.Context;
import android.location.Location;

import com.example.walkies.walkModel;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface CircularWalksContract {

    interface View {
        void showWalks(List<walkModel> walks);
        void showRoute(List<LatLng> points);
        void moveCamera(LatLng latLng, float zoom);
        void showMessage(String msg);
        void toggleWalkList(boolean show);
        Context getContext();
        void showMarkers(List<walkModel> walks);
        void showForcedWalkMarker(LatLng pendingForcedWalkDest);

        void showHint();
        void showLocationError();
    }

    interface Presenter {
        void onMapReady();
        void onLocationReceived(Location location);
        void onWalkSelected(walkModel walk);
        void onRouteRequested(walkModel walk);
        void onResume();
        void onPause();
        void setForcedWalk(double lat, double lon);
        void onDestroy();
    }

    interface Model {
        void fetchWalks(double lat, double lon, WalksCallback callback);
        void fetchRoute(LatLng origin, LatLng dest, RouteCallback callback);
        void shutdown();

        interface WalksCallback {
            void onLoaded(List<walkModel> walks);
        }

        interface RouteCallback {
            void onLoaded(List<LatLng> points);
        }
    }
}
