package cn.schoolpsych.appointment.admin.management;

import cn.schoolpsych.appointment.domain.location.Room;

public record RoomResponse(Long id, Long campusId, String campusName, String name, String locationDesc, String status) {
    static RoomResponse from(Room room, String campusName) {
        return new RoomResponse(
                room.getId(),
                room.getCampusId(),
                campusName,
                room.getName(),
                room.getLocationDesc(),
                room.getStatus());
    }
}
