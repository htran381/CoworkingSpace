package com.coworking.roombooking.model;

public class WaitlistEntry {
    private int waitlistId;
    private int roomId;
    private String roomName;
    private String location;
    private String roomType;
    private String customerName;
    private String customerEmail;
    private String date;
    private String startTime;
    private String endTime;
    private int position;

    public WaitlistEntry() {}

    public int getWaitlistId()       { return waitlistId; }
    public int getRoomId()           { return roomId; }
    public String getRoomName()      { return roomName; }
    public String getLocation()      { return location; }
    public String getRoomType()      { return roomType; }
    public String getCustomerName()  { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getDate()          { return date; }
    public String getStartTime()     { return startTime; }
    public String getEndTime()       { return endTime; }
    public int getPosition()         { return position; }

    public void setWaitlistId(int waitlistId)           { this.waitlistId = waitlistId; }
    public void setRoomId(int roomId)                   { this.roomId = roomId; }
    public void setRoomName(String roomName)            { this.roomName = roomName; }
    public void setLocation(String location)            { this.location = location; }
    public void setRoomType(String roomType)            { this.roomType = roomType; }
    public void setCustomerName(String customerName)    { this.customerName = customerName; }
    public void setCustomerEmail(String customerEmail)  { this.customerEmail = customerEmail; }
    public void setDate(String date)                    { this.date = date; }
    public void setStartTime(String startTime)          { this.startTime = startTime; }
    public void setEndTime(String endTime)              { this.endTime = endTime; }
    public void setPosition(int position)               { this.position = position; }
}
