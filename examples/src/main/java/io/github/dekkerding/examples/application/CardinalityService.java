package io.github.dekkerding.examples.application;

import org.redisson.api.RHyperLogLog;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基数统计服务（简化版）
 */
@Service
public class CardinalityService {

    private static final Logger log = LoggerFactory.getLogger(CardinalityService.class);
    private static final String HLL_KEY_PREFIX = "hll:";

    @Autowired(required = false)
    private RedissonClient redisson;

    public boolean add(String hllKey, Object element) {
        if (redisson == null) return false;
        try {
            RHyperLogLog<Object> hll = redisson.getHyperLogLog(HLL_KEY_PREFIX + hllKey);
            return hll.add(element);
        } catch (Exception e) {
            log.error("添加HLL元素失败", e);
            return false;
        }
    }

    public long count(String hllKey) {
        if (redisson == null) return 0;
        try {
            RHyperLogLog<Object> hll = redisson.getHyperLogLog(HLL_KEY_PREFIX + hllKey);
            return hll.count();
        } catch (Exception e) {
            log.error("统计HLL基数失败", e);
            return 0;
        }
    }

    public boolean delete(String hllKey) {
        if (redisson == null) return false;
        try {
            RHyperLogLog<Object> hll = redisson.getHyperLogLog(HLL_KEY_PREFIX + hllKey);
            return hll.delete();
        } catch (Exception e) {
            log.error("删除HLL失败", e);
            return false;
        }
    }

    public boolean recordDailyActiveUser(String dateStr, String userId) {
        return add("daily:active:" + dateStr, userId);
    }

    public long getDailyActiveUserCount(String dateStr) {
        return count("daily:active:" + dateStr);
    }
}
