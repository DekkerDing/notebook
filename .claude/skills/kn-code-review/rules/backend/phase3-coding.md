# Phase 3: 代码规范

> 规范依据: `openspec/conventions/02-coding-standards.md`

## 3.1 命名规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 类命名符合规范（AppService/PO/DTO/Enum/Repository 等） | 02-coding-standards.md §2 | 强制 | 🚫 |
| 包名使用小写，禁用缩写 | 02-coding-standards.md §1 | 强制 | ❌ |
| 常量命名全大写下划线分隔 | 02-coding-standards.md §4 | 强制 | ❌ |
| 禁止魔法值 | 02-coding-standards.md §4 | 强制 | 🚫 |
| 方法参数使用语义化命名（禁止 Long id） | 02-coding-standards.md §18 | 强制 | ⚠️ |

## 3.2 Controller 规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 使用 @RestController + @RequestMapping | 02-coding-standards.md §3.1 | 强制 | 🚫 |
| 使用 @Valid 触发参数校验 | 02-coding-standards.md §3.2 | 强制 | 🚫 |
| 使用 @BusinessOperator SessionUserInfo 获取用户 | 02-coding-standards.md §9 | 强制 | 🚫 |
| 禁止通过 @RequestHeader 获取用户信息 | 02-coding-standards.md §9 | 禁止 | 🚫 |
| 响应使用 CommonResponse<T> 封装 | 02-coding-standards.md §3.3 | 强制 | 🚫 |
| 分页参数使用 BasePageReqDTO | 02-coding-standards.md §8 | 强制 | ❌ |

## 3.3 DTO/VO/PO 规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 实现 Serializable + 声明 serialVersionUID | 02-coding-standards.md §5 | 强制 | ❌ |
| 日期使用 LocalDateTime（禁止 Date） | 02-coding-standards.md §5 | 强制 | 🚫 |
| Request DTO 使用 @NotNull/@NotBlank/@Size 校验 | 02-coding-standards.md §6 | 强制 | 🚫 |
| Response VO 使用 @ApiModelProperty | 02-coding-standards.md §5 | 强制 | ❌ |
| 枚举包含 code + desc + fromCode 方法 | 02-coding-standards.md §13 | 强制 | ❌ |

## 3.4 Service 层规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 使用 @RequiredArgsConstructor 构造器注入 | 02-coding-standards.md §11 | 强制 | 🚫 |
| 依赖字段声明为 final | 02-coding-standards.md §11 | 强制 | ❌ |
| 禁止 @Autowired 字段注入 | 02-coding-standards.md §11 | 禁止 | 🚫 |
| @Transactional 注解说明事务特性 | 02-coding-standards.md §10.1 | 强制 | ❌ |
| 使用 Converter 进行对象转换（禁止内联） | 02-coding-standards.md §14 | 强制 | ⚠️ |

## 3.5 Repository 层规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 接口定义在 domain 层 | 02-coding-standards.md §17.1 | 强制 | 🚫 |
| 实现定义在 infra 层 | 02-coding-standards.md §17.2 | 强制 | 🚫 |
| 使用 LambdaQueryWrapper/LambdaUpdateWrapper | 03-database.md §7.2 | 强制 | ❌ |
| 更新操作必须带 tenantId 条件 | 03-database.md §7.2 | 强制 | 🚫 |
| pageSize 服务端最大 100 校验 | 03-database.md §10.2 | 强制 | ❌ |

## 3.6 注释规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| 类、公共方法添加 JavaDoc | 02-coding-standards.md §10 | 强制 | ⚠️ |
| 复杂逻辑添加行内注释 | 02-coding-standards.md §10 | 强制 | ⚠️ |
| domain 模块领域对象添加 JavaDoc | 02-coding-standards.md §10 | 强制 | ⚠️ |
| 禁止无意义注释 | 02-coding-standards.md §10 | 禁止 | ⚠️ |
| 修改代码同步更新注释 | 02-coding-standards.md §10 | 强制 | ⚠️ |

## 3.7 Lombok 使用规范审查

| 检查项 | 规范位置 | 判定 | 优先级 |
|--------|----------|------|--------|
| domain Model 禁止 @Data | 02-coding-standards.md §12 | 禁止 | 🚫 |
| 值对象使用 @Value | 02-coding-standards.md §12 | 推荐 | ⚠️ |
| PO 使用 @Data + @TableName | 02-coding-standards.md §12 | 强制 | 🚫 |
| Service/Repository 使用 @RequiredArgsConstructor | 02-coding-standards.md §12 | 强制 | 🚫 |
| 日志类使用 @Slf4j（禁止手动创建 Logger） | 02-coding-standards.md §12 | 强制 | ❌ |