package io.github.dekkerding.examples.domain.document.parser.impl;

import io.github.dekkerding.examples.domain.document.model.ParsedDocument;
import io.github.dekkerding.examples.domain.document.parser.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Excel文档解析器
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class ExcelParser implements DocumentParser {

    private static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public String getSupportedFileType() {
        return XLSX_CONTENT_TYPE;
    }

    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();

        return XLSX_CONTENT_TYPE.equals(contentType) || XLS_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws IOException {
        Workbook workbook = null;

        try {
            // 判断是.xls还是.xlsx
            if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new HSSFWorkbook(inputStream);
            }

            StringBuilder textContent = new StringBuilder();
            List<ParsedDocument.Page> pages = new ArrayList<>();
            List<ParsedDocument.Table> tables = new ArrayList<>();

            // 遍历所有工作表
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);

                textContent.append("【工作表: ").append(sheet.getSheetName()).append("】\n");

                // 提取表格数据
                ParsedDocument.Table table = extractSheet(sheet, sheetIndex + 1);

                if (table != null && !table.getRows().isEmpty()) {
                    tables.add(table);

                    // 将表格转换为文本
                    for (List<String> row : table.getRows()) {
                        textContent.append(String.join(" | ", row)).append("\n");
                    }
                }

                textContent.append("\n");
            }

            // 每个工作表作为一页
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                pages.add(ParsedDocument.Page.builder()
                        .pageNumber(i + 1)
                        .content("【工作表: " + workbook.getSheetName(i) + "】")
                        .build());
            }

            // 提取元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sheet_count", workbook.getNumberOfSheets());

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .fileType(fileName != null && fileName.toLowerCase().endsWith(".xlsx") ? "xlsx" : "xls")
                    .fileSize((long) textContent.length())
                    .textContent(textContent.toString())
                    .pages(pages)
                    .tables(tables)
                    .metadata(metadata)
                    .parsedAt(new Date())
                    .build();

        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
    }

    @Override
    public ParsedDocument parse(MultipartFile file) throws IOException {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    /**
     * 提取工作表为表格
     */
    private ParsedDocument.Table extractSheet(Sheet sheet, int sheetNumber) {
        List<List<String>> rows = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        int firstRow = sheet.getFirstRowNum();
        int lastRow = sheet.getLastRowNum();

        if (firstRow < 0 || lastRow < 0) {
            return null;
        }

        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);

            if (row == null) {
                continue;
            }

            List<String> cells = new ArrayList<>();
            int firstCell = row.getFirstCellNum();
            int lastCell = row.getLastCellNum();

            for (int cellIndex = firstCell; cellIndex < lastCell; cellIndex++) {
                Cell cell = row.getCell(cellIndex);
                String cellValue = getCellValue(cell);
                cells.add(cellValue);
            }

            rows.add(cells);

            if (rowIndex == firstRow) {
                headers = new ArrayList<>(cells);
            }
        }

        return ParsedDocument.Table.builder()
                .pageNumber(sheetNumber)
                .rows(rows)
                .headers(headers)
                .build();
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                return cell.getCellFormula();

            case BLANK:
                return "";

            default:
                return "";
        }
    }
}
