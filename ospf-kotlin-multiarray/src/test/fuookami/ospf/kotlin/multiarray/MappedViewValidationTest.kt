package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class MappedViewValidationTest {

    @Test
    fun `reject duplicate map indices`() {
        val array = MultiArray.newWith(Shape3(2, 3, 4), 0)

        // Duplicate index 0
        val duplicateMap = listOf(
            MapIndex.Map(0),
            MapIndex.Map(0),
            MapIndex.Dummy(DummyIndex.all())
        )

        assertThrows<IllegalArgumentException> {
            MappedMultiArrayView(array, duplicateMap)
        }
    }

    @Test
    fun `reject out of bounds map indices`() {
        val array = MultiArray.newWith(Shape3(2, 3, 4), 0)

        // Index 5 is out of bounds for 3 dimensions
        val outOfBoundsMap = listOf(
            MapIndex.Map(5),
            MapIndex.Dummy(DummyIndex.all()),
            MapIndex.Dummy(DummyIndex.all())
        )

        assertThrows<IllegalArgumentException> {
            MappedMultiArrayView(array, outOfBoundsMap)
        }
    }

    @Test
    fun `reject non-contiguous map indices`() {
        val array = MultiArray.newWith(Shape3(2, 3, 4), 0)

        // Missing index 1 - not contiguous
        val nonContiguousMap = listOf(
            MapIndex.Map(0),
            MapIndex.Map(2),  // Should be 1 for contiguous coverage
            MapIndex.Dummy(DummyIndex.all())
        )

        assertThrows<IllegalArgumentException> {
            MappedMultiArrayView(array, nonContiguousMap)
        }
    }

    @Test
    fun `accept valid transpose mapping`() {
        val array = MultiArray.newWith(Shape3(2, 3, 4), 0)

        // Transpose: (d0, d1, d2) -> (d2, d0, d1)
        val transposeMap = listOf(
            MapIndex.Map(2),
            MapIndex.Map(0),
            MapIndex.Map(1)
        )

        val view = MappedMultiArrayView(array, transposeMap)

        // Should have shape [4, 2, 3]
        assertEquals(4, view.shape[0])
        assertEquals(2, view.shape[1])
        assertEquals(3, view.shape[2])
    }

    @Test
    fun `accept valid partial mapping`() {
        val array = MultiArray.newWith(Shape3(2, 3, 4), 0)

        // Fix dimension 1, keep others as dummy
        val partialMap = listOf(
            MapIndex.Dummy(DummyIndex.all()),
            MapIndex.Map(0),  // Map dimension 0 to position 1
            MapIndex.Dummy(DummyIndex.all())
        )

        val view = MappedMultiArrayView(array, partialMap)
        assertNotNull(view)
    }
}