# token — Token 管理包

:us: [English](README.md) | :cn: 简体中文

## 概述

`token` 包是 OSPF 框架中连接**变量**与**求解器**的核心桥梁层。它管理变量项与求解器索引之间的映射关系，提供求解结果的双视图访问（Flt64 求解器视图 ↔ 泛型 V 类型视图），并实现了多层级的缓存机制用于中间符号求值和表达式展开。

## 包结构

```
token/
├── Token.kt                      # Token 数据结构（变量↔求解器映射单元）
├── TokenList.kt                  # Token 列表（不可变/可变/自动/手动）
├── TokenTable.kt                 # Token 表接口与实现
├── ConcurrentTokenTable.kt       # 并发 Token 表
├── TokenCacheContext.kt          # 缓存上下文（线性展开/二次展开/值/范围）
├── TokenCacheKey.kt              # 缓存键
└── TokenTableRegistrationSupport.kt # Token 表注册辅助
```

## 核心概念

### Token (`Token.kt`)

`Token<V>` 是核心数据结构，表示一个变量在求解器中的注册单元。它采用**双视图模式**：

- **Flt64 视图**（`resultFlt64`）— 求解器后端始终产出 `Flt64`，用于求解器内部交互
- **V 类型视图**（`result`）— 通过 `IntoValue<V>` 转换器提供类型安全的公开 API

核心属性：
- `variable` — 关联的变量项（`AbstractVariableItem`）
- `solverIndex` — 求解器中的索引
- `result` / `resultFlt64` — 求解结果（双视图）
- `lowerBound` / `upperBound` — 变量边界
- `range` — 值范围（`ValueRange<Flt64>`）

### TokenList (`TokenList.kt`)

管理变量与 Token 之间的映射关系，提供四种实现：

| 类 | 特点 |
|---|---|
| `TokenList` | 不可变 Token 列表，线程安全 |
| `MutableTokenList` | 可变 Token 列表的密封基类 |
| `AutoTokenList` | 查询时若 Token 不存在则自动创建 |
| `ManualTokenList` | 变量需显式添加后才能查询 |

所有列表维护按 `solverIndex` 排序的 `tokensInSolver` 视图，并支持按列表/映射形式设置求解结果。

### TokenTable (`TokenTable.kt`)

Token 表是更高层的抽象，将 Token 列表与中间符号集合组合在一起，提供：

- **符号注册** — 注册 `IntermediateSymbol`，验证操作类别和重复性
- **Token 查询** — 按变量项、索引查找 Token
- **求解结果管理** — 设置/清除求解结果
- **多级缓存** — 线性展开缓存、二次展开缓存、值缓存、范围缓存
- **符号依赖管理** — 追踪符号间的依赖关系，验证无环

实现层次：
- `AbstractTokenTable<V>` — 抽象接口
- `AbstractMutableTokenTable<V>` — 可变表接口
- `TokenTable<V>` — 不可变实现
- `MutableTokenTable<V>` — 可变实现的密封基类

### 缓存体系 (`TokenCacheContext.kt`)

提供四种专用缓存上下文：

- **`LinearFlattenContext`** — 线性表达式展开缓存（`LinearFlattenData`）
- **`QuadraticFlattenContext`** — 二次表达式展开缓存（`QuadraticFlattenData`）
- **`ValueCacheContext`** — 求解值缓存，按 solution 和 fixedValues 两个维度分别缓存
- **`RangeCacheContext`** — 表达式范围缓存（`ExpressionRange`）

`TokenCacheContexts` 聚合容器统一管理所有缓存上下文，支持按符号绑定和解绑 Token 表上下文。

### 并发支持 (`ConcurrentTokenTable.kt`)

线程安全的 Token 表实现，适用于多线程求解场景。

## 双视图设计模式

```
求解器后端 ──Flt64──▶ Token._result ──IntoValue<V>──▶ Token.result (V?)
                                                  │
                                          Token.resultFlt64 (Flt64?)
```

这种设计使得：
- 求解器插件只需处理统一的 `Flt64` 类型
- 用户 API 通过 `IntoValue<V>` 获得类型安全的 V 类型视图
- 无需运行时强制类型转换

## 与其他包的关系

- **variable** — Token 持有 `AbstractVariableItem` 引用，建立变量到求解器索引的映射
- **symbol** — 中间符号通过 TokenTable 查询和缓存求值结果
- **model** — 模型通过 TokenTable 注册变量和符号，设置求解结果
- **solver** — 求解器通过 TokenList 的 `setSolverSolution` 写入 `Flt64` 结果