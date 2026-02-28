package com.example.walkies.tamagotchi;

import java.util.Set;

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

        void updateUI();
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

        void coins(int value);

        void spendCoins(int value);

        void gainCoins(int value);

        void xp(int value);

        void level(int value);

        void gainXP(int value);

        void levelUP();

        void decay(long seconds);

        int getCoins();
        int getXP();
        int getLevel();

        void checkXPLevels();

        Set<String> getOwnedHats();
        void setOwnedHats(Set<String> ownedHats);
        void addOwnedHat(String hatName);
        boolean isHatOwned(String hatName);

        int getSelectedHat();
        void setSelectedHat(int hatId);
    }
}
