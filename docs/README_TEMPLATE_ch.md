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

## Public API

列出用户应导入或依赖的稳定入口。

| API | 职责 |
| --- | --- |
| `<TypeOrFunction>` | `<responsibility>` |

## 建模扩展点

对于 framework 模块，说明 context、aggregation、model component、pipeline、extra context 或 extra pipeline 扩展点。如果不是 framework 模块，则说明相关扩展面，或省略本节。

## 泛型数值边界

说明 public API 是否使用 `V : RealNumber<V>` 或 `V : FloatingNumber<V>` 等泛型数值类型，以及哪里允许转换为 `Flt64`。

## 物理量边界

说明哪些字段应使用 `Quantity<V>` 或显式物理单位包装类型，哪些值可以保持无量纲。

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
