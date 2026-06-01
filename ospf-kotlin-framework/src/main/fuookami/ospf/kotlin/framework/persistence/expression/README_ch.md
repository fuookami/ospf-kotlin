# ospf-kotlin-framework/persistence/expression / 表达式仓储 DSL

:us: [English](README.md) | :cn: 简体中文

本包是 framework 层的表达式仓储 DSL 与仓储模型，不是完整 SQL AST，也不是某个数据库后端的查询构造器。

它的主要职责是：

- 用 `BooleanExpression` 表达 repository 的 `where` 谓词。
- 用 `SortBy` 表达排序。
- 用 `UpdateAssignments` 表达 update 的 set 列表。
- 用 resolver 把领域属性路径映射到具体后端字段或列。
- 为 Ktorm、MyBatis-Plus、MongoDB 等插件提供统一的输入模型。

聚合、`group by`、`join`、projection/select 字段、事务、批量操作、乐观锁、upsert、客户端过滤执行等能力不属于本包的表达式 DSL；它们应由更上层 repository API 或具体后端插件提供。

## 模块关系

| 模块 | 职责 |
|------|------|
| `ospf-kotlin-math` | 表达式 AST 与通用符号 DSL，例如 `BooleanExpression`、`ScalarExpression`、`PropertyPath`、比较、算术、`abs` |
| `ospf-kotlin-framework` | 仓储表达式模型、字符串/空值函数 DSL、KProperty 入口、注解 API |
| `ospf-kotlin-framework-plugin-persistence-expression-ksp` | 可选 KSP processor，按实体生成 schema block |
| persistence 后端插件 | 把 AST 翻译到 Ktorm、MyBatis-Plus、MongoDB 等后端 |

后端插件位于：

- `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm`
- `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis`
- `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb`

## 推荐写法

### KSP 生成 schema block

业务侧推荐使用 KSP 生成 schema block，获得接近 Ktorm 的强类型字段体验：

```kotlin
import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity
import fuookami.ospf.kotlin.framework.persistence.expression.PredicateField
import fuookami.ospf.kotlin.math.symbol.expression.dsl.and
import fuookami.ospf.kotlin.math.symbol.expression.dsl.gt
import fuookami.ospf.kotlin.math.symbol.expression.dsl.predicate

@PredicateEntity(schemaName = "Users")
data class User(
    @PredicateField("user_id")
    val id: Long,

    @PredicateField("user_status")
    val status: String,

    val age: Int,
    val deletedAt: String?
)

val where = Users.predicate {
    (status eq "active") and (age gt 18)
}

val columnName = Users.resolver("status") // "user_status"
```

KSP 会生成类似代码：

```kotlin
object Users : PredicateSchema<User>() {
    val id = field(User::id)
    val status = field(User::status)
    val age = field(User::age)
    val deletedAt = field(User::deletedAt)

    val resolver: (String) -> String? = { path ->
        when (path) {
            "id" -> "user_id"
            "status" -> "user_status"
            "age" -> "age"
            "deletedAt" -> "deletedAt"
            else -> null
        }
    }
}
```

`@PredicateField` 只生成 resolver 映射，不改变 AST path。`field(User::status)` 仍然生成 `PropertyPath.parse("status")`，后端 translator 再通过 resolver 把 `"status"` 映射到真实列名或字段名。

KSP processor 当前只支持顶层、非泛型实体。`schemaName` 必须是普通 Kotlin 标识符；启用 resolver 生成时，实体属性不能命名为 `resolver`。如果属性名是 Kotlin 关键字，生成代码会用反引号引用，例如 ``val `class` = field(User::`class`)``。

### 无代码生成的强类型入口

不启用 KSP 时，可以直接使用 `KProperty1`：

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.dsl.and
import fuookami.ospf.kotlin.math.symbol.expression.dsl.gt
import fuookami.ospf.kotlin.math.symbol.expression.dsl.prop
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments

val where = (prop(User::status) eq "active") and (prop(User::age) gt 18)
val sortBy = SortBy.desc(User::age)
val assignments = UpdateAssignments.set(User::status, "inactive")
```

`prop(User::status)` 与 schema 中的 `field(User::status)` 一样，都会保留 Kotlin 属性名作为 AST path。

### 字符串 path 入口

字符串 path 仍是公共 fallback API，适合动态字段、配置化查询或跨语言场景：

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.ScalarExpressionFactory
import fuookami.ospf.kotlin.math.symbol.expression.dsl.and
import fuookami.ospf.kotlin.math.symbol.expression.dsl.gt
import fuookami.ospf.kotlin.math.symbol.expression.dsl.path
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments

val where = (path("status") eq "active") and (path("age") gt 18)
val sortBy = SortBy.desc("createdAt")
val assignments = UpdateAssignments
    .set("status", "inactive")
    .thenSetNull("deletedAt")
```

字符串 path 不做编译期字段检查。需要强类型约束时优先使用 KSP schema 或 `prop(...)`。

## 谓词能力

### 比较与逻辑组合

比较、逻辑组合和基础谓词来自 `ospf-kotlin-math`：

```kotlin
val active = prop(User::status) eq "active"
val adult = prop(User::age) ge 18
val notDeleted = prop(User::deletedAt).isNull()

val where = active and adult and notDeleted
```

常用能力包括：

- `eq` / `ne`
- `lt` / `le` / `gt` / `ge`
- `isNull()` / `isNotNull()`
- `inList(...)`
- `like...`
- `and` / `or` / `not`

### 列列比较与标量表达式

当后端插件支持时，可以比较列与列，也可以比较基础算术表达式：

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.ScalarExpressionFactory

