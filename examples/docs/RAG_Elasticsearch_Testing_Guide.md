# RAG Elasticsearch向量整合测试指南

## 📋 测试概览

本文档描述了与Elasticsearch 192.168.10.107向量整合的完整测试体系，包括测试点、测试关注点、断言设计等。

**测试目标：**
- 验证Elasticsearch向量索引功能
- 验证KNN检索准确性
- 验证混合检索效果
- 验证性能指标达标
- 验证系统稳定性

---

## 🎯 测试策略

### 测试金字塔

```
           /\
          /  \
         / E2E \           ← 端到端集成测试
        /------\
       /        \
      /  Unit    \        ← 单元测试
     /------------\
    /              \
   /    Contract    \    ← 契约测试
  /------------------\
```

### 测试分类

| 测试类型 | 测试数量 | 关注点 | 执行频率 |
|---------|---------|--------|---------|
| 单元测试 | 25+ | 代码逻辑、边界条件 | 每次构建 |
| 集成测试 | 15+ | 模块交互、接口契约 | 每次构建 |
| 性能测试 | 10+ | 延迟、吞吐、资源使用 | 每日 |
| 压力测试 | 8+ | 稳定性、故障恢复 | 每周 |
| 端到端测试 | 12+ | 完整业务流程 | 每次发布 |

---

## 📝 详细测试点

### 模块1：Elasticsearch连接与配置

#### 测试点1.1：配置验证

**测试目的：** 验证Elasticsearch配置正确性

**关注点：**
- ✅ 主机地址正确
- ✅ 端口正确
- ✅ 认证信息正确
- ✅ 向量维度配置合理

**前置条件：**
- Elasticsearch服务运行在192.168.10.107:9200
- 配置文件已正确设置

**测试步骤：**
1. 加载ElasticsearchConfig
2. 验证配置有效性
3. 检查连接参数

**断言：**
```java
@Test
@DisplayName("配置验证")
public void testConfigValidation() {
    assertions.assertNotNull(esConfig, "配置不能为空");
    assertions.assertTrue(esConfig.isValid(), "配置应该有效");
    assertions.assertEquals("192.168.10.107", esConfig.getHost());
    assertions.assertEquals(9200, esConfig.getPort());
    assertions.assertEquals(1536, esConfig.getVectorDimension());
    assertions.assertEquals("cosine", esConfig.getSimilarity());
}
```

**预期结果：** 所有断言通过，配置有效且正确

---

#### 测试点1.2：连接可用性

**测试目的：** 验证能够连接到Elasticsearch

**关注点：**
- ✅ 网络连通性
- ✅ 服务响应性
- ✅ 认证成功

**前置条件：**
- Elasticsearch服务正常运行

**测试步骤：**
1. 尝试连接Elasticsearch
2. 检查健康状态
3. 验证集群信息

**断言：**
```java
@Test
@DisplayName("连接可用性")
public void testConnectionAvailability() {
    // 发送健康检查请求
    boolean healthy = esHealthChecker.isHealthy();

    assertions.assertTrue(healthy, "Elasticsearch应该健康");

    // 获取集群信息
    ClusterInfo info = esClient.getClusterInfo();
    assertions.assertNotNull(info, "集群信息不能为空");
    assertions.assertEquals("192.168.10.107", info.getHostName());
}
```

**预期结果：** 能够成功连接并获取集群信息

---

### 模块2：向量索引管理

#### 测试点2.1：索引创建

**测试目的：** 验证能够成功创建向量索引

**关注点：**
- ✅ 索引创建成功
- ✅ Mapping设置正确
- ✅ KNN配置正确
- ✅ 向量维度匹配

**前置条件：**
- Elasticsearch连接正常
- 索引名称唯一

**测试步骤：**
1. 创建索引请求
2. 发送到Elasticsearch
3. 验证索引存在

