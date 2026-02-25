package com.example.walkies.tamagotchi;

public interface TamagotchiContract {

    interface View {
        void updateHunger(int value);
        void updateClean(int value);
        void updateWalk(int value);

        void showDogState(int drawableRes);
        void playAnimation(int[] frames, int delay);

        void showFoodMenu();
        void showHatMenu();
        void hideMenus();

        void showWalkOptions();

        void showSponge();

        void tailWagAnimation();
    }


    interface Presenter {
        void attach();
        void detach();

        void onFeed(int value);
        void onClean(int value);
        void loadStats();
        void saveStats();
        void applyTimeDecay(long seconds);
        void onWalkClicked();
    }

    interface Model {
        int getHunger();
        int getClean();
        int getWalked();

        void feed(int value);
        void clean(int value);
        void walk(int value);

        void decay(long seconds);
    }
}

