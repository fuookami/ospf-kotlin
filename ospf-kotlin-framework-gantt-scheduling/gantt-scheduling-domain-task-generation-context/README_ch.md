# gantt-scheduling-domain-task-generation-context

:us: [English](README.md) | :cn: 简体中文

甘特调度框架中任务级生成的占位模块。任务生成功能当前已集成到束生成模块中；本模块保留架构层次，可能在未来的重构中填充实现。

## 关键类型

无 — 本模块目前没有主要源文件。

## 依赖

- [gantt-scheduling-infrastructure](../gantt-scheduling-infrastructure/) — 时间原语
- [gantt-scheduling-domain-task-context](../gantt-scheduling-domain-task-context/) — 任务领域模型

## 说明

任务生成逻辑当前位于 [gantt-scheduling-domain-bunch-generation-context](../gantt-scheduling-domain-bunch-generation-context/)。本模块为未来分离任务级生成关注点而保留。
