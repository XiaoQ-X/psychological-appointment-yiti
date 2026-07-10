package cn.schoolpsych.appointment.admin.student;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.schoolpsych.appointment.admin.audit.AuditActions;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.domain.student.Student;
import cn.schoolpsych.appointment.domain.student.StudentImportBatch;
import cn.schoolpsych.appointment.domain.student.StudentImportRow;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.StudentImportBatchRepository;
import cn.schoolpsych.appointment.repository.StudentImportRowRepository;
import cn.schoolpsych.appointment.repository.StudentRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import cn.schoolpsych.appointment.security.SensitiveDataEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentImportService {

    private final StudentExcelParser excelParser;
    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;
    private final StudentImportBatchRepository batchRepository;
    private final StudentImportRowRepository rowRepository;
    private final PasswordEncoder passwordEncoder;
    private final SensitiveDataEncryptor sensitiveDataEncryptor;
    private final AuditLogService auditLogService;
    private final int maxRows;

    public StudentImportService(
            StudentExcelParser excelParser,
            AccountRepository accountRepository,
            StudentRepository studentRepository,
            StudentImportBatchRepository batchRepository,
            StudentImportRowRepository rowRepository,
            PasswordEncoder passwordEncoder,
            SensitiveDataEncryptor sensitiveDataEncryptor,
            AuditLogService auditLogService,
            @Value("${app.import.max-student-rows}") int maxRows) {
        this.excelParser = excelParser;
        this.accountRepository = accountRepository;
        this.studentRepository = studentRepository;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.passwordEncoder = passwordEncoder;
        this.sensitiveDataEncryptor = sensitiveDataEncryptor;
        this.auditLogService = auditLogService;
        this.maxRows = maxRows;
    }

    @Transactional
    public StudentImportResponse importStudents(
            MultipartFile file,
            StudentImportStrategy strategy,
            AuthenticatedAccount operator,
            AuditRequestMetadata metadata) {
        List<ParsedStudentRow> parsedRows = excelParser.parse(file, maxRows);
        StudentImportBatch batch = batchRepository.save(StudentImportBatch.start(
                originalFilename(file),
                strategy.name(),
                operator.accountId()));

        int successCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        Set<String> seenStudentNos = new HashSet<>();
        List<StudentImportRow> importRows = new ArrayList<>();
        List<StudentImportRowResult> resultRows = new ArrayList<>();

        for (ParsedStudentRow row : parsedRows) {
            String validationError = validate(row, seenStudentNos);
            if (validationError != null) {
                failedCount++;
                addRow(batch, row, "FAILED", validationError, importRows, resultRows);
                continue;
            }
            if (studentRepository.existsByStudentNo(row.studentNo()) || accountRepository.existsByUsername(row.studentNo())) {
                skippedCount++;
                addRow(batch, row, "SKIPPED", "学号已存在，已跳过", importRows, resultRows);
                continue;
            }
            byte[] encryptedPhone = sensitiveDataEncryptor.encryptNullable(row.phone());
            Account account = Account.create(
                    row.studentNo(),
                    passwordEncoder.encode(row.initialPassword()),
                    AccountRole.STUDENT,
                    true);
            accountRepository.save(account);
            Student student = Student.create(
                    account.getId(),
                    row.studentNo(),
                    row.name(),
                    blankToNull(row.gender()),
                    row.college(),
                    blankToNull(row.major()),
                    blankToNull(row.className()),
                    blankToNull(row.grade()),
                    encryptedPhone,
                    normalizeStatus(row.status()));
            studentRepository.save(student);
            successCount++;
            addRow(batch, row, "SUCCESS", "导入成功", importRows, resultRows);
        }

        rowRepository.saveAll(importRows);
        batch.finish(parsedRows.size(), successCount, failedCount);
        batchRepository.save(batch);

        StudentImportResponse response = new StudentImportResponse(
                batch.getId(),
                parsedRows.size(),
                successCount,
                skippedCount,
                failedCount,
                resultRows);
        auditLogService.record(
                operator,
                AuditActions.STUDENT_IMPORT,
                "STUDENT_IMPORT_BATCH",
                batch.getId(),
                SensitiveLevel.SENSITIVE,
                metadata,
                java.util.Map.of(
                        "strategy", strategy.name(),
                        "totalRows", parsedRows.size(),
                        "successCount", successCount,
                        "skippedCount", skippedCount,
                        "failedCount", failedCount));
        return response;
    }

    private String validate(ParsedStudentRow row, Set<String> seenStudentNos) {
        if (isBlank(row.studentNo())) {
            return "学号不能为空";
        }
        if (isBlank(row.name())) {
            return "姓名不能为空";
        }
        if (isBlank(row.initialPassword())) {
            return "初始密码不能为空";
        }
        if (row.initialPassword().length() < 8) {
            return "初始密码至少 8 位";
        }
        if (isBlank(row.college())) {
            return "学院不能为空";
        }
        if (!seenStudentNos.add(row.studentNo())) {
            return "Excel 内学号重复";
        }
        return null;
    }

    private void addRow(
            StudentImportBatch batch,
            ParsedStudentRow row,
            String status,
            String message,
            List<StudentImportRow> importRows,
            List<StudentImportRowResult> resultRows) {
        importRows.add(StudentImportRow.of(batch.getId(), row.rowNo(), row.studentNo(), row.name(), status, message));
        resultRows.add(new StudentImportRowResult(row.rowNo(), row.studentNo(), row.name(), status, message));
    }

    private String normalizeStatus(String status) {
        if (isBlank(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("disabled") || normalized.equals("inactive") || normalized.equals("禁用")) {
            return "DISABLED";
        }
        return "ACTIVE";
    }

    private String originalFilename(MultipartFile file) {
        return file.getOriginalFilename() == null ? "students.xlsx" : file.getOriginalFilename();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
