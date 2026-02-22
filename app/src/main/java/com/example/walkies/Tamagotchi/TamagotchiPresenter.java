package com.example.walkies.Tamagotchi;

import android.graphics.Rect;

import com.example.walkies.R;

public class TamagotchiPresenter implements TamagotchiContract.Presenter {

    private final TamagotchiContract.View view;
    private final TamagotchiContract.Model model;
    private final TamagotchiRepository repository;

    public TamagotchiPresenter(TamagotchiContract.View view,
                               TamagotchiContract.Model model,
                               TamagotchiRepository repository){
        this.view = view;
        this.model = model;
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

        long secondsPassed = (System.currentTimeMillis() / 1000) - repository.getLastSavedTime();
        applyTimeDecay(secondsPassed);
    }

    @Override
    public void saveStats() {
        repository.saveStats(model.getHunger(), model.getClean(), model.getWalked());
        repository.saveTime(System.currentTimeMillis() / 1000);
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
    public void onWalkClicked(){
        view.showWalkOptions();
    }

    private void updateUI(){
        view.updateHunger(model.getHunger());
        view.updateClean(model.getClean());
        view.updateWalk(model.getWalked());

        int happiness = model.getHunger() + model.getClean() + model.getWalked();
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