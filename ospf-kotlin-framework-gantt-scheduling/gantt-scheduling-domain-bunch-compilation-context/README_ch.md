# gantt-scheduling-domain-bunch-compilation-context

:us: [English](README.md) | :cn: 简体中文

甘特调度框架的列生成主问题模块。处理集合划分公式中的束选择、成本计算和覆盖约束。提供基于时间槽的编译和容量预求解，以实现高效的列生成。

## 关键类型

| 类型 | 描述 |
|------|------|
| `BunchCompilation` | 束级决策的编译上下文 |
| `BunchCompilationContext` | 束编译操作的共享上下文 |
| `BunchAggregation` | 束级决策变量的聚合 |
| `BunchSchedulingSolution` | 从编译的束变量中提取解 |
| `SlotBasedBunch` | 使用离散时间槽的束表示 |
| `SlotBasedBunchCompilation` | 基于时间槽的束公式编译上下文 |
| `SlotBasedCapacityResult` | 基于时间槽预求解的容量可行性结果 |
| `TaskTime` | 束编译中的任务时间 |
| `TaskReverse` | 从任务到其束分配的反向映射 |

## 服务

| 服务 | 描述 |
|------|------|
| `BunchSolutionAnalyzer` | 分析束级解的质量和结构 |
| `TaskSolutionAnalyzer` | 分析束解中的任务级结果 |
| `SlotBasedBunchCompilationContext` | 构建基于时间槽的束编译模型 |
| `SlotBasedCapacityPreSolver` | 在完整编译前预求解容量可行性 |

## 限制（约束与目标）

| 限制 | 描述 |
|------|------|
| `BunchCostMinimization` | 最小化所选束的总成本 |

## 依赖

- [gantt-scheduling-infrastructure](../gantt-scheduling-infrastructure/) — 时间原语
- [gantt-scheduling-domain-task-context](../gantt-scheduling-domain-task-context/) — 任务领域模型
