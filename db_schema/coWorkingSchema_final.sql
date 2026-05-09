-- CoWorking Space Room Booking System
-- Hannah Tran - CMPE172

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS Notification CASCADE;
DROP TABLE IF EXISTS Waitlist CASCADE;
DROP TABLE IF EXISTS Reservation CASCADE;
DROP TABLE IF EXISTS TimeSlot CASCADE;
DROP TABLE IF EXISTS Customer CASCADE;
DROP TABLE IF EXISTS Room CASCADE;
DROP TABLE IF EXISTS Admin CASCADE;

-- ROOM
-- RoomID (PK), RoomName (unique), RoomType,
-- Location, Capacity, Status

CREATE TABLE Room (
    RoomID      SERIAL          PRIMARY KEY,
    RoomName    VARCHAR(100)    NOT NULL UNIQUE,
    RoomType    VARCHAR(50)     NOT NULL,
    Location    VARCHAR(100),
    Capacity    INT             NOT NULL CHECK (Capacity > 0),
    Status      VARCHAR(20)     NOT NULL DEFAULT 'Available'
                                CHECK (Status IN ('Available', 'Occupied'))
);

-- CUSTOMER
-- CustomerID (PK), Name, Email (unique)

CREATE TABLE Customer (
    CustomerID  SERIAL          PRIMARY KEY,
    Name        VARCHAR(100)    NOT NULL,
    Email       VARCHAR(100)    NOT NULL UNIQUE
);

-- ADMIN
-- AdminID (PK), Name, Email (unique)
-- Represents a functional system role
-- Not directly linked to specific rooms

CREATE TABLE Admin (
    AdminID     SERIAL          PRIMARY KEY,
    Name        VARCHAR(100)    NOT NULL,
    Email       VARCHAR(100)    NOT NULL UNIQUE
);


-- TIMESLOT
-- TimeSlotID (PK), Date, StartTime, EndTime

CREATE TABLE TimeSlot (
    TimeSlotID  SERIAL          PRIMARY KEY,
    Date        DATE            NOT NULL,
    StartTime   TIME            NOT NULL,
    EndTime     TIME            NOT NULL,
    CHECK (EndTime > StartTime)
);


-- RESERVATION
-- ReservationID (PK)
-- FK: CustomerID → Customer
-- FK: RoomID → Room
-- FK: TimeSlotID → TimeSlot
-- Composite UNIQUE (RoomID, TimeSlotID) prevents double booking at DB level
-- Application code also checks before inserting for a clearer error message

CREATE TABLE Reservation (
    ReservationID   SERIAL          PRIMARY KEY,
    TimeSlotID      INT             NOT NULL REFERENCES TimeSlot(TimeSlotID),
    CustomerID      INT             NOT NULL REFERENCES Customer(CustomerID),
    RoomID          INT             NOT NULL REFERENCES Room(RoomID),
    Status          VARCHAR(20)     NOT NULL DEFAULT 'Confirmed'
                                    CHECK (Status IN ('Confirmed', 'Cancelled', 'Waitlist')),
    CreatedAt       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT             NOT NULL DEFAULT 0,
    UNIQUE (RoomID, TimeSlotID)     -- Core constraint: prevents double booking
);


-- WAITLIST
-- WaitlistID (PK)
-- FK: RoomID → Room
-- FK: CustomerID → Customer
-- FK: TimeSlotID → TimeSlot  
-- Tracks customers waiting for a fully booked room+timeslot

CREATE TABLE Waitlist (
    WaitlistID  SERIAL          PRIMARY KEY,
    RoomID      INT             NOT NULL REFERENCES Room(RoomID),
    CustomerID  INT             NOT NULL REFERENCES Customer(CustomerID),
    TimeSlotID  INT             NOT NULL REFERENCES TimeSlot(TimeSlotID),
    Position    INT             NOT NULL CHECK (Position > 0),
    AddedAt     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- NOTIFICATION
-- NotificationID (PK)
-- FK: CustomerID → Customer
-- FK: ReservationID → Reservation (nullable: waitlist notifications have no reservation yet)
-- Status tracks whether notification was SENT or FAILED
-- Total participation: every notification belongs to a customer

CREATE TABLE Notification (
    NotificationID  SERIAL          PRIMARY KEY,
    CustomerID      INT             NOT NULL REFERENCES Customer(CustomerID),
    ReservationID   INT             REFERENCES Reservation(ReservationID),
    Message         TEXT            NOT NULL,
    SentAt          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Status          VARCHAR(20)     NOT NULL DEFAULT 'SENT'
                                    CHECK (Status IN ('SENT', 'FAILED'))
);


-- SAMPLE DATA

-- Rooms
INSERT INTO Room (RoomName, RoomType, Location, Capacity, Status) VALUES
    ('Focus Room A',        'Office',     'Floor 2, Room 201', 1,  'Available'),
    ('Meeting Hub',         'Conference', 'Floor 3, Room 305', 8,  'Available'),
    ('Collaboration Space', 'Conference', 'Floor 4, Room 410', 12, 'Occupied'),
    ('Chill Lounge',        'Lounge',     'Floor 1, Room 105', 6,  'Available');

-- Admin
INSERT INTO Admin (Name, Email) VALUES
    ('Admin User', 'admin@cowork.com');

-- Customers
INSERT INTO Customer (Name, Email) VALUES
    ('John Doe',   'john@example.com'),
    ('Jane Smith', 'jane@example.com'),
    ('Mike Johnson', 'mike@example.com');

-- TimeSlots
INSERT INTO TimeSlot (Date, StartTime, EndTime) VALUES
    ('2026-03-15', '09:00', '11:00'),
    ('2026-03-16', '14:00', '16:00'),
    ('2026-03-17', '10:00', '12:00');

-- Reservations
INSERT INTO Reservation (TimeSlotID, CustomerID, RoomID, Status) VALUES
    (1, 1, 1, 'Confirmed'),
    (2, 2, 2, 'Confirmed'),
    (3, 3, 3, 'Waitlist');

-- Waitlist
INSERT INTO Waitlist (RoomID, CustomerID, TimeSlotID, Position) VALUES
    (3, 3, 3, 1);

-- Notifications (sample — status column now required)
INSERT INTO Notification (CustomerID, ReservationID, Message, Status) VALUES
    (1, 1, 'Your booking for Focus Room A on 2026-03-15 at 09:00 AM is confirmed.', 'SENT'),
    (2, 2, 'Your booking for Meeting Hub on 2026-03-16 at 02:00 PM is confirmed.', 'SENT'),
    (3, NULL, 'You are #1 on the waitlist for Collaboration Space on 2026-03-17 at 10:00 AM.', 'SENT');