val validRange = prop(Event::startAt) lt prop(Event::endAt)
val amount = ScalarExpressionFactory.multiply(
    path("price").asScalar<Int>(),
    path("quantity").asScalar<Int>()
)
val amountCheck = amount gt 100
```

如果某个后端不能下推表达式，会按 `UnsupportedPredicatePolicy` 处理，不会静默变成无条件查询。

### 标量函数

函数表达式使用 `ospf-kotlin-math` 的 `ScalarFunction` AST 节点。函数名是受控符号，不是原始 SQL 字符串。

`abs` 保留在 math DSL，因为它本身也是数学函数：

```kotlin
val balanceCheck = abs(path("balance")) gt 10
```

面向仓储的字符串/空值函数由当前 framework 包提供，需要显式引入：

```kotlin
import fuookami.ospf.kotlin.framework.persistence.expression.lower
import fuookami.ospf.kotlin.framework.persistence.expression.coalesce

val normalizedName = lower(path("name")) eq "alice"
val displayName = coalesce(path("nickname"), path("name")) ne null
```

当前标准函数包括：

- `abs`
- `lower`
- `upper`
- `trim`
- `length`
- `coalesce`

Ktorm、MyBatis-Plus 与 MongoDB translator 只下推已登记的标准函数。未知函数走 `UnsupportedPredicatePolicy`，不会被拼成不受控 SQL 或 Mongo 表达式。

## 排序与更新

`SortBy` 支持多字段排序和可选空值顺序：

```kotlin
val sortBy = SortBy
    .desc(User::createdAt)
    .thenAsc(User::id)
```

`UpdateAssignments` 表达 update set 列表：

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.ScalarExpressionFactory

val assignments = UpdateAssignments
    .set(User::status, "inactive")
    .thenSetNull(User::deletedAt)
    .thenSetExpr(
        "score",
        ScalarExpressionFactory.add(
            path("score").asScalar<Int>(),
            ScalarExpressionFactory.constant(1)
        )
    )
```

`setExpr` / `thenSetExpr` 是否可下推由具体后端 translator 决定。

## Repository API

统一仓储接口定义在 `ExpressionRepository`：

```kotlin
val users = repository.find(where, sortBy = sortBy, limit = 10, offset = 0)
val total = repository.count(where)
val exists = repository.exists(where)
val updated = repository.update(where, assignments)
val deleted = repository.delete(where)
```

默认策略是 `UnsupportedPredicatePolicy.AlwaysFalse`：无法下推的谓词返回空结果或恒假条件，不会退化为未过滤查询。需要更严格行为时，可以在 repository 构造时选择 `FailFast`。`ClientFilter` 是保留策略；在客户端过滤真正实现前会明确失败。

## Resolver

resolver 是当前字段映射机制。它接收 AST path，返回后端字段或列：

```kotlin
val mybatisResolver: MybatisColumnNameResolver = { path ->
    when (path) {
        "id" -> "user_id"
        "status" -> "user_status"
        else -> path.substringAfterLast(".")
    }
}

val mongoResolver: MongoFieldNameResolver = { path ->
    when (path) {
        "deletedAt" -> "deleted_at"
        else -> path
    }
}
```

KSP 生成的 `Users.resolver` 可以直接传给支持字符串字段名的后端；Ktorm 这类需要列对象的后端通常仍需要把 path 映射到表列对象。

## KSP 引入方式

### Gradle

```kotlin
plugins {
    kotlin("jvm") version "<kotlin-version>"
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    implementation("io.github.fuookami.ospf.kotlin:ospf-kotlin-starter:<version>")
    ksp("io.github.fuookami.ospf.kotlin.framework.plugin:ospf-kotlin-framework-plugin-persistence-expression-ksp:<version>")
}
```

### Maven

Maven 的 `kapt` 只适用于 Java annotation processor；当前模块提供的是 KSP processor，不能通过 `kotlin-maven-plugin` 的 `kapt` goal 注册。使用 Maven 时需要选择第三方 KSP plugin，并在 plugin 的 `<dependencies>` 中显式注册 processor。

一个可用方案是 `me.kpavlov.ksp.maven:ksp-maven-plugin`：

```xml
<properties>
    <ospf-kotlin.version>1.1.0</ospf-kotlin.version>
    <ksp-maven-plugin.version>0.4.2</ksp-maven-plugin.version>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>me.kpavlov.ksp.maven</groupId>
            <artifactId>ksp-maven-plugin</artifactId>
            <version>${ksp-maven-plugin.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>process</goal>
                    </goals>
                </execution>
            </executions>
            <dependencies>
                <dependency>
                    <groupId>io.github.fuookami.ospf.kotlin.framework.plugin</groupId>
                    <artifactId>ospf-kotlin-framework-plugin-persistence-expression-ksp</artifactId>
                    <version>${ospf-kotlin.version}</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

processor 要作为 KSP Maven plugin 的 dependency 引入，不要只写在项目普通 `<dependencies>` 中。项目普通依赖仍按业务需要引入 framework 或 starter：

```xml
<dependencies>
    <dependency>
        <groupId>io.github.fuookami.ospf.kotlin</groupId>
        <artifactId>ospf-kotlin-starter</artifactId>
        <version>${ospf-kotlin.version}</version>
    </dependency>
</dependencies>
```

示意配置：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.fuookami.ospf.kotlin</groupId>
            <artifactId>ospf-kotlin-starter</artifactId>
            <version>${ospf-kotlin.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

processor 版本建议显式写 `${ospf-kotlin.version}`，避免 Maven plugin 无法从普通依赖中继承版本。

## 相关链接

- [math/symbol/expression](../../../../../../../../../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/expression/README_ch.md) - 核心表达式 AST
