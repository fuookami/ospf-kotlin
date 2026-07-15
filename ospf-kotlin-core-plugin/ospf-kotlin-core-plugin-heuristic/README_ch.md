# ospf-kotlin-core-plugin-heuristic

:us: [English](README.md) | :cn: 简体中文

## 简介

OSPF Kotlin 框架的元启发式算法插件模块。本模块提供了一系列基于种群和基于轨迹的元启发式优化算法，所有算法遵循统一的策略驱动架构。

## 算法

| 算法 | 状态 | 类型 | 说明 |
|------|------|------|------|
| GA | :white_check_mark: | 种群 | 遗传算法——选择、交叉、变异、迁移 |
| PSO | :white_check_mark: | 种群 | 粒子群优化——惯性权重、加速 |
| GWO | :white_check_mark: | 种群 | 灰狼优化器——alpha/beta/delta 领导层次 |
| SCA | :white_check_mark: | 种群 | 正弦余弦算法——正弦余弦位置更新 + Q-learning |
| SAA | :white_check_mark: | 轨迹 | 模拟退火——温度调度、Metropolis 准则 |
| MVO | :white_check_mark: | 种群 | 多元宇宙优化器——白洞/虫洞穿越 |
| EVO | :construction: | — | 能量谷优化器（占位） |
| GCO | :construction: | — | 生发中心优化器（占位） |
| HCA | :construction: | — | 爬山算法（占位） |
| HS | :construction: | — | 和声搜索算法（占位） |
| NS | :construction: | — | 邻域搜索算法（占位） |
| SOA | :construction: | — | 海鸥优化算法（占位） |
| WarSO | :construction: | — | 战争策略优化算法（占位） |
| WCA | :construction: | — | 水循环算法（占位） |

## 架构

所有算法遵循**策略驱动**设计：

```
┌──────────────────────────────────────────────────┐
│  {算法}（如 GeneAlgorithm、PSO、GWO）            │  主运行器
│    ├─ policy: Abstract{Algorithm}Policy          │  策略接口
│    ├─ invoke(model, runningCallBack)             │  执行
│    └─ 迭代跟踪、优良解池                         │
├──────────────────────────────────────────────────┤
│  {Algorithm}Policy                               │  具体策略
│    ├─ HeuristicPolicy (base)                     │  迭代/时间限制
│    └─ AbstractHeuristicPolicy                    │  coerceIn、update、finished
├──────────────────────────────────────────────────┤
│  个体类型                                        │
│    ├─ GA: Chromosome / Population                │
│    ├─ PSO: Particle                              │
│    ├─ GWO: Wolf / Population                     │
│    ├─ SCA/SAA/MVO: SolutionWithFitness           │
│    └─ 均实现 Individual<ObjValue, V> 接口        │
└──────────────────────────────────────────────────┘
```

## 文件结构

```
heuristic/
├── ga/         遗传算法
│   ├── GA.kt           GeneAlgorithm、GAPolicy、AbstractGAPolicy
│   └── Population.kt   Chromosome、Population 类型别名
├── pso/        粒子群优化
│   ├── PSO.kt          ParticleSwarmOptimizationAlgorithm、PSOPolicy
│   └── Particle.kt     Particle 数据类
├── gwo/        灰狼优化器
│   ├── GWO.kt          GreyWolfOptimizer、GWOPolicy
│   └── Population.kt   Wolf、alpha/beta/delta 辅助函数
├── sca/        正弦余弦算法
│   └── SCA.kt          SineCosineAlgorithm、SCAPolicy、QLearningState
├── saa/        模拟退火
│   └── SAA.kt          SimulatedAnnealingAlgorithm、SAAPolicy
├── mvo/        多元宇宙优化器
│   └── MVO.kt          MultiVerseOptimizer、MVOPolicy
├── evo/        能量谷优化器（占位）
├── gco/        生发中心优化器（占位）
├── hca/        爬山算法（占位）
├── hs/         和声搜索算法（占位）
├── ns/         邻域搜索算法（占位）
├── soa/        海鸥优化算法（占位）
├── warso/      战争策略优化算法（占位）
└── wca/        水循环算法（占位）
```

## 通用特性

所有已实现的算法支持：

- **泛型值类型** — 适用于任何 `RealNumber<V>` + `NumberField<V>`
- **单目标和多目标** — 通过类型别名（`GA` / `MulObjGA`、`PSO` / `MulObjPSO` 等）
- **迭代和时间限制** — 可配置的停止准则
- **运行回调** — 每次迭代观察进度
- **内存压力处理** — 迭代间自动清理
- **优良解池** — 维护已发现最优解的有序列表

## 使用

### 依赖

```kotlin
implementation("io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-heuristic:1.1.0")
```

### 遗传算法

```kotlin
val ga = GA(
    population = listOf(
        PopulationBuilder(
            eliteAmount = UInt64(2),
            densityRange = ValueRange(UInt64(20), UInt64(50)),
            mutationRateRange = ValueRange(Flt64(0.01), Flt64(0.1)),
            parentAmountRange = ValueRange(UInt64(2), UInt64(4))
        )
    ),
    migrationPeriod = UInt64(10),
    policy = GAPolicy(...)
)
val bestSolutions = ga(model)
```

### 粒子群优化

```kotlin
val pso = PSO(
    particleAmount = UInt64(100),
    solutionAmount = UInt64.one,
    policy = PSOPolicy(w = Flt64(0.4), c1 = Flt64(2.0), c2 = Flt64(2.0))
)
val bestSolutions = pso(model)
```

### 模拟退火

```kotlin
val saa = SimulatedAnnealingAlgorithm(
    policy = SAAPolicy(
        initialTemperature = Flt64(100.0),
        finalTemperature = Flt64(1.0),
        temperatureGradiant = Flt64(0.98)
    )
)
val bestSolutions = saa(model)
```

## 依赖

| 依赖 | 作用域 | 说明 |
|------|--------|------|
| `ospf-kotlin-core` | compile | 启发式策略接口、Individual、Iteration |
