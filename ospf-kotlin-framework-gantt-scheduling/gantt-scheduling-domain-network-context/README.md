# Gantt Network context

[简体中文](README_ch.md)

Optional bridge from Gantt business requests to the public Network path kernel.
Use `ospf-kotlin-starter-gantt-network`; the ordinary Gantt starter does not pull
in Network. This module does not depend on Network application services.

`NetworkBunchGenerationContext.generate` accepts a business `PathProblem`, search
budget, current `ColumnGenerationScope`, and a path-to-bunch extractor. The
extractor receives both the terminal state and full trace, preserving executor,
task assignment, internal activity times and business resource information.
Gantt continues to own the master, branching, fixed/hidden states and schedule.

The returned `ColumnGenerationBatch` separates candidates from completeness.
Rejected conversions, heuristic dominance and truncated searches cannot certify
no improving columns. Optional no-improvement evidence must be derived from the
complete search and current master contributions; a returned-column minimum is
not a valid global bound. Scope identities must match the current model, duals,
branch and executor/slot range. Exact pricing is not integer optimality.

Domain state retains generic quantities; numeric conversion belongs to the
pricing/solver boundary. A flow submodel may be registered into the Gantt model
through `FlowContext`, without starting a second application solve.

See Demo4's `BunchGenerationContext` and `FlightTaskBunchGenerator` for the actual
flight adaptation. `NetworkBunchGenerationContextTest` exercises conversion,
internal states, truncation and scope/evidence isolation.
