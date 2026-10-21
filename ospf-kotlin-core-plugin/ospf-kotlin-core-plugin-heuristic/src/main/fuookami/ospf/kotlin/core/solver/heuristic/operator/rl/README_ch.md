# rl — 强化学习算子

:us: [English](README.md) | :cn: 简体中文

## 概述

Q 学习进入元启发式有三个切入点，本包把它们分开：

| 关注点 | 契约 | 为什么分开 |
|--------|------|------------|
| 当前态势是什么？ | `StateDiscretizer` | Q 表是有限的，搜索态势是连续的 |
| 每个选择有多好？ | `QTable` | 纯粹的账本，不认识任何算法 |
| 该选哪一个？ | `ActionSelector` | 探索-利用权衡是一个旋钮，不是一次重写 |
| 刚才那步好不好？ | `Reward` | 与具体算法唯一的耦合点 |

`QlearningAgent` 负责把四者串起来。它刻意**不知道**动作是什么。

## 一份内核，两种用途

| 包装 | 动作是… | 效果 |
|------|---------|------|
| `QlearningUpdate` | 一个候选 `SolutionUpdate` | **算子选择** —— 每代切换更新规则 |
| `QlearningSource` | 一个候选 `ParameterSource` | **调参** —— 适配任何 `ParameterSource` 槽位 |

两者同时是 `IterationObserver`，因此骨架每轮结束后会把奖励交给它们：

```kotlin
// AbstractRunner.run 中 archive.update(...) 之后
for (observer in observers) {
    observer.observe(iteration, members, improved, converter)
}
```

## 状态：多样性 × 停滞度

`DiversityStagnationDiscretizer` 用两个**不需要目标值**的轴构造 `levels × levels` 网格：

- **多样性** —— 平均成对距离 ÷ 最大成对距离，落在 `[0, 1]`
- **停滞度** —— `notBetterIteration ÷ iteration`，落在 `[0, 1]`

两轴都不需要知道哪个个体最优，因此 `ObjValue` 不必成为类型参数，离散化器保持为无状态的 `data class`。

## 正确性说明

本模块此前的 Q 学习实现有三处缺陷，这里全部修正，且都有 `QlearningTest.kt` 的用例覆盖：

1. **动作表只有一个元素。** `mutableListOf(Flt64(9))` 构造出 `[9.0]`，是单元素列表，Q 表退化为标量。
   `QTable(stateAmount, actionAmount)` 按动作数分配整行；`QTableTest.shouldAllocateOneEntryPerAction` 钉住这一点。
2. **没有状态转移。** 时序差分目标取的是**当前**状态的最大值。`QTable.update` 使用 `nextState`：
   `Q(s,a) ← Q(s,a) + α·(r + γ·max_a' Q(s',a') − Q(s,a))`。
3. **Q 表从不影响决策。** 旧代码用 `state / 3`、`state % 3` 直接选参数，从不读 Q 值。
   这里选择器每轮都会读 `table.valuesOf(state)`。

参数校验是显式的：`QTable.update` 返回 `Ret<Flt64>`，下标越界或超参数非法返回 `Failed`。
代理在构造时把 `alpha`、`gamma` 夹取到 `[0, 1]`，因此循环本身没有失败分支。

## 示例

```kotlin
// 在两个群智能更新算子之间做选择。
val solver = ComposedSolvers.qLearning<Flt64, Flt64>(
    size = 30,
    stopping = IterationLimit(UInt64(400)),
    converter = IntoValue.Identity,
    entropy = Generator { Random.nextFlt64() },
    candidates = listOf(GreyWolfUpdate(), SineCosineUpdate())
)

// 为 PSO 的惯性权重调参。
val inertia = QlearningSource(
    candidates = listOf(ConstantSource(Flt64(0.4)), LinearSource(Flt64(0.9), Flt64(0.4))),
    randomGenerator = Generator { Random.nextFlt64() }
)
```

## 相关

- [算子族](../README_ch.md)
- [启发式插件 README](../../../../../README_ch.md)
- `runner/IterationObserver.kt` —— 奖励通道
