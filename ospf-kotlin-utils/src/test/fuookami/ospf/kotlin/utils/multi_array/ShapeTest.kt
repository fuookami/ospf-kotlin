package fuookami.ospf.kotlin.utils.multi_array

import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/**
 * 形状测试
 * Shape tests
 */
class ShapeTest {

    // ========================================================================
    // Shape1 测试
    // Shape1 tests
    // ========================================================================

    @Test
    fun testShape1Creation() {
        // 测试一维形状创建
        // Test 1D shape creation
        val shape = Shape1(5)
        assertEquals(1, shape.dimension)
        assertEquals(5, shape.size)
        assertEquals(5, shape[0])
    }

    @Test
    fun testShape1Offsets() {
        // 测试一维形状偏移量
        // Test 1D shape offsets
        val shape = Shape1(10)
        assertArrayEquals(intArrayOf(1), shape.offsets)
    }

    @Test
    fun testShape1Index() {
        // 测试一维形状索引计算
        // Test 1D shape index calculation
        val shape = Shape1(5)
        assertEquals(0, shape.index(intArrayOf(0)))
        assertEquals(2, shape.index(intArrayOf(2)))
        assertEquals(4, shape.index(intArrayOf(4)))
    }

    @Test
    fun testShape1Vector() {
        // 测试一维形状向量转换
        // Test 1D shape vector conversion
        val shape = Shape1(5)
        assertArrayEquals(intArrayOf(0), shape.vector(0))
        assertArrayEquals(intArrayOf(2), shape.vector(2))
        assertArrayEquals(intArrayOf(4), shape.vector(4))
    }

    @Test
    fun testShape1IndexOutOfBounds() {
        // 测试一维形状越界异常
        // Test 1D shape out of bounds exception
        val shape = Shape1(5)
        assertFailsWith<OutOfShapeException> { shape.index(intArrayOf(5)) }
        assertFailsWith<OutOfShapeException> { shape.index(intArrayOf(-1)) }
    }

    @Test
    fun testShape1VectorOutOfBounds() {
        // 测试一维形状向量越界异常
        // Test 1D shape vector out of bounds exception
        val shape = Shape1(5)
        assertFailsWith<ArrayIndexOutOfBoundsException> { shape.vector(5) }
        assertFailsWith<ArrayIndexOutOfBoundsException> { shape.vector(-1) }
    }

    @Test
    fun testShape1StorageOrder() {
        // 测试一维形状存储顺序
        // Test 1D shape storage order
        val shape1 = Shape1.withOrder(5, StorageOrder.RowMajor)
        assertEquals(StorageOrder.RowMajor, shape1.storageOrder)
        
        val shape2 = Shape1.withOrder(5, StorageOrder.ColumnMajor)
        assertEquals(StorageOrder.ColumnMajor, shape2.storageOrder)
    }

    // ========================================================================
    // Shape2 测试
    // Shape2 tests
    // ========================================================================

    @Test
    fun testShape2Creation() {
        // 测试二维形状创建
        // Test 2D shape creation
        val shape = Shape2(3, 4)
        assertEquals(2, shape.dimension)
        assertEquals(12, shape.size)
        assertEquals(3, shape[0])
        assertEquals(4, shape[1])
    }

    @Test
    fun testShape2RowMajor() {
        // 测试二维行主序形状
        // Test 2D row-major shape
        val shape = Shape2(3, 4)
        assertEquals(StorageOrder.RowMajor, shape.storageOrder)
        assertArrayEquals(intArrayOf(4, 1), shape.offsets)
    }

    @Test
    fun testShape2ColumnMajor() {
        // 测试二维列主序形状
        // Test 2D column-major shape
        val shape = Shape2.withOrder(3, 4, StorageOrder.ColumnMajor)
        assertEquals(StorageOrder.ColumnMajor, shape.storageOrder)
        assertArrayEquals(intArrayOf(1, 3), shape.offsets)
    }

    @Test
    fun testShape2IndexRowMajor() {
        // 测试二维行主序索引计算
        // Test 2D row-major index calculation
        val shape = Shape2(3, 4)
        
        assertEquals(0, shape.index(intArrayOf(0, 0)))
        assertEquals(1, shape.index(intArrayOf(0, 1)))
        assertEquals(2, shape.index(intArrayOf(0, 2)))
        assertEquals(3, shape.index(intArrayOf(0, 3)))
        assertEquals(4, shape.index(intArrayOf(1, 0)))
        assertEquals(5, shape.index(intArrayOf(1, 1)))
        assertEquals(11, shape.index(intArrayOf(2, 3)))
    }

