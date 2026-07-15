# solver/heuristic — 启发式求解器包

:us: [English](README.md) | :cn: 简体中文

## 概述

`heuristic` 子包提供了 OSPF 框架内置的**元启发式求解框架**。包含基于种群的优化算法（如粒子群优化 PSO），以及选择、交叉、变异、迁移和迭代控制等支撑组件。

## 包结构

```
heuristic/
├── ParticleSwarmHeuristicSolver.kt  # 粒子群优化求解器
├── Population.kt                    # 种群管理
├── Selection.kt                     # 选择算子
├── SelectionMode.kt                 # 选择模式（轮盘赌、锦标赛等）
├── Cross.kt                         # 交叉算子
├── CrossMode.kt                     # 交叉模式
├── Mutation.kt                      # 变异算子
├── MutationMode.kt                  # 变异模式
├── Migration.kt                     # 种群间迁移策略
├── Normalization.kt                 # 归一化工具
├── Iteration.kt                     # 迭代控制与收敛
└── Policy.kt                        # 策略定义
```

## 核心概念

### 粒子群优化 (`ParticleSwarmHeuristicSolver.kt`)

PSO 算法实现，通过模拟粒子群行为进行优化。每个粒子维护位置（解）和速度，受个体最优和全局最优解引导。

### 种群管理 (`Population.kt`)

管理个体（候选解）集合，用于进化算法。支持适应度评估和个体生命周期管理。

### 选择策略 (`Selection.kt`, `SelectionMode.kt`)

用于在进化算法中选择父代的选择算子：
- **轮盘赌** — 按适应度比例的概率选择
- **锦标赛** — k 个随机个体中的最优选择

### 交叉策略 (`Cross.kt`, `CrossMode.kt`)

组合父代解以产生后代的交叉算子：
- 均匀交叉
- 单点交叉
- 两点交叉

### 变异策略 (`Mutation.kt`, `MutationMode.kt`)

引入随机扰动的变异算子：
- 均匀变异
- 高斯变异
- 边界变异

### 迁移策略 (`Migration.kt`)

岛模型并行化的种群间个体迁移策略。

### 迭代控制 (`Iteration.kt`)

控制迭代次数、收敛准则和停止条件。

## 与其他包的关系

- **solver** — 启发式求解器使用 `CallBackModel` 接口而非稀疏矩阵模型
- **model/callback** — 提供目标函数和约束评估的回调模型接口