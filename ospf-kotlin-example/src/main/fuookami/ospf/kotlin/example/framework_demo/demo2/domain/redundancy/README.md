# Redundancy — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Manages redundancy and experimental longitudinal balance constraints for weight distribution analysis and safety margins.

### Dependent Contexts

1. Aircraft
2. Stowage

## Directory Structure

```
redundancy/
├── model/          # Domain models
│   ├── Redundancy.kt                      # Redundancy model
│   └── ExperimentalLongitudinalBalance.kt # Experimental longitudinal balance
├── service/        # Domain services
│   ├── limits/
│   │   ├── RedundancyLimit.kt
│   │   └── ExperimentalLongitudinalBalanceLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # Aggregate root
└── RedundancyContext.kt  # Context entry point
```

## Core Concepts

- **Redundancy**：Redundancy model for weight distribution safety margins.
- **Experimental Longitudinal Balance**：Experimental longitudinal balance model based on redundancy computations.

## Constraints

- **Redundancy Limit**：Redundancy must be within acceptable bounds.
- **Experimental Longitudinal Balance Limit**：Experimental longitudinal balance must be within bounds.

## Objective Function

This context does not define an objective function; it only provides constraints.

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage

**Downstream**: None (terminal constraint context)
