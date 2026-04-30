package com.example.walkies.tamagotchi;

import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.walkies.R;

import java.util.HashMap;
import java.util.Map;

public class Tamagotchi extends AppCompatActivity
        implements TamagotchiContract.View {

    private static final String CHANNEL_ID = "hungry_dog_channel";
    private static final String TAG = "TamagotchiActivity";

    private TamagotchiPresenter presenter;
    private TamagotchiModel model;
    private TamagotchiRepository repository;
    private TamagotchiUI ui;
    private Runnable updateRunnable;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    public ImageView dog;
    private ImageView accessories;
    private ImageView suds;
    private TextView coins;
    private TextView level;
    private ImageButton feedButton;
    private ImageButton batheButton;
    private ImageButton walkButton;
    private ImageButton helpButton;

    private ImageButton burgerMenu;
    private ImageButton backButton;
    private ImageButton tickButton;

    private View mainMenu;
    private View foodMenu;
    private View hatMenu;
    private View XPandCoins;
    private View levelGroup;

    private ImageView draggingFood;
    private ImageView draggingSponge;
    private float dX, dY;

    private TextView foodHint;
    private TextView bathHint;

    private TextView brownCost;
    private TextView pinkCost;
    private TextView greenCost;
    private TextView pinkPartyCost;

    private boolean brought = false;

    private int cleanProgress = 0;
    private int hatRes = 0;

    private SoundPool soundPool;
    private int crunchSoundId;
    private int bubblesSoundId;

    private final Map<Integer, Integer> hatPrices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_tamagotchi);

        createNotificationChannel();
        requestNotificationPermission();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();
        crunchSoundId = soundPool.load(this, R.raw.crunch_10, 1);
        bubblesSoundId = soundPool.load(this, R.raw.bubbles, 1);

        hatPrices.put(R.drawable.browncowboyhat, 100);
        hatPrices.put(R.drawable.pinkcowboyhat, 100);
        hatPrices.put(R.drawable.partyhatgreen, 75);
        hatPrices.put(R.drawable.partyhatpink, 75);

        feedButton = findViewById(R.id.feed);
        batheButton = findViewById(R.id.bathe);
        walkButton = findViewById(R.id.walk);
        ImageButton hatButton = findViewById(R.id.dress);
        dog = findViewById(R.id.Dog);
        burgerMenu = findViewById(R.id.burgerMenu);
        foodMenu = findViewById(R.id.foodMenu);
        backButton = findViewById(R.id.backButton);
        mainMenu = findViewById(R.id.mainMenu);
        hatMenu = findViewById(R.id.hatMenu);
        draggingFood = findViewById(R.id.draggingFood);
        draggingSponge = findViewById(R.id.draggingSponge);
        suds = findViewById(R.id.suds);
        accessories = findViewById(R.id.accessories);
        tickButton = findViewById(R.id.confirmButton);
        XPandCoins = findViewById(R.id.statsGroup);
        coins = findViewById(R.id.coins);
        level = findViewById(R.id.level);
        levelGroup = findViewById(R.id.levelGroup);
        ImageButton levelButton = findViewById(R.id.LevelImage);
        helpButton = findViewById(R.id.helpButton);
        foodHint = findViewById(R.id.foodHint);
        bathHint = findViewById(R.id.bathHint);

        brownCost = findViewById(R.id.brownCost);
        pinkCost = findViewById(R.id.pinkCost);
        greenCost = findViewById(R.id.greenCost);
        pinkPartyCost = findViewById(R.id.pinkPartyCost);

        model = new TamagotchiModel(100, 100, 100, 0, 0, 0, 0, 0, getString(R.string.walk_name), 0, 0, 0,0,0, "city");
        repository = new TamagotchiRepository(this);
        //repository.clear();
        ui = new TamagotchiUI(this);
        presenter = new TamagotchiPresenter(this, model, ui, repository, this);

        if (repository.getUsername().isEmpty()) {
            if (repository.IsFirstLaunch()) {
                model.feed(100);
                model.clean(100);
                model.walk(100);
                model.coins(200);

                String defaultCity = getString(R.string.default_city);
                model.setCity(defaultCity);
                repository.saveCity(defaultCity);
            } else {
                presenter.loadStats();
            }

            ui.showWelcomeDialog();
        } else {
            presenter.loadStats();
        }

        int selectedHat = model.getSelectedHat();
        if (selectedHat != 0) {
            accessories.setImageResource(selectedHat);
            accessories.setVisibility(VISIBLE);
        }

        coins.setText(getString(R.string.number_format, repository.getCoins()));
        level.setText(getString(R.string.number_format, repository.getLevel()));

        feedButton.setOnClickListener(v -> presenter.onFeedClicked());
        batheButton.setOnClickListener(v -> presenter.onCleanClicked());
        hatButton.setOnClickListener(v -> presenter.onHatClicked());
        backButton.setOnClickListener(v -> hideMenus());
        walkButton.setOnClickListener(v -> presenter.onWalkClicked());
        levelButton.setOnClickListener(v -> presenter.onLevelClicked());
        burgerMenu.setOnClickListener(v -> presenter.onSettingsClicked());

        helpButton.setOnClickListener(v -> {
            if (draggingFood.getVisibility() == VISIBLE) {
                foodHint.setVisibility(foodHint.getVisibility() == VISIBLE ? View.GONE : VISIBLE);
            } else if (draggingSponge.getVisibility() == VISIBLE) {
                bathHint.setVisibility(bathHint.getVisibility() == VISIBLE ? View.GONE : VISIBLE);
            }
        });

        View.OnClickListener foodClickListener = v -> {
            int drawableRes = 0;
            int hungerValue = 0;

            if (v.getId() == R.id.sock) {
                drawableRes = R.drawable.sock;
                if (buyFood(10)) return;
                hungerValue = 10;
            } else if (v.getId() == R.id.broccoli) {
                drawableRes = R.drawable.brocali;
                if (buyFood(20)) return;
                hungerValue = 25;
            } else if (v.getId() == R.id.cheese) {
                drawableRes = R.drawable.cheese;
                if (buyFood(35)) return;
                hungerValue = 50;
            } else if (v.getId() == R.id.meat) {
                drawableRes = R.drawable.food_icon;
                if (buyFood(50)) return;
                hungerValue = 100;
            }

            foodMenu.setVisibility(View.GONE);
            spawnFoodForDragging(drawableRes, hungerValue);
            updateUI();
        };
        findViewById(R.id.sock).setOnClickListener(foodClickListener);
        findViewById(R.id.broccoli).setOnClickListener(foodClickListener);
        findViewById(R.id.cheese).setOnClickListener(foodClickListener);
        findViewById(R.id.meat).setOnClickListener(foodClickListener);

        View.OnClickListener hatClickListener = v -> {
            if (v.getId() == R.id.brownCowboy) hatRes = R.drawable.browncowboyhat;
            else if (v.getId() == R.id.pinkCowboy) hatRes = R.drawable.pinkcowboyhat;
            else if (v.getId() == R.id.greenParty) hatRes = R.drawable.partyhatgreen;
            else if (v.getId() == R.id.pinkParty) hatRes = R.drawable.partyhatpink;
            else if (v.getId() == R.id.noHat) {
                accessories.setVisibility(View.GONE);
                hatRes = 0;
                return;
            }

            if (hatRes != 0) {
                accessories.setImageResource(hatRes);
                accessories.setVisibility(VISIBLE);
            }
        };
        findViewById(R.id.brownCowboy).setOnClickListener(hatClickListener);
        findViewById(R.id.pinkCowboy).setOnClickListener(hatClickListener);
        findViewById(R.id.greenParty).setOnClickListener(hatClickListener);
        findViewById(R.id.pinkParty).setOnClickListener(hatClickListener);
        findViewById(R.id.noHat).setOnClickListener(hatClickListener);

        tickButton.setOnClickListener(v -> buyHat(hatRes));

        updateRunnable = () -> {
            presenter.applyTimeDecay(10);
            handler.postDelayed(updateRunnable, 10000);
        };
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Hungry Dog Channel";
            String description = "Notifications for when your dog is hungry";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cancelHungryNotification();
        presenter.attach();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scheduleHungryNotification(model.getHunger());
        presenter.saveStats();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    @Override
    public void updateHunger(int value) {
        updateButton(feedButton, value);
    }

    @Override
    public void updateClean(int value) {
        updateButton(batheButton, value);
    }

    @Override
    public void updateWalk(int value) {
        updateButton(walkButton, value);
    }

    @Override
    public void showDogState(int drawable) {
        dog.setImageResource(drawable);
        dog.setTag(drawable);
    }

    @Override
    public void playAnimation(int[] frames, int delay) {
        for (int i = 0; i < frames.length; i++) {
            int frame = frames[i];
            handler.postDelayed(() -> dog.setImageResource(frame), (long) delay * i);
        }
    }

    private void updateButton(ImageButton btn, int val) {
        LayerDrawable bg = (LayerDrawable) btn.getBackground();
        ClipDrawable fill = (ClipDrawable) bg.findDrawableByLayerId(R.id.button_fill);
        fill.setLevel(val * 100);
    }

    public void updateUI(){
        coins.setText(getString(R.string.number_format, model.getCoins()));
        level.setText(getString(R.string.number_format, model.getLevel()));
    }

    public void showBackButton(boolean c, boolean h) {
        backButton.setVisibility(VISIBLE);
        backButton.setAlpha(0f);
        backButton.animate().alpha(1f).setDuration(300).start();
        if (c) {
            levelGroup.setVisibility(View.GONE);
        } else {
            XPandCoins.setVisibility(View.GONE);
        }
        if (h) {
            helpButton.setVisibility(View.VISIBLE);
        }
    }

    public void showXPandCoins(){
        XPandCoins.setVisibility(VISIBLE);
        levelGroup.setVisibility(VISIBLE);
        XPandCoins.setAlpha(0f);
        XPandCoins.animate().alpha(1f).setDuration(300).start();
        backButton.setVisibility(View.GONE);
        XPandCoins.animate().translationY(0).setDuration(300).start();
    }
    @Override
    public void showFoodMenu() {
        burgerMenu.animate().translationX(burgerMenu.getWidth() + 100).setDuration(300)
                .withEndAction(() -> burgerMenu.setVisibility(View.GONE));
        mainMenu.setVisibility(View.GONE);
        foodMenu.setVisibility(VISIBLE);
        foodMenu.setTranslationY(500);
        foodMenu.animate().translationY(0).setDuration(300).start();
        showBackButton(true, false);
        
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            XPandCoins.animate().translationY(0).setDuration(300).start();
        }
    }

    @Override
    public void showHatMenu() {
        updateHatPricesVisibility();
        burgerMenu.animate().translationX(burgerMenu.getWidth() + 100).setDuration(300)
                .withEndAction(() -> burgerMenu.setVisibility(View.GONE));
        mainMenu.setVisibility(View.GONE);
        hatMenu.setVisibility(VISIBLE);
        hatMenu.setTranslationY(500);
        hatMenu.animate().translationY(0).setDuration(300).start();
        showBackButton(true, false);
        tickButton.setVisibility(VISIBLE);
        tickButton.setAlpha(0f);
        tickButton.animate().alpha(1f).setDuration(300).start();

        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            XPandCoins.animate().translationY(0).setDuration(300).start();
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void showSponge() {
        mainMenu.setVisibility(View.GONE);
        burgerMenu.animate().translationX(burgerMenu.getWidth() + 100).setDuration(300)
                .withEndAction(() -> burgerMenu.setVisibility(View.GONE));
        cleanProgress = 0;
        draggingSponge.setAlpha(1.0f);
        draggingSponge.setVisibility(View.INVISIBLE);
        suds.setImageResource(R.drawable.suds1);
        suds.setVisibility(View.GONE);

        showBackButton(false, true);

        if (model.getLifetimeBathed() == 0) {
            bathHint.setVisibility(VISIBLE);
        }

        draggingSponge.post(() -> {
            View mainContainer = findViewById(R.id.main);
            draggingSponge.setX((mainContainer.getWidth() - draggingSponge.getWidth()) / 2f);
            draggingSponge.setY(mainContainer.getHeight() - draggingSponge.getHeight() - 100);
            draggingSponge.setVisibility(VISIBLE);
        });

        draggingSponge.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    v.performClick();
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    v.setX(event.getRawX() + dX);
                    v.setY(event.getRawY() + dY);

                    Rect spongeRect = new Rect();
                    draggingSponge.getGlobalVisibleRect(spongeRect);

                    Rect dogRect = new Rect();
                    dog.getGlobalVisibleRect(dogRect);

                    if (presenter.isSpongeCleaning(spongeRect, dogRect)) {
                        cleanProgress += 1;
                        cleanProgress = Math.min(cleanProgress, 225);

                        if (cleanProgress >= 5 && cleanProgress < 25)
                            suds.setImageResource(R.drawable.suds1);
                        else if (cleanProgress >= 25 && cleanProgress < 75)
                            suds.setImageResource(R.drawable.suds2);
                        else if (cleanProgress >= 75 && cleanProgress < 150)
                            suds.setImageResource(R.drawable.suds3);
                        else if (cleanProgress >= 150) {
                            if (suds.getVisibility() == View.VISIBLE && cleanProgress < 151) {
                                presenter.onClean(0);
                            }
                            if (cleanProgress == 150) {
                                playBubblesSound();
                            }
                            suds.setImageResource(R.drawable.suds4);
                        }

                        suds.setVisibility(View.VISIBLE);

                        if (cleanProgress >= 225) {
                            presenter.onClean(100);
                            draggingSponge.setVisibility(View.GONE);
                            draggingSponge.setOnTouchListener(null);
                            hideMenus();
                        }
                    }

                    break;
                }
                default:
                    return false;
            }
            return true;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void spawnFoodForDragging(int drawableRes, int hungerValue) {
        draggingFood.setImageResource(drawableRes);
        draggingFood.setAlpha(1.0f);
        draggingFood.setVisibility(View.INVISIBLE);

        showBackButton(false, true);

        if (model.getLifetimeFed() == 0) {
            foodHint.setVisibility(VISIBLE);
        }

        draggingFood.post(() -> {
            View mainContainer = findViewById(R.id.main);
            draggingFood.setX((mainContainer.getWidth() - draggingFood.getWidth()) / 2f);
            draggingFood.setY(mainContainer.getHeight() - draggingFood.getHeight() - 100);
            draggingFood.setVisibility(View.VISIBLE);
        });

        draggingFood.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        v.performClick();
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.performClick();

                        Rect foodRect = new Rect();
                        v.getGlobalVisibleRect(foodRect);

                        Rect dogRect = new Rect();
                        dog.getGlobalVisibleRect(dogRect);

                        if (Rect.intersects(foodRect, dogRect)) {
                            presenter.onFeed(hungerValue);
                            v.setVisibility(View.GONE);
                            hideMenus();
                        }

                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void hideMenus() {
        burgerMenu.setVisibility(VISIBLE);
        burgerMenu.animate().translationX(0).setDuration(300).start();

        if (foodMenu.getVisibility() == VISIBLE) {
            foodMenu.animate().translationY(500).setDuration(300)
                    .withEndAction(() -> {
                        foodMenu.setVisibility(View.GONE);
                        mainMenu.setVisibility(VISIBLE);
                    });
        } else if (hatMenu.getVisibility() == VISIBLE) {
            hatMenu.animate().translationY(500).setDuration(300)
                    .withEndAction(() -> {
                        hatMenu.setVisibility(View.GONE);
                        mainMenu.setVisibility(VISIBLE);
                    });
            
            if (!brought) {
                int selectedHat = model.getSelectedHat();
                if (selectedHat == 0) {
                    accessories.setVisibility(View.GONE);
                } else {
                    accessories.setImageResource(selectedHat);
                    accessories.setVisibility(VISIBLE);
                }
            }
            brought = false;
            tickButton.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> tickButton.setVisibility(View.GONE));
        } else mainMenu.setVisibility(VISIBLE);

        draggingFood.setVisibility(View.GONE);
        draggingSponge.setVisibility(View.GONE);
        suds.setVisibility(View.GONE);
        suds.setImageResource(R.drawable.suds1);
        backButton.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> {
                    backButton.setVisibility(View.GONE);
                    showXPandCoins();
                });
        helpButton.setVisibility(View.GONE);
        foodHint.setVisibility(View.GONE);
        bathHint.setVisibility(View.GONE);
    }

    private void updateHatPricesVisibility() {
        brownCost.setVisibility(model.isHatOwned("browncowboyhat") ? View.GONE : View.VISIBLE);
        pinkCost.setVisibility(model.isHatOwned("pinkcowboyhat") ? View.GONE : View.VISIBLE);
        greenCost.setVisibility(model.isHatOwned("partyhatgreen") ? View.GONE : View.VISIBLE);
        pinkPartyCost.setVisibility(model.isHatOwned("partyhatpink") ? View.GONE : View.VISIBLE);
    }

    private void buyHat(int hatID) {
        if (hatID == 0) {
            model.setSelectedHat(0);
            brought = true;
            hideMenus();
            presenter.saveStats();
            return;
        }

        String hatName = getResources().getResourceEntryName(hatID);

        if (model.isHatOwned(hatName)) {
            model.setSelectedHat(hatID);
            brought = true;
            hideMenus();
            presenter.saveStats();
        } else {
            Integer priceObj = hatPrices.get(hatID);
            int price = (priceObj != null) ? priceObj : 0;
            if (model.getCoins() >= price) {
                model.spendCoins(price);
                model.addOwnedHat(hatName);
                model.setSelectedHat(hatID);
                brought = true;
                updateHatPricesVisibility();
                hideMenus();
                updateUI();
                presenter.saveStats();
            } else {
                cantAfford();
            }
        }
    }

    private boolean buyFood(int cost){
        if (model.getCoins() >= cost){
            model.spendCoins(cost);
            updateUI();
            presenter.saveStats();
            return false;
        } else {
            cantAfford();
            return true;
        }
    }

    private void cantAfford(){
        ui.showCantAffordDialog();
    }

    @Override
    public void tailWagAnimation() {
        Object tag = dog.getTag();
        int baseId = (tag instanceof Integer) ? (int) tag : R.drawable.husky_idle;

        int wag1, wag2, wag3, wag4;

        if (baseId == R.drawable.husky_estatic) {
            wag1 = R.drawable.husky_estatic2;
            wag2 = R.drawable.husky_estatic3;
            wag3 = R.drawable.husky_estatic2;
            wag4 = R.drawable.husky_estatic;
        } else if (baseId == R.drawable.husky_happy) {
            wag1 = R.drawable.husky_happy2;
            wag2 = R.drawable.husky_happy3;
            wag3 = R.drawable.husky_happy2;
            wag4 = R.drawable.husky_happy;
        } else {
            wag1 = R.drawable.husky_idle2;
            wag2 = R.drawable.husky_idle3;
            wag3 = R.drawable.husky_idle2;
            wag4 = R.drawable.husky_idle;
        }

        int[] wagFrames = { wag1, wag2, wag3, wag4 };

        playAnimation(wagFrames, 150);
    }

    @Override
    public void playEatingSound() {
        if (soundPool != null && crunchSoundId != 0 && !repository.isMuted()) {
            soundPool.play(crunchSoundId, 1, 1, 0, 0, 1);
        }
    }

    @Override
    public void playBubblesSound() {
        if (soundPool != null && bubblesSoundId != 0 && !repository.isMuted()) {
            soundPool.play(bubblesSoundId, 1, 1, 0, 0, 1);
        }
    }

    @Override
    public void scheduleHungryNotification(int hunger) {
        long timeUntilEmptySeconds = (hunger <= 0) ? 10 : (long) hunger * 360;
        long triggerAtMillis = System.currentTimeMillis() + (timeUntilEmptySeconds * 1000);

        Log.d(TAG, "Scheduling notification for " + timeUntilEmptySeconds + " seconds from now");

        Intent intent = new Intent(this, HungryReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        }
    }

    @Override
    public void cancelHungryNotification() {
        Intent intent = new Intent(this, HungryReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(1);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    public TamagotchiContract.Presenter getPresenter() {
        return presenter;
    }

    public TamagotchiUI getTamagotchiUI() {
        return ui;
    }

    public TamagotchiRepository getRepository() {
        return repository;
    }
}