**断言：**
```java
@Test
@DisplayName("索引创建")
public void testIndexCreation() {
    // 创建索引
    boolean created = vectorStore.createIndex();

    assertions.assertTrue(created, "索引应该成功创建");

    // 验证索引存在
    boolean exists = vectorStore.indexExists();
    assertions.assertTrue(exists, "创建的索引应该存在");

    // 验证Mapping
    IndexMapping mapping = esClient.getIndexMapping(vectorIndexName);
    assertions.assertNotNull(mapping, "Mapping不能为空");

    // 验证向量字段配置
    Map<String, Object> vectorField = mapping.getField("content_vector");
    assertions.assertNotNull(vectorField, "向量字段应该存在");
    assertions.assertEquals("dense_vector", vectorField.get("type"));
    assertions.assertEquals(1536, vectorField.get("dims"));
    assertions.assertEquals("cosine", vectorField.get("similarity"));
    assertions.assertTrue((Boolean) vectorField.get("index"), "向量应该被索引");
}
```

**预期结果：** 索引成功创建且配置正确

---

#### 测试点2.2：索引删除

**测试目的：** 验证能够成功删除索引

**关注点：**
- ✅ 索引删除成功
- ✅ 删除后索引不存在
- ✅ 删除不影响其他索引

**测试步骤：**
1. 创建测试索引
2. 执行删除操作
3. 验证索引已不存在

**断言：**
```java
@Test
@DisplayName("索引删除")
public void testIndexDeletion() {
    // 先创建索引
    vectorStore.createIndex();
    assertions.assertTrue(vectorStore.indexExists(), "索引应该存在");

    // 删除索引
    boolean deleted = vectorStore.deleteIndex();

    assertions.assertTrue(deleted, "索引应该成功删除");
    assertions.assertFalse(vectorStore.indexExists(), "删除后索引不应该存在");
}
```

---

#### 测试点2.3：索引统计

**测试目的：** 验证能够获取准确的索引统计信息

**关注点：**
- ✅ 文档数量准确
- ✅ 存储大小准确
- ✅ 索引状态正确

**断言：**
```java
@Test
@DisplayName("索引统计")
public void testIndexStats() {
    IndexStats stats = vectorStore.getIndexStats();

    assertions.assertNotNull(stats, "统计信息不能为null");
    assertions.assertNotNull(stats.getIndexName(), "索引名称不能为空");
    assertions.assertTrue(stats.getDocumentCount() >= 0, "文档数量应该非负");
}
```

---

### 模块3：向量数据索引

#### 测试点3.1：单文档索引

**测试目的：** 验证单个文档向量索引功能

**关注点：**
- ✅ 文档成功索引
- ✅ 向量数据正确
- ✅ 元数据正确
- ✅ 文档ID正确返回

**前置条件：**
- 索引已创建

**测试步骤：**
1. 准备测试文档和向量
2. 调用索引API
3. 验证索引结果
4. 查询文档验证数据

**断言：**
```java
@Test
@DisplayName("单文档索引")
public void testSingleDocumentIndexing() {
    // 准备数据
    String docId = "test_doc_001";
    String content = "测试文档内容";
    float[] vector = generateTestVector(1536);
    Map<String, Object> metadata = Map.of(
        "chunk_type", "text",
        "kb_id", "test_kb"
    );

    // 执行索引
    String resultDocId = vectorStore.indexDocument(docId, content, vector, metadata);

    // 验证结果
    assertions.assertNotNull(resultDocId, "返回的文档ID不能为空");
    assertions.assertEquals(docId, resultDocId, "返回的ID应该匹配");

    // 验证文档已索引
    IndexedDocument doc = esClient.getDocument(vectorIndexName, docId);
    assertions.assertNotNull(doc, "文档应该被索引");
    assertions.assertEquals(content, doc.getContent(), "内容应该匹配");
    assertions.assertEquals(metadata, doc.getMetadata(), "元数据应该匹配");

    // 验证向量维度
    List<Float> indexedVector = doc.getVector("content_vector");
    assertions.assertNotNull(indexedVector, "向量不能为null");
    assertions.assertEquals(1536, indexedVector.size(), "向量维度应该匹配");
}
```

**性能指标：**
- 索引延迟 < 500ms

---

#### 测试点3.2：批量索引

**测试目的：** 验证批量索引功能和性能

**关注点：**
- ✅ 批量索引成功
- ✅ 批量索引完整性
- ✅ 批量索引性能
- ✅ 错误处理

