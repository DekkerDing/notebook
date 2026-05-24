package io.github.dekkerding.examples.infrastructure.compression;

import org.xerial.snappy.Snappy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Snappy压缩器实现
 *
 * <p>Snappy是一个快速压缩算法，特点：
 * <ul>
 *   <li>压缩速度：>500MB/s</li>
 *   <li>解压速度：>1GB/s</li>
 *   <li>压缩率：40-60%</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
public class SnappyCompressor {

    /**
     * 压缩数据
     *
     * @param data 原始数据
     * @return 压缩后的数据
     * @throws RuntimeException 压缩失败
     */
    public static byte[] compress(byte[] data) {
        try {
            return Snappy.compress(data);
        } catch (IOException e) {
            throw new RuntimeException("Snappy压缩失败", e);
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
        try {
            return Snappy.uncompress(compressedData);
        } catch (IOException e) {
            throw new RuntimeException("Snappy解压失败", e);
        }
    }

    /**
     * 估算压缩后大小
     *
     * @param originalSize 原始大小
     * @return 估算的压缩后大小（约50%）
     */
    public static int estimateCompressedSize(int originalSize) {
        return originalSize / 2;
    }
}
