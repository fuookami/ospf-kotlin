# Soft Security — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Manages soft safety constraints including empty loading division, main deck door empty preference, and ballast weight advice — constraints that improve safety but can be relaxed if needed.

### Dependent Contexts

1. Aircraft
2. Stowage

## Directory Structure

```
soft_security/
├── model/          # Domain models
│   └── DivideEmptyLoading.kt  # Divide empty loading
├── service/        # Domain services
│   ├── limits/
│   │   ├── EmptyHatedLimit.kt
│   │   ├── MainDeckDoorEmptyLimit.kt
│   │   ├── DivideEmptyLoadingLimit.kt
│   │   └── AdviceBallastWeightLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # Aggregate root
└── SoftSecurityContext.kt  # Context entry point
```

## Core Concepts

- **Divide Empty Loading**：Ensures empty positions are divided rather than clustered for structural safety.

## Constraints

- **Empty Hated Limit**：Penalty for empty positions (soft preference to fill positions).
- **Main Deck Door Empty Limit**：Main deck door positions should preferentially be empty (B757/B767).
- **Divide Empty Loading Limit**：Empty positions should be distributed across the aircraft.
- **Advice Ballast Weight Limit**：Ballast weight should meet advisory minimum.

## Objective Function

Minimize soft security violation penalties.

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage

**Downstream**: None (terminal constraint context)
