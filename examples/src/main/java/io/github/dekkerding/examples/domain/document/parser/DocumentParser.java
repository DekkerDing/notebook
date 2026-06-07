package io.github.dekkerding.examples.domain.document.parser;

import io.github.dekkerding.examples.domain.document.model.ParsedDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析器接口
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
public interface DocumentParser {

    /**
     * 支持的文件类型
     */
    String getSupportedFileType();

    /**
     * 验证文件是否可以被解析
     *
     * @param file 文件
     * @return 是否支持
     */
    boolean supports(MultipartFile file);

    /**
     * 解析文档
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 解析结果
     * @throws IOException 解析异常
     */
    ParsedDocument parse(InputStream inputStream, String fileName) throws IOException;

    /**
     * 解析文档
     *
     * @param file 文件
     * @return 解析结果
     * @throws IOException 解析异常
     */
    ParsedDocument parse(MultipartFile file) throws IOException;
}
