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

// 指定并发上限
val results = largeList.mapParallelly(concurrentAmount = 4uL) { process(it) }
```

**并发控制策略**：
- 使用 `concurrentAmount` 参数控制最大并发数
- 默认值根据集合大小和可用处理器自动计算
- Worker Pool 方案确保协程数量与 `concurrentAmount` 绑定
- 大集合场景下避免协程爆发问题

**支持并发控制的 API**：
| API | 说明 |
|-----|------|
| `mapParallelly` | 并行映射 |
| `filterParallelly` | 并行过滤 |
| `allParallelly` | 并行判断全部满足 |
| `anyParallelly` | 并行判断任一满足 |
| `countParallelly` | 并行计数 |
| `findParallelly` | 并行查找 |
| `associateParallelly` | 并行关联 |
| `flatMapParallelly` | 并行展平映射 |
| `maxByParallelly` | 并行最大值 |
| `minByParallelly` | 并行最小值 |

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

// Specify concurrency limit
val results = largeList.mapParallelly(concurrentAmount = 4uL) { process(it) }
```

**Concurrency Control Strategy**:
- Use `concurrentAmount` parameter to control maximum concurrency
- Default value calculated from collection size and available processors
- Worker Pool approach ensures coroutine count is bound to `concurrentAmount`
- Avoids coroutine explosion in large collection scenarios

**APIs with Concurrency Control**:
| API | Description |
|-----|-------------|
| `mapParallelly` | Parallel map |
| `filterParallelly` | Parallel filter |
| `allParallelly` | Parallel all check |
| `anyParallelly` | Parallel any check |
| `countParallelly` | Parallel count |
| `findParallelly` | Parallel find |
| `associateParallelly` | Parallel associate |
| `flatMapParallelly` | Parallel flatMap |
| `maxByParallelly` | Parallel max |
| `minByParallelly` | Parallel min |

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