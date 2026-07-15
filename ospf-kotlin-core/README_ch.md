# ospf-kotlin-core

:us: [English](README.md) | :cn: 简体中文

## 简介

ospf-kotlin-core 是 OSPF（Open Solver Platform Framework）Kotlin 项目的**核心模块**。它实现了数学优化模型的完整生命周期——从变量定义和符号表达式构建，到模型构建和展开，再到求解器抽象和结果获取。

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                       用户应用层                              │
├─────────────────────────────────────────────────────────────┤
│  model/       │ 优化模型定义与管理                            │
│  variable/    │ 变量类型系统与变量项                           │
│  symbol/      │ 中间符号表达式与函数符号                       │
│  token/       │ 变量-求解器映射与缓存                         │
│  solver/      │ 求解器抽象与值转换                            │
│  error/       │ 核心错误定义                                  │
├─────────────────────────────────────────────────────────────┤
│  ospf-kotlin-math  │ ospf-kotlin-utils  │ ospf-kotlin-multiarray │
└─────────────────────────────────────────────────────────────┘
```

## 模块结构

| 包 | 说明 | README |
|---|------|--------|
| `variable` | 变量类型系统、变量项、变量组合和范围 | [README](src/main/fuookami/ospf/kotlin/core/variable/README_ch.md) |
| `token` | Token 管理——变量-求解器映射、双视图结果、多级缓存 | [README](src/main/fuookami/ospf/kotlin/core/token/README_ch.md) |
| `symbol` | 中间符号系统——表达式、符号组合和函数符号 | [README](src/main/fuookami/ospf/kotlin/core/symbol/README_ch.md) |
| `model` | 优化模型生命周期——MetaModel → MechanismModel → Triad/TetradModel → Solver | [README](src/main/fuookami/ospf/kotlin/core/model/README_ch.md) |
| `solver` | 求解器抽象——线性/二次求解器、启发式、IIS 诊断、输出 | [README](src/main/fuookami/ospf/kotlin/core/solver/README_ch.md) |
| `error` | 核心错误码定义 | — |

## 四层模型架构

核心模块实现了**四层模型架构**：

```
用户定义层        →  MetaModel<V>                  （用户面向的 DSL）
    ↓ dump
机制模型层        →  MechanismModel<V>             （展开符号、约束）
    ↓ dump
标准形式层        →  LinearTriadModel /            （稀疏矩阵形式）
                    QuadraticTetradModel
    ↓ invoke
求解器层          →  SolverOutput                  （求解结果）
```

## 关键设计模式

### 双视图值访问

求解器后端始终产出 `Flt64` 结果。框架通过 `IntoValue<V>` 提供类型安全的访问：

```
求解器 → Flt64 → Token._result → IntoValue<V> → Token.result (V?)
```

### 中间符号系统

中间符号是由变量和常量组成的可求值表达式。框架提供 30+ 种内置函数符号（Slack、If、Max、分段线性等）用于约束构建。

### 缓存驱动求值

`TokenTable` 维护多级缓存（线性展开、二次展开、值、范围），避免模型构建和求解过程中的冗余符号求值。

## 子包概览

### solver/

| 子包 | 说明 |
|------|------|
| `config` | 求解器特定配置（COPT、Gurobi、SCIP） |
| `heuristic` | 元启发式框架（PSO、选择、交叉、变异） |
| `iis` | 不可约不可行子系统诊断 |
| `output` | 求解器输出数据结构（可行/不可行） |
| `value` | 值类型转换（IntoValue 接口） |

### model/

| 子包 | 说明 |
|------|------|
| `basic` | 基础接口、枚举和视图类型 |
| `mechanism` | MetaModel、MechanismModel、约束/目标 DSL |
| `intermediate` | 标准形式模型（三元组/四元组）、稀疏矩阵 |
| `callback` | 启发式求解器的回调模型接口 |

### symbol/

| 子包 | 说明 |
|------|------|
| `function` | 30+ 种函数符号（Slack、If、Max、分段线性等） |
| `flatten` | 表达式展开工具 |

## 测试

```bash
mvn -pl ospf-kotlin-core test -DskipITs