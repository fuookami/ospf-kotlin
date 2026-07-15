# Crew Domain Model

:us: English | :cn: [简体中文](domain-model_ch.md)

[toc]

## 1. Overview

Manages crew members, schedules, and transit times in the flight recovery scheduling system, providing crew data required for feasibility checking and cost calculation in bunch generation.

### 1. Dependent Contexts

1. **task** (Flight Task)
2. **infrastructure** (Infrastructure)

---

## 2. Concepts / Entities

### 1. Crew Member (CrewMember)

A crew member with identity information,分为 pilots and non-pilots.

**$\text{type}_{m}$** : Type of crew member $m$ (Operator, Attendant, Other).
**$\text{workerNo}_{m}$** : Worker number of crew member $m$.
**$\text{name}_{m}$** : Name of crew member $m$.
**$\text{nationality}_{m}$** : Nationality of crew member $m$.

### 2. Pilot (CrewPilotMember)

A crew member who is a pilot, delegating identity fields to the underlying `Pilot`.

**$\text{rank}_{p}$** : Rank of pilot $p$ (`PilotRank`).
**$\text{pilot}_{p}$** : Underlying `Pilot` object of pilot $p$.

### 3. Crew

A crew assigned to a flight task, composed of pilot and non-pilot members.

**$\text{flight}_{c}$** : Flight task assigned to crew $c$.
**$\text{members}_{c}$** : Member list of crew $c$.

### 4. Crew Schedule (CrewSchedule)

A crew member's schedule mapping flight tasks to their assigned rank.

**$\text{crewMan}_{s}$** : Crew member of schedule $s$.
**$\text{schedules}_{s}$** : Flight task to rank mapping of schedule $s$.

### 5. Transit Time (TransitTime)

Transit time scenario and its required duration based on aircraft and airport relationships.

**$\text{scene}_{t}$** : Scene of transit time $t$.
**$\text{duration}_{t}$** : Required duration of transit time $t$.

---

## 3. Variables

> The crew context does not directly define optimization variables; its data serves as input parameters for bunch generation.

---

## 4. Predicates

### 1. Crew Member Type

**isPilot** : Crew member $m$ is a pilot (`CrewPilotMember`).
**isNotPilot** : Crew member $m$ is not a pilot (`CrewNotPilotMember`).

### 2. Transit Time Scene

**isSameAircraft** : Previous and next tasks use the same aircraft.
**isDomesticNotSameAircraft** : Different aircraft but domestic airport.
**isInternationalNotSameAircraft** : Different aircraft and international airport.

---

## 5. Sets

### 1. Crews

**$C$** : Universal set of all crews.

**$C_{i}$** : Subset of crews assigned to flight task $i$, $\forall i \in I$.

### 2. Crew Members

**$M$** : Universal set of all crew members.

**$M^{P}$** : Subset of pilot members.
**$M^{N}$** : Subset of non-pilot members.
**$M_{c}$** : Subset of members in crew $c$, $\forall c \in C$.

### 3. Schedules

**$S$** : Universal set of all crew schedules.

**$S_{m}$** : Subset of schedules for crew member $m$, $\forall m \in M$.

### 4. Transit Times

**$T$** : Universal set of all transit time entries.

---

## 6. Intermediate Values

### 1. Transit Time Lookup

**Description**: Transit time for given consecutive flight tasks.

$$
\text{transitTime}(i_{prev}, i_{succ}) = \begin{cases}
T_{\text{SameAircraft}},& \text{same aircraft} \\ \\
T_{\text{Domestic}},& \text{different aircraft, domestic airport} \\ \\
T_{\text{International}},& \text{different aircraft, international airport} \\ \\
\text{null},& \text{otherwise}
\end{cases}
$$

---

## 7. Assertions

### 1. Crew Member Completeness

**Description**: Every crew must contain at least one member.

$$
\forall c \in C \; (|M_{c}| \geq 1)
$$

### 2. Schedule Continuity

**Description**: In a crew member's schedule, the arrival airport of a consecutive flight task must match the departure airport of the next task.

$$
\forall s \in S, \forall (i_{prev}, i_{succ}) \in \text{schedules}_{s} \; (i_{prev}.\text{arr} = i_{succ}.\text{dep})
$$

---

## 8. Constraints

> The crew context does not directly define optimization constraints; its data indirectly affects bunch generation feasibility via `ConnectionTimeCalculator` and `RuleChecker`.

---

## 9. Objective Function

> The crew context does not directly define an objective function.

---

## 10. Algorithm References

> No独立 algorithm references in this context.

---

## 11. Ubiquitous Language

| Term | Symbol | Definition |
|------|--------|------------|
| Crew | $C$ | A set of crew members assigned to a flight task |
| Crew Member | $M$ | A crew member with identity information |
| Pilot | $M^{P}$ | A crew member who operates the aircraft |
| Schedule | $S$ | Flight task assignments for a crew member |
| Transit Time | $T$ | Required interval between consecutive flights |

---

## 12. Design Decisions

| Decision | Alternatives | Rationale | Date |
|----------|--------------|-----------|------|
| Separate pilot/non-pilot modeling | Unified CrewMember type | Different rank systems; separation is clearer | - |
| Enumerate transit time scenes | Continuous function modeling | Limited number of scenes; enumeration is more intuitive | - |

---

## 13. Change Log

| Version | Change | Reason |
|---------|--------|--------|
| v1 | Initial implementation | Basic crew domain modeling |
