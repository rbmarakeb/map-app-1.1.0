package com.map.android.lib.drone.mission.item;

public class POIItem {

    private String name;
    private double latitude;
    private double longitude;

    public POIItem(String name, double lat, double lon) {
        this.name = name;
        this.latitude = lat;
        this.longitude = lon;
    }

    public POIItem(String[] tokens) {
        this.latitude = Double.parseDouble(tokens[0]);
        this.longitude = Double.parseDouble(tokens[1]);
        this.name = tokens[2];
    }

    public String getName() {
        if (name == null) {
            name = "NONE";
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String toString() {
        return String.format("%f,%f,%s\n", latitude, longitude, name);
    }
}
