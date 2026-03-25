package com.mindflow.security.messagecenter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "booking_events")
public class BookingEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 120)
    private String username;

    @Column(name = "route_number", nullable = false, length = 40)
    private String routeNumber;

    @Column(name = "passenger_phone_token", nullable = false, length = 120)
    private String passengerPhoneToken;

    @Column(name = "passenger_id_card_token", nullable = false, length = 120)
    private String passengerIdCardToken;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "reservation_success_sent", nullable = false)
    private boolean reservationSuccessSent;

    @Column(name = "arrival_reminder_sent", nullable = false)
    private boolean arrivalReminderSent;

    @Column(name = "missed_check_in_sent", nullable = false)
    private boolean missedCheckInSent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRouteNumber() {
        return routeNumber;
    }

    public void setRouteNumber(String routeNumber) {
        this.routeNumber = routeNumber;
    }

    public String getPassengerPhoneToken() {
        return passengerPhoneToken;
    }

    public void setPassengerPhoneToken(String passengerPhoneToken) {
        this.passengerPhoneToken = passengerPhoneToken;
    }

    public String getPassengerIdCardToken() {
        return passengerIdCardToken;
    }

    public void setPassengerIdCardToken(String passengerIdCardToken) {
        this.passengerIdCardToken = passengerIdCardToken;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public boolean isReservationSuccessSent() {
        return reservationSuccessSent;
    }

    public void setReservationSuccessSent(boolean reservationSuccessSent) {
        this.reservationSuccessSent = reservationSuccessSent;
    }

    public boolean isArrivalReminderSent() {
        return arrivalReminderSent;
    }

    public void setArrivalReminderSent(boolean arrivalReminderSent) {
        this.arrivalReminderSent = arrivalReminderSent;
    }

    public boolean isMissedCheckInSent() {
        return missedCheckInSent;
    }

    public void setMissedCheckInSent(boolean missedCheckInSent) {
        this.missedCheckInSent = missedCheckInSent;
    }
}
