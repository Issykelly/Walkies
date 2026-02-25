package com.example.walkies.mysteryWalks;

import com.example.walkies.walkModel;

public interface MysteryWalksContract {

    interface View {
        void showWalks(java.util.List<walkModel> walks);
        void showHint(String text, int index);
        void showHints(boolean visible);
        void showDistance(int meters);
        void showMessage(String msg);
        void showWalkList(boolean show);

        void openMap(double lat,double lon);
        void openTamagotchi();

        boolean hasLocationPermission();
        void requestLocationPermission();
    }

    interface Presenter {
        void init(boolean freshLaunch);
        void walkSelected(walkModel walk);
        void nextHint();
        void prevHint();
        void giveUp();

        void permissionResult(boolean granted);
        void resume();
        void pause();
    }

    interface Model {
        void loadWalks(Callback<java.util.List<walkModel>> cb);
        void startTracking(LocationCallback cb);
        void stopTracking();
        void getLastLocation(LocationCallback cb);
        void saveCompletion();

        interface Callback<T>{ void call(T data); }
        interface LocationCallback{ void call(android.location.Location loc); }
    }
}
