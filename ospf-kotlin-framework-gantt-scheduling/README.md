# ospf-kotlin-framework-gantt-scheduling

:us: English | :cn: [简体中文](README_ch.md)

`ospf-kotlin-framework-gantt-scheduling` is a reusable Gantt scheduling optimization framework based on column generation and branch-and-price algorithms. It provides generic scheduling kernels for Advanced Planning and Scheduling (APS), Master Production Scheduling (MPS), and Lot Scheduling Planning (LSP). Downstream request DTOs, formula languages, project runtime parameters, tenant context, heartbeat logic, and solver plugin selection are left to business adapters.

## Scope

The framework covers the following domain capabilities:

- **Task modeling**: task definition, executor assignment, time scheduling, task status, assignment policies
- **Task compilation**: task-level decision variables, constraints, and objectives for MILP
- **Task bunch (route) compilation**: column-generation master problem — bunch selection, cost, and coverage constraints
- **Task bunch generation**: pricing problem — label-setting shortest path with resource constraints
- **Capacity scheduling**: time-slot-based capacity allocation for executors
- **Resource management**: consumable resource capacity constraints with slack variables
- **Produce/consumption tracking**: material production and consumption with demand satisfaction
- **Time infrastructure**: time ranges, time windows, working calendars, duration ranges

Downstream business concepts such as task dependency DAGs beyond step graphs, complex shift calendars, and custom cost formulas are not modeled here until they first become generic domain entities.

## Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│                     Application Layer                     │
│          APS · MPS · LSP · BranchAndPriceAlgorithm       │
└──────────┬───────────────────────────────────┬───────────┘
           │                                   │
┌──────────▼──────────┐         ┌──────────────▼──────────────┐
│  Bunch Compilation   │         │    Task Compilation        │
│  (Master Problem)    │         │  (Task-level MILP)         │
│  · BunchAggregation  │         │  · TaskCompilation         │
│  · BunchSolution     │         │  · TaskAggregation         │
└──────────┬──────────┘         └──────────────┬─────────────┘
           │                                   │
┌──────────▼──────────┐         ┌──────────────▼─────────────┐
│  Bunch Generation    │         │  Capacity Scheduling       │
│  (Pricing Problem)   │         │  · CapacityColumn          │
│  · Graph · Label     │         │  · CapacityCompilation     │
│  · SlotBasedBunchGen │         │  · IterativeCapacityComp   │
└──────────┬──────────┘         └──────────────┬─────────────┘
           │                                   │
┌──────────▼───────────────────────────────────▼─────────────┐
│              Domain Foundation Layer                        │
│  Task Context · Resource Context · Produce Context         │
└──────────────────────┬────────────────────────────────────┘
                       │
