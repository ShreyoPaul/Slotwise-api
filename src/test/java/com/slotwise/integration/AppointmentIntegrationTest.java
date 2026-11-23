package com.slotwise.integration;

import com.slotwise.entity.Appointment;
import com.slotwise.entity.AppointmentStatus;
import com.slotwise.entity.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import com.slotwise.repository.AppointmentRepository;
import com.slotwise.repository.ProviderRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest 
@AutoConfigureMockMvc 
@ActiveProfiles ("test")
@Transactional
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Test
    void shouldCreateAppointmentThroughApi() throws Exception {
        Provider provider = createProvider();
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(1);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(provider.getId(), start, end)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.customerName").value("Rahul"))
                .andExpect(jsonPath("$.providerId").value(provider.getId()))
                .andExpect(jsonPath("$.status").value("BOOKED"));

        assertEquals(1, appointmentRepository.count());
    }

    @Test
    void shouldReturnAllAppointments() throws Exception {
        Provider provider = createProvider();
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        createAppointment(provider, start, start.plusHours(1));

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Rahul"));
    }

    @Test
    void shouldReturnAppointmentById() throws Exception {
        Provider provider = createProvider();
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        Appointment appointment = createAppointment(provider, start, start.plusHours(1));

        mockMvc.perform(get("/api/appointments/{id}", appointment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointment.getId()))
                .andExpect(jsonPath("$.customerName").value("Rahul"))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void shouldCancelAppointmentThroughApi() throws Exception {
        Provider provider = createProvider();
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        Appointment appointment = createAppointment(provider, start, start.plusHours(1));

        mockMvc.perform(patch("/api/appointments/{id}/cancel", appointment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Appointment updated = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void shouldRescheduleAppointmentThroughApi() throws Exception {
        Provider provider = createProvider();
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        Appointment appointment = createAppointment(provider, start, start.plusHours(1));

        LocalDateTime newStart = LocalDateTime.now().plusDays(4).withNano(0);
        LocalDateTime newEnd = newStart.plusHours(1);

        String request = """
                {
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(newStart, newEnd);

        mockMvc.perform(patch("/api/appointments/{id}/reschedule", appointment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.startTime").value(newStart.toString()));

        Appointment updated = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(newStart, updated.getStartTime());
        assertEquals(newEnd, updated.getEndTime());
    }

    @Test
    void shouldRejectOverlappingAppointmentsThroughApi() throws Exception {
        Provider provider = createProvider();
        LocalDateTime existingStart = LocalDateTime.now().plusDays(2);
        createAppointment(provider, existingStart, existingStart.plusHours(1));

        LocalDateTime requestedStart = existingStart.plusMinutes(30);
        LocalDateTime requestedEnd = existingStart.plusHours(2);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(provider.getId(), requestedStart, requestedEnd)))
                .andExpect(status().isBadRequest());

        assertEquals(1, appointmentRepository.count());
    }

    @Test
    void shouldReturnNotFoundForNonexistentAppointment() throws Exception {
        mockMvc.perform(get("/api/appointments/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowBookingSameSlotAfterPreviousAppointmentIsCancelled() throws Exception {
        Provider provider = createProvider();

        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);
        LocalDateTime end = start.plusHours(1);

        Appointment existing = createAppointment(provider, start, end);

        mockMvc.perform(patch("/api/appointments/{id}/cancel", existing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(provider.getId(), start, end)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));

        assertEquals(2, appointmentRepository.count());
    }

    @Test
    void shouldRejectReschedulingIntoOccupiedSlot() throws Exception {
        Provider provider = createProvider();

        LocalDateTime firstStart = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime secondStart = LocalDateTime.now().plusDays(4).withNano(0);

        Appointment first = createAppointment(
                provider, firstStart, firstStart.plusHours(1));

        createAppointment(
                provider, secondStart, secondStart.plusHours(1));

        LocalDateTime requestedStart = secondStart.plusMinutes(15);
        LocalDateTime requestedEnd = requestedStart.plusHours(1);

        String request = """
            {
                "startTime": "%s",
                "endTime": "%s"
            }
            """.formatted(requestedStart, requestedEnd);

        mockMvc.perform(patch("/api/appointments/{id}/reschedule", first.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        Appointment unchanged = appointmentRepository.findById(first.getId()).orElseThrow();

        assertEquals(firstStart, unchanged.getStartTime());
        assertEquals(AppointmentStatus.BOOKED, unchanged.getStatus());
        assertEquals(2, appointmentRepository.count());
    }

    @Test
    void shouldAllowBackToBackAppointments() throws Exception {
        Provider provider = createProvider();

        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);
        LocalDateTime end = start.plusHours(1);

        createAppointment(provider, start, end);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(provider.getId(), end, end.plusHours(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"));

        assertEquals(2, appointmentRepository.count());
    }

    @Test
    void shouldRejectAppointmentWithMissingCustomerName() throws Exception {
        Provider provider = createProvider();

        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);
        LocalDateTime end = start.plusHours(1);

        String request = """
            {
                "providerId": %d,
                "serviceName": "General Checkup",
                "startTime": "%s",
                "endTime": "%s"
            }
            """.formatted(provider.getId(), start, end);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertEquals(0, appointmentRepository.count());
    }








    private Provider createProvider() {
        return providerRepository.saveAndFlush(
                new Provider("Dr. Integration", "General Physician")
        );
    }

    private Appointment createAppointment(
            Provider provider,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return appointmentRepository.saveAndFlush(
                new Appointment("Rahul", provider, "General Checkup", start, end)
        );
    }

    private String createRequest(
            Long providerId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return """
                {
                    "customerName": "Rahul",
                    "providerId": %d,
                    "serviceName": "General Checkup",
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(providerId, start, end);
    }
}