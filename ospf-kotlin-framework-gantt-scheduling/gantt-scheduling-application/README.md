# gantt-scheduling-application

:us: English | :cn: [简体中文](README_ch.md)

Solve orchestration layer — provides business entry points (APS, MPS, LSP) and algorithm services (branch-and-price, column generation) that coordinate all domain sub-modules to produce scheduling solutions.

## Key Types

| Type | Description |
|------|-------------|
| `APS` | Advanced Planning & Scheduling — business entry point |
| `MPS` | Master Production Scheduling — business entry point |
| `LSP` | Lot Scheduling & Planning — business entry point |
| `BranchAndPriceAlgorithm` | Branch-and-price solver (bunch and task variants) |
| `ColumnGenerationAlgorithm` | Column generation solver (bunch and task variants) |
| `Iteration` | Single iteration state in the solving process |
| `IterationSnapshot` | Snapshot of iteration data for analysis |

### Slot-based branch-and-price

`BranchAndPriceAlgorithm` keeps the existing batch pricing entry point and adds an explicit executor-slot route through `Policy.bunchGeneratorByExecutorAndSlot` plus the constructor `slots` calendar. When configured, each pricing round invokes the generator once per visible `(executor, slot)` pair and forwards fixed, kept, and hidden sets. This route is intended to be used with `SlotBasedBunchCompilation` and its executor-slot selection constraints; omitting it preserves the legacy global/local pricing behavior.

## Dependencies

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`
- `gantt-scheduling-domain-bunch-generation-context`
- `gantt-scheduling-domain-capacity-scheduling-context`
- `gantt-scheduling-domain-resource-context`
- `gantt-scheduling-domain-produce-context`
