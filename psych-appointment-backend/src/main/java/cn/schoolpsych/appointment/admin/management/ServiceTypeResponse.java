package cn.schoolpsych.appointment.admin.management;

import cn.schoolpsych.appointment.domain.service.ServiceType;

public record ServiceTypeResponse(Long id, String code, String name, int durationMinutes, boolean enabled) {
    static ServiceTypeResponse from(ServiceType serviceType) {
        return new ServiceTypeResponse(
                serviceType.getId(),
                serviceType.getCode(),
                serviceType.getName(),
                serviceType.getDurationMinutes(),
                serviceType.isEnabled());
    }
}
