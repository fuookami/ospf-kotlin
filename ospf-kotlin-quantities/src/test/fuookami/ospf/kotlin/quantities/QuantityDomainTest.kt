package fuookami.ospf.kotlin.quantities

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.unit.Byte as ByteUnit
import fuookami.ospf.kotlin.quantities.dimension.*
import fuookami.ospf.kotlin.quantities.unit.UnitConversionRule

class QuantityDomainTest {
    @Test
    fun physicalUnitDomainDefaultsToQuantityDomain() {
        assertEquals(QuantityDomain.Discrete, Information.domain)
        assertEquals(QuantityDomain.Discrete, Bit.domain)
        assertEquals(QuantityDomain.Continuous, Kilobit.domain)
        assertEquals(QuantityDomain.Discrete, ByteUnit.domain)
        assertEquals(QuantityDomain.Continuous, Kilobyte.domain)
    }

    @Test
    fun derivedQuantityDomainComposition() {
        val discreteA = DerivedQuantity(
            dimension = CustomFundamentalQuantityDimension("A", "discrete A"),
            name = "discrete A",
            symbol = "A",
            domain = QuantityDomain.Discrete
        )
        val discreteB = DerivedQuantity(
            dimension = CustomFundamentalQuantityDimension("B", "discrete B"),
            name = "discrete B",
            symbol = "B",
            domain = QuantityDomain.Discrete
        )
        val continuous = DerivedQuantity(
            dimension = CustomFundamentalQuantityDimension("C", "continuous"),
            name = "continuous",
            symbol = "C"
        )

        assertEquals(QuantityDomain.Discrete, (discreteA * discreteB).domain)
        assertEquals(QuantityDomain.Continuous, (discreteA * continuous).domain)
        assertEquals(QuantityDomain.Continuous, (discreteA / discreteB).domain)
        assertEquals(QuantityDomain.Continuous, discreteA.reciprocal().domain)
        assertEquals(QuantityDomain.Discrete, discreteA.pow(2).domain)
        assertEquals(QuantityDomain.Continuous, discreteA.pow(0).domain)
    }

    @Test
    fun unitDomainCompositionUsesQuantityDomain() {
        val discreteA = DerivedQuantity(
            dimension = CustomFundamentalQuantityDimension("UA", "unit A"),
            name = "unit A",
            symbol = "UA",
            domain = QuantityDomain.Discrete
        )
        val discreteB = DerivedQuantity(
            dimension = CustomFundamentalQuantityDimension("UB", "unit B"),
            name = "unit B",
            symbol = "UB",
            domain = QuantityDomain.Discrete
        )

        val unitA = AnonymousPhysicalUnit(
            quantity = discreteA,
            conversionRule = UnitConversionRule.Linear(Scale()),
            name = "unit A",
            symbol = "UA"
        )
        val unitB = AnonymousPhysicalUnit(
            quantity = discreteB,
            conversionRule = UnitConversionRule.Linear(Scale()),
            name = "unit B",
            symbol = "UB"
        )

        assertEquals(QuantityDomain.Discrete, (unitA * unitB).domain)
        assertEquals(QuantityDomain.Continuous, (unitA / unitB).domain)
    }
}