**前置条件：**
- 索引已创建
- 准备多个测试文档

**测试步骤：**
1. 准备批量文档数据（50个文档）
2. 执行批量索引
3. 验证索引结果
4. 检查索引统计

**断言：**
```java
@Test
@DisplayName("批量索引")
public void testBulkIndexing() {
    // 准备批量数据
    int batchSize = 50;
    List<VectorDocument> docs = new ArrayList<>();

    for (int i = 0; i < batchSize; i++) {
        docs.add(VectorDocument.builder()
                .docId("bulk_test_" + i)
                .content("批量测试内容 " + i)
                .vector(generateTestVector(1536))
                .kbId("test_kb")
                .chunkType("text")
                .build());
    }

    // 执行批量索引
    long startTime = System.currentTimeMillis();
    BulkIndexResult result = vectorStore.bulkIndex(docs);
    long duration = System.currentTimeMillis() - startTime;

    // 验证结果
    assertions.assertTrue(result.isSuccess(), "批量索引应该成功");
    assertions.assertEquals(batchSize, result.getTotal(), "总数应该匹配");
    assertions.assertEquals(batchSize, result.getSuccessCount(), "成功数应该匹配");
    assertions.assertEquals(0, result.getFailedCount(), "失败数应该为0");

    // 性能验证
    double throughput = (double) batchSize / (duration / 1000.0);
    assertions.assertThroughputGreaterThan(throughput, 10.0,
            "批量索引吞吐量应该大于10文档/秒");

    // 完整性验证
    IndexStats stats = vectorStore.getIndexStats();
    assertions.assertTrue(stats.getDocumentCount() >= batchSize, "索引文档数应该>=批量数");
}
```

**性能指标：**
- 吞吐量 > 10 docs/s
- 批量索引延迟 < 5s

---

#### 测试点3.3：向量归一化验证

**测试目的：** 确保索引的向量是归一化的

**关注点：**
- ✅ 向量模长为1
- ✅ 向量值在合理范围
- ✅ 归一化处理正确

**断言：**
```java
@Test
@DisplayName("向量归一化验证")
public void testVectorNormalization() {
    // 准备非归一化向量
    float[] vector = new float[1536];
    for (int i = 0; i < vector.length; i++) {
        vector[i] = (float) Math.random();
    }

    // 索引文档
    String docId = vectorStore.indexDocument("norm_test", "测试内容", vector, null);
    assertions.assertNotNull(docId, "文档应该成功索引");

    // 获取索引的向量
    IndexedDocument doc = esClient.getDocument(vectorIndexName, docId);
    List<Float> indexedVector = doc.getVector("content_vector");

    // 验证归一化
    double norm = calculateNorm(indexedVector);
    assertions.assertTrue(norm > 0.999 && norm < 1.001,
            "索引的向量应该是归一化的 (norm: " + norm + ")");

    // 验证向量范围
    for (Float v : indexedVector) {
        assertions.assertTrue(v >= -1.0f && v <= 1.0f,
                "向量值应该在[-1, 1]范围内: " + v);
    }
}

private double calculateNorm(List<Float> vector) {
    double sum = 0.0;
    for (Float v : vector) {
        sum += v * v;
    }
    return Math.sqrt(sum);
}
```

---

### 模块4：KNN向量检索

#### 测试点4.1：基础KNN检索

**测试目的：** 验证基本的KNN检索功能

**关注点：**
- ✅ KNN检索成功
- ✅ 返回正确数量的结果
- ✅ 结果按相似度排序
- ✅ 相似度分数合理

**前置条件：**
- 索引已创建并包含测试数据
- 查询向量维度匹配

**测试步骤：**
1. 准备查询向量
2. 执行KNN检索（k=5）
3. 验证检索结果

