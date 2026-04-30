package com.example.walkies.tamagotchi;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

public class TamagotchiRepository {
    private SharedPreferences prefs;
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

    public TamagotchiRepository(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            this.prefs = EncryptedSharedPreferences.create(
                    "WalkiesSecurePrefs",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            this.prefs = context.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE);
        }
    }

    public void clear() {
        prefs.edit().clear().commit();
    }

    public void saveStats(int hunger, int clean, int walked) {
        prefs.edit()
                .putInt(KEY_HUNGER, hunger)
                .putInt(KEY_CLEAN, clean)
                .putInt(KEY_WALKED, walked)
                .commit();
    }

    public void saveXPandLevel(int xp, int level, int lifetimeXP, int monthlyXP, String lastMonth) {
        prefs.edit()
                .putInt(KEY_XP, xp)
                .putInt(KEY_LEVEL, level)
                .putInt(KEY_LIFETIME_XP, lifetimeXP)
                .putInt(KEY_MONTHLY_XP, monthlyXP)
                .putString(KEY_LAST_MONTH, lastMonth)
                .commit();
    }

    public void saveCoins(int coins, int lifetimeCoins) {
        prefs.edit()
                .putInt(KEY_COINS, coins)
                .putInt(KEY_LIFETIME_COINS, lifetimeCoins)
                .commit();
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
        
        editor.commit();
    }

    public void saveCity(String city) {
        prefs.edit()
                .putString(KEY_CITY, city)
                .commit();
    }

    public String getCity() {
        return prefs.getString(KEY_CITY, "city");
    }

    public void saveUsername(String username) {
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .commit();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void saveGoal(String goal) {
        prefs.edit()
                .putString(KEY_GOAL, goal)
                .commit();
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
                .commit();
    }

    public void saveLifetimeMystery(int mystery) {
        prefs.edit()
                .putInt(KEY_LIFETIME_MYSTERY, mystery)
                .commit();
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
                .commit();
    }

    public void saveLifetimeBathed(int bathed) {
        prefs.edit()
                .putInt(KEY_LIFETIME_BATHED, bathed)
                .commit();
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
        prefs.edit().putLong(KEY_LAST_SAVE_TIME, timestamp).commit();
    }

    public long getLastSavedTime() {
        return prefs.getLong(KEY_LAST_SAVE_TIME, System.currentTimeMillis() / 1000);
    }

    public boolean IsFirstLaunch(){
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        if (isFirstLaunch) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).commit();
            return true;
        } else {
            return false;
        }
    }

    public void saveOwnedHats(Set<String> ownedHats) {
        prefs.edit().putStringSet(KEY_OWNED_HATS, ownedHats).commit();
    }

    public Set<String> getOwnedHats() {
        return prefs.getStringSet(KEY_OWNED_HATS, new HashSet<>());
    }

    public void saveSelectedHat(int hatId) {
        prefs.edit().putInt(KEY_SELECTED_HAT, hatId).commit();
    }

    public int getSelectedHat() {
        return prefs.getInt(KEY_SELECTED_HAT, 0);
    }

    public void saveMuted(boolean muted) {
        prefs.edit().putBoolean(KEY_MUTED, muted).commit();
    }

    public boolean isMuted() {
        return prefs.getBoolean(KEY_MUTED, false);
    }
}
