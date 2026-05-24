package io.github.dekkerding.examples.infrastructure.compression;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 自适应消息压缩策略
 *
 * <p>根据消息大小自动选择最优压缩算法：
 * <ul>
 *   <li>小消息（<512B）：不压缩（压缩开销大于收益）</li>
 *   <li>中等消息（512B-4KB）：Snappy（快速压缩，40-60%压缩率）</li>
 *   <li>大消息（>4KB）：Gzip（高压缩率，70-85%压缩率）</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class AdaptiveCompressionStrategy {

    private static final int SNAPPY_THRESHOLD = 512;  // 字节
    private static final int GZIP_THRESHOLD = 4096;   // 字节

    /**
     * 压缩消息
     *
     * @param payload 原始消息
     * @return 压缩结果
     */
    public CompressedPayload compress(String payload) {
        if (payload == null || payload.isEmpty()) {
            return CompressedPayload.uncompressed(new byte[0]);
        }

        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        int originalSize = data.length;

        if (originalSize < SNAPPY_THRESHOLD) {
            // 小消息不压缩
            log.trace("消息太小({}B)，跳过压缩", originalSize);
            return CompressedPayload.uncompressed(data);
        } else if (originalSize < GZIP_THRESHOLD) {
            // 中等消息使用Snappy
            byte[] compressed = SnappyCompressor.compress(data);
            double ratio = (1 - (double) compressed.length / originalSize) * 100;

            // 如果压缩后反而变大，则不压缩
            if (compressed.length >= originalSize) {
                log.trace("Snappy压缩后变大({}→{}B)，跳过压缩", originalSize, compressed.length);
                return CompressedPayload.uncompressed(data);
            }

            log.debug("Snappy压缩: {}→{}B ({.1f}%)", originalSize, compressed.length, ratio);
            return new CompressedPayload(CompressionType.SNAPPY, compressed);
        } else {
            // 大消息使用Gzip
            byte[] compressed = GzipCompressor.compress(data);
            double ratio = (1 - (double) compressed.length / originalSize) * 100;

            if (compressed.length >= originalSize) {
                log.trace("Gzip压缩后变大({}→{}B)，跳过压缩", originalSize, compressed.length);
                return CompressedPayload.uncompressed(data);
            }

            log.debug("Gzip压缩: {}→{}B ({.1f}%)", originalSize, compressed.length, ratio);
            return new CompressedPayload(CompressionType.GZIP, compressed);
        }
    }

    /**
     * 解压消息
     *
     * @param payload 压缩后的消息
     * @return 原始消息
     */
    public String decompress(CompressedPayload payload) {
        if (payload == null) {
            return "";
        }

        byte[] data = payload.getData();

        if (payload.getCompressionType() == CompressionType.NONE) {
            return new String(data, StandardCharsets.UTF_8);
        } else if (payload.getCompressionType() == CompressionType.SNAPPY) {
            byte[] decompressed = SnappyCompressor.decompress(data);
            return new String(decompressed, StandardCharsets.UTF_8);
        } else if (payload.getCompressionType() == CompressionType.GZIP) {
            byte[] decompressed = GzipCompressor.decompress(data);
            return new String(decompressed, StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("未知压缩类型: " + payload.getCompressionType());
        }
    }

    /**
     * 压缩结果
     */
    public static class CompressedPayload {
        private final CompressionType compressionType;
        private final byte[] data;

        private CompressedPayload(CompressionType compressionType, byte[] data) {
            this.compressionType = compressionType;
            this.data = data;
        }

        private static CompressedPayload uncompressed(byte[] data) {
            return new CompressedPayload(CompressionType.NONE, data);
        }

        public CompressionType getCompressionType() {
            return compressionType;
        }

        public byte[] getData() {
            return data;
        }

        public int getSize() {
            return data.length;
        }

        public boolean isCompressed() {
            return compressionType != CompressionType.NONE;
        }
    }

    /**
     * 压缩类型枚举
     */
    public enum CompressionType {
        NONE,     // 不压缩
        SNAPPY,   // Snappy压缩
        GZIP      // Gzip压缩
    }
}
