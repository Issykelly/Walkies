package com.example.walkies.tamagotchi;

import android.content.Context;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.walkies.R;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Source;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TamagotchiPresenter implements TamagotchiContract.Presenter {

    private final TamagotchiContract.View view;
    private final TamagotchiContract.Model model;
    private final TamagotchiUI ui;
    private final TamagotchiRepository repository;
    private final Context context;

    private ZonedDateTime cachedSunrise;
    private ZonedDateTime cachedSunset;
    private String cachedCity;

    public TamagotchiPresenter(TamagotchiContract.View view,
                               TamagotchiContract.Model model,
                               TamagotchiUI ui,
                               TamagotchiRepository repository,
                               Context context){
        this.view = view;
        this.model = model;
        this.ui = ui;
        this.repository = repository;
        this.context = context;
    }

    @Override
    public void attach() {
        loadStats();
        if (isOnline()) {
            new Thread(() -> fetchSunsetTime(model.getCity())).start();
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
                                      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    @Override
    public void loadStats() {
        model.feed(repository.getHunger() - model.getHunger());
        model.clean(repository.getClean() - model.getClean());
        model.walk(repository.getWalked() - model.getWalked());
        model.setCity(repository.getCity());

        model.coins(repository.getCoins());
        model.xp(repository.getXP());
        model.level(repository.getLevel());
        model.setLifetimeXP(repository.getLifetimeXP());

        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        if (!currentMonth.equals(repository.getLastMonth())) {
            model.setMonthlyXP(0);
            model.setLastMonth(currentMonth);
        } else {
            model.setMonthlyXP(repository.getMonthlyXP());
            model.setLastMonth(repository.getLastMonth());
        }

        model.setLifetimeCoins(repository.getLifetimeCoins());
        model.setLifetimeCircular(repository.getLifetimeCircular());
        model.setLifetimeMystery(repository.getLifetimeMystery());
        model.setLifetimeFed(repository.getLifetimeFed());
        model.setLifetimeBathed(repository.getLifetimeBathed());

        model.setOwnedHats(repository.getOwnedHats());
        model.setSelectedHat(repository.getSelectedHat());

        updateUI();

        long secondsPassed = (System.currentTimeMillis() / 1000) - repository.getLastSavedTime();
        applyTimeDecay(secondsPassed);
    }

    @Override
    public void saveStats() {
        repository.saveStats(model.getHunger(), model.getClean(), model.getWalked());
        repository.saveXPandLevel(model.getXP(), model.getLevel(), model.getLifetimeXP(), model.getMonthlyXP(), model.getLastMonth());
        repository.saveCoins(model.getCoins(), model.getLifetimeCoins());
        repository.saveLifetimeCircular(model.getLifetimeCircular());
        repository.saveLifetimeMystery(model.getLifetimeMystery());
        repository.saveLifetimeFed(model.getLifetimeFed());
        repository.saveLifetimeBathed(model.getLifetimeBathed());
        repository.saveTime(System.currentTimeMillis() / 1000);

        repository.saveOwnedHats(model.getOwnedHats());
        repository.saveSelectedHat(model.getSelectedHat());

        updateFirestoreStats();
    }

    private void updateFirestoreStats() {
        String username = repository.getUsername().toLowerCase();
        if (!username.isEmpty()) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> updates = new HashMap<>();
            updates.put("lifetimeXP", model.getLifetimeXP());
            updates.put("monthlyXP", model.getMonthlyXP());
            updates.put("level", model.getLevel());
            updates.put("city", model.getCity());
            
            db.collection("Users").document(username)
                    .update(updates)
                    .addOnFailureListener(e -> Log.e("Firestore", "Error updating stats", e));
        }
    }

    @Override
    public void applyTimeDecay(long seconds) {
        model.decay(seconds);
        updateUI();
    }

    @Override
    public void onFeed(int value){
        model.feed(value);
        if (!repository.isMuted()) {
            view.playEatingSound();
        }
        updateUI();
    }

    @Override
    public void onClean(int value){
        model.clean(value);
        updateUI();
    }

    @Override
    public void onWalkClicked() {
        ZonedDateTime now = ZonedDateTime.now();
        if (cachedCity != null && cachedCity.equals(model.getCity()) && cachedSunrise != null && cachedSunset != null) {
            if (now.toLocalDate().equals(cachedSunrise.toLocalDate())) {
                boolean isNight = now.isAfter(cachedSunset) || now.isBefore(cachedSunrise);
                if (isNight) {
                    ui.showNightWalkWarning(ui::showWalkOptions);
                } else {
                    ui.showWalkOptions();
                }
                return;
            }
        }

        if (!isOnline()) {
            ui.showWalkOptions();
            return;
        }

        new Thread(() -> {
            boolean isNight = fetchSunsetTime(model.getCity());
            Log.d("isNight", String.valueOf(isNight));
            
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isNight) {
                    ui.showNightWalkWarning(ui::showWalkOptions);
                } else {
                    ui.showWalkOptions();
                }
            });
        }).start();
    }

    private boolean fetchSunsetTime(String city) {
        if (city == null || city.isEmpty()) return false;

        ZonedDateTime now = ZonedDateTime.now();
        if (city.equals(cachedCity) && cachedSunrise != null && cachedSunset != null) {
            if (now.toLocalDate().equals(cachedSunrise.toLocalDate())) {
                return now.isAfter(cachedSunset) || now.isBefore(cachedSunrise);
            }
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            DocumentSnapshot doc = Tasks.await(
                    db.collection("Cities").document(city).get(Source.DEFAULT)
            );

            if (!doc.exists()) {
                Log.e("SunCheck", "City document not found: " + city);
                return false;
            }

            GeoPoint geo = doc.getGeoPoint("Location");
            if (geo == null) {
                Log.e("SunCheck", "GeoPoint missing for city: " + city);
                return false;
            }

            double lat = geo.getLatitude();
            double lng = geo.getLongitude();
            Log.d("SunCheck", "City coordinates: lat=" + lat + ", lng=" + lng);

            JSONObject results = getJsonObject(lat, lng);

            String sunriseStr = results.getString("sunrise");
            String sunsetStr = results.getString("sunset");

            ZonedDateTime sunriseUtc = ZonedDateTime.parse(sunriseStr);
            ZonedDateTime sunsetUtc = ZonedDateTime.parse(sunsetStr);

            ZonedDateTime sunriseLocal = sunriseUtc.withZoneSameInstant(now.getZone());
            ZonedDateTime sunsetLocal = sunsetUtc.withZoneSameInstant(now.getZone());

            cachedSunrise = sunriseLocal;
            cachedSunset = sunsetLocal;
            cachedCity = city;

            Log.d("SunCheck", "Now: " + now);
            Log.d("SunCheck", "Sunrise: " + sunriseLocal);
            Log.d("SunCheck", "Sunset: " + sunsetLocal);

            boolean isNight = now.isAfter(sunsetLocal) || now.isBefore(sunriseLocal);
            Log.d("SunCheck", "Is night: " + isNight);
            return isNight;

        } catch (Exception e) {
            Log.e("SunCheck", "Error fetching sunset/sunrise", e);
            return false;
        }
    }

    @NonNull
    private static JSONObject getJsonObject(double lat, double lng) throws IOException, JSONException {
        URL url = new URL(
                "https://api.sunrise-sunset.org/json?lat=" + lat + "&lng=" + lng + "&formatted=0"
        );

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        JSONObject json = new JSONObject(sb.toString());
        return json.getJSONObject("results");
    }

    @Override
    public void onLevelClicked() {
        ui.showCurrentLevelDialog(model.getLevel(), model.getXP());
    }

    @Override
    public void onSettingsClicked(){
        ui.showSettingsDialog();
    }

    @Override
    public void onLifetimeStatsRequested() {
        ui.showLifetimeStatsDialog(
                model.getLifetimeXP(),
                model.getLifetimeCoins(),
                model.getLifetimeCircular(),
                model.getLifetimeMystery(),
                model.getLifetimeFed(),
                model.getLifetimeBathed()
        );
    }

    @Override
    public void onLeaderboardRequested() {
        ui.showLeaderboardDialog();
    }

    @Override
    public void onSettingsDetailsRequested() {
        ui.showSettingsDetailsDialog(
                repository.getUsername(),
                repository.getCity(),
                repository.isMuted()
        );
    }

    @Override
    public void onSettingsSaved(String newCity, boolean muted) {
        model.setCity(newCity);
        repository.saveCity(newCity);
        repository.saveMuted(muted);
        saveStats();
    }

    private void updateUI(){
        view.updateHunger(model.getHunger());
        view.updateClean(model.getClean());
        view.updateWalk(model.getWalked());

        boolean leveledUp = model.checkXPLevels();
        view.updateUI();
        if (leveledUp) {
            ui.showLevelUpDialog(model.getLevel());
        }
        int happiness = model.getHunger() + model.getClean() + model.getWalked();
        Log.d("Happiness", String.valueOf(happiness));
        if (happiness >= 250) {
            view.showDogState(R.drawable.husky_estatic);
            playTailWagAnimation();
        } else if (happiness >= 200) {
            view.showDogState(R.drawable.husky_happy);
            playTailWagAnimation();
        } else {
            view.showDogState(R.drawable.husky_idle);
            playTailWagAnimation();
        }
    }

    public void playTailWagAnimation() {
        view.tailWagAnimation();
    }

    public void onFeedClicked() {
        view.showFoodMenu();
    }

    public void onHatClicked() {
        view.showHatMenu();
    }

    public void onCleanClicked() {
        view.showSponge();
    }

    public boolean isSpongeCleaning(Rect spongeRect, Rect dogRect) {
        int top = dogRect.top + (int)(dogRect.height() * 0.45);
        int bottom = dogRect.top + (int)(dogRect.height() * 0.70);
        int left = dogRect.left + (int)(dogRect.width() * 0.15);
        int right = dogRect.right - (int)(dogRect.width() * 0.35);

        Rect cleanableArea = new Rect(left, top, right, bottom);

        return Rect.intersects(spongeRect, cleanableArea);
    }
}
