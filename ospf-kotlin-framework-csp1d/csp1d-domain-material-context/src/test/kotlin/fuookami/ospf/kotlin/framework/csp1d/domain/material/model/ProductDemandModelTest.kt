package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*

class ProductDemandModelTest {
    private fun product(id: String = "p"): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(Quantity(Flt64.one, Meter))
        )
    }

    @Test
    fun legacyRollShouldCreateDiscreteDemandWithRollMode() {
        val demand = ProductDemand.legacyRoll(
            product = product("roll"),
            rollAmount = Flt64(12.0)
        )

        assertEquals(DemandMode.Roll, demand.mode)
        assertTrue(demand.isDiscrete)
        assertFalse(demand.isContinuous)
        assertEquals(true, demand.quantity eq Quantity(Flt64(12.0), RollCountUnit))
    }

    @Test
    fun legacyWeightShouldCreateContinuousDemandWithWeightMode() {
        val demand = ProductDemand.legacyWeight(
            product = product("weight"),
            weightAmount = Flt64(520.0)
        )

        assertEquals(DemandMode.Weight, demand.mode)
        assertTrue(demand.isContinuous)
        assertFalse(demand.isDiscrete)
        assertEquals(true, demand.quantity eq Quantity(Flt64(520.0), Kilogram))
    }

    @Test
    fun demandDomainShouldBeDrivenByQuantityUnitInsteadOfMode() {
        val demand = ProductDemand.roll(
            product = product("mode-vs-domain"),
            quantity = Quantity(Flt64(1.0), Kilogram)
        )

        assertEquals(DemandMode.Roll, demand.mode)
        assertTrue(demand.isContinuous)
        assertFalse(demand.isDiscrete)
    }

    @Test
    fun cuttingPlanContributionShouldUseUnifiedQuantityValue() {
        val contribution = CuttingPlanDemandContribution(
            product = product("contribution"),
            quantity = Quantity(Flt64(88.0), Kilogram)
        )

        assertNotNull(contribution.product)
        assertEquals(true, contribution.quantity eq Quantity(Flt64(88.0), Kilogram))
    }

    @Test
    fun contributionBuilderShouldKeepRollDemandUnitForWeightedProduct() {
        val product = Product(
            id = "weighted-roll",
            name = "weighted roll",
            width = listOf(Quantity(Flt64(0.5), Meter)),
            length = Quantity(Flt64(10.0), Meter),
            unitWeight = Quantity(Flt64(2.0), Kilogram)
        )
        val demand = ProductDemand.roll(
            product = product,
            quantity = Quantity(Flt64(9.0), RollCountUnit)
        )

        val contribution = assertNotNull(demand.contribution(
            width = product.width.first(),
            amount = UInt64(3UL),
            arithmetic = (DefaultQuantityArithmetic.resolveFor(Flt64.one) as Ok).value
        ).value)

        assertEquals(true, contribution.quantity eq Quantity(Flt64(3.0), RollCountUnit))
    }

    @Test
    fun contributionBuilderShouldDeriveWeightWhenDemandUsesWeightUnit() {
        val product = Product(
            id = "weighted-demand",
            name = "weighted demand",
            width = listOf(Quantity(Flt64(0.5), Meter)),
            length = Quantity(Flt64(10.0), Meter),
            unitWeight = Quantity(Flt64(2.0), Kilogram)
        )
        val demand = ProductDemand.weight(
            product = product,
            quantity = Quantity(Flt64(30.0), Kilogram)
        )

        val contribution = assertNotNull(demand.contribution(
            width = product.width.first(),
            amount = UInt64(3UL),
            arithmetic = (DefaultQuantityArithmetic.resolveFor(Flt64.one) as Ok).value
        ).value)

        assertEquals(true, contribution.quantity eq Quantity(Flt64(30.0), Kilogram))
    }

    @Test
    fun materialEnabledShouldCheckPlanMaterialAndMachineWidthRange() {
        val material = Material(
            id = "m-enabled",
            name = "enabled material",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(1.0), Meter),
                    upperBound = Quantity(Flt64(2.0), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            ),
            machineId = "machine-narrow"
        )
        val plan = CuttingPlan(
            id = "plan-enabled",
            material = material,
            slices = listOf(
                CuttingPlanSlice(
                    production = product("enabled-product"),
                    width = Quantity(Flt64(1.5), Meter)
                )
            ),
            arithmetic = (DefaultQuantityArithmetic.resolveFor(Flt64.one) as Ok).value
        )
        val incompatibleMachine = Machine(
            id = "machine-narrow",
            name = "narrow machine",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64(0.1), Meter),
                    upperBound = Quantity(Flt64(1.0), Meter)
                ),
                step = Quantity(Flt64(0.1), Meter)
            )
        )
        val otherMaterial = material.copy(id = "m-other")

        assertFalse(material.enabled(plan, listOf(incompatibleMachine)))
        assertFalse(otherMaterial.enabled(plan))
    }
}