┌──────────────────────▼────────────────────────────────────┐
│              Infrastructure Layer                           │
│  TimeRange · TimeWindow · TimeSlot · DurationRange         │
│  WorkingCalendar · LocalDateOffset                         │
└───────────────────────────────────────────────────────────┘
```

## Core Concepts

### Task

A **Task** represents a schedulable work unit. Key properties:

- **Executor**: the resource/machine that performs the task
- **Scheduled time**: the planned start time
- **Duration**: the task execution duration
- **Time window**: earliest start / latest end constraints
- **Status**: `NotAdvance`, `NotDelay`, `NotCancel`, `NotCancelPreferred`, `NotExecutorChange`, `Parallelable`, `Divisible`

Unplanned tasks have no executor or time assignment yet. Planned tasks derive scheduling from a task plan with optional assignment policy.

### Task Bunch (Route)

A **TaskBunch** is an ordered group of tasks assigned to the same executor — analogous to a "route" in vehicle routing problems. Bunches are the columns in column generation: each bunch represents one feasible schedule for one executor.

### Column Generation

The framework uses a classic two-phase column generation architecture:

1. **Master Problem (RMP)**: Select bunches to cover all tasks at minimum cost, subject to task coverage, executor capacity, and resource constraints
2. **Pricing Problem**: Generate new bunches with negative reduced cost using a label-setting algorithm (shortest path with resource constraints on a directed graph)

### Branch-and-Price

The `BranchAndPriceAlgorithm` orchestrates the full solve flow:

1. Register initial bunches (columns)
2. Solve LP relaxation of the restricted master problem
3. Extract shadow prices from dual solution
4. Generate new columns via global and local pricing
5. Fix high-value columns
6. Solve final MILP for integer solution
7. Iterate until convergence or time limit

### Capacity Scheduling

Capacity scheduling handles time-slot-based capacity allocation. Executors are discretized into time slots with capacity constraints, enabling fine-grained resource allocation over the planning horizon.

### Resource & Produce

- **Resources**: consumable/limited capacities (materials, energy, storage) with slack variables for soft constraint handling (`overQuantity` / `lessQuantity`)
- **Produce/Consumption**: tracks material production and consumption, with demand satisfaction constraints and slack variables

## Sub-Modules

| Sub-Module | Responsibility |
|-----------|---------------|
| `gantt-scheduling-infrastructure` | Time primitives: TimeRange, TimeWindow, TimeSlot, DurationRange, WorkingCalendar |
| `gantt-scheduling-domain-task-context` | Core task model: Task, TaskBunch, TaskPlan, Executor, AssignmentPolicy |
| `gantt-scheduling-domain-task-compilation-context` | Task-level MILP: Compilation, TaskTime, Switch, Makespan, constraints & objectives |
| `gantt-scheduling-domain-task-generation-context` | Task generation (placeholder, integrated with bunch-generation) |
| `gantt-scheduling-domain-bunch-compilation-context` | Column-generation master: BunchCompilation, BunchAggregation, SlotBasedBunch |
| `gantt-scheduling-domain-bunch-generation-context` | Pricing problem: Graph, Label, SlotBasedBunchGenerator |
| `gantt-scheduling-domain-capacity-scheduling-context` | Capacity scheduling: Capacity, CapacityColumn, CapacityCompilation |
| `gantt-scheduling-domain-resource-context` | Resource constraints: ExecutionResource, StorageResource, ConnectionResource |
| `gantt-scheduling-domain-produce-context` | Produce/consumption: Produce, Consumption, ProductionTask |
| `gantt-scheduling-application` | Solve orchestration: APS, MPS, LSP, BranchAndPriceAlgorithm |

## Public API

### Business Entry Points

- **`APS`** — Advanced Planning and Scheduling: strategic-level planning across the full horizon
- **`MPS`** — Master Production Scheduling: production quantity and timing decisions
- **`LSP`** — Lot Scheduling Planning: detailed sequencing and scheduling of production lots

### Algorithm Services

- **`BranchAndPriceAlgorithm`** (bunch / task variants): full branch-and-price with column generation, column fixing, and final MILP
- **`ColumnGenerationAlgorithm`** (bunch / task variants): column generation only (LP relaxation + final MILP)

### Configuration

`BranchAndPriceAlgorithm` accepts configuration including:

- `badReducedAmount`: threshold for identifying columns with unfavorable reduced cost
- `maximumColumnAmount`: maximum number of columns in the model
- `minimumColumnAmountPerExecutor`: minimum columns per executor
- `timeLimit`: algorithm time limit

### Iteration Tracking

`Iteration` tracks convergence state: iteration count, LP/IP objective values, optimal rate, lower bound, slow improvement detection for early termination. `IterationSnapshot` provides a type-erased snapshot of iteration state.

## Modeling Extensions

The framework follows a context / aggregation / pipeline architecture:

- **Context**: the modeling entry point exposed to the application layer; initializes domain aggregation and registers to the model
- **Aggregation**: composes multiple model components within a domain and coordinates their registration order
- **Pipeline / Limit**: handles a single constraint family, objective family, penalty term, or shadow price logic

Downstream business constraints should be injected as extra context / pipeline extensions rather than modifying framework core code.

## Generic Numeric Boundaries

The framework uses `V : RealNumber<V>` as the generic numeric abstraction in public APIs. Internal solver operations use `Flt64`. Numeric type conversions are concentrated at context registration and result extraction boundaries.

Domain quantities (time, duration, capacity, resource amount, production amount) use `Quantity<V>` or explicit physical quantity types where applicable. `Flt64`, `Double`, and bare untyped values are restricted to solver adapter, registration, and extraction internals.

## Outputs

### Bunch-Level Solution

`BunchSolution` / `BunchSchedulingSolution` contains:

- Selected bunches (routes) with task assignments
- Canceled tasks
- Executor assignments and idle time
- Total cost

### Task-Level Solution

`TaskSolution` contains:

- Assigned tasks with executor and scheduled time
- Canceled tasks
- Task delays and advances

### Capacity Scheduling Solution

`CapacitySchedulingSolution` contains:

- Capacity columns (time-slot allocations)
- Production actions per time slot

## Local Validation

```powershell
mvn -B -ntp "-Dkotlin.compiler.execution.strategy=in-process" "-Dkotlin.daemon.enabled=false" -pl .\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-infrastructure,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-task-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-task-compilation-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-bunch-compilation-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-bunch-generation-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-capacity-scheduling-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-resource-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-domain-produce-context,.\ospf-kotlin-framework-gantt-scheduling\gantt-scheduling-application "-Dsurefire.failIfNoSpecifiedTests=false" test
```
