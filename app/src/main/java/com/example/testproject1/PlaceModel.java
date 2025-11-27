package com.example.testproject1;

public class PlaceModel {
    private String id;
    private String name;
    private String address;
    private double rating; // Giả lập
    private double lat;
    private double lon;

    // ⚠️ Firestore cần constructor rỗng
    public PlaceModel() {
    }

    // Constructor bạn đang dùng trong HomeActivity
    public PlaceModel(String name, String address, double rating, double lat, double lon) {
        // Tạo id duy nhất dựa trên lat + lon
        this.id = lat + "_" + lon;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.lat = lat;
        this.lon = lon;
    }

    // Getter & Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {   // để Firestore có thể set khi đọc
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) { this.address = address; }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) { this.rating = rating; }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) { this.lat = lat; }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) { this.lon = lon; }
}
