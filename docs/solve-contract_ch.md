# 求解报告与进度契约

[English](solve-contract.md)

OSPF 提供正交的求解报告和统一进度契约。`Ret<SolveReport<V>>` 是唯一主求解结果；尚未发布的
`1.1.0` 源码不再保留 `FeasibleSolverOutput` 兼容 facade。新集成应调用 `solveReport`，并按
`ProblemStatus`、`TerminationReason` 和 `SolutionPresence` 三个独立维度处理结果。

```kotlin
val progress = SolverProgressContext(
    reporter = ProgressReporter { snapshot ->
        saveProgress(snapshot)
    },
    labelResolver = applicationLabelResolver,
    locale = "zh-CN"
)

val report = solver.solveReport(model, progress)
```

`SolverStage` 和 `SolverSubStage` 只携带稳定的 `i18n.ospf.*` 键及简体中文默认文本。
目标语言翻译由调用方通过 `ProgressLabelResolver` 注入，OSPF 不保存业务系统的翻译矩阵。

统一报告还提供：

- `SolverCapabilities`、`SolverProvenance` 和脱敏 backend 配置契约；
- `ConstraintEvaluation`、`InfeasibilityEvidence` 和诊断失败信息；
- `SolveHandle`/`CancellationToken` 幂等取消；
- 带 schema 版本的规范化模型和 SHA-256 审计指纹；
- `SolveAttemptTrace`、远程报告字段和确定性批量实验模型。

远程协议的 `SolveResult` 新字段均有兼容默认值，旧节点返回的 `feasible`/`optimal` 仍可读取。
新节点应同时填写 `schemaVersion`、正交状态、provenance 和 fingerprints。
