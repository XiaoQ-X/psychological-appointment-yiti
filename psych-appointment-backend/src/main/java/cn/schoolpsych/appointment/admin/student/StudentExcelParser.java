package cn.schoolpsych.appointment.admin.student;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class StudentExcelParser {

    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("学号", "studentNo"),
            Map.entry("student_no", "studentNo"),
            Map.entry("studentno", "studentNo"),
            Map.entry("姓名", "name"),
            Map.entry("name", "name"),
            Map.entry("初始密码", "initialPassword"),
            Map.entry("密码", "initialPassword"),
            Map.entry("password", "initialPassword"),
            Map.entry("initialpassword", "initialPassword"),
            Map.entry("学院", "college"),
            Map.entry("college", "college"),
            Map.entry("专业", "major"),
            Map.entry("major", "major"),
            Map.entry("班级", "className"),
            Map.entry("class", "className"),
            Map.entry("classname", "className"),
            Map.entry("年级", "grade"),
            Map.entry("grade", "grade"),
            Map.entry("性别", "gender"),
            Map.entry("gender", "gender"),
            Map.entry("手机号", "phone"),
            Map.entry("手机", "phone"),
            Map.entry("phone", "phone"),
            Map.entry("状态", "status"),
            Map.entry("status", "status"));

    private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

    public List<ParsedStudentRow> parse(MultipartFile file, int maxRows) {
        validateFile(file);
        ZipSecureFile.setMinInflateRatio(0.01);
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 文件没有工作表");
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> headers = readHeaders(headerRow);
            requireHeader(headers, "studentNo", "学号");
            requireHeader(headers, "name", "姓名");
            requireHeader(headers, "initialPassword", "初始密码");
            requireHeader(headers, "college", "学院");

            List<ParsedStudentRow> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                if (rows.size() >= maxRows) {
                    throw new IllegalArgumentException("单次最多导入 " + maxRows + " 行学生数据");
                }
                rows.add(new ParsedStudentRow(
                        rowIndex + 1,
                        value(row, headers.get("studentNo")),
                        value(row, headers.get("name")),
                        value(row, headers.get("initialPassword")),
                        value(row, headers.get("college")),
                        value(row, headers.get("major")),
                        value(row, headers.get("className")),
                        value(row, headers.get("grade")),
                        value(row, headers.get("gender")),
                        value(row, headers.get("phone")),
                        value(row, headers.get("status"))));
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取 Excel 文件", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 Excel 文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new IllegalArgumentException("仅支持 .xlsx 或 .xls 文件");
        }
    }

    private Map<String, Integer> readHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel 第一行必须是表头");
        }
        Map<String, Integer> headers = new HashMap<>();
        for (int i = headerRow.getFirstCellNum(); i < headerRow.getLastCellNum(); i++) {
            String rawHeader = formatter.formatCellValue(headerRow.getCell(i)).trim();
            String key = HEADER_ALIASES.get(normalizeHeader(rawHeader));
            if (key != null) {
                headers.put(key, i);
            }
        }
        return headers;
    }

    private void requireHeader(Map<String, Integer> headers, String key, String label) {
        if (!headers.containsKey(key)) {
            throw new IllegalArgumentException("缺少必填表头：" + label);
        }
    }

    private String normalizeHeader(String value) {
        return value.trim().replace(" ", "").replace("_", "").toLowerCase(Locale.ROOT);
    }

    private String value(Row row, Integer cellIndex) {
        if (cellIndex == null) {
            return null;
        }
        return formatter.formatCellValue(row.getCell(cellIndex)).trim();
    }

    private boolean isBlankRow(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            if (!formatter.formatCellValue(row.getCell(i)).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }
}