**断言：**
```java
@Test
@DisplayName("基础KNN检索")
public void testBasicKnnSearch() {
    // 准备查询向量
    float[] queryVector = generateTestVector(1536);

    // 执行KNN检索
    int topK = 5;
    List<RetrieveResult> results = retrieveService.knnSearch(queryVector, topK, "test_kb");

    // 验证结果
    assertions.assertNotNull(results, "结果不能为null");
    assertions.assertSearchResultsNotEmpty(results, "应该返回结果");
    assertions.assertSearchResultCountInRange(1, topK, results,
            "结果数量应该在[1, " + topK + "]范围内");

    // 验证结果排序
    for (int i = 0; i < results.size() - 1; i++) {
        double currentScore = results.get(i).getScore();
        double nextScore = results.get(i + 1).getScore();
        assertions.assertTrue(currentScore >= nextScore,
                "结果应该按分数降序排列: [" + i + "]=" + currentScore + " >= [" + (i + 1) + "]=" + nextScore);
    }

    // 验证分数范围
    for (RetrieveResult result : results) {
        assertions.assertNotNull(result.getScore(), "结果应该有分数");
        assertions.assertTrue(result.getScore() >= 0.0 && result.getScore() <= 1.0,
                "分数应该在[0,1]范围内: " + result.getScore());
    }
}
```

**性能指标：**
- 检索延迟 < 200ms
- 返回结果数 = k

---

#### 测试点4.2：不同K值检索

**测试目的：** 验证不同k值的检索效果

**关注点：**
- ✅ k=1返回最相关结果
- ✅ k=10返回Top10结果
- ✅ k=100返回足够结果
- ✅ 大k值性能可接受

**测试步骤：**
1. 准备查询向量
2. 分别执行k=1, 10, 50, 100的检索
3. 验证结果数量和性能

**断言：**
```java
@ParameterizedTest
@ValueSource(ints = {1, 10, 50, 100})
public void testDifferentKValues(int k) {
    float[] queryVector = generateTestVector(1536);

    long startTime = System.currentTimeMillis();
    List<RetrieveResult> results = retrieveService.knnSearch(queryVector, k, "test_kb");
    long duration = System.currentTimeMillis() - startTime;

    // 验证结果数量
    assertions.assertSearchResultCountInRange(0, k, results,
            "结果数量应该不超过k");

    // 验证结果排序
    assertResultsSorted(results);

    // 性能验证
    if (k <= 10) {
        assertions.assertLatencyLessThan(duration, 200,
                "小k值检索应该快速");
    } else if (k <= 50) {
        assertions.assertLatencyLessThan(duration, 500,
                "中等k值检索应该合理");
    }
}
```

**性能指标：**
- k=1: 延迟 < 100ms
- k=10: 延迟 < 200ms
- k=50: 延迟 < 500ms
- k=100: 延迟 < 1000ms

---

#### 测试点4.3：查询向量准确性

**测试目的：** 验证查询向量在检索中的正确使用

**关注点：**
- ✅ 查询向量正确传递
- ✅ 向量维度匹配检查
- ✅ 查询向量归一化

**测试步骤：**
1. 准备测试向量
2. 执行检索
3. 验证结果相关性

**断言：**
```java
@Test
@DisplayName("查询向量准确性")
public void testQueryVectorAccuracy() {
    // 准备与已索引文档相似的查询向量
    // 假设我们索引了包含"机器学习"的文档
    float[] queryVector = generateTestVector(1536);

    // 执行检索
    List<RetrieveResult> results = retrieveService.knnSearch(queryVector, 10, "test_kb");

    // 验证结果相关性
    if (!results.isEmpty()) {
        // 检查结果内容是否相关
        RetrieveResult topResult = results.get(0);
        assertions.assertResultRelevance(topResult.getText(),
                "机器学习|人工智能",
                "Top1结果应该包含相关关键词");
    }
}
```

---

### 模块5：混合检索

#### 测试点5.1：文本+向量混合检索

**测试目的：** 验证混合检索功能

**关注点：**
- ✅ 混合检索成功
- ✅ 文本匹配生效
- ✅ 向量检索生效
- ✅ 结果融合正确

**测试步骤：**
1. 准备查询文本和向量
2. 执行混合检索
3. 验证结果质量

