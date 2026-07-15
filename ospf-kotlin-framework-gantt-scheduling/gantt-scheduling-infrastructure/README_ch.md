# gantt-scheduling-infrastructure

:us: [English](README.md) | :cn: 简体中文

甘特调度领域框架的时间原语模块。提供用于表示时间区间、时间窗口、时间槽、持续时间和工作日历的基础类型，被所有甘特调度子模块依赖。

## 关键类型

| 类型 | 描述 |
|------|------|
| `TimeRange` | 半开时间区间 [start, end)，支持集合运算 |
| `TimeWindow<V>` | 泛型时间窗口，支持离散化和舍入 |
| `TimeSlot` | 调度时间范围内的离散时间槽 |
| `DurationRange` | 任务可能持续时间的范围 |
| `WorkingCalendar` | 定义工作日与非工作日的日历 |
| `LocalDateOffset` | 遵循工作日历的日期偏移计算 |
| `GanttRenderTaskDTO` | 甘特图渲染的数据传输对象 |

## 依赖

无 — 本模块为甘特调度框架的基础模块。
