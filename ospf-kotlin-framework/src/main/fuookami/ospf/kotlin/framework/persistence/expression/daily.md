# OSPF Kotlin Framework Expression 实施计划

日期：2026-04-07
来源文档：`E:/workspace/ospf-kotlin/ospf-kotlin-framework/sql_expression.md`
前置条件：ospf-kotlin-math 的 Expression 系统已完成（Phase M0-M5）

## 1. 目标

在 `framework.persistence.expression` 实现以下能力：

1. **字段映射层**：`EntityMeta<E>` / `FieldBinding` - PO 字段到数据库列的映射
2. **排序模型**：`SortBy` / `SortItem` - 多字段排序支持
3. **更新赋值模型**：`UpdateAssignment` - UPDATE SET 语句支持
4. **Ktorm 翻译器**：
   - `KtormBooleanTranslator` - BooleanExpression -> Ktorm WHERE 条件
   - `KtormOrderByTranslator` - SortBy -> Ktorm ORDER BY
   - `KtormUpdateTranslator` - UpdateAssignment -> Ktorm UPDATE SET

## 2. 包结构

### 2.1 ospf-kotlin-framework (核心接口层)

```
framework.persistence.expression/
├── README.md                      # 模块说明
├── EntityMeta.kt                  # 实体元数据
├── FieldBinding.kt                # 字段绑定
├── SortBy.kt                      # 排序模型
├── UpdateAssignment.kt            # 更新赋值模型
└── RepositoryApi.kt               # 仓储查询接口 (ExpressionRepository<E>)
```

### 2.2 ospf-kotlin-framework-plugin-persistence-ktorm (Ktorm 实现)

```
framework.persistence.expression/
├── KtormRepository.kt             # KtormRepository<E> 抽象类
└── translator/
    ├── KtormBooleanTranslator.kt  # 布尔表达式翻译器
    ├── KtormOrderByTranslator.kt  # 排序翻译器
    ├── KtormUpdateTranslator.kt   # 更新翻译器
    ├── PatternMatchPolicy.kt      # 模式匹配方言策略
    └── package.kt
```

### 2.3 ospf-kotlin-framework-plugin-persistence-mybatis (MyBatis-Plus 实现) - 待开发

```
framework.persistence.expression/
├── MybatisRepository.kt           # MybatisRepository<E> 抽象类
└── translator/
    ├── MybatisBooleanTranslator.kt  # 布尔表达式翻译器
    ├── MybatisOrderByTranslator.kt  # 排序翻译器
    ├── MybatisUpdateTranslator.kt   # 更新翻译器
    └── package.kt
```

## 3. 分阶段实施计划

### Phase F0（0.5 天）：脚手架与依赖

**实施项：**
1. 在 `pom.xml` 添加 `ospf-kotlin-math` 依赖
2. 创建 `persistence.expression` 包结构
3. 创建基础 README

**验收标准：**
1. 模块可编译
2. 可引用 `math.symbol.expression.*`

---

### Phase F1（1 天）：字段映射与排序模型

**新增文件：**
1. `EntityMeta.kt` - 实体元数据定义
2. `FieldBinding.kt` - 字段绑定
3. `SortBy.kt` - 排序模型

**实施细节：**

#### EntityMeta.kt
```kotlin
/**
 * 实体元数据
 * Entity Metadata
 *
 * 管理 PO 字段到数据库列的映射。
 */
class EntityMeta<E : Any>(
    val entityClass: KClass<E>,
    val tableName: String,
    private val bindings: Map<PropertyPath, FieldBinding<*>>
) {
    /**
     * 获取字段绑定
     */
    operator fun get(path: PropertyPath): FieldBinding<*>? = bindings[path]

    /**
     * 解析路径为列
     */
    fun resolveColumn(path: PropertyPath): ColumnDeclaring<*>?

    companion object {
        /**
         * 从 PO 类创建元数据（使用反射）
         */
        inline fun <reified E : Any> from(
            tableName: String,
            block: EntityMetaBuilder<E>.() -> Unit
        ): EntityMeta<E>
    }
}
```

#### FieldBinding.kt
```kotlin
/**
 * 字段绑定
 * Field Binding
 *
 * 定义 PO 字段到数据库列的映射关系。
 */
data class FieldBinding<T : Any>(
    val path: PropertyPath,
    val column: ColumnDeclaring<T>,
    val transformer: ValueTransformer<T>? = null
)

/**
 * 值转换器
 */
interface ValueTransformer<T : Any> {
    fun toColumn(value: Any?): T?
    fun fromColumn(value: T?): Any?
}
```

