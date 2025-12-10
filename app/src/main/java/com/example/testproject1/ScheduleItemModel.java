package com.example.testproject1;

import java.io.Serializable;

public class ScheduleItemModel implements Serializable {
    private String itemId;      // ID duy nhất của mục này
    private String tripId;      // Thuộc về chuyến đi nào

    // Thông tin địa điểm (Lưu lại để không phải query nhiều lần)
    private String placeId;
    private String placeName;
    private String placeAddress;
    private double lat;
    private double lon;

    private String visitDate;   // Ngày đi thăm (vd: 12/05/2024)
    private String visitTime;   // Giờ đi thăm (vd: 09:00)
    private String note;        // Ghi chú

    public ScheduleItemModel() {}

    // Constructor tiện lợi để tạo từ PlaceModel
    public ScheduleItemModel(String itemId, String tripId, PlaceModel place, String visitDate, String visitTime) {
        this.itemId = itemId;
        this.tripId = tripId;
        this.placeId = place.getId();
        this.placeName = place.getName();
        this.placeAddress = place.getAddress();
        this.lat = place.getLat();
        this.lon = place.getLon();
        this.visitDate = visitDate;
        this.visitTime = visitTime;
    }

    // Getter & Setter
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public String getPlaceAddress() { return placeAddress; }
    public void setPlaceAddress(String placeAddress) { this.placeAddress = placeAddress; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }

    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }

    public String getVisitTime() { return visitTime; }
    public void setVisitTime(String visitTime) { this.visitTime = visitTime; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}