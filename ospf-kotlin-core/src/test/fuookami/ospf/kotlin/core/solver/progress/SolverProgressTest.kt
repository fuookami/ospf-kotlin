package fuookami.ospf.kotlin.core.solver.progress

import kotlin.test.*
import kotlinx.coroutines.CancellationException
import fuookami.ospf.kotlin.utils.functional.ok

class SolverProgressTest {
    @Test
    fun shouldResolveLabelsNormalizePercentagesAndRenderArguments() {
        var reported: SolverProgressSnapshot? = null
        val context = SolverProgressContext(
            reporter = ProgressReporter { snapshot ->
                reported = snapshot
                ok
            },
            labelResolver = ProgressLabelResolver { key, defaultText, locale ->
                "$locale:$key:$defaultText"
            },
            locale = "ja-JP"
        )

        context.report(
            SolverProgressSnapshot(
                stage = SolverStages.ColumnGeneration,
                subStage = SolverSubStage(
                    key = "master_iter",
                    defaultTemplate = "主问题求解（迭代 {iter}）",
                    messageKey = "i18n.ospf.substage.master_iter",
                    args = mapOf("iter" to "7")
                ),
                progressInStage = 120,
                overallProgress = -1
            )
        )

        assertEquals(100, reported?.progressInStage)
        assertEquals(0, reported?.overallProgress)
        assertEquals(
            "ja-JP:i18n.ospf.stage.COLUMN_GENERATION:列生成",
            reported?.resolvedStageLabel
        )
        assertEquals(
            "ja-JP:i18n.ospf.substage.master_iter:主问题求解（迭代 {iter}）".replace("{iter}", "7"),
            reported?.resolvedSubStageLabel
        )
    }

    @Test
    fun shouldRenderArgumentsAfterResolvingLocalizedTemplate() {
        var reported: SolverProgressSnapshot? = null
        val context = SolverProgressContext(
            reporter = ProgressReporter { snapshot ->
                reported = snapshot
                ok
            },
            labelResolver = ProgressLabelResolver { _, _, _ -> "Master iteration {iter}" },
            locale = "en-US"
        )

        context.report(
            SolverProgressSnapshot(
                stage = SolverStages.MILP,
                subStage = SolverSubStage(
                    key = "master_iter",
                    defaultTemplate = "主问题求解（迭代 {iter}）",
                    messageKey = "i18n.ospf.substage.master_iter",
                    args = mapOf("iter" to "7")
                ),
                progressInStage = 50,
                overallProgress = 80
            )
        )

        assertEquals("Master iteration 7", reported?.resolvedSubStageLabel)
    }

    @Test
    fun shouldKeepOutOfOrderStagesAndMakeProgressMonotonic() {
        val snapshots = mutableListOf<SolverProgressSnapshot>()
        val context = SolverProgressContext(
            reporter = ProgressReporter { snapshot ->
                snapshots += snapshot
                ok
            }
        )

        context.report(
            SolverProgressSnapshot(
                stage = SolverStages.ColumnGeneration,
                progressInStage = 100,
                overallProgress = 70
            )
        )
        context.report(
            SolverProgressSnapshot(
                stage = SolverStages.Registration,
                progressInStage = 100,
                overallProgress = 20
            )
        )
        context.report(
            SolverProgressSnapshot(
                stage = SolverStages.ColumnGeneration,
                progressInStage = 10,
                overallProgress = 40
            )
        )

        assertEquals(3, snapshots.size)
        assertEquals(70, snapshots[1].overallProgress)
        assertEquals(70, snapshots.last().overallProgress)
        assertEquals(100, snapshots.last().progressInStage)
        assertEquals(SolverStages.ColumnGeneration, snapshots.last().stage)
    }

    @Test
    fun shouldRetainMpsRegistrationMasterLpAndColumnGenerationStages() {
        val snapshots = mutableListOf<SolverProgressSnapshot>()
        val context = SolverProgressContext(
            reporter = ProgressReporter { snapshot ->
                snapshots += snapshot
                ok
            }
        )

        context.report(
            SolverProgressSnapshot(
                stage = SolverStages.Registration,
                progressInStage = 100,
                overallProgress = 10
            )
        )
        context.report(
            SolverProgressSnapshot(
                stage = SolverStages.MasterLP,
                progressInStage = 100,
                overallProgress = 70
            )
        )
        context.report(
            SolverProgressSnapshot(
                stage = SolverStages.ColumnGeneration,
                progressInStage = 20,
                overallProgress = 72
            )
        )

        assertEquals(
            listOf(SolverStages.Registration, SolverStages.MasterLP, SolverStages.ColumnGeneration),
            snapshots.map { it.stage }
        )
    }

    @Test
    fun shouldExposeStableChineseStageCatalog() {
        assertTrue(SolverStages.entries.all { it.messageKey == "i18n.ospf.stage.${it.id.value}" })
        assertTrue(SolverStages.entries.all { it.defaultLabel.isNotBlank() })
        assertEquals(SolverStageId("MILP"), SolverStages.MILP.id)
    }

    @Test
    fun shouldStopReportingWhenCancellationIsRequested() {
        val context = SolverProgressContext(
            reporter = ProgressReporter { ok },
            isCancelled = { true }
        )

        assertFailsWith<CancellationException> {
            context.report(
                SolverProgressSnapshot(
                    stage = SolverStages.MILP,
                    progressInStage = 0,
                    overallProgress = 30
                )
            )
        }
    }
}