    @Test
    fun testShape2IndexColumnMajor() {
        // 测试二维列主序索引计算
        // Test 2D column-major index calculation
        val shape = Shape2.withOrder(3, 4, StorageOrder.ColumnMajor)
        
        assertEquals(0, shape.index(intArrayOf(0, 0)))
        assertEquals(1, shape.index(intArrayOf(1, 0)))
        assertEquals(2, shape.index(intArrayOf(2, 0)))
        assertEquals(3, shape.index(intArrayOf(0, 1)))
        assertEquals(4, shape.index(intArrayOf(1, 1)))
        assertEquals(11, shape.index(intArrayOf(2, 3)))
    }

    @Test
    fun testShape2VectorRowMajor() {
        // 测试二维行主序向量转换
        // Test 2D row-major vector conversion
        val shape = Shape2(3, 4)
        
        assertArrayEquals(intArrayOf(0, 0), shape.vector(0))
        assertArrayEquals(intArrayOf(0, 1), shape.vector(1))
        assertArrayEquals(intArrayOf(0, 3), shape.vector(3))
        assertArrayEquals(intArrayOf(1, 0), shape.vector(4))
        assertArrayEquals(intArrayOf(1, 1), shape.vector(5))
        assertArrayEquals(intArrayOf(2, 3), shape.vector(11))
    }

    @Test
    fun testShape2VectorColumnMajor() {
        // 测试二维列主序向量转换
        // Test 2D column-major vector conversion
        val shape = Shape2.withOrder(3, 4, StorageOrder.ColumnMajor)
        
        assertArrayEquals(intArrayOf(0, 0), shape.vector(0))
        assertArrayEquals(intArrayOf(1, 0), shape.vector(1))
        assertArrayEquals(intArrayOf(2, 0), shape.vector(2))
        assertArrayEquals(intArrayOf(0, 1), shape.vector(3))
        assertArrayEquals(intArrayOf(1, 1), shape.vector(4))
        assertArrayEquals(intArrayOf(2, 3), shape.vector(11))
    }

    @Test
    fun testShape2IndexDimensionMismatch() {
        // 测试二维形状维度不匹配异常
        // Test 2D shape dimension mismatch exception
        val shape = Shape2(3, 4)
        assertFailsWith<DimensionMismatchingException> { shape.index(intArrayOf(0)) }
        assertFailsWith<DimensionMismatchingException> { shape.index(intArrayOf(0, 1, 2)) }
    }

    // ========================================================================
    // Shape3 测试
    // Shape3 tests
    // ========================================================================

    @Test
    fun testShape3Creation() {
        // 测试三维形状创建
        // Test 3D shape creation
        val shape = Shape3(2, 3, 4)
        assertEquals(3, shape.dimension)
        assertEquals(24, shape.size)
        assertEquals(2, shape[0])
        assertEquals(3, shape[1])
        assertEquals(4, shape[2])
    }

    @Test
    fun testShape3RowMajor() {
        // 测试三维行主序形状
        // Test 3D row-major shape
        val shape = Shape3(2, 3, 4)
        assertEquals(StorageOrder.RowMajor, shape.storageOrder)
        assertArrayEquals(intArrayOf(12, 4, 1), shape.offsets)
    }

    @Test
    fun testShape3ColumnMajor() {
        // 测试三维列主序形状
        // Test 3D column-major shape
        val shape = Shape3.withOrder(2, 3, 4, StorageOrder.ColumnMajor)
        assertEquals(StorageOrder.ColumnMajor, shape.storageOrder)
        assertArrayEquals(intArrayOf(1, 2, 6), shape.offsets)
    }

    @Test
    fun testShape3IndexRowMajor() {
        // 测试三维行主序索引计算
        // Test 3D row-major index calculation
        val shape = Shape3(2, 3, 4)
        
        assertEquals(0, shape.index(intArrayOf(0, 0, 0)))
        assertEquals(1, shape.index(intArrayOf(0, 0, 1)))
        assertEquals(4, shape.index(intArrayOf(0, 1, 0)))
        assertEquals(12, shape.index(intArrayOf(1, 0, 0)))
        assertEquals(23, shape.index(intArrayOf(1, 2, 3)))
    }

    @Test
    fun testShape3VectorRowMajor() {
        // 测试三维行主序向量转换
        // Test 3D row-major vector conversion
        val shape = Shape3(2, 3, 4)
        
        assertArrayEquals(intArrayOf(0, 0, 0), shape.vector(0))
        assertArrayEquals(intArrayOf(0, 0, 1), shape.vector(1))
        assertArrayEquals(intArrayOf(0, 1, 0), shape.vector(4))
        assertArrayEquals(intArrayOf(1, 0, 0), shape.vector(12))
        assertArrayEquals(intArrayOf(1, 2, 3), shape.vector(23))
    }

