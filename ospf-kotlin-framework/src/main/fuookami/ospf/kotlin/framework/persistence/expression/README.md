# ospf-kotlin-framework/persistence/expression / Expression Repository DSL

:us: English | :cn: [简体中文](README_ch.md)

This package contains the framework-level expression repository DSL and repository models. It is not a complete SQL AST and it is not a database-specific query builder.

Its main responsibilities are:

- Represent repository `where` predicates with `BooleanExpression`.
- Represent sorting with `SortBy`.
- Represent update set lists with `UpdateAssignments`.
- Map domain property paths to backend fields or columns through resolver functions.
- Provide a shared input model for Ktorm, MyBatis-Plus, MongoDB, and other persistence plugins.

Aggregation, `group by`, `join`, projection/select fields, transactions, batch operations, optimistic locking, upsert, and client-side filtering execution are outside this expression DSL. Those capabilities should be provided by higher-level repository APIs or concrete backend plugins.

## Module Boundaries

| Module | Responsibility |
|--------|----------------|
| `ospf-kotlin-math` | Expression AST and general symbolic DSL, such as `BooleanExpression`, `ScalarExpression`, `PropertyPath`, comparisons, arithmetic, and `abs` |
| `ospf-kotlin-framework` | Repository expression models, string/null function DSL, KProperty entry points, and annotation APIs |
| `ospf-kotlin-framework-plugin-persistence-expression-ksp` | Optional KSP processor that generates schema blocks from entities |
| persistence backend plugins | Translate the AST to Ktorm, MyBatis-Plus, MongoDB, or another backend |

Backend plugins live in:

- `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm`
- `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis`
- `ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb`

## Recommended Usage

### KSP-Generated Schema Blocks

For application code, the recommended shape is a generated schema block. It gives a type-safe field experience close to Ktorm:

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

KSP generates code similar to:

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

`@PredicateField` only generates resolver mappings. It does not change the AST path. `field(User::status)` still produces `PropertyPath.parse("status")`; backend translators use the resolver later to map `"status"` to a real column or field name.

The KSP processor currently supports top-level, non-generic entities only. `schemaName` must be a regular Kotlin identifier. If resolver generation is enabled, entity properties cannot be named `resolver`. Kotlin keyword property names are emitted with backticks, for example ``val `class` = field(User::`class`)``.

### Type-Safe Entry Without Code Generation

When KSP is not enabled, use `KProperty1` directly:

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

`prop(User::status)` and schema `field(User::status)` both preserve the Kotlin property name as the AST path.

### String Path Entry

String paths remain the public fallback API. They are useful for dynamic fields, configuration-driven queries, and cross-language scenarios:

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

String paths are not checked at compile time. Prefer KSP schemas or `prop(...)` when type safety matters.

## Predicate Capabilities

### Comparisons And Logic

Comparisons, logical composition, and basic predicates come from `ospf-kotlin-math`:

```kotlin
val active = prop(User::status) eq "active"
val adult = prop(User::age) ge 18
val notDeleted = prop(User::deletedAt).isNull()

val where = active and adult and notDeleted
```

Common capabilities include:

- `eq` / `ne`
- `lt` / `le` / `gt` / `ge`
- `isNull()` / `isNotNull()`
- `inList(...)`
- `like...`
- `and` / `or` / `not`

### Column-Column Comparisons And Scalar Expressions

When a backend plugin supports them, predicates can compare columns to columns and compare basic arithmetic expressions:

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.ScalarExpressionFactory

val validRange = prop(Event::startAt) lt prop(Event::endAt)
val amount = ScalarExpressionFactory.multiply(
    path("price").asScalar<Int>(),
    path("quantity").asScalar<Int>()
)
val amountCheck = amount gt 100
```

If a backend cannot push an expression down, it follows `UnsupportedPredicatePolicy` instead of silently becoming an unfiltered query.

### Scalar Functions

Function expressions use the `ScalarFunction` AST node from `ospf-kotlin-math`. Function names are controlled symbols, not raw SQL strings.

`abs` stays in the math DSL because it is also a mathematical function:

```kotlin
val balanceCheck = abs(path("balance")) gt 10
```

Repository-oriented string/null functions are exposed from this framework package and must be imported explicitly:

```kotlin
import fuookami.ospf.kotlin.framework.persistence.expression.lower
import fuookami.ospf.kotlin.framework.persistence.expression.coalesce

val normalizedName = lower(path("name")) eq "alice"
val displayName = coalesce(path("nickname"), path("name")) ne null
```

Current standard functions are:

- `abs`
- `lower`
- `upper`
- `trim`
- `length`
- `coalesce`

Ktorm, MyBatis-Plus, and MongoDB translators only push down registered standard functions. Unknown functions follow `UnsupportedPredicatePolicy` and are never emitted as uncontrolled SQL or Mongo expressions.

## Sorting And Updates

`SortBy` supports multi-field sorting and optional nulls order:

```kotlin
val sortBy = SortBy
    .desc(User::createdAt)
    .thenAsc(User::id)
```

`UpdateAssignments` represents update set lists:

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

Whether `setExpr` / `thenSetExpr` can be pushed down is decided by the concrete backend translator.

## Repository API

The shared repository contract is `ExpressionRepository`:

```kotlin
val users = repository.find(where, sortBy = sortBy, limit = 10, offset = 0)
val total = repository.count(where)
val exists = repository.exists(where)
val updated = repository.update(where, assignments)
val deleted = repository.delete(where)
```

The default policy is `UnsupportedPredicatePolicy.AlwaysFalse`: predicates that cannot be pushed down return empty results or false conditions instead of degrading to unfiltered queries. Select `FailFast` in repository constructors when explicit failures are preferred. `ClientFilter` is reserved and fails explicitly until client-side filtering is implemented.

## Resolver

A resolver is the current field mapping mechanism. It receives an AST path and returns a backend field or column:

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

The generated `Users.resolver` can be passed directly to backends that accept string field names. Backends such as Ktorm, which need column objects, typically still map paths to table column objects.

## KSP Setup

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

Maven `kapt` only applies to Java annotation processors. This module provides a KSP processor, so it cannot be registered through the `kapt` goal of `kotlin-maven-plugin`. Maven users need a third-party KSP plugin and must register the processor explicitly in the plugin `<dependencies>`.

One available option is `me.kpavlov.ksp.maven:ksp-maven-plugin`:

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

The processor must be added as a KSP Maven plugin dependency, not only as a regular project dependency. Regular project dependencies still bring in framework or starter APIs as needed:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.fuookami.ospf.kotlin</groupId>
        <artifactId>ospf-kotlin-starter</artifactId>
        <version>${ospf-kotlin.version}</version>
    </dependency>
</dependencies>
```

Example dependency management:

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

The processor version should still be declared explicitly as `${ospf-kotlin.version}` so the Maven plugin does not need to infer it from regular dependencies.

## Related

- [math/symbol/expression](../../../../../../../../../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/expression/README.md) - Core expression AST
