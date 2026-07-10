package cn.schoolpsych.appointment.domain.student;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_import_batches")
public class StudentImportBatch extends BaseEntity {

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "strategy", nullable = false, length = 32)
    private String strategy;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    protected StudentImportBatch() {
    }

    public static StudentImportBatch start(String fileName, String strategy, Long operatorId) {
        StudentImportBatch batch = new StudentImportBatch();
        batch.fileName = fileName;
        batch.strategy = strategy;
        batch.status = "PROCESSING";
        batch.operatorId = operatorId;
        return batch;
    }

    public void finish(int totalCount, int successCount, int failedCount) {
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.status = failedCount == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS";
    }
}
