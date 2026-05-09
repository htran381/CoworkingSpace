package com.coworking.roombooking.repository;

import com.coworking.roombooking.model.Room;
import com.coworking.roombooking.model.Reservation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import com.coworking.roombooking.model.WaitlistEntry;
import org.springframework.stereotype.Repository;
import com.coworking.roombooking.model.Notification;

import java.util.List;

/**
 * Repository Layer= handles all database access via JDBC.
 * only SQL queries.
 * Controller → Service → Repository → PostgreSQL
 */
@Repository
public class roomRepo {

        private final JdbcTemplate jdbcTemplate;

        public roomRepo(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        // RowMapper for Room --> convert db data into java Room object
        private final RowMapper<Room> roomMapper = (rs, rowNum) -> {
                Room room = new Room();
                room.setRoomId(rs.getInt("roomid"));
                room.setRoomName(rs.getString("roomname"));
                room.setRoomType(rs.getString("roomtype"));
                room.setLocation(rs.getString("location"));
                room.setCapacity(rs.getInt("capacity"));
                room.setStatus(rs.getString("status"));
                return room;
        };

        // RowMapper for Reservation --> convert db data into java Room object
        private final RowMapper<Reservation> reservationMapper = (rs, rowNum) -> {
                Reservation res = new Reservation();
                res.setReservationId(rs.getInt("reservationid"));
                res.setCustomerId(rs.getInt("customerid"));
                res.setCustomerName(rs.getString("name"));
                res.setCustomerEmail(rs.getString("email"));
                res.setRoomId(rs.getInt("roomid"));
                res.setRoomName(rs.getString("roomname"));
                res.setLocation(rs.getString("location"));
                res.setDate(rs.getString("date"));
                res.setStartTime(rs.getString("starttime"));
                res.setEndTime(rs.getString("endtime"));
                res.setStatus(rs.getString("status"));
                return res;
        };

        // Query rooms

        public List<Room> findAllRooms() {
                String sql = "SELECT * FROM Room ORDER BY roomid";
                return jdbcTemplate.query(sql, roomMapper);
        }

        /**
         * Returns all rooms, setting status to "Available" or "Occupied"
         * based on whether a Confirmed reservation already occupies the
         * requested date/time window. A room is occupied when ANY confirmed
         * reservation's time slot overlaps the requested [startTime, endTime)
         * on the given date.
         */
        public List<Room> findRoomsWithAvailability(String date, String startTime, String endTime) {
                String sql = """
                                SELECT r.*,
                                       CASE
                                         WHEN EXISTS (
                                           SELECT 1
                                           FROM Reservation res
                                           JOIN TimeSlot ts ON res.timeslotid = ts.timeslotid
                                           WHERE res.roomid = r.roomid
                                             AND res.status = 'Confirmed'
                                             AND ts.date = ?::date
                                             AND ts.starttime < ?::time
                                             AND ts.endtime   > ?::time
                                         ) THEN 'Occupied'
                                         ELSE 'Available'
                                       END AS status
                                FROM Room r
                                ORDER BY r.roomid
                                """;
                return jdbcTemplate.query(sql, roomMapper, date, endTime, startTime);
        }

        public Room findRoomById(int roomId) {
                String sql = "SELECT * FROM Room WHERE roomid = ?";
                List<Room> rooms = jdbcTemplate.query(sql, roomMapper, roomId);
                return rooms.isEmpty() ? null : rooms.get(0);
        }

        // Reservation Queries

        public List<Reservation> findAllReservations() {
                String sql = """
                                SELECT r.reservationid, r.status, r.customerid,
                                       c.name, c.email,
                                       rm.roomid, rm.roomname, rm.location,
                                       ts.date::text, TO_CHAR(ts.starttime, 'HH:MI AM') as starttime,
                                       TO_CHAR(ts.endtime, 'HH:MI AM') as endtime
                                FROM Reservation r
                                JOIN Customer c ON r.customerid = c.customerid
                                JOIN Room rm ON r.roomid = rm.roomid
                                JOIN TimeSlot ts ON r.timeslotid = ts.timeslotid
                                ORDER BY r.reservationid
                                """;
                return jdbcTemplate.query(sql, reservationMapper);
        }

        public int saveCustomer(String name, String email) {
                String checkSql = "SELECT customerid FROM Customer WHERE email = ?";
                List<Integer> ids = jdbcTemplate.query(checkSql,
                                (rs, rowNum) -> rs.getInt("customerid"), email);

                if (!ids.isEmpty()) {
                        // Update name in case it changed
                        String updateSql = "UPDATE Customer SET name = ? WHERE customerid = ?";
                        jdbcTemplate.update(updateSql, name, ids.get(0));
                        return ids.get(0);
                }

                String sql = "INSERT INTO Customer (name, email) VALUES (?, ?) RETURNING customerid";
                Integer id = jdbcTemplate.queryForObject(sql, Integer.class, name, email);
                return id != null ? id : 0;
        }

        public int saveTimeSlot(String date, String startTime, String endTime) {
                String checkSql = """
                                SELECT timeslotid FROM TimeSlot
                                WHERE date = ?::date AND starttime = ?::time AND endtime = ?::time
                                """;
                List<Integer> ids = jdbcTemplate.query(checkSql,
                                (rs, rowNum) -> rs.getInt("timeslotid"), date, startTime, endTime);
                if (!ids.isEmpty())
                        return ids.get(0);

                String sql = """
                                INSERT INTO TimeSlot (date, starttime, endtime)
                                VALUES (?::date, ?::time, ?::time)
                                RETURNING timeslotid
                                """;
                Integer id = jdbcTemplate.queryForObject(sql, Integer.class, date, startTime, endTime);
                return id != null ? id : 0;
        }

        public Reservation saveReservation(int customerId, int roomId,
                        int timeSlotId, String status) {
                String checkSQL = """
                                SELECT COUNT(*) FROM Reservation
                                WHERE roomid = ? and timeslotid = ? AND status = 'Confirmed'
                                """;
                Integer count = jdbcTemplate.queryForObject(checkSQL, Integer.class, roomId, timeSlotId);
                if (count != null && count > 0) {
                        throw new RuntimeException("Room is already booked for this time slot.");
                }
                String sql = """
                                INSERT INTO Reservation (timeslotid, customerid, roomid, status)
                                VALUES (?, ?, ?, ?)
                                RETURNING reservationid
                                """;
                Integer reservationId = jdbcTemplate.queryForObject(
                                sql, Integer.class, timeSlotId, customerId, roomId, status);
                if (reservationId == null)
                        throw new RuntimeException("Failed to create reservation");

                // Fetch the full reservation to return
                String fetchSql = """
                                SELECT r.reservationid, r.status, r.customerid,
                                       c.name, c.email,
                                       rm.roomid, rm.roomname, rm.location,
                                       ts.date::text, TO_CHAR(ts.starttime, 'HH:MI AM') as starttime,
                                       TO_CHAR(ts.endtime, 'HH:MI AM') as endtime
                                FROM Reservation r
                                JOIN Customer c ON r.customerid = c.customerid
                                JOIN Room rm ON r.roomid = rm.roomid
                                JOIN TimeSlot ts ON r.timeslotid = ts.timeslotid
                                WHERE r.reservationid = ?
                                """;
                return jdbcTemplate.queryForObject(fetchSql, reservationMapper, reservationId);
        }

        public boolean deleteReservation(int reservationId) {
                String sql = "DELETE FROM Reservation WHERE reservationid = ?";
                return jdbcTemplate.update(sql, reservationId) > 0;
        }

        public Reservation rescheduleReservation(int reservationId, String newDate,
                        String newStartTime, String newEndTime) {
                // Fetch current version
                String versionSql = "SELECT version, roomid FROM Reservation WHERE reservationid = ?";
                List<int[]> rows = jdbcTemplate.query(versionSql,
                                (rs, rowNum) -> new int[] { rs.getInt("version"), rs.getInt("roomid") }, reservationId);

                if (rows.isEmpty())
                        throw new RuntimeException("Reservation not found.");
                int currentVersion = rows.get(0)[0];
                int roomId = rows.get(0)[1];

                // Save or find the new time slot
                int newTimeSlotId = saveTimeSlot(newDate, newStartTime, newEndTime);

                // Check new slot is not already taken
                String checkSql = """
                                SELECT COUNT(*) FROM Reservation
                                WHERE roomid = ? AND timeslotid = ? AND status = 'Confirmed'
                                AND reservationid != ?
                                """;
                Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class,
                                roomId, newTimeSlotId, reservationId);
                if (count != null && count > 0)
                        throw new RuntimeException("Room is already booked for the requested time slot.");

                // Update with optimistic locking: only succeeds if version has not changed
                String updateSql = """
                                UPDATE Reservation
                                SET timeslotid = ?, version = version + 1
                                WHERE reservationid = ? AND version = ?
                                """;
                int rowsUpdated = jdbcTemplate.update(updateSql, newTimeSlotId, reservationId, currentVersion);

                if (rowsUpdated == 0)
                        throw new RuntimeException("Reservation was modified by another request. Please try again.");

                // Return updated reservation
                String fetchSql = """
                                SELECT r.reservationid, r.status, r.customerid,
                                       c.name, c.email,
                                       rm.roomid, rm.roomname, rm.location,
                                       ts.date::text,
                                       TO_CHAR(ts.starttime, 'HH:MI AM') as starttime,
                                       TO_CHAR(ts.endtime, 'HH:MI AM') as endtime
                                FROM Reservation r
                                JOIN Customer c  ON r.customerid = c.customerid
                                JOIN Room rm     ON r.roomid = rm.roomid
                                JOIN TimeSlot ts ON r.timeslotid = ts.timeslotid
                                WHERE r.reservationid = ?
                                """;
                return jdbcTemplate.queryForObject(fetchSql, reservationMapper, reservationId);
        }

