# Cargo

:us: English | :cn: [简体中文](README_ch.md)

Bounded context responsible for cargo domain operations in the flight recovery scheduling system. This context is currently a placeholder for future cargo-specific logic such as cargo routing, capacity tracking, and disruption handling.

## Responsibilities

- Manage cargo-specific domain objects and operations.
- Track cargo capacity across flight tasks.
- Handle cargo disruption scenarios during flight recovery.

## Key Classes

| Class | Description |
|---|---|
| `CargoContext` | Entry point for cargo domain operations. |
| `Aggregation` | Aggregation of cargo domain objects (placeholder). |

## Dependencies

- **task** — provides `FlightTask`, `Aircraft`, `AircraftCapacity.Cargo`.
- **rule** — provides cargo-related flow control and restrictions.
