# ospf-kotlin-framework-plugin-persistence-expression-ksp

:us: [English](README.md) | :cn: 简体中文

KSP 编译期处理器，根据 `@PredicateEntity` 注解的类生成类型化的 `PredicateSchema` 对象。

## 公开 API

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `PredicateSchemaProcessor` | class | KSP `SymbolProcessor`，扫描 `@PredicateEntity` 并生成 schema 源码 |
| `PredicateSchemaProcessorProvider` | class | KSP `SymbolProcessorProvider`，注册处理器 |
| `PredicateSchemaRenderer` | object | 内部渲染器，将 `PredicateSchemaModel` 转为 Kotlin 源码 |

## 生成产物

对于每个 `@PredicateEntity` 注解的类，处理器生成：

1. 一个 `PredicateSchema<EntityName>` 伴随对象，包含类型化的 `field(PropertyReference)` 条目。
2. 当 `generateResolver = true`（默认）时，生成可选的 `resolver: (String) -> String?` lambda，将属性路径映射到后端字段名（遵循 `@PredicateField(name = ...)` 指定）。

## 使用方式

```kotlin
@PredicateEntity
data class MaterialPO(
    val code: String,
    @PredicateField(name = "width_val") val widthValue: Double
)

// 生成结果（build/generated/ksp/.../MaterialPOSchema.kt）：
object MaterialPOSchema : PredicateSchema<MaterialPO>() {
    val code = field(MaterialPO::code)
    val widthValue = field(MaterialPO::widthValue)
    val resolver: (String) -> String? = { path ->
        when (path) {
            "code" -> "code"
            "widthValue" -> "width_val"
            else -> null
        }
    }
}
```

## Maven 配置

```xml
<plugin>
    <groupId>com.google.devtools.ksp</groupId>
    <artifactId>servlet-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>ksp</goal></goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.github.fuookami.ospf.kotlin.framework.plugin</groupId>
            <artifactId>ospf-kotlin-framework-plugin-persistence-expression-ksp</artifactId>
        </dependency>
    </dependencies>
</plugin>
```
