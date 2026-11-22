
package com.slotwise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    private String serviceName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    private LocalDateTime createdAt;

    // Required by JPA
    protected Appointment() {
    }

    // Constructor for creating appointments
    public Appointment(
            String customerName,
            Provider provider,
            String serviceName,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        this.customerName = customerName;
        this.provider = provider;
        this.serviceName = serviceName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AppointmentStatus.BOOKED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Provider getProvider() {
        return provider;
    }

    public String getServiceName() {
        return serviceName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void cancel() {
        if (this.status != AppointmentStatus.BOOKED) {
            throw new IllegalStateException(
                    "Only booked appointments can be cancelled");
        }

        this.status = AppointmentStatus.CANCELLED;
    }

    public void reschedule(
            LocalDateTime newStartTime,
            LocalDateTime newEndTime) {

        if (this.status != AppointmentStatus.BOOKED) {
            throw new IllegalArgumentException(
                    "Only booked appointments can be rescheduled");
        }

        if (!newStartTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "New start time must be in the future");
        }

        if (!newEndTime.isAfter(newStartTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time");
        }

        this.startTime = newStartTime;
        this.endTime = newEndTime;
    }
}
