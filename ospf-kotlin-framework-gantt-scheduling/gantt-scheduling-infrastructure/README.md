# gantt-scheduling-infrastructure

:us: English | :cn: [简体中文](README_ch.md)

Time primitives for the gantt-scheduling domain framework. Provides foundational types for representing time intervals, windows, slots, durations, and working calendars used across all gantt-scheduling sub-modules.

## Key Types

| Type | Description |
|------|-------------|
| `TimeRange` | Half-open time interval [start, end) with set operations |
| `TimeWindow<V>` | Generic time window with discretization and rounding |
| `TimeSlot` | Discrete time slot within a scheduling horizon |
| `DurationRange` | Range of possible durations for a task |
| `WorkingCalendar` | Calendar defining working vs. non-working periods |
| `LocalDateOffset` | Date offset calculation respecting working calendars |
| `GanttRenderTaskDTO` | Data transfer object for gantt chart rendering |

## Dependencies

None — this is the base module of the gantt-scheduling framework.
