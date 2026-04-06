# OSPF Kotlin Framework Expression 实施计划

日期：2026-04-06
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

```
framework.persistence.expression/
├── README.md                      # 模块说明
├── EntityMeta.kt                  # 实体元数据
├── FieldBinding.kt                # 字段绑定
├── SortBy.kt                      # 排序模型
├── UpdateAssignment.kt            # 更新赋值模型
├── translator/
│   ├── KtormBooleanTranslator.kt  # 布尔表达式翻译器
│   ├── KtormOrderByTranslator.kt  # 排序翻译器
│   ├── KtormUpdateTranslator.kt   # 更新翻译器
│   └── PatternMatchPolicy.kt      # 模式匹配方言策略
└── RepositoryApi.kt               # 仓储查询接口
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

| 阶段 | 预估时间 |
|------|----------|
| F0 | 0.5 天 |
| F1 | 1 天 |
| F2 | 1.5 天 |
| F3 | 1 天 |
| F4 | 0.5 天 |
| F5 | 0.5 天 |
| **总计** | **5 天** |

---

## 7. 执行顺序

1. F0：脚手架（添加依赖、创建包）
2. F1：EntityMeta + SortBy
3. F2：KtormBooleanTranslator + KtormOrderByTranslator
4. F3：UpdateAssignment + KtormUpdateTranslator
5. F4：RepositoryApi
6. F5：测试与文档