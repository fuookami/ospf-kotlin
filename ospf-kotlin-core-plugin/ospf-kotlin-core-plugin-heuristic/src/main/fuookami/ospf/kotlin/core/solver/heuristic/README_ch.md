# solver/heuristic — 启发式求解器包

:us: [English](README.md) | :cn: 简体中文

持久化会话和高层调度参见[超启发式框架](../hyper_heuristic/README_ch.md)。
完整求解 `AbstractRunner.run` 现在返回 `Ret<List<Individual<O, V>>>`。

## 概述

`heuristic` 包是元启发式插件的契约层与组装层。这里的算法都是**组合**出来的，从不手写：
一条生命周期骨架驱动循环，一组无状态算子决定「一步算什么」。

本包同时承载插件的公开面——`HeuristicSolver` 契约、个体/种群类型，以及 27 个求解器入口。

## 包结构

```
heuristic/                       （根包，刻意拍平）
├── HeuristicSolver.kt           # 求解器契约 + RunnerSolver 适配器
├── HeuristicResult.kt           # HeuristicResult、HeuristicSolutionStatus
├── Iteration.kt                 # 迭代计数器
├── Population.kt                # Individual、SolutionWithFitness
├── {算法}.kt × 27               # 每个算法一个顶层函数，转发工厂的全部参数
├── APSO.kt / CLPSO.kt / LSHADE.kt / GA.kt # 使用策略或规格 data class 的门面
├── runner/                      # 生命周期骨架
│   ├── AbstractRunner.kt        # 循环 + 档案 + 终止 + 观测者，与算法无关
│   ├── IterationObserver.kt     # 奖励反馈通道
│   ├── PopulationRunner.kt      # 一代 = 更新 → 变异 → 环境选择
│   ├── TrajectoryRunner.kt      # 一步 = 邻域候选 → 接受判据
│   ├── ParticleSwarmRunner.kt   # 粒子状态（位置 / 速度 / 个体最优）
│   ├── IslandRunner.kt          # N 个子种群 + 迁移
│   ├── Nsga2Runner.kt           # 真多目标（Pareto 排序）
│   ├── Nsga3Runner.kt           # 参考点式多目标
│   ├── ShadeRunner.kt           # 成功历史记忆
│   ├── ClpsoRunner.kt           # 分维度学习
│   └── CmaEsRunner.kt           # 分布 (m, σ²C) 状态
├── operator/                    # 18 个算子族（见 operator/README.md）
│   ├── Ordering.kt              # compareStrictly：严格弱序适配
│   ├── NumericSupport.kt        # 边界 / 拆包 / 夹紧 / 熵 / 概率辅助
│   ├── update/                  # SolutionUpdate · Cross · 18 个更新 · 10 个交叉
│   ├── mutation/                # Mutation · 12 个变异 · NoMutation
│   └── rl/                      # Q 学习：表 · 离散化 · 选择器 · 奖励
├── solver/                      # 组合式求解器工厂
│   ├── ComposedSolvers.kt       # 34 个单目标工厂
│   ├── MultiObjectiveSolvers.kt # NSGA-II / NSGA-III
│   └── MemeticSolvers.kt        # 在 GA 上挂局部搜索

```

### 为什么只有门面在根包

27 个门面直接位于根包：门面没有状态、没有自己的策略，独立命名空间买不到任何东西。凡是能填进槽位的
零件都在 `operator/` 下，这让「插件来自 `operator/`」这条规则成立且无需例外：

```kotlin
import fuookami.ospf.kotlin.core.solver.heuristic.*                    // 契约 + 27 个求解器
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.*    // SolutionUpdate、交叉
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.*  // Mutation、变异
```

交叉曾经也放在这里，理由是「跨族共享的数学原语」。这个说法在 `Mutation` 被确认为第 18 个算子族之后
就不成立了——它是一份契约加 13 个实现，被骨架以与 `SolutionUpdate` 完全相同的方式消费。族就该和族
待在一起。

现在已经没有任何算法子包。`gwo/`、`sca/`、`mvo/`、`saa/` 带着私有循环，最先删除；`ga/` 与 `pso/`
带着骨架之前的策略类，在 `StoppingCriterion` 与各算子族覆盖其能力后最后折叠。`ga/` 的异构岛屿
配置以 `IslandSpec(size, mutationRate)` 的形式保留——旧 `PopulationBuilder` 还带着 `eliteAmount`
与 `parentAmountRange`，但没有任何骨架读取它们。

## 核心概念

### 骨架与算子

骨架只暴露三个钩子——`initialize`、`advance`、`membersOf`——外加循环、档案、终止判断与回调。
它不知道自己装的是哪个算子。正因如此，GA 的交叉、灰狼的位置更新、正弦余弦的更新可以占据
`PopulationRunner` 的同一个槽位。

### 奖励通道

`AbstractRunner.run` 在推进迭代前，把「这一轮有没有改进」（`Archive.update` 的返回值）广播给每个
注册的 `IterationObserver`。自适应算子——`operator/rl` 里的 Q 学习，以及将来任何自适应步长——
消费该信号，而不必自己重复计算收敛情况。

### 已删除的旧契约

`Selection` / `SelectionMode` / `Normalization` / `CrossMode` / `MutationMode` 均已删除。它们是骨架
出现之前的策略参数：选择移交给 `survivor` 族、归一化移交给 `fitness` 族、迁移移交给
`operator/migration`，而两个「模式」新骨架根本不读。

## 与其他包的关系

- **core** — 启发式求解器使用 `CallBackModel` 接口而不是稀疏矩阵模型
- **model/callback** — 提供目标与约束求值的回调模型接口
- **operator/** — 算法所组合的 18 个算子族
- **runner/** — 生命周期骨架
