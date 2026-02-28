package com.example.walkies.tamagotchi;

import java.util.HashSet;
import java.util.Set;

public class TamagotchiModel implements TamagotchiContract.Model {

    private int hunger;
    private int clean;
    private int walked;
    private int coins;
    private int xp;
    private int level;
    private Set<String> ownedHats = new HashSet<>();
    private int selectedHat;


    public TamagotchiModel(int hunger, int clean, int walked, int coins, int xp, int level){
        this.hunger = hunger;
        this.clean = clean;
        this.walked = walked;

        this.coins = coins;
        this.xp = xp;
        this.level = level;
    }

    @Override public int getHunger(){ return hunger; }
    @Override public int getClean(){ return clean; }
    @Override public int getWalked(){ return walked; }
    @Override public int getCoins(){ return coins; }
    @Override public int getXP(){ return xp; }
    @Override public int getLevel(){ return level; }


    @Override
    public void feed(int value){
        hunger = Math.min(100, hunger + value);
    }

    @Override
    public void clean(int value){
        clean = Math.min(100, clean + value);
    }

    @Override
    public void walk(int value){
        walked = Math.min(100, walked + value);
    }

    @Override
    public void coins(int value){
        coins = value;
    }

    @Override
    public void spendCoins(int value){
        coins -= value;
    }

    @Override
    public void gainCoins(int value){
        coins += value;
    }

    @Override
    public void xp(int value){
        xp = value;
    }

    @Override
    public void level(int value){
        level = value;
    }

    @Override
    public void gainXP(int value){
        xp += value;
    }

    @Override
    public void levelUP(){
        level++;
        xp = 0;
    }


    @Override
    public void decay(long seconds){
        hunger -= (int) (seconds / 180);
        clean  -= (int) (seconds / 240);
        walked -= (int) (seconds / 240);

        hunger = Math.max(0, hunger);
        clean  = Math.max(0, clean);
        walked = Math.max(0, walked);
    }

    @Override
    public void checkXPLevels(){
        if (xp >= 100+(level*10)){
            levelUP();
        }
    }

    @Override
    public Set<String> getOwnedHats() {
        return ownedHats;
    }

    @Override
    public void setOwnedHats(Set<String> ownedHats) {
        this.ownedHats = ownedHats;
    }

    @Override
    public void addOwnedHat(String hatName) {
        ownedHats.add(hatName);
    }

    @Override
    public boolean isHatOwned(String hatName) {
        return ownedHats.contains(hatName);
    }

    @Override
    public int getSelectedHat() {
        return selectedHat;
    }

    @Override
    public void setSelectedHat(int hatId) {
        this.selectedHat = hatId;
    }
}
