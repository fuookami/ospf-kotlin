# Crew

:us: English | :cn: [简体中文](README_ch.md)

Bounded context responsible for crew domain operations in the flight recovery scheduling system. It models crew members (pilots and non-pilot staff), their schedules, and transit time requirements between consecutive flights.

## Responsibilities

- Model crew members with identity, rank, and type information.
- Track crew schedules mapping flight tasks to assigned ranks.
- Define transit time scenarios (same aircraft, domestic transfer, international transfer).
- Provide crew data for feasibility checking and cost calculation in bunch generation.

## Key Classes

| Class | Description |
|---|---|
| `CrewContext` | Entry point for crew domain operations. |
| `Aggregation` | Holds `Crew` list, `CrewSchedule` list, and `TransitTimeMap`. |
| `Crew` | A crew assigned to a flight task, composed of pilot and non-pilot members. |
| `CrewMember` | Sealed interface: `CrewPilotMember` or `CrewNotPilotMember`. |
| `CrewSchedule` | Maps flight tasks to assigned ranks for a crew member. |
| `TransitTime` | Associates a transit time scene with its required duration. |
| `TransitTimeScene` | Enum: `SameAircraft`, `DomainNotSameAircraft`, `InternationNotSameAircraft`. |

## Dependencies

- **task** — provides `FlightTask`, `Aircraft`, `Airport`, `AirportType`.
- **infrastructure** — provides `WorkerNo`, `Pilot`, `CrewMan`, `PilotRank`, `CrewManRank`.
