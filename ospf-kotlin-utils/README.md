# ospf-kotlin-utils

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

ospf-kotlin-utils is the core utility library for the OSPF Kotlin project, providing common utility functions and extensions.

### Module Structure

| Package | Description |
|---------|-------------|
| `config` | Version information management |
| `context` | Context variable management with stack-level lookup |
| `concept` | Concept interfaces (Move, Swap, Clone, Indexed) |
| `error` | Error codes and error handling |
| `functional` | Functional programming tools (Result, Either, Eq, Ord, etc.) — [see sub-package README](src/main/fuookami/ospf/kotlin/utils/functional/README.md) |
| `meta_programming` | Metaprogramming tools (naming system conversion, lazy delegate) |
| `parallel` | Parallel processing extension functions |
| `serialization` | Serialization tools (JSON, CSV, date/time) |

### Root Utilities

#### Time Extensions (`Time.kt`)

```kotlin
// Instant extensions
val next = instant.nextDay()
val prev = instant.lastDay()
val truncated = instant.truncatedTo(DurationUnit.HOURS)

// Duration extensions
val sum = durations.sum()
val (min, max) = durations.minMax()

// LocalDate/LocalDateTime extensions
val tomorrow = LocalDate(2024, 1, 1).nextDay()
```

#### UUIDv7 Generator (`UUIDv7.kt`)

```kotlin
// Generate time-sorted UUID
val uuid = UUIDv7.generate()

// Thread-safe version
val uuid = UUIDv7.generateSync()
```

#### System Utilities (`System.kt`)

```kotlin
// Check if memory usage exceeds threshold (default 80%)
if (memoryUseOver(0.8)) {
    // Perform memory cleanup or reject new tasks
}
```

#### Native Library Loading (`Library.kt`)

```kotlin
// Load native library from JAR
Library.loadInJar("native/lib.dll", "/tmp/lib.dll")
```

### Concept Interfaces (`concept`)

Semantic concept interfaces for defining type behaviors:

```kotlin
// Movable - Move semantics (resource transfer)
interface Movable<Self> {
    fun move(): Self
}

// Swappable - Swap semantics
interface Swappable<Self> {
    infix fun swap(rhs: Self)
}

// Copyable - Copy semantics
interface Copyable<Self> : Movable<Self> {
    fun copy(): Self
}

// Indexed - Index semantics
interface Indexed {
    val index: Int
    val uindex: ULong
}
```

#### Index Types

```kotlin
// Manual index - requires explicit setIndexed() call
class MyManualIndexed : ManualIndexed() {
    fun initialize() { setIndexed() }
}

// Auto index - automatically assigned on creation
class MyAutoIndexed : AutoIndexed(MyAutoIndexed::class)
```

### Error Handling (`error`)

```kotlin
// Define error
val error = Err(ErrorCode.FileNotFound, "File not found")

// Error with additional value
val exError = ExErr(ErrorCode.DataNotFound, "Data not found", additionalData)

// Error code enumeration
enum class ErrorCode {
    None, AuthenticationError,
    FileNotFound, DataNotFound,
    SerializationFailed, DeserializationFailed,
    ApplicationFailed, ApplicationError,
    // ... more error codes
}
```

### Functional Programming (`functional`)

#### Result Type

```kotlin
// Ok - success result
val ok: Result<String, Error> = Ok("success")

// Failed - failure result
val failed: Result<String, Error> = Failed(ErrorCode.FileNotFound, "File not found")

// Fatal - multiple error result
val fatal: Result<String, Error> = Fatal(errors)

// Warn - warning result (ExResult only)
val warn: ExResult<String, Error> = Warn("value", ErrorCode.Other, "Warning message")

// Chain processing
result
    .ifOk { println("Success: ${it.value}") }
    .ifFailed { println("Failed: ${it.message}") }
    .ifFatal { it.forEach { error -> println(error) } }

// Run multiple operations, stop on first error
run(
    { step1() },
    { step2() },
    { step3() }
)
```

