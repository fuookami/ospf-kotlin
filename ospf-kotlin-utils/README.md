# ospf-kotlin-utils

[中文](#中文) | [English](#english)

---

## 中文

ospf-kotlin-utils 是 OSPF Kotlin 项目的核心工具库，提供了通用工具函数和扩展。

### 模块结构

| 包名 | 说明 |
|------|------|
| `context` | 上下文变量管理，支持栈层级查找 |
| `error` | 错误码和错误处理 |
| `functional` | 函数式编程工具（Result 类型、提取器等） |
| `meta_programming` | 元编程工具（命名系统转换） |
| `parallel` | 并行处理扩展函数 |
| `serialization` | 序列化工具（JSON、CSV、日期时间） |

### 核心功能

#### 上下文变量 (`context`)

```kotlin
// 创建上下文变量
val myVar = ContextVar("default")

// 设置值并自动清理
myVar.set("value").use {
    println(myVar.get()) // "value"
}
// use 块结束后自动清理
```

#### 命名系统转换 (`meta_programming`)

```kotlin
val transfer = NameTransfer(NamingSystem.CamelCase, NamingSystem.SnakeCase)
val snakeCase = transfer("playStation") // "play_station"
```

#### 并行处理 (`parallel`)

```kotlin
// 并行映射
val results = listOf(1, 2, 3).mapParallelly { process(it) }

// 并行过滤
val filtered = listOf(1, 2, 3).filterParallelly { predicate(it) }
```

#### 序列化 (`serialization`)

```kotlin
// JSON 读写
val obj = readFromJson<MyClass>("path/to/file.json")
writeJsonToFile("path/to/file.json", obj)

// 使用命名策略
val policy = JsonNamingPolicy(NamingSystem.CamelCase, NamingSystem.SnakeCase)
val obj = readFromJson<MyClass>("path/to/file.json", policy)
```

### 测试

```bash
mvn -pl ospf-kotlin-utils test -DskipITs
```

---

## English

ospf-kotlin-utils is the core utility library for the OSPF Kotlin project, providing common utility functions and extensions.

### Module Structure

| Package | Description |
|---------|-------------|
| `context` | Context variable management with stack-level lookup |
| `error` | Error codes and error handling |
| `functional` | Functional programming tools (Result types, extractors, etc.) |
| `meta_programming` | Metaprogramming tools (naming system conversion) |
| `parallel` | Parallel processing extension functions |
| `serialization` | Serialization tools (JSON, CSV, date/time) |

### Core Features

#### Context Variables (`context`)

```kotlin
// Create context variable
val myVar = ContextVar("default")

// Set value with auto-cleanup
myVar.set("value").use {
    println(myVar.get()) // "value"
}
// Auto-cleanup after use block ends
```

#### Naming System Conversion (`meta_programming`)

```kotlin
val transfer = NameTransfer(NamingSystem.CamelCase, NamingSystem.SnakeCase)
val snakeCase = transfer("playStation") // "play_station"
```

#### Parallel Processing (`parallel`)

```kotlin
// Parallel map
val results = listOf(1, 2, 3).mapParallelly { process(it) }

// Parallel filter
val filtered = listOf(1, 2, 3).filterParallelly { predicate(it) }
```

#### Serialization (`serialization`)

```kotlin
// JSON read/write
val obj = readFromJson<MyClass>("path/to/file.json")
writeJsonToFile("path/to/file.json", obj)

// With naming policy
val policy = JsonNamingPolicy(NamingSystem.CamelCase, NamingSystem.SnakeCase)
val obj = readFromJson<MyClass>("path/to/file.json", policy)
```

### Testing

```bash
mvn -pl ospf-kotlin-utils test -DskipITs
```