        // ──────────────────────────────────────────────
        // Waitlist Queries
        // ──────────────────────────────────────────────

        // RowMapper for WaitlistEntry
        private final RowMapper<WaitlistEntry> waitlistEntryMapper = (rs, rowNum) -> {
                WaitlistEntry e = new WaitlistEntry();
                e.setWaitlistId(rs.getInt("waitlistid"));
                e.setRoomId(rs.getInt("roomid"));
                e.setRoomName(rs.getString("roomname"));
                e.setLocation(rs.getString("location"));
                e.setRoomType(rs.getString("roomtype"));
                e.setCustomerName(rs.getString("name"));
                e.setCustomerEmail(rs.getString("email"));
                e.setDate(rs.getString("date"));
                e.setStartTime(rs.getString("starttime"));
                e.setEndTime(rs.getString("endtime"));
                e.setPosition(rs.getInt("position"));
                return e;
        };

        /**
         * Adds a customer to the waitlist for a specific room + timeslot.
         * Position is scoped to (roomid, timeslotid) — each slot has its own queue.
         * No if the customer is already waiting for that exact room+slot.
         */
        public void addToWaitlist(int customerId, int roomId, int timeSlotId) {
                // don't add duplicates for the same room+timeslot
                String checkSql = """
                                SELECT COUNT(*) FROM Waitlist
                                WHERE customerid = ? AND roomid = ? AND timeslotid = ?
                                """;
                Integer existing = jdbcTemplate.queryForObject(checkSql, Integer.class,
                                customerId, roomId, timeSlotId);
                if (existing != null && existing > 0) {
                        return; // already on waitlist for this slot
                }

                String sql = """
                                INSERT INTO Waitlist (roomid, customerid, timeslotid, position, addedat)
                                VALUES (?, ?, ?, (
                                    SELECT COALESCE(MAX(position), 0) + 1
                                    FROM Waitlist WHERE roomid = ? AND timeslotid = ?
                                ), NOW())
                                """;
                jdbcTemplate.update(sql, roomId, customerId, timeSlotId, roomId, timeSlotId);
        }

