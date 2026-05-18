package fuookami.ospf.kotlin.example.business_compat

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols2
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols2
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbols
import fuookami.ospf.kotlin.core.intermediate_symbol.SymbolCombination
import fuookami.ospf.kotlin.core.intermediate_symbol.function.AbsFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.BinaryzationFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.CeilingFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaskingFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackRangeFunction
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.solver.QuadraticSolver
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearSolver
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiQuadraticSolver
import fuookami.ospf.kotlin.core.solver.scip.ScipColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.core.solver.scip.ScipQuadraticSolver
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.core.variable.BinVariable2
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.UIntVariable2
import fuookami.ospf.kotlin.core.variable.UInteger
import fuookami.ospf.kotlin.core.variable.URealVariable1
import fuookami.ospf.kotlin.core.variable.URealVariable2
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.CuttingPlanProductOrder
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.APS
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.model.PipelineList
import fuookami.ospf.kotlin.framework.model.invoke
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.le
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.minus
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.plus
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.qsum
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.sum
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class BusinessSourceCompatTest {
    @Test
    fun apsRepresentativeModelingChainShouldCompile() {
        val model = LinearMetaModel("p7_aps")
        val abstractModel: AbstractLinearMetaModel<Flt64> = model
        try {
            val work = BinVariable1("aps_work", Shape1(2))
            val capacity = URealVariable2("aps_capacity", Shape2(2, 2))
            assertOk(model.add(work), "APS work variables should be accepted")
            assertOk(model.add(capacity), "APS capacity variables should be accepted")

            val produce: LinearExpressionSymbols2<Flt64> = SymbolCombination("aps_produce", Shape2(2, 2)) { _, v ->
                LinearExpressionSymbol(capacity[v[0], v[1]], name = "aps_produce_${v[0]}_${v[1]}")
            }
            assertOk(model.add(produce), "APS LinearExpressionSymbols2 should be accepted")

            val maxProduce: LinearIntermediateSymbols1<Flt64> = SymbolCombination("aps_max_produce", Shape1(2)) { i, _ ->
                MaxFunction(
                    polynomials = listOf(produce[i, 0], produce[i, 1]),
                    name = "aps_max_produce_$i"
                )
            }
            assertOk(model.add(maxProduce), "APS MaxFunction symbol combination should be accepted")

            val slack: LinearIntermediateSymbols1<Flt64> = SymbolCombination("aps_slack", Shape1(2)) { i, _ ->
                SlackFunction(
                    x = maxProduce[i],
                    y = Flt64(10.0),
                    type = UContinuous,
                    name = "aps_slack_$i"
                )
            }
            assertOk(model.add(slack), "APS SlackFunction symbol combination should be accepted")

            val pipeline = linearPipeline("aps_capacity_pipeline", capacity[0, 0])
            val pipelines: PipelineList<AbstractLinearMetaModel<Flt64>> = listOf(pipeline)
            assertOk(pipelines(abstractModel), "APS PipelineList should register and invoke")
            assertEquals(1, model.constraintsOfGroup(pipeline).size)
            assertOk(model.minimize(sum(listOf(capacity[0, 0], capacity[1, 1]))), "APS objective should be accepted")

            // P10: intermediate symbol constraint — previously blocked by ClassCast
            assertOk(model.addConstraint(slack[0] le Flt64(10.0), name = "aps_slack0_limit"),
                "APS addConstraint with intermediate symbol should succeed")

            assertNotNull(APS(), "gantt starter dependency should expose APS application type")
        } finally {
            model.close()
        }
    }

    @Test
    fun csp1dRepresentativeModelingChainShouldCompile() {
        val model = LinearMetaModel("p7_csp1d")
        val abstractModel: AbstractLinearMetaModel<Flt64> = model
        try {
            val cut = UIntVariable2("csp_cut", Shape2(2, 3))
            val use = BinVariable1("csp_use", Shape1(3))
            assertOk(model.add(cut), "CSP1D UIntVariable2 should be accepted")
            assertOk(model.add(use), "CSP1D BinVariable1 should be accepted")

            assertTrue(cut[0, 1] belongsTo cut, "CSP1D variable item should belong to its combination")
            assertEquals(listOf(0, 1), cut[0, 1].vectorView.toList())

            val length: LinearExpressionSymbols2<Flt64> = SymbolCombination("csp_length", Shape2(2, 3)) { _, v ->
                LinearExpressionSymbol(cut[v[0], v[1]], name = "csp_length_${v[0]}_${v[1]}")
            }
            assertOk(model.add(length), "CSP1D LinearExpressionSymbols2 should be accepted")

            val masked: LinearIntermediateSymbols2<Flt64> = SymbolCombination("csp_masked", Shape2(2, 3)) { _, v ->
                MaskingFunction(
                    input = length[v[0], v[1]].toLinearPolynomial(),
                    mask = use[v[1]],
                    name = "csp_masked_${v[0]}_${v[1]}"
                )
            }
            assertOk(model.add(masked), "CSP1D MaskingFunction grid should be accepted")

            val pipeline = linearPipeline("csp_pattern_pipeline", use[0])
            val pipelines: PipelineList<AbstractLinearMetaModel<Flt64>> = listOf(pipeline)
            assertOk(pipelines(abstractModel), "CSP1D PipelineList should register and invoke")
            assertEquals(1, model.constraintsOfGroup(pipeline).size)
            assertOk(model.minimize(sum(listOf(cut[0, 0], cut[1, 2]))), "CSP1D sum objective should be accepted")

            // P10: intermediate symbol constraint — previously blocked by ClassCast
            assertOk(model.addConstraint(masked[0, 0] le Flt64(5.0), name = "csp_masked0_limit"),
                "CSP1D addConstraint with intermediate symbol should succeed")

            assertEquals(CuttingPlanProductOrder.Asc, CuttingPlanProductOrder.valueOf("Asc"))
        } finally {
            model.close()
        }
    }

    @Test
    fun bopRepresentativeModelingChainShouldCompile() {
        val model = QuadraticMetaModel("p7_bop")
        val abstractModel: AbstractQuadraticMetaModel<Flt64> = model
        try {
            val x = URealVariable1("bop_x", Shape1(2))
            val select = BinVariable2("bop_select", Shape2(2, 2))
            assertOk(model.add(x), "BOP URealVariable1 should be accepted")
            assertOk(model.add(select), "BOP BinVariable2 should be accepted")

            val use: QuadraticExpressionSymbols1<Flt64> = QuadraticIntermediateSymbols("bop_use", Shape1(2))
            assertOk(model.add(use), "BOP QuadraticIntermediateSymbols1 should be accepted")

            val absDiff = AbsFunction.fromLinearPolynomial(
                polynomial = x[0] - x[1],
                name = "bop_abs_diff"
            )
            val rangeSlack = SlackRangeFunction(
                x = x[0],
                lb = Flt64.zero,
                ub = Flt64.one,
                type = UContinuous,
                name = "bop_range_slack"
            )
            assertOk(model.add(absDiff), "BOP AbsFunction should be accepted")
            assertOk(model.add(rangeSlack), "BOP SlackRangeFunction should be accepted")

            val pipeline = quadraticPipeline("bop_quadratic_pipeline", QuadraticPolynomial(x[0]))
            val pipelines: PipelineList<AbstractQuadraticMetaModel<Flt64>> = listOf(pipeline)
            assertOk(pipelines(abstractModel), "BOP quadratic PipelineList should register and invoke")
            assertEquals(1, model.constraintsOfGroup(pipeline).size)

            val useSum = qsum(listOf(use[0], use[1]))
            assertEquals(2, useSum.monomials.size)
            val objective = QuadraticPolynomial(QuadraticMonomial.quadratic(Flt64.one, select[0, 0], select[1, 1]))
            assertOk(model.minimize(objective), "BOP quadratic objective should be accepted")

            // P12: quadratic constraint and mechanism model verification
            val lhsPoly = fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, select[0, 0], select[1, 1])),
                constant = Flt64.zero
            )
            val rhsPoly = fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial(
                monomials = emptyList(),
                constant = Flt64.one
            )
            val quadraticConstraint = QuadraticInequalityOf<Flt64>(
                lhs = lhsPoly,
                rhs = rhsPoly,
                comparison = Comparison.LE,
                name = "bop_select_product_limit"
            )
            assertOk(model.addConstraint(quadraticConstraint), "BOP quadratic inequality constraint should be accepted")

            @Suppress("DEPRECATION")
            val mechanismRet = runBlocking { QuadraticMechanismModel.invoke<Flt64>(metaModel = model) }
            assertTrue(mechanismRet is Ok<*, *, *>, "BOP QuadraticMechanismModel.invoke should succeed")
            val mechanismModel = requireNotNull(mechanismRet.value)
            assertTrue(mechanismModel.constraints.isNotEmpty(),
                "BOP mechanism model should have at least 1 constraint")
            assertTrue(mechanismModel.objectFunction.subObjects.isNotEmpty(),
                "BOP mechanism model should have objective sub-objects")
        } finally {
            model.close()
        }
    }

    @Test
    fun pspRepresentativeModelingAndSolverBuilderShapeShouldCompile() {
        val model = LinearMetaModel("p7_psp")
        val abstractModel: AbstractLinearMetaModel<Flt64> = model
        try {
            val charge = URealVariable1("psp_charge", Shape1(3))
            val on = BinVariable1("psp_on", Shape1(3))
            assertOk(model.add(charge), "PSP URealVariable1 should be accepted")
            assertOk(model.add(on), "PSP BinVariable1 should be accepted")

            val ceil: LinearIntermediateSymbols1<Flt64> = SymbolCombination("psp_ceil", Shape1(3)) { i, _ ->
                CeilingFunction.fromLinearPolynomial(
                    x = LinearPolynomial(charge[i]) + 0.2,
                    name = "psp_ceil_$i"
                )
            }
            assertOk(model.add(ceil), "PSP CeilingFunction grid should be accepted")

            val slack: LinearIntermediateSymbols1<Flt64> = SymbolCombination("psp_slack", Shape1(3)) { i, _ ->
                SlackFunction(
                    x = ceil[i],
                    threshold = Flt64.one,
                    type = UInteger,
                    withPositive = true,
                    name = "psp_slack_$i"
                )
            }
            assertOk(model.add(slack), "PSP threshold SlackFunction grid should be accepted")

            val binary: LinearIntermediateSymbols1<Flt64> = SymbolCombination("psp_binary", Shape1(3)) { i, _ ->
                BinaryzationFunction.fromLinearPolynomial(
                    polynomial = LinearPolynomial(charge[i]),
                    name = "psp_binary_$i"
                )
            }
            assertOk(model.add(binary), "PSP BinaryzationFunction grid should be accepted")

            val pipeline = linearPipeline("psp_energy_pipeline", on[0])
            val pipelines: PipelineList<AbstractLinearMetaModel<Flt64>> = listOf(pipeline)
            assertOk(pipelines(abstractModel), "PSP PipelineList should register and invoke")
            assertEquals(1, model.constraintsOfGroup(pipeline).size)
            assertOk(model.minimize(sum(listOf(charge[0], charge[1], charge[2]))), "PSP objective should be accepted")

            val config = SolverConfig(time = 5.seconds)
            assertEquals(5.seconds, config.time)
            pluginSolverBuilderConstructorsCompileOnly(config)
        } finally {
            model.close()
        }
    }

    private fun linearPipeline(
        pipelineName: String,
        item: fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>
    ): Pipeline<AbstractLinearMetaModel<Flt64>> {
        return object : Pipeline<AbstractLinearMetaModel<Flt64>> {
            override val name = pipelineName

            override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
                return model.addConstraint(item, group = this, name = "${pipelineName}_constraint")
            }
        }
    }

    private fun quadraticPipeline(
        pipelineName: String,
        polynomial: fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial<Flt64>
    ): Pipeline<AbstractQuadraticMetaModel<Flt64>> {
        return object : Pipeline<AbstractQuadraticMetaModel<Flt64>> {
            override val name = pipelineName

            override fun invoke(model: AbstractQuadraticMetaModel<Flt64>): Try {
                return model.addConstraint(polynomial, group = this, name = "${pipelineName}_constraint")
            }
        }
    }

    private fun pluginSolverBuilderConstructorsCompileOnly(config: SolverConfig) {
        if (System.getProperty("ospf.p7.instantiate.plugin.solvers") == "true") {
            val linearByGurobi: LinearSolver = GurobiLinearSolver(config)
            val linearByScip: LinearSolver = ScipLinearSolver(config)
            val quadraticByGurobi: QuadraticSolver = GurobiQuadraticSolver(config)
            val quadraticByScip: QuadraticSolver = ScipQuadraticSolver(config)
            val columnByGurobi: ColumnGenerationSolver = GurobiColumnGenerationSolver(config)
            val columnByScip: ColumnGenerationSolver = ScipColumnGenerationSolver(config)
            assertEquals(
                listOf("gurobi", "scip", "gurobi", "scip", "gurobi", "scip"),
                listOf(
                    linearByGurobi.name,
                    linearByScip.name,
                    quadraticByGurobi.name,
                    quadraticByScip.name,
                    columnByGurobi.name,
                    columnByScip.name
                )
            )
        }
    }

    private fun assertOk(result: Try, message: String) {
        assertTrue(result is Ok, message)
    }
}
