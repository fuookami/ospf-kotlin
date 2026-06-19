# ospf-kotlin 改进计划

## 强类型列绑定机制

### 目标

为 `PredicateSchema` 提供强类型列绑定机制，作为现有字符串 resolver 的**补充选项**，保持向后兼容。

### 背景

当前 ospf 的列映射基于字符串路径：
```kotlin
val resolver: (String) -> String? = { path ->
    when (path) {
        "id" -> "user_id"
        "status" -> "user_status"
        else -> null
    }
}
```

**现有方式的问题**（对某些项目而言）：
1. 字符串拼写错误编译不报错
2. 无法利用 IDE 的自动补全和重构功能
3. 需要手写字符串映射

**改进方向**：新增强类型列绑定能力（`ColumnBinder` + `withKtormTable` / `withMybatisMapping`），供需要类型安全的项目使用。现有字符串 resolver 继续保留，向后兼容。

### 设计原则

- ospf 提供**多种能力**，用户根据项目需求选择使用方式
- 字符串 resolver：简单灵活，适合快速开发
- 强类型列绑定：类型安全，适合大型项目
- 两种方式可以共存，互不冲突

### 架构设计

```
┌─────────────────────────────────────────────────────────┐
│           ospf-kotlin-framework (框架层)                  │
│           抽象接口，无后端依赖                              │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│    ospf-kotlin-framework-plugin-persistence-expression-ksp │
│    生成通用 schema 代码                                   │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│         ┌─────────────────┐  ┌─────────────────┐        │
│         │  Ktorm 插件      │  │  MyBatis 插件    │        │
│         │  列绑定实现       │  │  列绑定实现      │        │
│         └─────────────────┘  └─────────────────┘        │
└─────────────────────────────────────────────────────────┘
```

---

## Phase 1: ospf-kotlin-framework（框架层）

### 1.1 新增 ColumnBinder 接口

**文件**: `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression/ColumnBinder.kt`

```kotlin
package fuookami.ospf.kotlin.framework.persistence.expression

/**
 * 列绑定器接口
 * Column binder interface
 *
 * @param C 列类型 / Column type
 */
interface ColumnBinder<C> {
    /**
     * 解析属性路径为列
     * Resolve property path to column
     */
    fun resolve(path: String): C?
}

/**
 * 列绑定上下文
 * Column binding context
 */
class ColumnBindingContext<E, C>(
    private val schema: PredicateSchema<E>,
    private val binder: ColumnBinder<C>
) {
    /**
     * 解析属性路径
     * Resolve property path
     */
    fun resolveColumn(path: String): C? = binder.resolve(path)
}
```

### 1.2 扩展 PredicateSchema

**文件**: `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression/PredicateSchema.kt`

新增方法：

```kotlin
abstract class PredicateSchema<E> {
    // 现有方法保持不变
    protected fun <T> field(property: KProperty1<E, T>): PropertyPath<T>
    fun predicate(block: PredicateScope.() -> BooleanExpression): BooleanExpression
    
    // 新增：带列绑定的 predicate DSL
    fun <C> predicateWith(
        binder: ColumnBinder<C>,
        block: ColumnBindingContext<E, C>.(ColumnBindingContext<E, C>) -> BooleanExpression
    ): BooleanExpression {
        val context = ColumnBindingContext(this, binder)
        return block(context, context)
    }
}
```

### 1.3 扩展 PredicateEntity 注解

**文件**: `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression/PredicateAnnotations.kt`

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PredicateEntity(
    val schemaName: String = "",
    val generateResolver: Boolean = true,      // 是否生成字符串 resolver（向后兼容）
    val generateColumnMapping: Boolean = false  // 是否生成强类型列名映射（新增能力）
)
```

**说明**：
- `generateResolver`：保持现有行为，默认生成字符串 resolver
- `generateColumnMapping`：新增能力，默认不生成，需要的项目显式开启

---

## Phase 2: KSP Processor 修改

### 2.1 修改 PredicateSchemaRenderer

**文件**: `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-expression-ksp/src/main/fuookami/ospf/kotlin/framework/persistence/expression/ksp/PredicateSchemaRenderer.kt`

生成代码示例：

```kotlin
// KSP 生成（当 generateColumnMapping = true 时）
object Users : PredicateSchema<User>() {
    val id = field(User::id)
    val status = field(User::status)
    
