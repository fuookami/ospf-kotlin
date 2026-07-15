# Recommended Weight Equalization — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Manages recommended weight equalization — ensuring cargo weight is distributed evenly across positions according to priority appointments.

### Dependent Contexts

1. Aircraft
2. Stowage

## Directory Structure

```
recommended_weight_equalization/
├── model/          # Domain models
│   └── PriorityAppointment.kt  # Priority appointment
├── service/        # Domain services
│   ├── limits/
│   │   ├── ItemOrderLimit.kt
│   │   ├── PriorityAppointmentLimit.kt
│   │   └── RecommendedWeightEqualizationLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # Aggregate root
└── RecommendedWeightEqualizationContext.kt  # Context entry point
```

## Core Concepts

- **Priority Appointment**：Priority-based appointment of items to positions with weight equalization.

## Constraints

- **Item Order Limit**：Items must be loaded in priority order.
- **Priority Appointment Limit**：Priority appointments must be respected.
- **Recommended Weight Equalization Limit**：Load weight should equalize across positions.

## Objective Function

Minimize weight deviation from recommended values.

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage

**Downstream**: None (terminal constraint context)
