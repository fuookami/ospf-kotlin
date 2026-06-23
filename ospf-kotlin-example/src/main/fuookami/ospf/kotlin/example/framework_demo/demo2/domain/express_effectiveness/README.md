# Express Effectiveness — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Manages express effectiveness constraints that optimize item priority ordering — ensuring high-priority cargo is loaded preferentially.

### Dependent Contexts

1. Aircraft
2. Stowage

## Directory Structure

```
express_effectiveness/
├── model/          # Domain models
│   ├── AbsoluteOrder.kt     # Absolute order
│   └── RelativeOrder.kt     # Relative order
├── service/        # Domain services
│   ├── limits/
│   │   ├── MustShipLimit.kt
│   │   ├── ItemPriorityLimit.kt
│   │   └── ItemPriorityReverseLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # Aggregate root
└── ExpressEffectivenessContext.kt  # Context entry point
```

## Core Concepts

- **Absolute Order**：Defines absolute priority ordering for predistribution mode.
- **Relative Order**：Defines relative priority ordering for full-load mode.
- **Must-Ship Items**：Items that must be shipped regardless of priority.

## Constraints

- **Must-Ship Limit**：Must-ship items must be loaded.
- **Item Priority Limit**：Higher priority items should be loaded before lower priority items.
- **Item Priority Reverse Limit**：Penalty for loading items in reverse priority order.

## Objective Function

Minimize priority violation cost.

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage

**Downstream**: None (terminal constraint context)
