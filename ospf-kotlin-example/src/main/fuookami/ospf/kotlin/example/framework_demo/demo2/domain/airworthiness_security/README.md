# Airworthiness Security — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Enforces airworthiness and safety constraints including linear/surface density limits, zone load weight limits, cumulative load weight limits, CLIM limits, envelope constraints, and payload limits.

### Dependent Contexts

1. Aircraft
2. Stowage
3. Mean Aerodynamic Chord (MAC)

## Directory Structure

```
airworthiness_security/
├── model/          # Domain models
│   ├── LinearDensity.kt           # Linear density
│   ├── SurfaceDensity.kt          # Surface density
│   ├── MaxZoneLoadWeight.kt       # Max zone load weight
│   ├── MaxCumulativeLoadWeight.kt # Max cumulative load weight
│   ├── MaxUnsymmetricalLinearDensity.kt  # Max unsymmetrical linear density
│   ├── MaxCLIM.kt                 # Max CLIM
│   ├── MinLowPayload.kt          # Min low payload
│   ├── Envelope.kt                # Envelope
│   └── model/                     # Other models
├── service/        # Domain services
│   ├── limits/                    # Constraint definitions
│   │   ├── LinearDensityLimit.kt
│   │   ├── SurfaceDensityLimit.kt
│   │   ├── ZoneLoadWeightLimit.kt
│   │   ├── CumulativeLoadWeightLimit.kt
│   │   ├── CLIMLimit.kt
│   │   ├── EnvelopeLimit.kt
│   │   ├── PayloadLimit.kt
│   │   ├── LowPayloadLimit.kt
│   │   ├── TotalWeightLimit.kt
│   │   ├── BallastWeightLimit.kt
│   │   ├── HorizontalStabilizerLimit.kt
│   │   ├── UnsymmetricalLinearDensityLimit.kt
│   │   └── AdjacentGapLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # Aggregate root
└── AirworthinessSecurityContext.kt  # Context entry point
```

## Core Concepts

- **Linear Density**：Linear weight density per fuselage zone with upper/lower limits.
- **Surface Density**：Surface weight density per zone with upper/lower limits.
- **Max Zone Load Weight**：Maximum allowable load weight per fuselage zone.
- **Max Cumulative Load Weight**：Maximum cumulative load weight from nose/tail.
- **Max Unsymmetrical Linear Density**：Maximum allowable unsymmetrical linear density for wide-body aircraft.
- **Max CLIM**：Maximum CLIM limits for wide-body aircraft.
- **Min Low Payload**：Minimum payload required in the lower deck.
- **Envelope**：Weight-CG envelope constraints per flight phase.
- **Ballast Weight**：Minimum ballast weight requirement for balance.
- **Adjacent Gap**：Maximum allowable weight gap between adjacent positions.

## Constraints

- **Linear Density Limit**：Linear density per zone must be within limits.
- **Surface Density Limit**：Surface density per zone must be within limits.
- **Zone Load Weight Limit**：Zone load weight must not exceed maximum.
- **Cumulative Load Weight Limit**：Cumulative load weight from nose/tail must not exceed maximum.
- **CLIM Limit**：CLIM must be within allowable range.
- **Envelope Limit**：Weight-CG combination must be within the envelope.
- **Payload Limit**：Payload must be within planned and maximum bounds.
- **Low Payload Limit**：Low deck payload must meet minimum requirement.
- **Total Weight Limit**：Total weight per flight phase must not exceed maximum.
- **Ballast Weight Limit**：Ballast weight must meet minimum requirement.
- **Horizontal Stabilizer Limit**：Horizontal stabilizer position must be within limits.
- **Unsymmetrical Linear Density Limit**：Unsymmetrical linear density must not exceed maximum.
- **Adjacent Gap Limit**：Weight gap between adjacent positions must not exceed maximum.

## Objective Function

This context does not define an objective function; it only provides constraints.

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage, MAC

**Downstream**: None (terminal constraint context)
