# Payload Maximization — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Maximizes the total payload (cargo weight) loaded onto the aircraft within all safety and structural constraints.

### Dependent Contexts

1. Aircraft
2. Stowage

## Directory Structure

```
payload_maximization/
├── service/        # Domain services
│   ├── limits/
│   │   └── MaxPayloadLimit.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # Aggregate root
└── PayloadMaximizationContext.kt  # Context entry point
```

## Core Concepts

- **Payload**：Total cargo weight to be maximized.

## Constraints

- **Max Payload Limit**：Payload must not exceed aircraft maximum payload capacity.

## Objective Function

$$
\max \sum_{i \in I} \sum_{j \in J} weight_i \cdot x_{ij}
$$

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage

**Downstream**: None (terminal objective function context)
