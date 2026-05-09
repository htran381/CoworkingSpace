package com.coworking.roombooking.service;

import org.springframework.stereotype.Component;


@Component
public class NotificationClient {

    private final NotificationService notificationService;

    public NotificationClient(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Sends a booking confirmation notification.
     * Returns "SENT" on success, "FAILED" on error.
     */
    public String sendBookingConfirmation(int customerId, int reservationId,
                                          String email, String roomName,
                                          String date, String startTime) {
        return send(customerId, reservationId, email,
                "Your booking for " + roomName + " on " + date +
                " at " + startTime + " is confirmed.");
    }

    /**
     * Sends a waitlist join confirmation notification.
     * Returns "SENT" on success, "FAILED" on error.
     */
    public String sendWaitlistConfirmation(int customerId, int reservationId,
                                           String email, String roomName,
                                           String date, String startTime,
                                           int position) {
        return send(customerId, reservationId, email,
                "You are #" + position + " on the waitlist for " +
                roomName + " on " + date + " at " + startTime + ".");
    }

    /**
     * Sends a waitlist promotion notification (customer moved to Confirmed).
     * Returns "SENT" on success, "FAILED" on error.
     */
    public String sendWaitlistPromotion(int customerId, int reservationId,
                                        String email, String roomName,
                                        String date, String startTime) {
        return send(customerId, reservationId, email,
                "Good news! A spot opened up. Your booking for " +
                roomName + " on " + date + " at " + startTime +
                " is now confirmed.");
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private String send(int customerId, int reservationId,
                        String email, String message) {
        try {
            notificationService.saveNotification(
                    customerId, reservationId, email, message, "SENT");
            System.out.println(">>> Notification SENT to: " + email + " | " + message);
            return "SENT";
        } catch (Exception e) {
            System.err.println(">>> Notification FAILED: " + e.getMessage());
            return "FAILED";
        }
    }
}