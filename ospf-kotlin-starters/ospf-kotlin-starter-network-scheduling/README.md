# ospf-kotlin-starter-network-scheduling

[简体中文](README_ch.md)

Standalone Network entry: topology, flow, resource paths, design, explicit-scenario resilience and VRPTW branch-and-price application.

Includes transport allocation/hubs, binary whole-path selection, rooted/Steiner structures and facility ownership, and finite-capacity generalized flows with explicit loss/yield or single-input conversion. For a smaller embedded dependency surface, import only the required domain modules instead of this starter. Finite candidate-pool optimality does not certify full-network optimality.

See [Network framework](../../ospf-kotlin-framework-network-scheduling/README.md) for units, model registration, search completion and proof boundaries. A Gantt schedule remains owned by Gantt; Network only contributes its adapted subproblem.
