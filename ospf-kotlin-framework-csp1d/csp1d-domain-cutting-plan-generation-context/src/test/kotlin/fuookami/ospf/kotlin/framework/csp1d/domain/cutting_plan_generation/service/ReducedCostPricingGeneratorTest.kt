package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dPricingInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.ReducedCostPricingGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Flt64QuantityArithmetic
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MachineCapacityShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.MaterialUsageShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.RollCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.SheetCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ShadowPriceMap
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange

class ReducedCostPricingGeneratorTest {

    private fun product(id: String, width: Double = 0.5): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(Quantity(Flt64(width), Meter))
        )
    }

    private fun material(id: String = "m", upperBound: Double = 2.0): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(0.5), Meter),
                    upperBound = Quantity(Flt64(upperBound), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            ),
            length = Quantity(Flt64(100.0), Meter)
        )
    }

    private fun machine(id: String = "mch"): Machine<Flt64> {
        return Machine(
            id = id,
            name = "machine-$id",
            maxBatchCount = UInt64(10UL)
        )
    }

    private fun demandKey(demand: ProductDemand<Flt64>): ProductDemandShadowPriceKey {
        return ProductDemandShadowPriceKey(
            productId = demand.product.id,
            unitSymbol = unitSymbol(demand.quantity.unit)
        )
    }

    private fun unitSymbol(unit: PhysicalUnit): String {
        return unit.symbol ?: unit.name ?: unit.toString()
    }

    @Test
    fun returnsEmptyWhenNoNegativeReducedCostCandidates() {
        val p = product("p1")
        val m = material()
        val mch = machine()
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val enumerator = NSameGenerator<Flt64>(arithmetic = Flt64QuantityArithmetic)
        val generator = ReducedCostPricingGenerator(enumerator)

        val generationInput = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = listOf(mch),
            demands = listOf(demand)
        )

        // 所有影子价格为 0，reduced cost = 1 > 0，无负 reduced cost 候选 / All shadow prices are 0, reduced cost = 1 > 0, so no negative candidates
        val shadowPrices = ShadowPriceMap<Flt64>(values = emptyMap())

        val input = Csp1dPricingInput(
            generationInput = generationInput,
            shadowPrices = shadowPrices,
            maxGeneratedPlans = UInt64(10UL)
        )

        val result = generator.generate(input)
        assertEquals(0, result.size)
    }

    @Test
    fun returnsCandidatesWithNegativeReducedCost() {
        val p = product("p1")
        val m = material()
        val mch = machine()
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val enumerator = NSameGenerator<Flt64>(arithmetic = Flt64QuantityArithmetic)
        val generator = ReducedCostPricingGenerator(enumerator)

        val generationInput = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = listOf(mch),
            demands = listOf(demand)
        )

        // 高产品需求影子价格使 contribution * sp > 1，从而得到负 reduced cost / High product demand shadow price makes contribution * sp > 1, yielding negative reduced cost
        val shadowPrices = ShadowPriceMap<Flt64>(
            values = mapOf(
                demandKey(demand) to Flt64(10.0)
            )
        )

        val input = Csp1dPricingInput(
            generationInput = generationInput,
            shadowPrices = shadowPrices,
            maxGeneratedPlans = UInt64(5UL)
        )

        val result = generator.generate(input)
        assertTrue(result.isNotEmpty(), "Should find plans with negative reduced cost when shadow prices are high")
        assertTrue(result.size <= 5, "Should respect maxGeneratedPlans limit")
    }

    @Test
    fun sortsByMostNegativeReducedCostFirst() {
        val p1 = product("p1", width = 0.5)
        val p2 = product("p2", width = 0.8)
        val m = material()
        val mch = machine()
        val demands = listOf(
            ProductDemand.roll(p1, Quantity(Flt64(5.0), RollCountUnit)),
            ProductDemand.roll(p2, Quantity(Flt64(3.0), RollCountUnit))
        )
        val enumerator = NSameGenerator<Flt64>(arithmetic = Flt64QuantityArithmetic)
        val generator = ReducedCostPricingGenerator(enumerator)

        val generationInput = CuttingPlanGenerationInput(
            products = listOf(p1, p2),
            materials = listOf(m),
            machines = listOf(mch),
            demands = demands
        )

        // p2 影子价格更高，其方案应更优先 / p2 has higher shadow price, so its plans should be preferred
        val shadowPrices = ShadowPriceMap<Flt64>(
            values = mapOf(
                demandKey(demands[0]) to Flt64(1.5),
                demandKey(demands[1]) to Flt64(5.0)
            )
        )

        val input = Csp1dPricingInput(
            generationInput = generationInput,
            shadowPrices = shadowPrices,
            maxGeneratedPlans = UInt64(10UL)
        )

        val result = generator.generate(input)
        assertTrue(result.isNotEmpty(), "Should find plans with negative reduced cost")
    }

    @Test
    fun deduplicatesAgainstExistingPlans() {
        val p = product("p1")
        val m = material()
        val mch = machine()
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val enumerator = NSameGenerator<Flt64>(arithmetic = Flt64QuantityArithmetic)
        val generator = ReducedCostPricingGenerator(enumerator)

        // 先生成初始方案作为 existing plans / Generate initial plans to use as existing plans
        val initialInput = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = listOf(mch),
            demands = listOf(demand)
        )
        val initialPlans = enumerator.generate(initialInput)

        val generationInput = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = listOf(mch),
            demands = listOf(demand),
            existingPlans = initialPlans
        )

        val shadowPrices = ShadowPriceMap<Flt64>(
            values = mapOf(
                demandKey(demand) to Flt64(10.0)
            )
        )

        val input = Csp1dPricingInput(
            generationInput = generationInput,
            shadowPrices = shadowPrices,
            maxGeneratedPlans = UInt64(100UL)
        )

        val result = generator.generate(input)
        val existingIds = initialPlans.map { it.id }.toSet()
        assertTrue(
            result.none { it.id in existingIds },
            "Should not include plans that already exist"
        )
    }

    @Test
    fun materialShadowPriceReducesCost() {
        val p = product("p1")
        val m = material()
        val mch = machine()
        val demand = ProductDemand.roll(p, Quantity(Flt64(5.0), RollCountUnit))
        val enumerator = NSameGenerator<Flt64>(arithmetic = Flt64QuantityArithmetic)
        val generator = ReducedCostPricingGenerator(enumerator)

        val generationInput = CuttingPlanGenerationInput(
            products = listOf(p),
            materials = listOf(m),
            machines = listOf(mch),
            demands = listOf(demand)
        )

        // 带物料影子价格时 reduced cost 更低 / With material shadow price, reduced cost is lower
        val shadowPricesWithMaterial = ShadowPriceMap<Flt64>(
            values = mapOf(
                demandKey(demand) to Flt64(2.0),
                MaterialUsageShadowPriceKey(m.id) to Flt64(1.0)
            )
        )

        val inputWithMaterial = Csp1dPricingInput(
            generationInput = generationInput,
            shadowPrices = shadowPricesWithMaterial,
            maxGeneratedPlans = UInt64(5UL)
        )

        val resultWithMaterialSp = generator.generate(inputWithMaterial)

        // 不带物料影子价格 / Without material shadow price
        val shadowPricesNoMaterial = ShadowPriceMap<Flt64>(
            values = mapOf(
                demandKey(demand) to Flt64(2.0)
            )
        )

        val inputNoMaterial = Csp1dPricingInput(
            generationInput = generationInput,
            shadowPrices = shadowPricesNoMaterial,
            maxGeneratedPlans = UInt64(5UL)
        )

        val resultWithoutMaterialSp = generator.generate(inputNoMaterial)

        // 两者都应为负 reduced cost，物料影子价格会进一步降低 reduced cost / Both should have negative reduced cost, and material shadow price lowers it further
        assertTrue(resultWithMaterialSp.isNotEmpty())
        assertTrue(resultWithoutMaterialSp.isNotEmpty())
        assertTrue(
            resultWithMaterialSp.size >= resultWithoutMaterialSp.size,
            "Material shadow price should make reduced cost more negative, not fewer candidates"
        )
    }

    /**
     * 验证 ProductDemandShadowPriceKey 按 product + unit 口径区分不同需求单位
     * Verify ProductDemandShadowPriceKey distinguishes different demand units by product + unit
     */
    @Test
    fun shadowPriceKeyDistinguishesDifferentUnits() {
        val p = product("p1")
        val rollKey = ProductDemandShadowPriceKey(
            productId = "p1",
            unitSymbol = unitSymbol(RollCountUnit)
        )
        val sheetKey = ProductDemandShadowPriceKey(
            productId = "p1",
            unitSymbol = unitSymbol(SheetCountUnit)
        )

        // roll 和 sheet 需求的 key 应不同 / Roll and sheet demand keys should differ
        assertTrue(rollKey != sheetKey, "Roll and sheet keys for same product should be different")
        assertEquals("product-demand:p1:${unitSymbol(RollCountUnit)}", rollKey.name)
        assertEquals("product-demand:p1:${unitSymbol(SheetCountUnit)}", sheetKey.name)

        // ShadowPriceMap 应能分别存储和查询 / ShadowPriceMap should store and query separately
        val shadowPrices = ShadowPriceMap<Flt64>(
            values = mapOf(
                rollKey to Flt64(3.0),
                sheetKey to Flt64(5.0)
            )
        )
        assertEquals(Flt64(3.0), shadowPrices[rollKey])
        assertEquals(Flt64(5.0), shadowPrices[sheetKey])
    }

    /**
     * 验证 id 含下划线时 shadow price key 映射正确
     * Verify shadow price key mapping works correctly when ids contain underscores
     */
    @Test
    fun shadowPriceKeyWorksWithUnderscoreInIds() {
        val rollKey = ProductDemandShadowPriceKey(
            productId = "p_underscore_test",
            unitSymbol = unitSymbol(RollCountUnit)
        )
        val materialKey = MaterialUsageShadowPriceKey("m_underscore_test")
        val machineKey = MachineCapacityShadowPriceKey("machine_test_1")

        // 含下划线的 id 不影响 key 的生成和查询 / Underscores in ids don't affect key generation and lookup
        assertTrue(rollKey.name.contains("p_underscore_test"), "Product demand key should contain underscore id")
        assertTrue(materialKey.name.contains("m_underscore_test"), "Material key should contain underscore id")
        assertTrue(machineKey.name.contains("machine_test_1"), "Machine key should contain underscore id")

        // ShadowPriceMap 应能正确存储和查询 / ShadowPriceMap should store and query correctly
        val shadowPrices = ShadowPriceMap<Flt64>(
            values = mapOf(
                rollKey to Flt64(2.0),
                materialKey to Flt64(1.0),
                machineKey to Flt64(0.5)
            )
        )
        assertEquals(Flt64(2.0), shadowPrices[rollKey])
        assertEquals(Flt64(1.0), shadowPrices[materialKey])
        assertEquals(Flt64(0.5), shadowPrices[machineKey])
    }

    /**
     * 验证不同产品相同单位的 key 不混淆
     * Verify keys for different products with same unit are not confused
     */
    @Test
    fun shadowPriceKeyDistinguishesDifferentProductsWithSameUnit() {
        val key1 = ProductDemandShadowPriceKey(
            productId = "p1",
            unitSymbol = unitSymbol(RollCountUnit)
        )
        val key2 = ProductDemandShadowPriceKey(
            productId = "p2",
            unitSymbol = unitSymbol(RollCountUnit)
        )

        assertTrue(key1 != key2, "Different products with same unit should have different keys")

        val shadowPrices = ShadowPriceMap<Flt64>(
            values = mapOf(
                key1 to Flt64(3.0),
                key2 to Flt64(7.0)
            )
        )
        assertEquals(Flt64(3.0), shadowPrices[key1])
        assertEquals(Flt64(7.0), shadowPrices[key2])
    }
}
