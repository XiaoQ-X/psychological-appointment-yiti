package cn.schoolpsych.appointment.admin.student;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class StudentExcelParserTest {

    @Test
    void parsesRequiredAndOptionalStudentColumns() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes());

        var rows = new StudentExcelParser().parse(file, 100);

        assertThat(rows).hasSize(1);
        ParsedStudentRow row = rows.get(0);
        assertThat(row.rowNo()).isEqualTo(2);
        assertThat(row.studentNo()).isEqualTo("20260001");
        assertThat(row.name()).isEqualTo("张三");
        assertThat(row.initialPassword()).isEqualTo("Password123");
        assertThat(row.college()).isEqualTo("信息学院");
        assertThat(row.phone()).isEqualTo("13800000000");
    }

    private byte[] workbookBytes() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("students");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("学号");
            header.createCell(1).setCellValue("姓名");
            header.createCell(2).setCellValue("初始密码");
            header.createCell(3).setCellValue("学院");
            header.createCell(4).setCellValue("手机号");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("20260001");
            row.createCell(1).setCellValue("张三");
            row.createCell(2).setCellValue("Password123");
            row.createCell(3).setCellValue("信息学院");
            row.createCell(4).setCellValue("13800000000");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
