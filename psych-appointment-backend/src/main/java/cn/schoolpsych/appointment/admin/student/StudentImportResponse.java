package cn.schoolpsych.appointment.admin.student;

import java.util.List;

public record StudentImportResponse(
        Long batchId,
        int totalRows,
        int successCount,
        int skippedCount,
        int failedCount,
        List<StudentImportRowResult> rows) {
}
