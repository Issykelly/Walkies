package com.example.walkies.Tamagotchi;

public class TamagotchiModel implements TamagotchiContract.Model {

    private int hunger;
    private int clean;
    private int walked;

    public TamagotchiModel(int hunger, int clean, int walked){
        this.hunger = hunger;
        this.clean = clean;
        this.walked = walked;
    }

    @Override public int getHunger(){ return hunger; }
    @Override public int getClean(){ return clean; }
    @Override public int getWalked(){ return walked; }

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
    public void decay(long seconds){
        hunger -= seconds / 180;
        clean  -= seconds / 240;
        walked -= seconds / 240;

        hunger = Math.max(0, hunger);
        clean  = Math.max(0, clean);
        walked = Math.max(0, walked);
    }
}

