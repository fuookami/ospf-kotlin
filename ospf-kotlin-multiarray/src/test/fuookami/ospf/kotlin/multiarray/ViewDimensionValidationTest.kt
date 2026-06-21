package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.Test

class ViewDimensionValidationTest {
    @Test
    fun `multiArrayView vector dimension mismatch returns failure`() {
        val array = MultiArray.newWith(Shape2(2, 3), 0)
        val view = array[_a, _a]

        assertFailed(view[intArrayOf(0)])

        assertFailed(view[intArrayOf(0, 1, 2)])
    }

    @Test
    fun `mappedMultiArrayView vector dimension mismatch returns failure`() {
        val array = MultiArray.newWith(Shape2(2, 3), 0)
        val view = MappedMultiArrayView(
            array,
            listOf(MapIndex.Map(1), MapIndex.Map(0))
        )

        assertFailed(view[intArrayOf(0)])

        assertFailed(view[intArrayOf(0, 1, 2)])
    }
}
