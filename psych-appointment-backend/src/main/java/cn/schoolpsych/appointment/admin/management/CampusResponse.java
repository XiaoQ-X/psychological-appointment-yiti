package cn.schoolpsych.appointment.admin.management;

import cn.schoolpsych.appointment.domain.location.Campus;

public record CampusResponse(Long id, String name, String address, String status) {
    static CampusResponse from(Campus campus) {
        return new CampusResponse(campus.getId(), campus.getName(), campus.getAddress(), campus.getStatus());
    }
}
