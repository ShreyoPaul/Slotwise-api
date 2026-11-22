
package com.slotwise.service.impl;

import com.slotwise.dto.AppointmentRequest;
import com.slotwise.dto.AppointmentResponse;
import com.slotwise.dto.RescheduleRequest;
import com.slotwise.entity.Appointment;
import com.slotwise.entity.AppointmentStatus;
import com.slotwise.entity.Provider;
import com.slotwise.exception.ResourceNotFoundException;
import com.slotwise.repository.AppointmentRepository;
import com.slotwise.repository.ProviderRepository;
import com.slotwise.service.AppointmentService;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentServiceImpl implements AppointmentService {

        private final AppointmentRepository appointmentRepository;
        private final ProviderRepository providerRepository;

        public AppointmentServiceImpl(
                        AppointmentRepository appointmentRepository,
                        ProviderRepository providerRepository) {
                this.appointmentRepository = appointmentRepository;
                this.providerRepository = providerRepository;
        }

        @Override
        public AppointmentResponse createAppointment(AppointmentRequest request) {

                Provider provider = providerRepository.findById(
                                request.getProviderId()).orElseThrow(
                                                () -> new ResourceNotFoundException(
                                                                "Provider not found with ID: "
                                                                                + request.getProviderId()));

                if (!request.getEndTime().isAfter(request.getStartTime())) {
                        throw new IllegalArgumentException(
                                        "End time must be after start time");
                }

                boolean hasConflict = appointmentRepository
                                .existsByProvider_IdAndStatusAndStartTimeBeforeAndEndTimeAfter(provider.getId(),
                                                AppointmentStatus.BOOKED,
                                                request.getEndTime(), request.getStartTime());

                if (hasConflict) {
                        throw new IllegalArgumentException(
                                        "Provider already has an appointment during this time");
                }

                Appointment appointment = new Appointment(
                                request.getCustomerName().trim(),
                                provider,
                                request.getServiceName().trim(),
                                request.getStartTime(),
                                request.getEndTime());

                Appointment saved = appointmentRepository.save(appointment);

                return convertToResponse(saved);
        }

        @Override
        public List<AppointmentResponse> getAllAppointments() {
                List<Appointment> appointments = appointmentRepository.findAll();
                return appointments.stream().map(this::convertToResponse).toList();
        }

        @Override
        public AppointmentResponse getAppointmentById(Long id) {
                Appointment appointment = appointmentRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Appointment not found: " + id));
                return convertToResponse(appointment);
        }

        @Override
        @Transactional
        public AppointmentResponse cancelAppointment(Long id) {

                Appointment appointment = appointmentRepository
                                .findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Appointment not found with ID: " + id));

                appointment.cancel();

                Appointment saved = appointmentRepository.save(appointment);

                return convertToResponse(saved);
        }

        @Override
        @Transactional
        public AppointmentResponse rescheduleAppointment(
                        Long id,
                        RescheduleRequest request) {

                Appointment appointment = appointmentRepository
                                .findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Appointment not found with ID: " + id));

                if (!request.getEndTime().isAfter(request.getStartTime())) {
                        throw new IllegalArgumentException(
                                        "End time must be after start time");
                }

                boolean hasConflict = appointmentRepository
                                .existsByProvider_IdAndStatusAndIdNotAndStartTimeBeforeAndEndTimeAfter(
                                                appointment.getProvider().getId(), AppointmentStatus.BOOKED,
                                                appointment.getId(), request.getEndTime(), request.getStartTime());

                if (hasConflict) {
                        throw new IllegalArgumentException(
                                        "Provider already has an appointment during this time, cannot reschedule");
                }

                appointment.reschedule(
                                request.getStartTime(),
                                request.getEndTime());

                Appointment saved = appointmentRepository.save(appointment);

                return convertToResponse(saved);
        }

        private AppointmentResponse convertToResponse(Appointment appointment) {

                return new AppointmentResponse(
                                appointment.getId(),
                                appointment.getCustomerName(),
                                appointment.getProvider().getId(),
                                appointment.getServiceName(),
                                appointment.getStartTime(),
                                appointment.getEndTime(),
                                appointment.getStatus());
        }

}
