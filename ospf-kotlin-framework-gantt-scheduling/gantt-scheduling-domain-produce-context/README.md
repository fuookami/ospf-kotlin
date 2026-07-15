# gantt-scheduling-domain-produce-context

:us: English | :cn: [简体中文](README_ch.md)

Material production and consumption tracking with demand satisfaction — models produce/consumption flows, tracks quantities across scheduling contexts, and enforces production and consumption limits.

## Key Types

| Type | Description |
|------|-------------|
| `Produce` | Material production definition |
| `Consumption` | Material consumption definition |
| `ProduceSlack` | Slack variable for produce constraint relaxation |
| `ProduceSolverValue` | Solver-level value for produce variables |
| `ProductionTask` | Task that produces or consumes material |
| `BunchCapacitySchedulingProduce` | Produce tracking in bunch capacity scheduling |
| `CapacityActionProduce` | Produce associated with a capacity action |
| `CapacitySchedulingProduce` | Produce tracking in capacity scheduling |
| `PlanCapacitySchedulingProduce` | Produce tracking in plan-level capacity scheduling |
| `ProduceQuantityConstraint` | Limit: enforce produce quantity bounds |
| `ProduceQuantityMaximization` | Limit: maximize total production |
| `ProduceQuantityMinimization` | Limit: minimize total production |
| `ProduceOverQuantityMinimization` | Limit: minimize over-production |
| `ProduceLessQuantityMinimization` | Limit: minimize under-production |
| `ConsumptionQuantityConstraint` | Limit: enforce consumption quantity bounds |
| `ConsumptionQuantityMaximization` | Limit: maximize total consumption |
| `ConsumptionQuantityMinimization` | Limit: minimize total consumption |
| `ConsumptionOverQuantityMinimization` | Limit: minimize over-consumption |
| `ConsumptionLessQuantityMinimization` | Limit: minimize under-consumption |

## Dependencies

- `gantt-scheduling-infrastructure`
- `gantt-scheduling-domain-task-context`
