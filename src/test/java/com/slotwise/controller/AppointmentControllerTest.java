package com.slotwise.controller;

import com.slotwise.dto.AppointmentRequest;
import com.slotwise.dto.AppointmentResponse;
import com.slotwise.dto.RescheduleRequest;
import com.slotwise.entity.AppointmentStatus;
import com.slotwise.exception.ResourceNotFoundException;
import com.slotwise.service.AppointmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.Mockito.*;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    void shouldCreateAppointmentAndReturn201() throws Exception {
        LocalDateTime startTime = LocalDateTime.parse("2027-01-10T10:00:00");
        LocalDateTime endTime = startTime.plusMinutes(30);

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Rahul",
                1L,
                "General Checkup",
                startTime,
                endTime,
                AppointmentStatus.BOOKED
        );

        when(appointmentService.createAppointment(any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "customerName": "Rahul",
                            "providerId": 1,
                            "serviceName": "General Checkup",
                            "startTime": "2027-01-10T10:00:00",
                            "endTime": "2027-01-10T10:30:00"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Rahul"))
                .andExpect(jsonPath("$.providerId").value(1))
                .andExpect(jsonPath("$.serviceName").value("General Checkup"))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void shouldReturnAllAppointments() throws Exception {
        LocalDateTime startTime = LocalDateTime.parse("2027-01-10T10:00:00");
        LocalDateTime endTime = startTime.plusMinutes(30);

        AppointmentResponse first = new AppointmentResponse(
                1L, "Rahul", 1L, "General Checkup",
                startTime, endTime, AppointmentStatus.BOOKED
        );

        AppointmentResponse second = new AppointmentResponse(
                2L, "Amit", 2L, "Cardiology Consultation",
                startTime.plusHours(1), endTime.plusHours(1),
                AppointmentStatus.BOOKED
        );

        when(appointmentService.getAllAppointments())
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerName").value("Rahul"))
                .andExpect(jsonPath("$[1].customerName").value("Amit"));
    }

    @Test
    void shouldReturnAppointmentById() throws Exception {
        LocalDateTime startTime = LocalDateTime.parse("2027-01-10T10:00:00");
        LocalDateTime endTime = startTime.plusMinutes(30);

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Rahul",
                1L,
                "General Checkup",
                startTime,
                endTime,
                AppointmentStatus.BOOKED
        );

        when(appointmentService.getAppointmentById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/appointments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Rahul"))
                .andExpect(jsonPath("$.providerId").value(1))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void shouldReturn404WhenAppointmentDoesNotExist() throws Exception {
        when(appointmentService.getAppointmentById(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Appointment not found: 99"
                ));

        mockMvc.perform(get("/api/appointments/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCancelAppointmentAndReturn200() throws Exception {
        LocalDateTime startTime = LocalDateTime.parse("2027-01-10T10:00:00");
        LocalDateTime endTime = startTime.plusMinutes(30);

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Rahul",
                1L,
                "General Checkup",
                startTime,
                endTime,
                AppointmentStatus.CANCELLED
        );

        when(appointmentService.cancelAppointment(1L))
                .thenReturn(response);

        mockMvc.perform(patch("/api/appointments/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Rahul"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldRescheduleAppointmentAndReturn200() throws Exception {
        LocalDateTime newStartTime = LocalDateTime.parse("2027-01-12T11:00:00");
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Rahul",
                1L,
                "General Checkup",
                newStartTime,
                newEndTime,
                AppointmentStatus.BOOKED
        );

        when(appointmentService.rescheduleAppointment(
                eq(1L),
                any(RescheduleRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/appointments/1/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "startTime": "2027-01-12T11:00:00",
                        "endTime": "2027-01-12T11:45:00"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.startTime").value("2027-01-12T11:00:00"))
                .andExpect(jsonPath("$.endTime").value("2027-01-12T11:45:00"))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void shouldReturnEmptyListWhenNoAppointmentsExist() throws Exception {
        when(appointmentService.getAllAppointments())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn400WhenCustomerNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "providerId": 1,
                        "serviceName": "General Checkup",
                        "startTime": "2027-01-10T10:00:00",
                        "endTime": "2027-01-10T10:30:00"
                    }
                    """))
                .andExpect(status().isBadRequest());

        verify(appointmentService, never())
                .createAppointment(any(AppointmentRequest.class));
    }

    @Test
    void shouldReturn400WhenAppointmentBookingConflicts() throws Exception {
        when(appointmentService.createAppointment(any(AppointmentRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Provider already has an appointment during this time"
                ));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "customerName": "Rahul",
                        "providerId": 1,
                        "serviceName": "General Checkup",
                        "startTime": "2027-01-10T10:00:00",
                        "endTime": "2027-01-10T10:30:00"
                    }
                    """))
                .andExpect(status().isBadRequest());

        verify(appointmentService).createAppointment(any(AppointmentRequest.class));
    }

    @Test
    void shouldReturn404WhenCancellingNonExistingAppointment() throws Exception {
        when(appointmentService.cancelAppointment(99L))
                .thenThrow(new ResourceNotFoundException(
                        "Appointment not found with ID: 99"
                ));

        mockMvc.perform(patch("/api/appointments/99/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenReschedulingNonExistingAppointment() throws Exception {
        when(appointmentService.rescheduleAppointment(
                eq(99L),
                any(RescheduleRequest.class)
        )).thenThrow(new ResourceNotFoundException(
                "Appointment not found with ID: 99"
        ));

        mockMvc.perform(patch("/api/appointments/99/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "startTime": "2027-01-12T11:00:00",
                        "endTime": "2027-01-12T11:45:00"
                    }
                    """))
                .andExpect(status().isNotFound());
    }


}