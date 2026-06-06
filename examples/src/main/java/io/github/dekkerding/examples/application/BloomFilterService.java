package io.github.dekkerding.examples.application;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 布隆过滤器服务（简化版）
 */
@Service
public class BloomFilterService {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterService.class);
    private static final String BF_KEY_PREFIX = "bloom:";

    @Autowired(required = false)
    private RedissonClient redisson;

    public <T> boolean init(String bfKey, long expectedInsertions, double falseProbability) {
        if (redisson == null) return false;
        try {
            RBloomFilter<T> bloomFilter = redisson.getBloomFilter(BF_KEY_PREFIX + bfKey);
            return bloomFilter.tryInit(expectedInsertions, falseProbability);
        } catch (Exception e) {
            log.error("初始化布隆过滤器失败", e);
            return false;
        }
    }

    public <T> boolean add(String bfKey, T element) {
        if (redisson == null) return false;
        try {
            RBloomFilter<T> bloomFilter = redisson.getBloomFilter(BF_KEY_PREFIX + bfKey);
            return bloomFilter.add(element);
        } catch (Exception e) {
            log.error("添加布隆过滤器元素失败", e);
            return false;
        }
    }

    public <T> boolean contains(String bfKey, T element) {
        if (redisson == null) return false;
        try {
            RBloomFilter<T> bloomFilter = redisson.getBloomFilter(BF_KEY_PREFIX + bfKey);
            return bloomFilter.contains(element);
        } catch (Exception e) {
            log.error("检查布隆过滤器失败", e);
            return false;
        }
    }

    public boolean delete(String bfKey) {
        if (redisson == null) return false;
        try {
            RBloomFilter<Object> bloomFilter = redisson.getBloomFilter(BF_KEY_PREFIX + bfKey);
            return bloomFilter.delete();
        } catch (Exception e) {
            log.error("删除布隆过滤器失败", e);
            return false;
        }
    }

    public boolean isSpamEmail(String email) {
        String bfKey = "spam:email";
        if (!exists(bfKey)) {
            init(bfKey, 10000000, 0.0001);
        }
        return contains(bfKey, email);
    }

    public boolean addSpamEmail(String email) {
        String bfKey = "spam:email";
        if (!exists(bfKey)) {
            init(bfKey, 10000000, 0.0001);
        }
        return add(bfKey, email);
    }

    private boolean exists(String bfKey) {
        if (redisson == null) return false;
        try {
            RBloomFilter<Object> bloomFilter = redisson.getBloomFilter(BF_KEY_PREFIX + bfKey);
            return bloomFilter.isExists();
        } catch (Exception e) {
            return false;
        }
    }
}
