package com.example.testproject1;

import java.util.ArrayList;

public class CommentModel {

    private String user;
    private String message;
    private long timestamp;
    private int rating;
    private ArrayList<String> imageUrls;   // <--- Nhiều ảnh

    public CommentModel() {}

    public CommentModel(String user, String message, long timestamp, int rating) {
        this.user = user;
        this.message = message;
        this.timestamp = timestamp;
        this.rating = rating;
        this.imageUrls = new ArrayList<>();
    }

    public CommentModel(String user, String message, long timestamp, int rating, ArrayList<String> imageUrls) {
        this.user = user;
        this.message = message;
        this.timestamp = timestamp;
        this.rating = rating;
        this.imageUrls = imageUrls;
    }

    public String getUser() { return user; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public int getRating() { return rating; }
    public ArrayList<String> getImageUrls() { return imageUrls; }
}
