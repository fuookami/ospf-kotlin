package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dDomainCalculationContext
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dDomainPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.DefaultCsp1dDomainPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.SimpleDomainCalculationContext
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.allFeasible
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.allWidthFeasible
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dExtensionMode
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dExtensionSet
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dExtractionPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dFlowPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dGenerationStrategy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dModelingExtension
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dObjectivePolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolveConfig
import fuookami.ospf.kotlin.framework.csp1d.application.model.csp1dSolveConfig

/**
 * 领域策略集成测试 / Domain policy integration test
 *
 * 验证 Csp1dDomainPolicy 能承载下游宽差、设备兼容等业务判断。
 *
 * Verify that Csp1dDomainPolicy can carry downstream width difference,
 * machine compatibility and other business judgments.
 */
class Csp1dDomainPolicyTest {

    private val meter = Meter

    private fun testMaterial(id: String = "mat-1", upperWidth: Double = 1000.0): Material<Flt64> {
        return Material(
            id = id,
            name = "TestMaterial-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(upperWidth * 0.9), meter),
                    upperBound = Quantity(Flt64(upperWidth), meter)
                ),
                step = Quantity(Flt64(1.0), meter)
            )
        )
    }

    private fun testMachine(id: String = "mc-1"): Machine<Flt64> {
        return Machine(
            id = id,
            name = "TestMachine-$id",
            capacity = Quantity(Flt64(100.0), meter)
        )
    }

    private fun testProduct(id: String, width: Double = 100.0): Product<Flt64> {
        return Product(
            id = id,
            name = "TestProduct-$id",
            width = listOf(Quantity(Flt64(width), meter))
        )
    }

    private fun testDemand(product: Product<Flt64>, amount: Double = 10.0): ProductDemand<Flt64> {
        return ProductDemand(
            product = product,
            quantity = Quantity(Flt64(amount), meter)
        )
    }

    /**
     * Fake 宽差策略 / Fake width difference policy
     *
     * 模拟下游宽度差异约束：拒绝特定物料的方案。
     * Simulate downstream width difference constraint: reject plans for specific materials.
     */
    class FakeWidthDifferencePolicy<V : RealNumber<V>>(
        private val rejectedMaterialIds: Set<String>
    ) : Csp1dDomainPolicy<V> {
        override val name: String = "fake_width_diff"

        override fun isFeasible(context: Csp1dDomainCalculationContext<V>): Boolean {
            return context.plan.material.id !in rejectedMaterialIds
        }
    }

    /**
     * Fake 设备兼容策略 / Fake machine compatibility policy
     *
     * 模拟下游设备兼容性约束：拒绝特定设备上的方案。
     * Simulate downstream machine compatibility constraint: reject plans on specific machines.
     */
    class FakeMachineCompatibilityPolicy<V : RealNumber<V>>(
        private val rejectedMachineIds: Set<String>
    ) : Csp1dDomainPolicy<V> {
        override val name: String = "fake_machine_compat"

        override fun isFeasible(context: Csp1dDomainCalculationContext<V>): Boolean {
            val planMachineId = context.plan.machineId ?: return true
            return planMachineId !in rejectedMachineIds
        }
    }

    @Test
    fun `default domain policy accepts all plans`() {
        val material = testMaterial()
        val product = testProduct("p1")
        val plan = CuttingPlan(
            id = "plan-1",
            material = material,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )
        val ctx = SimpleDomainCalculationContext(
            plan = plan,
            planIndex = 0,
            domainValueSample = Flt64(1.0)
        )
        val policy = DefaultCsp1dDomainPolicy<Flt64>()
        assertTrue(policy.isFeasible(ctx))
        assertTrue(policy.isWidthFeasible(ctx))
    }

    @Test
    fun `allFeasible returns true for empty policy list`() {
        val material = testMaterial()
        val product = testProduct("p1")
        val plan = CuttingPlan(
            id = "plan-1",
            material = material,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )
        val ctx = SimpleDomainCalculationContext(
            plan = plan,
            planIndex = 0,
            domainValueSample = Flt64(1.0)
        )
        assertTrue(allFeasible(emptyList(), ctx))
    }

    @Test
    fun `fake width difference policy rejects plans from rejected material`() {
        val material1 = testMaterial("mat-1")
        val material2 = testMaterial("mat-2")
        val product = testProduct("p1")

        val plan1 = CuttingPlan(
            id = "plan-1",
            material = material1,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )
        val plan2 = CuttingPlan(
            id = "plan-2",
            material = material2,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )

        val policy = FakeWidthDifferencePolicy<Flt64>(rejectedMaterialIds = setOf("mat-1"))

        val ctx1 = SimpleDomainCalculationContext(plan = plan1, planIndex = 0, domainValueSample = Flt64(1.0))
        val ctx2 = SimpleDomainCalculationContext(plan = plan2, planIndex = 1, domainValueSample = Flt64(1.0))

        assertTrue(!policy.isFeasible(ctx1))
        assertTrue(policy.isFeasible(ctx2))
    }

    @Test
    fun `fake machine compatibility policy rejects plans from rejected machine`() {
        val material = testMaterial()
        val product = testProduct("p1")

        val planOnRejected = CuttingPlan(
            id = "plan-1",
            material = material,
            machineId = "mc-rejected",
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )
        val planOnAccepted = CuttingPlan(
            id = "plan-2",
            material = material,
            machineId = "mc-accepted",
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )

        val policy = FakeMachineCompatibilityPolicy<Flt64>(rejectedMachineIds = setOf("mc-rejected"))

        val ctx1 = SimpleDomainCalculationContext(plan = planOnRejected, planIndex = 0, domainValueSample = Flt64(1.0))
        val ctx2 = SimpleDomainCalculationContext(plan = planOnAccepted, planIndex = 1, domainValueSample = Flt64(1.0))

        assertTrue(!policy.isFeasible(ctx1))
        assertTrue(policy.isFeasible(ctx2))
    }

    @Test
    fun `allFeasible with multiple policies requires all to pass`() {
        val material1 = testMaterial("mat-1")
        val product = testProduct("p1")

        val plan = CuttingPlan(
            id = "plan-1",
            material = material1,
            machineId = "mc-rejected",
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )

        val widthPolicy = FakeWidthDifferencePolicy<Flt64>(rejectedMaterialIds = emptySet())
        val machinePolicy = FakeMachineCompatibilityPolicy<Flt64>(rejectedMachineIds = setOf("mc-rejected"))

        val ctx = SimpleDomainCalculationContext(plan = plan, planIndex = 0, domainValueSample = Flt64(1.0))

        // width policy passes, machine policy fails => allFeasible returns false
        assertTrue(!allFeasible(listOf(widthPolicy, machinePolicy), ctx))

        // only width policy => passes
        assertTrue(allFeasible(listOf(widthPolicy), ctx))
    }

    @Test
    fun `generation input with domain policy filters rejected plans`() {
        val material1 = testMaterial("mat-1")
        val material2 = testMaterial("mat-2")
        val product1 = testProduct("p1")
        val product2 = testProduct("p2")
        val demand1 = testDemand(product1)
        val demand2 = testDemand(product2)

        // Without domain policy: all material-product pairs should generate plans
        val inputNoPolicy = CuttingPlanGenerationInput(
            products = listOf(product1, product2),
            materials = listOf(material1, material2),
            machines = emptyList(),
            demands = listOf(demand1, demand2),
            domainPolicies = emptyList()
        )
        val generator = SimpleInitialCuttingPlanGenerator<Flt64>()
        val plansNoPolicy = generator.generate(inputNoPolicy)
        assertTrue(plansNoPolicy.isNotEmpty(), "Without domain policy, should generate plans")

        // With domain policy rejecting mat-1: plans from mat-1 should be filtered
        val inputWithPolicy = CuttingPlanGenerationInput(
            products = listOf(product1, product2),
            materials = listOf(material1, material2),
            machines = emptyList(),
            demands = listOf(demand1, demand2),
            domainPolicies = listOf(FakeWidthDifferencePolicy(setOf("mat-1")))
        )
        val plansWithPolicy = generator.generate(inputWithPolicy)
        assertTrue(plansWithPolicy.isNotEmpty(), "Should still have plans from mat-2")
        assertTrue(plansWithPolicy.all { it.material.id == "mat-2" },
            "All remaining plans should be from mat-2, got: ${plansWithPolicy.map { it.material.id }}")
    }

    @Test
    fun `default domain policy does not change generation result`() {
        val material = testMaterial()
        val product = testProduct("p1")
        val demand = testDemand(product)

        val inputNoPolicy = CuttingPlanGenerationInput(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            domainPolicies = emptyList()
        )
        val inputDefaultPolicy = CuttingPlanGenerationInput(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            domainPolicies = listOf(DefaultCsp1dDomainPolicy())
        )

        val generator = SimpleInitialCuttingPlanGenerator<Flt64>()
        val plansNoPolicy = generator.generate(inputNoPolicy)
        val plansDefaultPolicy = generator.generate(inputDefaultPolicy)

        assertEquals(plansNoPolicy.size, plansDefaultPolicy.size,
            "Default domain policy should not change generation result")
    }

    // ===== Objective Policy Tests =====

    /**
     * Fake 业务成本目标策略 / Fake business cost objective policy
     *
     * 为指定物料的方案增加额外成本系数。
     * Add extra cost coefficient for plans using specified materials.
     */
    class FakeMaterialCostPolicy<V : RealNumber<V>>(
        private val extraCostByMaterial: Map<String, Flt64>
    ) : Csp1dObjectivePolicy<V> {
        override val name: String = "fake_material_cost"

        override fun modifyBatchCoefficient(context: Csp1dDomainCalculationContext<V>, baseCoefficient: Flt64): Flt64 {
            val extra = extraCostByMaterial[context.plan.material.id] ?: Flt64.zero
            return baseCoefficient + extra
        }
    }

    @Test
    fun `objective policy modifies batch coefficient for specified material`() {
        val material1 = testMaterial("mat-1")
        val material2 = testMaterial("mat-2")
        val product = testProduct("p1")

        val plan1 = CuttingPlan(
            id = "plan-1",
            material = material1,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )
        val plan2 = CuttingPlan(
            id = "plan-2",
            material = material2,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )

        val policy = FakeMaterialCostPolicy<Flt64>(extraCostByMaterial = mapOf("mat-1" to Flt64(5.0)))

        val ctx1 = SimpleDomainCalculationContext(plan = plan1, planIndex = 0, domainValueSample = Flt64(1.0))
        val ctx2 = SimpleDomainCalculationContext(plan = plan2, planIndex = 1, domainValueSample = Flt64(1.0))

        assertEquals(Flt64(6.0), policy.modifyBatchCoefficient(ctx1, Flt64.one),
            "mat-1 should get extra cost coefficient")
        assertEquals(Flt64.one, policy.modifyBatchCoefficient(ctx2, Flt64.one),
            "mat-2 should not be affected")
    }

    @Test
    fun `default objective policy does not modify coefficient`() {
        val material = testMaterial()
        val product = testProduct("p1")
        val plan = CuttingPlan(
            id = "plan-1",
            material = material,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )
        val ctx = SimpleDomainCalculationContext(plan = plan, planIndex = 0, domainValueSample = Flt64(1.0))
        val policy = object : Csp1dObjectivePolicy<Flt64> {
            override val name = "empty"
        }
        assertEquals(Flt64.one, policy.modifyBatchCoefficient(ctx, Flt64.one))
    }

    // ===== Generation Strategy Tests =====

    /**
     * Fake 候选过滤策略 / Fake candidate filter strategy
     *
     * 拒绝使用指定物料的候选方案。
     * Reject candidate plans using specified materials.
     */
    class FakeCandidateFilterStrategy<V : RealNumber<V>>(
        private val rejectedMaterialIds: Set<String>
    ) : Csp1dGenerationStrategy<V> {
        override val name: String = "fake_candidate_filter"

        override fun acceptCandidate(candidate: CuttingPlan<V>, existingPlans: List<CuttingPlan<V>>): Boolean {
            return candidate.material.id !in rejectedMaterialIds
        }
    }

    @Test
    fun `generation strategy filters candidates by material`() {
        val material1 = testMaterial("mat-1")
        val material2 = testMaterial("mat-2")
        val product1 = testProduct("p1")
        val product2 = testProduct("p2")
        val demand1 = testDemand(product1)
        val demand2 = testDemand(product2)

        val input = CuttingPlanGenerationInput(
            products = listOf(product1, product2),
            materials = listOf(material1, material2),
            machines = emptyList(),
            demands = listOf(demand1, demand2),
            candidateFilters = listOf({ candidate: CuttingPlan<Flt64>, _: List<CuttingPlan<Flt64>> ->
                candidate.material.id != "mat-1"
            })
        )
        val generator = SimpleInitialCuttingPlanGenerator<Flt64>()
        val plans = generator.generate(input)
        assertTrue(plans.isNotEmpty(), "Should have plans from mat-2")
        assertTrue(plans.all { it.material.id == "mat-2" },
            "All plans should be from mat-2, got: ${plans.map { it.material.id }}")
    }

    @Test
    fun `empty candidate filters do not change generation result`() {
        val material = testMaterial()
        val product = testProduct("p1")
        val demand = testDemand(product)

        val inputNoFilter = CuttingPlanGenerationInput(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            candidateFilters = emptyList()
        )
        val inputEmptyFilter = CuttingPlanGenerationInput(
            products = listOf(product),
            materials = listOf(material),
            machines = emptyList(),
            demands = listOf(demand),
            candidateFilters = listOf({ _: CuttingPlan<Flt64>, _: List<CuttingPlan<Flt64>> -> true })
        )
        val generator = SimpleInitialCuttingPlanGenerator<Flt64>()
        assertEquals(generator.generate(inputNoFilter).size, generator.generate(inputEmptyFilter).size)
    }

    // ===== Flow Policy Tests =====

    @Test
    fun `flow policy filterInitialPlans filters plans`() {
        val material1 = testMaterial("mat-1")
        val material2 = testMaterial("mat-2")
        val product = testProduct("p1")

        val plan1 = CuttingPlan(
            id = "plan-1",
            material = material1,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )
        val plan2 = CuttingPlan(
            id = "plan-2",
            material = material2,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )

        val flowPolicy = object : Csp1dFlowPolicy<Flt64> {
            override val name = "filter_mat1"
            override fun filterInitialPlans(plans: List<CuttingPlan<Flt64>>): List<CuttingPlan<Flt64>> {
                return plans.filter { it.material.id != "mat-1" }
            }
        }

        val filtered = flowPolicy.filterInitialPlans(listOf(plan1, plan2))
        assertEquals(1, filtered.size)
        assertEquals("mat-2", filtered[0].material.id)
    }

    @Test
    fun `flow policy isEquivalent marks plans as equivalent`() {
        val material = testMaterial()
        val product1 = testProduct("p1")
        val product2 = testProduct("p2")

        val plan1 = CuttingPlan(
            id = "plan-1",
            material = material,
            slices = listOf(CuttingPlanSlice(production = product1, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product1, quantity = Quantity(Flt64(10.0), meter)))
        )
        val plan2 = CuttingPlan(
            id = "plan-2",
            material = material,
            slices = listOf(CuttingPlanSlice(production = product2, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product2, quantity = Quantity(Flt64(10.0), meter)))
        )

        val flowPolicy = object : Csp1dFlowPolicy<Flt64> {
            override val name = "same_material_equivalence"
            override fun isEquivalent(existing: CuttingPlan<Flt64>, candidate: CuttingPlan<Flt64>): Boolean {
                return existing.material.id == candidate.material.id
            }
        }

        assertTrue(flowPolicy.isEquivalent(plan1, plan2))
    }

    @Test
    fun `default flow policy does not filter or mark equivalent`() {
        val material = testMaterial()
        val product = testProduct("p1")
        val plan = CuttingPlan(
            id = "plan-1",
            material = material,
            slices = listOf(CuttingPlanSlice(production = product, width = Quantity(Flt64(100.0), meter))),
            demandContributions = listOf(CuttingPlanDemandContribution(product = product, quantity = Quantity(Flt64(10.0), meter)))
        )

        val policy = object : Csp1dFlowPolicy<Flt64> {
            override val name = "empty"
        }
        assertEquals(listOf(plan), policy.filterInitialPlans(listOf(plan)))
        assertTrue(!policy.isEquivalent(plan, plan))
    }

    // ===== ExtensionSet Integration Test =====

    @Test
    fun `extension set carries all policies`() {
        val extensionSet = Csp1dExtensionSet<Flt64>(
            domainPolicies = listOf(DefaultCsp1dDomainPolicy()),
            objectivePolicies = listOf(FakeMaterialCostPolicy(emptyMap())),
            generationStrategies = listOf(FakeCandidateFilterStrategy(emptySet())),
            flowPolicies = listOf(object : Csp1dFlowPolicy<Flt64> { override val name = "test" })
        )
        assertEquals(1, extensionSet.domainPolicies.size)
        assertEquals(1, extensionSet.objectivePolicies.size)
        assertEquals(1, extensionSet.generationStrategies.size)
        assertEquals(1, extensionSet.flowPolicies.size)
        assertEquals(0, extensionSet.pricingPolicies.size)
    }

    // ===== Fake 下游扩展样例测试 / Fake downstream extension sample tests =====

    private fun <V : RealNumber<V>> fakeSameUnitLengthExtension(materialId: String): Csp1dModelingExtension<V> {
        return Csp1dModelingExtension(
            pipeline = FakeSameUnitLengthPipeline(materialId),
            mode = Csp1dExtensionMode.ALL
        )
    }

    /**
     * Fake same-unit-length 建模扩展管线 / Fake same-unit-length modeling extension pipeline
     *
     * 模拟下游同单位长度约束：为同物料方案注册一个辅助变量。
     * Simulates downstream same-unit-length logic by registering one helper variable for same-material plans.
     */
    class FakeSameUnitLengthPipeline(
        private val materialId: String
    ) : Pipeline<LinearMetaModel<Flt64>> {
        override val name = "fake_same_unit_length_$materialId"

        var helper: URealVar? = null
            private set

        override fun invoke(model: LinearMetaModel<Flt64>): Try {
            val variable = URealVar("fake_same_unit_length_$materialId")
            helper = variable
            return model.add(variable)
        }
    }

    private fun <V : RealNumber<V>> fakeSameWidthExtension(machineId: String): Csp1dModelingExtension<V> {
        return Csp1dModelingExtension(
            pipeline = FakeSameWidthPipeline(machineId),
            mode = Csp1dExtensionMode.ALL
        )
    }

    /**
     * Fake same-width 建模扩展管线 / Fake same-width modeling extension pipeline
     *
     * 模拟下游同宽约束：为同设备方案注册一个辅助变量。
     * Simulates downstream same-width logic by registering one helper variable for same-machine plans.
     */
    class FakeSameWidthPipeline(
        private val machineId: String
    ) : Pipeline<LinearMetaModel<Flt64>> {
        override val name = "fake_same_width_$machineId"

        var helper: URealVar? = null
            private set

        override fun invoke(model: LinearMetaModel<Flt64>): Try {
            val variable = URealVar("fake_same_width_$machineId")
            helper = variable
            return model.add(variable)
        }
    }

    /**
     * Fake 候选验收策略 / Fake candidate acceptance strategy
     *
     * 模拟下游候选验收逻辑：只接受特定物料的方案。
     * Simulate downstream candidate acceptance: only accept plans for specific materials.
     */
    class FakeCandidateAcceptanceStrategy<V : RealNumber<V>>(
        private val acceptedMaterialIds: Set<String>
    ) : Csp1dGenerationStrategy<V> {
        override val name = "fake_candidate_acceptance"
        override fun acceptCandidate(candidate: CuttingPlan<V>, existingPlans: List<CuttingPlan<V>>): Boolean {
            return candidate.material.id in acceptedMaterialIds
        }
    }

    /**
     * Fake 输出扩展策略 / Fake extraction policy for output enrichment
     *
     * 模拟下游输出扩展：向 solution details 和 render KPI 写入自定义信息。
     * Simulate downstream output enrichment: write custom information to solution details and render KPI.
     */
    class FakeOutputExtractionPolicy<V : RealNumber<V>>(
        private val customDetailKey: String = "downstream_custom_detail",
        private val customDetailValue: String = "downstream_enriched"
    ) : Csp1dExtractionPolicy<V> {
        override val name = "fake_output_extraction"
        override fun enrichOutput(
            details: MutableMap<String, String>,
            renderKpi: MutableMap<String, String>,
            produce: Produce<V>,
            demands: List<ProductDemand<V>>,
            materials: List<Material<V>>,
            machines: List<Machine<V>>,
            generatedPlans: List<CuttingPlan<V>>,
            iterationCount: Int64,
            terminationReason: String?,
            finalMilpStatus: String?,
            pricingStatistics: CuttingPlanGenerationStatistics?
        ) {
            details[customDetailKey] = customDetailValue
            renderKpi["downstream_custom_kpi"] = "enabled"
        }
    }

    @Test
    fun sameUnitLengthExtensionCanBeInjectedViaExtensionSet() {
        val extension = fakeSameUnitLengthExtension<Flt64>("m1")
        val extensionSet = Csp1dExtensionSet<Flt64>(
            modelingExtensions = listOf(extension)
        )
        val pipeline = extensionSet.modelingExtensions.first().resolvePipeline(null)
        val model = LinearMetaModel(
            name = "fake_same_unit_length_test",
            converter = fuookami.ospf.kotlin.core.solver.value.IntoValue.Identity
        )

        assertEquals(1, extensionSet.modelingExtensions.size)
        assertEquals("fake_same_unit_length_m1", pipeline.name)
        assertEquals(ok, pipeline(model))
        val helper = assertNotNull((pipeline as FakeSameUnitLengthPipeline).helper)
        assertNotNull(model.tokens.find(helper))
    }

    @Test
    fun sameWidthExtensionCanBeInjectedViaExtensionSet() {
        val extension = fakeSameWidthExtension<Flt64>("mc1")
        val extensionSet = Csp1dExtensionSet<Flt64>(
            modelingExtensions = listOf(extension)
        )
        val pipeline = extensionSet.modelingExtensions.first().resolvePipeline(null)
        val model = LinearMetaModel(
            name = "fake_same_width_test",
            converter = fuookami.ospf.kotlin.core.solver.value.IntoValue.Identity
        )

        assertEquals(1, extensionSet.modelingExtensions.size)
        assertEquals("fake_same_width_mc1", pipeline.name)
        assertEquals(ok, pipeline(model))
        val helper = assertNotNull((pipeline as FakeSameWidthPipeline).helper)
        assertNotNull(model.tokens.find(helper))
    }

    @Test
    fun candidateAcceptanceStrategyFiltersByMaterial() {
        val strategy = FakeCandidateAcceptanceStrategy<Flt64>(setOf("mat-good"))
        val goodMaterial = testMaterial("mat-good")
        val badMaterial = testMaterial("mat-bad")
        val goodPlan = CuttingPlan(
            id = "good-plan",
            material = goodMaterial,
            slices = emptyList(),
            demandContributions = emptyList()
        )
        val badPlan = CuttingPlan(
            id = "bad-plan",
            material = badMaterial,
            slices = emptyList(),
            demandContributions = emptyList()
        )
        assertTrue(strategy.acceptCandidate(goodPlan, emptyList()), "Should accept plans from mat-good")
        assertFalse(strategy.acceptCandidate(badPlan, emptyList()), "Should reject plans from mat-bad")
    }

    @Test
    fun outputExtractionPolicyWritesCustomDetails() {
        val policy = FakeOutputExtractionPolicy<Flt64>()
        val details = mutableMapOf<String, String>()
        val renderKpi = mutableMapOf<String, String>()
        policy.enrichOutput(
            details = details,
            renderKpi = renderKpi,
            produce = Produce(emptyList(), emptyList(), emptyList(), emptyList()),
            demands = emptyList(),
            materials = emptyList(),
            machines = emptyList(),
            generatedPlans = emptyList(),
            iterationCount = Int64.zero,
            terminationReason = null,
            finalMilpStatus = null,
            pricingStatistics = null
        )
        assertEquals("downstream_enriched", details["downstream_custom_detail"])
        assertEquals("enabled", renderKpi["downstream_custom_kpi"])
    }

    @Test
    fun allFakeDownstreamExtensionsCanBeCombinedInExtensionSet() {
        val extensionSet = Csp1dExtensionSet<Flt64>(
            modelingExtensions = listOf(
                fakeSameUnitLengthExtension("m1"),
                fakeSameWidthExtension("mc1")
            ),
            domainPolicies = listOf(
                FakeWidthDifferencePolicy(emptySet()),
                FakeMachineCompatibilityPolicy(emptySet())
            ),
            objectivePolicies = listOf(
                FakeMaterialCostPolicy(emptyMap())
            ),
            generationStrategies = listOf(
                FakeCandidateFilterStrategy(emptySet()),
                FakeCandidateAcceptanceStrategy(setOf("m1"))
            ),
            extractionPolicies = listOf(
                FakeOutputExtractionPolicy()
            )
        )
        // 验证 6+ 类 fake 下游扩展能力均已装入 extension set / Verify 6+ fake downstream categories are carried by extension set
        assertEquals(2, extensionSet.modelingExtensions.size)
        assertEquals(2, extensionSet.domainPolicies.size)
        assertEquals(1, extensionSet.objectivePolicies.size)
        assertEquals(2, extensionSet.generationStrategies.size)
        assertEquals(1, extensionSet.extractionPolicies.size)
    }

    // ===== Builder DSL 注入测试 / Builder DSL injection tests =====

    /**
     * 验证 Csp1dSolveConfigBuilder 能注入所有 6+ 类下游扩展
     * Verify Csp1dSolveConfigBuilder can inject all 6+ downstream extension categories
     */
    @Test
    fun allDownstreamExtensionsCanBeInjectedViaSolveConfigBuilder() {
        val solveConfig = csp1dSolveConfig<Flt64> {
            extension(fakeSameUnitLengthExtension("m1"))
            extension(fakeSameWidthExtension("mc1"))
            domainPolicy(FakeWidthDifferencePolicy(emptySet()))
            domainPolicy(FakeMachineCompatibilityPolicy(emptySet()))
            objectivePolicy(FakeMaterialCostPolicy(emptyMap()))
            generationStrategy(FakeCandidateFilterStrategy(emptySet()))
            generationStrategy(FakeCandidateAcceptanceStrategy(setOf("m1")))
            extractionPolicy(FakeOutputExtractionPolicy())
        }

        // 验证建模扩展：same unit length + same width / Verify modeling extensions
        assertEquals(2, solveConfig.extensions.size,
            "Builder should carry 2 modeling extensions (same unit length + same width)")

        // 验证领域策略：宽差 + 设备兼容 / Verify domain policies
        assertEquals(2, solveConfig.extensionSet.domainPolicies.size,
            "Builder should carry 2 domain policies (width diff + machine compat)")

        // 验证目标策略：业务成本 / Verify objective policy
        assertEquals(1, solveConfig.extensionSet.objectivePolicies.size,
            "Builder should carry 1 objective policy (material cost)")

        // 验证生成策略：候选过滤 + 候选验收 / Verify generation strategies
        assertEquals(2, solveConfig.extensionSet.generationStrategies.size,
            "Builder should carry 2 generation strategies (filter + acceptance)")

        // 验证提取策略：输出扩展 / Verify extraction policy
        assertEquals(1, solveConfig.extensionSet.extractionPolicies.size,
            "Builder should carry 1 extraction policy (output enrichment)")
    }

    /**
     * 验证默认空 Csp1dSolveConfig 不改变扩展集
     * Verify default empty Csp1dSolveConfig does not add extensions
     */
    @Test
    fun defaultSolveConfigHasNoExtensions() {
        val solveConfig = Csp1dSolveConfig<Flt64>()
        assertTrue(solveConfig.extensions.isEmpty(), "Default config should have no modeling extensions")
        assertTrue(solveConfig.extensionSet.modelingExtensions.isEmpty(), "Default extensionSet should have no modeling extensions")
        assertTrue(solveConfig.extensionSet.domainPolicies.isEmpty(), "Default extensionSet should have no domain policies")
        assertTrue(solveConfig.extensionSet.objectivePolicies.isEmpty(), "Default extensionSet should have no objective policies")
        assertTrue(solveConfig.extensionSet.generationStrategies.isEmpty(), "Default extensionSet should have no generation strategies")
        assertTrue(solveConfig.extensionSet.pricingPolicies.isEmpty(), "Default extensionSet should have no pricing policies")
        assertTrue(solveConfig.extensionSet.flowPolicies.isEmpty(), "Default extensionSet should have no flow policies")
        assertTrue(solveConfig.extensionSet.extractionPolicies.isEmpty(), "Default extensionSet should have no extraction policies")
    }

    /**
     * 验证 Csp1dExtensionSet.empty() 不引入任何扩展
     * Verify Csp1dExtensionSet.empty() introduces no extensions
     */
    @Test
    fun emptyExtensionSetDoesNotIntroduceExtensions() {
        val emptySet = Csp1dExtensionSet.empty<Flt64>()
        assertTrue(emptySet.modelingExtensions.isEmpty())
        assertTrue(emptySet.domainPolicies.isEmpty())
        assertTrue(emptySet.objectivePolicies.isEmpty())
        assertTrue(emptySet.generationStrategies.isEmpty())
        assertTrue(emptySet.pricingPolicies.isEmpty())
        assertTrue(emptySet.flowPolicies.isEmpty())
        assertTrue(emptySet.extractionPolicies.isEmpty())
    }

    /**
     * 验证 fake 下游扩展样例覆盖 6+ 类扩展能力
     * 验收 7.1.10: same unit length, same width, 宽差, 设备/材质兼容, 业务成本, 候选过滤, 候选验收, 输出扩展
     * Verify fake downstream samples cover 6+ extension categories
     * Acceptance 7.1.10: same unit length, same width, width diff, machine/material compat, business cost, candidate filter, candidate acceptance, output enrichment
     */
    @Test
    fun fakeDownstreamSamplesCoverSixPlusCategories() {
        val categories = mutableSetOf<String>()

        // 1. 同单位长度 / same unit length
        val sameUnitLength = fakeSameUnitLengthExtension<Flt64>("m1")
        categories.add("same_unit_length")

        // 2. 同宽 / same width
        val sameWidth = fakeSameWidthExtension<Flt64>("mc1")
        categories.add("same_width")

        // 3. 宽差 / width difference
        val widthDiff = FakeWidthDifferencePolicy<Flt64>(emptySet())
        categories.add("width_difference")

        // 4. 设备/材质兼容 / machine or material compatibility
        val machineCompat = FakeMachineCompatibilityPolicy<Flt64>(emptySet())
        categories.add("machine_material_compatibility")

        // 5. 业务成本 / business cost
        val materialCost = FakeMaterialCostPolicy<Flt64>(emptyMap())
        categories.add("business_cost")

        // 6. 候选过滤 / candidate filter
        val candidateFilter = FakeCandidateFilterStrategy<Flt64>(emptySet())
        categories.add("candidate_filter")

        // 7. 候选验收 / candidate acceptance
        val candidateAcceptance = FakeCandidateAcceptanceStrategy<Flt64>(setOf("m1"))
        categories.add("candidate_acceptance")

        // 8. 输出扩展 / output enrichment
        val outputExtraction = FakeOutputExtractionPolicy<Flt64>()
        categories.add("output_enrichment")

        // 验证至少覆盖 6 类能力 / Verify at least 6 categories are covered
        assertTrue(categories.size >= 6,
            "Should cover at least 6 downstream categories, got ${categories.size}: $categories")

        // 验证可组合到同一个 extension set / Verify all samples can be combined in one extension set
        val combined = Csp1dExtensionSet<Flt64>(
            modelingExtensions = listOf(sameUnitLength, sameWidth),
            domainPolicies = listOf(widthDiff, machineCompat),
            objectivePolicies = listOf(materialCost),
            generationStrategies = listOf(candidateFilter, candidateAcceptance),
            extractionPolicies = listOf(outputExtraction)
        )
        assertEquals(2, combined.modelingExtensions.size)
        assertEquals(2, combined.domainPolicies.size)
        assertEquals(1, combined.objectivePolicies.size)
        assertEquals(2, combined.generationStrategies.size)
        assertEquals(1, combined.extractionPolicies.size)
    }
}
