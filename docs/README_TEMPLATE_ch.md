# <module-name>

:us: [English](README.md) | :cn: 简体中文

## 简介

简要说明本模块提供什么能力，以及它在 OSPF Kotlin 仓库中的位置。

## 作用范围

说明哪些内容属于本模块。对于 framework 模块，应说明可复用领域能力，并把下游业务 adapter 排除在本模块之外。

明确非目标：

1. 业务专属 DTO、公式语言、租户上下文和运行时策略。
2. 求解器后端实现，除非本模块本身就是 plugin 模块。
3. starter 依赖聚合，除非本模块本身就是 starter 模块。

## 模块结构

| 包或子模块 | 职责 |
| --- | --- |
| `<name>` | `<responsibility>` |

## 架构概览

对于 framework 模块，说明 context / aggregation / model component / pipeline 流程和 `MetaModel` 注册路径。对于 utility 或 math 模块，说明主要类型层次和数据流边界。

<!-- 对于简单模块，本节可简短描述。对于复杂模块，建议使用图表或流程说明。 -->

## 核心概念

列出读者使用或扩展本模块前需要理解的概念。

<!-- 示例：
- **Linear Meta Model**: 优化模型容器，持有变量、约束和目标函数。
- **Shadow Price**: 约束的对偶值，表示放松约束的边际成本。
-->

## Public API

| API | 职责 | 稳定性 |
| --- | --- | --- |
| `<TypeOrFunction>` | `<responsibility>` | stable / migration / internal |

<!-- 稳定性等级：
  - stable: API 已生产就绪且向后兼容
  - migration: API 正在迁移中或将会变更
  - internal: API 仅供内部使用，不属于公开契约
-->

## 建模扩展点

对于 framework 模块，说明 context、aggregation、model component、pipeline、extra context 或 extra pipeline 扩展点。如果不是 framework 模块，则说明相关扩展面，或省略本节。

## 泛型数值边界

说明 public API 是否使用 `V : RealNumber<V>` 或 `V : FloatingNumber<V>` 等泛型数值类型，以及哪里允许转换为 `Flt64`。

## 物理量边界

说明哪些字段应使用 `Quantity<V>` 或显式物理单位包装类型，哪些值可以保持无量纲。

## 求解生命周期

对于 framework 模块，按实际情况说明以下阶段：

1. **注册**: 变量、约束和目标函数如何注册到模型。
2. **LP/MILP 求解**: 求解器如何调用和配置。
3. **Shadow Price 提取**: 如何从已求解的约束中获取对偶值。
4. **加列/删列**: 如何动态添加或删除列（如适用）。
5. **最终求解**: 如何触发最终优化求解。
6. **结果提取**: 如何从已求解的模型中提取结果。

<!-- 对于 utility 或 math 模块，可省略本节或替换为相关生命周期说明。 -->

## 输出

说明模块的主要输出：

- **Solution**: 优化结果（变量值、目标函数值）。
- **Trace**: 执行轨迹或求解器日志（如适用）。
- **KPI**: 从解中提取的关键性能指标。
- **Render DTO**: 用于可视化的数据传输对象（如适用）。
- **Diagnostic**: 错误报告、验证结果或调试信息。

<!-- 不适用的子节可省略。 -->

## 使用方式

```kotlin
// 最小示例。
```

## 本地验证

```powershell
mvn -B -ntp -pl <module-name> -am test
```

## 相关模块

- [根项目 README](../README_ch.md)
