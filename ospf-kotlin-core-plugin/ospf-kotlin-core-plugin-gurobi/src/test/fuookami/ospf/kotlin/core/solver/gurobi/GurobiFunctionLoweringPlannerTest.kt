package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.core.variable.RealVar

class GurobiFunctionLoweringPlannerTest {
    private val input = RealVar("planner_input")
    private val result = RealVar("planner_result")
    private val capabilities = FunctionSolverCapabilities(
        solver = "gurobi",
        supported = setOf(FunctionNativeCapability.PiecewiseLinearObjective, FunctionNativeCapability.PiecewiseLinearConstraint)
    )

    @Test
    fun objectiveOnlyStillPreservesResultRelation() {
        val model = model(structure())
        val plan = planGurobiFunctionLowering(model, capabilities).single()
        assertTrue(plan.decision.useNative)
        assertEquals(FunctionNativeCapability.PiecewiseLinearConstraint, plan.decision.capability)
        val objectiveOnly = capabilities.copy(supported = setOf(FunctionNativeCapability.PiecewiseLinearObjective))
        assertFalse(planGurobiFunctionLowering(model, objectiveOnly).single().decision.useNative)
    }

    @Test
    fun unsafePiecewiseShapesFallBack() {
        val original = structure()
        val invalid = listOf(
            original.copy(input = LinearPolynomial(listOf(LinearMonomial(Flt64.two, input)), Flt64.one)),
            original.copy(breakpoints = listOf(Flt64.zero, Flt64.two, Flt64.one)),
            original.copy(slopes = listOf(Flt64.one)),
            original.copy(intercepts = listOf(Flt64.zero, Flt64.zero)),
            original.copy(slopes = listOf(Flt64.infinity, Flt64.two)),
            original.copy(converter = null)
        )
        for (structure in invalid) {
            assertFalse(planGurobiFunctionLowering(model(structure), capabilities).single().decision.useNative)
        }
        assertFalse(planGurobiFunctionLowering(model(original, upperBound = Flt64(3.0)), capabilities).single().decision.useNative)
    }

    @Test
    fun eagerAndAlreadyMaterializedModelsFallBack() {
        val structure = structure()
        val model = model(structure)
        assertFalse(planGurobiFunctionLowering(
            model.copy(functionExpansionPolicy = FunctionExpansionPolicy.EAGER),
            capabilities
        ).single().decision.useNative)
        assertFalse(planGurobiFunctionLowering(
            model.copy(deferredFunctionConstraintRegions = listOf(DeferredFunctionConstraintRegion(
                structure = structure,
                firstConstraintIndex = 0,
                constraintCount = 1
            ))),
            capabilities
        ).single().decision.useNative)
    }

    @Test
    fun capabilityDetectionRequiresExactSdkSignatures() {
        val detected = gurobiFunctionSolverCapabilities()
        assertTrue(detected.supports(FunctionNativeCapability.PiecewiseLinearConstraint))
        assertTrue(detected.supports(FunctionNativeCapability.SOS1))
        assertTrue(detected.supports(FunctionNativeCapability.Indicator))
        assertTrue(gurobiFunctionSolverCapabilities(WrongSignature::class.java).supported.isEmpty())
    }

    private fun structure() = UnivariateLinearPiecewiseStructure(
        input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
        breakpoints = listOf(Flt64.zero, Flt64.one, Flt64.two),
        slopes = listOf(Flt64.one, Flt64.two),
        intercepts = listOf(Flt64.zero, -Flt64.one),
        resultVariable = result,
        selectorVariables = emptyList(),
        usage = FunctionUsageSummary(inObjective = true),
        converter = IntoValue.Identity
    )

    private fun model(
        structure: UnivariateLinearPiecewiseStructure<Flt64>,
        upperBound: Flt64 = Flt64.two
    ): LinearTriadModel {
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = listOf(input, result).mapIndexed { index, variable ->
                    Variable(
                        index = index,
                        lowerBound = Flt64.zero,
                        upperBound = upperBound,
                        type = Continuous,
                        origin = variable,
                        name = variable.name
                    )
                },
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "function-planner"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(category = ObjectCategory.Minimum, objective = emptyList()),
            functionExpansionPolicy = FunctionExpansionPolicy.AUTO,
            deferredFunctionStructures = listOf(structure)
        )
    }

    @Suppress("UNUSED_PARAMETER")
    class WrongSignature {
        fun addGenConstrPWL(first: String, second: String, third: String, fourth: String, fifth: String): String = ""
    }
}
