package cn.schoolpsych.appointment.domain.location;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "campuses")
public class Campus extends BaseEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected Campus() {
    }

    public static Campus create(String name, String address, String status) {
        Campus campus = new Campus();
        campus.name = name;
        campus.address = address;
        campus.status = status;
        return campus;
    }

    public void update(String name, String address, String status) {
        this.name = name;
        this.address = address;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }
}
