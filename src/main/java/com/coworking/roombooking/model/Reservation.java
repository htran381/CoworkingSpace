package com.coworking.roombooking.model;

public class Reservation {
    private int reservationId;
    private int customerId;
    private String customerName;
    private String customerEmail;
    private int roomId;
    private String roomName;
    private String location;
    private String date;
    private String startTime;
    private String endTime;
    private String status;
    private int version;

    public Reservation() {}

    public Reservation(int reservationId, int customerId, String customerName, String customerEmail,
                       int roomId, String roomName, String location,
                       String date, String startTime, String endTime, String status) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.roomId = roomId;
        this.roomName = roomName;
        this.location = location;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public int getReservationId()      { return reservationId; }
    public int getCustomerId()         {return customerId; }
    public String getCustomerName()    { return customerName; }
    public String getCustomerEmail()   { return customerEmail; }
    public int getRoomId()             { return roomId; }
    public String getRoomName()        { return roomName; }
    public String getLocation()        { return location; }
    public String getDate()            { return date; }
    public String getStartTime()       { return startTime; }
    public String getEndTime()         { return endTime; }
    public String getStatus()          { return status; }
    public int getVersion()             {return version;}

    public void setReservationId(int reservationId)      { this.reservationId = reservationId; }
    public void setCustomerId (int customerId)           { this.customerId = customerId; }
    public void setCustomerName(String customerName)     { this.customerName = customerName; }
    public void setCustomerEmail(String customerEmail)   { this.customerEmail = customerEmail; }
    public void setRoomId(int roomId)                    { this.roomId = roomId; }
    public void setRoomName(String roomName)             { this.roomName = roomName; }
    public void setLocation(String location)             { this.location = location; }
    public void setDate(String date)                     { this.date = date; }
    public void setStartTime(String startTime)           { this.startTime = startTime; }
    public void setEndTime(String endTime)               { this.endTime = endTime; }
    public void setStatus(String status)                 { this.status = status; }
    public void setVersion(int version)                 {this.version = version;}
}