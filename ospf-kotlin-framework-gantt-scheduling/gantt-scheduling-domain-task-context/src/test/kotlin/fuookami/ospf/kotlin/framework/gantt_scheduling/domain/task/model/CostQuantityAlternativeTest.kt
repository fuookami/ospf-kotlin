package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit

class CostQuantityAlternativeTest {
    @Test
    fun costItemPrimaryConstructorShouldStoreQuantity() {
        val item = CostItem<FltX>(
            tag = "fuel",
            costQuantity = Quantity(FltX("12.50"), NoneUnit)
        )
        assertNotNull(item.costQuantity)
        assertTrue(item.costQuantity!!.value eq FltX("12.50"))
        assertEquals(NoneUnit, item.costQuantity!!.unit)
    }

    @Test
    fun costItemNullQuantityShouldBeHandled() {
        val item = CostItem<FltX>(
            tag = "empty",
            costQuantity = null
        )
        assertNull(item.costQuantity)
    }

    @Test
    fun immutableCostPrimaryConstructorShouldStoreQuantity() {
        val items = listOf(
            CostItem<FltX>(tag = "a", costQuantity = Quantity(FltX("1.0"), NoneUnit)),
            CostItem<FltX>(tag = "b", costQuantity = Quantity(FltX("2.0"), NoneUnit))
        )
        val cost = Cost(
            items = items,
            costSum = Quantity(FltX("3.0"), NoneUnit)
        )
        assertNotNull(cost.costSum)
        assertTrue(cost.costSum!!.value eq FltX("3.0"))
        assertEquals(NoneUnit, cost.costSum!!.unit)
        assertTrue(cost.valid)
    }

    @Test
    fun immutableCostNullSumShouldBeHandled() {
        val items = listOf(
            CostItem<FltX>(tag = "x", costQuantity = null)
        )
        val cost = Cost(
            items = items,
            costSum = null
        )
        assertNull(cost.costSum)
        assertTrue(!cost.valid)
    }

    @Test
    fun mutableCostPrimaryConstructorShouldStoreQuantity() {
        val mc = MutableCost<FltX>(
            constants = FltX,
            items = ArrayList(),
            costSum = Quantity(FltX("10.0"), NoneUnit)
        )
        assertNotNull(mc.costSum)
        assertTrue(mc.costSum!!.value eq FltX("10.0"))
        assertTrue(mc.valid)
    }
}
