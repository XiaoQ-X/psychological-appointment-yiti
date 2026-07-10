package cn.schoolpsych.appointment.domain.student;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_import_rows")
public class StudentImportRow extends BaseEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "row_no", nullable = false)
    private int rowNo;

    @Column(name = "student_no", length = 64)
    private String studentNo;

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    protected StudentImportRow() {
    }

    public static StudentImportRow of(Long batchId, int rowNo, String studentNo, String name, String status, String errorMessage) {
        StudentImportRow row = new StudentImportRow();
        row.batchId = batchId;
        row.rowNo = rowNo;
        row.studentNo = studentNo;
        row.name = name;
        row.status = status;
        row.errorMessage = errorMessage;
        return row;
    }
}
