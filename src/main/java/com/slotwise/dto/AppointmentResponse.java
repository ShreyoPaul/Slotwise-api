
package com.slotwise.dto;

import com.slotwise.entity.AppointmentStatus;
import java.time.LocalDateTime;

public class AppointmentResponse {

    private final Long id;
    private final String customerName;
    private final Long providerId;
    private final String serviceName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final AppointmentStatus status;

    public AppointmentResponse(
            Long id,
            String customerName,
            Long providerId,
            String serviceName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AppointmentStatus status) {

        this.id = id;
        this.customerName = customerName;
        this.providerId = providerId;
        this.serviceName = serviceName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public Long getProviderId() { return providerId; }
    public String getServiceName() { return serviceName; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AppointmentStatus getStatus() { return status; }
}
