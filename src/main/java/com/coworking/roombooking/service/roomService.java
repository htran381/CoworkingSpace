package com.coworking.roombooking.service;

import com.coworking.roombooking.model.Room;
import com.coworking.roombooking.model.Reservation;
import com.coworking.roombooking.model.WaitlistEntry;
import com.coworking.roombooking.repository.roomRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Layer - contains all business logic.
 * Controller calls Service, Service calls Repository.
 * DOES NOT ACCESS Repository directly from Controller.
 */
@Service
public class roomService {

    private static final Logger log = LoggerFactory.getLogger(roomService.class);

    private final roomRepo roomRepository;
    private final NotificationClient notificationClient;

    public roomService(roomRepo roomRepository,
            NotificationClient notificationClient) {
        this.roomRepository = roomRepository;
        this.notificationClient = notificationClient;
    }

    // ──────────────────────────────────────────────
    // Room
    // ──────────────────────────────────────────────

    public List<Room> getAllRooms() {
        return roomRepository.findAllRooms();
    }

    public List<Room> getRoomsWithAvailability(String date, String startTime, String endTime) {
        if (date == null || date.isBlank() ||
                startTime == null || startTime.isBlank() ||
                endTime == null || endTime.isBlank()) {
            return roomRepository.findAllRooms();
        }
        return roomRepository.findRoomsWithAvailability(date, startTime, endTime);
    }

    public Room getRoomById(int roomId) {
        return roomRepository.findRoomById(roomId);
    }

    public int getRoomCount() {
        return roomRepository.findAllRooms().size();
    }

    // ──────────────────────────────────────────────
    // Reservation
    // ──────────────────────────────────────────────

