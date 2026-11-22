package com.slotwise.repository;

import com.slotwise.entity.Appointment;
import com.slotwise.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AppointmentRepository
        extends JpaRepository<Appointment, Long> {
    // The naming conversion generates function like this -->
    //    SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
    //    FROM appointments
    //    WHERE provider_id = ?
    //    AND status = ?
    //    AND start_time < ?
    //    AND end_time > ?;
    boolean existsByProvider_IdAndStatusAndStartTimeBeforeAndEndTimeAfter(
            Long providerId,
            AppointmentStatus status,
            LocalDateTime requestedEnd,
            LocalDateTime requestedStart
    );


    // The naming conversion generates function like this -->
    //    SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
    //    FROM appointments
    //    WHERE provider_id = ?
    //    AND status = ?
    //    AND id <> ?
    //    AND start_time < ?
    //    AND end_time > ?;
    boolean existsByProvider_IdAndStatusAndIdNotAndStartTimeBeforeAndEndTimeAfter(
            Long providerId,
            AppointmentStatus status,
            Long appointmentId,
            LocalDateTime requestedEnd,
            LocalDateTime requestedStart
    );


}