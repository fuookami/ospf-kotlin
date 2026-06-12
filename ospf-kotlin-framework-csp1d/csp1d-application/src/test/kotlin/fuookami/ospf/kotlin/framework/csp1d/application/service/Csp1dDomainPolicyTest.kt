package fuookami.ospf.kotlin.framework.csp1d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dDomainCalculationContext
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Csp1dDomainPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.DefaultCsp1dDomainPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.SimpleDomainCalculationContext
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.allFeasible
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.allWidthFeasible
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanSlice
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dExtensionSet
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dFlowPolicy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dGenerationStrategy
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.Csp1dObjectivePolicy
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter

/**
 * 领域策略集成测试 / Domain policy integration test
 *
 * 验证 Csp1dDomainPolicy 能承载 POIT 的宽差、设备兼容等业务判断。
 *
 * Verify that Csp1dDomainPolicy can carry POIT's width difference,
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
     * 模拟 POIT 中宽度差异约束：拒绝特定物料的方案。
     * Simulate POIT's width difference constraint: reject plans for specific materials.
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
     * 模拟 POIT 中设备兼容性约束：拒绝特定设备上的方案。
     * Simulate POIT's machine compatibility constraint: reject plans on specific machines.
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
            vSample = Flt64(1.0)
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
            vSample = Flt64(1.0)
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

        val ctx1 = SimpleDomainCalculationContext(plan = plan1, planIndex = 0, vSample = Flt64(1.0))
        val ctx2 = SimpleDomainCalculationContext(plan = plan2, planIndex = 1, vSample = Flt64(1.0))

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

        val ctx1 = SimpleDomainCalculationContext(plan = planOnRejected, planIndex = 0, vSample = Flt64(1.0))
        val ctx2 = SimpleDomainCalculationContext(plan = planOnAccepted, planIndex = 1, vSample = Flt64(1.0))

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

        val ctx = SimpleDomainCalculationContext(plan = plan, planIndex = 0, vSample = Flt64(1.0))

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

        val ctx1 = SimpleDomainCalculationContext(plan = plan1, planIndex = 0, vSample = Flt64(1.0))
        val ctx2 = SimpleDomainCalculationContext(plan = plan2, planIndex = 1, vSample = Flt64(1.0))

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
        val ctx = SimpleDomainCalculationContext(plan = plan, planIndex = 0, vSample = Flt64(1.0))
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
}
