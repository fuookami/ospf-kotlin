# Mean Aerodynamic Chord (MAC) — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Computes Mean Aerodynamic Chord (MAC) percentage, longitudinal/lateral torque, CLIM, and index for each flight phase from aircraft and stowage data.

### Dependent Contexts

1. Aircraft
2. Stowage

## Directory Structure

```
mac/
├── model/          # Domain models
│   ├── MAC.kt              # MAC computation
│   ├── Torque.kt           # Torque computation
│   └── HorizontalStabilizer.kt  # Horizontal stabilizer
├── service/        # Domain services
│   └── AggregationInitializer.kt
├── Aggregation.kt  # Aggregate root
└── MacContext.kt   # Context entry point
```

## Core Concepts

- **Torque**：Computes longitudinal torque, lateral torque, CLIM, and index per flight phase from load, fuel, fuselage, and formula data.
- **MAC**：Computes MAC percentage as a linear intermediate symbol from torque index and total weight.
- **Horizontal Stabilizer**：Horizontal stabilizer position and limits for balance computation.

## Constraints

This context does not define constraints; it only provides intermediate computation values.

## Objective Function

This context does not define an objective function.

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage

**Downstream**: Airworthiness Security, MAC Optimization
