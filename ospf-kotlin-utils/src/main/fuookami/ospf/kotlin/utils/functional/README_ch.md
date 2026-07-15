# functional

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 项目的函数式编程工具集。

## 概述

本包提供核心函数式编程抽象，包括结果类型、相等性/排序类型类、变体类型、谓词组合和集合扩展。

## 模块

### 结果类型 (`Result.kt`)

支持 `Ok`、`Failed`、`Fatal` 和 `Warn` 四种状态的错误处理，类似于 Rust 的 Result 类型。

```kotlin
val ok: Result<String, Error> = Ok("success")
val failed: Result<String, Error> = Failed(ErrorCode.FileNotFound, "文件不存在")
val fatal: Result<String, Error> = Fatal(errors)
val warn: ExResult<String, Error> = Warn("value", ErrorCode.Other, "警告信息")

result.ifOk { println("成功: ${it.value}") }
    .ifFailed { println("失败: ${it.message}") }
    .ifFatal { it.forEach { error -> println(error) } }
```

### Either 类型 (`Either.kt`)

可以是 `Left` 或 `Right` 的值，支持类型安全的提取和模式匹配。

```kotlin
val left: Either<Int, String> = Either.Left(42)
val right: Either<Int, String> = Either.Right("hello")

val result = either.ifLeft { "Left: $it" }.ifRight { "Right: $it" }.invoke()
```

### 变体类型 (`Variant.kt`)

类型安全的和类型（Variant2 到 Variant20），支持流式模式匹配。

```kotlin
val v: Variant3<Int, String, Double> = Variant3.V2("hello")

val result = v
    .if1 { "Int: $it" }
    .if2 { "String: $it" }
    .if3 { "Double: $it" }
    .invoke()
```

### 相等性与排序 (`Eq.kt`、`Ord.kt`)

Haskell 风格的 `Eq` 和 `Ord` 类型类，用于类型安全的相等性和排序。

```kotlin
class MyClass : Ord<MyClass> {
    override infix fun ord(rhs: MyClass): Order = orderBetween(this.priority, rhs.priority)
}

if (a ls b) { /* a < b */ }
if (a gr b) { /* a > b */ }
```

### 谓词组合 (`Predicate.kt`)

可组合的谓词，支持 `and`、`or`、`xor`、`not` 运算符，以及各种谓词/提取器类型别名。

```kotlin
val combined = predicate1 and predicate2
val either = predicate1 or predicate2
val negated = !predicate
```

### 集合扩展 (`Collection.kt`)

扩展的集合操作，包括：
- 洗牌（Fisher-Yates 算法）
- 使用各种比较器类型的 min/max/minMax
- 使用部分和三路比较器的过滤/排序
- 具有空值安全性的关联操作

```kotlin
val (min, max) = list.minMax()
val (minBy, maxBy) = list.minMaxBy { it.value }
val shuffled = list.shuffle()
```

### 元组类型 (`Quadruple.kt`)

四元组数据类和均质元组类型别名（`Pair2`、`Triple3`、`Quadruple4`）。

### 其他工具

| 文件 | 说明 |
|------|------|
| `Boolean.kt` | 布尔扩展函数 |
| `Condition.kt` | 条件执行工具 |
| `DateTimeRange.kt` | 日期时间范围类型和操作 |
| `List.kt` | 多维列表类型别名和扩展 |
| `Map.kt` | 多维映射类型别名和扩展 |
| `MinMax.kt` | MinMax 结果类型和基于比较器的操作 |
| `Nullable.kt` | 空值安全工具（`ifNull`、`ifNullOrEmpty`） |
