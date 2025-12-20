package com.example.testproject1;

import java.io.Serializable;

public class TripModel implements Serializable {
    private String tripId;
    private String name;        // Tên chuyến đi (vd: Du lịch hè)
    private String startDate;   // Ngày bắt đầu
    private String endDate;     // Ngày kết thúc
    private String userId;      // ID của người dùng tạo chuyến đi
private String groupId;
private String sourceTripId; // trip gốc từ group



    // Constructor rỗng cho Firestore
    public TripModel() {}

    public TripModel(String tripId, String name, String startDate, String endDate, String userId) {
        this.tripId = tripId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
    }
    public TripModel(String tripId, String name, String startDate, String endDate, String userId,String groupId) {
        this.tripId = tripId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
        this.groupId=groupId;
    }

    // Getter & Setter
    public String getSourceTripId() {
        return sourceTripId;
    }

    public void setSourceTripId(String sourceTripId) {
        this.sourceTripId = sourceTripId;
    }
    public String getTripId() { return tripId; }
    public String getGroupId(){return groupId;}
    public void setGroupId(String gr){this.groupId=gr;}
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}