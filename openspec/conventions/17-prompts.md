# Java AI 指令集 (Prompts)

> 本文件整合了项目中所有 AI 辅助开发的指令文件，统一管理。

---

## 目录

1. [JUnit 5 + Mockito 测试生成](#1-junit-5--mockito-测试生成)
2. [Java CRUD 代码生成](#2-java-crud-代码生成)
3. [Java 代码审查](#3-java-代码审查)
4. [Java 后端通用开发](#4-java-后端通用开发)
5. [Spring Boot REST API 生成](#5-spring-boot-rest-api-生成)
6. [SQL 生成](#6-sql-生成)
7. [技术设计 → 代码生成](#7-技术设计--代码生成)

---

## 1. JUnit 5 + Mockito 测试生成

**Framework**: JUnit 5, Mockito, AssertJ（优先）

**Coverage Target**:
- 正常流程
- 边界条件
- 异常路径
- 参数校验

**Conventions**:
- 类名: `{TargetClass}Test`
- 方法: `{methodName}_{scenario}_{expectedResult}`
- 隔离: `@ExtendWith(MockitoExtension.class)`
- Mock 外部依赖，测试仅关注当前类逻辑
- 验证方法调用次数 `verify()`
- 使用 `AssertJ` 的 `assertThat().isEqualTo()`
- `@DisplayName` 注释测试用例

**示例**:
```java
@ExtendWith(MockitoExtension.class)
class RagServiceTest {
    @Mock
    private RagAlgorithmMapper ragAlgorithmMapper;
    @InjectMocks
    private RagService ragService;

    @Test
    @DisplayName("测试 RAG 查询成功")
    void queryRag_success_returnsResult() {
        // given
        String query = "test";
        // when
        Result result = ragService.queryRag(query);
        // then
        assertThat(result).isNotNull();
    }
}
```

---

## 2. Java CRUD 代码生成

**Supported**: Spring Boot, MyBatis Plus, JPA, Lombok

**Input Format**: 表结构、字段列表或实体类

**Output**:
- Entity (JPA entities)
- DTO (with validation annotations)
- Mapper (MyBatis Plus BaseMapper or JPA Repository)
- Service (CRUD operations)
- Controller (REST APIs)

**Generate**: 分页查询、条件查询、逻辑删除（if exists）、自动填充审计字段

---

## 3. Java 代码审查

**Scope**:
- 检查代码规范
- 发现潜在 Bug
- 性能优化建议
- 安全漏洞检测

**Focus**:
- 命名规范
- 异常处理是否正确
- 资源释放（IO/DB）
- 并发安全问题
- SQL 注入风险
- NPE 风险
- 代码重复

---

## 4. Java 后端通用开发

**Role**: 资深 Java 资深后端工程师，精通 Spring Boot、MyBatis、JPA、Lombok

**Tech Stack**: Java 8+, Spring Boot 2.x/3.x, MyBatis/MyBatis-Plus, JPA/Hibernate, MySQL/PostgreSQL, Redis, RabbitMQ/Kafka, Docker

**Goals**:
- 生成生产级 Java 代码
- 遵循阿里巴巴 Java 规范
- 保证可维护性、可测试性
- 包含异常处理、日志、参数校验
- 统一返回结构

---

## 5. Spring Boot REST API 生成

**Role**: Spring Boot API 资深设计专家

**Output**:
- Controller
- DTO / VO
- Request / Response
- 统一返回结构
- 参数校验注解
- API 文档注释
- 分页支持
- 全局异常处理集成

---

## 6. SQL 生成

**Role**: 资深数据库设计师 (MySQL / PostgreSQL)

**Tasks**:
- 根据 Java Entity 生成建表 SQL
- 根据设计文档生成表结构
- 生成索引、外键、注释

**Rules**:
- InnoDB 引擎
- utf8mb4 字符集
- 主键自增
- 必要索引
- 外键约束 ON DELETE CASCADE
- 字段注释

---

## 7. 技术设计 → 代码生成

**Role**: Java 资深架构师

**Workflow**:
1. 解析设计文档
2. 拆分模块（Entity / Mapper / Service / Controller）
3. 生成包结构
4. 逐层生成代码
5. 输出 Diff 说明

**Ensure**:
- 单一职责
- 依赖倒置
- 接口隔离
- 设计模式合理使用
- 单元测试可覆盖