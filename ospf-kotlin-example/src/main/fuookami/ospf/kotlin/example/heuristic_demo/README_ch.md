# heuristic_demo

:us: [English](README.md) | :cn: 简体中文

## 简介

`heuristic_demo` 包含使用粒子群优化 (PSO) 的启发式求解器演示。这些演示展示如何使用 `CallBackModel` 处理黑盒目标函数，以及如何将 `LinearMetaModel` 转换为 `CallBackModel` 以进行基于 PSO 的求解。

## 作用范围

- **Demo1**：构建一个带有简单二次目标函数 `(x - 1)^2` 的 `CallBackModel`，直接通过 PSO 求解。
- **Demo2**：构建一个 `LinearMetaModel`（最大化 `x + y`，带边界），将其转换为 `MechanismModel`，然后包装为 `CallBackModel` 并用 PSO 求解。

## 模块结构

| 文件 | 说明 |
| --- | --- |
| `Demo1.kt` | 使用 `CallBackModel` 的简单二次最小化 -- 定义黑盒目标函数 `(x - 1)^2` 并用 PSO 求解（10 秒时间限制） |
| `Demo2.kt` | 线性元模型配合 PSO -- 通过 `LinearMetaModel` 构建线性模型，转换为机制模型，包装为 `CallBackModel`，用 PSO 求解（10 秒时间限制） |

## 用法

每个演示是一个带 `invoke()` 运算符的 `class`：

```kotlin
import fuookami.ospf.kotlin.example.heuristic_demo.Demo1

fun main() {
    val result = Demo1()
    when (result) {
        is Ok -> println("求解成功")
        is Failed -> println("失败: ${result.error}")
        is Fatal -> println("致命错误: ${result.errors}")
    }
}
```

```kotlin
import fuookami.ospf.kotlin.example.heuristic_demo.Demo2

fun main() {
    val result = Demo2()
    when (result) {
        is Ok -> println("求解成功")
        is Failed -> println("失败: ${result.error}")
        is Fatal -> println("致命错误: ${result.errors}")
    }
}
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
