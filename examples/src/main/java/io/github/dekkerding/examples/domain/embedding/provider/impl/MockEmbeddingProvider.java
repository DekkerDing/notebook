package io.github.dekkerding.examples.domain.embedding.provider.impl;

import io.github.dekkerding.examples.domain.embedding.model.EmbeddingRequest;
import io.github.dekkerding.examples.domain.embedding.model.EmbeddingResult;
import io.github.dekkerding.examples.domain.embedding.provider.EmbeddingProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock向量化提供商 - 用于测试
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Slf4j
public class MockEmbeddingProvider implements EmbeddingProvider {

    private static final int DIMENSION = 1536;
    private final Random random = new Random();

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public EmbeddingResult embed(String text) {
        return embed(text, null);
    }

    @Override
    public EmbeddingResult embed(String text, EmbeddingRequest request) {
        log.debug("Mock向量化: textLength={}", text != null ? text.length() : 0);

        float[] vector = generateRandomVector(DIMENSION);

        return EmbeddingResult.builder()
                .input(text)
                .embedding(floatArrayToList(vector))
                .dimensions(DIMENSION)
                .model(getName())
                .tokens(estimateTokens(text))
                .build();
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts) {
        return embedBatch(texts, null);
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingRequest request) {
        log.debug("Mock批量向量化: count={}", texts.size());

        List<EmbeddingResult> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embed(text, request));
        }

        return results;
    }

    /**
     * 生成随机向量
     */
    private float[] generateRandomVector(int dimension) {
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = random.nextFloat();
        }

        // 归一化
        normalize(vector);

        return vector;
    }

    /**
     * 向量归一化
     */
    private void normalize(float[] vector) {
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    /**
     * 估算token数
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;

        // 简单估算：中文约1.5字符=1token，英文约4字符=1token
        int chineseChars = 0;
        for (char c : text.toCharArray()) {
            if (isChinese(c)) chineseChars++;
        }

        int chineseTokens = (int) (chineseChars / 1.5);
        int nonChineseTokens = (int) ((text.length() - chineseChars) / 4.0);

        return chineseTokens + nonChineseTokens;
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * float数组转List
     */
    private java.util.List<Float> floatArrayToList(float[] array) {
        java.util.List<Float> list = new java.util.ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }
}
