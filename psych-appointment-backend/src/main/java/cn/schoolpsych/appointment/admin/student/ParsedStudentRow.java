package cn.schoolpsych.appointment.admin.student;

public record ParsedStudentRow(
        int rowNo,
        String studentNo,
        String name,
        String initialPassword,
        String college,
        String major,
        String className,
        String grade,
        String gender,
        String phone,
        String status) {
}
