package com.example.testproject1.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatGroup {
    private String id;
    private String name;
    private String avatarUrl;
    private List<String> memberIds;
    private List<String> adminIds; // Danh sách admin của nhóm
    private String lastMessageContent;
    private String lastMessageSenderId;
    private String lastMessageSenderName;
    private Timestamp lastMessageTime;
    private Map<String, Integer> unreadCount; // userId -> count
    private Timestamp createdAt;
    private String createdBy;
    private boolean isArchived;
    private boolean isMuted;
    private boolean isPinned;
    private boolean isGroup; // true = nhóm chat, false = chat 1-1

    public ChatGroup() {
        // Required empty constructor for Firestore
        this.memberIds = new ArrayList<>();
        this.adminIds = new ArrayList<>();
        this.unreadCount = new HashMap<>();
    }

    public ChatGroup(String name, String createdBy, List<String> memberIds) {
        this.name = name;
        this.createdBy = createdBy;
        this.memberIds = memberIds;
        this.adminIds = new ArrayList<>();
        this.adminIds.add(createdBy); // Người tạo mặc định là admin
        this.createdAt = Timestamp.now();
        this.unreadCount = new HashMap<>();
        this.isArchived = false;
        this.isMuted = false;
        this.isPinned = false;
        this.isGroup = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public String getLastMessageContent() { return lastMessageContent; }
    public void setLastMessageContent(String lastMessageContent) { 
        this.lastMessageContent = lastMessageContent; 
    }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { 
        this.lastMessageSenderId = lastMessageSenderId; 
    }

    public String getLastMessageSenderName() { return lastMessageSenderName; }
    public void setLastMessageSenderName(String lastMessageSenderName) { 
        this.lastMessageSenderName = lastMessageSenderName; 
    }

    public Timestamp getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Timestamp lastMessageTime) { 
        this.lastMessageTime = lastMessageTime; 
    }

    public Map<String, Integer> getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Map<String, Integer> unreadCount) { 
        this.unreadCount = unreadCount; 
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public List<String> getAdminIds() { return adminIds; }
    public void setAdminIds(List<String> adminIds) { this.adminIds = adminIds; }

    public boolean isGroup() { return isGroup; }
    public void setGroup(boolean group) { isGroup = group; }

    // Kiểm tra user có phải admin không
    public boolean isAdmin(String userId) {
        return adminIds != null && adminIds.contains(userId);
    }

    // Kiểm tra user có phải người tạo không
    public boolean isCreator(String userId) {
        return createdBy != null && createdBy.equals(userId);
    }
}
