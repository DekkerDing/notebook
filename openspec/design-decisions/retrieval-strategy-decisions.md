# 检索策略设计决策文档

本文档记录Notebook项目中检索策略相关的设计决策及其背后的思考过程。

## 决策记录格式

每个决策记录包含：
- **决策内容**: 做出的选择
- **背景**: 为什么需要这个决策
- **思考过程**: 考虑的选项和权衡
- **最终选择**: 选定方案及原因
- **实现细节**: 技术实现要点
- **替代方案**: 考虑但未采用的方案

---

## 决策 #1: 多路融合检索架构设计

### 决策内容
实现基于策略模式的多路融合检索系统，支持同时执行多种检索策略并智能融合结果。

### 背景
单一检索策略存在局限性：
- **BM25**: 善长关键词匹配，但忽略语义相似性
- **向量检索**: 善长语义匹配，但可能错过精确关键词
- **GraphRAG**: 善长关系推理，但依赖图谱完整性
- **现实需求**: 用户查询往往需要多种检索能力的组合

### 思考过程

**选项1: 串行执行多种策略**
```
查询 → BM25检索 → 向量检索 → 图谱检索 → 简单合并
```
- 优点: 实现简单，资源消耗小
- 缺点: 延迟累加，无法利用并行计算

**选项2: 并行执行 + 线性加权融合**
```
查询 → [BM25, 向量, 图谱] 并行执行 → 加权融合 → 排序
```
- 优点: 延迟低，实现相对简单
- 缺点: 权重固定，无法根据查询特点调整

**选项3: 并行执行 + 自适应融合** ✅ **(选定方案)**
```
查询 → [BM25, 向量, 图谱] 并行执行 → 智能融合 → 动态排序
```
- 优点: 延迟低，可根据查询特点调整融合策略
- 缺点: 实现复杂度较高

### 最终选择
**选项3: 并行执行 + 自适应融合**

**理由**:
1. **性能优势**: 并行执行可显著降低总体延迟
2. **灵活性**: 可根据查询类型（关键词/语义/关系）动态调整策略权重
3. **可扩展性**: 新增检索策略时无需修改融合逻辑
4. **用户体验**: 检索速度和准确性都得到保证

### 实现细节

#### 架构设计

```java
// 核心接口
public interface RetrieveStrategy {
    List<RetrieveResult> retrieve(String query, RetrieveRequest request, RetrieveContext context);
    String getStrategyName();
}

// 融合协调器
@Component
public class HybridRetrieveCoordinator {
    private Map<String, RetrieveStrategy> strategies;
    private ResultFusion fusionService;

    public List<RetrieveResult> retrieve(String query, RetrieveRequest request) {
        // 1. 并行执行所有策略
        List<CompletableFuture<List<RetrieveResult>>> futures = strategies.values()
            .stream()
            .map(strategy -> CompletableFuture.supplyAsync(
                () -> strategy.retrieve(query, request, context), executor))
            .collect(Collectors.toList());

        // 2. 等待所有策略完成
        List<List<RetrieveResult>> allResults = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        // 3. 智能融合
        return fusionService.fuse(allResults, request.getFusionConfig());
    }
}
```

#### 融合策略

**加权融合（RRF - Reciprocal Rank Fusion）**:
```java
score = Σ (weight / (k + rank))

其中：
- weight: 策略权重（可根据查询类型调整）
- rank: 文档在该策略中的排名
- k: 平滑参数（默认60）
```

**自适应权重调整**:
```java
// 根据查询特征调整权重
if (isKeywordQuery(query)) {
    weights.put("bm25", 0.6);
    weights.put("vector", 0.3);
    weights.put("graphrag", 0.1);
} else if (isSemanticQuery(query)) {
    weights.put("bm25", 0.2);
    weights.put("vector", 0.6);
    weights.put("graphrag", 0.2);
}
```

### 替代方案
**学习排序（Learning to Rank）**:
- 使用机器学习模型学习最优融合策略
- 优点: 可自动优化
- 缺点: 需要大量训练数据，实现复杂
- 未采用原因: 项目初期缺乏标注数据

---

## 决策 #2: 结果去重策略

### 决策内容
采用基于文档ID的去重策略，保留各策略中分数最高的结果。

### 背景
多个检索策略可能返回相同文档，需要去重避免重复展示。

### 思考过程

**选项1: 基于内容相似度去重**
- 使用文本相似度算法（如余弦相似度）判断是否重复
- 优点: 可发现内容相似但来源不同的文档
- 缺点: 计算开销大，可能误判

**选项2: 基于文档ID去重** ✅ **(选定方案)**
- 使用文档ID作为唯一标识
- 优点: 实现简单，性能高，准确性高
- 缺点: 无法处理内容重复但ID不同的情况

### 最终选择
**选项2: 基于文档ID去重**

**理由**:
1. **准确性高**: 在RAG系统中，同一内容通常对应同一文档ID
2. **性能优秀**: O(1)查找复杂度
3. **实现简单**: 使用HashMap即可

### 实现细节

