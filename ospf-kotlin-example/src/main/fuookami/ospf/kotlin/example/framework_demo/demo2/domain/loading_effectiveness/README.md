# Loading Effectiveness — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Manages loading effectiveness constraints for operational efficiency — including trailer loading, sequential loading, transfer adjacency, and source/destination grouping.

### Dependent Contexts

1. Aircraft
2. Stowage

## Directory Structure

```
loading_effectiveness/
├── model/          # Domain models
│   ├── AdviceLoading.kt           # Advice loading
│   ├── TransferAdjacentLoading.kt # Transfer adjacent loading
│   ├── SequentialLoading.kt       # Sequential loading
│   ├── TrailerLoading.kt          # Trailer loading
│   └── Trailer.kt                 # Trailer
├── service/        # Domain services
│   ├── limits/
│   │   ├── AdviceLoadAmountLimit.kt
│   │   ├── AdviceLoadWeightLimit.kt
│   │   ├── SameSourceAdjacentLimit.kt
│   │   ├── SameDestinationAdjacent.kt
│   │   ├── ItemOrderReverseLimit.kt
│   │   ├── PriorityOrderLimit.kt
│   │   ├── TrailerChangeLimit.kt
│   │   ├── TrailerCirclingLimit.kt
│   │   ├── ItemAheadLoadLimit.kt
│   │   ├── ItemReserveLimit.kt
│   │   ├── SourceEarlyLimit.kt
│   │   └── ItemReweighNeededLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # Aggregate root
└── LoadingEffectivenessContext.kt  # Context entry point
```

## Core Concepts

- **Advice Loading**：Suggested loading amounts and weights per position for predistribution mode.
- **Transfer Adjacent Loading**：Same-source/same-destination adjacency constraints for transfer efficiency.
- **Sequential Loading**：Sequential loading constraints based on position ordering.
- **Trailer Loading**：Trailer change and circling constraints for full-load mode.

## Constraints

- **Advice Load Amount Limit**：Load amount per position should match advice.
- **Advice Load Weight Limit**：Load weight per position should match advice.
- **Same Source Adjacent Limit**：Same-source items should be loaded in adjacent positions.
- **Same Destination Adjacent Limit**：Same-destination items should be loaded in adjacent positions.
- **Item Order Reverse Limit**：Penalty for loading items in reverse order.
- **Priority Order Limit**：Priority-based loading order constraints.
- **Trailer Change Limit**：Minimize trailer changes during loading.
- **Trailer Circling Limit**：Minimize trailer circling during loading.
- **Item Ahead Load Limit**：Constraints for loading items ahead of schedule.
- **Item Reserve Limit**：Reserved items must not be loaded.
- **Source Early Limit**：Early source loading constraints.
- **Item Reweigh Needed Limit**：Items needing reweigh must be in accessible positions.

## Objective Function

Minimize loading operational cost.

## Relationships with Other Contexts

**Upstream**: Aircraft, Stowage

**Downstream**: None (terminal constraint context)