        /**
         * Returns all waitlist entries joined with room and timeslot details.
         */
        public List<WaitlistEntry> findAllWaitlistEntries() {
                String sql = """
                                SELECT w.waitlistid, w.position,
                                       r.roomid, r.roomname, r.location, r.roomtype,
                                       c.name, c.email,
                                       ts.date::text,
                                       TO_CHAR(ts.starttime, 'HH:MI AM') as starttime,
                                       TO_CHAR(ts.endtime,   'HH:MI AM') as endtime
                                FROM Waitlist w
                                JOIN Customer c  ON w.customerid  = c.customerid
                                JOIN Room r      ON w.roomid       = r.roomid
                                JOIN TimeSlot ts ON w.timeslotid   = ts.timeslotid
                                ORDER BY w.addedat
                                """;
                return jdbcTemplate.query(sql, waitlistEntryMapper);
        }

        /**
         * Returns the 1-based queue position of a customer for a specific
         * room+timeslot,
         * or -1 if they are not on it.
         */
        public int getWaitlistPosition(int customerId, int roomId, int timeSlotId) {
                String sql = """
                                SELECT position FROM Waitlist
                                WHERE customerid = ? AND roomid = ? AND timeslotid = ?
                                """;
                List<Integer> positions = jdbcTemplate.query(sql,
                                (rs, rowNum) -> rs.getInt("position"), customerId, roomId, timeSlotId);
                return positions.isEmpty() ? -1 : positions.get(0);
        }

