@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce

import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialDemand
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialReserves

class MaterialFltXTest {
    @Test
    fun materialDemandShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("10"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(
            quantity = quantityRange,
            lessQuantity = FltX("5.0"),
            overQuantity = FltX("3.0")
        )

        assertTrue(demand.quantity.lowerBound.value eq FltX("10"))
        assertTrue(demand.quantity.upperBound.value eq FltX("100"))
        assertTrue(demand.lessQuantity!! eq FltX("5.0"))
        assertTrue(demand.overQuantity!! eq FltX("3.0"))
        assertTrue(demand.lessEnabled)
        assertTrue(demand.overEnabled)
    }

    @Test
    fun materialDemandWithoutPenaltiesShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("0"), FltX("50"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(quantity = quantityRange)

        assertNotNull(demand.quantity)
        assertTrue(demand.quantity.lowerBound.value eq FltX("0"))
        assertTrue(demand.quantity.upperBound.value eq FltX("50"))
        assertTrue(!demand.lessEnabled)
        assertTrue(!demand.overEnabled)
    }

    @Test
    fun materialReservesShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("20"), FltX("200"), Interval.Closed, Interval.Closed, FltX).value!!
        val reserves = MaterialReserves<FltX>(
            quantity = quantityRange,
            lessQuantity = FltX("10.0"),
            overQuantity = FltX("8.0")
        )

        assertTrue(reserves.quantity.lowerBound.value eq FltX("20"))
        assertTrue(reserves.quantity.upperBound.value eq FltX("200"))
        assertTrue(reserves.lessQuantity!! eq FltX("10.0"))
        assertTrue(reserves.overQuantity!! eq FltX("8.0"))
        assertTrue(reserves.lessEnabled)
        assertTrue(reserves.overEnabled)
    }

    @Test
    fun materialReservesWithoutPenaltiesShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("0"), FltX("1000"), Interval.Closed, Interval.Closed, FltX).value!!
        val reserves = MaterialReserves<FltX>(quantity = quantityRange)

        assertNotNull(reserves.quantity)
        assertTrue(reserves.quantity.lowerBound.value eq FltX("0"))
        assertTrue(reserves.quantity.upperBound.value eq FltX("1000"))
        assertTrue(!reserves.lessEnabled)
        assertTrue(!reserves.overEnabled)
    }

    @Test
    fun materialDemandFixedValueRangeWithFltX() {
        val fixedRange = ValueRange(FltX("42"), FltX("42"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(quantity = fixedRange)

        assertTrue(demand.quantity.fixed)
        assertTrue(demand.quantity.fixedValue!! eq FltX("42"))
    }

    @Test
    fun materialDemandValueRangeContainsWithFltX() {
        val range = ValueRange(FltX("10"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(quantity = range)

        assertTrue(FltX("50") in demand.quantity)
        assertTrue(FltX("10") in demand.quantity)
        assertTrue(FltX("100") in demand.quantity)
    }
}
