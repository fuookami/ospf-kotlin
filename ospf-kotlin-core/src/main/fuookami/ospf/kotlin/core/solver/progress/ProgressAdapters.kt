package fuookami.ospf.kotlin.core.solver.progress

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.solver.output.*

/**
 * 将模型构建回调适配为统一进度，并保留原回调。 / Adapt model-building callbacks to unified progress while preserving the delegate.
 *
 * @param delegate 原回调 / Original callback
 * @return 组合回调 / Composed callback
 */
fun SolverProgressContext.modelBuildingCallback(
    delegate: ModelBuildingStatusCallBack? = null
): ModelBuildingStatusCallBack {
    return ModelBuildingStatusCallBack { status ->
        val progress = percentage(status.ready.toString(), status.total.toString())
        report(
            SolverProgressSnapshot(
                stage = SolverStages.ModelBuilding,
                subStage = SolverSubStage(
                    key = status.stage.name.lowercase(),
                    defaultTemplate = "模型构建：{stage}",
                    messageKey = "i18n.ospf.substage.model_building",
                    args = mapOf("stage" to status.stage.name)
                ),
                progressInStage = progress,
                overallProgress = progress / 10,
                diagnostics = mapOf(
                    "model" to status.modelName,
                    "ready" to status.ready.toString(),
                    "total" to status.total.toString()
                )
            )
        ).then(delegate?.let { { it(status) } } ?: { ok })
    }
}

/**
 * 将注册回调适配为统一进度，并保留原回调。 / Adapt registration callbacks to unified progress while preserving the delegate.
 *
 * @param delegate 原回调 / Original callback
 * @return 组合回调 / Composed callback
 */
fun SolverProgressContext.registrationCallback(
    delegate: RegistrationStatusCallBack? = null
): RegistrationStatusCallBack {
    return RegistrationStatusCallBack { status ->
        val progress = percentage(status.readySymbolAmount.toString(), status.totalSymbolAmount.toString())
        report(
            SolverProgressSnapshot(
                stage = SolverStages.Registration,
                progressInStage = progress,
                overallProgress = 10 + progress / 5,
                diagnostics = mapOf(
                    "ready" to status.readySymbolAmount.toString(),
                    "total" to status.totalSymbolAmount.toString(),
                    "empty" to status.emptySymbolAmount.toString()
                )
            )
        ).then(delegate?.let { { it(status) } } ?: { ok })
    }
}

/**
 * 将原生求解回调适配为统一进度，并保留原回调。 / Adapt native solving callbacks to unified progress while preserving the delegate.
 *
 * @param stage 求解阶段 / Solve stage
 * @param delegate 原回调 / Original callback
 * @return 组合回调 / Composed callback
 */
fun SolverProgressContext.solvingCallback(
    stage: SolverStage = SolverStages.MILP,
    delegate: SolvingStatusCallBack? = null
): SolvingStatusCallBack {
    return SolvingStatusCallBack { status ->
        val gap = status.gap.toString().toDoubleOrNull()
        val progress = gap?.let { value ->
            ((1.0 - value.coerceIn(0.0, 1.0)) * 99.0).toInt()
        } ?: 0
        val overallProgress = when (stage.kind) {
            StageKind.MasterLP -> 30 + progress * 4 / 10
            StageKind.ColumnGeneration -> 60 + progress * 2 / 10
            StageKind.BranchAndPrice -> 70 + progress * 2 / 10
            else -> 30 + progress * 7 / 10
        }
        report(
            SolverProgressSnapshot(
                stage = stage,
                subStage = SolverSubStage(
                    key = "incumbent",
                    defaultTemplate = "求解中（gap {gap}）",
                    messageKey = "i18n.ospf.substage.incumbent",
                    args = mapOf("gap" to status.gap.toString())
                ),
                progressInStage = progress,
                overallProgress = overallProgress,
                diagnostics = mapOf(
                    "solver" to status.solver,
                    "objective" to status.obj.toString(),
                    "bestBound" to (status.bestBound?.toString() ?: ""),
                    "gap" to status.gap.toString(),
                    "iterations" to (status.iterations?.toString() ?: ""),
                    "nodes" to (status.nodeCount?.toString() ?: "")
                )
            )
        ).then(delegate?.let { { it(status) } } ?: { ok })
    }
}

private fun Try.then(next: () -> Try): Try {
    return when (this) {
        is Ok -> next()
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}

private fun percentage(ready: String, total: String): Int {
    val readyValue = ready.toDoubleOrNull() ?: return 0
    val totalValue = total.toDoubleOrNull() ?: return 0
    return if (totalValue == 0.0) {
        100
    } else {
        (readyValue * 100.0 / totalValue).toInt().coerceIn(0, 100)
    }
}
