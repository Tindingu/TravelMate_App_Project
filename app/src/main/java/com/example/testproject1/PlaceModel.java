package com.example.testproject1;

public class PlaceModel {
    private String name;
    private String address;
    private double rating;
    private double lat;
    private double lon;

    private boolean isFavorite = false;

    public PlaceModel(String name, String address, double rating, double lat, double lon) {
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.lat = lat;
        this.lon = lon;
    }

    // Getter & Setter cũ
    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getRating() { return rating; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }

    // ⭐ GETTER & SETTER CHO FAVORITE (HomeActivity đang gọi hàm này)
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}