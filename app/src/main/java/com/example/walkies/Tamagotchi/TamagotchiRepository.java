package com.example.walkies.Tamagotchi;

import android.content.SharedPreferences;

public class TamagotchiRepository {

    private final SharedPreferences prefs;

    private static final String KEY_HUNGER = "hunger";
    private static final String KEY_CLEAN = "clean";
    private static final String KEY_WALKED = "walked";
    private static final String KEY_LAST_SAVE_TIME = "last_save_time";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    public TamagotchiRepository(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void saveStats(int hunger, int clean, int walked) {
        prefs.edit()
                .putInt(KEY_HUNGER, hunger)
                .putInt(KEY_CLEAN, clean)
                .putInt(KEY_WALKED, walked)
                .apply();
    }

    public int getHunger() {
        return prefs.getInt(KEY_HUNGER, 100);
    }

    public int getClean() {
        return prefs.getInt(KEY_CLEAN, 100);
    }

    public int getWalked() {
        return prefs.getInt(KEY_WALKED, 100);
    }

    public void saveTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SAVE_TIME, timestamp).apply();
    }

    public long getLastSavedTime() {
        long currentTime = System.currentTimeMillis() / 1000;
        return prefs.getLong(KEY_LAST_SAVE_TIME, currentTime);
    }

    public boolean IsFirstLaunch(){
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        if (isFirstLaunch) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
            return true;
        } else {
            return false;
        }
    }
}