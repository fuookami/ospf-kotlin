package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model.*

/** 手算验证广义流，不依赖原生求解器。 / Verify generalized flow with hand-calculated witnesses, without native solvers. */
class GeneralizedFlowContextTest {
    @Test
    fun handCalculatedLossPathChargesAndBalancesInputAndArrivalOutput() {
        val source = node(
            id = "s",
            unit = Kilogram,
            netSupply = 10.0
        )
        val middle = node(
            id = "m",
            unit = Kilogram,
            netSupply = 0.0
        )
        val sink = node(
            id = "t",
            unit = Kilogram,
            netSupply = -6.0
        )
        val first = arc(
            id = "sm",
            from = "s",
            to = "m",
            tariff = 2.0,
            inputUnit = Kilogram
        )
        val second = arc(
            id = "mt",
            from = "m",
            to = "t",
            tariff = 3.0,
            inputUnit = Kilogram
        )
        val arcs = listOf(
            generalizedArc(
                networkArc = first,
                upperInput = 10.0,
                inputUnit = Kilogram,
                yield = 0.8
            ),
            generalizedArc(
                networkArc = second,
                upperInput = 8.0,
                inputUnit = Kilogram,
                yield = 0.75
            )
        )
        val problem = problem(
            nodes = listOf(source, middle, sink),
            arcs = arcs,
            costUnit = NoneUnit
        )
        val context = GeneralizedFlowContext(
            problem = problem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "loss_path"
        )
        val model = LinearMetaModel(name = "loss_path")
        assertTrue(context.register(model).ok)

        val witness = model.tokens.tokensInSolver.map { token ->
            when (token.variable.name.substringAfter("_input_")) {
                "sm" -> Flt64(10.0)
                "mt" -> Flt64(8.0)
                else -> Flt64.zero
            }
        }
        assertTrue(model.constraints.all { it.isTrue(witness, model.tokens, false) == true })
        model.setSolution(witness)

        val solution = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(44.0), solution.totalCost.value)
        assertEquals(Flt64(10.0), solution.arcs.getValue(NetworkArcId("sm")).input.value)
        assertEquals(Flt64(10.0), solution.arcs.getValue(NetworkArcId("sm")).unitConvertedOutput.value)
        assertEquals(Flt64(8.0), solution.arcs.getValue(NetworkArcId("sm")).output.value)
        assertEquals(Flt64(2.0), solution.arcs.getValue(NetworkArcId("sm")).lossOrGain.value)
        assertEquals(Flt64(24.0), solution.arcs.getValue(NetworkArcId("mt")).cost.value)
        assertTrue(solution.nodes.values.all { it.residual.value eq Flt64.zero })
        assertEquals(Flt64(8.0), solution.nodes.getValue(NetworkNodeId("m")).incomingOutput.value)
        assertEquals(Flt64(8.0), solution.nodes.getValue(NetworkNodeId("m")).outgoingInput.value)
    }

    @Test
    fun boundedLossGainCircuitAllowsOnlyFiniteCirculation() {
        val a = node(
            id = "a",
            unit = NoneUnit,
            netSupply = 0.0
        )
        val b = node(
            id = "b",
            unit = NoneUnit,
            netSupply = 0.0
        )
        val ab = arc(
            id = "ab",
            from = "a",
            to = "b",
            tariff = 0.0,
            inputUnit = NoneUnit
        )
        val ba = arc(
            id = "ba",
            from = "b",
            to = "a",
            tariff = 0.0,
            inputUnit = NoneUnit
        )
        val arcs = listOf(
            generalizedArc(
                networkArc = ab,
                upperInput = 3.0,
                inputUnit = NoneUnit,
                yield = 2.0
            ),
            generalizedArc(
                networkArc = ba,
                upperInput = 6.0,
                inputUnit = NoneUnit,
                yield = 0.5
            )
        )
        val context = GeneralizedFlowContext(
            problem = problem(
                nodes = listOf(a, b),
                arcs = arcs,
                costUnit = NoneUnit
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "bounded_circuit"
        )
        val model = LinearMetaModel(name = "bounded_circuit")
        assertTrue(context.register(model, minimizeCost = false).ok)

        val witness = model.tokens.tokensInSolver.map { token ->
            if (token.variable.name.endsWith("_ab")) Flt64(3.0) else Flt64(6.0)
        }
        assertTrue(model.constraints.all { it.isTrue(witness, model.tokens, false) == true })
        val overCapacity = model.tokens.tokensInSolver.map { token ->
            if (token.variable.name.endsWith("_ab")) Flt64(3.1) else Flt64(6.2)
        }
        assertFalse(model.constraints.all { it.isTrue(overCapacity, model.tokens, false) == true })
        model.setSolution(witness)

        val solution = assertNotNull(context.extractSolution(model).value)
        val gainArc = solution.arcs.getValue(NetworkArcId("ab"))
        val lossArc = solution.arcs.getValue(NetworkArcId("ba"))
        assertEquals(Flt64(6.0), gainArc.output.value)
        assertEquals(Flt64(-3.0), gainArc.lossOrGain.value)
        assertEquals(Flt64(3.0), lossArc.lossOrGain.value)
        assertTrue(solution.nodes.values.all { it.residual.value eq Flt64.zero })
    }

    @Test
    fun sameDimensionUnitConversionIsSeparateFromYieldAndCrossDimensionNeedsExplicitRatio() {
        val grams = node(
            id = "grams",
            unit = Gram,
            netSupply = 2000.0
        )
        val kilograms = node(
            id = "kilograms",
            unit = Kilogram,
            netSupply = -1.0
        )
        val gramArc = arc(
            id = "gram_to_kilogram",
            from = "grams",
            to = "kilograms",
            tariff = 0.01,
            inputUnit = Gram
        )
        val gramFlow = generalizedArc(
            networkArc = gramArc,
            upperInput = 2000.0,
            inputUnit = Gram,
            yield = 0.5
        )
        val gramContext = GeneralizedFlowContext(
            problem = problem(
                nodes = listOf(grams, kilograms),
                arcs = listOf(gramFlow),
                costUnit = NoneUnit
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "metric_conversion"
        )
        val gramModel = LinearMetaModel(name = "metric_conversion")
        assertTrue(gramContext.register(gramModel).ok)
        gramModel.setSolution(listOf(Flt64(2000.0)))
        val gramResult = assertNotNull(gramContext.extractSolution(gramModel).value)
        val gramArcResult = gramResult.arcs.getValue(gramArc.id)
        assertEquals(Flt64(2.0), gramArcResult.unitConvertedOutput.value)
        assertEquals(Flt64(1.0), gramArcResult.output.value)
        assertEquals(Flt64(1.0), gramArcResult.lossOrGain.value)
        assertEquals(Flt64(20.0), gramResult.totalCost.value)
        assertTrue(gramResult.nodes.values.all { it.residual.value eq Flt64.zero })

        val liters = node(
            id = "liters",
            unit = Liter,
            netSupply = 1.0
        )
        val mass = node(
            id = "mass",
            unit = Kilogram,
            netSupply = -1.0
        )
        val volumeArc = arc(
            id = "liter_to_mass",
            from = "liters",
            to = "mass",
            tariff = 4.0,
            inputUnit = Liter
        )
        val missingRatio = generalizedArc(
            networkArc = volumeArc,
            upperInput = 1.0,
            inputUnit = Liter,
            yield = 0.5
        )
        val invalidContext = GeneralizedFlowContext(
            problem = problem(
                nodes = listOf(liters, mass),
                arcs = listOf(missingRatio),
                costUnit = NoneUnit
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "missing_ratio"
        )
        assertFalse(invalidContext.register(LinearMetaModel(name = "missing_ratio")).ok)

        val explicitRatio = generalizedArc(
            networkArc = volumeArc,
            upperInput = 1.0,
            inputUnit = Liter,
            yield = 0.5,
            inputToOutputFactor = Quantity(Flt64(2.0), Kilogram / Liter)
        )
        val massContext = GeneralizedFlowContext(
            problem = problem(
                nodes = listOf(liters, mass),
                arcs = listOf(explicitRatio),
                costUnit = NoneUnit
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "explicit_mass_yield"
        )
        val massModel = LinearMetaModel(name = "explicit_mass_yield")
        assertTrue(massContext.register(massModel).ok)
        massModel.setSolution(listOf(Flt64.one))
        val massResult = assertNotNull(massContext.extractSolution(massModel).value)
        val converted = massResult.arcs.getValue(volumeArc.id)
        assertEquals(Flt64(2.0), converted.unitConvertedOutput.value)
        assertEquals(Flt64.one, converted.output.value)
        assertEquals(Flt64.one, converted.lossOrGain.value)
        assertTrue(massResult.nodes.values.all { it.residual.value eq Flt64.zero })
    }

    @Test
    fun fltXAdapterWorksAndSolverConversionFailureStaysInRet() {
        val source = assertNotNull(NetworkNode<FltX>(NetworkNodeId("source")).value)
        val sink = assertNotNull(NetworkNode<FltX>(NetworkNodeId("sink")).value)
        val networkArc = assertNotNull(NetworkArc(
            from = source.id,
            to = sink.id,
            cost = Quantity(FltX("2"), NoneUnit),
            id = NetworkArcId("fltx_arc")
        ).value)
        val sourceNode = assertNotNull(GeneralizedFlowNode(
            networkNode = source,
            unit = NoneUnit,
            netSupply = Quantity(FltX("2"), NoneUnit)
        ).value)
        val sinkNode = assertNotNull(GeneralizedFlowNode(
            networkNode = sink,
            unit = NoneUnit,
            netSupply = Quantity(FltX("-2"), NoneUnit)
        ).value)
        val capacity = assertNotNull(CapacityBounds(
            Quantity(FltX("0"), NoneUnit),
            Quantity(FltX("3"), NoneUnit)
        ).value)
        val flowArc = assertNotNull(GeneralizedFlowArc(networkArc, capacity).value)
        val problem = GeneralizedFlowProblem(
            graph = assertNotNull(NetworkGraph(listOf(source, sink), listOf(networkArc)).value),
            nodes = listOf(sourceNode, sinkNode),
            arcs = listOf(flowArc),
            zero = FltX("0"),
            costUnit = NoneUnit
        )
        val context = GeneralizedFlowContext(
            problem = problem,
            adapter = FltXNetworkSchedulingSolverValueAdapter,
            name = "fltx"
        )
        val model = LinearMetaModel(name = "fltx")
        assertTrue(context.register(model).ok)
        model.setSolution(listOf(Flt64(2.0)))
        val solution = assertNotNull(context.extractSolution(model).value)
        assertEquals(FltX("2"), solution.arcs.getValue(networkArc.id).input.value)
        assertEquals(FltX("4"), solution.totalCost.value)

        val failingAdapter = object : NetworkSchedulingSolverValueAdapter<Flt64> {
            override fun toSolverValue(value: Flt64): Ret<Flt64> {
                return networkSchedulingFailure("测试转换失败 / Test conversion failure")
            }

            override fun fromSolverValue(value: Flt64): Ret<Flt64> {
                return Flt64NetworkSchedulingSolverValueAdapter.fromSolverValue(value)
            }
        }
        val failingContext = GeneralizedFlowContext(
            problem = problem(
                nodes = listOf(
                    node(id = "s", unit = NoneUnit, netSupply = 1.0),
                    node(id = "t", unit = NoneUnit, netSupply = -1.0)
                ),
                arcs = listOf(generalizedArc(
                    networkArc = arc(id = "st", from = "s", to = "t", tariff = 1.0, inputUnit = NoneUnit),
                    upperInput = 1.0,
                    inputUnit = NoneUnit,
                    yield = 1.0
                )),
                costUnit = NoneUnit
            ),
            adapter = failingAdapter,
            name = "bad_conversion"
        )
        assertFalse(failingContext.register(LinearMetaModel(name = "bad_conversion")).ok)
    }

    @Test
    fun extractionRejectsNegativeInputsCapacityViolationsAndUnbalancedWitnesses() {
        val negativeContext = twoNodeContext(
            name = "negative_input",
            sourceSupply = 1.0,
            sinkNetSupply = -1.0,
            upperInput = 1.0
        )
        val negativeModel = LinearMetaModel(name = "negative_input")
        assertTrue(negativeContext.register(negativeModel).ok)
        negativeModel.setSolution(listOf(Flt64(-0.1)))
        assertFalse(negativeContext.extractSolution(negativeModel).ok)

        val capacityContext = twoNodeContext(
            name = "over_capacity",
            sourceSupply = 1.0,
            sinkNetSupply = -1.0,
            upperInput = 1.0
        )
        val capacityModel = LinearMetaModel(name = "over_capacity")
        assertTrue(capacityContext.register(capacityModel).ok)
        capacityModel.setSolution(listOf(Flt64(1.1)))
        assertFalse(capacityContext.extractSolution(capacityModel).ok)

        val balanceContext = twoNodeContext(
            name = "wrong_balance",
            sourceSupply = 1.0,
            sinkNetSupply = -2.0,
            upperInput = 2.0
        )
        val balanceModel = LinearMetaModel(name = "wrong_balance")
        assertTrue(balanceContext.register(balanceModel).ok)
        balanceModel.setSolution(listOf(Flt64.one))
        assertFalse(balanceContext.extractSolution(balanceModel).ok)
    }

    @Test
    fun solverToleranceAppliesAtCapacityAndBalanceExtractionBoundaries() {
        val relaxedContext = twoNodeContext(
            name = "relaxed_tolerance",
            sourceSupply = 1.0,
            sinkNetSupply = -1.0,
            upperInput = 1.0,
            solverTolerance = GeneralizedFlowSolverTolerance(
                absoluteTolerance = Flt64(1e-6),
                relativeTolerance = Flt64.zero
            )
        )
        val relaxedModel = LinearMetaModel(name = "relaxed_tolerance")
        assertTrue(relaxedContext.register(relaxedModel).ok)
        relaxedModel.setSolution(listOf(Flt64(1.0000005)))
        val relaxedSolution = assertNotNull(relaxedContext.extractSolution(relaxedModel).value)
        assertTrue(relaxedSolution.nodes.values.all { it.residual.value.abs() <= Flt64(1e-6) })

        val invalidToleranceContext = twoNodeContext(
            name = "negative_tolerance",
            sourceSupply = 1.0,
            sinkNetSupply = -1.0,
            upperInput = 1.0,
            solverTolerance = GeneralizedFlowSolverTolerance(
                absoluteTolerance = Flt64(-1e-6),
                relativeTolerance = Flt64.zero
            )
        )
        assertFalse(invalidToleranceContext.register(LinearMetaModel(name = "negative_tolerance")).ok)
    }

    @Test
    fun smallDiscreteOracleFindsTheKnownGeneralizedFlowOptimum() {
        var bestCost = Double.POSITIVE_INFINITY
        var bestInputs: Pair<Double, Double>? = null
        for (lossyStep in 0..8) {
            val lossyInput = lossyStep / 2.0
            for (standardStep in 0..8) {
                val standardInput = standardStep / 2.0
                val sourceResidual = lossyInput + standardInput - 4.0
                val sinkResidual = -(0.5 * lossyInput + standardInput) - (-3.0)
                if (kotlin.math.abs(sourceResidual) <= 1e-9 && kotlin.math.abs(sinkResidual) <= 1e-9) {
                    val cost = lossyInput + 3.0 * standardInput
                    if (cost < bestCost) {
                        bestCost = cost
                        bestInputs = lossyInput to standardInput
                    }
                }
            }
        }

        assertEquals(8.0, bestCost, 1e-9)
        assertEquals(2.0 to 2.0, assertNotNull(bestInputs))
    }

    @Test
    fun registrationRejectsNegativeYieldAndInfiniteInputCapacity() {
        val source = node(
            id = "s",
            unit = NoneUnit,
            netSupply = 1.0
        )
        val sink = node(
            id = "t",
            unit = NoneUnit,
            netSupply = -1.0
        )
        val serviceArc = arc(
            id = "st",
            from = "s",
            to = "t",
            tariff = 1.0,
            inputUnit = NoneUnit
        )
        val negativeYield = generalizedArc(
            networkArc = serviceArc,
            upperInput = 1.0,
            inputUnit = NoneUnit,
            yield = -1.0
        )
        val invalidYieldContext = GeneralizedFlowContext(
            problem = problem(
                nodes = listOf(source, sink),
                arcs = listOf(negativeYield),
                costUnit = NoneUnit
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "negative_yield"
        )
        assertFalse(invalidYieldContext.register(LinearMetaModel(name = "negative_yield")).ok)

        val infiniteCapacityContext = twoNodeContext(
            name = "infinite_capacity",
            sourceSupply = 1.0,
            sinkNetSupply = -1.0,
            upperInput = Double.POSITIVE_INFINITY
        )
        assertFalse(infiniteCapacityContext.register(LinearMetaModel(name = "infinite_capacity")).ok)
    }

    @Test
    fun selfLoopUsesItsArrivalOutputInGeneralizedBalance() {
        val self = node(
            id = "self",
            unit = NoneUnit,
            netSupply = -4.0
        )
        val loopArc = arc(
            id = "self_loop",
            from = "self",
            to = "self",
            tariff = 2.0,
            inputUnit = NoneUnit
        )
        val loop = generalizedArc(
            networkArc = loopArc,
            upperInput = 4.0,
            inputUnit = NoneUnit,
            yield = 2.0
        )
        val context = GeneralizedFlowContext(
            problem = problem(
                nodes = listOf(self),
                arcs = listOf(loop),
                costUnit = NoneUnit
            ),
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "self_loop"
        )
        val model = LinearMetaModel(name = "self_loop")
        assertTrue(context.register(model).ok)
        model.setSolution(listOf(Flt64(4.0)))

        val solution = assertNotNull(context.extractSolution(model).value)
        assertEquals(Flt64(8.0), solution.arcs.getValue(loopArc.id).output.value)
        assertEquals(Flt64(-4.0), solution.nodes.getValue(self.id).realizedNetSupply.value)
        assertTrue(solution.nodes.getValue(self.id).residual.value eq Flt64.zero)
    }

    @Test
    fun solverToDomainConversionFailureRemainsInRetDuringExtraction() {
        val failingAdapter = object : NetworkSchedulingSolverValueAdapter<Flt64> {
            override fun toSolverValue(value: Flt64): Ret<Flt64> {
                return Flt64NetworkSchedulingSolverValueAdapter.toSolverValue(value)
            }

            override fun fromSolverValue(value: Flt64): Ret<Flt64> {
                return networkSchedulingFailure("结果转换失败 / Result conversion failed")
            }
        }
        val context = twoNodeContext(
            name = "failed_extraction_conversion",
            sourceSupply = 1.0,
            sinkNetSupply = -1.0,
            upperInput = 1.0,
            adapter = failingAdapter
        )
        val model = LinearMetaModel(name = "failed_extraction_conversion")
        assertTrue(context.register(model).ok)
        model.setSolution(listOf(Flt64.one))

        assertFalse(context.extractSolution(model).ok)
    }

    @Test
    fun contextsRegisterIndependentlyAndExtensionsConstrainInputExpressions() {
        val node = node(
            id = "shared",
            unit = NoneUnit,
            netSupply = 0.0
        )
        val loop = arc(
            id = "loop",
            from = "shared",
            to = "shared",
            tariff = 0.0,
            inputUnit = NoneUnit
        )
        val flowArc = generalizedArc(
            networkArc = loop,
            upperInput = 5.0,
            inputUnit = NoneUnit,
            yield = 1.0
        )
        val immutableProblem = problem(
            nodes = listOf(node),
            arcs = listOf(flowArc),
            costUnit = NoneUnit
        )
        val extension = GeneralizedFlowPipelineFactory<Flt64> { expressions ->
            object : Pipeline<AbstractLinearMetaModel<Flt64>> {
                override val name = "business_input_limit"

                override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
                    return when (val input = expressions.input(loop.id)) {
                        is Ok -> model.addConstraint(input.value leq Flt64(2.0), name = name)
                        is Failed -> Failed(input.error)
                        is Fatal -> Fatal(input.errors)
                    }
                }
            }
        }
        val limited = GeneralizedFlowContext(
            immutableProblem,
            Flt64NetworkSchedulingSolverValueAdapter,
            name = "limited",
            extraPipelineFactories = listOf(extension)
        )
        val plain = GeneralizedFlowContext(
            problem = immutableProblem,
            adapter = Flt64NetworkSchedulingSolverValueAdapter,
            name = "plain"
        )
        val limitedModel = LinearMetaModel(name = "limited")
        val plainModel = LinearMetaModel(name = "plain")
        assertTrue(limited.register(limitedModel).ok)
        assertTrue(plain.register(plainModel).ok)
        assertNotSame(
            limitedModel.tokens.tokensInSolver.single().variable,
            plainModel.tokens.tokensInSolver.single().variable
        )
        assertFalse(limited.register(plainModel).ok)
        assertTrue(limited.register(limitedModel).ok)

        val overBusinessLimit = listOf(Flt64(3.0))
        assertFalse(limitedModel.constraints.all { it.isTrue(overBusinessLimit, limitedModel.tokens, false) == true })
        assertTrue(plainModel.constraints.all { it.isTrue(overBusinessLimit, plainModel.tokens, false) == true })
    }

    private fun problem(
        nodes: List<GeneralizedFlowNode<Flt64>>,
        arcs: List<GeneralizedFlowArc<Flt64>>,
        costUnit: PhysicalUnit
    ): GeneralizedFlowProblem<Flt64> = GeneralizedFlowProblem(
        graph = assertNotNull(NetworkGraph(nodes.map { it.networkNode }, arcs.map { it.networkArc }).value),
        nodes = nodes,
        arcs = arcs,
        zero = Flt64.zero,
        costUnit = costUnit
    )

    private fun node(id: String, unit: PhysicalUnit, netSupply: Double): GeneralizedFlowNode<Flt64> {
        val networkNode = assertNotNull(NetworkNode<Flt64>(NetworkNodeId(id)).value)
        return assertNotNull(GeneralizedFlowNode(
            networkNode = networkNode,
            unit = unit,
            netSupply = Quantity(Flt64(netSupply), unit)
        ).value)
    }

    private fun arc(
        id: String,
        from: String,
        to: String,
        tariff: Double,
        inputUnit: PhysicalUnit
    ): NetworkArc<Flt64> {
        return assertNotNull(NetworkArc(
            from = NetworkNodeId(from),
            to = NetworkNodeId(to),
            cost = Quantity(Flt64(tariff), NoneUnit / inputUnit),
            id = NetworkArcId(id)
        ).value)
    }

    private fun generalizedArc(
        networkArc: NetworkArc<Flt64>,
        upperInput: Double,
        inputUnit: PhysicalUnit,
        yield: Double,
        inputToOutputFactor: Quantity<Flt64>? = null
    ): GeneralizedFlowArc<Flt64> {
        val capacity = assertNotNull(CapacityBounds(
            Quantity(Flt64.zero, inputUnit),
            Quantity(Flt64(upperInput), inputUnit)
        ).value)
        return assertNotNull(GeneralizedFlowArc(
            networkArc = networkArc,
            inputCapacity = capacity,
            productionYield = Quantity(Flt64(yield), NoneUnit),
            inputToOutputFactor = inputToOutputFactor
        ).value)
    }

    private fun twoNodeContext(
        name: String,
        sourceSupply: Double,
        sinkNetSupply: Double,
        upperInput: Double,
        solverTolerance: GeneralizedFlowSolverTolerance = GeneralizedFlowSolverTolerance(),
        adapter: NetworkSchedulingSolverValueAdapter<Flt64> = Flt64NetworkSchedulingSolverValueAdapter
    ): GeneralizedFlowContext<Flt64> {
        val source = node(
            id = "source",
            unit = NoneUnit,
            netSupply = sourceSupply
        )
        val sink = node(
            id = "sink",
            unit = NoneUnit,
            netSupply = sinkNetSupply
        )
        val serviceArc = arc(
            id = "service",
            from = "source",
            to = "sink",
            tariff = 1.0,
            inputUnit = NoneUnit
        )
        val flowArc = generalizedArc(
            networkArc = serviceArc,
            upperInput = upperInput,
            inputUnit = NoneUnit,
            yield = 1.0
        )
        return GeneralizedFlowContext(
            problem = problem(
                nodes = listOf(source, sink),
                arcs = listOf(flowArc),
                costUnit = NoneUnit
            ),
            adapter = adapter,
            name = name,
            solverTolerance = solverTolerance
        )
    }
}
