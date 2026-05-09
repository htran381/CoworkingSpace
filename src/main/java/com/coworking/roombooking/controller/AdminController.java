package com.coworking.roombooking.controller;

import com.coworking.roombooking.model.Reservation;
import com.coworking.roombooking.model.Room;
import com.coworking.roombooking.service.roomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminController — handles all admin pages and actions.
 * Protected by a simple session check (admin:admin).
 * No Spring Security required.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String SESSION_KEY    = "adminLoggedIn";

    private final roomService roomService;

    public AdminController(roomService roomService) {
        this.roomService = roomService;
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    // GET /admin → login page
    @GetMapping
    public String loginPage(HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute(SESSION_KEY))) {
            return "redirect:/admin/dashboard";
        }
        return "admin-login";
    }

    // POST /admin/login → check credentials
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
            session.setAttribute(SESSION_KEY, true);
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("error", "Invalid username or password.");
        return "admin-login";
    }

    // GET /admin/logout → clear session
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    // GET /admin/dashboard → main admin page
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/admin";

        List<Room> rooms             = roomService.getAllRooms();
        List<Reservation> reservations = roomService.getAllReservations();

        model.addAttribute("rooms",        rooms);
        model.addAttribute("reservations", reservations);
        return "admin-dashboard";
    }

    // ── Room Management ───────────────────────────────────────────────────────

    // POST /admin/rooms/add → add a new room
    @PostMapping("/rooms/add")
    public String addRoom(@RequestParam String roomName,
                          @RequestParam String roomType,
                          @RequestParam String location,
                          @RequestParam int capacity,
                          @RequestParam(defaultValue = "Available") String status,
                          HttpSession session,
                          Model model) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        try {
            roomService.addRoom(roomName, roomType, location, capacity, status);
        } catch (Exception e) {
            model.addAttribute("roomError", e.getMessage());
            model.addAttribute("rooms",        roomService.getAllRooms());
            model.addAttribute("reservations", roomService.getAllReservations());
            return "admin-dashboard";
        }
        return "redirect:/admin/dashboard";
    }

    // POST /admin/rooms/delete → delete a room
    @PostMapping("/rooms/delete")
    public String deleteRoom(@RequestParam int roomId,
                             HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        roomService.deleteRoom(roomId);
        return "redirect:/admin/dashboard";
    }

    // POST /admin/rooms/status → toggle room status
    @PostMapping("/rooms/status")
    public String updateRoomStatus(@RequestParam int roomId,
                                   @RequestParam String status,
                                   HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        roomService.updateRoomStatus(roomId, status);
        return "redirect:/admin/dashboard";
    }

    // ── Booking Management ────────────────────────────────────────────────────

    // POST /admin/bookings/cancel → cancel any reservation
    @PostMapping("/bookings/cancel")
    public String cancelBooking(@RequestParam int reservationId,
                                HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/admin";
        roomService.cancelReservation(reservationId);
        return "redirect:/admin/dashboard";
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean isLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(SESSION_KEY));
    }
}
