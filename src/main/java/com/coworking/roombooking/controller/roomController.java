package com.coworking.roombooking.controller;

import com.coworking.roombooking.model.Reservation;
import com.coworking.roombooking.model.Room;
import com.coworking.roombooking.service.roomService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller Layer - handles HTTP requests only.
 * Uses Page Controller pattern: each method handles one page.
 * Delegates ALL logic to RoomService - no business logic.
 * 
 * Request Flow:
 * Browser → Controller → Service → Repository → PostgreSQL
 * Browser ← Thymeleaf Template ← Service ← Repository ← PostgreSQL
 * 
 */
@Controller
public class roomController {

    private final roomService roomService;

    public roomController(roomService roomService) {
        this.roomService = roomService;
    }

    // GET / -> Home page
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // GET /rooms -> View all rooms (with optional search)

    @GetMapping("/rooms")
    public String viewRooms(@RequestParam(required = false) String date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            Model model) {
        List<Room> rooms = roomService.getRoomsWithAvailability(date, startTime, endTime);
        model.addAttribute("rooms", rooms);
        model.addAttribute("searchDate", date);
        model.addAttribute("searchStart", startTime);
        model.addAttribute("searchEnd", endTime);
        return "rooms";
    }

    // GET /book --> booking form
    @GetMapping("/book")
    public String showBookForm(@RequestParam int roomId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            Model model) {
        model.addAttribute("roomId", roomId);
        model.addAttribute("date", date);
        model.addAttribute("startTime", startTime);
        model.addAttribute("endTime", endTime);
        return "book";
    }

    // POST /book -> Submit booking form -> save to PostgreSQL
    @PostMapping("/book")
    public String submitBooking(@RequestParam String name,
            @RequestParam String email,
            @RequestParam int roomId,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime,
            Model model) {
        try {
            Reservation reservation = roomService.makeReservation(
                    name, email, roomId, date, startTime, endTime);
            Room room = roomService.getRoomById(roomId);
            model.addAttribute("reservation", reservation);
            model.addAttribute("room", room);
            return "confirmation";
        } catch (RuntimeException e) {
            model.addAttribute("roomId", roomId);
            model.addAttribute("date", date);
            model.addAttribute("startTime", startTime);
            model.addAttribute("endTime", endTime);
            model.addAttribute("error", e.getMessage());
            return "book";
        }
    }

    // GET /reschedule -> show reschedule form
    @GetMapping("/reschedule")
    public String showRescheduleForm(@RequestParam int reservationId, Model model) {
        model.addAttribute("reservationId", reservationId);
        return "reschedule";
    }

    // POST /reschedule -> submit reschedule
    @PostMapping("/reschedule")
    public String submitReschedule(@RequestParam int reservationId,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime,
            Model model) {
        try {
            Reservation reservation = roomService.rescheduleReservation(
                    reservationId, date, startTime, endTime);
            Room room = roomService.getRoomById(reservation.getRoomId()); // ← missing
            model.addAttribute("reservation", reservation);
            model.addAttribute("room", room); // ← missing
            return "confirmation";
        } catch (RuntimeException e) {
            model.addAttribute("reservationId", reservationId);
            model.addAttribute("error", e.getMessage());
            return "reschedule";
        }
    }

    // GET /reservations → View all reservations and waitlist entries
    @GetMapping("/reservations")
    public String viewReservations(Model model) {
        model.addAttribute("reservations", roomService.getAllReservations());
        model.addAttribute("waitlistEntries", roomService.getAllWaitlistEntries());
        return "reservations";
    }

    // GET /cancel → Cancel a reservation (promotes next waiter automatically)
    @GetMapping("/cancel")
    public String cancelReservation(@RequestParam int reservationId) {
        roomService.cancelReservation(reservationId);
        return "redirect:/reservations";
    }

    // GET /cancel-waitlist → Remove a waitlist entry
    @GetMapping("/cancel-waitlist")
    public String cancelWaitlistEntry(@RequestParam int waitlistId) {
        roomService.cancelWaitlistEntry(waitlistId);
        return "redirect:/reservations";
    }

    // GET /waitlist → Show waitlist join form (date+time carried from rooms search)
    @GetMapping("/waitlist")
    public String showWaitlistForm(@RequestParam int roomId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            Model model) {
        Room room = roomService.getRoomById(roomId);
        if (room == null) {
            return "redirect:/rooms";
        }
        model.addAttribute("room", room);
        model.addAttribute("date", date);
        model.addAttribute("startTime", startTime);
        model.addAttribute("endTime", endTime);
        return "waitlist";
    }

    // POST /waitlist → Submit waitlist form
    @PostMapping("/waitlist")
    public String submitWaitlist(@RequestParam String name,
            @RequestParam String email,
            @RequestParam int roomId,
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime,
            Model model) {
        try {
            int position = roomService.joinWaitlist(name, email, roomId, date, startTime, endTime);
            Room room = roomService.getRoomById(roomId);
            model.addAttribute("room", room);
            model.addAttribute("position", position);
            model.addAttribute("customerName", name);
            model.addAttribute("date", date);
            model.addAttribute("startTime", to12Hour(startTime));
            model.addAttribute("endTime", to12Hour(endTime));
            return "waitlist-confirmation";
        } catch (RuntimeException e) {
            Room room = roomService.getRoomById(roomId);
            model.addAttribute("room", room);
            model.addAttribute("date", date);
            model.addAttribute("startTime", startTime);
            model.addAttribute("endTime", endTime);
            model.addAttribute("error", e.getMessage());
            return "waitlist";
        }
    }

    // Converts "HH:mm" to "h:mm AM/PM"
    private String to12Hour(String time) {
        if (time == null || time.isBlank())
            return time;
        try {
            LocalTime lt = LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm"));
            return lt.format(DateTimeFormatter.ofPattern("h:mm a"));
        } catch (Exception e) {
            return time;
        }
    }

    // GET /health → health check endpoint
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        try {
            int roomCount = roomService.getRoomCount();
            status.put("status", "UP");
            status.put("database", "UP");
            status.put("roomCount", roomCount);
            status.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("database", "UNREACHABLE");
            status.put("error", e.getMessage());
            status.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.status(503).body(status);
        }
    }
}