package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter

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
}

