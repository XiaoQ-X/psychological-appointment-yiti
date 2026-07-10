package cn.schoolpsych.appointment.admin.student;

public record StudentImportRowResult(
        int rowNo,
        String studentNo,
        String name,
        String status,
        String message) {
}
