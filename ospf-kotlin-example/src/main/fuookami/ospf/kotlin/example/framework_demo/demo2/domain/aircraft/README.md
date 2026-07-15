# Aircraft — README

:us: English | :cn: [简体中文](README_ch.md)

## Overview

Manages aircraft configuration data including model classification, fuselage parameters, fuel constants, deck layout, cargo positions, and neighbour relationships.

### Dependent Contexts

No upstream dependencies.

## Directory Structure

```
aircraft/
├── model/          # Domain models (entities, value objects)
│   ├── AircraftModel.kt    # Aircraft model and type enumeration
│   ├── Deck.kt              # Decks and hatch doors
│   ├── Position.kt          # Cargo position coordinates and shape
│   ├── Fuselage.kt          # Fuselage properties
│   ├── Fuel.kt              # Fuel constants
│   ├── Formula.kt           # Calculation formulas
│   ├── ULD.kt               # Unit Load Device
│   ├── HatchDoor.kt         # Hatch doors
│   ├── Neighbour.kt         # Neighbour relationships
│   ├── FlightPhase.kt       # Flight phases
│   └── LoadingOrder.kt      # Loading order
├── service/        # Domain services
│   ├── AggregationInitializer.kt  # Aggregate initialization
│   ├── NeighbourCalculator.kt     # Neighbour relationship calculation
│   └── LoadingOrderOutputExporter.kt  # Loading order export
├── Aggregation.kt  # Aggregate root
└── AircraftContext.kt  # Context entry point
```

## Core Concepts

- **Aircraft Model**：Aircraft type classification (B737/B757/B767/B747) with physical unit definitions.
- **Fuselage**：Aircraft fuselage properties including DOW, balanced arm, DOI, and liferaft.
- **Deck**：Physical deck on the aircraft with doors, cargo positions, and door proximity mappings.
- **Position**：Cargo position with coordinates (longitudinal/lateral arm), shape, location tags, and loading order.
- **Fuel**：Fuel constants per flight phase (takeoff, landing, zero-fuel) with weight and index.
- **ULD**：Unit Load Device with code and dimensions.
- **Neighbour**：Adjacency relationships between positions for constraint generation.
- **Loading Order**：Defines the order in which positions should be loaded.

## Constraints

This context does not define constraints; it only provides base configuration data for other contexts.

## Objective Function

This context does not define an objective function.

## Relationships with Other Contexts

**Downstream**: Stowage, MAC, Airworthiness Security, Express Effectiveness, Loading Effectiveness, MAC Optimization, Payload Maximization, Recommended Weight Equalization, Redundancy, Soft Security
