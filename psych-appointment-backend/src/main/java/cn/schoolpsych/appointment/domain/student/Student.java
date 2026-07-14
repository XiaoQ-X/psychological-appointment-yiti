package cn.schoolpsych.appointment.domain.student;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "students")
public class Student extends BaseEntity {

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "student_no", nullable = false, unique = true, length = 64)
    private String studentNo;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "gender", length = 16)
    private String gender;

    @Column(name = "college", nullable = false, length = 128)
    private String college;

    @Column(name = "major", length = 128)
    private String major;

    @Column(name = "class_name", length = 128)
    private String className;

    @Column(name = "grade", length = 32)
    private String grade;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "phone_encrypted")
    private byte[] phoneEncrypted;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "no_show_count", nullable = false)
    private int noShowCount;

    @Column(name = "booking_restricted_until")
    private java.time.LocalDateTime bookingRestrictedUntil;

    protected Student() {
    }

    public static Student create(
            Long accountId,
            String studentNo,
            String name,
            String gender,
            String college,
            String major,
            String className,
            String grade,
            byte[] phoneEncrypted,
            String status) {
        Student student = new Student();
        student.accountId = accountId;
        student.studentNo = studentNo;
        student.name = name;
        student.gender = gender;
        student.college = college;
        student.major = major;
        student.className = className;
        student.grade = grade;
        student.phoneEncrypted = phoneEncrypted;
        student.status = status == null || status.isBlank() ? "ACTIVE" : status;
        student.noShowCount = 0;
        return student;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getCollege() {
        return college;
    }

    public String getMajor() {
        return major;
    }

    public String getClassName() {
        return className;
    }

    public String getGrade() {
        return grade;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getBookingRestrictedUntil() {
        return bookingRestrictedUntil;
    }

    public int getNoShowCount() {
        return noShowCount;
    }

    public void recordNoShow() {
        noShowCount++;
    }

    public boolean canBookNow(LocalDateTime now, int noShowRestrictThreshold) {
        if (noShowRestrictThreshold < 1) {
            throw new IllegalArgumentException("No-show restriction threshold must be positive");
        }
        return "ACTIVE".equals(status)
                && noShowCount < noShowRestrictThreshold
                && (bookingRestrictedUntil == null || !bookingRestrictedUntil.isAfter(now));
    }
}
