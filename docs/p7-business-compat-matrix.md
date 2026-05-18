# P7 Business Compatibility Matrix

- Generated at: 2026-05-17 12:37:38 +08:00
- Business root: `E:\workspace\poit\poit-or`
- Scan scope: APS, CSP1D, BOP, PSP Kotlin sources

External direct compile is not the default P7 gate because these projects still pin old OSPF coordinates and old `core.frontend` package names. The in-repo `business-source-compat` profile is the source-compat fixture for the restored current API surface.

## External direct compile status

Checked on `2026-05-18`:

| Project | Direct compile against ospf-kotlin 1.1.0 | Default gate scope | Notes |
|---|---|---|---|
| APS | Not enabled | Excluded | Large-scale old package imports (`core.frontend.*`, `utils.math.*`) still need project-side migration |
| CSP1D | Not enabled | Excluded | Same as APS; migration map provided by `scripts/p13-migration-map.md` |
| BOP | Not enabled | Excluded | Includes quadratic-path migration and starter wiring updates |
| PSP | Not enabled | Excluded | Includes gantt/starter migration and old package replacement |

Current policy:

1. Repository default gate uses `-Pbusiness-source-compat` fixtures instead of direct external compile.
2. External direct compile remains a project-side migration step and is tracked as non-blocking release evidence.
3. Promote to default gate only after all four projects complete package/API migration to 1.1.0.

## Project matrix

| Project | Kotlin files | LinearMetaModel | QuadraticMetaModel | AbstractLinearMetaModel | AbstractQuadraticMetaModel | Pipeline/PipelineList | LinearIntermediateSymbolsN | LinearExpressionSymbolsN | QuadraticIntermediateSymbolsN | BinVariableN | UIntVariableN | UReal/PctVariableN | vectorView/belongsTo | sum/qsum | Function symbols | Solver builders | Solver calls |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| APS | 221 | 22 | 0 | 118 | 0 | 39 | 109 | 6 | 0 | 16 | 0 | 22 | 17 | 121 | 35 | 8 | 7 |
| CSP1D | 173 | 13 | 0 | 308 | 0 | 29 | 192 | 63 | 0 | 11 | 28 | 14 | 22 | 529 | 66 | 14 | 7 |
| BOP | 73 | 3 | 1 | 39 | 16 | 27 | 11 | 0 | 5 | 19 | 4 | 7 | 0 | 52 | 35 | 7 | 11 |
| PSP | 92 | 46 | 0 | 43 | 0 | 22 | 42 | 0 | 0 | 15 | 4 | 1 | 0 | 86 | 13 | 0 | 1 |

## APS module matrix

| Module | Kotlin files | LinearMetaModel | QuadraticMetaModel | Pipeline/PipelineList | LinearIntermediateSymbolsN | LinearExpressionSymbolsN | QuadraticIntermediateSymbolsN | BinVariableN | UIntVariableN | UReal/PctVariableN | sum/qsum | Function symbols | Solver builders | Solver calls |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| aps-application | 8 | 18 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 5 | 0 |
| aps-domain-bunch-compilation-context | 16 | 3 | 0 | 1 | 24 | 6 | 0 | 4 | 0 | 6 | 13 | 6 | 0 | 0 |
| aps-domain-bunch-generation-context | 6 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| aps-domain-filling-production-schedule-context | 18 | 0 | 0 | 6 | 0 | 0 | 0 | 6 | 0 | 0 | 4 | 0 | 0 | 0 |
| aps-domain-material-requirements-planning-context | 19 | 0 | 0 | 9 | 20 | 0 | 0 | 4 | 0 | 8 | 21 | 2 | 0 | 0 |
| aps-domain-produce-context | 19 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| aps-domain-sequence-schedule-context | 4 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| aps-domain-task-context | 15 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 |
| aps-domain-work-shift-schedule-context | 38 | 1 | 0 | 23 | 65 | 0 | 0 | 2 | 0 | 8 | 82 | 27 | 1 | 0 |
| aps-infrastructure | 64 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 7 |
| aps-interface-consoles | 7 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| aps-interface-service | 7 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |

## CSP1D module matrix

| Module | Kotlin files | LinearMetaModel | QuadraticMetaModel | Pipeline/PipelineList | LinearIntermediateSymbolsN | LinearExpressionSymbolsN | QuadraticIntermediateSymbolsN | BinVariableN | UIntVariableN | UReal/PctVariableN | sum/qsum | Function symbols | Solver builders | Solver calls |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| csp1d-application | 9 | 10 | 0 | 0 | 0 | 0 | 0 | 3 | 4 | 2 | 41 | 0 | 9 | 0 |
| csp1d-domain-cutting-plan-generation-context | 15 | 2 | 0 | 13 | 19 | 4 | 0 | 2 | 16 | 0 | 229 | 8 | 2 | 0 |
| csp1d-domain-length-assignment-context | 21 | 0 | 0 | 12 | 25 | 1 | 0 | 4 | 0 | 4 | 5 | 6 | 0 | 0 |
| csp1d-domain-material-context | 16 | 1 | 0 | 0 | 0 | 0 | 0 | 2 | 0 | 0 | 110 | 0 | 1 | 0 |
| csp1d-domain-produce-context | 39 | 0 | 0 | 2 | 124 | 46 | 0 | 0 | 8 | 8 | 77 | 36 | 0 | 0 |
| csp1d-domain-wasting-minimization-context | 14 | 0 | 0 | 0 | 3 | 6 | 0 | 0 | 0 | 0 | 16 | 0 | 0 | 0 |
| csp1d-domain-yield-context | 24 | 0 | 0 | 2 | 21 | 6 | 0 | 0 | 0 | 0 | 51 | 16 | 0 | 0 |
| csp1d-infrastructure | 27 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 7 |
| csp1d-interface-consoles | 5 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| csp1d-interface-service | 3 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |

## BOP module matrix

| Module | Kotlin files | LinearMetaModel | QuadraticMetaModel | Pipeline/PipelineList | LinearIntermediateSymbolsN | LinearExpressionSymbolsN | QuadraticIntermediateSymbolsN | BinVariableN | UIntVariableN | UReal/PctVariableN | sum/qsum | Function symbols | Solver builders | Solver calls |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| bop-application | 3 | 2 | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 0 | 3 | 0 |
| bop-domain-cost-minimization-context | 5 | 0 | 0 | 6 | 0 | 0 | 0 | 0 | 0 | 0 | 3 | 0 | 0 | 0 |
| bop-domain-craft-context | 17 | 1 | 0 | 5 | 2 | 0 | 5 | 7 | 3 | 4 | 35 | 18 | 2 | 0 |
| bop-domain-formula-context | 13 | 0 | 0 | 7 | 6 | 0 | 0 | 0 | 1 | 1 | 5 | 7 | 0 | 0 |
| bop-domain-material-context | 5 | 0 | 0 | 0 | 0 | 0 | 0 | 12 | 0 | 0 | 4 | 6 | 0 | 0 |
| bop-domain-produce-context | 12 | 0 | 0 | 7 | 3 | 0 | 0 | 0 | 0 | 2 | 3 | 4 | 0 | 0 |
| bop-domain-stability-maximization-context | 3 | 0 | 0 | 2 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| bop-infrastructure | 13 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 11 |
| bop-interface-consoles | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| bop-interface-service | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |

## PSP module matrix

