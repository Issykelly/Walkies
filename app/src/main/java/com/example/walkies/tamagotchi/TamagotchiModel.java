package com.example.walkies.tamagotchi;

import java.util.HashSet;
import java.util.Set;

public class TamagotchiModel implements TamagotchiContract.Model {

    private String city;
    private int hunger;
    private int clean;
    private int walked;
    private int coins;
    private int lifetimeCoins;
    private int xp;
    private int lifetimeXP;
    private int level;
    private int lifetimeCircular;
    private int lifetimeMystery;
    private int lifetimeFed;
    private int lifetimeBathed;
    private Set<String> ownedHats = new HashSet<>();
    private int selectedHat;


    public TamagotchiModel(int hunger, int clean, int walked, int coins, int xp, int level, int lifetimeXP, int lifetimeCoins, int lifetimeCircular, int lifetimeMystery, int lifetimeFed, int lifetimeBathed, String city){
        this.hunger = hunger;
        this.clean = clean;
        this.walked = walked;

        this.coins = coins;
        this.lifetimeCoins = lifetimeCoins;
        this.xp = xp;
        this.level = level;
        this.lifetimeXP = lifetimeXP;
        this.lifetimeCircular = lifetimeCircular;
        this.lifetimeMystery = lifetimeMystery;
        this.lifetimeFed = lifetimeFed;
        this.lifetimeBathed = lifetimeBathed;
        this.city = city;
    }

    @Override public int getHunger(){ return hunger; }
    @Override public int getClean(){ return clean; }
    @Override public int getWalked(){ return walked; }
    @Override public int getCoins(){ return coins; }
    @Override public int getXP(){ return xp; }
    @Override public int getLevel(){ return level; }
    @Override public int getLifetimeXP() { return lifetimeXP; }
    @Override public int getLifetimeCoins() { return lifetimeCoins; }
    @Override public int getLifetimeCircular() { return lifetimeCircular; }
    @Override public int getLifetimeMystery() { return lifetimeMystery; }
    @Override public int getLifetimeFed() { return lifetimeFed; }
    @Override public int getLifetimeBathed() { return lifetimeBathed; }
    @Override public String getCity() { return city; }

    @Override
    public void feed(int value){
        hunger = Math.min(100, hunger + value);
        lifetimeFed++;
    }

    @Override
    public void clean(int value){
        clean = Math.min(100, clean + value);
        lifetimeBathed++;
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
        lifetimeCoins += value;
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
        lifetimeXP += value;
    }

    @Override
    public void levelUP(int Extraxp){
        level++;
        gainCoins(100);
        xp = Extraxp;
    }

    @Override
    public void setLifetimeXP(int value) {
        this.lifetimeXP = value;
    }

    @Override
    public void setLifetimeCoins(int value) {
        this.lifetimeCoins = value;
    }

    @Override
    public void setLifetimeCircular(int value) {
        this.lifetimeCircular = value;
    }

    @Override
    public void setLifetimeMystery(int value) {
        this.lifetimeMystery = value;
    }

    @Override
    public void setLifetimeFed(int value) {
        this.lifetimeFed = value;
    }

    @Override
    public void setLifetimeBathed(int value) {
        this.lifetimeBathed = value;
    }

    @Override
    public void setCity(String city) {
        this.city = city;
    }

    // decay
    // ------------------------------------------------------------------------)

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
    public boolean checkXPLevels(){
        if (xp >= 100 + (level * 10)){
            int extraXP = xp - (100 + (level * 10));
            levelUP(extraXP);
            return true;
        }
        return false;
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
