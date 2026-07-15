# gantt-scheduling-domain-task-generation-context

:us: English | :cn: [简体中文](README_ch.md)

Placeholder module for task-level generation in the gantt-scheduling framework. Task generation functionality is currently integrated with bunch-generation; this module exists to preserve the architectural layer and may be populated in a future refactor.

## Key Types

None — this module has no main source files at present.

## Dependencies

- [gantt-scheduling-infrastructure](../gantt-scheduling-infrastructure/) — time primitives
- [gantt-scheduling-domain-task-context](../gantt-scheduling-domain-task-context/) — task domain model

## Note

Task generation logic currently resides in [gantt-scheduling-domain-bunch-generation-context](../gantt-scheduling-domain-bunch-generation-context/). This module is reserved for future separation of task-level generation concerns.