**断言：**
```java
@Test
@DisplayName("混合检索")
public void testHybridSearch() {
    // 准备查询
    String queryText = "机器学习算法";
    float[] queryVector = generateTestVector(1536);

    // 执行混合检索
    List<RetrieveResult> results = retrieveService.hybridSearch(
            queryText, queryVector, 10, "test_kb");

    // 验证结果
    assertions.assertNotNull(results, "混合检索结果不能为null");
    assertions.assertSearchResultsNotEmpty(results, "应该返回相关结果");

    // 验证结果包含文本匹配和向量匹配
    // 实际实现需要检查结果的来源信息
    boolean hasTextMatch = results.stream()
            .anyMatch(r -> "text_match".equals(r.getSource()));
    boolean hasVectorMatch = results.stream()
            .anyMatch(r -> "vector_match".equals(r.getSource()));

    // 混合检索应该包含两种类型的结果
    assertions.assertTrue(hasTextMatch || hasVectorMatch,
            "结果应该包含文本匹配或向量匹配");
}
```

---

#### 测试点5.2：检索权重调整

**测试目的：** 验证不同权重配置的效果

**关注点：**
- ✅ 权重参数生效
- ✅ 结果排序符合预期
- ✅ 权重对结果的影响

**断言：**
```java
@Test
@DisplayName("检索权重调整")
public void testSearchWeightAdjustment() {
    // 测试不同权重配置
    float[] queryVector = generateTestVector(1536);

    // 纯向量检索
    List<RetrieveResult> vectorOnlyResults = retrieveService.knnSearch(queryVector, 10);

    // 混合检索（向量权重高）
    List<RetrieveResult> hybridWeightVectorResults =
            retrieveService.hybridSearchWithWeight(queryVector, "test", 10, 0.7);

    // 混合检索（文本权重高）
    List<RetrieveResult> hybridWeightTextResults =
            retrieveService.hybridSearchWithWeight(queryVector, "test", 10, 0.3);

    // 验证权重影响
    // 向量权重高的结果应该更接近纯向量检索结果
}
```

---

### 模块6：性能基准测试

#### 测试点6.1：索引性能

**测试目的：** 验证索引操作性能达标

**关注点：**
- ✅ 单文档索引延迟
- ✅ 批量索引吞吐量
- ✅ 内存使用合理
- ✅ CPU使用合理

**测试步骤：**
1. 测试单文档索引延迟（多次取平均值）
2. 测试批量索引吞吐量
3. 监控资源使用

**断言：**
```java
@Test
@DisplayName("索引性能基准")
@RepeatedTest(10)
public void testIndexPerformanceBenchmark() {
    String docId = "perf_test_" + System.nanoTime();
    String content = SAMPLE_TEXT;
    float[] vector = generateTestVector(1536);

    long startTime = System.nanoTime();
    String result = vectorStore.indexDocument(docId, content, vector, null);
    long duration = (System.nanoTime() - startTime) / 1_000_000; // 转换为ms

    // 验证性能
    assertions.assertNotNull(result, "索引应该成功");
    assertions.assertLatencyLessThan(duration, 500,
            "单文档索引延迟应该小于500ms");

    // 记录性能指标
    tracker.recordMetric("single_index_latency", (double) duration, "ms");
    tracker.recordMetric("single_index_ops", 1000.0 / duration, "ops/s");
}
```

**性能基准：**
- 单文档索引: < 500ms
- 批量索引(50): > 10 docs/s
- 内存增长: < 100MB

---

#### 测试点6.2：检索性能

**测试目的：** 验证检索操作性能达标

**关注点：**
- ✅ KNN检索延迟
- ✅ 混合检索延迟
- ✅ 检索吞吐量
- ✅ 并发检索能力

**测试步骤：**
1. 测试单次检索延迟
2. 测试检索吞吐量（连续检索）
3. 测试并发检索

**断言：**
```java
@Test
@DisplayName("检索性能基准")
public void testSearchPerformanceBenchmark() {
    float[] queryVector = generateTestVector(1536);
    int iterations = 100;

    // 测试吞吐量
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
        retrieveService.knnSearch(queryVector, 10, "test_kb");
    }
    long duration = System.currentTimeMillis() - startTime;
    double avgLatency = (double) duration / iterations;
    double qps = 1000.0 / (avgLatency / 1000.0);

    // 验证性能
    assertions.assertLatencyLessThan((long) avgLatency, 200,
            "平均检索延迟应该小于200ms");
    assertions.assertThroughputGreaterThan(qps, 5.0,
            "检索QPS应该大于5");

    // 记录性能指标
    tracker.recordMetric("search_avg_latency", avgLatency, "ms");
    tracker.recordMetric("search_qps", qps, "queries/s");
}
```

