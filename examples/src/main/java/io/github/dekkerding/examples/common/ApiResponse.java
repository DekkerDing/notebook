package io.github.dekkerding.examples.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应格式
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    /**
     * 请求追踪ID
     */
    private String traceId;

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("200")
                .message("Success")
                .data(data)
                .build();
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code("200")
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 失败响应（带数据）
     */
    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 业务错误响应
     */
    public static <T> ApiResponse<T> businessError(String message) {
        return error("BUSINESS_ERROR", message);
    }

    /**
     * 参数错误响应
     */
    public static <T> ApiResponse<T> paramError(String message) {
        return error("PARAM_ERROR", message);
    }

    /**
     * 系统错误响应
     */
    public static <T> ApiResponse<T> systemError(String message) {
        return error("SYSTEM_ERROR", message);
    }

    /**
     * 未找到响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error("NOT_FOUND", message);
    }

    /**
     * 未授权响应
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return error("UNAUTHORIZED", message);
    }

    /**
     * 禁止访问响应
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return error("FORBIDDEN", message);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return "200".equals(this.code);
    }

    /**
     * 判断是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
}
