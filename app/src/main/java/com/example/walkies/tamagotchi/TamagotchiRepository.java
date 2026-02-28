package com.example.walkies.tamagotchi;

import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class TamagotchiRepository {
    private final SharedPreferences prefs;
    private static final String KEY_COINS = "coins";
    private static final String KEY_XP = "xp";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_HUNGER = "hunger";
    private static final String KEY_CLEAN = "clean";
    private static final String KEY_WALKED = "walked";
    private static final String KEY_LAST_SAVE_TIME = "last_save_time";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_OWNED_HATS = "owned_hats";
    private static final String KEY_SELECTED_HAT = "selected_hat";

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

    public void saveXPandLevel(int xp, int level) {
        prefs.edit()
                .putInt(KEY_XP, xp)
                .putInt(KEY_LEVEL, level)
                .apply();

    }

    public void saveCoins(int coins) {
        prefs.edit()
                .putInt(KEY_COINS, coins)
                .apply();
    }

    public int getCoins() {
        return prefs.getInt(KEY_COINS, 0);
    }

    public void saveXP(int xp) {
        prefs.edit()
                .putInt(KEY_XP, xp)
                .apply();
    }

    public void saveLevel(int level) {
        prefs.edit()
                .putInt(KEY_LEVEL, level)
                .apply();
    }

    public int getXP() {
        return prefs.getInt(KEY_XP, 0);
    }

    public int getLevel() {
        return prefs.getInt(KEY_LEVEL, 1);
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

    public void saveOwnedHats(Set<String> ownedHats) {
        prefs.edit().putStringSet(KEY_OWNED_HATS, ownedHats).apply();
    }

    public Set<String> getOwnedHats() {
        return prefs.getStringSet(KEY_OWNED_HATS, new HashSet<>());
    }

    public void saveSelectedHat(int hatId) {
        prefs.edit().putInt(KEY_SELECTED_HAT, hatId).apply();
    }

    public int getSelectedHat() {
        return prefs.getInt(KEY_SELECTED_HAT, 0);
    }
}
