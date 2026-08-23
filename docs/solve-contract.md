# Solve Report and Progress Contracts

[简体中文](solve-contract_ch.md)

OSPF exposes orthogonal solve reports and a unified progress contract. `Ret<SolveReport<V>>` is the
primary solver result; there is no `FeasibleSolverOutput` compatibility facade in the unreleased
`1.1.0` source line. New integrations should call `solveReport` and handle `ProblemStatus`,
`TerminationReason`, and `SolutionPresence` as independent dimensions.

```kotlin
val progress = SolverProgressContext(
    reporter = ProgressReporter { snapshot ->
        saveProgress(snapshot)
    },
    labelResolver = applicationLabelResolver,
    locale = "en-US"
)

val report = solver.solveReport(model, progress)
```

`SolverStage` and `SolverSubStage` carry stable `i18n.ospf.*` keys and Simplified Chinese fallback
text only. Callers inject target-language translations through `ProgressLabelResolver`.

The contract also includes solver capabilities and provenance, structured constraint and
infeasibility diagnostics, idempotent cancellation handles, versioned normalized models, SHA-256
audit fingerprints, attempt traces, versioned remote result fields, and deterministic batch
experiment models.

All new fields in the remote `SolveResult` have compatibility defaults. Older nodes that only return
`feasible` and `optimal` remain readable; upgraded nodes should populate the schema version,
orthogonal statuses, provenance, and fingerprints.