#### SortBy.kt
```kotlin
/**
 * 排序定义
 * Sort Definition
 */
data class SortBy(
    val items: List<SortItem>
) {
    companion object {
        val empty = SortBy(emptyList())

        fun asc(path: String): SortBy = SortBy(listOf(SortItem(path, SortDirection.Asc)))
        fun desc(path: String): SortBy = SortBy(listOf(SortItem(path, SortDirection.Desc)))
    }

    operator fun plus(other: SortBy): SortBy = SortBy(items + other.items)
}

/**
 * 排序项
 * Sort Item
 */
data class SortItem(
    val path: String,
    val direction: SortDirection,
    val nulls: NullsOrder? = null
)

/**
 * 排序方向
 */
enum class SortDirection {
    Asc, Desc
}

/**
 * 空值排序策略
 */
enum class NullsOrder {
    NullsFirst, NullsLast
}
```

**验收标准：**
1. `EntityMeta` 可正确映射字段
2. `SortBy` 支持多字段组合
3. 支持空值排序配置

---

### Phase F2（1.5 天）：Ktorm 翻译器

**新增文件：**
1. `translator/KtormBooleanTranslator.kt`
2. `translator/KtormOrderByTranslator.kt`
3. `translator/PatternMatchPolicy.kt`

**实施细节：**

#### KtormBooleanTranslator.kt
```kotlin
/**
 * Ktorm 布尔表达式翻译器
 * Ktorm Boolean Expression Translator
 *
 * 将 BooleanExpression 翻译为 Ktorm ColumnDeclaring<Boolean>
 */
class KtormBooleanTranslator(
    private val meta: EntityMeta<*>,
    private val patternMatchPolicy: PatternMatchPolicy = DefaultPatternMatchPolicy
) {
    /**
     * 翻译布尔表达式
     */
    fun translate(expr: BooleanExpression): ColumnDeclaring<Boolean>?
}

// 便捷扩展函数
fun BooleanExpression.toKtormWhere(
    meta: EntityMeta<*>,
    policy: PatternMatchPolicy = DefaultPatternMatchPolicy
): ColumnDeclaring<Boolean>?
```

**翻译规则：**
| BooleanExpression | Ktorm 等价 |
|-------------------|------------|
| `Comparison(Eq, a, b)` | `col(a) eq col(b)` |
| `Comparison(Ne, a, b)` | `col(a) neq col(b)` |
| `Comparison(Lt, a, b)` | `col(a) lt col(b)` |
| `Comparison(Le, a, b)` | `col(a) lte col(b)` |
| `Comparison(Gt, a, b)` | `col(a) gt col(b)` |
| `Comparison(Ge, a, b)` | `col(a) gte col(b)` |
| `InExpression(value, candidates)` | `col(value) inList cols(candidates)` |
| `NullCheck(path, IsNull)` | `col(path) isNull` |
| `NullCheck(path, IsNotNull)` | `col(path) isNotNull` |
| `AndExpression(operands)` | `operands.map { translate(it) }.reduce { a, b -> a and b }` |
| `OrExpression(operands)` | `operands.map { translate(it) }.reduce { a, b -> a or b }` |
| `NotExpression(operand)` | `translate(operand).not()` |
| `PatternMatch(...)` | 方言策略处理 |

#### KtormOrderByTranslator.kt
```kotlin
/**
 * Ktorm 排序翻译器
 */
class KtormOrderByTranslator(
    private val meta: EntityMeta<*>,
    private val nullsOrderSupport: NullsOrderSupport = NullsOrderSupport.Auto
) {
    /**
     * 应用排序到查询
     */
    fun apply(query: QuerySource, sortBy: SortBy): QuerySource
}

enum class NullsOrderSupport {
    Auto,       // 自动检测数据库支持
    Always,     // 总是使用 NULLS FIRST/LAST
    Never,      // 从不使用，降级处理
    OnlyAsc     // 仅在 ASC 时支持
}
```