    // 新增：列名映射（通用，不依赖后端）
    val columnMapping: Map<String, String> = mapOf(
        "id" to "user_id",
        "status" to "user_status"
    )
    
    // 新增：创建列绑定器的工厂方法
    fun <C> createBinder(resolver: (String) -> C?): ColumnBinder<C> {
        return object : ColumnBinder<C> {
            override fun resolve(path: String): C? {
                val backendName = columnMapping[path] ?: path
                return resolver(backendName)
            }
        }
    }
}
```

### 2.2 修改渲染逻辑

```kotlin
internal object PredicateSchemaRenderer {
    fun render(model: PredicateSchemaModel): String {
        return buildString {
            // ... 现有代码 ...
            
            // 生成 columnMapping（当 generateColumnMapping = true 时）
            if (model.generateColumnMapping) {
                appendLine()
                appendLine("    override val columnMapping: Map<String, String> = mapOf(")
                model.properties.forEachIndexed { index, property ->
                    val comma = if (index < model.properties.size - 1) "," else ""
                    appendLine("        \"${escape(property.propertyName)}\" to \"${escape(property.backendName)}\"$comma")
                }
                appendLine("    )")
                
                appendLine()
                appendLine("    fun <C> createBinder(resolver: (String) -> C?): ColumnBinder<C> {")
                appendLine("        return object : ColumnBinder<C> {")
                appendLine("            override fun resolve(path: String): C? {")
                appendLine("                val backendName = columnMapping[path] ?: path")
                appendLine("                return resolver(backendName)")
                appendLine("            }")
                appendLine("        }")
                appendLine("    }")
            }
        }
    }
}
```

---

## Phase 3: Ktorm 插件扩展

### 3.1 新增 KtormColumnBinder

**文件**: `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm/src/main/fuookami/ospf/kotlin/framework/persistence/expression/KtormColumnBinder.kt`

```kotlin
package fuookami.ospf.kotlin.framework.persistence.expression

import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table

/**
 * Ktorm 列绑定器
 * Ktorm column binder
 *
 * @param T Ktorm 表类型 / Ktorm table type
 * @property table Ktorm 表定义 / Ktorm table definition
 */
class KtormColumnBinder<T : Table<*>>(val table: T) : ColumnBinder<ColumnDeclaring<*>> {
    override fun resolve(path: String): ColumnDeclaring<*>? {
        return table.columns.find { it.name == path } as? ColumnDeclaring<*>
    }
}

/**
 * 使用 Ktorm 表的 predicate DSL 扩展
 * Predicate DSL extension with Ktorm table
 */
fun <E : Any, T : Table<*>> PredicateSchema<E>.withKtormTable(
    table: T,
    columnMapping: Map<String, String>,
    block: ColumnBindingContext<E, ColumnDeclaring<*>>.() -> BooleanExpression
): BooleanExpression {
    val binder = object : ColumnBinder<ColumnDeclaring<*>> {
        override fun resolve(path: String): ColumnDeclaring<*>? {
            val backendName = columnMapping[path] ?: path
            return (table.columns.find { it.name == backendName } as? ColumnDeclaring<*>)
        }
    }
    return predicateWith(binder, block)
}

/**
 * 使用 KSP 生成的 schema 的简化版
 * Simplified version using KSP generated schema
 */
fun <E : Any, T : Table<*>> PredicateSchema<E>.withKtormTable(
    table: T,
    block: ColumnBindingContext<E, ColumnDeclaring<*>>.() -> BooleanExpression
): BooleanExpression {
    // 假设 schema 有 columnMapping 属性（KSP 生成）
    val columnMapping = (this as? HasColumnMapping)?.columnMapping ?: emptyMap()
    return withKtormTable(table, columnMapping, block)
}

/**
 * 列映射接口（KSP 生成的 schema 实现此接口）
 * Column mapping interface (KSP generated schema implements this)
 */
