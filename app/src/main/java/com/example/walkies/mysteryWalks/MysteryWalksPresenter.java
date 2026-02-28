package com.example.walkies.mysteryWalks;

import com.example.walkies.walkModel;

public class MysteryWalksPresenter implements MysteryWalksContract.Presenter {

    private static final float THRESHOLD=3000f;

    private final MysteryWalksContract.View view;
    private final MysteryWalksContract.Model model;

    private walkModel active;
    private String[] hints;
    private int hintIndex=0;
    private boolean tracking=false;
    private boolean reached=false;

    public MysteryWalksPresenter(
            MysteryWalksContract.View view,
            MysteryWalksContract.Model model){
        this.view=view;
        this.model=model;
    }

    @Override
    public void init(boolean fresh){

        if(!view.hasLocationPermission())
            view.requestLocationPermission();

        model.loadWalks(view::showWalks);

        if(fresh){
            active=null;
            view.showHints(false);
            view.showWalkList(true);
        }
    }

    @Override
    public void walkSelected(walkModel w){

        active=w;
        hints=w.getHints();
        hintIndex=0;
        reached=false;

        if(hints!=null && hints[0]!=null){
            view.showHints(true);
            view.showHint(hints[0],1);
        }

        view.showWalkList(false);

        if(!view.hasLocationPermission()){
            view.requestLocationPermission();
            return;
        }

        model.getLastLocation(l->{
            if(l!=null) view.showDistance(dist(l));
        });

        model.startTracking(this::location);
        tracking=true;
    }

    private void location(android.location.Location l){
        if(l==null||active==null||reached)return;

        int d=dist(l);
        view.showDistance(d);

        if(d<THRESHOLD){
            reached=true;
            model.stopTracking();
            model.saveCompletion();
            view.openTamagotchi();
        }
    }

    private int dist(android.location.Location l){
        float[] r=new float[1];
        android.location.Location.distanceBetween(
                l.getLatitude(),l.getLongitude(),
                active.getWalkLatitude(),
                active.getWalkLongitude(),
                r);
        return Math.round(r[0]);
    }

    @Override
    public void nextHint(){
        if(hints==null) return;
        if(hintIndex<hints.length-1 && hints[hintIndex+1]!=null){
            hintIndex++;
            view.showHint(hints[hintIndex],hintIndex+1);
        }
    }

    @Override
    public void prevHint(){
        if(hints==null) return;
        if(hintIndex>0){
            hintIndex--;
            view.showHint(hints[hintIndex],hintIndex+1);
        }
    }

    @Override
    public void giveUp(){
        if(active!=null)
            view.openMap(active.getWalkLatitude(),
                    active.getWalkLongitude());
    }

    @Override
    public void permissionResult(boolean granted){
        if(!granted)
            view.showMessage("Location permission required for Walks");
        else if(active!=null && !tracking){
            model.startTracking(this::location);
            tracking=true;
        }
    }

    @Override
    public void resume(){
        if(active!=null && !tracking && view.hasLocationPermission()){
            model.startTracking(this::location);
            tracking=true;
        }
    }

    @Override
    public void pause(){
        model.stopTracking();
        tracking=false;
    }
}
