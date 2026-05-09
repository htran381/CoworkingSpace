package com.coworking.roombooking.model;

public class Notification {
    private int notificationId;
    private int customerId;
    private int reservationId;
    private String customerEmail;
    private String message;
    private String sentAt;
    private String status; // "SENT" or "FAILED"

    public Notification() {}

    public int getNotificationId()     { return notificationId; }
    public int getCustomerId()         { return customerId; }
    public int getReservationId()      { return reservationId; }
    public String getCustomerEmail()   { return customerEmail; }
    public String getMessage()         { return message; }
    public String getSentAt()          { return sentAt; }
    public String getStatus()          { return status; }

    public void setNotificationId(int notificationId)       { this.notificationId = notificationId; }
    public void setCustomerId(int customerId)               { this.customerId = customerId; }
    public void setReservationId(int reservationId)         { this.reservationId = reservationId; }
    public void setCustomerEmail(String customerEmail)      { this.customerEmail = customerEmail; }
    public void setMessage(String message)                  { this.message = message; }
    public void setSentAt(String sentAt)                    { this.sentAt = sentAt; }
    public void setStatus(String status)                    { this.status = status; }
}
