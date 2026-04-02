package com.example.walkies;

import android.location.Location;

public class walkModel {

    private final String name;
    private double distance;
    private final double longitude;
    private final double latitude;
    private final String[] hints;

    public walkModel(String name, double distance, double longitude, double latitude, String[] hints) {
        this.name = name;
        this.distance = distance;
        this.longitude = longitude;
        this.latitude = latitude;
        this.hints = hints;
    }

    public String getWalkName() { return name; }
    public double getWalkDistance() { return distance; }
    public double getWalkLongitude() { return longitude; }
    public double getWalkLatitude() { return latitude; }
    public String[] getHints() { return hints; }

    public void updateDistance(Location userLocation) {
        float[] results = new float[1];
        Location.distanceBetween(
                userLocation.getLatitude(),
                userLocation.getLongitude(),
                latitude,
                longitude,
                results
        );
        distance = results[0] * 0.000621371;
    }

    public void reset() {
        distance = 0;
    }
}