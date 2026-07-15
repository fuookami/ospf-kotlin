# 批次选择（Bunch Selection）

:us: [English](README.md) | :cn: 简体中文

负责使用分支定价算法选择最优航班任务束集合的限界上下文。编排主问题求解和定价子问题协调，以最低成本找到覆盖所有必需任务的最优束组合。

## 职责

- 求解主问题（集合覆盖/集合划分公式）以选择束。
- 与 bunch_generation 协调定价具有负缩减成本的新束。
- 与 bunch_compilation 协调向模型添加新列。
- 对分数解进行分支以获得整数可行的排班方案。

## 核心类

| 类名 | 说明 |
|---|---|
| `BunchSelectionContext` | 分支定价算法中批次选择操作的入口上下文。 |
| `BranchAndPriceAlgorithm` | 航班恢复调度问题的分支定价求解器。 |

## 依赖

- **bunch_compilation** — 注册约束并添加列。
- **bunch_generation** — 通过定价生成新束。
- **task** — 提供 `FlightTaskBunch`、`ShadowPriceMap`。