        /**
         * Removes a specific waitlist entry by ID, then shifts remaining
         * positions down by 1 for that same room+timeslot queue.
         */
        public boolean deleteWaitlistEntry(int waitlistId) {
                // Fetch roomid, timeslotid, and position before deleting
                String lookupSql = """
                                SELECT roomid, timeslotid, position FROM Waitlist
                                WHERE waitlistid = ?
                                """;
                List<int[]> rows = jdbcTemplate.query(lookupSql,
                                (rs, rowNum) -> new int[] {
                                                rs.getInt("roomid"),
                                                rs.getInt("timeslotid"),
                                                rs.getInt("position")
                                }, waitlistId);

                if (rows.isEmpty())
                        return false;

                int roomId = rows.get(0)[0];
                int timeSlotId = rows.get(0)[1];
                int position = rows.get(0)[2];

                String deleteSql = "DELETE FROM Waitlist WHERE waitlistid = ?";
                int affected = jdbcTemplate.update(deleteSql, waitlistId);
                if (affected == 0)
                        return false;

                // Shift everyone behind this entry up
                String shiftSql = """
                                UPDATE Waitlist SET position = position - 1
                                WHERE roomid = ? AND timeslotid = ? AND position > ?
                                """;
                jdbcTemplate.update(shiftSql, roomId, timeSlotId, position);
                return true;
        }

        /**
         * When a Confirmed reservation is cancelled, find the first customer waiting
         * for that exact room+timeslot, promote them to a Confirmed reservation,
         * remove them from the waitlist, and shift remaining positions down by 1
         * Returns the new reservationId, or -1 if nobody was waiting
         */
        public int promoteFromWaitlist(int roomId, int timeSlotId) {
                // Find the customer at position 1 for this specific room+timeslot
                // fetch their email so the service layer can notify them
                String findSql = """
                                SELECT w.customerid, c.email FROM Waitlist w
                                JOIN Customer c ON w.customerid = c.customerid
                                WHERE w.roomid = ? AND w.timeslotid = ?
                                ORDER BY w.position ASC
                                LIMIT 1
                                """;
                List<Object[]> candidates = jdbcTemplate.query(findSql,
                                (rs, rowNum) -> new Object[] {
                                                rs.getInt("customerid"),
                                                rs.getString("email")
                                }, roomId, timeSlotId);

                if (candidates.isEmpty())
                        return -1; // nobody waiting for this slot

                int nextCustomerId = (int) candidates.get(0)[0];

                // Promote: create a Confirmed reservation and return its ID
                String insertSql = """
                                INSERT INTO Reservation (timeslotid, customerid, roomid, status)
                                VALUES (?, ?, ?, 'Confirmed')
                                RETURNING reservationid
                                """;
                Integer newReservationId = jdbcTemplate.queryForObject(
                                insertSql, Integer.class, timeSlotId, nextCustomerId, roomId);

                // Remove them from the waitlist
                String deleteSql = """
                                DELETE FROM Waitlist
                                WHERE customerid = ? AND roomid = ? AND timeslotid = ?
                                """;
                jdbcTemplate.update(deleteSql, nextCustomerId, roomId, timeSlotId);

                // Shift remaining waiters for this room+timeslot down by 1
                String shiftSql = """
                                UPDATE Waitlist SET position = position - 1
                                WHERE roomid = ? AND timeslotid = ? AND position > 1
                                """;
                jdbcTemplate.update(shiftSql, roomId, timeSlotId);

                return newReservationId != null ? newReservationId : -1;
        }

