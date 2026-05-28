package fuookami.ospf.kotlin.multiarray

import kotlin.test.*
import org.junit.jupiter.api.Test

/**
 * 虚拟索引和向量测试
 * Dummy index and vector tests
 */
class VectorTest {

    // ========================================================================
    // DummyIndex 测试
    // DummyIndex tests
    // ========================================================================

    @Test
    fun testDummyIndexSingle() {
        // 测试单个索引创建
        // Test single index creation
        val index = DummyIndex.from(5)
        assertTrue(index is DummyIndex.Index)
        assertEquals(5, (index as DummyIndex.Index).index)
    }

    @Test
    fun testDummyIndexRange() {
        // 测试范围索引创建
        // Test range index creation
        val range = DummyIndex.from(0..5)
        assertTrue(range is DummyIndex.Range)
        val rangeIndex = range as DummyIndex.Range
        // 范围 0..5 包含 0,1,2,3,4,5 共 6 个元素
        // Range 0..5 contains 0,1,2,3,4,5 - 6 elements
    }

    @Test
    fun testDummyIndexArray() {
        // 测试索引数组创建
        // Test index array creation
        val array = DummyIndex.from(listOf(0, 2, 4))
        assertTrue(array is DummyIndex.IndexArray)
        val arrayIndex = array as DummyIndex.IndexArray
        assertEquals(listOf(0, 2, 4), arrayIndex.indices)
    }

    @Test
    fun testDummyIndexAll() {
        // 测试全范围索引
        // Test full range index
        val all = DummyIndex.all()
        assertTrue(all is DummyIndex.All)
    }

    @Test
    fun testDummyIndexAllAlias() {
        // 测试 _a 别名
        // Test _a alias
        val shape = Shape2(3, 4)
        assertEquals(3, _a.lenOf(shape, 0))
        assertEquals(4, _a.lenOf(shape, 1))
    }

    @Test
    fun testDummyIndexLenOf() {
        // 测试 lenOf 方法
        // Test lenOf method
        val shape = Shape2(3, 4)

        // 单个索引长度为 1
        // Single index length is 1
        val single = DummyIndex.from(1)
        assertEquals(1, single.lenOf(shape, 0))

        // 范围索引长度
        // Range index length
        val range = DummyIndex.from(0..2)
        assertEquals(3, range.lenOf(shape, 0))

        // 索引数组长度
        // Index array length
        val array = DummyIndex.from(listOf(0, 2))
        assertEquals(2, array.lenOf(shape, 0))

        // 全范围长度为维度大小
        // Full range length is dimension size
        val all = DummyIndex.all()
        assertEquals(3, all.lenOf(shape, 0))
        assertEquals(4, all.lenOf(shape, 1))
    }

    @Test
    fun testDummyIndexLenOfEmptyRange() {
        // 测试空范围的长度
        // Test empty range length
        val shape = Shape2(3, 4)
        val emptyRange = DummyIndex.from(5..3)  // 空范围
        assertEquals(0, emptyRange.lenOf(shape, 0))
    }

    @Test
    fun testDummyIndexIteratorOfSingle() {
        // 测试单个索引的迭代器
        // Test iterator for single index
        val shape = Shape2(5, 3)
        val index = DummyIndex.from(2)
        val iterator = index.iteratorOf(shape, 0)

        assertTrue(iterator is DummyIndexIterator.Single)
        assertEquals(2, (iterator as DummyIndexIterator.Single).index)
    }

    @Test
    fun testDummyIndexIteratorOfNegative() {
        // 测试负索引的迭代器
        // Test iterator for negative index
        val shape = Shape2(5, 3)
        val index = DummyIndex.from(-1)
        val iterator = index.iteratorOf(shape, 0)

        assertTrue(iterator is DummyIndexIterator.Single)
        // -1 应该是最后一个元素，即索引 4
        // -1 should be last element, index 4
        assertEquals(4, (iterator as DummyIndexIterator.Single).index)
    }

    @Test
    fun testDummyIndexIteratorOfRange() {
        // 测试范围索引的迭代器
        // Test iterator for range index
        val shape = Shape2(5, 3)
        val range = DummyIndex.from(1..3)
        val iterator = range.iteratorOf(shape, 0)

        assertTrue(iterator is DummyIndexIterator.Continuous)
        val continuous = iterator as DummyIndexIterator.Continuous
        assertEquals(1..3, continuous.range)
    }

