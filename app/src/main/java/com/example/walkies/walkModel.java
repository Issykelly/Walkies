package com.example.walkies;

public class walkModel {
    private String name;
    private double distance;
    private double longitude;
    private double latitude;

    public walkModel(String name, double distance, double longitude, double latitude) {
        this.name = name;
        this.distance = distance;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getWalkName() {
        return name;
    }

    public double getWalkDistance() {
        return distance;
    }

    public double getWalkLongitude() {
        return longitude;
    }

    public double getWalkLatitude() {
        return latitude;
    }

    public void setWalkName(String name) {
        this.name = name;
    }

    public void setWalkDistance(double distance) {
        this.distance = distance;
    }

    public void setWalkLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setWalkLatitude(double latitude) {
        this.latitude = latitude;
    }
}
