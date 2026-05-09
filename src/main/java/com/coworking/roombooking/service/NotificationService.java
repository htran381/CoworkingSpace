package com.coworking.roombooking.service;

import com.coworking.roombooking.model.Notification;
import com.coworking.roombooking.repository.roomRepo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * NotificationService handles all notification business logic.
 * Used by NotificationController (mock external service endpoint)
 * to persist notification records.
 */
@Service
public class NotificationService {

    private final roomRepo roomRepository;

    public NotificationService(roomRepo roomRepository) {
        this.roomRepository = roomRepository;
    }

    public int saveNotification(int customerId, int reservationId,
                                String email, String message, String status) {
        return roomRepository.saveNotification(
                customerId, reservationId, email, message, status);
    }

    public List<Notification> getAllNotifications() {
        return roomRepository.findAllNotifications();
    }
}
