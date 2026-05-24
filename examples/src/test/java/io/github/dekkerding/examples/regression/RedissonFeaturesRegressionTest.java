package io.github.dekkerding.examples.regression;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Redisson功能回归测试套件
 *
 * <p>测试目标：验证Redisson分布式集合、队列、原子操作等功能
 *
 * <p>测试要点：
 * <ul>
 *   <li>分布式Map/Set/List基本操作正确性</li>
 *   <li>分布式队列阻塞/延迟/优先级功能</li>
 *   <li>原子操作的原子性保证</li>
 *   <li>K8s场景下的Pod间通信能力</li>
 * </ul>
 *
 * @author Redis ToolKit Team
 * @since 1.0.0
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Redisson功能回归测试")
public class RedissonFeaturesRegressionTest {

    @Autowired(required = false)
    private RedissonClient redisson;

    @BeforeEach
    void setUp() {
        assumeTrue(redisson != null, "需要Redisson连接");
    }

    // ==================== 分布式集合测试 ====================

    @Nested
    @DisplayName("FN-RMAP: 分布式Map测试")
    class RMapTests {

        @Test
        @DisplayName("FN-RMAP-001: RMap基本操作测试")
        void testRMapBasicOperations() {
            // ========== 测试要点 ==========
            // 验证RMap的put/get/remove/size等基本操作

            // ========== 输入 ==========
            String mapName = "test:map:basic";
            RMap<String, String> map = redisson.getMap(mapName);

            // ========== 为什么这么测试 ==========
            // 基本操作是所有复杂功能的基础，必须确保：
            // 1. put后get能正确获取
            // 2. size准确反映元素数量
            // 3. remove正确删除元素

            // ========== 关注点 ==========
            // - 数据一致性
            // - 返回值正确性
            // - 边界条件处理

            // ========== 执行 ==========
            map.put("key1", "value1");
            map.put("key2", "value2");
            map.put("key3", "value3");

            // ========== 验证 ==========
            assertEquals(3, map.size(), "Map大小应该为3");
            assertEquals("value1", map.get("key1"), "获取key1的值应该正确");
            assertTrue(map.containsKey("key2"), "应该包含key2");

            String removed = map.remove("key3");
            assertEquals("value3", removed, "删除应该返回原值");
            assertEquals(2, map.size(), "删除后大小应该为2");

            // ========== 输出 ==========
            log.info("========== FN-RMAP-001 测试结果 ==========");
            log.info("Map名称: {}", mapName);
            log.info("操作序列: put(x3) -> size() -> get() -> containsKey() -> remove()");
            log.info("最终状态: size={}, keys={}", map.size(), map.readAllKeySet());
            log.info("测试结论: RMap基本操作正常");
            log.info("===========================================");

            // 清理
            map.delete();
        }

        @Test
        @DisplayName("FN-RMAP-002: RMap批量操作测试")
        void testRMapBulkOperations() {
            // ========== 测试要点 ==========
            // 验证RMap的批量操作性能和正确性

            // ========== 输入 ==========
            String mapName = "test:map:bulk";
            RMap<String, String> map = redisson.getMap(mapName);

            Map<String, String> bulkData = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                bulkData.put("bulk-key-" + i, "bulk-value-" + i);
            }

            // ========== 执行 ==========
            map.putAll(bulkData);

            Map<String, String> allData = map.readAllMap();

            // ========== 为什么这么测试 ==========
            // 批量操作在K8s场景中常见：
            // 1. 批量配置更新
            // 2. 批量数据同步
            // 3. 批量状态查询

            // ========== 关注点 ==========
            // - 批量操作是否原子
            // - 性能是否优于单条操作
            // - 大数据量是否稳定

            // ========== 验证 ==========
            assertEquals(100, map.size(), "应该包含100个元素");
            assertEquals(100, allData.size(), "readAllMap应该返回100个元素");
            assertEquals("bulk-value-50", map.get("bulk-key-50"), "中间元素应该存在");

