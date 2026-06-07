package io.github.dekkerding.examples.domain.document.parser.impl;

import io.github.dekkerding.examples.domain.document.model.ParsedDocument;
import io.github.dekkerding.examples.domain.document.parser.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Word文档解析器
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class WordParser implements DocumentParser {

    private static final String DOC_CONTENT_TYPE = "application/msword";
    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Override
    public String getSupportedFileType() {
        return DOCX_CONTENT_TYPE;
    }

    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();

        return DOCX_CONTENT_TYPE.equals(contentType) || DOC_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws IOException {
        // 判断是.doc还是.docx
        if (fileName != null && fileName.toLowerCase().endsWith(".docx")) {
            return parseDocx(inputStream, fileName);
        } else {
            return parseDoc(inputStream, fileName);
        }
    }

    @Override
    public ParsedDocument parse(MultipartFile file) throws IOException {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    /**
     * 解析.docx格式
     */
    private ParsedDocument parseDocx(InputStream inputStream, String fileName) throws IOException {
        XWPFDocument document = null;

        try {
            document = new XWPFDocument(inputStream);

            StringBuilder textContent = new StringBuilder();
            List<ParsedDocument.Page> pages = new ArrayList<>();
            List<ParsedDocument.Table> tables = new ArrayList<>();

            // 提取段落文本
            int pageNumber = 0;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();

                if (text != null && !text.trim().isEmpty()) {
                    textContent.append(text).append("\n");

                    // 简单分页：每10个段落算一页
                    if (pageNumber == 0 || (pages.get(pages.size() - 1).getContent().length() + text.length() > 2000)) {
                        pageNumber++;
                        pages.add(ParsedDocument.Page.builder()
                                .pageNumber(pageNumber)
                                .content(text)
                                .build());
                    } else {
                        pages.get(pages.size() - 1).setContent(
                                pages.get(pages.size() - 1).getContent() + "\n" + text);
                    }
                }
            }

            // 提取表格
            for (XWPFTable table : document.getTables()) {
                ParsedDocument.Table parsedTable = extractTable(table, pageNumber);
                if (parsedTable != null) {
                    tables.add(parsedTable);
                }
            }

            // 提取元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paragraph_count", document.getParagraphs().size());
            metadata.put("table_count", document.getTables().size());

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .fileType("docx")
                    .fileSize((long) textContent.length())
                    .textContent(textContent.toString())
                    .pages(pages)
                    .tables(tables)
                    .metadata(metadata)
                    .parsedAt(new Date())
                    .build();

        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    /**
     * 解析.doc格式
     */
    private ParsedDocument parseDoc(InputStream inputStream, String fileName) throws IOException {
        HWPFDocument document = null;

        try {
            document = new HWPFDocument(inputStream);
            WordExtractor extractor = new WordExtractor(document);

            String textContent = extractor.getText();

            List<ParsedDocument.Page> pages = new ArrayList<>();
            pages.add(ParsedDocument.Page.builder()
                    .pageNumber(1)
                    .content(textContent)
                    .build());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("legacy_format", true);

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .fileType("doc")
                    .fileSize((long) textContent.length())
                    .textContent(textContent)
                    .pages(pages)
                    .tables(new ArrayList<ParsedDocument.Table>())
                    .metadata(metadata)
                    .parsedAt(new Date())
                    .build();

        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    /**
     * 提取表格
     */
    private ParsedDocument.Table extractTable(XWPFTable table, int pageNumber) {
        List<List<String>> rows = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        int rowCount = 0;
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();

            for (int i = 0; i < row.getTableCells().size(); i++) {
                String cellText = row.getCell(i).getText();
                cells.add(cellText);
            }

            rows.add(cells);

            if (rowCount == 0) {
                headers = new ArrayList<>(cells);
            }

            rowCount++;
        }

        return ParsedDocument.Table.builder()
                .pageNumber(pageNumber)
                .rows(rows)
                .headers(headers)
                .build();
    }
}
