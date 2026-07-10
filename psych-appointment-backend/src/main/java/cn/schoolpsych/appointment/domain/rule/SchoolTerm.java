package cn.schoolpsych.appointment.domain.rule;

import java.time.LocalDate;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "school_terms")
public class SchoolTerm extends BaseEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected SchoolTerm() {
    }

    public Long getId() {
        return super.getId();
    }
}
