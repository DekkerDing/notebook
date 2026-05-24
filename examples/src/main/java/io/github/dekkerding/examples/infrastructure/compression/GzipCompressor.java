package io.github.dekkerding.examples.infrastructure.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip压缩器实现
 *
 * <p>Gzip是一个高压缩率算法，特点：
 * <ul>
 *   <li>压缩速度：~50MB/s</li>
 *   <li>解压速度：~100MB/s</li>
 *   <li>压缩率：70-85%</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
public class GzipCompressor {

    private static final int BUFFER_SIZE = 8192;

    /**
     * 压缩数据
     *
     * @param data 原始数据
     * @return 压缩后的数据
     * @throws RuntimeException 压缩失败
     */
    public static byte[] compress(byte[] data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gos = new GZIPOutputStream(bos)) {

            gos.write(data);
            gos.finish();
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Gzip压缩失败", e);
        }
    }

    /**
     * 解压数据
     *
     * @param compressedData 压缩的数据
     * @return 原始数据
     * @throws RuntimeException 解压失败
     */
    public static byte[] decompress(byte[] compressedData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPInputStream gis = new GZIPInputStream(bis)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Gzip解压失败", e);
        }
    }

    /**
     * 估算压缩后大小
     *
     * @param originalSize 原始大小
     * @return 估算的压缩后大小（约25%）
     */
    public static int estimateCompressedSize(int originalSize) {
        return originalSize / 4;
    }
}