#### PatternMatchPolicy.kt
```kotlin
/**
 * 模式匹配方言策略
 */
interface PatternMatchPolicy {
    /**
     * 翻译 LIKE 模式
     */
    fun translateLike(column: ColumnDeclaring<*>, pattern: String, caseSensitive: Boolean): ColumnDeclaring<Boolean>

    /**
     * 翻译正则匹配
     */
    fun translateRegex(column: ColumnDeclaring<*>, pattern: String): ColumnDeclaring<Boolean>?
}

/**
 * 默认策略（标准 SQL）
 */
object DefaultPatternMatchPolicy : PatternMatchPolicy {
    override fun translateLike(column: ColumnDeclaring<*>, pattern: String, caseSensitive: Boolean): ColumnDeclaring<Boolean> {
        return if (caseSensitive) {
            column.like(pattern)
        } else {
            // 使用 LOWER() 函数
            column.like(pattern.lowercase())
        }
    }
}

/**
 * SQLite 策略
 */
object SqlitePatternMatchPolicy : PatternMatchPolicy

/**
 * PostgreSQL 策略（支持 ILIKE）
 */
object PostgresPatternMatchPolicy : PatternMatchPolicy
```

**验收标准：**
1. BooleanExpression 全部类型可翻译
2. SortBy 多字段排序正确
3. PatternMatch 方言策略可切换

---

### Phase F3（1 天）：更新模型与翻译器

**新增文件：**
1. `UpdateAssignment.kt`
2. `translator/KtormUpdateTranslator.kt`

**实施细节：**

#### UpdateAssignment.kt
```kotlin
/**
 * 更新赋值集合
 */
data class UpdateAssignments(
    val items: List<UpdateAssignment>
) {
    companion object {
        val empty = UpdateAssignments(emptyList())

        fun set(path: String, value: Any?): UpdateAssignments
        fun setNull(path: String): UpdateAssignments
        fun setExpr(path: String, expr: ScalarExpression<*>): UpdateAssignments
    }

    operator fun plus(other: UpdateAssignments): UpdateAssignments
}

/**
 * 更新赋值项
 */
sealed interface UpdateAssignment {
    val path: String
}

/**
 * 设置值
 */
data class SetValue(
    override val path: String,
    val value: Any?
) : UpdateAssignment

/**
 * 设置 NULL
 */
data class SetNull(
    override val path: String
) : UpdateAssignment

/**
 * 从表达式设置
 */
data class SetFromExpression(
    override val path: String,
    val expression: ScalarExpression<*>
) : UpdateAssignment
```

#### KtormUpdateTranslator.kt
```kotlin
/**
 * Ktorm 更新翻译器
 */
class KtormUpdateTranslator(
    private val meta: EntityMeta<*>
) {
    /**
     * 应用更新到 builder
     */
    fun apply(builder: UpdateBuilder, assignments: UpdateAssignments)

    /**
     * 翻译标量表达式为 Ktorm 表达式
     */
    fun translateScalar(expr: ScalarExpression<*>): Any?
}
```

**验收标准：**
1. SetValue/SetNull/SetFromExpression 全部可用
2. 类型校验早失败
3. 与 WHERE 条件组合正确

---

### Phase F4（0.5 天）：仓储 API

**新增文件：**
1. `RepositoryApi.kt`

**实施细节：**

```kotlin
/**
 * 表达式仓储接口
 */
interface ExpressionRepository<E : Any> {
    /**
     * 查询
     */
    fun find(where: BooleanExpression): List<E>
    fun find(where: BooleanExpression, sortBy: SortBy?, limit: Int?, offset: Int?): List<E>

    /**
     * 计数
     */
    fun count(where: BooleanExpression): Long

    /**
     * 更新
     */
    fun update(where: BooleanExpression, assignments: UpdateAssignments): Int

    /**
     * 删除
     */
    fun delete(where: BooleanExpression): Int
}

/**
 * 抽象 Ktorm 仓储实现
 */
abstract class KtormRepository<E : Any>(
    protected val database: Database,
    protected val meta: EntityMeta<E>
) : ExpressionRepository<E> {
    // 实现通用方法
}
```

**验收标准：**
1. CRUD 操作完整
2. 分页排序正确
3. 类型安全

---

### Phase F5（0.5 天）：文档与集成测试

**新增文件：**
1. `README.md` / `README_ch.md`
2. 测试文件

**测试清单：**
- `EntityMetaTest.kt`
- `KtormBooleanTranslatorTest.kt`
- `KtormOrderByTranslatorTest.kt`
- `KtormUpdateTranslatorTest.kt`
- `RepositoryIntegrationTest.kt`

---

## 4. 依赖变更

