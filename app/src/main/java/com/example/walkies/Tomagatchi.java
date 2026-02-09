package com.example.walkies;

import static android.view.View.VISIBLE;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.firestore.FirebaseFirestore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.atomic.AtomicInteger;

public class Tomagatchi extends AppCompatActivity {

    private static final String TAG = "WalkiesDebug";
    private int hunger;
    private int clean;
    private int walked;
    private boolean animating = false;
    private boolean animatingHalted = false;
    private boolean brought = false;
    private static final String PREFS_NAME = "WalkiesPrefs";
    private static final String KEY_LAST_SAVE_TIME = "last_save_time";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    
    private static final String KEY_HUNGER = "hunger";
    private static final String KEY_CLEAN = "clean";
    private static final String KEY_WALKED = "walked";

    private ImageButton feedButton;
    private ImageButton batheButton;
    private ImageButton walkButton;
    private ImageButton dressButton;
    private ImageButton backButton;
    private ImageButton tickButton;
    private ImageButton imageButton3;
    private LinearLayout mainMenu;
    private LinearLayout foodMenu;
    private HorizontalScrollView hatMenu;
    private ImageView draggingFood;
    private ImageView draggingSponge;
    private ImageView suds;
    private ImageView accessories;
    
    private SharedPreferences prefs;
    private ImageView dog;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    private float dX, dY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tomagatchi);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check Google Play Services status
        checkPlayServices();

        // Initialize Views
        feedButton = findViewById(R.id.feed);
        batheButton = findViewById(R.id.bathe);
        walkButton = findViewById(R.id.walk);
        dressButton = findViewById(R.id.dress);
        backButton = findViewById(R.id.backButton);
        imageButton3 = findViewById(R.id.burgerMenu);
        mainMenu = findViewById(R.id.mainMenu);
        foodMenu = findViewById(R.id.foodMenu);
        hatMenu = findViewById(R.id.hatMenu);
        draggingFood = findViewById(R.id.draggingFood);
        draggingSponge = findViewById(R.id.draggingSponge);
        dog = findViewById(R.id.Dog);
        suds = findViewById(R.id.suds);
        accessories = findViewById(R.id.accessories);
        tickButton = findViewById(R.id.confirmButton);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Check for first launch
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        if (isFirstLaunch) {
            hunger = 100;
            clean = 100;
            walked = 100;
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
            saveCurrentStats();
        } else {
            setNeedsAfterTime();
        }

        // Click listeners
        feedButton.setOnClickListener(v -> showFoodMenu());
        dressButton.setOnClickListener(v -> showHatMenu());
        backButton.setOnClickListener(v -> hideAllSubMenus());
        tickButton.setOnClickListener(v -> buyHat(1));

        batheButton.setOnClickListener(v -> {
            mainMenu.setVisibility(View.GONE);
            backButton.setVisibility(VISIBLE);
            backButton.setAlpha(1f);
            imageButton3.setVisibility(View.GONE);
            spawnSpongeForDragging();
        });

        walkButton.setOnClickListener(v -> {
            Log.d(TAG, "Walk button clicked. Launching CircularWalksMap...");
            Intent intent = new Intent(Tomagatchi.this, CircularWalksMap.class);
            intent.putExtra("is_fresh_launch", true);
            startActivity(intent);
        });
        
        // Food item clicks
        View.OnClickListener foodClickListener = v -> {
            int drawableRes = 0;
            int hungerValue = 0;
            
            if (v.getId() == R.id.sock) {
                drawableRes = R.drawable.sock;
                hungerValue = 10;
            } else if (v.getId() == R.id.broccoli) {
                drawableRes = R.drawable.brocali;
                hungerValue = 25;
            } else if (v.getId() == R.id.cheese) {
                drawableRes = R.drawable.cheese;
                hungerValue = 50;
            } else if (v.getId() == R.id.meat) {
                drawableRes = R.drawable.food_icon;
                hungerValue = 100;
            }
            
            foodMenu.setVisibility(View.GONE);
            spawnFoodForDragging(drawableRes, hungerValue);
        };

        findViewById(R.id.sock).setOnClickListener(foodClickListener);
        findViewById(R.id.broccoli).setOnClickListener(foodClickListener);
        findViewById(R.id.cheese).setOnClickListener(foodClickListener);
        findViewById(R.id.meat).setOnClickListener(foodClickListener);

        // Hat item clicks
        View.OnClickListener hatClickListener = v -> {
            int hatRes = 0;
            if (v.getId() == R.id.brownCowboy) hatRes = R.drawable.browncowboyhat;
            else if (v.getId() == R.id.pinkCowboy) hatRes = R.drawable.pinkcowboyhat;
            else if (v.getId() == R.id.greenParty) hatRes = R.drawable.partyhatgreen;
            else if (v.getId() == R.id.pinkParty) hatRes = R.drawable.partyhatpink;
            else if (v.getId() == R.id.noHat) {
                accessories.setVisibility(View.GONE);
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

        updateButtonUI();

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                saveCurrentStats();
                saveCurrentTime();
                updateButtonUI();
                handler.postDelayed(this, 10000);
            }
        };
    }

    private void checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services NOT available! Error code: " + resultCode);
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000).show();
            }
        } else {
            Log.d(TAG, "Google Play Services is available and ready.");
        }
    }

    private void buyHat(int hatID) {
        brought = true;
        hideAllSubMenus();
    }

    private void hideAllSubMenus() {
        imageButton3.setVisibility(VISIBLE);
        imageButton3.animate().translationX(0).setDuration(300).start();
        
        if (foodMenu.getVisibility() == VISIBLE) {
            foodMenu.animate().translationY(500).setDuration(300).withEndAction(() -> {
                foodMenu.setVisibility(View.GONE);
                mainMenu.setVisibility(VISIBLE);
            });
        } else if (hatMenu.getVisibility() == VISIBLE) {
            hatMenu.animate().translationY(500).setDuration(300).withEndAction(() -> {
                hatMenu.setVisibility(View.GONE);
                mainMenu.setVisibility(VISIBLE);
            });
            if (!brought){
                accessories.setVisibility(View.GONE);
            }
            brought = false;
            tickButton.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                tickButton.setVisibility(View.GONE);
                mainMenu.setVisibility(VISIBLE);
            });
        } else {
            mainMenu.setVisibility(VISIBLE);
        }
        
        draggingFood.setVisibility(View.GONE);
        draggingSponge.setVisibility(View.GONE);
        suds.setVisibility(View.GONE);
        backButton.animate().alpha(0f).setDuration(300).withEndAction(() -> backButton.setVisibility(View.GONE));
    }


    private void showHatMenu() {
        imageButton3.animate().translationX(imageButton3.getWidth() + 100).setDuration(300).withEndAction(() -> imageButton3.setVisibility(View.GONE));
        mainMenu.setVisibility(View.GONE);
        hatMenu.setVisibility(VISIBLE);
        hatMenu.setTranslationY(500);
        hatMenu.animate().translationY(0).setDuration(300).start();
        backButton.setVisibility(VISIBLE);
        backButton.setAlpha(0f);
        backButton.animate().alpha(1f).setDuration(300).start();
        tickButton.setVisibility(VISIBLE);
        tickButton.setAlpha(0f);
        tickButton.animate().alpha(1f).setDuration(300).start();
    }

    private void updateButtonUI() {
        updateSingleButton(feedButton, hunger);
        updateSingleButton(batheButton, clean);
        updateSingleButton(walkButton, walked);
    }

    private void updateSingleButton(ImageButton button, int value) {
        if (button != null && button.getBackground() instanceof LayerDrawable) {
            LayerDrawable bg = (LayerDrawable) button.getBackground();
            Drawable fillDrawable = bg.findDrawableByLayerId(R.id.button_fill);
            if (fillDrawable instanceof ClipDrawable) {
                ClipDrawable fill = (ClipDrawable) fillDrawable;
                fill.setLevel(value * 100);
            }
        }
    }

    private void setNeedsAfterTime(){
        hunger = prefs.getInt(KEY_HUNGER, 100);
        clean = prefs.getInt(KEY_CLEAN, 100);
        walked = prefs.getInt(KEY_WALKED, 100);

        long currentTime = System.currentTimeMillis() / 1000;
        long lastSaveTime = prefs.getLong(KEY_LAST_SAVE_TIME, currentTime);
        long secondsPassed = currentTime - lastSaveTime;

        hunger -= (int) (secondsPassed / 180); 
        clean -= (int) (secondsPassed / 240);  
        walked -= (int) (secondsPassed / 240);

        hunger = Math.max(0, hunger);
        clean = Math.max(0, clean);
        walked = Math.max(0, walked);

        updateButtonUI();
        checkHappiness();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setNeedsAfterTime();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentStats();
        saveCurrentTime();
        handler.removeCallbacks(updateRunnable);
    }

    private void saveCurrentStats() {
        prefs.edit()
                .putInt(KEY_HUNGER, hunger)
                .putInt(KEY_CLEAN, clean)
                .putInt(KEY_WALKED, walked)
                .apply();
    }

    private void saveCurrentTime() {
        long currentTime = System.currentTimeMillis() / 1000;
        prefs.edit().putLong(KEY_LAST_SAVE_TIME, currentTime).apply();
    }

    private void checkHappiness() {
        if (!animating) {
            int happiness = hunger + clean + walked;
            int resId;

            if (happiness >= 250) {
                resId = R.drawable.husky_estatic;
            } else if (happiness >= 200) {
                resId = R.drawable.husky_happy;
            } else {
                resId = R.drawable.husky_idle;
            }

            dog.setImageResource(resId);
            dog.setTag(resId);

            if (happiness >= 250) {
                dog.post(() -> tailWag(2));
            }
        } else {
            animatingHalted = true;
        }
    }

    private void tailWag(int count) {
        try {
            animating = true;
            int baseId = (dog.getTag() != null) ? (int) dog.getTag() : R.drawable.husky_idle;
            String baseName = getResources().getResourceEntryName(baseId);

            int wag1 = getResources().getIdentifier(baseName + "2", "drawable", getPackageName());
            int wag2 = getResources().getIdentifier(baseName + "3", "drawable", getPackageName());

            if (wag1 != 0 && wag2 != 0) {
                int wagDuration = 600;
                for (int i = 0; i < count; i++) {
                    int offset = i * wagDuration;

                    handler.postDelayed(() -> dog.setImageResource(wag1), offset + 100);
                    handler.postDelayed(() -> dog.setImageResource(wag2), offset + 250);
                    handler.postDelayed(() -> dog.setImageResource(wag1), offset + 400);
                    handler.postDelayed(() -> {
                        dog.setImageResource(baseId);
                        if (offset + wagDuration >= count * wagDuration) {
                            animating = false;
                            if (animatingHalted) {
                                animatingHalted = false;
                                checkHappiness();
                            }
                        }
                    }, offset + 550);
                }
            } else {
                animating = false;
            }
        } catch (Exception e) {
            animating = false;
            Log.e("TailWag", "Animation error: " + e.getMessage());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void spawnFoodForDragging(int drawableRes, int hungerValue) {
        draggingFood.setImageResource(drawableRes);
        draggingFood.setAlpha(1.0f);
        draggingFood.setVisibility(View.INVISIBLE);

        draggingFood.post(() -> {
            View mainContainer = findViewById(R.id.main);
            float startX = (mainContainer.getWidth() / 2f) - (draggingFood.getWidth() / 2f);
            float startY = mainContainer.getHeight() - draggingFood.getHeight() - 100;

            draggingFood.setX(startX);
            draggingFood.setY(startY);
            draggingFood.setVisibility(VISIBLE);
        });

        draggingFood.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.setX(event.getRawX() + dX);
                    v.setY(event.getRawY() + dY);
                    break;
                case MotionEvent.ACTION_UP:
                    checkIfFed(hungerValue);
                    break;
                default:
                    return false;
            }
            return true;
        });
    }

    private void checkIfFed(int hungerValue) {
        Rect foodRect = new Rect();
        draggingFood.getGlobalVisibleRect(foodRect);

        Rect dogRect = new Rect();
        dog.getGlobalVisibleRect(dogRect);

        int headTop = dogRect.top + (int)(dogRect.height() * 0.45);
        int headBottom = dogRect.top + (int)(dogRect.height() * 0.55);

        int headLeft = dogRect.left + (int)(dogRect.width() * 0.25);
        int headRight = dogRect.right - (int)(dogRect.width() * 0.25);

        Rect headRect = new Rect(headLeft, headTop, headRight, headBottom);

        if (Rect.intersects(foodRect, headRect)) {
            hunger = Math.min(100, hunger + hungerValue);
            updateButtonUI();
            checkHappiness();
            draggingFood.setVisibility(View.GONE);

            tailWag(5);

            hideAllSubMenus();
        }
    }

    private void showFoodMenu() {
        imageButton3.animate().translationX(imageButton3.getWidth() + 100).setDuration(300).withEndAction(() -> imageButton3.setVisibility(View.GONE));
        mainMenu.setVisibility(View.GONE);
        foodMenu.setVisibility(VISIBLE);
        foodMenu.setTranslationY(500);
        foodMenu.animate().translationY(0).setDuration(300).start();
        backButton.setVisibility(VISIBLE);
        backButton.setAlpha(0f);
        backButton.animate().alpha(1f).setDuration(300).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void spawnSpongeForDragging() {
        AtomicInteger cleanProgress = new AtomicInteger();
        draggingSponge.setAlpha(1.0f);
        draggingSponge.setVisibility(View.INVISIBLE);
        suds.setVisibility(View.GONE);

        draggingSponge.post(() -> {
            View mainContainer = findViewById(R.id.main);
            float startX = (mainContainer.getWidth() / 2f) - (draggingSponge.getWidth() / 2f);
            float startY = mainContainer.getHeight() - draggingSponge.getHeight() - 100;

            draggingSponge.setX(startX);
            draggingSponge.setY(startY);
            draggingSponge.setVisibility(VISIBLE);
        });

        draggingSponge.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.setX(event.getRawX() + dX);
                    v.setY(event.getRawY() + dY);
                    if (checkIfClean()){
                        int progress = cleanProgress.addAndGet(1);
                        if (progress > 5 && suds.getVisibility() != VISIBLE){
                            suds.setVisibility(VISIBLE);
                            suds.setImageResource(R.drawable.suds1);
                        } else if (progress >= 25 && progress < 75){
                            suds.setImageResource(R.drawable.suds2);
                        } else if (progress >= 75 && progress < 150){
                            suds.setImageResource(R.drawable.suds3);
                        } else if (progress >= 150 && progress < 225){
                            suds.setImageResource(R.drawable.suds4);
                        }

                        if (progress >= 225){
                            clean = 100;
                            updateButtonUI();
                            checkHappiness();
                            draggingSponge.setVisibility(View.GONE);
                            draggingSponge.setOnTouchListener(null);
                            tailWag(3);
                            hideAllSubMenus();
                        }
                    }
                    break;
                default:
                    return false;
            }
            return true;
        });
    }

    private boolean checkIfClean(){
        Rect spongeRect = new Rect();
        draggingSponge.getGlobalVisibleRect(spongeRect);

        Rect dogRect = new Rect();
        dog.getGlobalVisibleRect(dogRect);

        int headTop = dogRect.top + (int)(dogRect.height() * 0.45);
        int dogBottom = dogRect.top + (int)(dogRect.height() * 0.70);

        int dogLeft = dogRect.left + (int)(dogRect.width() * 0.15);
        int dogRight = dogRect.right - (int)(dogRect.width() * 0.35);

        Rect headRect = new Rect(dogLeft, headTop, dogRight, dogBottom);

        return Rect.intersects(spongeRect, headRect);
    }

    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = hunger; }
    public int getClean() { return clean; }
    public void setClean(int clean) { this.clean = clean; }
    public int getWalked() { return walked; }
    public void setWalked(int walked) { this.walked = walked; }
}