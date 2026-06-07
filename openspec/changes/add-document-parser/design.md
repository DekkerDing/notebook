# Design: 文档解析功能实现

## Context

### 当前状态

现有系统仅支持纯文本输入，用户需要手动复制粘贴文档内容。系统缺乏：
1. 多格式文档支持
2. 文档元数据提取
3. 文档结构保留
4. 大文件处理能力

### 约束条件

- Java 8
- Spring Boot 2.6.14
- 文件大小限制：单文档<50MB
- 内存限制：JVM堆<1GB
- 不支持OCR（图片转文字）

### 利益相关者

- 用户：需要快速导入各种格式文档
- 管理员：关注文件安全和存储
- 开发：需要易扩展的解析器架构

## Goals / Non-Goals

**Goals:**
1. 支持PDF、Word、Excel三种格式解析
2. 提取文档文本和元数据
3. 统一的解析器接口，易于扩展
4. 异步处理大文件
5. 文档安全校验

**Non-Goals:**
- 不支持OCR（图片转文字）
- 不支持扫描版PDF
- 不支持PowerPoint（后续版本）
- 不实现文档编辑功能
- 不实现文档版本控制

## Decisions

### 决策1: PDF解析器选择

**选择**：Apache PDFBox 2.0.x

**理由**：
- 成熟稳定的PDF库
- 支持文本提取和表格解析
- Java原生实现，无外部依赖
- Apache 2.0许可

**替代方案**：
- ❌ iText 7：商业许可证限制
- ❌ Ghost4j：需要native库
- ✅ **Apache PDFBox**：开源免费

### 决策2: Office文档解析器

**选择**：Apache POI 5.2.x

**理由**：
- 官方推荐库
- 支持.docx和.xlsx格式
- 活跃维护
- 与Spring Boot集成良好

### 决策3: 解析器架构

**选择**：策略模式 + 工厂模式

**理由**：
- 统一的解析器接口`DocumentParser`
- 每种格式一个实现类
- 工厂类根据文件扩展名选择解析器
- 易于扩展新格式

**架构**：
```java
interface DocumentParser {
    ParsedDocument parse(InputStream input) throws ParseException;
}

class PdfParser implements DocumentParser { }
class WordParser implements DocumentParser { }
class ExcelParser implements DocumentParser { }

class DocumentParserFactory {
    DocumentParser getParser(String fileExtension);
}
```

### 决策4: 大文件处理策略

**选择**：流式处理 + 分块上传

**理由**：
- 避免一次性加载大文件到内存
- 使用流式API边读边处理
- 分块上传支持断点续传

**实现**：
- PDF：使用PdfParser按页解析
- Word：使用XWPFDocument事件处理
- Excel：使用SXSSF流式API

### 决策5: 文档存储策略

**选择**：原始文件存储 + 解析结果存储

**理由**：
- 保留原始文件便于重新解析
- 解析结果存储到kb_document表
- 文件系统存储原始文件（MinIO/OSS）

**存储位置**：
- 原始文件：`/data/documents/{year}/{month}/{docId}.{ext}`
- 解析结果：MySQL kb_document表

### 决策6: 安全性

**选择**：文件类型白名单 + 病毒扫描接口

**理由**：
- 只允许上传PDF/DOCX/XLSX
- 文件头验证（Magic Number）
- 留下病毒扫描接口（集成ClamAV）

## Risks / Trade-offs

### 风险1: 内存溢出
**风险**：大文件解析可能耗尽JVM内存

**缓解措施**：
- 限制单文档<50MB
- 使用流式处理
- 实现内存监控

### 风险2: 恶意文件
**风险**：用户上传包含恶意代码的文档

**缓解措施**：
- 文件头验证
- 文件大小限制
- 仅解析文本，不执行宏
- 集成病毒扫描（可选）

### 风险3: 解析失败
**风险**：损坏或加密文档无法解析

**缓解措施**：
- 详细的错误日志
- 友好的错误提示
- 保留原始文件便于手动处理

### 权衡1: 解析精度 vs 性能
**权衡**：完整解析（图片、表格、样式）耗时更长

**决策**：优先提取文本内容，格式信息作为元数据

### 权衡2: 同步 vs 异步
**权衡**：同步解析简单但可能阻塞

**决策**：
- 小文件（<5MB）：同步解析
- 大文件（>=5MB）：异步解析

## Migration Plan

### 阶段1: 基础设施（1周）
1. 添加依赖到build.gradle
2. 创建document-parser模块
3. 实现解析器接口和工厂

### 阶段2: PDF解析器（1周）
1. 实现PdfParser
2. 添加PDF文本提取
3. 添加PDF表格解析
4. 单元测试

### 阶段3: Office解析器（1周）
1. 实现WordParser
2. 实现ExcelParser
3. 添加单元测试

### 阶段4: API集成（1周）
1. 实现文档上传API
2. 实现文件校验
3. 集成到知识库流程

### 回滚策略
- 新增模块，可独立回滚
- 通过配置禁用文档解析功能
- 保留手动文本输入方式

## Open Questions

1. **Q**: 是否需要支持加密PDF？
   **A**: v1.0不支持，作为后续增强

2. **Q**: 是否需要提取文档中的图片？
   **A**: v1.0不提取，保存原始文件供后续处理

3. **Q**: 是否需要支持多语言文档？
   **A**: 解析器本身支持多语言，但元数据提取主要针对中文和英文

## Performance Estimates

基于文档大小和复杂度的预估解析时间：

| 文档类型 | 大小 | 页数/行数 | 预估时间 |
|---------|------|-----------|---------|
| PDF | 1MB | 10页 | ~1s |
| PDF | 10MB | 100页 | ~8s |
| Word | 1MB | 10页 | ~0.5s |
| Excel | 1MB | 1000行 | ~0.8s |

## API Design

### 文档上传API

```
POST /api/v1/documents/upload
Content-Type: multipart/form-data

Request:
- file: 文档文件
- kb_id: 知识库ID（可选）
- metadata: 元数据（可选）

Response:
{
  "code": "200",
  "data": {
    "documentId": "doc-123",
    "status": "completed",
    "text": "解析后的文本...",
    "metadata": {...}
  }
}
```

### 异步解析状态查询

```
GET /api/v1/documents/{documentId}/status

Response:
{
  "code": "200",
  "data": {
    "documentId": "doc-123",
    "status": "processing",
    "progress": 0.6
  }
}
```

## Error Handling

### 错误码定义

| 错误码 | 描述 | HTTP状态 |
|--------|------|---------|
| INVALID_FILE_TYPE | 不支持的文件类型 | 400 |
| FILE_TOO_LARGE | 文件超过大小限制 | 413 |
| PARSE_FAILED | 文档解析失败 | 422 |
| ENCRYPTED_FILE | 加密文档不支持 | 422 |
| CORRUPT_FILE | 文件损坏 | 422 |

## Security

### 文件类型白名单

```
允许的文件扩展名：
- .pdf
- .docx
- .xlsx
```

### 文件头验证

每个文件类型验证Magic Number：

| 类型 | Magic Number |
|------|--------------|
| PDF | `%PDF-` |
| DOCX | `PK\x03\x04` (ZIP) |
| XLSX | `PK\x03\x04` (ZIP) |

### 权限控制

- 仅认证用户可上传文档
- 文档所有者可查看和删除
- 管理员可查看所有文档
