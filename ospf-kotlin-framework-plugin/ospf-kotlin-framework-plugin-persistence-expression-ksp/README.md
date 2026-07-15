# ospf-kotlin-framework-plugin-persistence-expression-ksp

:us: English | :cn: [简体中文](README_ch.md)

KSP compile-time processor that generates typed `PredicateSchema` objects from `@PredicateEntity`-annotated classes.

## Public API

| Symbol | Kind | Description |
| --- | --- | --- |
| `PredicateSchemaProcessor` | class | KSP `SymbolProcessor` that scans `@PredicateEntity` and emits schema source |
| `PredicateSchemaProcessorProvider` | class | KSP `SymbolProcessorProvider` that registers the processor |
| `PredicateSchemaRenderer` | object | Internal renderer that converts a `PredicateSchemaModel` to Kotlin source |

## Generated Artifacts

For each `@PredicateEntity`-annotated class, the processor generates:

1. A `PredicateSchema<EntityName>` companion object with typed `field(PropertyReference)` entries.
2. An optional `resolver: (String) -> String?` lambda when `generateResolver = true` (default), mapping property paths to backend field names (respecting `@PredicateField(name = ...)`).

## Usage

```kotlin
@PredicateEntity
data class MaterialPO(
    val code: String,
    @PredicateField(name = "width_val") val widthValue: Double
)

// Generated (build/generated/ksp/.../MaterialPOSchema.kt):
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

## Maven Configuration

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
