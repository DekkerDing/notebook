package io.github.dekkerding.examples.domain.document.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档解析器工厂
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Component
public class DocumentParserFactory {

    private final Map<String, DocumentParser> parserCache = new HashMap<>();
    private final Map<String, String> fileTypeExtensions = new HashMap<>();

    @Autowired
    public DocumentParserFactory(List<DocumentParser> parsers) {
        for (DocumentParser parser : parsers) {
            String fileType = parser.getSupportedFileType();
            parserCache.put(fileType, parser);
        }

        // 初始化文件扩展名映射
        fileTypeExtensions.put("pdf", "application/pdf");
        fileTypeExtensions.put("doc", "application/msword");
        fileTypeExtensions.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        fileTypeExtensions.put("xls", "application/vnd.ms-excel");
        fileTypeExtensions.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /**
     * 根据文件类型获取解析器
     *
     * @param fileType 文件类型（MIME类型）
     * @return 解析器，不支持则返回null
     */
    public DocumentParser getParser(String fileType) {
        return parserCache.get(fileType);
    }

    /**
     * 根据文件扩展名获取解析器
     *
     * @param extension 文件扩展名（如"pdf"）
     * @return 解析器，不支持则返回null
     */
    public DocumentParser getParserByExtension(String extension) {
        String mimeType = fileTypeExtensions.get(extension.toLowerCase());
        if (mimeType == null) {
            return null;
        }
        return getParser(mimeType);
    }

    /**
     * 获取支持的文件类型列表
     *
     * @return 文件类型列表
     */
    public java.util.Set<String> getSupportedFileTypes() {
        return parserCache.keySet();
    }

    /**
     * 检查文件类型是否支持
     *
     * @param fileType 文件类型
     * @return 是否支持
     */
    public boolean isSupported(String fileType) {
        return parserCache.containsKey(fileType);
    }

    /**
     * 检查文件扩展名是否支持
     *
     * @param extension 文件扩展名
     * @return 是否支持
     */
    public boolean isSupportedExtension(String extension) {
        String mimeType = fileTypeExtensions.get(extension.toLowerCase());
        return mimeType != null && isSupported(mimeType);
    }
}