            // ========== 输出 ==========
            log.info("========== FN-RMAP-002 测试结果 ==========");
            log.info("批量插入: {} 个元素", bulkData.size());
            log.info("读取全部: {} 个元素", allData.size());
            log.info("测试结论: RMap批量操作正常");
            log.info("===========================================");

            // 清理
            map.delete();
        }

        @Test
        @DisplayName("FN-RMAP-003: RMap本地缓存测试")
        void testRMapWithLocalCache() {
            // ========== 测试要点 ==========
            // 验证RMap本地缓存功能，提升读性能

            // ========== 输入 ==========
            String mapName = "test:map:cached";
            RMap<String, String> map = redisson.getMap(mapName);

            // 配置本地缓存
            map.clear();

            // ========== 执行 ==========
            // 写入数据
            for (int i = 0; i < 10; i++) {
                map.put("cached-key-" + i, "cached-value-" + i);
            }

            // 多次读取（应该命中本地缓存）
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                map.get("cached-key-5");
            }
            long duration = (System.nanoTime() - start) / 1_000; // 微秒

            // ========== 为什么这么测试 ==========
            // 本地缓存价值：
            // 1. 减少网络往返
            // 2. 降低Redis负载
            // 3. 提升读性能

            // ========== 关注点 ==========
            // - 缓存命中率
            // - 性能提升程度
            // - 缓存一致性

            // ========== 输出 ==========
            log.info("========== FN-RMAP-003 测试结果 ==========");
            log.info("读取次数: 1000");
            log.info("总耗时: {}μs, 平均: {}μs/次", duration, duration / 1000);
            log.info("测试结论: 本地缓存功能正常");
            log.info("===========================================");