    @Transactional
    public Reservation makeReservation(String customerName, String customerEmail,
            int roomId, String date,
            String startTime, String endTime) {

        log.info("Attempting reservation: customer={}, roomId={}, date={}, slot={}-{}",
                customerEmail, roomId, date, startTime, endTime);

        try {
            int customerId = roomRepository.saveCustomer(customerName, customerEmail);
            int timeSlotId = roomRepository.saveTimeSlot(date, startTime, endTime);
            long start = System.currentTimeMillis();
            Reservation reservation = roomRepository.saveReservation(
                    customerId, roomId, timeSlotId, "Confirmed");
            long latency = System.currentTimeMillis() - start;

            log.info("Reservation created: reservationId={}, customerId={}, roomId={}, date={}, slot={}-{}",
                    reservation.getReservationId(), customerId, roomId, date, startTime, endTime);
            log.info("Booking latency: {}ms, reservationId={}",
                    latency, reservation.getReservationId());
            log.info("Reservation created: reservationId={}, customerId={}, roomId={}, date={}, slot={}-{}",
                    reservation.getReservationId(), customerId, roomId, date, startTime, endTime);

            // Notify customer — non-blocking, booking succeeds even if this fails
            Room room = roomRepository.findRoomById(roomId);
            String notifStatus = notificationClient.sendBookingConfirmation(
                    customerId,
                    reservation.getReservationId(),
                    customerEmail,
                    room.getRoomName(),
                    date,
                    startTime);

            if ("FAILED".equals(notifStatus)) {
                log.warn("Notification failed for reservationId={}, customer={} — booking unaffected",
                        reservation.getReservationId(), customerEmail);
            }

            return reservation;

        } catch (RuntimeException e) {
            log.error("Reservation failed: roomId={}, date={}, slot={}-{}, error={}",
                    roomId, date, startTime, endTime, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Reservation rescheduleReservation(int reservationId, String newDate,
            String newStartTime, String newEndTime) {

        log.info("Rescheduling reservationId={} to date={}, slot={}-{}",
                reservationId, newDate, newStartTime, newEndTime);

        try {
            Reservation r = roomRepository.rescheduleReservation(
                    reservationId, newDate, newStartTime, newEndTime);
            log.info("Reschedule successful: reservationId={}, new slot={} {}-{}",
                    reservationId, newDate, newStartTime, newEndTime);
            return r;
        } catch (RuntimeException e) {
            log.error("Reschedule failed: reservationId={}, error={}", reservationId, e.getMessage());
            throw e;
        }
    }

    public List<Reservation> getAllReservations() {
        return roomRepository.findAllReservations();
    }

    /**
     * Cancels a reservation. If it was Confirmed, the next customer
     * on the waitlist is automatically promoted and notified.
     */
    @Transactional
    public boolean cancelReservation(int reservationId) {

        log.info("Cancelling reservationId={}", reservationId);

        int promotedReservationId = roomRepository.deleteReservationAndPromote(reservationId);

        if (promotedReservationId == -2) {
            log.warn("Cancel failed: reservationId={} not found", reservationId);
            return false;
        }

        log.info("Reservation cancelled: reservationId={}", reservationId);

        // promotedReservationId > 0 means someone was promoted from waitlist
        if (promotedReservationId > 0) {
            Reservation promoted = roomRepository.findReservationById(promotedReservationId);
            if (promoted != null) {
                log.info("Waitlist promotion: customerId={} promoted to reservationId={} for roomId={}",
                        promoted.getCustomerId(), promotedReservationId, promoted.getRoomId());

                String notifStatus = notificationClient.sendWaitlistPromotion(
                        promoted.getCustomerId(),
                        promotedReservationId,
                        promoted.getCustomerEmail(),
                        promoted.getRoomName(),
                        promoted.getDate(),
                        promoted.getStartTime());

                if ("FAILED".equals(notifStatus)) {
                    log.warn("Promotion notification failed for customerId={} — promotion still applied",
                            promoted.getCustomerId());
                }
            } else {
                log.error("Waitlist promotion failed: findReservationById returned null for promotedReservationId={}",
                        promotedReservationId);
            }
        }

        return true;
    }

    // ──────────────────────────────────────────────
    // Waitlist
    // ──────────────────────────────────────────────

    @Transactional
    public int joinWaitlist(String customerName, String customerEmail,
            int roomId, String date, String startTime, String endTime) {

        log.info("Customer joining waitlist: customer={}, roomId={}, date={}, slot={}-{}",
                customerEmail, roomId, date, startTime, endTime);

        int customerId = roomRepository.saveCustomer(customerName, customerEmail);
        int timeSlotId = roomRepository.saveTimeSlot(date, startTime, endTime);
        if (timeSlotId == 0) {
            throw new RuntimeException("Invalid date or time slot. Please try again.");
        }
        roomRepository.addToWaitlist(customerId, roomId, timeSlotId);
        int position = roomRepository.getWaitlistPosition(customerId, roomId, timeSlotId);

        log.info("Waitlist joined: customerId={}, roomId={}, timeslotId={}, position={}",
                customerId, roomId, timeSlotId, position);

        String notifStatus = notificationClient.sendWaitlistConfirmation(
                customerId, 0, customerEmail,
                roomRepository.findRoomById(roomId).getRoomName(),
                date, startTime, position);

        if ("FAILED".equals(notifStatus)) {
            log.warn("Waitlist notification failed for customerId={} — waitlist entry still saved",
                    customerId);
        }

        return position;
    }

    public List<WaitlistEntry> getAllWaitlistEntries() {
        return roomRepository.findAllWaitlistEntries();
    }

    @Transactional
    public boolean cancelWaitlistEntry(int waitlistId) {
        log.info("Cancelling waitlist entry: waitlistId={}", waitlistId);
        boolean result = roomRepository.deleteWaitlistEntry(waitlistId);
        if (result) {
            log.info("Waitlist entry cancelled: waitlistId={}", waitlistId);
        } else {
            log.warn("Waitlist cancel failed: waitlistId={} not found", waitlistId);
        }
        return result;
    }

    // ──────────────────────────────────────────────
    // Admin
    // ──────────────────────────────────────────────

    public void addRoom(String roomName, String roomType, String location,
            int capacity, String status) {
        log.info("Admin adding room: name={}, type={}, capacity={}", roomName, roomType, capacity);
        roomRepository.addRoom(roomName, roomType, location, capacity, status);
    }

    public boolean deleteRoom(int roomId) {
        log.info("Admin deleting room: roomId={}", roomId);
        return roomRepository.deleteRoom(roomId);
    }

    public boolean updateRoomStatus(int roomId, String status) {
        log.info("Admin updating room status: roomId={}, status={}", roomId, status);
        return roomRepository.updateRoomStatus(roomId, status);
    }
}