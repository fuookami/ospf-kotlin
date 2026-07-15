package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.Test

/**
 * 多维数组视图维度验证测试。
 * Multi-array view dimension validation tests.
 */
class ViewDimensionValidationTest {
    /** 多维数组视图维度不匹配时返回失败 / Multi-array view returns failure on dimension mismatch */
    @Test
    fun `multiArrayView vector dimension mismatch returns failure`() {
        val array = MultiArray.newWith(Shape2(2, 3), 0)
        val view = array[_a, _a]

        assertFailed(view[intArrayOf(0)])

        assertFailed(view[intArrayOf(0, 1, 2)])
    }

    /** 映射多维数组视图维度不匹配时返回失败 / Mapped multi-array view returns failure on dimension mismatch */
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