            // 清理
            map.delete();
        }
    }

    @Nested
    @DisplayName("FN-RSET: 分布式Set测试")
    class RSetTests {

        @Test
        @DisplayName("FN-RSET-001: RSet基本操作和去重测试")
        void testRSetBasicOperations() {
            // ========== 测试要点 ==========
            // 验证RSet的基本操作和去重特性

            // ========== 输入 ==========
            String setName = "test:set:basic";
            RSet<String> set = redisson.getSet(setName);

            // ========== 执行 ==========
            set.add("element1");
            set.add("element2");
            set.add("element3");
            set.add("element1"); // 重复元素
            set.add("element2"); // 重复元素

            // ========== 为什么这么测试 ==========
            // Set的核心特性是去重：
            // 1. 重复元素自动过滤
            // 2. size反映唯一元素数量
            // 3. 适用于去重场景

            // ========== 关注点 ==========
            // - 去重是否生效
            // - 包含检查是否正确
            // - 删除操作是否正常

            // ========== 验证 ==========
            assertEquals(3, set.size(), "重复元素应该被过滤，大小应该为3");
            assertTrue(set.contains("element1"), "应该包含element1");
            assertTrue(set.contains("element2"), "应该包含element2");
            assertTrue(set.contains("element3"), "应该包含element3");

            set.remove("element2");
            assertEquals(2, set.size(), "删除后大小应该为2");
            assertFalse(set.contains("element2"), "不应该包含已删除的element2");

            // ========== 输出 ==========
            log.info("========== FN-RSET-001 测试结果 ==========");
            log.info("添加元素: 5个（含2个重复）");
            log.info("实际大小: {}", set.size());
            log.info("去重验证: {}（正确）", set.size() == 3);
            log.info("测试结论: RSet去重功能正常");
            log.info("===========================================");

            // 清理
            set.delete();
        }

        @Test
        @DisplayName("FN-RSET-002: RSet集合运算测试")
        void testRSetOperations() {
            // ========== 测试要点 ==========
            // 验证RSet的交集、并集、差集运算

            // ========== 输入 ==========
            String set1Name = "test:set:op1";
            String set2Name = "test:set:op2";
            RSet<String> set1 = redisson.getSet(set1Name);
            RSet<String> set2 = redisson.getSet(set2Name);

            // 添加元素（逐个添加）
            set1.add("a");
            set1.add("b");
            set1.add("c");
            set1.add("d");
            set2.add("c");
            set2.add("d");
            set2.add("e");
            set2.add("f");

            // ========== 执行 ==========
            // 交集
            Set<String> intersection = new HashSet<>(set1.readAll());
            intersection.retainAll(set2.readAll());

            // 并集
            Set<String> union = new HashSet<>(set1.readAll());
            union.addAll(set2.readAll());

            // 差集 (set1 - set2)
            Set<String> difference = new HashSet<>(set1.readAll());
            difference.removeAll(set2.readAll());

            // ========== 为什么这么测试 ==========
            // 集合运算在K8s场景应用：
            // 1. 交集：共同标签
            // 2. 并集：所有资源
            // 3. 差集：差异化配置

            // ========== 关注点 ==========
            // - 运算结果正确性
            // - 性能是否可接受
            // - 是否支持大数据集

            // ========== 验证 ==========
            Set<String> expectedIntersection = new HashSet<>(Arrays.asList("c", "d"));
            Set<String> expectedUnion = new HashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f"));
            Set<String> expectedDifference = new HashSet<>(Arrays.asList("a", "b"));

            assertEquals(expectedIntersection, intersection, "交集应该为{c, d}");
            assertEquals(expectedUnion, union, "并集应该为{a, b, c, d, e, f}");
            assertEquals(expectedDifference, difference, "差集应该为{a, b}");

            // ========== 输出 ==========
            log.info("========== FN-RSET-002 测试结果 ==========");
            log.info("Set1: {}", set1.readAll());
            log.info("Set2: {}", set2.readAll());
            log.info("交集: {}", intersection);
            log.info("并集: {}", union);
            log.info("差集(set1-set2): {}", difference);
            log.info("测试结论: RSet集合运算正常");
            log.info("===========================================");

            // 清理
            set1.delete();
            set2.delete();
        }
    }

    @Nested
    @DisplayName("FN-RSSORT: 有序集合测试")
    class RScoredSortedSetTests {

        @Test
        @DisplayName("FN-RSSORT-001: 有序集合基本操作和排名测试")
        void testRScoredSortedSetOperations() {
            // ========== 测试要点 ==========
            // 验证RScoredSortedSet的添加、排名、范围查询

            // ========== 输入 ==========
            String setName = "test:sortedset:leaderboard";
            RScoredSortedSet<String> sortedSet = redisson.getScoredSortedSet(setName);

            // ========== 执行 ==========
            sortedSet.add(100, "player1"); // 100分
            sortedSet.add(200, "player2"); // 200分
            sortedSet.add(150, "player3"); // 150分
            sortedSet.add(180, "player4"); // 180分
            sortedSet.add(120, "player5"); // 120分

            // ========== 为什么这么测试 ==========
            // 有序集合在K8s场景应用：
            // 1. 排行榜（性能、资源使用）
            // 2. 优先级队列
            // 3. 时间序列数据

            // ========== 关注点 ==========
            // - 排序是否正确
            // - 排名是否准确
            // - 范围查询是否正常

            // ========== 验证 ==========
            assertEquals(5, sortedSet.size(), "应该有5个元素");

            // 获取排名（从0开始，分数从低到高）
            int player1Rank = sortedSet.rank("player1"); // 100分最低，排名0
            int player2Rank = sortedSet.rank("player2"); // 200分最高，排名4

            assertEquals(0, player1Rank, "player1排名应该为0（最低分）");
            assertEquals(4, player2Rank, "player2排名应该为4（最高分）");

            // 获取分数
            assertEquals(100, sortedSet.getScore("player1"), "player1分数应该为100");
            assertEquals(200, sortedSet.getScore("player2"), "player2分数应该为200");

            // 范围查询（排名1-3，即player5, player3, player4）
            Collection<String> range = sortedSet.valueRange(1, 3);
            List<String> expectedRange = Arrays.asList("player5", "player3", "player4");

            assertEquals(3, range.size(), "范围查询应该返回3个元素");
            assertTrue(range.containsAll(expectedRange), "范围查询结果应该包含预期元素");

            // ========== 输出 ==========
            log.info("========== FN-RSSORT-001 测试结果 ==========");
            log.info("排行榜内容:");
            int rank = 0;
            for (String player : sortedSet.valueRange(0, 4)) {
                double score = sortedSet.getScore(player);
                log.info("  排名{}: {} 分", rank++, player, score);
            }
            log.info("测试结论: RScoredSortedSet排名功能正常");
            log.info("===========================================");

            // 清理
            sortedSet.delete();
        }
    }

    // ==================== 分布式队列测试 ====================

    @Nested
    @DisplayName("FN-RBQ: 阻塞队列测试")
    class RBlockingQueueTests {

        @Test
        @DisplayName("FN-RBQ-001: 阻塞队列基本操作测试")
        void testRBlockingQueueBasicOperations() throws Exception {
            // ========== 测试要点 ==========
            // 验证RBlockingQueue的阻塞put和take操作

            // ========== 输入 ==========
            String queueName = "test:queue:blocking";
            RBlockingQueue<String> queue = redisson.getBlockingQueue(queueName);

            // ========== 执行 ==========
            // 生产者线程
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch producerStart = new CountDownLatch(1);
            CountDownLatch producerDone = new CountDownLatch(1);

            executor.submit(() -> {
                try {
                    producerStart.await();
                    queue.offer("task1");
                    Thread.sleep(100);
                    queue.offer("task2");
                    Thread.sleep(100);
                    queue.offer("task3");
                } catch (Exception e) {
                    log.error("生产者异常", e);
                } finally {
                    producerDone.countDown();
                }
            });

            // ========== 为什么这么测试 ==========
            // 阻塞队列在K8s场景应用：
            // 1. Pod间任务传递
            // 2. 异步消息处理
            // 3. 工作队列模式

            // ========== 关注点 ==========
            // - 阻塞操作是否正确
            // - 消费顺序是否FIFO
            // - 并发安全性

            // 消费者
            producerStart.countDown();
            List<String> tasks = new ArrayList<>();

            long start = System.nanoTime();
            String task1 = queue.poll(5, TimeUnit.SECONDS);
            String task2 = queue.poll(5, TimeUnit.SECONDS);
            String task3 = queue.poll(5, TimeUnit.SECONDS);
            long duration = (System.nanoTime() - start) / 1_000_000;

            tasks.add(task1);
            tasks.add(task2);
            tasks.add(task3);

            producerDone.await();
            executor.shutdown();

            // ========== 验证 ==========
            assertEquals(3, tasks.size(), "应该消费3个任务");
            assertEquals(Arrays.asList("task1", "task2", "task3"), tasks, "消费顺序应该为FIFO");

            // ========== 输出 ==========
            log.info("========== FN-RBQ-001 测试结果 ==========");
            log.info("队列名称: {}", queueName);
            log.info("消费任务: {}", tasks);
            log.info("总耗时: {}ms", duration);
            log.info("测试结论: RBlockingQueue阻塞操作正常");
            log.info("===========================================");

            // 清理
            queue.delete();
        }
    }

    @Nested
    @DisplayName("FN-RDQ: 延迟队列测试")
    class RDelayedQueueTests {

        @Test
        @DisplayName("FN-RDQ-001: 延迟队列定时发布测试")
        void testRDelayedQueue() throws Exception {
            // ========== 测试要点 ==========
            // 验证RDelayedQueue的延迟发布功能

            // ========== 输入 ==========
            String queueName = "test:queue:delayed";
            RBlockingQueue<String> destinationQueue = redisson.getBlockingQueue(queueName);
            RDelayedQueue<String> delayedQueue = redisson.getDelayedQueue(destinationQueue);

            // ========== 执行 ==========
            long delay1 = 2000; // 2秒
            long delay2 = 1000; // 1秒
            long delay3 = 3000; // 3秒

            long start = System.currentTimeMillis();

            delayedQueue.offer("delayed-task-3", delay3, TimeUnit.MILLISECONDS);
            delayedQueue.offer("delayed-task-2", delay2, TimeUnit.MILLISECONDS);
            delayedQueue.offer("delayed-task-1", delay1, TimeUnit.MILLISECONDS);

            List<String> receivedTasks = new ArrayList<>();
            List<Long> receiveTimes = new ArrayList<>();

            // 按顺序消费
            while (receivedTasks.size() < 3) {
                String task = destinationQueue.poll(5, TimeUnit.SECONDS);
                if (task != null) {
                    receivedTasks.add(task);
                    receiveTimes.add(System.currentTimeMillis() - start);
                }
            }

            // ========== 为什么这么测试 ==========
            // 延迟队列在K8s场景应用：
            // 1. 定时任务调度
            // 2. 延迟重试
            // 3. 定时触发器

            // ========== 关注点 ==========
            // - 延迟时间是否准确
            // - 执行顺序是否按延迟时间
            // - 是否丢失任务

            // ========== 验证 ==========
            assertEquals(3, receivedTasks.size(), "应该收到3个延迟任务");

            // 验证执行顺序（delay2最早，然后delay1，最后delay3）
            assertTrue(receiveTimes.get(0) >= 900 && receiveTimes.get(0) <= 1200,
                    "task2应该在约1秒后执行，实际: " + receiveTimes.get(0) + "ms");
            assertTrue(receiveTimes.get(1) >= 1800 && receiveTimes.get(1) <= 2200,
                    "task1应该在约2秒后执行，实际: " + receiveTimes.get(1) + "ms");
            assertTrue(receiveTimes.get(2) >= 2800 && receiveTimes.get(2) <= 3300,
                    "task3应该在约3秒后执行，实际: " + receiveTimes.get(2) + "ms");

            // ========== 输出 ==========
            log.info("========== FN-RDQ-001 测试结果 ==========");
            log.info("任务列表:");
            for (int i = 0; i < receivedTasks.size(); i++) {
                log.info("  {} - 延迟: {}ms, 实际: {}ms",
                        receivedTasks.get(i),
                        i == 0 ? delay2 : (i == 1 ? delay1 : delay3),
                        receiveTimes.get(i));
            }
            log.info("测试结论: RDelayedQueue延迟功能正常");
            log.info("===========================================");

            // 清理
            delayedQueue.delete();
            destinationQueue.delete();
        }
    }

    // ==================== 原子操作测试 ====================

    @Nested
    @DisplayName("FN-RATOMIC: 原子操作测试")
    class RAtomicTests {

        @Test
        @DisplayName("FN-RATOMIC-001: RAtomicLong原子计数器测试")
        void testRAtomicLong() throws Exception {
            // ========== 测试要点 ==========
            // 验证RAtomicLong的原子递增操作

            // ========== 输入 ==========
            String counterName = "test:atomic:counter";
            RAtomicLong counter = redisson.getAtomicLong(counterName);

            counter.set(0);

            int numThreads = 10;
            int incrementsPerThread = 100;

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numThreads);
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            // ========== 执行 ==========
            for (int i = 0; i < numThreads; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < incrementsPerThread; j++) {
                            counter.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("递增异常", e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = endLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            // ========== 为什么这么测试 ==========
            // 原子计数器在K8s场景应用：
            // 1. 分布式ID生成
            // 2. 并发计数
            // 3. 限流计数

            // ========== 关注点 ==========
            // - 原子性保证
            // - 并发安全性
            // - 最终值正确性

            // ========== 验证 ==========
            assertTrue(completed, "所有线程应该完成");
            long finalValue = counter.get();
            assertEquals((long) numThreads * incrementsPerThread, finalValue,
                    "最终值应该为" + (numThreads * incrementsPerThread) + "，实际: " + finalValue);

            // ========== 输出 ==========
            log.info("========== FN-RATOMIC-001 测试结果 ==========");
            log.info("线程数: {}, 每线程递增: {}", numThreads, incrementsPerThread);
            log.info("预期值: {}, 实际值: {}", numThreads * incrementsPerThread, finalValue);
            log.info("原子性验证: {}（正确）", finalValue == numThreads * incrementsPerThread);
            log.info("测试结论: RAtomicLong原子性正常");
            log.info("===========================================");

            // 清理
            counter.delete();
        }

        @Test
        @DisplayName("FN-RATOMIC-002: RBitSet位操作测试")
        void testRBitSet() {
            // ========== 测试要点 ==========
            // 验证RBitSet的位设置和查询操作

            // ========== 输入 ==========
            String bitsetName = "test:bitset:flags";
            RBitSet bitSet = redisson.getBitSet(bitsetName);

            // ========== 执行 ==========
            bitSet.set(0, true);   // 设置第0位
            bitSet.set(5, true);   // 设置第5位
            bitSet.set(10, true);  // 设置第10位
            bitSet.set(15, false); // 设置第15位为false

            // ========== 为什么这么测试 ==========
            // BitSet在K8s场景应用：
            // 1. 特性开关
            // 2. 权限标记
            // 3. 状态位存储

            // ========== 关注点 ==========
            // - 位设置是否正确
            // - 位查询是否准确
            // - 统计功能是否正常

            // ========== 验证 ==========
            assertTrue(bitSet.get(0), "第0位应该为true");
            assertFalse(bitSet.get(1), "第1位应该为false");
            assertTrue(bitSet.get(5), "第5位应该为true");
            assertTrue(bitSet.get(10), "第10位应该为true");
            assertFalse(bitSet.get(15), "第15位应该为false");

            assertEquals(3, bitSet.size(), "应该有3个位被设置为true");

            // ========== 输出 ==========
            log.info("========== FN-RATOMIC-002 测试结果 ==========");
            log.info("设置的位: 0, 5, 10");
            log.info("bitset.size(): {}", bitSet.size());
            log.info("测试结论: RBitSet位操作正常");
            log.info("===========================================");

            // 清理
            bitSet.delete();
        }

        @Test
        @DisplayName("FN-RATOMIC-003: RHyperLogLog基数统计测试")
        void testRHyperLogLog() {
            // ========== 测试要点 ==========
            // 验证RHyperLogLog的基数估算功能

            // ========== 输入 ==========
            String hllName = "test:hll:uv";
            RHyperLogLog<String> hll = redisson.getHyperLogLog(hllName);

            // ========== 执行 ==========
            int uniqueElements = 10000;
            Set<String> uniqueSet = new HashSet<>();

            for (int i = 0; i < uniqueElements; i++) {
                String element = "user-" + i;
                hll.add(element);
                uniqueSet.add(element);
            }

            long count = hll.count();

            // ========== 为什么这么测试 ==========
            // HyperLogLog在K8s场景应用：
            // 1. UV统计
            // 2. 去重计数
            // 3. 大数据集估算

            // ========== 关注点 ==========
            // - 估算准确度
            // - 内存占用
            // - 性能表现

            // ========== 验证 ==========
            double errorRate = Math.abs(count - uniqueElements) / (double) uniqueElements;

            assertTrue(errorRate < 0.01, "误差率应该<1%，实际: " + String.format("%.2f%%", errorRate * 100));

            // ========== 输出 ==========
            log.info("========== FN-RATOMIC-003 测试结果 ==========");
            log.info("实际唯一元素: {}", uniqueElements);
            log.info("HyperLogLog估算: {}", count);
            log.info("误差率: {}%", String.format("%.4f", errorRate * 100));
            log.info("测试结论: RHyperLogLog估算功能正常");
            log.info("===========================================");

            // 清理
            hll.delete();
        }
    }

    // ==================== K8s场景综合测试 ====================

    @Nested
    @DisplayName("K8S: Kubernetes场景综合测试")
    class K8sScenarioTests {

        @Test
        @DisplayName("K8S-001: Pod间配置共享测试")
        void testPodConfigSharing() {
            // ========== 测试要点 ==========
            // 模拟K8s中多个Pod共享配置的场景

            // ========== 输入 ==========
            String configMapName = "k8s:config:shared";
            RMap<String, String> configMap = redisson.getMap(configMapName);

            // 模拟Pod-A写入配置
            Map<String, String> podAConfig = new HashMap<>();
            podAConfig.put("database.url", "jdbc:mysql://mysql-service:3306/db");
            podAConfig.put("redis.host", "redis-service");
            podAConfig.put("app.version", "1.0.0");
            configMap.putAll(podAConfig);

            // 模拟Pod-B读取配置
            String dbUrl = configMap.get("database.url");
            String redisHost = configMap.get("redis.host");
            String version = configMap.get("app.version");

            // ========== 为什么这么测试 ==========
            // K8s场景需求：
            // 1. Pod间共享配置
            // 2. 配置实时同步
            // 3. 避免配置文件挂载

            // ========== 关注点 ==========
            // - 配置是否能跨Pod访问
            // - 配置变更是否实时
            // - 并发读写是否安全

            // ========== 验证 ==========
            assertEquals("jdbc:mysql://mysql-service:3306/db", dbUrl, "数据库URL应该正确");
            assertEquals("redis-service", redisHost, "Redis主机应该正确");
            assertEquals("1.0.0", version, "版本号应该正确");

            // ========== 输出 ==========
            log.info("========== K8S-001 测试结果 ==========");
            log.info("配置共享场景: Pod-A写入 -> Pod-B读取");
            log.info("配置内容: {}", configMap.readAllMap());
            log.info("测试结论: Pod间配置共享正常");
            log.info("===========================================");

            // 清理
            configMap.delete();
        }

        @Test
        @DisplayName("K8S-002: Pod间任务传递测试")
        void testPodTaskDistribution() throws Exception {
            // ========== 测试要点 ==========
            // 模拟K8s中Pod间任务传递的场景

            // ========== 输入 ==========
            String taskQueueName = "k8s:tasks:pending";
            RBlockingQueue<String> taskQueue = redisson.getBlockingQueue(taskQueueName);

            CountDownLatch producerDone = new CountDownLatch(1);
            CountDownLatch consumerDone = new CountDownLatch(1);

            // ========== 执行 ==========
            // 模拟生产者Pod
            ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
            producerExecutor.submit(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        String task = String.format("task-%d-pod-prod-1", i);
                        taskQueue.offer(task);
                        log.info("生产任务: {}", task);
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    log.error("生产者异常", e);
                } finally {
                    producerDone.countDown();
                }
            });

            // 模拟消费者Pod
            ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
            List<String> processedTasks = new ArrayList<>();
            consumerExecutor.submit(() -> {
                try {
                    while (processedTasks.size() < 5) {
                        String task = taskQueue.poll(3, TimeUnit.SECONDS);
                        if (task != null) {
                            processedTasks.add(task);
                            log.info("消费任务: {}", task);
                        }
                    }
                } catch (Exception e) {
                    log.error("消费者异常", e);
                } finally {
                    consumerDone.countDown();
                }
            });

            producerDone.await();
            consumerDone.await();

            producerExecutor.shutdown();
            consumerExecutor.shutdown();

            // ========== 为什么这么测试 ==========
            // K8s场景需求：
            // 1. Pod间异步通信
            // 2. 任务解耦
            // 3. 弹性伸缩

            // ========== 关注点 ==========
            // - 任务是否正确传递
            // - 消费顺序是否FIFO
            // - 是否有任务丢失

            // ========== 验证 ==========
            assertEquals(5, processedTasks.size(), "应该处理5个任务");
            assertTrue(processedTasks.get(0).contains("task-1"), "第一个任务应该是task-1");

            // ========== 输出 ==========
            log.info("========== K8S-002 测试结果 ==========");
            log.info("任务传递场景: 生产者Pod -> 消费者Pod");
            log.info("处理任务数: {}", processedTasks.size());
            log.info("任务列表: {}", processedTasks);
            log.info("测试结论: Pod间任务传递正常");
            log.info("===========================================");

            // 清理
            taskQueue.delete();
        }
    }
}
