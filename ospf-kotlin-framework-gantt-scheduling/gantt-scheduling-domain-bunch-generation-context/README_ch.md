# gantt-scheduling-domain-bunch-generation-context

:us: [English](README.md) | :cn: 简体中文

列生成的定价子问题 — 使用带资源约束的标签设定最短路径算法，生成具有负检验数的新束（路径）。

## 关键类型

| 类型 | 描述 |
|------|------|
| `Node` | 密封类，表示图顶点：`RootNode`、`EndNode`、`TaskNode` |
| `Edge` | 连接图中节点的有向边 |
| `Graph` | 用于路径搜索的有向图结构 |
| `Label` | 动态规划 / 最短路径的标签，跟踪资源状态 |
| `SlotBasedBunchGenerator` | 基于时间槽可用性生成束 |
| `PlannedTaskBunchGenerator` | 从已计划任务生成束 |
| `UnplannedTaskBunchGenerator` | 从未计划任务生成束 |

## 依赖

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`