    // ========================================================================
    // Shape4 测试
    // Shape4 tests
    // ========================================================================

    @Test
    fun testShape4Creation() {
        // 测试四维形状创建
        // Test 4D shape creation
        val shape = Shape4(2, 3, 4, 5)
        assertEquals(4, shape.dimension)
        assertEquals(120, shape.size)
        assertEquals(2, shape[0])
        assertEquals(3, shape[1])
        assertEquals(4, shape[2])
        assertEquals(5, shape[3])
    }

    @Test
    fun testShape4RowMajor() {
        // 测试四维行主序形状
        // Test 4D row-major shape
        val shape = Shape4(2, 3, 4, 5)
        assertEquals(StorageOrder.RowMajor, shape.storageOrder)
        assertArrayEquals(intArrayOf(60, 20, 5, 1), shape.offsets)
    }

    @Test
    fun testShape4ColumnMajor() {
        // 测试四维列主序形状
        // Test 4D column-major shape
        val shape = Shape4.withOrder(2, 3, 4, 5, StorageOrder.ColumnMajor)
        assertEquals(StorageOrder.ColumnMajor, shape.storageOrder)
        assertArrayEquals(intArrayOf(1, 2, 6, 24), shape.offsets)
    }

    // ========================================================================
    // DynShape 测试
    // DynShape tests
    // ========================================================================

    @Test
    fun testDynShapeCreation() {
        // 测试动态形状创建
        // Test dynamic shape creation
        val shape = DynShape(intArrayOf(2, 3, 4))
        assertEquals(3, shape.dimension)
        assertEquals(24, shape.size)
        assertEquals(2, shape[0])
        assertEquals(3, shape[1])
        assertEquals(4, shape[2])
    }

    @Test
    fun testDynShapeRowMajor() {
        // 测试动态行主序形状
        // Test dynamic row-major shape
        val shape = DynShape(intArrayOf(2, 3, 4))
        assertEquals(StorageOrder.RowMajor, shape.storageOrder)
        assertArrayEquals(intArrayOf(12, 4, 1), shape.offsets)
    }

    @Test
    fun testDynShapeColumnMajor() {
        // 测试动态列主序形状
        // Test dynamic column-major shape
        val shape = DynShape.withOrder(intArrayOf(2, 3, 4), StorageOrder.ColumnMajor)
        assertEquals(StorageOrder.ColumnMajor, shape.storageOrder)
        assertArrayEquals(intArrayOf(1, 2, 6), shape.offsets)
    }

    @Test
    fun testDynShapeIndex() {
        // 测试动态形状索引计算
        // Test dynamic shape index calculation
        val shape = DynShape(intArrayOf(2, 3, 4))
        
        assertEquals(0, shape.index(intArrayOf(0, 0, 0)))
        assertEquals(1, shape.index(intArrayOf(0, 0, 1)))
        assertEquals(4, shape.index(intArrayOf(0, 1, 0)))
        assertEquals(12, shape.index(intArrayOf(1, 0, 0)))
        assertEquals(23, shape.index(intArrayOf(1, 2, 3)))
    }

    @Test
    fun testDynShapeVector() {
        // 测试动态形状向量转换
        // Test dynamic shape vector conversion
        val shape = DynShape(intArrayOf(2, 3, 4))
        
        assertArrayEquals(intArrayOf(0, 0, 0), shape.vector(0))
        assertArrayEquals(intArrayOf(0, 0, 1), shape.vector(1))
        assertArrayEquals(intArrayOf(0, 1, 0), shape.vector(4))
        assertArrayEquals(intArrayOf(1, 0, 0), shape.vector(12))
        assertArrayEquals(intArrayOf(1, 2, 3), shape.vector(23))
    }

    @Test
    fun testDynShapeWithOrder() {
        // 测试动态形状转换存储顺序
        // Test dynamic shape storage order conversion
        val shapeRow = DynShape.withOrder(intArrayOf(2, 3), StorageOrder.RowMajor)
        val shapeCol = shapeRow.withStorageOrder(StorageOrder.ColumnMajor)
        
        assertEquals(StorageOrder.ColumnMajor, shapeCol.storageOrder)
        assertEquals(6, shapeCol.size)
        assertArrayEquals(intArrayOf(1, 2), shapeCol.offsets)
    }

    @Test
    fun testDynShapeEquality() {
        // 测试动态形状相等性
        // Test dynamic shape equality
        val shape1 = DynShape(intArrayOf(2, 3, 4))
        val shape2 = DynShape(intArrayOf(2, 3, 4))
        val shape3 = DynShape(intArrayOf(2, 3, 5))
        
        assertEquals(shape1, shape2)
        assertEquals(shape1.hashCode(), shape2.hashCode())
        assertTrue(shape1 != shape3)
    }

