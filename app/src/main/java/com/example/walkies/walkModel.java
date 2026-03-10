package com.example.walkies;

public class walkModel {
    private String name;
    private double distance;
    private double longitude;
    private double latitude;
    private boolean isSelected;
    private String[] hints;

    public walkModel(String name, double distance, double longitude, double latitude, String[] hints) {
        this.name = name;
        this.distance = distance;
        this.longitude = longitude;
        this.latitude = latitude;
        this.isSelected = false;
        this.hints = hints;
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

    public String[] getHints() { return hints;} // may return null

    public boolean isSelected() {
        return isSelected;
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

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