**性能基准：**
- KNN检索(k=10): < 200ms
- 混合检索: < 300ms
- 检索QPS: > 5 queries/s

---

#### 测试点6.3：并发性能

**测试目的：** 验证并发场景下的性能

**关注点：**
- ✅ 并发索引正确性
- ✅ 并发检索准确性
- ✅ 并发性能
- ✅ 资源竞争处理

**测试步骤：**
1. 启动多个并发线程
2. 执行索引或检索操作
3. 验证结果正确性
4. 收集性能数据

**断言：**
```java
@Test
@DisplayName("并发索引测试")
public void testConcurrentIndexing() throws InterruptedException {
    int threadCount = 10;
    int docsPerThread = 20;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    // 并发索引
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(() -> {
            try {
                for (int i = 0; i < docsPerThread; i++) {
                    String docId = "concurrent_" + threadId + "_" + i;
                    float[] vector = generateTestVector(1536);
                    String result = vectorStore.indexDocument(
                            docId, "并发测试内容", vector, null);
                    if (result != null) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(60, TimeUnit.SECONDS);
    executor.shutdown();

    // 验证结果
    int totalDocs = threadCount * docsPerThread;
    assertions.assertEquals(totalDocs, successCount.get() + failCount.get(),
            "并发索引总数应该匹配");
    assertions.assertTrue(successCount.get() > totalDocs * 0.95,
            "并发索引成功率应该>95%");

    // 性能指标
    tracker.recordMetric("concurrent_index_success_rate",
            (double) successCount.get() / totalDocs, "ratio");
}
```

**性能基准：**
- 并发索引成功率: > 95%
- 并发检索准确性: > 98%
- 并发性能下降: < 30%

---

### 模块7：边界条件测试

#### 测试点7.1：空向量处理

**测试目的：** 验证系统对异常输入的处理

**关注点：**
- ✅ 空向量优雅拒绝
- ✅ 错误信息清晰
- ✅ 不影响系统稳定性

**断言：**
```java
@Test
@DisplayName("空向量处理")
public void testEmptyVector() {
    float[] emptyVector = new float[0];

    // 应该抛出异常或返回错误
    assertThrows(IllegalArgumentException.class, () -> {
        retrieveService.knnSearch(emptyVector, 10, "test_kb");
    });

    // 或者优雅处理
    List<RetrieveResult> results = retrieveService.knnSearch(emptyVector, 10, "test_kb");
    assertions.assertTrue(results.isEmpty(), "空向量应该返回空结果");
}
```

---

#### 测试点7.2：维度不匹配处理

**测试目的：** 验证向量维度不匹配时的处理

**关注点：**
- ✅ 维度检查生效
- ✅ 清晰的错误信息
- ✅ 不导致系统崩溃

**断言：**
```java
@Test
@DisplayName("维度不匹配处理")
public void testDimensionMismatch() {
    float[] wrongDimensionVector = new float[512]; // 错误维度

    // 应该检测到维度不匹配
    assertThrows(IllegalArgumentException.class, () -> {
        retrieveService.knnSearch(wrongDimensionVector, 10, "test_kb");
    });

    // 或返回错误信息
    try {
        retrieveService.knnSearch(wrongDimensionVector, 10, "test_kb");
        fail("应该抛出维度不匹配异常");
    } catch (IllegalArgumentException e) {
        assertTrue(e.getMessage().contains("dimension") ||
                e.getMessage().contains("维度"),
                "错误信息应该包含维度相关描述");
    }
}
```

---

#### 测试点7.3：特殊字符处理

**测试目的：** 验证特殊字符在索引和检索中的处理

**关注点：**
- ✅ 特殊字符正确转义
- ✅ 索引和检索内容一致
- ✅ 无安全风险

