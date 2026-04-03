package fuookami.ospf.kotlin.multiarray

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 数据框测试
 * DataFrame tests
 */
class DataFrameTest {

    // ========================================================================
    // DataFrame 创建测试
    // DataFrame creation tests
    // ========================================================================

    @Test
    fun testDataFrameCreation() {
        // 测试数据框创建
        // Test DataFrame creation
        val df = DataFrame<String>(3, 2, listOf("Name", "Value"))

        assertEquals(3, df.getNRows())
        assertEquals(2, df.getNCols())
        assertEquals(listOf("Name", "Value"), df.columnNames)
    }

    @Test
    fun testDataFrameColumnIndex() {
        // 测试列索引
        // Test column index
        val df = DataFrame<String>(3, 2, listOf("A", "B"))

        assertEquals(0, df.getColumnIndex("A"))
        assertEquals(1, df.getColumnIndex("B"))
        assertEquals(null, df.getColumnIndex("C"))
    }

    @Test
    fun testDataFrameColumnNameMismatch() {
        // 测试列名数量不匹配
        // Test column name count mismatch
        assertFailsWith<IllegalArgumentException> {
            DataFrame<String>(3, 2, listOf("A"))  // 列名数量不等于列数
        }
    }

    // ========================================================================
    // DataFrame 访问测试
    // DataFrame access tests
    // ========================================================================

    @Test
    fun testDataFrameGetSet() {
        // 测试获取和设置
        // Test get and set
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 10)
        df.set(0, 1, 20)
        df.set(1, 0, 30)

