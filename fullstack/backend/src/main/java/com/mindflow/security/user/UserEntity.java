package com.mindflow.security.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "ux_users_tenant_username", columnNames = {"tenant_id", "username"})
)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 120)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "arrival_reminder_enabled", nullable = false)
    private boolean arrivalReminderEnabled = true;

    @Column(name = "reservation_success_enabled", nullable = false)
    private boolean reservationSuccessEnabled = true;

    @Column(name = "reminder_lead_minutes", nullable = false)
    private int reminderLeadMinutes = 10;

    @Column(name = "dnd_start")
    private LocalTime dndStart;

    @Column(name = "dnd_end")
    private LocalTime dndEnd;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId = "default";

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isArrivalReminderEnabled() {
        return arrivalReminderEnabled;
    }

    public void setArrivalReminderEnabled(boolean arrivalReminderEnabled) {
        this.arrivalReminderEnabled = arrivalReminderEnabled;
    }

    public boolean isReservationSuccessEnabled() {
        return reservationSuccessEnabled;
    }

    public void setReservationSuccessEnabled(boolean reservationSuccessEnabled) {
        this.reservationSuccessEnabled = reservationSuccessEnabled;
    }

    public int getReminderLeadMinutes() {
        return reminderLeadMinutes;
    }

    public void setReminderLeadMinutes(int reminderLeadMinutes) {
        this.reminderLeadMinutes = reminderLeadMinutes;
    }

    public LocalTime getDndStart() {
        return dndStart;
    }

    public void setDndStart(LocalTime dndStart) {
        this.dndStart = dndStart;
    }

    public LocalTime getDndEnd() {
        return dndEnd;
    }

    public void setDndEnd(LocalTime dndEnd) {
        this.dndEnd = dndEnd;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