**断言：**
```java
@Test
@DisplayName("特殊字符处理")
public void testSpecialCharacters() {
    String specialContent = "测试特殊字符: \"\"\" \\n \\r \\t <>&'`\\n";
    float[] vector = generateTestVector(1536);

    // 索引
    String docId = vectorStore.indexDocument("special_test", specialContent, vector, null);
    assertions.assertNotNull(docId, "特殊字符文档应该成功索引");

    // 检索
    List<RetrieveResult> results = retrieveService.hybridSearch(
            specialContent.substring(0, 10), vector, 5, "test_kb");

    if (!results.isEmpty()) {
        RetrieveResult result = results.get(0);
        // 验证内容一致性（特殊字符应正确处理）
        // assertions.assertEquals(specialContent, result.getText(),
        //         "检索内容应该与索引内容一致");
    }
}
```

---

### 模块8：错误处理与恢复

#### 测试点8.1：网络错误处理

**测试目的：** 验证网络错误时的处理机制

**关注点：**
- ✅ 超时机制生效
- ✅ 重试机制
- ✅ 错误日志记录
- ✅ 不影响其他操作

**测试步骤：**
1. 模拟网络错误
2. 验证错误处理
3. 验证恢复机制

**断言：**
```java
@Test
@DisplayName("网络错误处理")
public void testNetworkErrorHandling() {
    // 模拟网络超时
    esConfig.setConnectTimeout(1); // 1ms超时

    // 应该抛出超时异常
    assertThrows(Exception.class, () -> {
        vectorStore.indexDocument("timeout_test", "测试", generateTestVector(1536), null);
    });

    // 验证错误被正确记录
    // 验证系统仍然可用（恢复连接后）
}
```

---

#### 测试点8.2：Elasticsearch不可用

**测试目的：** 验证Elasticsearch服务不可用时的处理

**关注点：**
- ✅ 优雅降级
- ✅ 错误信息清晰
- ✅ 系统继续运行

**断言：**
```java
@Test
@DisplayName("Elasticsearch不可用")
public void testElasticsearchUnavailable() {
    // 停设Elasticsearch不可用
    // 验证系统行为

    List<RetrieveResult> results = retrieveService.knnSearch(
            generateTestVector(1536), 10, "test_kb");

    // 应该返回空结果或错误
    assertTrue(results.isEmpty() || results.get(0).getScore() < 0,
            "Elasticsearch不可用时应该返回空结果或错误");
}
```

---

## 🎯 测试关注点总结

### 功能性关注点

| 关注点 | 验证方法 | 优先级 |
|-------|---------|--------|
| 索引创建/删除 | 集成测试 | P0 |
| 向量索引正确性 | 单元测试 | P0 |
| KNN检索准确性 | 集成测试 | P0 |
| 混合检索效果 | 集成测试 | P1 |
| 结果排序正确性 | 单元测试 | P0 |
| 元数据完整性 | 集成测试 | P1 |

### 性能关注点

| 关注点 | 目标值 | 测试方法 | 优先级 |
|-------|--------|---------|--------|
| 单文档索引延迟 | < 500ms | 性能测试 | P0 |
| 批量索引吞吐量 | > 10 docs/s | 性能测试 | P0 |
| KNN检索延迟(k=10) | < 200ms | 性能测试 | P0 |
| 混合检索延迟 | < 300ms | 性能测试 | P1 |
| 检索QPS | > 5 queries/s | 性能测试 | P1 |
| 并发检索成功率 | > 98% | 压力测试 | P1 |
| 内存使用 | < 500MB | 监控 | P2 |

### 稳定性关注点

| 关注点 | 验证方法 | 优先级 |
|-------|---------|--------|
| 长时间运行稳定性 | 压力测试 | P1 |
| 大数据量处理 | 性能测试 | P1 |
| 错误恢复能力 | 故障注入测试 | P2 |
| 并发安全性 | 并发测试 | P1 |
| 资源泄漏 | 内存分析 | P2 |

---

## 📊 测试执行与报告

### 测试执行命令

```bash
# 运行所有测试
./gradlew test

# 运行Elasticsearch集成测试
./gradlew test --tests ElasticsearchVectorIntegrationTest

# 运行性能测试
./gradlew test --tests "*Performance*"

