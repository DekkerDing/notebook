package io.github.dekkerding.examples.regression;

import io.github.dekkerding.examples.application.BloomFilterService;
import io.github.dekkerding.examples.application.CardinalityService;
import io.github.dekkerding.examples.application.GeoLocationService;
import io.github.dekkerding.examples.application.LuaScriptService;
import org.redisson.api.GeoPosition;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 高级功能回归测试套件
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("高级功能回归测试")
public class AdvancedFeaturesRegressionTest {

    @Autowired(required = false)
    private GeoLocationService geoLocationService;

    @Autowired(required = false)
    private CardinalityService cardinalityService;

    @Autowired(required = false)
    private BloomFilterService bloomFilterService;

    @Autowired(required = false)
    private LuaScriptService luaScriptService;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(geoLocationService != null, "GeoLocationService 未配置");
        Assumptions.assumeTrue(cardinalityService != null, "CardinalityService 未配置");
        Assumptions.assumeTrue(bloomFilterService != null, "BloomFilterService 未配置");
        Assumptions.assumeTrue(luaScriptService != null, "LuaScriptService 未配置");

        try {
            cardinalityService.add("test:connection:check", "test");
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Redisson")) {
                Assumptions.assumeTrue(false, "Redisson未连接，跳过测试");
            }
        }
    }

    // ==================== 地理位置服务测试 ====================

    @Test
    @Order(1)
    @DisplayName("GEO-001: 添加位置并获取坐标")
    void testAddAndGetLocation() {
        String geoKey = "test:location:add";

        try {
            boolean added = geoLocationService.addLocation(geoKey, "tianamen", 116.404, 39.915);
            assertTrue(added, "应该成功添加位置");

            GeoPosition position = geoLocationService.getPosition(geoKey, "tianamen");
            assertNotNull(position, "位置不应为空");

            System.out.println("GEO-001测试通过: key=" + geoKey);
        } finally {
            geoLocationService.deleteGeoSet(geoKey);
        }
    }

    // ==================== 基数统计测试 ====================

    @Test
    @Order(10)
    @DisplayName("HLL-001: 添加元素并统计基数")
    void testAddAndCount() {
        String hllKey = "test:hll:count";

        try {
            assertTrue(cardinalityService.add(hllKey, "user1"));
            assertTrue(cardinalityService.add(hllKey, "user2"));
            assertFalse(cardinalityService.add(hllKey, "user1"));  // 重复添加

            long count = cardinalityService.count(hllKey);
            assertEquals(2, count, "基数应该是2");

            System.out.println("HLL-001测试通过: key=" + hllKey + ", count=" + count);
        } finally {
            cardinalityService.delete(hllKey);
        }
    }

    @Test
    @Order(11)
    @DisplayName("HLL-002: 日活跃用户统计")
    void testDailyActiveUser() {
        String dateStr = "20260524";

        try {
            assertTrue(cardinalityService.recordDailyActiveUser(dateStr, "user1"));
            assertTrue(cardinalityService.recordDailyActiveUser(dateStr, "user2"));
            assertFalse(cardinalityService.recordDailyActiveUser(dateStr, "user1"));

            long dau = cardinalityService.getDailyActiveUserCount(dateStr);
            assertEquals(2, dau, "DAU应该是2");

            System.out.println("HLL-002测试通过: DAU=" + dau);
        } finally {
            cardinalityService.delete("daily:active:" + dateStr);
        }
    }

    // ==================== 布隆过滤器测试 ====================

    @Test
    @Order(20)
    @DisplayName("BF-001: 初始化并添加元素")
    void testInitAndAdd() {
        String bfKey = "test:bf:init";

        try {
            assertTrue(bloomFilterService.init(bfKey, 1000, 0.01));
            assertTrue(bloomFilterService.add(bfKey, "email1@example.com"));
            assertTrue(bloomFilterService.contains(bfKey, "email1@example.com"));
            assertFalse(bloomFilterService.contains(bfKey, "email2@example.com"));

            System.out.println("BF-001测试通过: key=" + bfKey);
        } finally {
            bloomFilterService.delete(bfKey);
        }
    }

    @Test
    @Order(21)
    @DisplayName("BF-002: 垃圾邮件检测")
    void testSpamDetection() {
        try {
            assertTrue(bloomFilterService.addSpamEmail("spam1@badsite.com"));
            assertTrue(bloomFilterService.isSpamEmail("spam1@badsite.com"));
            assertFalse(bloomFilterService.isSpamEmail("good@example.com"));

            System.out.println("BF-002测试通过");
        } finally {
            bloomFilterService.delete("bloom:spam:email");
        }
    }

    // ==================== Lua脚本测试 ====================

    @Test
    @Order(30)
    @DisplayName("LUA-001: 执行简单脚本")
    void testExecuteSimpleScript() {
        String script = "return redis.call('SET', KEYS[1], ARGV[1])";

        Object result = luaScriptService.execute(
                script,
                java.util.Collections.singletonList("lua:test:key"),
                java.util.Collections.singletonList("test-value")
        );

        assertNotNull(result, "脚本执行结果不应为空");
        System.out.println("LUA-001测试通过: result=" + result);
    }

    @Test
    @Order(31)
    @DisplayName("LUA-002: 脚本Digest管理")
    void testScriptDigest() {
        String script = "return redis.call('GET', KEYS[1])";

        String digest = luaScriptService.loadScript(script);
        assertNotNull(digest, "Digest不应为空");
        assertEquals(40, digest.length(), "Digest应该是40位SHA1");

        System.out.println("LUA-002测试通过: digest=" + digest);
    }

    @Test
    @Order(32)
    @DisplayName("LUA-003: 预定义锁脚本")
    void testPredefinedLockScript() {
        String lockKey = "lua:lock:test";

        try {
            boolean locked = luaScriptService.executeLockScript(lockKey, "lock-value", 5000);
            assertTrue(locked, "应该成功获取锁");

            boolean lockedAgain = luaScriptService.executeLockScript(lockKey, "lock-value2", 5000);
            assertFalse(lockedAgain, "锁已被占用，应该失败");

            System.out.println("LUA-003测试通过");
        } finally {
            luaScriptService.execute(
                    "return redis.call('DEL', KEYS[1])",
                    java.util.Collections.singletonList(lockKey),
                    java.util.Collections.emptyList()
            );
        }
    }
}
