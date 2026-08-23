package fuookami.ospf.kotlin.core.solver.report

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import fuookami.ospf.kotlin.utils.functional.*

/** 批量实验场景 / Batch experiment case */
data class SolveExperimentCase<I>(
    val id: String,
    val input: I,
    val seed: Long? = null,
    val generatorVersion: String? = null,
    val modelFingerprint: AuditFingerprint? = null,
    val configurationFingerprint: AuditFingerprint? = null
)

/** 单个实验结果 / Single experiment result */
data class SolveExperimentResult<V>(
    val caseId: String,
    val report: SolveReport<V>?,
    val errors: List<SolveIssue> = emptyList()
)

/** 批量实验报告 / Batch experiment report */
data class BatchSolveExperimentReport<V>(
    val schemaVersion: String = "1.0",
    val concurrencyBudget: Int,
    val results: List<SolveExperimentResult<V>>
)

/**
 * 纯输入输出批量实验运行器。 / Pure input-output batch experiment runner.
 *
 * 场景按 ID 排序后调度，结果始终按 ID 排序；并发只影响执行时序，不影响报告顺序。 / Cases are scheduled and reported by ID; concurrency affects execution timing only.
 */
object BatchSolveExperimentRunner {
    /**
     * 运行确定性批量实验。 / Run a deterministic batch experiment.
     *
     * @param cases 实验场景 / Experiment cases
     * @param concurrencyBudget 最大并发数 / Maximum concurrency
     * @param executor 无副作用场景执行器 / Side-effect-free case executor
     * @return 结构化实验报告 / Structured experiment report
     */
    suspend fun <I, V> run(
        cases: List<SolveExperimentCase<I>>,
        concurrencyBudget: Int,
        executor: suspend (SolveExperimentCase<I>) -> Ret<SolveReport<V>>
    ): BatchSolveExperimentReport<V> = coroutineScope {
        val effectiveBudget = concurrencyBudget.coerceAtLeast(1)
        val semaphore = Semaphore(effectiveBudget)
        val results = cases.sortedBy { it.id }.map { case ->
            async {
                semaphore.withPermit {
                    when (val result = executor(case)) {
                        is Ok -> SolveExperimentResult(
                            caseId = case.id,
                            report = result.value
                        )
                        is Failed -> SolveExperimentResult(
                            caseId = case.id,
                            report = null,
                            errors = listOf(
                                SolveIssue(
                                    code = result.error.code.toString(),
                                    category = SolveIssueCategory.Backend,
                                    message = result.error.message
                                )
                            )
                        )
                        is Fatal -> SolveExperimentResult(
                            caseId = case.id,
                            report = null,
                            errors = result.errors.map { error ->
                                SolveIssue(
                                    code = error.code.toString(),
                                    category = SolveIssueCategory.Backend,
                                    message = error.message
                                )
                            }
                        )
                    }
                }
            }
        }.awaitAll().sortedBy { it.caseId }
        BatchSolveExperimentReport(
            concurrencyBudget = effectiveBudget,
            results = results
        )
    }
}

/** 生成式 MILP 样本审计元数据 / Generative MILP sample audit metadata */
data class GeneratedMilpSampleMetadata(
    val generatorId: String,
    val generatorVersion: String,
    val seed: Long,
    val modelFingerprint: AuditFingerprint,
    val configurationFingerprint: AuditFingerprint,
    val solverFingerprint: AuditFingerprint? = null,
    val provenance: SolverProvenance? = null
)

/** 离线学习观测，不携带生产求解控制指令 / Offline learning observation without production solve controls */
data class OfflineSolveObservation<V>(
    val sample: GeneratedMilpSampleMetadata,
    val problemStatus: ProblemStatus,
    val terminationReason: TerminationReason,
    val objective: V? = null,
    val bestBound: V? = null,
    val gap: V? = null,
    val features: Map<String, String> = emptyMap(),
    val labels: Map<String, String> = emptyMap()
)