**pom.xml 需添加：**
```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-math</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## 5. 风险与控制

| 风险 | 控制措施 |
|------|----------|
| PatternMatch 方言差异 | `PatternMatchPolicy` 策略模式 |
| 列类型不一致 | `EntityMeta` 类型校验早失败 |
| NULLS ORDER 支持度 | 方言检测 + 降级策略 |
| 更新表达式类型错误 | 翻译前类型兼容校验 |

---

## 6. 时间估算

| 阶段 | 预估时间 | 状态 |
|------|----------|------|
| F0 | 0.5 天 | ✅ 已完成 |
| F1 | 1 天 | ✅ 已完成 |
| F2 | 1.5 天 | ✅ 已完成 |
| F3 | 1 天 | ✅ 已完成 |
| F4 | 0.5 天 | ✅ 已完成 |
| F5 | 0.5 天 | ✅ 已完成 |
| F6 (迁移到插件) | 0.5 天 | ✅ 已完成 |
| **总计** | **5.5 天** | |

## 7. 完成状态

| 阶段 | 状态 | 提交 |
|------|------|------|
| F0 | ✅ 已完成 | 8beaffbb |
| F1 | ✅ 已完成 | 923e8dbd |
| F2 | ✅ 已完成 | 52268758 |
| F3 | ✅ 已完成 | 0dc2053a |
| F4 | ✅ 已完成 | 2eb37d95 |
| F5 | ✅ 已完成 | bf362f8d |
| F6 | ✅ 已完成 | (待提交) |

### 新增文件清单

**F0 Scaffolding:**
- `expression/package.kt`
- `expression/translator/package.kt`
- `expression/README.md` / `README_ch.md`

**F1 Models:**
- `expression/FieldBinding.kt` - 字段绑定定义
- `expression/EntityMeta.kt` - 实体元数据
- `expression/SortBy.kt` - 排序模型

**F2 Translators:**
- `translator/PatternMatchPolicy.kt` - 模式匹配方言策略
- `translator/KtormBooleanTranslator.kt` - 布尔表达式翻译器
- `translator/KtormOrderByTranslator.kt` - 排序翻译器

**F3 Update:**
- `expression/UpdateAssignment.kt` - 更新赋值模型
- `translator/KtormUpdateTranslator.kt` - 更新翻译器

**F4 Repository:**
- `expression/RepositoryApi.kt` - 仓储接口

**F6 迁移到插件模块:**
- 将 translator/ 从 framework 迁移到 `ospf-kotlin-framework-plugin-persistence-ktorm`
- 在 framework 中保留 `EntityMeta`, `FieldBinding`, `SortBy`, `UpdateAssignment`, `RepositoryApi` (接口)
- 在 plugin 中实现 `KtormRepository` 和各翻译器

### 关键设计决策

1. **EntityMeta** 使用 `PropertyPath` 作为字段引用主语义
2. **PatternMatchPolicy** 策略模式处理不同数据库的 LIKE/ILIKE 差异
3. **KtormRepository** 提供抽象基类，子类只需实现 `mapToEntity`
4. **UpdateAssignments** 支持 `set/setNull/setExpr` 链式调用
5. **模块分离**: framework 提供接口和模型，plugin 提供具体 ORM 实现

---

## 8. MyBatis-Plus 支持计划 (Phase F7)

### 8.1 目标

在 `ospf-kotlin-framework-plugin-persistence-mybatis` 模块实现基于 MyBatis-Plus 的 Expression 翻译和仓储。

### 8.2 包结构

```
ospf-kotlin-framework-plugin-persistence-mybatis/
├── pom.xml
└── src/main/fuookami/ospf/kotlin/framework/persistence/expression/
    ├── MybatisRepository.kt           # MyBatis 仓储实现
    ├── MybatisEntityMeta.kt           # MyBatis 实体元数据扩展
    └── translator/
        ├── MybatisBooleanTranslator.kt  # BooleanExpression -> Wrapper
        ├── MybatisOrderByTranslator.kt  # SortBy -> OrderByItem
        ├── MybatisUpdateTranslator.kt   # UpdateAssignment -> SetSql
        └── package.kt
```

### 8.3 分阶段实施

#### Phase F7.1 (0.5 天): 模块初始化

**实施项：**
1. 创建 `ospf-kotlin-framework-plugin-persistence-mybatis` 模块
2. 添加依赖：
   - `ospf-kotlin-framework`
   - `mybatis-plus-boot-starter`
3. 创建基础包结构

**pom.xml:**
```xml
<dependencies>
    <dependency>
        <groupId>io.github.fuookami.ospf.kotlin</groupId>
        <artifactId>ospf-kotlin-framework</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>
