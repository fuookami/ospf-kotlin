# core_demo

:us: [English](README.md) | :cn: 简体中文

## 简介

`core_demo` 包含可运行的优化模型构建演示，展示 OSPF Kotlin 核心 API。每个演示构建线性或二次元模型，注册变量/符号/约束/目标函数，进行求解并提取解。`GenericNumberDemo` 验证模型构建器在多种数值类型（`Flt64`、`Rtn64`、`FltX`、`RtnX`）下均可正确工作。

## 作用范围

- Demo1 到 Demo17 使用 `LinearMetaModel`、中间表达符号和 SCIP 求解器对各种经典优化问题进行建模。
- `GenericNumberDemo` 使用四种数值类型构建线性和二次元模型，并在不求解的情况下提取机制级摘要。

## 模块结构

| 文件 | 问题类型 |
| --- | --- |
| `Demo1.kt` | 资本投资选择 -- 在资本和负债约束下最大化利润（二元变量） |
| `Demo2.kt` | 分配问题 -- 将产品分配给公司以最小化总成本（二元变量） |
| `Demo3.kt` | 生产规划 / 配料 -- 满足产品产出要求的同时最小化材料成本（无符号整数变量） |
| `Demo4.kt` | 生产优化 -- 在材料可用性和产量差异约束下最大化利润（实数变量） |
| `Demo5.kt` | 0-1 背包问题 -- 在重量限制下最大化货物价值（二元变量） |
| `Demo6.kt` | 有界背包问题 -- 在每项物品数量限制下最大化货物价值（无符号整数变量） |
| `Demo7.kt` | 运输问题 -- 最小化从仓库到商店的运输成本（无符号整数变量） |
| `Demo8.kt` | 设备工时约束下的生产规划 -- 在设备产能约束下最大化利润（无符号整数变量） |
| `Demo9.kt` | 设施选址 -- 找到使到所有定居点的曼哈顿距离之和最小的点（整数变量，绝对值分解） |
| `Demo10.kt` | 旅行商问题 (TSP) -- 恰好访问所有城市一次的最短路径，使用 MTZ 子回路消除（二元 + 整数变量） |
| `Demo11.kt` | 最大流问题 -- 在有容量网络中最大化从源到汇的流量（无符号整数变量） |
| `Demo12.kt` | 投资组合 -- 在风险和资金分配约束下最大化收益，使用二值化和最大值函数（无符号整数变量） |
| `Demo13.kt` | 车辆路径 -- 在卡车容量约束下最小化从配送中心到经销商的配送距离（无符号整数变量） |
| `Demo14.kt` | 多商品流通 -- 最小化包含生产、销售和转运节点的运输成本（无符号整数变量） |
| `Demo15.kt` | 多工厂带替代的流通 -- 最小化从制造商到配送中心的物流成本，含车型替代规则（无符号整数 + 百分比变量） |
| `Demo16.kt` | 生产排程 -- 最小化跨月度的生产、仓储和延迟交付成本（无符号整数变量） |
| `Demo17.kt` | 带时间窗的车辆路径问题 (VRPTW) -- 最小化满足时间窗和容量约束的车队固定和行驶成本（二元 + 无符号实数变量） |
| `GenericNumberDemo.kt` | 泛型数值类型验证 -- 使用 `Flt64`、`Rtn64`、`FltX` 和 `RtnX` 构建线性和二次元模型，确认跨类型正确性 |

## 用法

每个演示是一个 Kotlin `data object` 或 `class`，带有一个 suspend `invoke()` 运算符。运行演示：

```kotlin
import fuookami.ospf.kotlin.example.core_demo.Demo1

suspend fun main() {
    val result = Demo1()
    when (result) {
        is Ok -> println("求解成功")
        is Failed -> println("失败: ${result.error}")
        is Fatal -> println("致命错误: ${result.errors}")
    }
}
```

`GenericNumberDemo`（非 suspend）：

```kotlin
import fuookami.ospf.kotlin.example.core_demo.GenericNumberDemo

fun main() {
    val summaries = GenericNumberDemo.runBuildAndDump()
    for (summary in summaries) {
        println("${summary.numberType}: linear=${summary.linear.success}, quadratic=${summary.quadratic.success}")
    }
}
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
