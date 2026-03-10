package com.example.walkies.tamagotchi;

import android.content.Context;
import java.util.Set;

public interface TamagotchiContract {

    interface View {

        // STATE UPDATES
        void updateHunger(int value);
        void updateClean(int value);
        void updateWalk(int value);
        void updateUI();

        void showDogState(int drawableRes);
        void playAnimation(int[] frames, int delay);

        // SCREEN UI (not events)
        void showFoodMenu();
        void showHatMenu();
        void showSponge();

        void hideMenus();

        void showLevelUpPopup(int newLevel);

        void tailWagAnimation();

        Context getContext();
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

        void onLevelClicked();

        void onSettingsClicked();

        void onLifetimeStatsRequested();
    }

    interface Model {
        int getHunger();
        int getClean();
        int getWalked();

        String getCity();

        void feed(int value);
        void clean(int value);
        void walk(int value);

        void coins(int value);

        void spendCoins(int value);

        void gainCoins(int value);

        void xp(int value);

        void level(int value);

        void gainXP(int value);

        void levelUP(int xp);

        void setCity(String city);

        void decay(long seconds);

        int getCoins();
        int getXP();
        int getLevel();

        int getLifetimeXP();
        void setLifetimeXP(int value);

        int getLifetimeCoins();
        void setLifetimeCoins(int value);

        int getLifetimeCircular();
        void setLifetimeCircular(int value);

        int getLifetimeMystery();
        void setLifetimeMystery(int value);

        int getLifetimeFed();
        void setLifetimeFed(int value);

        int getLifetimeBathed();
        void setLifetimeBathed(int value);

        boolean checkXPLevels();

        Set<String> getOwnedHats();
        void setOwnedHats(Set<String> ownedHats);
        void addOwnedHat(String hatName);
        boolean isHatOwned(String hatName);

        int getSelectedHat();
        void setSelectedHat(int hatId);
    }
}
