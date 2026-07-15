# demo1 — 带带宽约束的网络路由

:us: [English](README.md) | :cn: 简体中文

## 简介

`demo1` 演示**最短服务路径 (SSP)** 问题：在网络中为服务分配路由，同时满足边和节点的带宽容量约束。使用两个领域上下文 — `route_context`（路由分配）和 `bandwidth_context`（带宽分配）— 构建线性优化模型，由 SCIP 求解。

## 作用范围

- 建模有向网络图（边、节点、服务）
- 为每条服务分配一条路径（边序列），最小化总成本
- 在边和转运节点上施加带宽约束
- 满足服务需求和容量限制

## 模块结构

| 包 | 职责 |
| --- | --- |
| `route_context` | 路由分配：图模型、服务路径、分配约束 |
| `bandwidth_context` | 带宽分配：边/节点/服务带宽、成本目标 |
| `infrastructure` | 输入/输出 DTO 定义 |

### route_context

| 文件 | 说明 |
| --- | --- |
| `model/Graph.kt` | 网络图：节点和边 |
| `model/Service.kt` | 服务定义（源、汇、需求） |
| `model/Assignment.kt` | 路由分配决策变量 |
| `service/limits/NodeAssignmentConstraint.kt` | 每个节点的流守恒 |
| `service/limits/ServiceAssignmentConstraint.kt` | 每条服务恰好分配一条路径 |
| `service/limits/ServiceCostObjective.kt` | 最小化总路由成本 |
| `service/PipelineListGenerator.kt` | 约束注册管道 |

### bandwidth_context

| 文件 | 说明 |
| --- | --- |
| `model/EdgeBandwidth.kt` | 边带宽容量和使用量 |
| `model/NodeBandwidth.kt` | 转运节点带宽容量 |
| `model/ServiceBandwidth.kt` | 服务带宽需求 |
| `service/limits/EdgeBandwidthConstraint.kt` | 边带宽容量约束 |
| `service/limits/TransferNodeBandwidthConstraint.kt` | 转运节点容量约束 |
| `service/limits/ServiceCapacityConstraint.kt` | 服务容量约束 |
| `service/limits/DemandConstraint.kt` | 服务需求满足 |
| `service/limits/BandwidthCostObjective.kt` | 带宽成本目标 |
| `service/SolutionAnalyzer.kt` | 从解中提取路由分配 |

## 架构

```
demo1/
  Application.kt          -- 入口：SSP 类，带 invoke()
  Interface.kt             -- 公共接口定义
  infrastructure/DTO.kt    -- 输入/输出 DTO
  route_context/           -- 路由分配领域
    RouteContext.kt         -- 上下文：init, register, construct, analyze
    Aggregation.kt          -- 领域模型状态
    model/                  -- Graph, Service, Assignment
    service/limits/         -- 约束和目标
  bandwidth_context/       -- 带宽分配领域
    BandwidthContext.kt     -- 上下文：init, register, construct, analyze
    Aggregation.kt          -- 领域模型状态
    model/                  -- 边/节点/服务带宽
    service/limits/         -- 约束和目标
```

## 用法

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo1.SSP

suspend fun main() {
    val ssp = SSP()
    val input = Input(/* 网络图、服务、带宽参数 */)
    val result = ssp(input)
    when (result) {
        is Ok -> println("解: ${result.value}")
        is Failed -> println("失败: ${result.error}")
        is Fatal -> println("致命错误: ${result.errors}")
    }
}
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
