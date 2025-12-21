package com.example.travelmate.home;

import java.util.ArrayList;

public class CommentModel {

    private String id;                // Firestore document ID
    private String uid;               // UID người comment
    private String user;              // Tên hiển thị
    private String message;           // Nội dung comment
    private long timestamp;           // Thời gian
    private int rating;               // Số sao
    private ArrayList<String> imageUrls;  // Danh sách ảnh

    private int likeCount;            // Tổng like
    private int dislikeCount;         // Tổng dislike

    private ArrayList<String> likedBy;     // UID đã like
    private ArrayList<String> dislikedBy;  // UID đã dislike

    // --------------------------------------------------
    // 🔹 Constructor bắt buộc cho Firestore
    // --------------------------------------------------
    public CommentModel() {
        this.imageUrls = new ArrayList<>();
        this.likedBy = new ArrayList<>();
        this.dislikedBy = new ArrayList<>();
        this.likeCount = 0;
        this.dislikeCount = 0;
    }


    // --------------------------------------------------
    // 🔹 Constructor không có ảnh
    // --------------------------------------------------
    public CommentModel(String user, String message, long timestamp, int rating) {
        this(user, message, timestamp, rating, new ArrayList<>());
    }
    public CommentModel(String user, String uid, String message, long timestamp, int rating, ArrayList<String> imageUrls) {
        this.user = user;
        this.uid = uid;   // ⭐ quan trọng
        this.message = message;
        this.timestamp = timestamp;
        this.rating = rating;
        this.imageUrls = imageUrls;

        this.likedBy = new ArrayList<>();
        this.dislikedBy = new ArrayList<>();
        this.likeCount = 0;
        this.dislikeCount = 0;
    }

    // --------------------------------------------------
    // 🔹 Constructor đầy đủ
    // --------------------------------------------------
    public CommentModel(String user, String message, long timestamp, int rating, ArrayList<String> imageUrls) {
        this.user = user;
        this.message = message;
        this.timestamp = timestamp;
        this.rating = rating;

        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();

        this.likedBy = new ArrayList<>();
        this.dislikedBy = new ArrayList<>();
        this.likeCount = 0;
        this.dislikeCount = 0;
    }

    // --------------------------------------------------
    // 🔹 GETTER & SETTER
    // --------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public ArrayList<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(ArrayList<String> imageUrls) {
        this.imageUrls = (imageUrls != null ? imageUrls : new ArrayList<>());
    }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; }

    public ArrayList<String> getLikedBy() { return likedBy; }
    public void setLikedBy(ArrayList<String> likedBy) {
        this.likedBy = (likedBy != null ? likedBy : new ArrayList<>());
    }

    public ArrayList<String> getDislikedBy() { return dislikedBy; }
    public void setDislikedBy(ArrayList<String> dislikedBy) {
        this.dislikedBy = (dislikedBy != null ? dislikedBy : new ArrayList<>());
    }

}
