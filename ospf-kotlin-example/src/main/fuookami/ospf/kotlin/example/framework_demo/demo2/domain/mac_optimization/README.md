# MAC Optimization — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Manages MAC optimization including longitudinal balance (MAC range constraints) and lateral balance constraints for aircraft weight distribution.

### Dependent Contexts

1. Aircraft
2. Stowage
3. Mean Aerodynamic Chord (MAC)

## Directory Structure

```
mac_optimization/
├── model/          # Domain models
│   ├── MACRange.kt           # MAC range
│   ├── LongitudinalBalance.kt # Longitudinal balance
│   └── LateralBalance.kt     # Lateral balance
├── service/        # Domain services
│   ├── limits/
│   │   ├── LongitudinalBalanceLimit.kt
│   │   ├── LateralBalanceLimit.kt
│   │   ├── HorizontalStabilizerLimit.kt
│   │   └── AggregationInitializer.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # Aggregate root
└── MacOptimizationContext.kt  # Context entry point
```

## Core Concepts

- **MAC Range**：Defines allowable MAC percentage range based on total weight.
- **Longitudinal Balance**：Longitudinal balance constraints ensuring MAC is within allowable range per flight phase.
- **Lateral Balance**：Lateral balance constraints for wide-body aircraft ensuring symmetrical loading.

## Constraints

- **Longitudinal Balance Limit**：MAC percentage must be within allowable range per flight phase.
- **Lateral Balance Limit**：Lateral torque must be within allowable range (wide-body only).
- **Horizontal Stabilizer Limit**：Horizontal stabilizer position must match MAC.

## Objective Function

Minimize MAC deviation from target range.

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage, MAC

**Downstream**: None (terminal constraint context)
