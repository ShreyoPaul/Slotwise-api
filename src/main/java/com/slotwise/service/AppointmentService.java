
package com.slotwise.service;

import com.slotwise.dto.AppointmentRequest;
import com.slotwise.dto.AppointmentResponse;
import com.slotwise.dto.RescheduleRequest;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse createAppointment(AppointmentRequest request);

    List<AppointmentResponse> getAllAppointments();

    AppointmentResponse getAppointmentById(Long id);

    AppointmentResponse cancelAppointment(Long id);

    AppointmentResponse rescheduleAppointment(
            Long id,
            RescheduleRequest request);

}
