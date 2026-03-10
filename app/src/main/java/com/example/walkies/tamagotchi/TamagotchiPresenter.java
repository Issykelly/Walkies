package com.example.walkies.tamagotchi;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.walkies.R;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;

public class TamagotchiPresenter implements TamagotchiContract.Presenter {

    private final TamagotchiContract.View view;
    private final TamagotchiContract.Model model;
    private final TamagotchiUI ui;
    private final TamagotchiRepository repository;

    public TamagotchiPresenter(TamagotchiContract.View view,
                               TamagotchiContract.Model model,
                               TamagotchiUI ui,
                               TamagotchiRepository repository){
        this.view = view;
        this.model = model;
        this.ui = ui;
        this.repository = repository;
    }

    @Override
    public void attach() {
        loadStats();
    }

    @Override
    public void detach() { }

    @Override
    public void loadStats() {
        model.feed(repository.getHunger() - model.getHunger());
        model.clean(repository.getClean() - model.getClean());
        model.walk(repository.getWalked() - model.getWalked());

        model.coins(repository.getCoins());
        model.xp(repository.getXP());
        model.level(repository.getLevel());
        model.setLifetimeXP(repository.getLifetimeXP());
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
        repository.saveXPandLevel(model.getXP(), model.getLevel(), model.getLifetimeXP());
        repository.saveCoins(model.getCoins(), model.getLifetimeCoins());
        repository.saveLifetimeCircular(model.getLifetimeCircular());
        repository.saveLifetimeMystery(model.getLifetimeMystery());
        repository.saveLifetimeFed(model.getLifetimeFed());
        repository.saveLifetimeBathed(model.getLifetimeBathed());
        repository.saveTime(System.currentTimeMillis() / 1000);

        repository.saveOwnedHats(model.getOwnedHats());
        repository.saveSelectedHat(model.getSelectedHat());
    }

    @Override
    public void applyTimeDecay(long seconds) {
        model.decay(seconds);
        updateUI();
    }

    @Override
    public void onFeed(int value){
        model.feed(value);
        updateUI();
    }

    @Override
    public void onClean(int value){
        model.clean(value);
        updateUI();
    }

    @Override
    public void onWalkClicked() {
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
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            DocumentSnapshot doc = Tasks.await(
                    db.collection("Cities")
                            .document(city)
                            .get()
            );

            if (doc.exists()) {

                GeoPoint geo = doc.getGeoPoint("Location");
                Log.d("SunCheck", "Geopoint: " + geo);


                if (geo != null) {

                    double lat = geo.getLatitude();
                    double lng = geo.getLongitude();

                    URL url = new URL(
                            "https://api.sunrise-sunset.org/json?lat=" + lat + "&lng=" + lng + "&formatted=0"
                    );

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject results = json.getJSONObject("results");

                    String sunsetStr = results.getString("sunset");
                    String sunriseStr = results.getString("sunrise");

                    ZonedDateTime now = ZonedDateTime.now();

                    ZonedDateTime sunsetTime = ZonedDateTime.parse(sunsetStr)
                            .withZoneSameInstant(now.getZone());

                    ZonedDateTime sunriseTime = ZonedDateTime.parse(sunriseStr)
                            .withZoneSameInstant(now.getZone());

                    return now.isAfter(sunsetTime) || now.isBefore(sunriseTime);
                }
            }

        } catch (Exception e) {
            Log.e("SunsetCheck", "Error fetching sunset", e);
        }
        return false;
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

    public boolean isFoodFed(Rect foodRect, Rect dogRect) {
        Rect headRect = new Rect(
                dogRect.left + (int)(dogRect.width()*0.25),
                dogRect.top + (int)(dogRect.height()*0.45),
                dogRect.right - (int)(dogRect.width()*0.25),
                dogRect.top + (int)(dogRect.height()*0.55)
        );
        return Rect.intersects(foodRect, headRect);
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
