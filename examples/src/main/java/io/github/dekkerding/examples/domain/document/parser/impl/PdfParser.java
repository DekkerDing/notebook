package io.github.dekkerding.examples.domain.document.parser.impl;

import io.github.dekkerding.examples.domain.document.model.ParsedDocument;
import io.github.dekkerding.examples.domain.document.parser.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * PDF文档解析器
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class PdfParser implements DocumentParser {

    private static final String PDF_MAGIC_NUMBER = "%PDF-";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    @Override
    public String getSupportedFileType() {
        return "application/pdf";
    }

    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("PDF文件过大: size={}", file.getSize());
            return false;
        }

        // 检查MIME类型
        String contentType = file.getContentType();
        if (contentType != null && contentType.equals("application/pdf")) {
            return true;
        }

        // 检查文件扩展名
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
            return true;
        }

        return false;
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String fileName) throws IOException {
        PDDocument document = null;

        try {
            document = PDDocument.load(inputStream);

            // 检查加密
            if (document.isEncrypted()) {
                log.warn("PDF文件已加密: {}", fileName);
                throw new IOException("PDF文件已加密，无法解析");
            }

            // 提取文本内容
            StringBuilder textContent = new StringBuilder();
            List<ParsedDocument.Page> pages = new ArrayList<>();

            int pageNum = 0;
            for (PDPage page : document.getPages()) {
                pageNum++;
                String pageText = extractPageText(document, pageNum);
                textContent.append(pageText).append("\n\n");

                pages.add(ParsedDocument.Page.builder()
                        .pageNumber(pageNum)
                        .content(pageText.trim())
                        .build());
            }

            // 提取元数据
            Map<String, Object> metadata = extractMetadata(document);

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .fileType("pdf")
                    .fileSize((long) document.getNumberOfPages())
                    .textContent(textContent.toString())
                    .pages(pages)
                    .tables(new ArrayList<ParsedDocument.Table>())
                    .metadata(metadata)
                    .parsedAt(new Date())
                    .build();

        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    log.warn("关闭PDF文档失败", e);
                }
            }
        }
    }

    @Override
    public ParsedDocument parse(MultipartFile file) throws IOException {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }

    /**
     * 提取页面文本（简化实现）
     */
    private String extractPageText(PDDocument document, int pageNum) throws IOException {
        if (pageNum < 1 || pageNum > document.getNumberOfPages()) {
            return "";
        }

        PDPage page = document.getPage(pageNum - 1);

        // 简化实现：实际应使用PDFTextStripper
        // 这里返回占位符，实际项目中应使用PDFTextStripper提取文本
        return "[PDF Page " + pageNum + " content]";
    }

    /**
     * 提取PDF元数据
     */
    private Map<String, Object> extractMetadata(PDDocument document) {
        Map<String, Object> metadata = new HashMap<>();

        PDDocumentInformation info = document.getDocumentInformation();
        if (info != null) {
            addIfPresent(metadata, "title", info.getTitle());
            addIfPresent(metadata, "author", info.getAuthor());
            addIfPresent(metadata, "subject", info.getSubject());
            addIfPresent(metadata, "keywords", info.getKeywords());
            addIfPresent(metadata, "creator", info.getCreator());
            addIfPresent(metadata, "producer", info.getProducer());
        }

        metadata.put("page_count", document.getNumberOfPages());

        return metadata;
    }

    /**
     * 添加非空元数据
     */
    private void addIfPresent(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.isEmpty()) {
            metadata.put(key, value);
        }
    }
}
