package io.github.dekkerding.examples.regression;

import io.github.dekkerding.examples.application.TwoLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis中文支持测试
 *
 * <p>测试目标：验证Redis中中文key和value的正确存储和显示
 *
 * <p>测试范围：
 * <ul>
 *   <li>中文key的缓存操作</li>
 *   <li>中文value的缓存操作</li>
 *   <li>混合中英文的缓存操作</li>
 *   <li>特殊Unicode字符的支持</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Redis中文支持测试")
public class ChineseCharacterSupportTest {

    @Autowired(required = false)
    private TwoLevelCacheService cacheService;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(cacheService != null, "TwoLevelCacheService 未配置");
    }

    @Test
    @Order(1)
    @DisplayName("ZH-CN-001: 中文key缓存操作")
    void testChineseKey() {
        try {
            // 使用中文key
            String chineseKey = "用户:123";
            TestUser user = new TestUser("user-123", "张三");

            // 设置缓存
            cacheService.set(chineseKey, user, 5, TimeUnit.MINUTES);
            log.info("设置中文key缓存成功: key={}", chineseKey);

            // 获取缓存
            TestUser cached = cacheService.get(chineseKey, TestUser.class);
            assertNotNull(cached, "缓存不应为空");
            assertEquals("user-123", cached.getId(), "用户ID应该匹配");
            assertEquals("张三", cached.getName(), "用户名应该匹配");
            log.info("获取中文key缓存成功: user={}", cached);

            // 检查存在性
            assertTrue(cacheService.exists(chineseKey), "缓存应该存在");

            // 删除缓存
            cacheService.delete(chineseKey);
            TestUser deleted = cacheService.get(chineseKey, TestUser.class);
            assertNull(deleted, "删除后缓存应该为空");

            log.info("中文key缓存操作测试通过: key={}", chineseKey);
        } catch (Exception e) {
            log.error("中文key缓存操作测试失败", e);
            fail("中文key缓存操作测试失败: " + e.getMessage());
        } finally {
            try {
                cacheService.delete("用户:123");
            } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(2)
    @DisplayName("ZH-CN-002: 中文value缓存操作")
    void testChineseValue() {
        try {
            String key = "user:chinese:001";
            // 使用包含中文的value
            TestUser user = new TestUser("CN001", "李四");
            user.setAddress("北京市朝阳区");
            user.setRemark("这是一个测试用户");

            // 设置缓存
            cacheService.set(key, user, 5, TimeUnit.MINUTES);
            log.info("设置中文value缓存成功: key={}", key);

            // 获取缓存
            TestUser cached = cacheService.get(key, TestUser.class);
            assertNotNull(cached, "缓存不应为空");
            assertEquals("CN001", cached.getId(), "用户ID应该匹配");
            assertEquals("李四", cached.getName(), "用户名应该匹配");
            assertEquals("北京市朝阳区", cached.getAddress(), "地址应该匹配");
            assertEquals("这是一个测试用户", cached.getRemark(), "备注应该匹配");
            log.info("获取中文value缓存成功: user={}", cached);

            log.info("中文value缓存操作测试通过: key={}", key);
        } catch (Exception e) {
            log.error("中文value缓存操作测试失败", e);
            fail("中文value缓存操作测试失败: " + e.getMessage());
        } finally {
            cacheService.delete("user:chinese:001");
        }
    }

    @Test
    @Order(3)
    @DisplayName("ZH-CN-003: 混合中英文缓存操作")
    void testMixedChineseEnglish() {
        try {
            // 混合中英文key和value
            String key = "cache:用户:test:001";
            TestUser user = new TestUser("MIX-001", "测试用户");
            user.setAddress("Test Address: 测试地址");
            user.setRemark("混合中英文Mixed Chinese and English");

            // 设置缓存
            cacheService.set(key, user, 5, TimeUnit.MINUTES);
            log.info("设置混合中英文缓存成功: key={}", key);

            // 获取缓存
            TestUser cached = cacheService.get(key, TestUser.class);
            assertNotNull(cached, "缓存不应为空");
            assertEquals("MIX-001", cached.getId(), "用户ID应该匹配");
            assertEquals("测试用户", cached.getName(), "用户名应该匹配");
            assertEquals("Test Address: 测试地址", cached.getAddress(), "地址应该匹配");
            assertEquals("混合中英文Mixed Chinese and English", cached.getRemark(), "备注应该匹配");
            log.info("获取混合中英文缓存成功: user={}", cached);

            log.info("混合中英文缓存操作测试通过: key={}", key);
        } catch (Exception e) {
            log.error("混合中英文缓存操作测试失败", e);
            fail("混合中英文缓存操作测试失败: " + e.getMessage());
        } finally {
            cacheService.delete("cache:用户:test:001");
        }
    }

    @Test
    @Order(4)
    @DisplayName("ZH-CN-004: 特殊Unicode字符支持")
    void testSpecialUnicodeCharacters() {
        try {
            String key = "cache:unicode:测试:emoji:😀";
            TestUser user = new TestUser("UNI-001", "用户😀");
            user.setAddress("地址🏠");
            user.setRemark("特殊字符Special Characters!@#$%^&*()");

            // 设置缓存
            cacheService.set(key, user, 5, TimeUnit.MINUTES);
            log.info("设置特殊Unicode字符缓存成功: key={}", key);

            // 获取缓存
            TestUser cached = cacheService.get(key, TestUser.class);
            assertNotNull(cached, "缓存不应为空");
            assertEquals("UNI-001", cached.getId(), "用户ID应该匹配");
            assertEquals("用户😀", cached.getName(), "用户名应该匹配");
            assertEquals("地址🏠", cached.getAddress(), "地址应该匹配");
            assertEquals("特殊字符Special Characters!@#$%^&*()", cached.getRemark(), "备注应该匹配");
            log.info("获取特殊Unicode字符缓存成功: user={}", cached);

            log.info("特殊Unicode字符支持测试通过: key={}", key);
        } catch (Exception e) {
            log.error("特殊Unicode字符支持测试失败", e);
            fail("特殊Unicode字符支持测试失败: " + e.getMessage());
        } finally {
            cacheService.delete("cache:unicode:测试:emoji:😀");
        }
    }

    @Test
    @Order(5)
    @DisplayName("ZH-CN-005: 批量中文缓存操作")
    void testBatchChineseCacheOperations() {
        try {
            // 批量设置中文缓存
            for (int i = 1; i <= 5; i++) {
                String key = "用户:batch:" + i;
                TestUser user = new TestUser("BATCH-" + i, "批量用户" + i);
                cacheService.set(key, user, 5, TimeUnit.MINUTES);
            }

            // 批量获取并验证
            int successCount = 0;
            for (int i = 1; i <= 5; i++) {
                String key = "用户:batch:" + i;
                TestUser user = cacheService.get(key, TestUser.class);
                if (user != null && user.getName().equals("批量用户" + i)) {
                    successCount++;
                }
            }

            assertEquals(5, successCount, "应该成功获取所有5个用户");

            // 清理
            for (int i = 1; i <= 5; i++) {
                cacheService.delete("用户:batch:" + i);
            }

            log.info("批量中文缓存操作测试通过: count={}", successCount);
        } catch (Exception e) {
            log.error("批量中文缓存操作测试失败", e);
            fail("批量中文缓存操作测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("ZH-CN-006: 中文前缀删除操作")
    void testChinesePrefixDelete() {
        try {
            // 准备测试数据
            String[] keys = {
                    "测试:delete:1",
                    "测试:delete:2",
                    "测试:delete:3"
            };

            for (String key : keys) {
                cacheService.set(key, new TestUser(key, "Test"));
            }

            // 按前缀删除
            cacheService.deleteByPrefix("测试:delete:");

            // 验证删除
            for (String key : keys) {
                assertFalse(cacheService.exists(key), "缓存应该被删除: " + key);
            }

            log.info("中文前缀删除操作测试通过");
        } catch (Exception e) {
            log.error("中文前缀删除操作测试失败", e);
            fail("中文前缀删除操作测试失败: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("ZH-CN-007: 带加载器的中文缓存")
    void testChineseCacheWithLoader() {
        try {
            String key = "用户:loader:001";

            // 第一次获取：使用加载器
            TestUser user1 = cacheService.get(key, TestUser.class, () -> {
                return new TestUser("LOAD-001", "加载用户");
            });

            assertNotNull(user1, "加载的用户不应为空");
            assertEquals("加载用户", user1.getName(), "应该返回加载的数据");
            log.info("第一次加载中文缓存: user={}", user1);

            // 第二次获取：应该命中缓存
            TestUser user2 = cacheService.get(key, TestUser.class, () -> {
                return new TestUser("SHOULD-NOT-LOAD", "不应加载");
            });

            assertEquals("加载用户", user2.getName(), "应该返回缓存的数据");
            log.info("第二次获取中文缓存（缓存命中）: user={}", user2);

            log.info("带加载器的中文缓存测试通过: key={}", key);
        } catch (Exception e) {
            log.error("带加载器的中文缓存测试失败", e);
            fail("带加载器的中文缓存测试失败: " + e.getMessage());
        } finally {
            cacheService.delete("用户:loader:001");
        }
    }

    // ==================== 测试模型类 ====================

    static class TestUser {
        private String id;
        private String name;
        private String address;
        private String remark;

        public TestUser() {
        }

        public TestUser(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
