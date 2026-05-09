package com.coworking.roombooking.model;

public class Room {
    private int roomId;
    private String roomName;
    private String roomType;
    private String location;
    private int capacity;
    private String status;


    public Room (){}

    public Room(int roomId, String roomName, String roomType, String location, int capacity, String status){
        this.roomId = roomId;
        this.roomName =roomName;
        this.roomType =roomType;
        this.location =location;
        this.capacity =capacity;
        this.status =status;
    }

    public int getRoomId() {return roomId;}
    public String getRoomName() {return roomName;}
    public String getRoomType() {return roomType;}
    public String getLocation() {return location;}
    public int getCapacity () {return capacity;}
    public String getStatus (){return status;}

    public void setRoomId(int roomId)         { this.roomId = roomId; }
    public void setRoomName(String roomName)  { this.roomName = roomName; }
    public void setRoomType(String roomType)  { this.roomType = roomType; }
    public void setLocation(String location)  { this.location = location; }
    public void setCapacity(int capacity)     { this.capacity = capacity; }
    public void setStatus(String status)      { this.status = status; }

}