interface HasColumnMapping {
    val columnMapping: Map<String, String>
}
```

### 3.2 修改 PredicateSchemaRenderer

让 KSP 生成的 schema 实现 `HasColumnMapping` 接口：

```kotlin
// KSP 生成
object Users : PredicateSchema<User>(), HasColumnMapping {
    // ...
    override val columnMapping: Map<String, String> = mapOf(...)
}
```

---

## Phase 4: MyBatis 插件扩展

### 4.1 新增 MybatisColumnBinder

**文件**: `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis/src/main/fuookami/ospf/kotlin/framework/persistence/expression/MybatisColumnBinder.kt`

```kotlin
package fuookami.ospf.kotlin.framework.persistence.expression

/**
 * MyBatis 列绑定器
 * MyBatis column binder
 *
 * @property columnMapping 列名映射 / Column name mapping
 */
class MybatisColumnBinder(
    private val columnMapping: Map<String, String>
) : ColumnBinder<String> {
    override fun resolve(path: String): String? {
        return columnMapping[path] ?: path.toSnakeCase()
    }
    
    companion object {
        /**
         * 驼峰转蛇形命名
         * Camel case to snake case
         */
        private fun String.toSnakeCase(): String {
            return replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
        }
    }
}

/**
 * 使用 MyBatis 映射的 predicate DSL 扩展
 * Predicate DSL extension with MyBatis mapping
 */
fun <E : Any> PredicateSchema<E>.withMybatisMapping(
    columnMapping: Map<String, String>,
    block: ColumnBindingContext<E, String>.(ColumnBindingContext<E, String>) -> BooleanExpression
): BooleanExpression {
    val binder = MybatisColumnBinder(columnMapping)
    return predicateWith(binder, block)
}

/**
 * 使用 KSP 生成的 schema 的简化版
 * Simplified version using KSP generated schema
 */
fun <E : Any> PredicateSchema<E>.withMybatisMapping(
    block: ColumnBindingContext<E, String>.(ColumnBindingContext<E, String>) -> BooleanExpression
): BooleanExpression {
    val columnMapping = (this as? HasColumnMapping)?.columnMapping ?: emptyMap()
    return withMybatisMapping(columnMapping, block)
}
```

---

## Phase 5: 验收标准

### 5.1 Phase 1 验收

- [ ] `ColumnBinder` 接口定义完成
- [ ] `ColumnBindingContext` 类定义完成
- [ ] `PredicateSchema.predicateWith` 方法实现
- [ ] `@PredicateEntity` 注解新增 `generateColumnMapping` 参数（保留 `generateResolver`）
- [ ] 编译通过

### 5.2 Phase 2 验收

- [ ] KSP 生成 `columnMapping` 属性
- [ ] KSP 生成 `createBinder` 工厂方法
- [ ] KSP 生成的 schema 实现 `HasColumnMapping` 接口
- [ ] 生成代码编译通过
- [ ] 单元测试通过

### 5.3 Phase 3 验收

- [ ] `KtormColumnBinder` 实现完成
- [ ] `withKtormTable` 扩展方法实现
- [ ] 集成测试通过（Ktorm 仓储）

### 5.4 Phase 4 验收

- [ ] `MybatisColumnBinder` 实现完成
- [ ] `withMybatisMapping` 扩展方法实现
- [ ] 集成测试通过（MyBatis 仓储）

---

## 使用示例

### 定义实体

```kotlin
@PredicateEntity(
    schemaName = "Users",
    generateColumnMapping = true
)
data class User(
    val id: Long,
    val status: String,
    val name: String
)
```

**注意**：PO 字段禁止使用 `@PredicateField` 注解，列名映射通过强类型绑定实现。

### KSP 生成

```kotlin
object Users : PredicateSchema<User>(), HasColumnMapping {
    val id = field(User::id)
    val status = field(User::status)
    val name = field(User::name)
    
    override val columnMapping: Map<String, String> = mapOf(
        "id" to "id",
        "status" to "status",
        "name" to "name"
    )
    
    fun <C> createBinder(resolver: (String) -> C?): ColumnBinder<C> {
        return object : ColumnBinder<C> {
            override fun resolve(path: String): C? {
                val backendName = columnMapping[path] ?: path
                return resolver(backendName)
            }
        }
    }
}
```

### Ktorm 使用

```kotlin
// Ktorm Table 定义
object UsersTable : Table<User>("users") {
    val userId = long("user_id").primaryKey().bind { it.id }
    val userStatus = varchar("user_status").bind { it.status }
    val name = varchar("name").bind { it.name }
}