    @Test
    fun testDynShapeClone() {
        // 测试动态形状克隆
        // Test dynamic shape clone
        val shape1 = DynShape(intArrayOf(2, 3, 4))
        val shape2 = shape1.copy()
        
        assertEquals(shape1, shape2)
        assertEquals(shape1.hashCode(), shape2.hashCode())
    }

    // ========================================================================
    // Shape 通用测试
    // Shape common tests
    // ========================================================================

    @Test
    fun testShapeIndices() {
        // 测试形状索引范围
        // Test shape index range
        val shape = Shape2(3, 4)
        val indices = shape.indices
        assertEquals(0 until 2, indices)
    }

    @Test
    fun testShapeIsEmpty() {
        // 测试形状空检查
        // Test shape empty check
        val shape1 = Shape2(3, 4)
        val shape2 = Shape2(0, 4)
        val shape3 = Shape2(3, 0)
        
        assertFalse(shape1.isEmpty())
        assertTrue(shape2.isEmpty())
        assertTrue(shape3.isEmpty())
    }

    @Test
    fun testShapeZero() {
        // 测试零向量创建
        // Test zero vector creation
        val shape = Shape3(2, 3, 4)
        val zero = shape.zero()
        assertArrayEquals(intArrayOf(0, 0, 0), zero)
    }

    @Test
    fun testShapeNext() {
        // 测试下一个向量
        // Test next vector
        val shape = Shape2(2, 3)
        
        var vector = intArrayOf(0, 0)
        val results = mutableListOf<IntArray>()
        results.add(vector.clone())
        
        while (true) {
            val next = shape.next(vector) ?: break
            vector = next
            results.add(vector.clone())
        }
        
        assertEquals(6, results.size)
        assertArrayEquals(intArrayOf(0, 0), results[0])
        assertArrayEquals(intArrayOf(0, 1), results[1])
        assertArrayEquals(intArrayOf(0, 2), results[2])
        assertArrayEquals(intArrayOf(1, 0), results[3])
        assertArrayEquals(intArrayOf(1, 1), results[4])
        assertArrayEquals(intArrayOf(1, 2), results[5])
    }

    @Test
    fun testShapeActualIndex() {
        // 测试实际索引计算（处理负索引）
        // Test actual index calculation (negative indices)
        val shape = Shape1(5)
        
        // 正索引
        // Positive indices
        assertEquals(0, shape.actualIndex(0, 0))
        assertEquals(2, shape.actualIndex(0, 2))
        assertEquals(4, shape.actualIndex(0, 4))
        assertEquals(null, shape.actualIndex(0, 5))
        
        // 负索引
        // Negative indices
        assertEquals(4, shape.actualIndex(0, -1))
        assertEquals(3, shape.actualIndex(0, -2))
        assertEquals(0, shape.actualIndex(0, -5))
        assertEquals(null, shape.actualIndex(0, -6))
    }

    @Test
    fun testShapeActualIndex2D() {
        // 测试二维实际索引
        // Test 2D actual index
        val shape = Shape2(3, 4)
        
        assertEquals(0, shape.actualIndex(0, 0))
        assertEquals(2, shape.actualIndex(0, -1))
        assertEquals(null, shape.actualIndex(0, -4))
        
        assertEquals(0, shape.actualIndex(1, 0))
        assertEquals(3, shape.actualIndex(1, -1))
        assertEquals(null, shape.actualIndex(1, -5))
    }

    @Test
    fun testShapeOffset() {
        // 测试维度偏移量
        // Test dimension offset
        val shape = Shape3(2, 3, 4)
        
        assertEquals(12, shape.offset(0))
        assertEquals(4, shape.offset(1))
        assertEquals(1, shape.offset(2))
        
        assertFailsWith<DimensionMismatchingException> { shape.offset(3) }
        assertFailsWith<DimensionMismatchingException> { shape.offset(-1) }
    }

    @Test
    fun testShapeUdimension() {
        // 测试无符号维度
        // Test unsigned dimension
        val shape = Shape2(3, 4)
        assertEquals(2uL, shape.udimension.value)
    }

    @Test
    fun testShapeUsize() {
        // 测试无符号大小
        // Test unsigned size
        val shape = Shape2(3, 4)
        assertEquals(12uL, shape.usize.value)
    }

    // ========================================================================
    // 辅助函数
    // Helper functions
    // ========================================================================

    private fun assertArrayEquals(expected: IntArray, actual: IntArray) {
        assertEquals(expected.size, actual.size, "Array sizes differ")
        for (i in expected.indices) {
            assertEquals(expected[i], actual[i], "Element at index $i differs")
        }
    }
}