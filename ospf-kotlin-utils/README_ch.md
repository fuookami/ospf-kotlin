# ospf-kotlin-utils

:us: [English](README.md) | :cn: 简体中文

## 介绍

ospf-kotlin-utils 是 OSPF Kotlin 项目的核心工具库，提供了通用工具函数和扩展。

### 模块结构

| 包名 | 说明 |
|------|------|
| `config` | 版本信息管理 |
| `context` | 上下文变量管理，支持栈层级查找 |
| `concept` | 概念接口（Move、Swap、Clone、Indexed） |
| `error` | 错误码和错误处理 |
| `functional` | 函数式编程工具（Result、Either、Eq、Ord 等）— [查看子包 README](src/main/fuookami/ospf/kotlin/utils/functional/README_ch.md) |
| `meta_programming` | 元编程工具（命名系统转换、懒委托） |
| `parallel` | 并行处理扩展函数 |
| `serialization` | 序列化工具（JSON、CSV、日期时间） |

### 根目录工具

#### 时间扩展 (`Time.kt`)

```kotlin
// Instant 扩展
val next = instant.nextDay()
val prev = instant.lastDay()
val truncated = instant.truncatedTo(DurationUnit.HOURS)

// Duration 扩展
val sum = durations.sum()
val (min, max) = durations.minMax()

// LocalDate/LocalDateTime 扩展
val tomorrow = LocalDate(2024, 1, 1).nextDay()
```

#### UUIDv7 生成器 (`UUIDv7.kt`)

```kotlin
// 生成时间排序的 UUID
val uuid = UUIDv7.generate()

// 线程安全版本
val uuid = UUIDv7.generateSync()
```

#### 系统工具 (`System.kt`)

```kotlin
// 检查内存使用是否超过阈值（默认 80%）
if (memoryUseOver(0.8)) {
    // 执行内存清理或拒绝新任务
}
```

#### 本地库加载 (`Library.kt`)

```kotlin
// 从 JAR 中加载本地库
Library.loadInJar("native/lib.dll", "/tmp/lib.dll")
```

### 概念接口 (`concept`)

提供语义化的概念接口，用于定义类型行为：

```kotlin
// Movable - 移动语义（资源转移）
interface Movable<Self> {
    fun move(): Self
}

// Swappable - 交换语义
interface Swappable<Self> {
    infix fun swap(rhs: Self)
}

// Copyable - 复制语义
interface Copyable<Self> : Movable<Self> {
    fun copy(): Self
}

// Indexed - 索引语义
interface Indexed {
    val index: Int
    val uindex: ULong
}
```

#### 索引类型

```kotlin
// 手动索引 - 需要显式调用 setIndexed()
class MyManualIndexed : ManualIndexed() {
    fun initialize() { setIndexed() }
}

// 自动索引 - 创建时自动分配
class MyAutoIndexed : AutoIndexed(MyAutoIndexed::class)
```

### 错误处理 (`error`)

```kotlin
// 定义错误
val error = Err(ErrorCode.FileNotFound, "文件不存在")

// 带附加值的错误
val exError = ExErr(ErrorCode.DataNotFound, "数据未找到", additionalData)

// 错误码枚举
enum class ErrorCode {
    None, AuthenticationError,
    FileNotFound, DataNotFound,
    SerializationFailed, DeserializationFailed,
    ApplicationFailed, ApplicationError,
    // ... 更多错误码
}
```

### 函数式编程 (`functional`)

#### Result 类型

```kotlin
// Ok - 成功结果
val ok: Result<String, Error> = Ok("success")

// Failed - 失败结果
val failed: Result<String, Error> = Failed(ErrorCode.FileNotFound, "文件不存在")

// Fatal - 多错误结果
val fatal: Result<String, Error> = Fatal(errors)

// Warn - 警告结果（仅 ExResult）
val warn: ExResult<String, Error> = Warn("value", ErrorCode.Other, "警告信息")

// 链式处理
result
    .ifOk { println("成功: ${it.value}") }
    .ifFailed { println("失败: ${it.message}") }
    .ifFatal { it.forEach { error -> println(error) } }

// 执行多个操作，遇到错误即停止
run(
    { step1() },
    { step2() },
    { step3() }
)
```

