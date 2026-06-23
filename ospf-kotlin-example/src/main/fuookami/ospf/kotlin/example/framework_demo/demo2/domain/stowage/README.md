# Stowage — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Manages cargo stowage assignment decisions — which items go to which positions — including load weight computation, payload calculation, total weight, and max load weight.

### Dependent Contexts

1. Aircraft

## Directory Structure

```
stowage/
├── model/          # Domain models (entities, value objects)
│   ├── Item.kt              # Cargo item
│   ├── Position.kt          # Stowage position
│   ├── Stowage.kt           # Stowage assignment decision
│   ├── Load.kt              # Load weight/amount
│   ├── Payload.kt           # Payload
│   ├── TotalWeight.kt       # Total weight
│   ├── MaxLoadWeight.kt     # Max load weight
│   ├── Ballast.kt           # Ballast
│   ├── Flight.kt            # Flight
│   ├── Cargo.kt             # Cargo type
│   ├── Appointment.kt       # Appointment
│   ├── Solution.kt          # Solution
│   └── BiologicalLimit.kt   # Biological limit
├── service/        # Domain services
│   ├── limits/              # Constraint definitions
│   │   ├── ItemAssignmentLimit.kt
│   │   ├── LoadAmountLimit.kt
│   │   ├── LoadWeightLimit.kt
│   │   ├── AppointmentLimit.kt
│   │   ├── StowageLimit.kt
│   │   ├── ELDAdjacentLimit.kt
│   │   ├── LoadingOrderLimit.kt
│   │   ├── BiologicalAdjacentLimit.kt
│   │   ├── AOGMATBulkConflictLimit.kt
│   │   ├── PredicateLoadWeightLimit.kt
│   │   ├── RecommendLoadWeightLimit.kt
│   │   ├── EmptyForbiddenLimit.kt
│   │   ├── ItemAdjustmentLimit.kt
│   │   ├── NormalBulkDestinationAssignmentLimit.kt
│   │   └── BiologicalBulkConflictLimit.kt
│   ├── AggregationInitializer.kt
│   ├── PipelineListGenerator.kt
│   └── SolutionAnalyzer.kt
├── Aggregation.kt  # Aggregate root
└── StowageContext.kt  # Context entry point
```

## Core Concepts

- **Item**：A cargo item with destination, weight, ULD, location tags, cargo type, priority, and status.
- **Position**：Stowage position with max load amount (MLA), predicate load weight (PLW), recommended load weight, and status.
- **Stowage**：Decision model: binary assignment variables x[i,j] and adjustment variables u[i,j] for item-position pairs.
- **Load**：Load weight and load amount per position, including predicate/recommended weight slack, torque, CLIM, and index computations.
- **Payload**：Planned/max/computed payload with stowage mode-dependent registration.
- **Total Weight**：Total aircraft weight per flight phase (takeoff/landing/zero-fuel) with computed values.
- **Max Load Weight**：Maximum allowable load weight per position based on total weight constraints.
- **Ballast**：Ballast weight for aircraft requiring balance correction (B757/B767).
- **Flight**：Flight information with departure/arrival airports and flight number.
- **Appointment**：Pre-assigned item-to-position appointments.

## Constraints

- **Item Assignment Limit**：Each item must be assigned to exactly one position (or zero if optional).
- **Load Amount Limit**：Load amount per position must not exceed MLA.
- **Load Weight Limit**：Load weight per position must not exceed max load weight.
- **Appointment Limit**：Pre-assigned appointments must be respected.
- **Stowage Limit**：Items can only be assigned to compatible positions.
- **ELD Adjacent Limit**：Empty Loading Device adjacency constraints.
- **Loading Order Limit**：Loading order must be respected for position accessibility.
- **Biological Adjacent Limit**：Biological cargo adjacency restrictions.
- **AOG/MAT Bulk Conflict Limit**：Aircraft-on-ground and mail/AT bulk conflict constraints.

## Objective Function

This context does not define an independent objective function; it only provides constraints.

## Relationships with Other Contexts

**Upstream**: Aircraft

**Downstream**: MAC, Airworthiness Security, Express Effectiveness, Loading Effectiveness, MAC Optimization, Payload Maximization, Recommended Weight Equalization, Redundancy, Soft Security
