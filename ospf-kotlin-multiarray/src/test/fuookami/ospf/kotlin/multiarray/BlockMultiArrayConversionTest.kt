package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class BlockMultiArrayConversionTest {

    @Test
    fun `toMultiArray with empty blocks`() {
        val shape = Shape2(2, 3)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)
        val denseArray = blockArray.toMultiArray(defaultValue = -1)

        // All positions should have default value
        for (i in 0 until 2) {
            for (j in 0 until 3) {
                assertEquals(-1, denseArray[i, j])
            }
        }
    }

    @Test
    fun `toMultiArray with partial blocks`() {
        val shape = Shape2(2, 3)
        val blockArray = BlockMultiArray.empty<Int, Shape2>(shape)
        blockArray[intArrayOf(0, 1)] = 10
        blockArray[intArrayOf(1, 2)] = 20

        val denseArray = blockArray.toMultiArray(defaultValue = 0)

        assertEquals(0, denseArray[0, 0])
        assertEquals(10, denseArray[0, 1])
        assertEquals(0, denseArray[1, 0])
        assertEquals(20, denseArray[1, 2])
    }

    @Test
    fun `toMultiArray with full blocks`() {
        val shape = Shape3(2, 2, 2)
        val original = MultiArray.newBy(shape) { i, _ -> i * 10 }
        val blockArray = BlockMultiArray.fromMultiArray(original)
        val denseArray = blockArray.toMultiArray(defaultValue = 0)

        // All values preserved
        for (i in 0 until shape.size) {
            assertEquals(original.list[i], denseArray.list[i])
        }
    }
}