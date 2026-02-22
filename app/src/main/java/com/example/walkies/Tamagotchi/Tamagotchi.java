package com.example.walkies.Tamagotchi;

import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.example.walkies.CircularWalks.CircularWalksMap;
import com.example.walkies.MysteryWalks.MysteryWalks;
import com.example.walkies.R;

public class Tamagotchi extends AppCompatActivity
        implements TamagotchiContract.View {

    // model view presenter classes
    // ------------------------------------------------------------------------
    private TamagotchiPresenter presenter;
    private Runnable updateRunnable;
    private Handler handler = new Handler(Looper.getMainLooper());

    // views from activity
    // ------------------------------------------------------------------------
    public static ImageView dog;
    private ImageView accessories;
    private ImageView suds;

    // stats buttons
    // ------------------------------------------------------------------------
    private ImageButton feedButton;
    private ImageButton batheButton;
    private ImageButton walkButton;

    // navigation
    // ------------------------------------------------------------------------
    private ImageButton burgerMenu;
    private ImageButton backButton;
    private ImageButton hatButton;
    private ImageButton tickButton;

    // menu containers
    // ------------------------------------------------------------------------
    private View mainMenu;
    private View foodMenu;
    private HorizontalScrollView hatMenu;

    // for interactivity mini games
    // ------------------------------------------------------------------------
    private ImageView draggingFood;
    private ImageView draggingSponge;
    private float dX, dY;

    // flags
    // ------------------------------------------------------------------------
    private boolean brought = false;

    // progress
    // ------------------------------------------------------------------------
    private int cleanProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tamagotchi);

        // define activity elements
        // ------------------------------------------------------------------------
        feedButton = findViewById(R.id.feed);
        batheButton = findViewById(R.id.bathe);
        walkButton = findViewById(R.id.walk);
        hatButton = findViewById(R.id.dress);
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

        // initalise mvp
        // ------------------------------------------------------------------------

        TamagotchiModel model = new TamagotchiModel(100, 100, 100);
        TamagotchiRepository repository = new TamagotchiRepository(getSharedPreferences("WalkiesPrefs", MODE_PRIVATE));
        presenter = new TamagotchiPresenter(this, model, repository);

        // is this the first launch? are we fetching saved stats or creating new ones?
        // ------------------------------------------------------------------------

        if (repository.IsFirstLaunch()){
            model.feed(100);
            model.clean(100);
            model.walk(100);
        }else {
            presenter.loadStats();
        }

        // on click listeners
        // ------------------------------------------------------------------------
        feedButton.setOnClickListener(v -> presenter.onFeedClicked());
        batheButton.setOnClickListener(v -> presenter.onCleanClicked());
        hatButton.setOnClickListener(v -> presenter.onHatClicked());
        backButton.setOnClickListener(v -> hideMenus());
        walkButton.setOnClickListener(v -> presenter.onWalkClicked());

        // food menu
        // ------------------------------------------------------------------------
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

        // hat menu
        // ------------------------------------------------------------------------
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

        tickButton.setOnClickListener(v -> buyHat(1));

        updateRunnable = () -> handler.postDelayed(updateRunnable, 10000);
    }

    // on pause / resume implementation
    // ------------------------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        presenter.attach();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.saveStats();
        handler.removeCallbacks(updateRunnable);
    }

    // update stats
    // ------------------------------------------------------------------------
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

    // update dog state
    // ------------------------------------------------------------------------

    @Override
    public void showDogState(int drawable) {
        dog.setImageResource(drawable);
    }

    @Override
    public void playAnimation(int[] frames, int delay) {
        for (int i = 0; i < frames.length; i++) {
            int frame = frames[i];
            handler.postDelayed(() -> dog.setImageResource(frame), delay * i);
        }
    }

    // updates buttons
    // ------------------------------------------------------------------------

    private void updateButton(ImageButton btn, int val) {
        LayerDrawable bg = (LayerDrawable) btn.getBackground();
        ClipDrawable fill = (ClipDrawable) bg.findDrawableByLayerId(R.id.button_fill);
        fill.setLevel(val * 100);
    }

    // menu maintainers
    // ------------------------------------------------------------------------

    @Override
    public void showFoodMenu() {
        burgerMenu.animate().translationX(burgerMenu.getWidth() + 100).setDuration(300)
                .withEndAction(() -> burgerMenu.setVisibility(View.GONE));
        mainMenu.setVisibility(View.GONE);
        foodMenu.setVisibility(VISIBLE);
        foodMenu.setTranslationY(500);
        foodMenu.animate().translationY(0).setDuration(300).start();
        backButton.setVisibility(VISIBLE);
        backButton.setAlpha(0f);
        backButton.animate().alpha(1f).setDuration(300).start();
    }

    @Override
    public void showHatMenu() {
        burgerMenu.animate().translationX(burgerMenu.getWidth() + 100).setDuration(300)
                .withEndAction(() -> burgerMenu.setVisibility(View.GONE));
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

    @Override
    public void showWalkOptions() {
        StartWalkDialogFragment dialog = new StartWalkDialogFragment();
        dialog.show(getSupportFragmentManager(), "WalkDialogFragment");
    }

    //dragging minigames
    // ------------------------------------------------------------------------

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void showSponge() {
        mainMenu.setVisibility(View.GONE);
        cleanProgress = 0;
        draggingSponge.setAlpha(1.0f);
        draggingSponge.setVisibility(View.INVISIBLE);
        suds.setVisibility(View.GONE);

        draggingSponge.post(() -> {
            View mainContainer = findViewById(R.id.main);
            draggingSponge.setX((mainContainer.getWidth() - draggingSponge.getWidth()) / 2f);
            draggingSponge.setY(mainContainer.getHeight() - draggingSponge.getHeight() - 100);
            draggingSponge.setVisibility(VISIBLE);
        });

        draggingSponge.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
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
                            else if (cleanProgress >= 150)
                                suds.setImageResource(R.drawable.suds4);

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
            }
        });
    }

    // food minigame
    // ------------------------------------------------------------------------
    private void spawnFoodForDragging(int drawableRes, int hungerValue) {
        draggingFood.setImageResource(drawableRes);
        draggingFood.setAlpha(1.0f);
        draggingFood.setVisibility(View.INVISIBLE);

        draggingFood.post(new Runnable() {
            @Override
            public void run() {
                View mainContainer = findViewById(R.id.main);
                draggingFood.setX((mainContainer.getWidth() - draggingFood.getWidth()) / 2f);
                draggingFood.setY(mainContainer.getHeight() - draggingFood.getHeight() - 100);
                draggingFood.setVisibility(View.VISIBLE);
            }
        });

        draggingFood.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        Rect foodRect = new Rect();
                        draggingFood.getGlobalVisibleRect(foodRect);

                        Rect dogRect = new Rect();
                        dog.getGlobalVisibleRect(dogRect);

                        if (presenter.isFoodFed(foodRect, dogRect)) {
                            presenter.onFeed(hungerValue);
                            draggingFood.setVisibility(View.GONE);
                            hideMenus();
                        }
                        break;
                    }
                    default:
                        return false;
                }
                return true;
            }
        });
    }
    // hide menus
    // ------------------------------------------------------------------------

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
            if (!brought) accessories.setVisibility(View.GONE);
            brought = false;
            tickButton.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> tickButton.setVisibility(View.GONE));
        } else mainMenu.setVisibility(VISIBLE);

        draggingFood.setVisibility(View.GONE);
        draggingSponge.setVisibility(View.GONE);
        suds.setVisibility(View.GONE);
        suds.setImageResource(R.drawable.suds1);
        backButton.animate().alpha(0f).setDuration(300)
                .withEndAction(() -> backButton.setVisibility(View.GONE));
    }

    // did the user buy the hat?
    // ------------------------------------------------------------------------
    private void buyHat(int hatID) {
        brought = true;
        hideMenus();
    }

    // animation
    // ------------------------------------------------------------------------

    @Override
    public void tailWagAnimation() {
        int baseId = (dog.getTag() != null) ? (int) dog.getTag() : R.drawable.husky_idle;
        String baseName = getResources().getResourceEntryName(baseId);

        int wag1 = getResources().getIdentifier(baseName + "2", "drawable", getPackageName());
        int wag2 = getResources().getIdentifier(baseName + "3", "drawable", getPackageName());
        int wag3 = getResources().getIdentifier(baseName + "2", "drawable", getPackageName());
        int wag4 = getResources().getIdentifier(baseName, "drawable", getPackageName());

        int[] wagFrames = { wag1, wag2, wag3, wag4 };

        playAnimation(wagFrames, 150);
    }

    // for user input
    // ------------------------------------------------------------------------

    public static class StartWalkDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());        builder.setMessage(R.string.dialog_choose_walk_type)
                    .setPositiveButton(R.string.circular, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(getActivity(), CircularWalksMap.class);
                            intent.putExtra("is_fresh_launch", true);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.mystery, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent intent = new Intent(getActivity(), MysteryWalks.class);
                            intent.putExtra("is_fresh_launch", true);
                            startActivity(intent);
                        }
                    });
            return builder.create();
        }
    }
}