package com.example.testproject1.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Message {
    private String id;
    private String groupId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String type; // text, image, file, location, system
    private Timestamp timestamp;
    private String replyToId;
    private String replyToContent;
    private String replyToSenderName;
    private Map<String, String> reactions; // userId -> emoji
    private List<String> readBy;
    private String imageUrl;
    private String fileName;
    private String fileUrl;
    private double latitude;
    private double longitude;
    private String locationName;

    // Edit/Delete status
    private boolean isEdited;
    private Timestamp editedAt;
    private String originalContent;
    private boolean isDeleted;
    private String action;    // "OPEN_TRIP"
    private String actionId;  // tripId

    public Message() {
        // Required empty constructor for Firestore
        this.reactions = new HashMap<>();
        this.readBy = new ArrayList<>();
        this.isEdited = false;
        this.isDeleted = false;
    }

    public Message(String groupId, String senderId, String senderName, String senderAvatar, 
                   String content, String type) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
        this.content = content;
        this.type = type;
        this.timestamp = Timestamp.now();
        this.reactions = new HashMap<>();
        this.readBy = new ArrayList<>();
        this.isEdited = false;
        this.isDeleted = false;
    }
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getReplyToId() { return replyToId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }

    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }

    public String getReplyToSenderName() { return replyToSenderName; }
    public void setReplyToSenderName(String replyToSenderName) { this.replyToSenderName = replyToSenderName; }

    public Map<String, String> getReactions() { return reactions; }
    public void setReactions(Map<String, String> reactions) { this.reactions = reactions; }

    public List<String> getReadBy() { return readBy; }
    public void setReadBy(List<String> readBy) { this.readBy = readBy; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public Timestamp getEditedAt() { return editedAt; }
    public void setEditedAt(Timestamp editedAt) { this.editedAt = editedAt; }

    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}