        assertEquals(10, df.get(0, 0))
        assertEquals(20, df.get(0, 1))
        assertEquals(30, df.get(1, 0))
        assertEquals(null, df.get(1, 1))  // 未设置
    }

    @Test
    fun testDataFrameGetSetByName() {
        // 测试通过列名获取和设置
        // Test get and set by column name
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.setByName(0, "A", 100)
        df.setByName(0, "B", 200)

        assertEquals(100, df.getByName(0, "A"))
        assertEquals(200, df.getByName(0, "B"))
    }

    @Test
    fun testDataFrameGetByNameInvalidColumn() {
        // 测试无效列名
        // Test invalid column name
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        assertFailsWith<IllegalArgumentException> {
            df.getByName(0, "C")
        }
    }

    @Test
    fun testDataFrameIndexOutOfBounds() {
        // 测试索引越界
        // Test index out of bounds
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        assertFailsWith<IllegalArgumentException> {
            df.get(5, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            df.get(0, 5)
        }
    }

    // ========================================================================
    // DataFrame 行和列测试
    // DataFrame row and column tests
    // ========================================================================

    @Test
    fun testDataFrameGetRow() {
        // 测试获取行
        // Test get row
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(0, 1, 2)
        df.set(1, 0, 3)
        df.set(1, 1, 4)

        assertEquals(listOf(1, 2), df.getRow(0))
        assertEquals(listOf(3, 4), df.getRow(1))
        assertEquals(listOf(null, null), df.getRow(2))
    }

    @Test
    fun testDataFrameGetColumn() {
        // 测试获取列
        // Test get column
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(1, 0, 2)
        df.set(2, 0, 3)
        df.set(0, 1, 4)
        df.set(1, 1, 5)
        df.set(2, 1, 6)

        assertEquals(listOf(1, 2, 3), df.getColumn(0))
        assertEquals(listOf(4, 5, 6), df.getColumn(1))
    }

    @Test
    fun testDataFrameGetColumnByName() {
        // 测试通过列名获取列
        // Test get column by name
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(1, 0, 2)
        df.set(2, 0, 3)

        assertEquals(listOf(1, 2, 3), df.getColumnByName("A"))
    }

    @Test
    fun testDataFrameRowIndexOutOfBounds() {
        // 测试行索引越界
        // Test row index out of bounds
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        assertFailsWith<IllegalArgumentException> {
            df.getRow(5)
        }
    }

    @Test
    fun testDataFrameColumnIndexOutOfBounds() {
        // 测试列索引越界
        // Test column index out of bounds
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        assertFailsWith<IllegalArgumentException> {
            df.getColumn(5)
        }
    }

    // ========================================================================
    // DataFrame 子数据框测试
    // DataFrame sub-DataFrame tests
    // ========================================================================

    @Test
    fun testDataFrameSubDataFrame() {
        // 测试子数据框
        // Test sub-DataFrame
        val df = DataFrame<Int>(5, 4, listOf("A", "B", "C", "D"))

        // 填充数据
        for (i in 0 until 5) {
            for (j in 0 until 4) {
                df.set(i, j, i * 10 + j)
            }
        }

        val subDf = df.subDataFrame(rows = 1..3, cols = 1..2)

        assertEquals(3, subDf.getNRows())
        assertEquals(2, subDf.getNCols())
        assertEquals(listOf("B", "C"), subDf.columnNames)
    }

    @Test
    fun testDataFrameSubDataFrameValues() {
        // 测试子数据框值
        // Test sub-DataFrame values
        val df = DataFrame<Int>(5, 4, listOf("A", "B", "C", "D"))

        for (i in 0 until 5) {
            for (j in 0 until 4) {
                df.set(i, j, i * 10 + j)
            }
        }

        val subDf = df.subDataFrame(rows = 1..2, cols = 1..2)

        assertEquals(11, subDf.get(0, 0))  // 原 [1, 1]
        assertEquals(12, subDf.get(0, 1))  // 原 [1, 2]
        assertEquals(21, subDf.get(1, 0))  // 原 [2, 1]
        assertEquals(22, subDf.get(1, 1))  // 原 [2, 2]
    }

    // ========================================================================
    // DataFrame 选择测试
    // DataFrame select tests
    // ========================================================================

    @Test
    fun testDataFrameSelect() {
        // 测试选择列
        // Test select columns
        val df = DataFrame<Int>(3, 4, listOf("A", "B", "C", "D"))

        for (i in 0 until 3) {
            for (j in 0 until 4) {
                df.set(i, j, i * 10 + j)
            }
        }

        val selected = df.select("A", "C")

        assertEquals(3, selected.getNRows())
        assertEquals(2, selected.getNCols())
        assertEquals(listOf("A", "C"), selected.columnNames)
        assertEquals(0, selected.get(0, 0))
        assertEquals(2, selected.get(0, 1))
    }

    @Test
    fun testDataFrameSelectInvalidColumn() {
        // 测试选择无效列
        // Test select invalid column
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        assertFailsWith<IllegalArgumentException> {
            df.select("A", "C")
        }
    }

    // ========================================================================
    // DataFrame 过滤测试
    // DataFrame filter tests
    // ========================================================================

    @Test
    fun testDataFrameFilter() {
        // 测试过滤行
        // Test filter rows
        val df = DataFrame<Int>(5, 2, listOf("A", "B"))

        for (i in 0 until 5) {
            df.set(i, 0, i)
            df.set(i, 1, i * 2)
        }

        // 过滤出 A > 2 的行
        // Filter rows where A > 2
        val filtered = df.filter { row ->
            val a = row[0] as? Int ?: 0
            a > 2
        }

        assertEquals(2, filtered.getNRows())  // 只有 3 和 4 满足条件
        assertEquals(3, filtered.get(0, 0))
        assertEquals(4, filtered.get(1, 0))
    }

    @Test
    fun testDataFrameFilterAllMatch() {
        // 测试过滤所有行匹配
        // Test filter all rows match
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        for (i in 0 until 3) {
            df.set(i, 0, i)
            df.set(i, 1, i * 2)
        }

        val filtered = df.filter { true }

        assertEquals(3, filtered.getNRows())
    }

    @Test
    fun testDataFrameFilterNoneMatch() {
        // 测试过滤没有行匹配
        // Test filter no rows match
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        for (i in 0 until 3) {
            df.set(i, 0, i)
            df.set(i, 1, i * 2)
        }

        val filtered = df.filter { false }

        assertEquals(0, filtered.getNRows())
    }

    // ========================================================================
    // DataFrame 添加行测试
    // DataFrame add row tests
    // ========================================================================

    @Test
    fun testDataFrameCopyWithAddedRow() {
        // 测试添加行
        // Test add row
        val df = DataFrame<Int>(2, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(0, 1, 2)
        df.set(1, 0, 3)
        df.set(1, 1, 4)

        val newDf = df.copyWithAddedRow(listOf(5, 6))

        assertEquals(2, df.getNRows())  // 原数据框不变
        assertEquals(3, newDf.getNRows())
        assertEquals(5, newDf.get(2, 0))
        assertEquals(6, newDf.get(2, 1))
    }

    @Test
    fun testDataFrameCopyWithAddedRowSizeMismatch() {
        // 测试添加行大小不匹配
        // Test add row size mismatch
        val df = DataFrame<Int>(2, 2, listOf("A", "B"))

        assertFailsWith<IllegalArgumentException> {
            df.copyWithAddedRow(listOf(1, 2, 3))  // 值数量不等于列数
        }
    }

    // ========================================================================
    // DataFrame Map 转换测试
    // DataFrame Map conversion tests
    // ========================================================================

    @Test
    fun testDataFrameToMap() {
        // 测试转换为 Map
        // Test convert to Map
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(1, 0, 2)
        df.set(2, 0, 3)
        df.set(0, 1, 4)
        df.set(1, 1, 5)
        df.set(2, 1, 6)

        val map = df.toMap()

        assertEquals(2, map.size)
        assertEquals(listOf(1, 2, 3), map["A"])
        assertEquals(listOf(4, 5, 6), map["B"])
    }

    @Test
    fun testDataFrameFromMap() {
        // 测试从 Map 创建
        // Test create from Map
        val map = mapOf(
            "A" to listOf(1, 2, 3),
            "B" to listOf(4, 5, 6)
        )

        val df = DataFrame.fromMap(map)

        assertEquals(3, df.getNRows())
        assertEquals(2, df.getNCols())
        assertEquals(listOf("A", "B"), df.columnNames)
        assertEquals(1, df.get(0, 0))
        assertEquals(6, df.get(2, 1))
    }

    @Test
    fun testDataFrameFromMapLengthMismatch() {
        // 测试从 Map 创建长度不匹配
        // Test create from Map length mismatch
        val map = mapOf(
            "A" to listOf(1, 2, 3),
            "B" to listOf(4, 5)  // 长度不同
        )

        assertFailsWith<IllegalArgumentException> {
            DataFrame.fromMap(map)
        }
    }

    @Test
    fun testDataFrameFromEmptyMap() {
        // 测试从空 Map 创建
        // Test create from empty Map
        val map = emptyMap<String, List<Int>>()

        val df = DataFrame.fromMap(map)

        assertEquals(0, df.getNRows())
        assertEquals(0, df.getNCols())
    }

    // ========================================================================
    // DataFrame 构建器测试
    // DataFrame builder tests
    // ========================================================================

    @Test
    fun testDataFrameBuilder() {
        // 测试构建器
        // Test builder
        val df = DataFrame.build<Int>("A", "B") {
            row(1, 2)
            row(3, 4)
            row(5, 6)
        }

        assertEquals(3, df.getNRows())
        assertEquals(2, df.getNCols())
        assertEquals(1, df.get(0, 0))
        assertEquals(6, df.get(2, 1))
    }

    @Test
    fun testDataFrameBuilderRows() {
        // 测试构建器多行
        // Test builder multiple rows
        val df = DataFrame.build<Int>("A", "B") {
            rows(
                listOf(
                    listOf(1, 2),
                    listOf(3, 4),
                    listOf(5, 6)
                )
            )
        }

        assertEquals(3, df.getNRows())
        assertEquals(1, df.get(0, 0))
        assertEquals(6, df.get(2, 1))
    }

    @Test
    fun testDataFrameBuilderRowSizeMismatch() {
        // 测试构建器行大小不匹配
        // Test builder row size mismatch
        assertFailsWith<IllegalArgumentException> {
            DataFrame.build<Int>("A", "B") {
                row(1, 2, 3)  // 值数量不等于列数
            }
        }
    }

    // ========================================================================
    // 便捷函数测试
    // Convenience function tests
    // ========================================================================

    @Test
    fun testDataFrameOf() {
        // 测试便捷创建函数
        // Test convenience creation function
        val df = dataFrameOf(
            "A" to listOf(1, 2, 3),
            "B" to listOf(4, 5, 6)
        )

        assertEquals(3, df.getNRows())
        assertEquals(2, df.getNCols())
        assertEquals(1, df.get(0, 0))
        assertEquals(6, df.get(2, 1))
    }

    @Test
    fun testDataFrameFromRows() {
        // 测试从行创建
        // Test create from rows
        val df = dataFrameFromRows(
            listOf("A", "B"),
            listOf(
                listOf(1, 2),
                listOf(3, 4),
                listOf(5, 6)
            )
        )

        assertEquals(3, df.getNRows())
        assertEquals(2, df.getNCols())
        assertEquals(1, df.get(0, 0))
        assertEquals(6, df.get(2, 1))
    }

    @Test
    fun testDataFrameFromRowsLengthMismatch() {
        // 测试从行创建长度不匹配
        // Test create from rows length mismatch
        assertFailsWith<IllegalArgumentException> {
            dataFrameFromRows(
                listOf("A", "B"),
                listOf(
                    listOf(1, 2, 3)  // 行长度不等于列数
                )
            )
        }
    }

    // ========================================================================
    // DataFrame 迭代测试
    // DataFrame iteration tests
    // ========================================================================

    @Test
    fun testDataFrameIterator() {
        // 测试迭代器
        // Test iterator
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(0, 1, 2)
        df.set(1, 0, 3)
        df.set(1, 1, 4)
        df.set(2, 0, 5)
        df.set(2, 1, 6)

        val rows = df.toList()
        assertEquals(3, rows.size)
    }

    @Test
    fun testDataFrameContains() {
        // 测试包含检查
        // Test contains check
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(0, 1, 2)

        // 测试 containsAll
        // Test containsAll
        assertTrue(df.containsAll(listOf(listOf(1, 2), listOf(null, null))))
    }

    @Test
    fun testDataFrameContainsElement() {
        // 测试包含元素
        // Test contains element
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(0, 1, 2)

        assertTrue(df.contains(listOf(1, 2)))
        assertFalse(df.contains(listOf(3, 4)))
    }

    @Test
    fun testDataFrameIsEmpty() {
        // 测试空检查
        // Test empty check
        val df1 = DataFrame<Int>(3, 2, listOf("A", "B"))
        val df2 = DataFrame.empty<Int>("A", "B")

        assertFalse(df1.isEmpty())
        assertTrue(df2.isEmpty())
    }

    // ========================================================================
    // DataFrame toString 测试
    // DataFrame toString tests
    // ========================================================================

    @Test
    fun testDataFrameToString() {
        // 测试 toString 方法
        // Test toString method
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(0, 1, 2)

        val str = df.toString()
        assertTrue(str.contains("A"))
        assertTrue(str.contains("B"))
    }

    @Test
    fun testDataFrameToStringLarge() {
        // 测试大数据框 toString
        // Test large DataFrame toString
        val df = DataFrame<Int>(15, 2, listOf("A", "B"))

        for (i in 0 until 15) {
            df.set(i, 0, i)
            df.set(i, 1, i * 2)
        }

        val str = df.toString()
        assertTrue(str.contains("..."))  // 应该有省略号
    }

    // ========================================================================
    // NullableValue 测试
    // NullableValue tests
    // ========================================================================

    @Test
    fun testNullableValueWithNull() {
        // 测试空值包装
        // Test null value wrapper
        val nv = NullableValue<String>(null)

        assertEquals(null, nv.value)
        assertEquals("null", nv.toString())
    }

    @Test
    fun testNullableValueWithValue() {
        // 测试有值包装
        // Test value wrapper
        val nv = NullableValue("Hello")

        assertEquals("Hello", nv.value)
        assertEquals("Hello", nv.toString())
    }

    @Test
    fun testNullableValueEquality() {
        // 测试相等性
        // Test equality
        val nv1 = NullableValue("Hello")
        val nv2 = NullableValue("Hello")
        val nv3 = NullableValue<String>(null)

        assertEquals(nv1, nv2)
        assertTrue(nv1 != nv3)
    }

    // ========================================================================
    // toNullableMultiArray 测试
    // toNullableMultiArray tests
    // ========================================================================

    @Test
    fun testDataFrameToNullableMultiArray() {
        // 测试转换为 NullableMultiArray
        // Test convert to NullableMultiArray
        val df = DataFrame<Int>(3, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(0, 1, 2)
        df.set(1, 0, 3)
        df.set(1, 1, null)
        df.set(2, 0, null)
        df.set(2, 1, 6)

        val array = df.toNullableMultiArray()

        assertEquals(6, array.size)
    }

    // ========================================================================
    // 边界情况测试
    // Edge case tests
    // ========================================================================

    @Test
    fun testDataFrameSingleRow() {
        // 测试单行数据框
        // Test single row DataFrame
        val df = DataFrame<Int>(1, 2, listOf("A", "B"))

        df.set(0, 0, 1)
        df.set(0, 1, 2)

        assertEquals(1, df.getNRows())
        assertEquals(2, df.getRow(0).size)
    }

    @Test
    fun testDataFrameSingleColumn() {
        // 测试单列数据框
        // Test single column DataFrame
        val df = DataFrame<Int>(3, 1, listOf("A"))

        df.set(0, 0, 1)
        df.set(1, 0, 2)
        df.set(2, 0, 3)

        assertEquals(3, df.getNRows())
        assertEquals(1, df.getNCols())
        assertEquals(listOf(1, 2, 3), df.getColumn(0))
    }

    @Test
    fun testDataFrameZeroRows() {
        // 测试零行数据框
        // Test zero row DataFrame
        val df = DataFrame<Int>(0, 2, listOf("A", "B"))

        assertEquals(0, df.getNRows())
        assertEquals(2, df.getNCols())
        assertTrue(df.isEmpty())
    }

    @Test
    fun testDataFrameZeroColumns() {
        // 测试零列数据框
        // Test zero column DataFrame
        val df = DataFrame<Int>(3, 0, emptyList())

        assertEquals(3, df.getNRows())
        assertEquals(0, df.getNCols())
    }

    // ========================================================================
    // 类型别名测试
    // Type alias tests
    // ========================================================================

    @Test
    fun testTypeAlias() {
        // 测试类型别名
        // Test type alias
        val df: DataFrame2<Int> = DataFrame(3, 2, listOf("A", "B"))

        assertEquals(3, df.getNRows())
        assertEquals(2, df.getNCols())
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