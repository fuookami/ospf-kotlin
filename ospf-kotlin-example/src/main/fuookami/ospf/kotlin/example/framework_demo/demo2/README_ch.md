# demo2 — 飞机货物装载优化

:us: [English](README.md) | :cn: 简体中文

## 简介

`demo2` 是一个完整的**飞机货物装载优化**生产级示例。它确定如何将货物装入飞机，同时满足适航性、结构和操作约束。模型支持多种求解路径，包括直接 MILP 和 Benders 分解。

## 作用范围

- 建模飞机物理属性（机身、甲板、燃油、舱门）
- 定义货物项目（位置、装载、预约）
- 施加适航约束（CLIM、包络线、纵/横向平衡）
- 优化 MAC（平均气动弦）以控制重心
- 应用软安全约束（压舱物、空载）
- 在满足结构限制的前提下最大化载荷
- 支持快递货物优先级和装载顺序优化

## 模块结构

| 领域上下文 | 职责 |
| --- | --- |
| `aircraft` | 飞机模型：机身、甲板、燃油、舱门、装载顺序 |
| `stowage` | 货物装载：项目、装载、位置、预约 |
| `mac` | 平均气动弦计算 |
| `airworthiness_security` | 结构限制：CLIM、包络线、区域重量、累积载荷 |
| `soft_security` | 压舱物和空载约束 |
| `mac_optimization` | 重心优化（纵/横向平衡） |
| `payload_maximization` | 最大化货物载荷 |
| `express_effectiveness` | 快递货物优先级排序 |
| `loading_effectiveness` | 装载顺序、拖车管理、顺序装载 |
| `recommended_weight_equalization` | 重量均衡和优先预约 |
| `redundancy` | 冗余约束和实验性平衡 |
| `infrastructure` | 求解器配置、DTO、Benders 策略、诊断 |

## 架构

每个领域上下文遵循 DDD 模式：

```
domain/<context>/
  <Context>Context.kt     -- 上下文类：init(), register(), construct(), analyze()
  Aggregation.kt          -- 聚合根：持有领域模型状态
  model/                  -- 领域实体和值对象
  service/
    AggregationInitializer.kt  -- 从输入初始化聚合
    PipelineListGenerator.kt   -- 将约束注册到模型
    limits/                    -- 约束定义（业务规则）
    SolutionAnalyzer.kt        -- 提取该上下文的解
```

## 应用入口

| 文件 | 说明 |
| --- | --- |
| `FullLoadApplication.kt` | 满载优化 |
| `LoadingOrderApplication.kt` | 装载顺序生成 |
| `PredistributionApplication.kt` | 预分配优化 |
| `WeightRecommendationApplication.kt` | 重量推荐 |

## 用法

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo2.FullLoadApplication

suspend fun main() {
    val app = FullLoadApplication()
    val input = RequestDTO(/* 飞机配置、货物项目 */)
    val result = app(input)
    when (result) {
        is Ok -> println("装载方案: ${result.value}")
        is Failed -> println("失败: ${result.error}")
        is Fatal -> println("致命错误: ${result.errors}")
    }
}
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