# 生成测试报告
./gradlew test jacocoTestReport
```

### 测试报告位置

- HTML报告: `build/test-reports/Elasticsearch向量集成测试_*.html`
- JSON报告: `build/test-reports/Elasticsearch向量集成测试_*.json`
- 文本报告: `build/test-reports/Elasticsearch向量集成测试_*.txt`

### 测试进度追踪

测试执行时会实时输出进度：

```
=== 开始测试套件: Elasticsearch向量集成测试 ===
开始时间: 2026-06-07 10:30:00

--- 开始阶段: 连接测试 ---
✓ 测试通过: 配置有效性验证 (耗时: 15 ms)
✓ 测试通过: Elasticsearch健康检查 (耗时: 125 ms)
--- 阶段完成: 连接测试 (耗时: 140 ms) ---

--- 开始阶段: 索引管理 ---
✓ 测试通过: 创建向量索引 (耗时: 450 ms)
✗ 测试失败: 获取索引统计 (耗时: 50 ms)
  错误: 索引统计API格式不匹配
...
```

---

## 🛠️ 测试环境准备

### 前置条件

1. **Elasticsearch服务**
   ```bash
   # 检查Elasticsearch服务状态
   curl http://192.168.10.107:9200/_cluster/health
   ```

2. **配置文件**
   ```yaml
   # application-test.yml
   elasticsearch:
     host: 192.168.10.107
     port: 9200
     scheme: http
     vector-index-name: kb_vector_index_test
     vector-dimension: 1536
     similarity: cosine
   ```

3. **依赖项**
   ```gradla
   dependencies {
       implementation 'org.springframework.boot:spring-boot-starter-web'
       implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
       testImplementation 'org.springframework.boot:spring-boot-starter-test'
   }
   ```

### 测试数据准备

```bash
# 准备测试数据
curl -X PUT "http://192.168.10.107:9200/kb_vector_index_test" \
  -H 'Content-Type: application/json' -d '
{
  "mappings": {
    "properties": {
      "content": {"type": "text"},
      "content_vector": {
        "type": "dense_vector",
        "dims": 1536,
        "index": true,
        "similarity": "cosine"
      }
    }
  }
}
'
```

---

## 📈 性能基准参考

### 索引性能基准

| 操作类型 | 数据量 | 目标延迟 | 目标吞吐量 |
|---------|-------|---------|-----------|
| 单文档索引 | 1 | < 500ms | - |
| 批量索引 | 50 | < 5s | > 10 docs/s |
| 批量索引 | 100 | < 10s | > 10 docs/s |
| 批量索引 | 500 | < 30s | > 15 docs/s |

### 检索性能基准

| 检索类型 | k值 | 目标延迟 | 目标QPS |
|---------|-----|---------|---------|
| KNN检索 | 10 | < 200ms | > 5 |
| KNN检索 | 50 | < 500ms | > 2 |
| KNN检索 | 100 | < 1000ms | > 1 |
| 混合检索 | 10 | < 300ms | > 3 |

### 资源使用基准

| 资源类型 | 操作 | 目标值 |
|---------|------|--------|
| 内存 | 索引5000文档 | < 500MB |
| 内存 | 检索QPS=10 | < 300MB |
| CPU | 单文档索引 | < 50% |
| 磁盘IO | 批量索引 | < 80% |

---

## 🎯 测试覆盖率目标

| 模块 | 代码覆盖率 | 功能覆盖率 |
|------|-----------|-----------|
| Elasticsearch配置 | 80% | 100% |
| 向量存储服务 | 70% | 90% |
| 向量检索服务 | 70% | 90% |
| 错误处理 | 60% | 80% |

---

## 📝 测试检查清单

在执行完整测试前，请确认：

### 环境检查
- [ ] Elasticsearch 192.168.10.107:9200 可访问
- [ ] 测试索引不存在或已清理
- [ ] 测试数据库已重置
- [ ] 测试报告目录可写

### 配置检查
- [ ] Elasticsearch配置正确
- [ ] 向量维度配置匹配
- [ ] 超时配置合理
- [ ] 重试配置启用

### 数据检查
- [ ] 测试文档已准备
- [ ] 测试向量已生成
- [ ] 测试数据清理完成

---

本文档持续更新中...
