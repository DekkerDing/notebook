package io.github.dekkerding.examples.infrastructure.elasticsearch;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

/**
 * 向量序列化工具类
 *
 * <p>优化向量数据的存储和传输，使用二进制格式替代JSON数组</p>
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
public class VectorSerializer {

    /**
     * 将float数组序列化为二进制
     *
     * @param vector 向量数组
     * @return 二进制数据
     */
    public static byte[] serialize(float[] vector) {
        if (vector == null) {
            return null;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(vector.length * 4);
            DataOutputStream dos = new DataOutputStream(baos);

            for (float v : vector) {
                dos.writeFloat(v);
            }

            dos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("向量序列化失败", e);
            return null;
        }
    }

    /**
     * 将二进制反序列化为float数组
     *
     * @param data 二进制数据
     * @return 向量数组
     */
    public static float[] deserialize(byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            int vectorSize = data.length / 4;
            float[] vector = new float[vectorSize];

            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

            for (int i = 0; i < vectorSize; i++) {
                vector[i] = dis.readFloat();
            }

            return vector;
        } catch (Exception e) {
            log.error("向量反序列化失败", e);
            return null;
        }
    }

    /**
     * 将float数组转换为ByteBuffer（ES专用）
     *
     * @param vector 向量数组
     * @return ByteBuffer
     */
    public static ByteBuffer toByteBuffer(float[] vector) {
        if (vector == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4);
        for (float v : vector) {
            buffer.putFloat(v);
        }
        buffer.flip();
        return buffer;
    }

    /**
     * 从ByteBuffer转换为float数组（ES专用）
     *
     * @param buffer ByteBuffer
     * @return 向量数组
     */
    public static float[] fromByteBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }

        int vectorSize = buffer.remaining() / 4;
        float[] vector = new float[vectorSize];

        for (int i = 0; i < vectorSize; i++) {
            vector[i] = buffer.getFloat();
        }

        return vector;
    }

    /**
     * 获取序列化后的字节大小
     *
     * @param vector 向量数组
     * @return 字节大小
     */
    public static int getSerializedSize(float[] vector) {
        return vector != null ? vector.length * 4 : 0;
    }
}
