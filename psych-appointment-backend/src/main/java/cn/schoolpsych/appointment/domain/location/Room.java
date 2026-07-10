package cn.schoolpsych.appointment.domain.location;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    @Column(name = "campus_id", nullable = false)
    private Long campusId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "location_desc", nullable = false, length = 255)
    private String locationDesc;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected Room() {
    }

    public static Room create(Long campusId, String name, String locationDesc, String status) {
        Room room = new Room();
        room.campusId = campusId;
        room.name = name;
        room.locationDesc = locationDesc;
        room.status = status;
        return room;
    }

    public void update(Long campusId, String name, String locationDesc, String status) {
        this.campusId = campusId;
        this.name = name;
        this.locationDesc = locationDesc;
        this.status = status;
    }

    public Long getCampusId() {
        return campusId;
    }

    public String getName() {
        return name;
    }

    public String getLocationDesc() {
        return locationDesc;
    }

    public String getStatus() {
        return status;
    }
}