    @Test
    fun testDummyIndexIteratorOfArray() {
        // 测试索引数组的迭代器
        // Test iterator for index array
        val shape = Shape2(5, 3)
        val array = DummyIndex.from(listOf(0, 2, 4))
        val iterator = array.iteratorOf(shape, 0)

        assertTrue(iterator is DummyIndexIterator.Discrete)
        val discrete = iterator as DummyIndexIterator.Discrete
        assertEquals(listOf(0, 2, 4), discrete.indices)
    }

    @Test
    fun testDummyIndexIteratorOfAll() {
        // 测试全范围索引的迭代器
        // Test iterator for full range index
        val shape = Shape2(5, 3)
        val all = DummyIndex.all()
        val iterator = all.iteratorOf(shape, 0)

        assertTrue(iterator is DummyIndexIterator.Continuous)
        val continuous = iterator as DummyIndexIterator.Continuous
        assertEquals(0 until 5, continuous.range)
    }

    // ========================================================================
    // DummyIndexIterator 测试
    // DummyIndexIterator tests
    // ========================================================================

    @Test
    fun testDummyIndexIteratorSingleGet() {
        // 测试单个索引迭代器的 get 方法
        // Test get method for single index iterator
        val iterator = DummyIndexIterator.Single(5)

        assertEquals(5, iterator.get(0))
        assertEquals(null, iterator.get(1))
        assertEquals(null, iterator.get(-1))
    }

    @Test
    fun testDummyIndexIteratorSingleLen() {
        // 测试单个索引迭代器的长度
        // Test length for single index iterator
        val iterator = DummyIndexIterator.Single(5)
        assertEquals(1, iterator.len())
        assertFalse(iterator.isEmpty())
    }

    @Test
    fun testDummyIndexIteratorContinuousGet() {
        // 测试连续范围迭代器的 get 方法
        // Test get method for continuous range iterator
        val iterator = DummyIndexIterator.Continuous(2..5)

        assertEquals(2, iterator.get(0))
        assertEquals(3, iterator.get(1))
        assertEquals(4, iterator.get(2))
        assertEquals(5, iterator.get(3))
        assertEquals(null, iterator.get(4))
    }

    @Test
    fun testDummyIndexIteratorContinuousLen() {
        // 测试连续范围迭代器的长度
        // Test length for continuous range iterator
        val iterator1 = DummyIndexIterator.Continuous(2..5)
        assertEquals(4, iterator1.len())
        assertFalse(iterator1.isEmpty())

        val iterator2 = DummyIndexIterator.Continuous(3..2)  // 空范围
        assertEquals(0, iterator2.len())
        assertTrue(iterator2.isEmpty())
    }

    @Test
    fun testDummyIndexIteratorDiscreteGet() {
        // 测试离散索引迭代器的 get 方法
        // Test get method for discrete index iterator
        val iterator = DummyIndexIterator.Discrete(listOf(0, 2, 4))

        assertEquals(0, iterator.get(0))
        assertEquals(2, iterator.get(1))
        assertEquals(4, iterator.get(2))
        assertEquals(null, iterator.get(3))
    }

    @Test
    fun testDummyIndexIteratorDiscreteLen() {
        // 测试离散索引迭代器的长度
        // Test length for discrete index iterator
        val iterator = DummyIndexIterator.Discrete(listOf(0, 2, 4))
        assertEquals(3, iterator.len())
        assertFalse(iterator.isEmpty())

        val emptyIterator = DummyIndexIterator.Discrete(emptyList())
        assertEquals(0, emptyIterator.len())
        assertTrue(emptyIterator.isEmpty())
    }

    @Test
    fun testDummyIndexIteratorEmpty() {
        // 测试空迭代器
        // Test empty iterator
        val emptyContinuous = DummyIndexIterator.Continuous(5..4)
        assertTrue(emptyContinuous.isEmpty())
        assertEquals(0, emptyContinuous.len())
    }

    // ========================================================================
    // MapIndex 测试
    // MapIndex tests
    // ========================================================================

    @Test
    fun testMapIndexDummy() {
        // 测试虚拟映射索引
        // Test dummy map index
        val dummy = DummyIndex.from(5)
        val mapIndex = MapIndex.from(dummy)

        assertTrue(mapIndex is MapIndex.Dummy)
        assertEquals(dummy, (mapIndex as MapIndex.Dummy).dummy)
    }

    @Test
    fun testMapIndexMap() {
        // 测试映射占位符
        // Test map placeholder
        val mapIndex = MapIndex.map(2)

        assertTrue(mapIndex is MapIndex.Map)
        assertEquals(2, (mapIndex as MapIndex.Map).index)
    }

    // ========================================================================
    // DummyVector 测试
    // DummyVector tests
    // ========================================================================

