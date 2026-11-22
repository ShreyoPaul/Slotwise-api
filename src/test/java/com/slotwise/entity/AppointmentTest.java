package com.slotwise.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppointmentTest {

    //    ----------------------------------------Cancel---------------------------------------------

    @Test
    void shouldCancelBookedAppointment() {

        Provider provider = new Provider(
                "Dr. Sharma",
                "General Physician"
        );

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                startTime,
                endTime
        );

        appointment.cancel();

        assertEquals(
                AppointmentStatus.CANCELLED,
                appointment.getStatus()
        );
    }

    @Test
    void shouldThrowExceptionWhenCancellingAlreadyCancelledAppointment() {
        Provider provider = new Provider(
                "Dr. Sharma",
                "General Physician"
        );

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                startTime,
                endTime
        );

        appointment.cancel();

        assertThrows(IllegalStateException.class, appointment::cancel);
    }

    //    ----------------------------------------Reschedule---------------------------------------------

    @Test
    void shouldRescheduleBookedAppointment() {
        Provider provider = new Provider(
                "Dr. Sharma",
                "General Physician"
        );

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                startTime,
                endTime
        );

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        appointment.reschedule(newStartTime, newEndTime);

        assertEquals(newStartTime, appointment.getStartTime());
        assertEquals(newEndTime, appointment.getEndTime());
        assertEquals(AppointmentStatus.BOOKED, appointment.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenReschedulingCancelledAppointment() {
        Provider provider = new Provider(
                "Dr. Sharma",
                "General Physician"
        );

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                startTime,
                endTime
        );

        appointment.cancel();

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        assertThrows(
                IllegalArgumentException.class,
                () -> appointment.reschedule(newStartTime, newEndTime)
        );
    }

    @Test
    void shouldThrowExceptionWhenEndTimeIsBeforeStartTime() {
        Provider provider = new Provider(
                "Dr. Sharma",
                "General Physician"
        );

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                startTime,
                endTime
        );

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
        LocalDateTime newEndTime = newStartTime.minusMinutes(15);

        assertThrows(
                IllegalArgumentException.class,
                () -> appointment.reschedule(newStartTime, newEndTime)
        );
    }

}