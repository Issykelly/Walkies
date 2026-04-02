package com.example.walkies.tamagotchi;

import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class TamagotchiRepository {
    private final SharedPreferences prefs;
    public static final String KEY_COINS = "coins";
    public static final String KEY_LIFETIME_COINS = "lifetime_coins";
    public static final String KEY_XP = "xp";
    public static final String KEY_LIFETIME_XP = "lifetime_xp";
    public static final String KEY_MONTHLY_XP = "monthly_xp";
    public static final String KEY_LAST_MONTH = "last_month";
    public static final String KEY_LEVEL = "level";
    public static final String KEY_LIFETIME_CIRCULAR = "lifetime_circular";
    public static final String KEY_LIFETIME_MYSTERY = "lifetime_mystery";
    public static final String KEY_LIFETIME_FED = "lifetime_fed";
    public static final String KEY_LIFETIME_BATHED = "lifetime_bathed";
    public static final String KEY_HUNGER = "hunger";
    public static final String KEY_CLEAN = "clean";
    public static final String KEY_WALKED = "walked";
    public static final String KEY_LAST_SAVE_TIME = "last_save_time";
    public static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_OWNED_HATS = "owned_hats";
    public static final String KEY_SELECTED_HAT = "selected_hat";
    public static final String KEY_CITY = "city";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_GOAL = "goal";
    public static final String KEY_MUTED = "muted";

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

    public void saveXPandLevel(int xp, int level, int lifetimeXP, int monthlyXP, String lastMonth) {
        prefs.edit()
                .putInt(KEY_XP, xp)
                .putInt(KEY_LEVEL, level)
                .putInt(KEY_LIFETIME_XP, lifetimeXP)
                .putInt(KEY_MONTHLY_XP, monthlyXP)
                .putString(KEY_LAST_MONTH, lastMonth)
                .apply();
    }

    public void saveCoins(int coins, int lifetimeCoins) {
        prefs.edit()
                .putInt(KEY_COINS, coins)
                .putInt(KEY_LIFETIME_COINS, lifetimeCoins)
                .apply();
    }

    public void addWalkRewards(int earnedXp, int earnedCoins, boolean isCircular) {
        int currentXp = getXP();
        int currentMonthlyXp = getMonthlyXP();
        int currentLevel = getLevel();
        int currentLifetimeXp = getLifetimeXP();
        int currentCoins = getCoins();
        int currentLifetimeCoins = getLifetimeCoins();

        int newXp = currentXp + earnedXp;
        int newMonthlyXp = currentMonthlyXp + earnedXp;
        int newLifetimeXp = currentLifetimeXp + earnedXp;
        int newCoins = currentCoins + earnedCoins;
        int newLifetimeCoins = currentLifetimeCoins + earnedCoins;

        int newLevel = currentLevel;
        while (newXp >= 100 + (newLevel * 10)) {
            newXp -= (100 + (newLevel * 10));
            newLevel++;
            newCoins += 100;
            newLifetimeCoins += 100;
        }

        SharedPreferences.Editor editor = prefs.edit()
                .putInt(KEY_XP, newXp)
                .putInt(KEY_LEVEL, newLevel)
                .putInt(KEY_LIFETIME_XP, newLifetimeXp)
                .putInt(KEY_MONTHLY_XP, newMonthlyXp)
                .putInt(KEY_COINS, newCoins)
                .putInt(KEY_LIFETIME_COINS, newLifetimeCoins)
                .putInt(KEY_WALKED, 100)
                .putLong(KEY_LAST_SAVE_TIME, System.currentTimeMillis() / 1000);

        if (isCircular) {
            editor.putInt(KEY_LIFETIME_CIRCULAR, getLifetimeCircular() + 1);
        } else {
            editor.putInt(KEY_LIFETIME_MYSTERY, getLifetimeMystery() + 1);
        }
        
        editor.apply();
    }

    public void saveCity(String city) {
        prefs.edit()
                .putString(KEY_CITY, city)
                .apply();
    }

    public String getCity() {
        return prefs.getString(KEY_CITY, "city");
    }

    public void saveUsername(String username) {
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void saveGoal(String goal) {
        prefs.edit()
                .putString(KEY_GOAL, goal)
                .apply();
    }

    public String getGoal() {
        return prefs.getString(KEY_GOAL, "");
    }

    public int getCoins() {
        return prefs.getInt(KEY_COINS, 200);
    }

    public int getLifetimeCoins() {
        return prefs.getInt(KEY_LIFETIME_COINS, 200);
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

    public int getMonthlyXP() {
        return prefs.getInt(KEY_MONTHLY_XP, 0);
    }

    public String getLastMonth() {
        return prefs.getString(KEY_LAST_MONTH, "");
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

    public void saveMuted(boolean muted) {
        prefs.edit().putBoolean(KEY_MUTED, muted).apply();
    }

    public boolean isMuted() {
        return prefs.getBoolean(KEY_MUTED, false);
    }
}