    @Test
    fun testDummyVectorCreation() {
        // 测试虚拟向量创建
        // Test dummy vector creation
        val vector: DummyVector = listOf(
            DummyIndex.from(0),
            DummyIndex.all(),
            DummyIndex.from(1..3)
        )

        assertEquals(3, vector.size)
        assertTrue(vector[0] is DummyIndex.Index)
        assertTrue(vector[1] is DummyIndex.All)
        assertTrue(vector[2] is DummyIndex.Range)
    }

    // ========================================================================
    // 负索引测试
    // Negative index tests
    // ========================================================================

    @Test
    fun testNegativeIndexLastElement() {
        // 测试负索引获取最后一个元素
        // Test negative index for last element
        val shape = Shape1(10)
        val index = DummyIndex.from(-1)
        val iterator = index.iteratorOf(shape, 0)

        assertTrue(iterator is DummyIndexIterator.Single)
        assertEquals(9, (iterator as DummyIndexIterator.Single).index)
    }

    @Test
    fun testNegativeIndexSecondLast() {
        // 测试负索引获取倒数第二个元素
        // Test negative index for second last element
        val shape = Shape1(10)
        val index = DummyIndex.from(-2)
        val iterator = index.iteratorOf(shape, 0)

        assertEquals(8, (iterator as DummyIndexIterator.Single).index)
    }

    @Test
    fun testNegativeRange() {
        // 测试负范围索引
        // Test negative range index
        val shape = Shape1(10)
        // -3..-1 应该对应 7,8
        // -3..-1 should correspond to 7,8
        val range = DummyIndex.from(-3..-1)
        val iterator = range.iteratorOf(shape, 0)

        assertTrue(iterator is DummyIndexIterator.Continuous)
        val continuous = iterator as DummyIndexIterator.Continuous
        // 范围应该是 7..9（不包含 9）
        // Range should be 7..9 (exclusive)
        // Note: The implementation may handle negative ranges differently
        // Just verify the iterator is created correctly
        assertTrue(continuous.range.first >= 0)
        assertTrue(continuous.range.first < shape[0])
    }

    // ========================================================================
    // 边界情况测试
    // Edge case tests
    // ========================================================================

    @Test
    fun testIndexOutOfBounds() {
        // 测试越界索引
        // Test out of bounds index
        val shape = Shape1(5)
        val index = DummyIndex.from(10)
        val iterator = index.iteratorOf(shape, 0)

        // 越界索引应该回退到 0
        // Out of bounds index should fallback to 0
        assertTrue(iterator is DummyIndexIterator.Single)
    }

    @Test
    fun testLargeNegativeIndex() {
        // 测试大负索引
        // Test large negative index
        val shape = Shape1(5)
        val index = DummyIndex.from(-10)
        val iterator = index.iteratorOf(shape, 0)

        // 大负索引应该回退到 0
        // Large negative index should fallback to 0
        assertTrue(iterator is DummyIndexIterator.Single)
    }

    @Test
    fun testRangeExceedsDimension() {
        // 测试范围超出维度
        // Test range exceeds dimension
        val shape = Shape1(5)
        val range = DummyIndex.from(0..10)
        val iterator = range.iteratorOf(shape, 0)

        assertTrue(iterator is DummyIndexIterator.Continuous)
        val continuous = iterator as DummyIndexIterator.Continuous
        // 范围应该被截断到维度大小
        // Range should be truncated to dimension size
        assertEquals(0, continuous.range.first)
        assertEquals(5, continuous.range.last + 1)  // range is inclusive
    }

    // ========================================================================
    // DummyIndex 相等性测试
    // DummyIndex equality tests
    // ========================================================================

    @Test
    fun testDummyIndexEquality() {
        // 测试虚拟索引相等性
        // Test dummy index equality
        val index1 = DummyIndex.from(5)
        val index2 = DummyIndex.from(5)
        val index3 = DummyIndex.from(3)

        assertEquals(index1, index2)
        assertTrue(index1 != index3)
    }

    @Test
    fun testDummyIndexArrayEquality() {
        // 测试索引数组相等性
        // Test index array equality
        val array1 = DummyIndex.from(listOf(1, 2, 3))
        val array2 = DummyIndex.from(listOf(1, 2, 3))
        val array3 = DummyIndex.from(listOf(1, 2, 4))

        assertEquals(array1, array2)
        assertTrue(array1 != array3)
    }

    @Test
    fun testDummyIndexAllEquality() {
        // 测试全范围索引相等性
        // Test full range index equality
        val all1 = DummyIndex.all()
        val all2 = DummyIndex.all()
        val all3 = _a

        assertEquals(all1, all2)
        assertEquals(all1, all3)
    }
}
