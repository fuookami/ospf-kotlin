package fuookami.ospf.kotlin.example.business_compat

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.Aggregation as CspProduceAggregation
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce as CspProduce
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.CuttingPlanProductOrder
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.APS
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.LSP
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.MPS
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.BunchCompilationContext
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.CapacitySchedulingContext
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Produce as GanttProduce
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.Resource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Task
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.IterativeTaskCompilationContext
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.HAPipeline
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.model.PipelineList
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.model.invoke
import fuookami.ospf.kotlin.framework.solver.Flt64FeasibleSolverOutput
import fuookami.ospf.kotlin.framework.solver.Flt64LinearMetaModel
import fuookami.ospf.kotlin.framework.solver.Flt64QuadraticMetaModel
import fuookami.ospf.kotlin.framework.solver.Flt64SolutionPool
import fuookami.ospf.kotlin.framework.solver.FltXFeasibleSolverOutput
import fuookami.ospf.kotlin.framework.solver.FltXLinearMetaModel
import fuookami.ospf.kotlin.framework.solver.FltXQuadraticMetaModel
import fuookami.ospf.kotlin.framework.solver.FltXSolutionPool
import fuookami.ospf.kotlin.framework.solver.FrameworkSolveOptions
import fuookami.ospf.kotlin.framework.solver.Rtn64FeasibleSolverOutput
import fuookami.ospf.kotlin.framework.solver.Rtn64LinearMetaModel
import fuookami.ospf.kotlin.framework.solver.Rtn64QuadraticMetaModel
import fuookami.ospf.kotlin.framework.solver.Rtn64SolutionPool
import fuookami.ospf.kotlin.framework.solver.RtnXFeasibleSolverOutput
import fuookami.ospf.kotlin.framework.solver.RtnXLinearMetaModel
import fuookami.ospf.kotlin.framework.solver.RtnXQuadraticMetaModel
import fuookami.ospf.kotlin.framework.solver.RtnXSolutionPool
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FrameworkStarterCompatTest {
    @Test
    fun pipelineAndCgRefreshShouldKeepBusinessCallShape() {
        val model = LinearMetaModel("p8_pipeline")
        val variables = BinVariable1("p8_x", Shape1(1))
        val key = ShadowPriceKey(FrameworkStarterCompatTest::class)
        try {
            assertOk(model.add(variables))

            val pipeline = object : CGPipeline<Any, LinearMetaModel<Flt64>, CompatShadowPriceMap> {
                override val name: String = "p8_cg_pipeline"

                override fun invoke(model: LinearMetaModel<Flt64>): Try {
                    return model.addConstraint(
                        constraint = variables[0],
                        group = this,
                        name = "p8_cg_constraint",
                        args = key
                    )
                }
            }

            val pipelines: PipelineList<LinearMetaModel<Flt64>> = listOf(pipeline)
            assertOk(pipelines(model))
            val constraint = model.constraintsOfGroup(pipeline).single()
            assertSame(key, constraint.args)

            val shadowPrices = MetaDualSolution(mapOf(constraint to Flt64(7.0)), emptyMap())
            val refreshMap = CompatShadowPriceMap()
            assertOk(pipeline.refresh(refreshMap, model, shadowPrices))
            assertEquals(Flt64(7.0), refreshMap[key]?.price)

            val staticRefreshMap = CompatShadowPriceMap()
            assertOk(CGPipeline.refreshByKeyAsArgs(pipeline, staticRefreshMap, model, shadowPrices))
            assertEquals(Flt64(7.0), staticRefreshMap[key]?.price)
        } finally {
            model.close()
        }
    }

    @Test
    fun haPipelineShouldCalculateAndCheckSolution() {
        val model = LinearMetaModel("p8_ha")
        try {
            val pipeline = object : HAPipeline<LinearMetaModel<Flt64>> {
                override val name: String = "p8_ha_pipeline"

                override fun calculate(model: LinearMetaModel<Flt64>, solution: List<Flt64>): Ret<Flt64?> {
                    return Ok(solution.fold(Flt64.zero) { acc, value -> acc + value })
                }
            }

            val obj = (pipeline(model, listOf(Flt64(1.0), Flt64(2.0))) as Ok).value
            assertEquals("p8_ha_pipeline", obj.tag)
            assertEquals(Flt64(3.0), obj.value)
            assertOk(pipeline.check(model, listOf(Flt64(1.0))))
        } finally {
            model.close()
        }
    }

    @Test
    fun frameworkSolverTypeAliasesShouldCompileForFourNumericFamilies() {
        val flt64Linear: Flt64LinearMetaModel = LinearMetaModel("p8_flt64_linear")
        val fltXLinear: FltXLinearMetaModel = LinearMetaModel("p8_fltx_linear", FltX)
        val rtn64Linear: Rtn64LinearMetaModel = LinearMetaModel("p8_rtn64_linear", Rtn64)
        val rtnXLinear: RtnXLinearMetaModel = LinearMetaModel("p8_rtnx_linear", RtnX)
        val flt64Quadratic: Flt64QuadraticMetaModel = fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel("p8_flt64_quadratic")
        val fltXQuadratic: FltXQuadraticMetaModel = fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel("p8_fltx_quadratic", FltX)
        val rtn64Quadratic: Rtn64QuadraticMetaModel = fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel("p8_rtn64_quadratic", Rtn64)
        val rtnXQuadratic: RtnXQuadraticMetaModel = fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel("p8_rtnx_quadratic", RtnX)
        val flt64Pool: Flt64SolutionPool = emptyList()
        val fltXPool: FltXSolutionPool = emptyList()
        val rtn64Pool: Rtn64SolutionPool = emptyList()
        val rtnXPool: RtnXSolutionPool = emptyList()
        val flt64Output: Flt64FeasibleSolverOutput? = null
        val fltXOutput: FltXFeasibleSolverOutput? = null
        val rtn64Output: Rtn64FeasibleSolverOutput? = null
        val rtnXOutput: RtnXFeasibleSolverOutput? = null
        try {
            assertEquals("p8_flt64_linear", flt64Linear.name)
            assertEquals("p8_fltx_linear", fltXLinear.name)
            assertEquals("p8_rtn64_linear", rtn64Linear.name)
            assertEquals("p8_rtnx_linear", rtnXLinear.name)
            assertEquals("p8_flt64_quadratic", flt64Quadratic.name)
            assertEquals("p8_fltx_quadratic", fltXQuadratic.name)
            assertEquals("p8_rtn64_quadratic", rtn64Quadratic.name)
            assertEquals("p8_rtnx_quadratic", rtnXQuadratic.name)
            assertTrue(flt64Pool.isEmpty())
            assertTrue(fltXPool.isEmpty())
            assertTrue(rtn64Pool.isEmpty())
            assertTrue(rtnXPool.isEmpty())
            assertEquals(null, flt64Output)
            assertEquals(null, fltXOutput)
            assertEquals(null, rtn64Output)
            assertEquals(null, rtnXOutput)
        } finally {
            flt64Linear.close()
            fltXLinear.close()
            rtn64Linear.close()
            rtnXLinear.close()
            flt64Quadratic.close()
            fltXQuadratic.close()
            rtn64Quadratic.close()
            rtnXQuadratic.close()
        }
    }

    @Test
    fun frameworkSolveOptionsBuilderShouldKeepBusinessSolverShape() {
        val options = FrameworkSolveOptions.build {
            name = "p8_solver"
            toLogModel = true
            solutionAmount = UInt64(3UL)
        }

        assertEquals("p8_solver", options.solveName("fallback"))
        assertEquals(true, options.toLogModel)
        assertEquals(UInt64(3UL), options.solutionAmount)
        assertEquals(UInt64(3UL), options.toCoreSolveOptions().solutionAmount)
    }

    @Test
    fun startersShouldExposeGanttAndCsp1dBusinessModules() {
        val exposedClasses = listOf(
            APS::class.java.name,
            LSP::class.java.name,
            MPS::class.java.name,
            TimeRange::class.java.name,
            BunchCompilationContext::class.java.name,
            CapacitySchedulingContext::class.java.name,
            IterativeTaskCompilationContext::class.java.name,
            GanttProduce::class.java.name,
            Resource::class.java.name,
            Task::class.java.name,
            CuttingPlanProductOrder::class.java.name,
            Product::class.java.name,
            CuttingPlan::class.java.name,
            CspProduceAggregation::class.java.name,
            CspProduce::class.java.name
        )

        assertNotNull(APS())
        assertNotNull(LSP())
        assertNotNull(MPS())
        assertEquals(CuttingPlanProductOrder.Asc, CuttingPlanProductOrder.valueOf("Asc"))
        assertTrue(exposedClasses.any { it.contains("framework.gantt_scheduling.domain.produce") })
        assertTrue(exposedClasses.any { it.contains("framework.gantt_scheduling.domain.resource") })
        assertTrue(exposedClasses.any { it.contains("framework.csp1d.domain.material") })
        assertTrue(exposedClasses.any { it.contains("framework.csp1d.domain.produce") })
    }

    private class CompatShadowPriceMap : AbstractShadowPriceMap<Any, CompatShadowPriceMap>()

    private fun assertOk(result: Try) {
        assertTrue(result is Ok)
    }
}