#### Either 类型

```kotlin
// 二选一类型
val left: Either<Int, String> = Either.Left(42)
val right: Either<Int, String> = Either.Right("hello")

// 条件处理
val result = either
    .ifLeft { "Left: $it" }
    .ifRight { "Right: $it" }
    .invoke()
```

#### 相等性与排序 (`Eq` / `Ord`)

```kotlin
// 自定义相等性
class MyClass : Eq<MyClass> {
    override infix fun eq(rhs: MyClass): Boolean = this.id == rhs.id
}

// 自定义排序
class MyClass : Ord<MyClass> {
    override infix fun ord(rhs: MyClass): Order =
        orderBetween(this.priority, rhs.priority)
}

// 使用比较
if (a ls b) { /* a < b */ }  // less than
if (a leq b) { /* a <= b */ } // less or equal
if (a gr b) { /* a > b */ }  // greater than
if (a geq b) { /* a >= b */ } // greater or equal
```

#### Predicate 组合

```kotlin
// predicate 组合
val combined = predicate1 and predicate2
val either = predicate1 or predicate2
val negated = !predicate

// 三路比较器
val threeWay: ThreeWayComparator<T> = { lhs, rhs -> orderOf(compare(lhs, rhs)) }
```

#### 集合扩展

```kotlin
// 多维列表
typealias List2<T> = List<List<T>>
typealias List3<T> = List<List<List<T>>>

// 多维 Map
typealias MultiMap2<K1, K2, V> = Map<K1, Map<K2, V>>

// 洗牌（Fisher-Yates 算法）
val shuffled = list.shuffle()

// MinMax 同时获取
val (min, max) = list.minMax()
val (minBy, maxBy) = list.minMaxBy { it.value }
```

### 上下文变量 (`context`)

```kotlin
// 创建上下文变量
val myVar = ContextVar("default")

// 设置值并自动清理
myVar.set("value").use {
    println(myVar.get()) // "value"
}
// use 块结束后自动清理
```

### 元编程工具 (`meta_programming`)

#### 命名系统转换

```kotlin
val transfer = NameTransfer(NamingSystem.CamelCase, NamingSystem.SnakeCase)
val snakeCase = transfer("playStation") // "play_station"
```

#### 懒委托

```kotlin
// 范围型懒委托
val lazyValue by lazyDelegate { computeValue() }

// 自引用懒委托
val selfRef by selfLazyDelegate { this.computeValue() }

// 挂起懒加载
val suspendValue by suspendLazy { computeAsync() }
```

### 并行处理 (`parallel`)

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
| `foldParallelly` | 并行折叠 |
| `minMaxByParallelly` | 并行同时获取最小最大值 |

### 序列化 (`serialization`)

#### JSON 读写

```kotlin
// JSON 读写
val obj = readFromJson<MyClass>("path/to/file.json")
writeJsonToFile("path/to/file.json", obj)

// 使用命名策略
val policy = JsonNamingPolicy(NamingSystem.CamelCase, NamingSystem.SnakeCase)
val obj = readFromJson<MyClass>("path/to/file.json", policy)
```

#### CSV 读写

```kotlin
// CSV 读取
val data = readFromCSV("path/to/file.csv")

// CSV 写入
writeCSVToFile("path/to/file.csv", data, headers = listOf("col1", "col2"))
```

#### Duration 序列化器

```kotlin
// 离散 Duration 序列化
@Serializable
data class Config(
    @Serializable(DiscreteDurationSerializer::class)
    val timeout: Duration
)

// 连续 Duration 序列化
@Serializable
data class Timing(
    @Serializable(ContinuousDurationSerializer::class)
    val duration: Duration
)
```

### 测试

```bash
mvn -pl ospf-kotlin-utils test -DskipITs
```