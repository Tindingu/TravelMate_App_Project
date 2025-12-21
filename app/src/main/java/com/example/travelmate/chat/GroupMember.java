package com.example.travelmate.chat;

import com.google.firebase.Timestamp;

public class GroupMember {
    private String userId;
    private String userName;
    private String userAvatar;
    private String role; // admin, member
    private Timestamp joinedAt;
    private boolean isActive;

    public GroupMember() {
        // Required empty constructor for Firestore
    }

    public GroupMember(String userId, String userName, String userAvatar, String role) {
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.role = role;
        this.joinedAt = Timestamp.now();
        this.isActive = true;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