</dependencies>
```

---

#### Phase F7.2 (1 天): MyBatis 翻译器

**新增文件：**
1. `translator/MybatisBooleanTranslator.kt`
2. `translator/MybatisOrderByTranslator.kt`
3. `translator/MybatisUpdateTranslator.kt`

**实施细节：**

##### MybatisBooleanTranslator.kt
```kotlin
/**
 * MyBatis 布尔表达式翻译器
 * MyBatis Boolean Expression Translator
 *
 * 将 BooleanExpression 翻译为 MyBatis-Plus Wrapper 条件。
 */
class MybatisBooleanTranslator<T : Any>(
    private val meta: EntityMeta<T>
) {
    /**
     * 翻译为 QueryWrapper 条件
     */
    fun translate(wrapper: QueryWrapper<T>, expr: BooleanExpression): QueryWrapper<T>

    /**
     * 翻译为 LambdaQueryWrapper 条件
     */
    fun translate(wrapper: LambdaQueryWrapper<T>, expr: BooleanExpression): LambdaQueryWrapper<T>
}

// 翻译规则示例：
// Comparison(Eq, ref, const) -> wrapper.eq(column, value)
// Comparison(Ne, ref, const) -> wrapper.ne(column, value)
// AndExpression(operands) -> operands.forEach { translate(wrapper, it) }
// OrExpression(operands) -> wrapper.and { w -> ... }
// NullCheck(path, IsNull) -> wrapper.isNull(column)
```

##### MybatisOrderByTranslator.kt
```kotlin
/**
 * MyBatis 排序翻译器
 */
class MybatisOrderByTranslator<T : Any>(
    private val meta: EntityMeta<T>
) {
    /**
     * 应用排序到 Wrapper
     */
    fun apply(wrapper: QueryWrapper<T>, sortBy: SortBy): QueryWrapper<T>

    /**
     * 应用排序到 LambdaQueryWrapper
     */
    fun apply(wrapper: LambdaQueryWrapper<T>, sortBy: SortBy): LambdaQueryWrapper<T>
}

// 翻译规则：
// SortItem(path, Asc) -> wrapper.orderByAsc(column)
// SortItem(path, Desc) -> wrapper.orderByDesc(column)
// nulls order -> 使用 last("NULLS FIRST/LAST") 或忽略
```

##### MybatisUpdateTranslator.kt
```kotlin
/**
 * MyBatis 更新翻译器
 */
class MybatisUpdateTranslator<T : Any>(
    private val meta: EntityMeta<T>
) {
    /**
     * 应用更新到 UpdateWrapper
     */
    fun apply(wrapper: UpdateWrapper<T>, assignments: UpdateAssignments): UpdateWrapper<T>
}

// 翻译规则：
// SetValue(path, value) -> wrapper.set(column, value)
// SetNull(path) -> wrapper.setNull(column)
// SetFromExpression(path, expr) -> wrapper.setSql("$column = $expr")
```

---

#### Phase F7.3 (0.5 天): MyBatis 仓储实现

**新增文件：**
1. `MybatisRepository.kt`

**实施细节：**

```kotlin
/**
 * MyBatis 仓储实现
 * MyBatis Repository Implementation
 *
 * 基于 MyBatis-Plus BaseMapper 的仓储实现。
 */
