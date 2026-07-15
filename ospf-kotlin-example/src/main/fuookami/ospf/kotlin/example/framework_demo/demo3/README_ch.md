# demo3 — 一维下料问题 (CSP1D)

:us: [English](README.md) | :cn: 简体中文

## 简介

`demo3` 演示通过列生成和 SCIP 求解器解决**一维下料问题 (CSP1D)**。给定一组固定宽度的原材料和一组具有特定宽度需求的产品，目标是找到最小化原材料使用量的切割方案，同时满足所有产品需求。

## 作用范围

- 定义固定宽度的原材料（卷/板材）
- 定义具有宽度需求和数量的产品
- 使用定价子问题生成切割方案（列）
- 使用 SCIP 通过列生成求解主问题
- 使用物理量（`Quantity<Flt64>` 配合 `Meter` 单位）表示宽度

## 模块结构

| 文件 | 说明 |
| --- | --- |
| `Main.kt` | 入口：`CSP` 类，定义问题并求解 |

## 问题描述

CSP1D 问题：
- **输入**：原材料宽度（如 1000mm）和一组产品 `(宽度, 需求)` 对
- **输出**：最小化原材料使用量的切割方案
- **方法**：列生成 — 主问题选择方案，定价子问题生成负缩减成本的新方案

### 示例数据

| 产品 | 宽度 (mm) | 需求 |
| --- | --- | --- |
| p-0 | 450 | 97 |
| p-1 | 360 | 610 |
| p-2 | 310 | 395 |
| p-3 | 140 | 211 |

## 架构

本演示使用 `csp1d` 框架模块：

```
ospf-kotlin-framework-csp1d/
  csp1d-application/
    model/Csp1dConfiguration.kt  -- 问题配置
    model/csp1dProblem.kt        -- 问题定义
    service/Csp1dColumnGeneration.kt  -- 列生成算法
  csp1d-domain-material/
    model/Material.kt            -- 原材料定义
    model/Product.kt             -- 产品定义
  csp1d-domain-cutting_plan_generation/
    model/GenerationConstraints.kt  -- 生成约束
    service/FullSumGenerator.kt     -- 全和方案生成器
    service/NSameGenerator.kt       -- N-相同方案生成器
    ReducedCostPricingGenerator.kt  -- 定价子问题
```

## 用法

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo3.CSP

suspend fun main() {
    val csp = CSP()
    val result = csp()
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
