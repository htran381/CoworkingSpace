package com.coworking.roombooking.controller;

import com.coworking.roombooking.model.Notification;
import com.coworking.roombooking.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NotificationController 
 *
 * 1. POST /notifications/send  — pretends to be the external notification
 *    service. roomController calls this via NotificationClient (RestTemplate).
 *
 * 2. GET /notifications — UI page showing the Notification table so you can
 *    verify every notification was sent without opening a DB client.
 */
@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ── Mock external service endpoint ───────────────────────────────────────

    /**
     * POST /notifications/send
     * Called by NotificationClient inside the main app.
     * Saves a row to the Notification table and returns { notificationId, status, sentTo }.
     */
    @PostMapping("/notifications/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendNotification(
            @RequestBody Map<String, Object> payload) {

        int customerId    = (int) payload.get("customerId");
        int reservationId = (int) payload.get("reservationId");
        String email      = (String) payload.get("email");
        String message    = (String) payload.get("message");

        int notificationId = notificationService.saveNotification(
                customerId, reservationId, email, message, "SENT");

        Map<String, Object> response = new HashMap<>();
        response.put("notificationId", notificationId);
        response.put("status",         "SENT");
        response.put("sentTo",         email);
        return ResponseEntity.ok(response);
    }

    // ── UI page ───────────────────────────────────────────────────────────────

    /**
     * GET /notifications
     * Shows all notifications with their SENT/FAILED status.
     */
    @GetMapping("/notifications")
    public String viewNotifications(Model model) {
        List<Notification> notifications = notificationService.getAllNotifications();
        model.addAttribute("notifications", notifications);
        return "notifications";
    }
}
