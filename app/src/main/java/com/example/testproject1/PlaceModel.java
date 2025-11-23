package com.example.testproject1;

public class PlaceModel {
    private String name;
    private String address;
    private double rating; // Giả lập
    private double lat;
    private double lon;

    public PlaceModel(String name, String address, double rating, double lat, double lon) {
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.lat = lat;
        this.lon = lon;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getRating() { return rating; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
}