// 仓储实现
class UserRepository(database: Database) : KtormRepository<User>(
    database, UsersTable, Users.createBinder { path ->
        UsersTable.columns.find { it.name == path } as? ColumnDeclaring<*>
    }
) {
    fun findActiveUsers(): List<User> {
        val where = Users.withKtormTable(UsersTable) {
            (status eq "active") and (name like "%test%")
        }
        return find(where)
    }
}
```

### MyBatis 使用

```kotlin
class UserMapper {
    fun findActiveUsers(): List<User> {
        val where = Users.withMybatisMapping {
            (status eq "active") and (name like "%test%")
        }
        // where 可以转换为 MyBatis 的 SQL 条件
        return selectList("selectUsers", where)
    }
}
```

---

## aps 项目使用规范

以下规范是 **aps 项目的选择**，不是 ospf 的限制。ospf 提供多种能力，aps 选择使用强类型列绑定方式。

### 背景

aps 项目是一个大型企业级应用，需要：
- 编译期类型检查，减少运行时错误
- IDE 支持（自动补全、重构安全）
- 明确的接口语义，便于团队协作

因此 aps 选择使用 ospf 提供的强类型列绑定能力。

### aps 规范 1：查询接口优先使用谓词，语义化接口按需定义

aps 项目中，查询接口（存在性、计数、单个、批量、分页）使用基于 `BooleanExpression` 的通用版本。仅当查询具有特殊业务语义或复杂逻辑时，才额外定义语义化查询接口。

```kotlin
// aps 正确：通用谓词接口
fun findOne(where: BooleanExpression? = null): User?
fun list(where: BooleanExpression? = null): List<User>

// aps 正确：特殊语义查询（需要关联查询或复杂逻辑）
fun findEffectiveBom(materialId: MaterialId): Bom?  // 跨表查询
fun findSubTreeById(nodeId: NodeId): List<ProduceNode>  // 递归查询

// aps 错误：仅为语法糖定义语义化接口
fun findById(id: UserId): User?  // 应使用 findOne(UserPredicates.id eq id)
```

### aps 规范 2：修改接口不提供谓词版本，必须定义语义化接口

aps 项目中，写操作（增删改）必须定义语义化接口，明确业务语义。

```kotlin
// aps 正确：语义化写接口
fun save(entity: User): User
fun activateUser(userId: UserId): User
fun deactivateUser(userId: UserId): User

// aps 错误：谓词化写接口
fun update(where: BooleanExpression, assignments: UpdateAssignments): Int
fun delete(where: BooleanExpression): Int
```

### aps 规范 3：PO 字段使用强类型列绑定

aps 项目选择使用 ospf 提供的强类型列绑定机制，而非字符串 resolver。

```kotlin
// aps 方式：开启强类型列映射
@PredicateEntity(schemaName = "Users", generateColumnMapping = true)
data class User(
    val id: Long,
    val status: String,
    val name: String
)

// aps 使用方式：通过强类型绑定
val where = Users.withKtormTable(UsersTable) {
    (status eq "active") and (name like "%test%")
}
```

**其他项目可以选择不同方式**：

```kotlin
// 其他项目：使用字符串 resolver（同样有效）
@PredicateEntity(schemaName = "Users", generateResolver = true)
data class User(
    @PredicateField("user_id")
    val id: Long,
    @PredicateField("user_status")
    val status: String
)

// 使用字符串 resolver
val resolver: (String) -> String? = { path ->
    when (path) {
        "id" -> "user_id"
        "status" -> "user_status"
        else -> null
    }
}
```

---

## 风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| 接口变更影响现有代码 | 保持向后兼容，旧 API 继续可用 |
| KSP 生成代码增加 | 通过 `generateColumnMapping` 参数控制是否生成 |
| 性能开销 | 列绑定在编译时解析，运行时无额外开销 |
| 学习曲线 | 提供文档和示例代码 |

---

## 实施顺序

1. **Phase 1**: ospf-kotlin-framework 框架层修改
2. **Phase 2**: KSP Processor 修改
3. **Phase 3**: Ktorm 插件扩展
4. **Phase 4**: MyBatis 插件扩展
5. **Phase 5**: 集成测试和文档

建议按 Phase 顺序实施，每个 Phase 完成后进行验证。
