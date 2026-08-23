package fuookami.ospf.kotlin.core.solver.scip

import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import jscip.SCIP_Status
import jscip.SCIP_Vartype
import jscip.Constraint
import jscip.Scip
import jscip.Variable
import fuookami.ospf.kotlin.core.solver.report.TerminationReason

/** 验证 JSCIP 原生 CP 约束创建与求解能力 / Verifies JSCIP native CP constraint creation and solving capabilities. */
class ScipConstraintProgrammingProbeIT {
    @Test
    fun completedNativeStatusesWinOverLateCancellation() {
        loadNativeLibrary()

        listOf(
            SCIP_Status.SCIP_STATUS_OPTIMAL,
            SCIP_Status.SCIP_STATUS_INFEASIBLE,
            SCIP_Status.SCIP_STATUS_UNBOUNDED,
            SCIP_Status.SCIP_STATUS_INFORUNBD
        ).forEach { status ->
            assertEquals(
                TerminationReason.Completed,
                ScipConstraintProgrammingStatusMapper.terminationReason(
                    status = status,
                    cancellationRequested = true
                )
            )
        }
    }

    @Test
    fun jscipShouldSolveBooleanIndicatorAndCumulativeModel() {
        loadNativeLibrary()

        val scip = Scip()
        val variables = ArrayList<Variable>()
        val constraints = ArrayList<Constraint>()
        try {
            scip.create("constraint-programming-probe")
            scip.hideOutput(true)

            val firstLiteral = binaryVariable(scip, variables, "first_literal")
            val secondLiteral = binaryVariable(scip, variables, "second_literal")
            val conjunction = binaryVariable(scip, variables, "conjunction")
            val firstStart = integerVariable(scip, variables, "first_start", 0.0, 3.0)
            val secondStart = integerVariable(scip, variables, "second_start", 0.0, 3.0)

            addConstraint(
                scip = scip,
                constraints = constraints,
                constraint = scip.createConsLinear(
                    "force_boolean_inputs",
                    arrayOf(firstLiteral, secondLiteral),
                    doubleArrayOf(1.0, 1.0),
                    2.0,
                    2.0
                )
            )
            addConstraint(
                scip = scip,
                constraints = constraints,
                constraint = scip.createConsAnd(
                    "boolean_and",
                    conjunction,
                    arrayOf(firstLiteral, secondLiteral)
                )
            )
            addConstraint(
                scip = scip,
                constraints = constraints,
                constraint = scip.createConsIndicator(
                    "indicator_start_limit",
                    conjunction,
                    arrayOf(firstStart),
                    doubleArrayOf(1.0),
                    1.0
                )
            )
            addConstraint(
                scip = scip,
                constraints = constraints,
                constraint = scip.createConsCumulative(
                    "single_machine_cumulative",
                    arrayOf(firstStart, secondStart),
                    intArrayOf(2, 2),
                    intArrayOf(1, 1),
                    1
                )
            )

            scip.solve()

            assertEquals(
                SCIP_Status.SCIP_STATUS_OPTIMAL,
                scip.status,
                "JSCIP 应证明探针模型最优 / JSCIP should prove the probe model optimal"
            )
            val solution = assertNotNull(
                scip.bestSol,
                "JSCIP 应返回探针模型解 / JSCIP should return a probe model solution"
            )
            assertEquals(1.0, scip.getSolVal(solution, firstLiteral))
            assertEquals(1.0, scip.getSolVal(solution, secondLiteral))
            assertEquals(1.0, scip.getSolVal(solution, conjunction))

            val firstStartValue = scip.getSolVal(solution, firstStart)
            val secondStartValue = scip.getSolVal(solution, secondStart)
            assertTrue(
                firstStartValue <= 1.0 + TOLERANCE,
                "indicator 应在 conjunction 为真时限制 firstStart / indicator should limit firstStart when conjunction is true"
            )
            assertTrue(
                abs(firstStartValue - secondStartValue) + TOLERANCE >= 2.0,
                "cumulative 应禁止两个 duration=2 的任务重叠 / cumulative should prevent two duration=2 tasks from overlapping"
            )
        } finally {
            constraints.asReversed().forEach(scip::releaseCons)
            variables.asReversed().forEach(scip::releaseVar)
            scip.free()
        }
    }

    private fun binaryVariable(scip: Scip, variables: MutableList<Variable>, name: String): Variable {
        return integerVariable(
            scip = scip,
            variables = variables,
            name = name,
            lowerBound = 0.0,
            upperBound = 1.0,
            type = SCIP_Vartype.SCIP_VARTYPE_BINARY
        )
    }

    private fun integerVariable(
        scip: Scip,
        variables: MutableList<Variable>,
        name: String,
        lowerBound: Double,
        upperBound: Double,
        type: SCIP_Vartype = SCIP_Vartype.SCIP_VARTYPE_INTEGER
    ): Variable {
        return scip.createVar(name, lowerBound, upperBound, 0.0, type).also(variables::add)
    }

    private fun addConstraint(scip: Scip, constraints: MutableList<Constraint>, constraint: Constraint) {
        scip.addCons(constraint)
        constraints.add(constraint)
    }

    private fun loadNativeLibrary() {
        val library = assertNotNull(
            nativeLibraryCandidates().firstOrNull(Files::isRegularFile),
            "未找到 JSCIP 原生库；设置 ospf.scip.library 或 JSCIP_HOME / " +
                "JSCIP native library was not found; set ospf.scip.library or JSCIP_HOME"
        )
        System.load(library.toAbsolutePath().toString())
    }

    private fun nativeLibraryCandidates(): List<Path> {
        val libraryName = when {
            System.getProperty("os.name").contains("win", ignoreCase = true) -> "jscip.dll"
            System.getProperty("os.name").contains("mac", ignoreCase = true) -> "libjscip.dylib"
            else -> "libjscip.so"
        }
        val explicitLibrary = System.getProperty("ospf.scip.library")
        val jscipHome = System.getenv("JSCIP_HOME")
        return buildList {
            if (!explicitLibrary.isNullOrBlank()) {
                add(Path.of(explicitLibrary))
            }
            if (!jscipHome.isNullOrBlank()) {
                add(Path.of(jscipHome, libraryName))
            }
        }
    }

    private companion object {
        const val TOLERANCE = 1e-6
    }
}
