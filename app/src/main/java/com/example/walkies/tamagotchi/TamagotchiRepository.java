package com.example.walkies.tamagotchi;

import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class TamagotchiRepository {
    private final SharedPreferences prefs;
    private static final String KEY_COINS = "coins";
    private static final String KEY_LIFETIME_COINS = "lifetime_coins";
    private static final String KEY_XP = "xp";
    private static final String KEY_LIFETIME_XP = "lifetime_xp";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_LIFETIME_CIRCULAR = "lifetime_circular";
    private static final String KEY_LIFETIME_MYSTERY = "lifetime_mystery";
    private static final String KEY_LIFETIME_FED = "lifetime_fed";
    private static final String KEY_LIFETIME_BATHED = "lifetime_bathed";
    private static final String KEY_HUNGER = "hunger";
    private static final String KEY_CLEAN = "clean";
    private static final String KEY_WALKED = "walked";
    private static final String KEY_LAST_SAVE_TIME = "last_save_time";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_OWNED_HATS = "owned_hats";
    private static final String KEY_SELECTED_HAT = "selected_hat";
    private static final String KEY_CITY = "city";

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

    public void saveXPandLevel(int xp, int level, int lifetimeXP) {
        prefs.edit()
                .putInt(KEY_XP, xp)
                .putInt(KEY_LEVEL, level)
                .putInt(KEY_LIFETIME_XP, lifetimeXP)
                .apply();
    }

    public void saveCoins(int coins, int lifetimeCoins) {
        prefs.edit()
                .putInt(KEY_COINS, coins)
                .putInt(KEY_LIFETIME_COINS, lifetimeCoins)
                .apply();
    }

    public void saveCity(String city) {
        prefs.edit()
                .putString(KEY_CITY, city)
                .apply();
    }

    public String getCity() {
        return prefs.getString(KEY_CITY, "city");
    }

    public int getCoins() {
        return prefs.getInt(KEY_COINS, 0);
    }

    public int getLifetimeCoins() {
        return prefs.getInt(KEY_LIFETIME_COINS, 0);
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

    public void saveLifetimeXP(int lifetimeXP) {
        prefs.edit()
                .putInt(KEY_LIFETIME_XP, lifetimeXP)
                .apply();
    }

    public int getXP() {
        return prefs.getInt(KEY_XP, 0);
    }

    public int getLevel() {
        return prefs.getInt(KEY_LEVEL, 1);
    }

    public int getLifetimeXP() {
        return prefs.getInt(KEY_LIFETIME_XP, 0);
    }

    public void saveLifetimeCircular(int circular) {
        prefs.edit()
                .putInt(KEY_LIFETIME_CIRCULAR, circular)
                .apply();
    }

    public void saveLifetimeMystery(int mystery) {
        prefs.edit()
                .putInt(KEY_LIFETIME_MYSTERY, mystery)
                .apply();
    }

    public int getLifetimeCircular() {
        return prefs.getInt(KEY_LIFETIME_CIRCULAR, 0);
    }

    public int getLifetimeMystery() {
        return prefs.getInt(KEY_LIFETIME_MYSTERY, 0);
    }

    public void saveLifetimeFed(int fed) {
        prefs.edit()
                .putInt(KEY_LIFETIME_FED, fed)
                .apply();
    }

    public void saveLifetimeBathed(int bathed) {
        prefs.edit()
                .putInt(KEY_LIFETIME_BATHED, bathed)
                .apply();
    }

    public int getLifetimeFed() {
        return prefs.getInt(KEY_LIFETIME_FED, 0);
    }

    public int getLifetimeBathed() {
        return prefs.getInt(KEY_LIFETIME_BATHED, 0);
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