        /**
         * Cancels a reservation and promotes the next waiter for that exact
         * room+timeslot if the cancelled reservation was Confirmed.
         * Returns the new promoted reservationId, -1 if no promotion, -2 if not found.
         */
        public int deleteReservationAndPromote(int reservationId) {
                String lookupSql = """
                                SELECT roomid, timeslotid, status FROM Reservation
                                WHERE reservationid = ?
                                """;
                List<int[]> rows = jdbcTemplate.query(lookupSql,
                                (rs, rowNum) -> new int[] {
                                                rs.getInt("roomid"),
                                                rs.getInt("timeslotid"),
                                                "Confirmed".equals(rs.getString("status")) ? 1 : 0
                                }, reservationId);

                if (rows.isEmpty())
                        return -2;

                int roomId = rows.get(0)[0];
                int timeSlotId = rows.get(0)[1];
                boolean wasConfirmed = rows.get(0)[2] == 1;

                String deleteNotifSql = "DELETE FROM Notification WHERE reservationid = ?";
                jdbcTemplate.update(deleteNotifSql, reservationId);

                String deleteSql = "DELETE FROM Reservation WHERE reservationid = ?";
                int affected = jdbcTemplate.update(deleteSql, reservationId);
                if (affected == 0)
                        return -2;

                if (wasConfirmed) {
                        return promoteFromWaitlist(roomId, timeSlotId);
                }
                return -1;
        }

        /**
         * Fetches a single reservation by ID — used after promotion to get
         * the promoted customer's details for notification.
         */
        public Reservation findReservationById(int reservationId) {
                String sql = """
                                SELECT r.reservationid, r.status, r.customerid,
                                       c.name, c.email,
                                       rm.roomid, rm.roomname, rm.location,
                                       ts.date::text,
                                       TO_CHAR(ts.starttime, 'HH:MI AM') as starttime,
                                       TO_CHAR(ts.endtime,   'HH:MI AM') as endtime
                                FROM Reservation r
                                JOIN Customer c  ON r.customerid  = c.customerid
                                JOIN Room rm     ON r.roomid       = rm.roomid
                                JOIN TimeSlot ts ON r.timeslotid   = ts.timeslotid
                                WHERE r.reservationid = ?
                                """;
                List<Reservation> results = jdbcTemplate.query(sql, reservationMapper, reservationId);
                return results.isEmpty() ? null : results.get(0);
        }

        // ──────────────────────────────────────────────
        // Notification Queries
        // ──────────────────────────────────────────────

        private final RowMapper<Notification> notificationMapper = (rs, rowNum) -> {
                Notification n = new Notification();
                n.setNotificationId(rs.getInt("notificationid"));
                n.setCustomerId(rs.getInt("customerid"));
                n.setReservationId(rs.getInt("reservationid"));
                n.setCustomerEmail(rs.getString("email"));
                n.setMessage(rs.getString("message"));
                n.setSentAt(rs.getString("sentat"));
                n.setStatus(rs.getString("status"));
                return n;
        };

        public int saveNotification(int customerId, int reservationId,
                        String email, String message, String status) {
                String sql = """
                                INSERT INTO Notification (customerid, reservationid, message, sentat, status)
                                VALUES (?, ?, ?, NOW(), ?)
                                RETURNING notificationid
                                """;
                Integer id = jdbcTemplate.queryForObject(
                                sql, Integer.class, customerId,
                                reservationId == 0 ? null : reservationId,
                                message, status);
                return id != null ? id : 0;
        }

        public List<Notification> findAllNotifications() {
                String sql = """
                                SELECT n.notificationid, n.customerid, n.reservationid,
                                       n.message, n.status,
                                       TO_CHAR(n.sentat, 'YYYY-MM-DD HH:MI AM') as sentat,
                                       c.email
                                FROM Notification n
                                JOIN Customer c ON n.customerid = c.customerid
                                ORDER BY n.sentat DESC
                                """;
                return jdbcTemplate.query(sql, notificationMapper);
        }
        // ──────────────────────────────────────────────
        // Admin Room Management
        // ──────────────────────────────────────────────

        public void addRoom(String roomName, String roomType, String location,
                        int capacity, String status) {
                String sql = """
                                INSERT INTO Room (roomname, roomtype, location, capacity, status)
                                VALUES (?, ?, ?, ?, ?)
                                """;
                jdbcTemplate.update(sql, roomName, roomType, location, capacity, status);
        }

        public boolean deleteRoom(int roomId) {
                String sql = "DELETE FROM Room WHERE roomid = ?";
                return jdbcTemplate.update(sql, roomId) > 0;
        }

        public boolean updateRoomStatus(int roomId, String status) {
                String sql = "UPDATE Room SET status = ? WHERE roomid = ?";
                return jdbcTemplate.update(sql, status, roomId) > 0;
        }
}