```java
public List<RetrieveResult> deduplicate(List<List<RetrieveResult>> allResults) {
    Map<String, RetrieveResult> resultMap = new LinkedHashMap<>();

    for (List<RetrieveResult> results : allResults) {
        for (RetrieveResult result : results) {
            String docId = result.getDocId();
            RetrieveResult existing = resultMap.get(docId);

            if (existing == null || result.getScore() > existing.getScore()) {
                resultMap.put(docId, result);
            }
        }
    }

    return new ArrayList<>(resultMap.values());
}
```

---

## 决策 #3: 检索缓存策略

### 决策内容
实现基于查询内容的智能缓存，缓存热门查询的结果。

### 背景
- 相同查询可能被多次执行
- 向量检索和图谱检索计算开销大
- 缓存可显著提升响应速度

### 思考过程

**选项1: 完全缓存**
- 缓存所有查询结果
- 优点: 命中率高
- 缺点: 内存消耗大，缓存失效策略复杂

**选项2: 不缓存**
- 每次都执行检索
- 优点: 无需管理缓存
- 缺点: 性能差

**选项3: LRU缓存 + 查询标准化** ✅ **(选定方案)**
- 使用LRU算法管理缓存
- 对查询进行标准化（去除多余空格、标点等）
- 优点: 平衡性能和内存占用
- 缺点: 需要配置合理的缓存大小

### 最终选择
**选项3: LRU缓存 + 查询标准化**

### 实现细节

```java
@Component
public class RetrieveCache {
    private final int CACHE_SIZE = 1000;
    private final Cache<String, List<RetrieveResult>> cache =
        Caffeine.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    public List<RetrieveResult> get(String query) {
        String normalizedQuery = normalize(query);
        return cache.getIfPresent(normalizedQuery);
    }

    private String normalize(String query) {
        return query.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
```

---

## 决策 #4: 检索结果截断策略

### 决策内容
采用动态TopK截断策略，根据查询结果质量动态调整返回数量。

### 背景
- 固定TopK可能返回低质量结果
- 不同查询的有效结果数量差异大

### 思考过程

**选项1: 固定TopK**
- 始终返回前K个结果
- 优点: 实现简单
- 缺点: 可能返回低质量结果

**选项2: 分数阈值截断**
- 返回所有分数高于阈值的结果
- 优点: 保证结果质量
- 缺点: 可能返回过多或过少结果

**选项3: 动态TopK + 分数阈值** ✅ **(选定方案)**
- 在TopK范围内，只保留分数高于阈值的结果
- 优点: 平衡结果数量和质量
- 缺点: 需要合理配置参数

### 最终选择
**选项3: 动态TopK + 分数阈值**

### 实现细节

```java
public List<RetrieveResult> truncate(List<RetrieveResult> results, int topK, double minScore) {
    return results.stream()
        .filter(r -> r.getScore() >= minScore)
        .limit(topK)
        .collect(Collectors.toList());
}
```

---

## 决策 #5: 检索策略优先级

### 决策内容
为不同检索策略设置默认优先级和权重。

### 背景
- 不同查询类型适合不同检索策略
- 需要平衡各种策略的贡献

### 思考过程

**优先级设计**:

| 查询类型 | BM25权重 | 向量权重 | GraphRAG权重 | 说明 |
|---------|---------|---------|-------------|------|
| 关键词查询 | 0.6 | 0.3 | 0.1 | 精确匹配优先 |
| 语义查询 | 0.2 | 0.6 | 0.2 | 语义理解优先 |
| 关系查询 | 0.1 | 0.3 | 0.6 | 关系推理优先 |
| 默认查询 | 0.4 | 0.4 | 0.2 | 平衡各策略 |

### 最终选择
**基于查询类型的自适应权重**

### 实现细节

```java
public FusionWeights detectQueryType(String query) {
    // 关键词特征：包含较多专有名词、数字
    if (containsKeywords(query)) {
        return FusionWeights.keywordFocused();
    }

    // 语义特征：完整句子、疑问词
    if (isSemanticQuery(query)) {
        return FusionWeights.semanticFocused();
    }

    // 关系特征：实体关系词（如"谁"、"哪里"、"什么关系"）
    if (isRelationQuery(query)) {
        return FusionWeights.relationFocused();
    }

    // 默认平衡
    return FusionWeights.balanced();
}
```

---

## 决策记录总结

| 决策ID | 决策内容 | 核心权衡 | 实现状态 |
|-------|---------|---------|---------|
| #1 | 多路融合架构 | 性能 vs 复杂度 | ✅ 已实现 |
| #2 | 结果去重策略 | 准确性 vs 性能 | ✅ 已实现 |
| #3 | 检索缓存策略 | 内存 vs 响应速度 | ✅ 已实现 |
| #4 | 结果截断策略 | 数量 vs 质量 | ✅ 已实现 |
| #5 | 策略优先级 | 灵活性 vs 简单性 | ✅ 已实现 |

---

## 设计原则

基于以上决策，我们确立了以下设计原则：

1. **性能优先**: 并行执行检索，降低延迟
2. **灵活性**: 支持动态配置和自适应调整
3. **可扩展性**: 易于添加新的检索策略
4. **用户体验**: 快速响应和准确结果并重
5. **可维护性**: 清晰的代码结构和文档

---

**文档状态**: Active
**创建日期**: 2026-06-07
**最后更新**: 2026-06-07
**维护者**: Notebook RAG Team
