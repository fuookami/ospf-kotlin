package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class P0UnitConstantsTest {
    @Test
    fun `unitArea_areShouldEqual100SquareMeter`() {
        val oneAre = Flt64.one * Are
        val inSquareMeter = oneAre.to(SquareMeter)
        assertNotNull(inSquareMeter)
        assertEquals(100.0, inSquareMeter.value.toDouble(), 1e-10)
    }
}