abstract class MybatisRepository<E : Any, M : BaseMapper<E>>(
    protected val mapper: M,
    protected val meta: EntityMeta<E>
) : ExpressionRepository<E> {

    private val booleanTranslator = MybatisBooleanTranslator(meta)
    private val orderByTranslator = MybatisOrderByTranslator(meta)
    private val updateTranslator = MybatisUpdateTranslator(meta)

    override fun find(where: BooleanExpression): List<E> {
        val wrapper = QueryWrapper<E>()
        booleanTranslator.translate(wrapper, where)
        return mapper.selectList(wrapper)
    }

    override fun find(
        where: BooleanExpression,
        sortBy: SortBy?,
        limit: Int?,
        offset: Int?
    ): List<E> {
        var wrapper = QueryWrapper<E>()
        booleanTranslator.translate(wrapper, where)

        if (sortBy != null && sortBy.isNotEmpty()) {
            orderByTranslator.apply(wrapper, sortBy)
        }

        if (limit != null) {
            wrapper.last("LIMIT $limit")
        }
        if (offset != null) {
            wrapper.last("OFFSET $offset")
        }

        return mapper.selectList(wrapper)
    }

    override fun count(where: BooleanExpression): Long {
        val wrapper = QueryWrapper<E>()
        booleanTranslator.translate(wrapper, where)
        return mapper.selectCount(wrapper)
    }

    override fun update(where: BooleanExpression, assignments: UpdateAssignments): Int {
        var wrapper = UpdateWrapper<E>()
        booleanTranslator.translate(wrapper, where)
        updateTranslator.apply(wrapper, assignments)
        return mapper.update(null, wrapper)
    }

    override fun delete(where: BooleanExpression): Int {
        val wrapper = QueryWrapper<E>()
        booleanTranslator.translate(wrapper, where)
        return mapper.delete(wrapper)
    }
}
```

---

#### Phase F7.4 (0.5 天): 测试与文档

**测试清单：**
- `MybatisBooleanTranslatorTest.kt`
- `MybatisOrderByTranslatorTest.kt`
- `MybatisUpdateTranslatorTest.kt`
- `MybatisRepositoryIntegrationTest.kt`

---

### 8.4 翻译规则对比

| BooleanExpression | Ktorm | MyBatis-Plus |
|-------------------|-------|--------------|
| `Comparison(Eq, ref, const)` | `col eq value` | `wrapper.eq(col, value)` |
| `Comparison(Ne, ref, const)` | `col neq value` | `wrapper.ne(col, value)` |
| `Comparison(Lt, ref, const)` | `col lt value` | `wrapper.lt(col, value)` |
| `InExpression(value, list)` | `col inList list` | `wrapper.in(col, list)` |
| `NullCheck(path, IsNull)` | `col.isNull()` | `wrapper.isNull(col)` |
| `AndExpression(operands)` | `a and b` | `wrapper.and { ... }` |
| `OrExpression(operands)` | `a or b` | `wrapper.or()` |

---

### 8.5 时间估算

| 阶段 | 预估时间 | 状态 |
|------|----------|------|
| F7.1 模块初始化 | 0.5 天 | ✅ 已完成 |
| F7.2 翻译器实现 | 1 天 | ✅ 已完成 |
| F7.3 仓储实现 | 0.5 天 | ✅ 已完成 |
| F7.4 测试文档 | 0.5 天 | ✅ 已完成 |
| **总计** | **2.5 天** | |

### 8.6 完成状态

| 阶段 | 状态 | 提交 |
|------|------|------|
| F7.1 | ✅ 已完成 | (待提交) |
| F7.2 | ✅ 已完成 | (待提交) |
| F7.3 | ✅ 已完成 | (待提交) |
| F7.4 | ✅ 已完成 | (待提交) |

### 8.7 新增文件清单

**F7 MyBatis-Plus:**
- `ospf-kotlin-framework-plugin-persistence-mybatis/pom.xml` - 模块配置
- `translator/package.kt` - 包定义
- `translator/MybatisBooleanTranslator.kt` - 布尔表达式翻译器
- `translator/MybatisOrderByTranslator.kt` - 排序翻译器
- `translator/MybatisUpdateTranslator.kt` - 更新翻译器
- `MybatisRepository.kt` - MyBatis 仓储实现
- `translator/MybatisBooleanTranslatorTest.kt` - 单元测试

---

### 8.6 风险与控制

| 风险 | 控制措施 |
|------|----------|
| MyBatis-Plus Wrapper 类型复杂 | 使用泛型 + 扩展函数简化 |
| 动态表名支持 | 通过 Meta 传递表名 |
| 批量操作性能 | 提供 batch 方法扩展 |
| 复杂嵌套条件 | 递归翻译 + 子 Wrapper |

---

### P8-1 完全泛型化 Solution 与 Token 写回链路

- 将公开模型侧 solution 概念拆成 `Solution<V>` 与 solver 边界 `SolverSolution`。
- 将 `AbstractTokenTable<V>.setSolution`、`AbstractTokenList<T>.setSolution`、`Model.setSolution` 的主签名切到 `List<V>` / `Map<AbstractVariableItem<*, *>, V>`。
- 仅在 solver adapter 或 `AbstractTokenTableF64` 兼容扩展中保留 `List<F64>` / `Map<..., Flt64>`。
- solver 返回值写回模型时，通过 `SolveValue`/`IntoValue` 从 `Flt64` 转换回 `V`，并按策略处理精度、溢出和非有限值。

### P8-2 泛型化缓存与求值路径

- 将 `TokenCacheContext` 中以 `List<F64>`、`Map<Symbol, Flt64>` 为 key/value 的公开缓存路径改成 `V` typed。
- 将 `Cell.evaluate(solution: List<F64>)`、`evaluateF64`、constraint input、meta constraint result 等接口拆分为模型侧泛型求值与 solver 侧扁平化求值。
- 明确 cache key 是否使用模型值 `V`、solver 值 `F64`，或专用 boundary key，避免泛型模型被 solver 数值域反向污染。

### P8-3 完整化值转换与结构化错误

- 让 `SolveValue` 不只做校验，还提供可组合的 `fromF64` / `toF64` 转换结果。
- 补齐 `NonFinite`、`PrecisionLoss`、`Overflow` 等结构化错误，并让调用链返回可定位的错误信息。
- 复核 Kotlin 默认转换策略，必要时改为与 Rust 一致的 `Strict`，`AllowRounding` 只在显式配置时启用。
- 如 Kotlin 数值层已具备对应类型，补齐 BigDecimal/BigRational 等非 F64 数值域的转换实现与测试。

### P8-4 恢复旧 Kotlin 快捷接口与命名兼容

- 对照 `E:\workspace\ospf-kotlin-main`，补齐旧 `MetaConstraintGroup` 中 monomial、polynomial、inequality、intermediate symbol 的 `addConstraint` 与 `partition` overload。
- 复核 `LinearMetaModel`、`LinearExpressionSymbol`、`LinearExpressionSymbols1`、`LinearIntermediateSymbols` 等旧命名到当前 `*F64` 命名的兼容策略。
- 对照 `E:\workspace\ospf\examples\ospf-kotlin-example`，整理 example 用户迁移时最常见的 import/package/命名差异，必要时提供 typealias 或迁移文档。

### P8-5 收敛 solver/framework 入口命名

- 将 Rust 风格 `solve` / `solveWithOptions` 作为 Kotlin 推荐入口，并检查 core solver、framework solver、column generation、Benders 等模块是否一致。
- 评估 `solveMILP`、`solveLP`、`solveMILPAsync`、`solveLPAsync` 是否保留为兼容 wrapper，或补充统一入口后降级为快捷方法。
- 保持旧 `operator fun invoke` 可用，但文档中应明确主入口，减少 example 与 Rust 文档之间的命名差异。

### P8-6 Rust 功能缺口复核与补齐

- **LP 导出**：Kotlin 已有 `exportLP`，需要按 Rust 的 LP/MIP/QP 输出能力、变量命名、约束命名、目标方向和 bounds 表达逐项验收。
- **IIS**：复核 deletion filtering、elastic filtering、配置项、返回结构和 solver 集成是否与 Rust 对齐。
- **PSO**：Rust core 已有 PSO 求解器，Kotlin 需要确认是否缺失、是否放入 heuristic 模块，或是否明确暂不支持。
- **Solver extension**：复核 Gurobi/SCIP option、callback、IIS、LP dump、日志和异常语义是否与 Rust framework 对齐。
- **元数据消费**：确认 constraint group、lazy、priority、args 等 metadata 不只是可存储，还能被 solver/framework 侧正确消费。

### P8-7 Expression 后续插件与横切能力

- JPA/Hibernate 支持：沿用 Ktorm/MyBatis 插件结构。
- Exposed 支持：提供 Kotlin 原生 SQL DSL translator/repository。
- JOOQ 支持：提供类型安全 SQL 构建器 translator/repository。
- MongoDB 支持：提供 Document 查询翻译。
- 缓存层：基于 expression/repository 的查询缓存。
- 审计日志：自动记录 CRUD 操作与变更上下文。

### P8-8 验证与门禁

- 增加 core/model 公开签名门禁，防止新的 `List<F64>`、`Map<Symbol, Flt64>` 回流到泛型模型接口。
- 增加泛型模型与 F64 solver 边界转换测试，覆盖严格转换、允许舍入、非有限值和溢出。
- 保留 P6/P7 门禁，并新增 P8 泛型化门禁；最终运行 core/framework/example 编译和全量 `mvn clean test`。
