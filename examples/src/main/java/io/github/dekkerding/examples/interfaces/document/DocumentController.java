package io.github.dekkerding.examples.interfaces.document;

import io.github.dekkerding.examples.common.ApiResponse;
import io.github.dekkerding.examples.domain.document.model.ParsedDocument;
import io.github.dekkerding.examples.domain.document.parser.DocumentParser;
import io.github.dekkerding.examples.domain.document.parser.DocumentParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档解析接口
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    @Autowired
    private DocumentParserFactory parserFactory;

    /**
     * 上传并解析文档
     */
    @PostMapping("/upload")
    public ApiResponse<ParsedDocument> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("接收文档上传: filename={}, size={}, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            // 验证文件
            validateFile(file);

            // 获取解析器
            DocumentParser parser = parserFactory.getParser(file.getContentType());

            if (parser == null) {
                return ApiResponse.error("UNSUPPORTED_TYPE", "不支持的文件类型: " + file.getContentType());
            }

            // 解析文档
            ParsedDocument result = parser.parse(file);

            log.info("文档解析成功: filename={}, pages={}, tables={}",
                    result.getFileName(),
                    result.getPages() != null ? result.getPages().size() : 0,
                    result.getTables() != null ? result.getTables().size() : 0);

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("文档解析失败: filename={}", file.getOriginalFilename(), e);
            return ApiResponse.error("PARSE_ERROR", "文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 获取支持的文件类型
     */
    @GetMapping("/supported-types")
    public ApiResponse<Map<String, Object>> getSupportedTypes() {
        Map<String, Object> result = new HashMap<>();

        result.put("supportedTypes", parserFactory.getSupportedFileTypes());
        result.put("maxFileSize", MAX_FILE_SIZE);
        result.put("extensions", java.util.Arrays.asList(
                "pdf", "doc", "docx", "xls", "xlsx"
        ));

        return ApiResponse.success(result);
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制: " + MAX_FILE_SIZE + "字节");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 检查文件扩展名
        String extension = getFileExtension(filename).toLowerCase();

        if (!parserFactory.isSupportedExtension(extension)) {
            throw new IllegalArgumentException("不支持的文件扩展名: " + extension);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');

        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }

        return "";
    }
}
