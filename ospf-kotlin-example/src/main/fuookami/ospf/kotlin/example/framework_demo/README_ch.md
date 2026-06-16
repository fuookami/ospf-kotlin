# framework_demo

:us: [English](README.md) | :cn: 简体中文

## 简介

`framework_demo` 包含面向框架的示例应用，展示 OSPF Kotlin 的 DDD（领域驱动设计）风格优化建模。每个演示将其领域逻辑组织为**上下文（context）**，每个上下文拥有一个**聚合（aggregation）**（领域模型）、**服务（service）**层（业务限制/约束），并将其变量和约束注册到共享的 `LinearMetaModel` 中。

## 作用范围

| 演示 | 说明 | 详情 |
| --- | --- | --- |
| [`demo1`](demo1/README_ch.md) | 带带宽约束的网络路由 | 使用 `route_context` 和 `bandwidth_context` 的最短服务路径 (SSP) |
| [`demo2`](demo2/README_ch.md) | 飞机货物装载优化 | 完整生产级示例，11 个领域上下文，支持 Benders 分解 |
| [`demo3`](demo3/README_ch.md) | 一维下料问题 (CSP1D) | 通过列生成和 SCIP 求解 |
| [`demo4`](demo4/README_ch.md) | 机组排班 | 跨 `gantt_scheduling` 框架类型的泛型数量使用 |

## 模块结构

| 演示 | 说明 | 领域上下文 |
| --- | --- | --- |
| `demo1` | 带带宽约束的网络路由 | `route_context`（路由分配）、`bandwidth_context`（带宽分配） |
| `demo2` | 飞机货物装载优化 | `aircraft`（飞机型号、甲板、燃油）、`stowage`（货物、装载、位置）、`mac`（平均气动弦）、`airworthiness_security`（结构限制）、`soft_security`（压舱物、空载）、`mac_optimization`（重心优化）、`express_effectiveness`（快递货物优先级）、`loading_effectiveness`（装载顺序、拖车） |
| `demo3` | 一维下料问题（列生成） | `material`（产品、材料宽度范围）、`cutting_plan_generation`（定价、初始方案生成） |
| `demo4` | 甘特排程泛型数量示例 | `task`（任务时间、切换）、`produce`（生产、消耗）、`resource`（容量、使用）、`bunch_compilation`（迭代、解摘要） |

## 架构模式

每个领域上下文遵循以下结构：

```
context/
  Context.kt              -- 上下文类：init()、register()、construct()、analyze()
  Aggregation.kt          -- 聚合根：持有领域模型状态
  model/                  -- 领域模型类（实体、值对象）
  service/
    AggregationInitializer.kt  -- 从输入初始化聚合
    limits/                    -- 约束定义（业务规则）
```

典型流程为：

1. **init** -- 从输入 DTO 初始化上下文，填充每个上下文的聚合。
2. **register** -- 每个上下文将其变量和中间符号注册到共享的 `LinearMetaModel`。
3. **construct** -- 每个上下文将其约束（限制）和目标函数贡献添加到模型。
4. **solve** -- 通过 SCIP 或列生成求解模型。
5. **analyze** -- 每个上下文提取其对应的解部分。

## 用法

每个演示的入口点是一个 `Application` 类（或等效类），带有一个 suspend `invoke()` 运算符：

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo1.SSP

suspend fun main() {
    val ssp = SSP()
    val input = Input(/* ... */)
    val result = ssp(input)
    when (result) {
        is Ok -> println("解: ${result.value}")
        is Failed -> println("失败: ${result.error}")
        is Fatal -> println("致命错误: ${result.errors}")
    }
}
```

demo3（CSP1D 列生成）：

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo3.CSP

suspend fun main() {
    val csp = CSP()
    csp()
}
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