#### Either Type

```kotlin
// Either type
val left: Either<Int, String> = Either.Left(42)
val right: Either<Int, String> = Either.Right("hello")

// Conditional handling
val result = either
    .ifLeft { "Left: $it" }
    .ifRight { "Right: $it" }
    .invoke()
```

#### Equality and Ordering (`Eq` / `Ord`)

```kotlin
// Custom equality
class MyClass : Eq<MyClass> {
    override infix fun eq(rhs: MyClass): Boolean = this.id == rhs.id
}

// Custom ordering
class MyClass : Ord<MyClass> {
    override infix fun ord(rhs: MyClass): Order =
        orderBetween(this.priority, rhs.priority)
}

// Comparison operators
if (a ls b) { /* a < b */ }  // less than
if (a leq b) { /* a <= b */ } // less or equal
if (a gr b) { /* a > b */ }  // greater than
if (a geq b) { /* a >= b */ } // greater or equal
```

#### Predicate Composition

```kotlin
// Predicate combination
val combined = predicate1 and predicate2
val either = predicate1 or predicate2
val negated = !predicate

// Three-way comparator
val threeWay: ThreeWayComparator<T> = { lhs, rhs -> orderOf(compare(lhs, rhs)) }
```

#### Collection Extensions

```kotlin
// Multi-dimensional lists
typealias List2<T> = List<List<T>>
typealias List3<T> = List<List<List<T>>>

// Multi-dimensional Map
typealias MultiMap2<K1, K2, V> = Map<K1, Map<K2, V>>

// Shuffle (Fisher-Yates algorithm)
val shuffled = list.shuffle()

// MinMax simultaneous retrieval
val (min, max) = list.minMax()
val (minBy, maxBy) = list.minMaxBy { it.value }
```

### Context Variables (`context`)

```kotlin
// Create context variable
val myVar = ContextVar("default")

// Set value with auto-cleanup
myVar.set("value").use {
    println(myVar.get()) // "value"
}
// Auto-cleanup after use block ends
```

### Metaprogramming Tools (`meta_programming`)

#### Naming System Conversion

```kotlin
val transfer = NameTransfer(NamingSystem.CamelCase, NamingSystem.SnakeCase)
val snakeCase = transfer("playStation") // "play_station"
```

#### Lazy Delegate

```kotlin
// Range-based lazy delegate
val lazyValue by lazyDelegate { computeValue() }

// Self-referencing lazy delegate
val selfRef by selfLazyDelegate { this.computeValue() }

// Suspend lazy loading
val suspendValue by suspendLazy { computeAsync() }
```

### Parallel Processing (`parallel`)

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
| `foldParallelly` | Parallel fold |
| `minMaxByParallelly` | Parallel simultaneous min/max retrieval |

### Serialization (`serialization`)

#### JSON Read/Write

```kotlin
// JSON read/write
val obj = readFromJson<MyClass>("path/to/file.json")
writeJsonToFile("path/to/file.json", obj)

// With naming policy
val policy = JsonNamingPolicy(NamingSystem.CamelCase, NamingSystem.SnakeCase)
val obj = readFromJson<MyClass>("path/to/file.json", policy)
```

#### CSV Read/Write

```kotlin
// CSV read
val data = readFromCSV("path/to/file.csv")

// CSV write
writeCSVToFile("path/to/file.csv", data, headers = listOf("col1", "col2"))
```

#### Duration Serializers

```kotlin
// Discrete Duration serialization
@Serializable
data class Config(
    @Serializable(DiscreteDurationSerializer::class)
    val timeout: Duration
)

// Continuous Duration serialization
@Serializable
data class Timing(
    @Serializable(ContinuousDurationSerializer::class)
    val duration: Duration
)
```

### Testing

```bash
mvn -pl ospf-kotlin-utils test -DskipITs
```