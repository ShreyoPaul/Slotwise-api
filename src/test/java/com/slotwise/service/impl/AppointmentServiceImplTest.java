
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private void setAppointmentId(Appointment appointment, Long id) {
        try {
            Field field = Appointment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(appointment, id);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }


    @Test
    void shouldCreateAppointmentSuccessfully() {

        Provider provider = mock(Provider.class);

        when(provider.getId()).thenReturn(1L);

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        AppointmentRequest request = new AppointmentRequest();
        request.setCustomerName("Rahul");
        request.setProviderId(1L);
        request.setServiceName("General Checkup");
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        when(providerRepository.findById(1L))
                .thenReturn(Optional.of(provider));

        when(appointmentRepository
                .existsByProvider_IdAndStatusAndStartTimeBeforeAndEndTimeAfter(
                        1L,
                        AppointmentStatus.BOOKED,
                        endTime,
                        startTime
                ))
                .thenReturn(false);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> {
                    Appointment appointment = invocation.getArgument(0);
                    return appointment;
                });

        AppointmentResponse response =
                appointmentService.createAppointment(request);

        assertNotNull(response);
        assertEquals("Rahul", response.getCustomerName());
        assertEquals(1L, response.getProviderId());
        assertEquals("General Checkup", response.getServiceName());
        assertEquals(startTime, response.getStartTime());
        assertEquals(endTime, response.getEndTime());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());

        verify(providerRepository).findById(1L);
        verify(appointmentRepository).save(any(Appointment.class));

    }

    @Test
    void shouldThrowExceptionWhenAppointmentTimeConflicts() {
        Provider provider = mock(Provider.class);
        when(provider.getId()).thenReturn(1L);

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        AppointmentRequest request = new AppointmentRequest();
        request.setCustomerName("Rahul");
        request.setProviderId(1L);
        request.setServiceName("General Checkup");
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        when(providerRepository.findById(1L))
                .thenReturn(Optional.of(provider));

        when(appointmentRepository
                .existsByProvider_IdAndStatusAndStartTimeBeforeAndEndTimeAfter(
                        1L,
                        AppointmentStatus.BOOKED,
                        endTime,
                        startTime
                ))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request)
        );

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldThrowExceptionWhenProviderDoesNotExist() {
        AppointmentRequest request = new AppointmentRequest();
        request.setCustomerName("Rahul");
        request.setProviderId(99L);
        request.setServiceName("General Checkup");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(30));

        when(providerRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.createAppointment(request)
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));

        verify(appointmentRepository, never())
                .existsByProvider_IdAndStatusAndStartTimeBeforeAndEndTimeAfter(
                        anyLong(),
                        any(AppointmentStatus.class),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void shouldThrowExceptionWhenEndTimeIsBeforeStartTime() {
        Provider provider = mock(Provider.class);

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.minusMinutes(30);

        AppointmentRequest request = new AppointmentRequest();
        request.setCustomerName("Rahul");
        request.setProviderId(1L);
        request.setServiceName("General Checkup");
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        when(providerRepository.findById(1L))
                .thenReturn(Optional.of(provider));

        assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.createAppointment(request)
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));

        verify(appointmentRepository, never())
                .existsByProvider_IdAndStatusAndStartTimeBeforeAndEndTimeAfter(
                        anyLong(),
                        any(AppointmentStatus.class),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void shouldCancelAppointmentSuccessfully() {
        Provider provider = mock(Provider.class);
        when(provider.getId()).thenReturn(1L);

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                startTime,
                endTime
        );

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                appointmentService.cancelAppointment(1L);

        assertNotNull(response);
        assertEquals(AppointmentStatus.CANCELLED, response.getStatus());
        verify(appointmentRepository).findById(1L);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void shouldThrowExceptionWhenCancellingAlreadyCancelledAppointment() {
        Provider provider = mock(Provider.class);

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

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));

        assertThrows(
                IllegalStateException.class,
                () -> appointmentService.cancelAppointment(1L)
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));
    }

    @Test
    void shouldRescheduleAppointmentSuccessfully() {
        Provider provider = mock(Provider.class);
        when(provider.getId()).thenReturn(1L);

        LocalDateTime oldStartTime = LocalDateTime.now().plusDays(1);
        LocalDateTime oldEndTime = oldStartTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                oldStartTime,
                oldEndTime
        );

        setAppointmentId(appointment, 1L);

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        RescheduleRequest request = new RescheduleRequest();
        request.setStartTime(newStartTime);
        request.setEndTime(newEndTime);

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));

        when(appointmentRepository
                .existsByProvider_IdAndStatusAndIdNotAndStartTimeBeforeAndEndTimeAfter(
                        1L,
                        AppointmentStatus.BOOKED,
                        1L,
                        newEndTime,
                        newStartTime
                ))
                .thenReturn(false);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                appointmentService.rescheduleAppointment(1L, request);

        assertNotNull(response);
        assertEquals(newStartTime, response.getStartTime());
        assertEquals(newEndTime, response.getEndTime());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());

        verify(appointmentRepository).findById(1L);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void shouldThrowExceptionWhenRescheduledTimeConflicts() {
        Provider provider = mock(Provider.class);
        when(provider.getId()).thenReturn(1L);

        LocalDateTime oldStartTime = LocalDateTime.now().plusDays(1);
        LocalDateTime oldEndTime = oldStartTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                oldStartTime,
                oldEndTime
        );

        setAppointmentId(appointment, 1L);

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        RescheduleRequest request = new RescheduleRequest();
        request.setStartTime(newStartTime);
        request.setEndTime(newEndTime);

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));

        when(appointmentRepository
                .existsByProvider_IdAndStatusAndIdNotAndStartTimeBeforeAndEndTimeAfter(
                        1L,
                        AppointmentStatus.BOOKED,
                        1L,
                        newEndTime,
                        newStartTime
                ))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.rescheduleAppointment(1L, request)
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));
    }

    @Test
    void shouldThrowExceptionWhenReschedulingCancelledAppointment() {
        Provider provider = mock(Provider.class);
        when(provider.getId()).thenReturn(1L);

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = startTime.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                startTime,
                endTime
        );

        setAppointmentId(appointment, 1L);
        appointment.cancel();

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        RescheduleRequest request = new RescheduleRequest();
        request.setStartTime(newStartTime);
        request.setEndTime(newEndTime);

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));

        assertThrows(
                IllegalArgumentException.class,
                () -> appointmentService.rescheduleAppointment(1L, request)
        );

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldThrowExceptionWhenReschedulingNonExistingAppointment() {
        LocalDateTime newStartTime = LocalDateTime.now().plusDays(2);
        LocalDateTime newEndTime = newStartTime.plusMinutes(45);

        RescheduleRequest request = new RescheduleRequest();
        request.setStartTime(newStartTime);
        request.setEndTime(newEndTime);

        when(appointmentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.rescheduleAppointment(99L, request)
        );

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void shouldAllowAppointmentsWhenPreviousAppointmentEndsAtNewStartTime() {
        Provider provider = mock(Provider.class);
        when(provider.getId()).thenReturn(1L);

        LocalDateTime existingStart = LocalDateTime.now().plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);

        LocalDateTime existingEnd = existingStart.plusMinutes(30);

        Appointment existingAppointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                existingStart,
                existingEnd
        );

        LocalDateTime newStart = existingEnd;
        LocalDateTime newEnd = newStart.plusMinutes(30);

        AppointmentRequest request = new AppointmentRequest();
        request.setCustomerName("Amit");
        request.setProviderId(1L);
        request.setServiceName("General Checkup");
        request.setStartTime(newStart);
        request.setEndTime(newEnd);

        when(providerRepository.findById(1L))
                .thenReturn(Optional.of(provider));

        when(appointmentRepository
                .existsByProvider_IdAndStatusAndStartTimeBeforeAndEndTimeAfter(
                        1L,
                        AppointmentStatus.BOOKED,
                        newEnd,
                        newStart
                ))
                .thenReturn(false);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                appointmentService.createAppointment(request);

        assertNotNull(response);
        assertEquals(newStart, response.getStartTime());
        assertEquals(newEnd, response.getEndTime());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());

        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void shouldAllowAppointmentWhenNewEndTimeMatchesExistingStartTime() {
        Provider provider = mock(Provider.class);
        when(provider.getId()).thenReturn(1L);

        LocalDateTime existingStart = LocalDateTime.now().plusDays(1)
                .withHour(10).withMinute(30).withSecond(0).withNano(0);

        LocalDateTime existingEnd = existingStart.plusMinutes(30);

        AppointmentRequest request = new AppointmentRequest();
        request.setCustomerName("Amit");
        request.setProviderId(1L);
        request.setServiceName("General Checkup");
        request.setStartTime(existingStart.minusMinutes(30));
        request.setEndTime(existingStart);

        when(providerRepository.findById(1L))
                .thenReturn(Optional.of(provider));

        when(appointmentRepository
                .existsByProvider_IdAndStatusAndStartTimeBeforeAndEndTimeAfter(
                        1L,
                        AppointmentStatus.BOOKED,
                        request.getEndTime(),
                        request.getStartTime()
                ))
                .thenReturn(false);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                appointmentService.createAppointment(request);

        assertNotNull(response);
        assertEquals(request.getStartTime(), response.getStartTime());
        assertEquals(request.getEndTime(), response.getEndTime());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());

        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void shouldExcludeCurrentAppointmentFromRescheduleConflictCheck() {
        Provider provider = mock(Provider.class);
        when(provider.getId()).thenReturn(1L);

        LocalDateTime oldStart = LocalDateTime.now().plusDays(1);
        LocalDateTime oldEnd = oldStart.plusMinutes(30);

        Appointment appointment = new Appointment(
                "Rahul",
                provider,
                "General Checkup",
                oldStart,
                oldEnd
        );

        setAppointmentId(appointment, 25L);

        LocalDateTime newStart = LocalDateTime.now().plusDays(2);
        LocalDateTime newEnd = newStart.plusMinutes(45);

        RescheduleRequest request = new RescheduleRequest();
        request.setStartTime(newStart);
        request.setEndTime(newEnd);

        when(appointmentRepository.findById(25L))
                .thenReturn(Optional.of(appointment));

        when(appointmentRepository
                .existsByProvider_IdAndStatusAndIdNotAndStartTimeBeforeAndEndTimeAfter(
                        1L,
                        AppointmentStatus.BOOKED,
                        25L,
                        newEnd,
                        newStart
                ))
                .thenReturn(false);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response =
                appointmentService.rescheduleAppointment(25L, request);

        assertEquals(newStart, response.getStartTime());
        assertEquals(newEnd, response.getEndTime());

        verify(appointmentRepository).existsByProvider_IdAndStatusAndIdNotAndStartTimeBeforeAndEndTimeAfter(
                1L,
                AppointmentStatus.BOOKED,
                25L,
                newEnd,
                newStart
        );
    }

}
