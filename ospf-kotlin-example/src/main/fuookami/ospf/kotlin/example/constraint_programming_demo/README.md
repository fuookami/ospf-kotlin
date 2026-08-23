# Constraint Programming Demos

:us: English | :cn: [简体中文](README_ch.md)

This package contains the smallest end-to-end CP examples in the repository:

- `DirectConstraintProgrammingDemo` builds an independent `ConstraintProgrammingModel` and solves it with the deterministic `FakeConstraintProgrammingSolver`.
- `CpDemoContext` owns an aggregation and `ConstraintProgrammingPipeline`; downstream code can add a pipeline without changing the solver engine.
- `LogicBasedBendersDemo` binds a binary master assignment to a CP subproblem and converges after a verified conflict no-good cut.

The Fake solver is deliberately used for the runnable smoke tests so they do not require a local SCIP or commercial-solver installation. Production callers can replace it with `ScipConstraintProgrammingSolver` through the same `ConstraintProgrammingSolver` interface.

The public contracts are documented in [`ospf-kotlin-core`](../../../../../../../../ospf-kotlin-core/README.md) and [`ospf-kotlin-framework`](../../../../../../../../ospf-kotlin-framework/README.md).