| Module | Kotlin files | LinearMetaModel | QuadraticMetaModel | Pipeline/PipelineList | LinearIntermediateSymbolsN | LinearExpressionSymbolsN | QuadraticIntermediateSymbolsN | BinVariableN | UIntVariableN | UReal/PctVariableN | sum/qsum | Function symbols | Solver builders | Solver calls |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| psp-application | 7 | 16 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 0 | 0 | 0 |
| psp-domain-bunch-compilation-context | 7 | 16 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 2 | 0 | 0 | 0 |
| psp-domain-bunch-generation-context | 10 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 10 | 0 | 0 | 0 |
| psp-domain-capacity-compilation-context | 31 | 14 | 0 | 22 | 42 | 0 | 0 | 15 | 4 | 1 | 53 | 13 | 0 | 0 |
| psp-domain-energy-context | 6 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| psp-domain-pulping-context | 10 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 16 | 0 | 0 | 0 |
| psp-infrastructure | 19 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 3 | 0 | 0 | 1 |
| psp-interface-consoles | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| psp-interface-service | 1 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |

## APS top imports

| Import | Count |
|---|---|
| `fuookami.ospf.kotlin.utils.math.*` | 157 |
| `fuookami.ospf.kotlin.utils.functional.*` | 121 |
| `fuookami.ospf.kotlin.core.frontend.model.mechanism.*` | 75 |
| `fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*` | 64 |
| `fuookami.ospf.kotlin.core.frontend.expression.polynomial.*` | 47 |
| `fuookami.ospf.kotlin.utils.multi_array.*` | 40 |
| `fuookami.ospf.kotlin.framework.model.*` | 38 |
| `fuookami.ospf.kotlin.utils.serialization.*` | 33 |
| `fuookami.ospf.kotlin.core.frontend.expression.monomial.*` | 29 |
| `fuookami.ospf.kotlin.core.frontend.inequality.*` | 26 |

## CSP1D top imports

| Import | Count |
|---|---|
| `fuookami.ospf.kotlin.utils.math.*` | 150 |
| `fuookami.ospf.kotlin.utils.functional.*` | 134 |
| `fuookami.ospf.kotlin.core.frontend.model.mechanism.*` | 111 |
| `fuookami.ospf.kotlin.core.frontend.expression.polynomial.*` | 72 |
| `fuookami.ospf.kotlin.utils.multi_array.*` | 61 |
| `fuookami.ospf.kotlin.core.frontend.expression.monomial.*` | 56 |
| `fuookami.ospf.kotlin.framework.model.*` | 44 |
| `fuookami.ospf.kotlin.utils.math.value_range.*` | 41 |
| `fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*` | 28 |
| `fuookami.ospf.kotlin.core.frontend.inequality.*` | 26 |

## BOP top imports

| Import | Count |
|---|---|
| `fuookami.ospf.kotlin.utils.math.*` | 59 |
| `fuookami.ospf.kotlin.utils.functional.*` | 51 |
| `fuookami.ospf.kotlin.core.frontend.model.mechanism.*` | 42 |
| `fuookami.ospf.kotlin.framework.model.*` | 27 |
| `fuookami.ospf.kotlin.utils.math.value_range.*` | 27 |
| `fuookami.ospf.kotlin.core.frontend.expression.polynomial.*` | 22 |
| `fuookami.ospf.kotlin.core.frontend.expression.monomial.*` | 17 |
| `fuookami.ospf.kotlin.core.frontend.inequality.*` | 13 |
| `fuookami.ospf.kotlin.core.frontend.variable.*` | 12 |
| `fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*` | 11 |

## PSP top imports

| Import | Count |
|---|---|
| `fuookami.ospf.kotlin.utils.math.*` | 72 |
| `fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*` | 54 |
| `fuookami.ospf.kotlin.utils.functional.*` | 54 |
| `fuookami.ospf.kotlin.core.frontend.model.mechanism.*` | 37 |
| `fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*` | 24 |
| `fuookami.ospf.kotlin.framework.model.*` | 22 |
| `fuookami.ospf.kotlin.core.frontend.expression.polynomial.*` | 20 |
| `fuookami.ospf.kotlin.core.frontend.expression.monomial.*` | 16 |
| `fuookami.ospf.kotlin.utils.multi_array.*` | 15 |
| `fuookami.ospf.kotlin.utils.*` | 14 |
