@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce

import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialDemand
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialReserves

class MaterialFltXTest {
    @Test
    fun materialDemandShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("10"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(
            quantityRangeValue = Quantity(quantityRange, NoneUnit),
            lessQuantityValue = Quantity(FltX("5.0"), NoneUnit),
            overQuantityValue = Quantity(FltX("3.0"), NoneUnit)
        )

        assertTrue(demand.quantityRangeValue.value.lowerBound.value eq FltX("10"))
        assertTrue(demand.quantityRangeValue.value.upperBound.value eq FltX("100"))
        assertTrue(demand.lessQuantityValue!!.value eq FltX("5.0"))
        assertTrue(demand.overQuantityValue!!.value eq FltX("3.0"))
        assertTrue(demand.lessEnabled)
        assertTrue(demand.overEnabled)
    }

    @Test
    fun materialDemandWithoutPenaltiesShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("0"), FltX("50"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(
            quantityRangeValue = Quantity(quantityRange, NoneUnit)
        )

        assertNotNull(demand.quantityRangeValue)
        assertTrue(demand.quantityRangeValue.value.lowerBound.value eq FltX("0"))
        assertTrue(demand.quantityRangeValue.value.upperBound.value eq FltX("50"))
        assertTrue(!demand.lessEnabled)
        assertTrue(!demand.overEnabled)
    }

    @Test
    fun materialReservesShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("20"), FltX("200"), Interval.Closed, Interval.Closed, FltX).value!!
        val reserves = MaterialReserves<FltX>(
            quantityRangeValue = Quantity(quantityRange, NoneUnit),
            lessQuantityValue = Quantity(FltX("10.0"), NoneUnit),
            overQuantityValue = Quantity(FltX("8.0"), NoneUnit)
        )

        assertTrue(reserves.quantityRangeValue.value.lowerBound.value eq FltX("20"))
        assertTrue(reserves.quantityRangeValue.value.upperBound.value eq FltX("200"))
        assertTrue(reserves.lessQuantityValue!!.value eq FltX("10.0"))
        assertTrue(reserves.overQuantityValue!!.value eq FltX("8.0"))
        assertTrue(reserves.lessEnabled)
        assertTrue(reserves.overEnabled)
    }

    @Test
    fun materialReservesWithoutPenaltiesShouldSupportFltX() {
        val quantityRange = ValueRange(FltX("0"), FltX("1000"), Interval.Closed, Interval.Closed, FltX).value!!
        val reserves = MaterialReserves<FltX>(
            quantityRangeValue = Quantity(quantityRange, NoneUnit)
        )

        assertNotNull(reserves.quantityRangeValue)
        assertTrue(reserves.quantityRangeValue.value.lowerBound.value eq FltX("0"))
        assertTrue(reserves.quantityRangeValue.value.upperBound.value eq FltX("1000"))
        assertTrue(!reserves.lessEnabled)
        assertTrue(!reserves.overEnabled)
    }

    @Test
    fun materialDemandFixedValueRangeWithFltX() {
        val fixedRange = ValueRange(FltX("42"), FltX("42"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(
            quantityRangeValue = Quantity(fixedRange, NoneUnit)
        )

        assertTrue(demand.quantityRangeValue.value.fixed)
        assertTrue(demand.quantityRangeValue.value.fixedValue!! eq FltX("42"))
    }

    @Test
    fun materialDemandValueRangeContainsWithFltX() {
        val range = ValueRange(FltX("10"), FltX("100"), Interval.Closed, Interval.Closed, FltX).value!!
        val demand = MaterialDemand<FltX>(
            quantityRangeValue = Quantity(range, NoneUnit)
        )

        assertTrue(FltX("50") in demand.quantityRangeValue.value)
        assertTrue(FltX("10") in demand.quantityRangeValue.value)
        assertTrue(FltX("100") in demand.quantityRangeValue.value)
    }
}
