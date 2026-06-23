/**
 * DataFrame 模块
 * DataFrame Module
 *
 * 本模块提供带命名列的二维数据结构，类似于 pandas DataFrame 或 R data.frame。
 * This module provides 2D data structure with named columns,
 * similar to pandas DataFrame or R data.frame.
 *
 * 主要组件：
 * Main components:
 * - [NullableValue]: 可空值包装类
 *   Nullable value wrapper class
 * - [DataFrame]: 带命名列的二维数据结构
 *   2D data structure with named columns
 *
 * 特性：
 * Features:
 * - 列命名：支持通过列名访问数据
 *   Column naming: Access data by column name
 * - 类型安全：每列有明确的类型信息
 *   Type safety: Each column has explicit type information
 * - 空值支持：内置空值处理机制
 *   Null support: Built-in null handling mechanism
 *
 * 使用场景：
 * Use cases:
 * - 表格数据处理
 *   Tabular data processing
 * - 数据分析和统计
 *   Data analysis and statistics
 * - 结构化数据存储
 *   Structured data storage
 *
 * @see MultiArray
 */
package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 可空值包装类
 * Nullable value wrapper class
 *
 * 用于在 MultiArray 中存储可能为空的值。
 * Used to store potentially null values in MultiArray.
 *
 * @param T 值类型 / Value type
 * @property value 包装的值 / Wrapped value
 */
data class NullableValue<T>(val value: T?) {
    override fun toString(): String = value?.toString() ?: "null"
}

/**
 * DataFrame - 带命名列的 2D 多维数组
 * DataFrame - 2D multi-dimensional array with named columns
 *
 * 类似于 pandas DataFrame 或 R data.frame，支持：
 * Similar to pandas DataFrame or R data.frame, supports:
 *
 * - 2D 多维数组存储 / 2D multi-dimensional array storage
 * - 命名列访问 / Named column access
 * - 可选值（Nullable） / Optional values
 *
 * @param T 元素类型 / Element type
 * @param nrows 行数 / Number of rows
 * @param ncols 列数 / Number of columns
 * @param columnNames 列名列表 / List of column names
 */
class DataFrame<T>(
    val nrows: Int,
    val ncols: Int,
    val columnNames: List<String>
) : Collection<Collection<T?>> {

    // 底层存储：List<List<T?>>
    // Underlying storage: List<List<T?>>
    private val data: List<MutableList<T?>>

    // 列名到索引的映射
    // Column name to index mapping
    private val columnIndex: Map<String, Int>

    init {
        require(columnNames.size == ncols) {
            "列名数量 ($columnNames.size) 必须等于列数 ($ncols) / Column name count must equal column count"
        }
        data = List(nrows) { MutableList(ncols) { null } }
        columnIndex = columnNames.mapIndexed { index, name -> name to index }.toMap()
    }

    /**
     * 获取行数
     * Get number of rows
     */
    fun getNRows(): Int = nrows

    /**
     * 获取列数
     * Get number of columns
     */
    fun getNCols(): Int = ncols

    /**
     * 通过列名获取列索引
     * Get column index by column name
     */
    fun getColumnIndex(name: String): Int? = columnIndex[name]

    /**
     * 构建列名未找到的失败结果。
     * Build failure result for column name not found.
     *
     * @param columnName 未找到的列名 / The column name not found
     * @return 失败结果 / Failure result
     */
    private fun columnNotFound(columnName: String): Failed<Nothing, ErrorCode, Error<ErrorCode>> {
        return Failed(ErrorCode.DataNotFound, "列名不存在：$columnName / Column name not found")
    }

    /**
     * 获取指定位置的值
     * Get value at specified position
     *
     * @param row 行索引 / Row index
     * @param col 列索引 / Column index
     * @return 值 / Value
     */
    fun get(row: Int, col: Int): T? {
        require(row in 0 until nrows && col in 0 until ncols) {
            "索引越界：($row, $col) / Index out of bounds"
        }
        return data[row][col]
    }

    /**
     * 通过行和列名获取值
     * Get value by row and column name
     *
     * @param row 行索引 / Row index
     * @param columnName 列名 / Column name
     * @return 值 / Value
     */
    fun getByNameOrNull(row: Int, columnName: String): T? {
        val col = columnIndex[columnName] ?: return null
        return get(row, col)
    }

    /**
     * 通过行和列名安全获取值
     * Safely get value by row and column name
     *
     * @param row 行索引 / Row index
     * @param columnName 列名 / Column name
     * @return 获取结果 / Get result
     */
    fun getByNameSafe(row: Int, columnName: String): Ret<T?> {
        val col = columnIndex[columnName] ?: return columnNotFound(columnName)
        return Ok(get(row, col))
    }

    /**
     * 通过行和列名获取值
     * Get value by row and column name
     *
     * @param row 行索引 / Row index
     * @param columnName 列名 / Column name
     * @return 获取结果 / Get result
     */
    fun getByName(row: Int, columnName: String): Ret<T?> {
        return getByNameSafe(
            row = row,
            columnName = columnName
        )
    }

    /**
     * 设置指定位置的值
     * Set value at specified position
     *
     * @param row 行索引 / Row index
     * @param col 列索引 / Column index
     * @param value 要设置的值 / Value to set
     */
    fun set(row: Int, col: Int, value: T?) {
        require(row in 0 until nrows && col in 0 until ncols) {
            "索引越界：($row, $col) / Index out of bounds"
        }
        data[row][col] = value
    }

    /**
     * 通过行和列名设置值
     * Set value by row and column name
     *
     * @param row 行索引 / Row index
     * @param columnName 列名 / Column name
     * @param value 要设置的值 / Value to set
     */
    fun setByNameSafe(row: Int, columnName: String, value: T?): Try {
        val col = columnIndex[columnName] ?: return columnNotFound(columnName)
        set(row, col, value)
        return ok
    }

    /**
     * 通过行和列名设置值
     * Set value by row and column name
     *
     * @param row 行索引 / Row index
     * @param columnName 列名 / Column name
     * @param value 要设置的值 / Value to set
     * @return 设置结果 / Set result
     */
    fun setByName(row: Int, columnName: String, value: T?): Try {
        return setByNameSafe(
            row = row,
            columnName = columnName,
            value = value
        )
    }

    /**
     * 获取指定行的所有值
     * Get all values in a row
     *
     * @param row 行索引 / Row index
     * @return 行数据 / Row data
     */
    fun getRow(row: Int): List<T?> {
        require(row in 0 until nrows) { "行索引越界：$row / Row index out of bounds" }
        return data[row].toList()
    }

    /**
     * 获取指定列的所有值
     * Get all values in a column
     *
     * @param col 列索引 / Column index
     * @return 列数据 / Column data
     */
    fun getColumn(col: Int): List<T?> {
        require(col in 0 until ncols) { "列索引越界：$col / Column index out of bounds" }
        return (0 until nrows).map { data[it][col] }
    }

    /**
     * 通过列名获取列
     * Get column by column name
     *
     * @param columnName 列名 / Column name
     * @return 列数据 / Column data
     */
    fun getColumnByNameOrNull(columnName: String): List<T?>? {
        val col = columnIndex[columnName] ?: return null
        return getColumn(col)
    }

    /**
     * 通过列名安全获取列
     * Safely get column by column name
     *
     * @param columnName 列名 / Column name
     * @return 列数据结果 / Column data result
     */
    fun getColumnByNameSafe(columnName: String): Ret<List<T?>> {
        val col = columnIndex[columnName] ?: return columnNotFound(columnName)
        return Ok(getColumn(col))
    }

    /**
     * 通过列名获取列
     * Get column by column name
     *
     * @param columnName 列名 / Column name
     * @return 列数据结果 / Column data result
     */
    fun getColumnByName(columnName: String): Ret<List<T?>> {
        return getColumnByNameSafe(columnName)
    }

    /**
     * 转换为可空元素的 MultiArray
     * Convert to MultiArray with nullable elements
     *
     * 使用包装类 NullableValue 来存储可能为空的值。
     * Uses wrapper class NullableValue to store potentially null values.
     *
     * @return 包含可空值的二维多维数组 / 2D multi-array with nullable values
     */
    fun toNullableMultiArray(): MultiArray2<NullableValue<T>> {
        val array = MutableMultiArray2<NullableValue<T>>(Shape2(nrows, ncols)) { _, _ -> NullableValue(null) }
        for (i in 0 until nrows) {
            for (j in 0 until ncols) {
                array[i, j] = NullableValue(data[i][j])
            }
        }
        return array.toImmutable()
    }

    /**
     * 获取指定范围的视图
     * Get view of specified range
     *
     * @param rows 行范围 / Row range
     * @param cols 列范围 / Column range
     * @return 子 DataFrame / Sub DataFrame
     */
    fun subDataFrame(
        rows: IntRange = 0 until nrows,
        cols: IntRange = 0 until ncols
    ): DataFrame<T> {
        val newDf = DataFrame<T>(
            nrows = rows.count(),
            ncols = cols.count(),
            columnNames = columnNames.slice(cols)
        )
        for ((newRow, row) in rows.withIndex()) {
            for ((newCol, col) in cols.withIndex()) {
                newDf.set(newRow, newCol, get(row, col))
            }
        }
        return newDf
    }

    /**
     * 选择指定列
     * Select specified columns
     *
     * @param columnNames 要选择的列名 / Column names to select
     * @return 包含指定列的 DataFrame / DataFrame with selected columns
     */
    fun selectSafe(vararg columnNames: String): Ret<DataFrame<T>> {
        val colIndices = ArrayList<Int>()
        for (name in columnNames) {
            colIndices.add(columnIndex[name] ?: return columnNotFound(name))
        }
        val newDf = DataFrame<T>(
            nrows = nrows,
            ncols = columnNames.size,
            columnNames = columnNames.toList()
        )
        for (row in 0 until nrows) {
            for ((newCol, col) in colIndices.withIndex()) {
                newDf.set(row, newCol, get(row, col))
            }
        }
        return Ok(newDf)
    }

    /**
     * 选择指定列
     * Select specified columns
     *
     * @param columnNames 要选择的列名 / Column names to select
     * @return 包含指定列的 DataFrame 结果 / DataFrame result with selected columns
     */
    fun select(vararg columnNames: String): Ret<DataFrame<T>> {
        return selectSafe(*columnNames)
    }

    /**
     * 过滤行
     * Filter rows
     *
     * @param predicate 行过滤谓词 / Row filter predicate
     * @return 过滤后的 DataFrame / Filtered DataFrame
     */
    fun filter(predicate: (List<T?>) -> Boolean): DataFrame<T> {
        val selectedRows = (0 until nrows).filter { row ->
            predicate(getRow(row))
        }
        val newDf = DataFrame<T>(
            nrows = selectedRows.size,
            ncols = ncols,
            columnNames = columnNames
        )
        for ((newRow, row) in selectedRows.withIndex()) {
            for (col in 0 until ncols) {
                newDf.set(newRow, col, get(row, col))
            }
        }
        return newDf
    }

    /**
     * 复制并添加行
     * Copy and add a row
     *
     * @param values 新行的值 / Values for the new row
     * @return 添加行后的新 DataFrame / New DataFrame with added row
     */
    fun copyWithAddedRow(values: List<T?>): DataFrame<T> {
        require(values.size == ncols) {
            "值的数量 ($values.size) 必须等于列数 ($ncols) / Value count must equal column count"
        }
        val newDf = DataFrame<T>(
            nrows = nrows + 1,
            ncols = ncols,
            columnNames = columnNames
        )
        // 拷贝原数据 / Copy original data
        for (i in 0 until nrows) {
            for (j in 0 until ncols) {
                newDf.set(i, j, get(i, j))
            }
        }
        // 添加新行 / Add new row
        for (j in 0 until ncols) {
            newDf.set(nrows, j, values[j])
        }
        return newDf
    }

    /**
     * 转换为 Map 表示
     * Convert to Map representation
     *
     * @return 列名到列数据的映射 / Mapping from column name to column data
     */
    fun toMapOrNull(): Map<String, List<T?>>? {
        val ret = LinkedHashMap<String, List<T?>>()
        for (name in columnNames) {
            ret[name] = getColumnByNameOrNull(name) ?: return null
        }
        return ret
    }

    /**
     * 安全转换为 Map 表示
     * Safely convert to Map representation
     *
     * @return 列名到列数据映射的结果 / Result of mapping from column name to column data
     */
    fun toMapSafe(): Ret<Map<String, List<T?>>> {
        val ret = LinkedHashMap<String, List<T?>>()
        for (name in columnNames) {
            ret[name] = when (val result = getColumnByName(name)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return Ok(ret)
    }

    /**
     * 转换为 Map 表示
     * Convert to Map representation
     *
     * @return 列名到列数据映射的结果 / Result of mapping from column name to column data
     */
    fun toMap(): Ret<Map<String, List<T?>>> {
        return toMapSafe()
    }

    /**
     * 迭代器 - 按行迭代
     * Iterator - iterate by row
     */
    override fun iterator(): Iterator<Collection<T?>> {
        return data.map { it.toList() }.iterator()
    }

    override val size: Int get() = nrows

    /**
     * 检查是否包含元素
     * Check if contains all elements
     */
    override fun containsAll(elements: Collection<Collection<T?>>): Boolean {
        return elements.all { row -> data.contains(row) }
    }

    override fun contains(element: Collection<T?>): Boolean {
        return data.any { it == element }
    }

    override fun isEmpty(): Boolean = nrows == 0

    /**
     * 字符串表示
     * String representation
     */
    override fun toString(): String {
        val sb = StringBuilder()
        // 表头 / Header
        sb.append("| ")
        sb.append(columnNames.joinToString(" | ") { it.padEnd(12) })
        sb.append(" |\n")
        // 分隔线 / Separator
        sb.append("|")
        sb.append(columnNames.joinToString("|") { "------------" })
        sb.append("|\n")
        // 数据行 / Data rows
        for (i in 0 until nrows.coerceAtMost(10)) {
            sb.append("| ")
            sb.append(data[i].joinToString(" | ") {
                it?.toString()?.padEnd(12) ?: "null".padEnd(12)
            })
            sb.append(" |\n")
        }
        if (nrows > 10) {
            sb.append("... 还有 ${nrows - 10} 行\n")
        }
        return sb.toString()
    }

    companion object {
        /**
         * 从 Map 创建 DataFrame
         * Create DataFrame from Map
         *
         * @param data 列名到列数据的映射 / Mapping from column name to column data
         * @return DataFrame 实例 / DataFrame instance
         */
        fun <T> fromMap(data: Map<String, List<T?>>): DataFrame<T> {
            val columnNames = data.keys.toList()
            val nrows = data.values.firstOrNull()?.size ?: 0
            val ncols = columnNames.size

            // 验证所有列长度相同 / Verify all columns have same length
            for ((name, values) in data) {
                require(values.size == nrows) {
                    "列 '$name' 的长度 (${values.size}) 与其他列不匹配 / Column length mismatch"
                }
            }

            val df = DataFrame<T>(nrows, ncols, columnNames)
            for ((colIdx, columnName) in columnNames.withIndex()) {
                for ((rowIdx, value) in data[columnName]!!.withIndex()) {
                    df.set(rowIdx, colIdx, value)
                }
            }
            return df
        }

        /**
         * 创建空 DataFrame
         * Create empty DataFrame
         *
         * @param columnNames 列名 / Column names
         * @return 空 DataFrame / Empty DataFrame
         */
        fun <T> empty(vararg columnNames: String): DataFrame<T> {
            return DataFrame(0, columnNames.size, columnNames.toList())
        }

        /**
         * 使用构建器创建 DataFrame
         * Create DataFrame using builder
         *
         * @param columnNames 列名 / Column names
         * @param block 构建器块 / Builder block
         * @return DataFrame 实例 / DataFrame instance
         */
        inline fun <T> build(
            vararg columnNames: String,
            block: DataFrameBuilder<T>.() -> Unit
        ): DataFrame<T> {
            val builder = DataFrameBuilder<T>(columnNames.toList())
            builder.block()
            return builder.build()
        }
    }
}

/**
 * DataFrame 构建器
 * DataFrame builder
 *
 * @param T 元素类型 / Element type
 * @param columnNames 列名列表 / List of column names
 */
class DataFrameBuilder<T>(
    private val columnNames: List<String>
) {
    private val rows = mutableListOf<List<T?>>()

    /**
     * 添加一行数据
     * Add a row of data
     *
     * @param values 行数据 / Row data
     */
    fun row(vararg values: T?) {
        require(values.size == columnNames.size) {
            "值的数量 (${values.size}) 必须等于列数 (${columnNames.size}) / Value count must equal column count"
        }
        rows.add(values.toList())
    }

    /**
     * 添加多行数据
     * Add multiple rows of data
     *
     * @param values 多行数据 / Multiple rows of data
     */
    fun rows(values: List<List<T?>>) {
        for (row in values) {
            require(row.size == columnNames.size) {
                "值的数量 (${row.size}) 必须等于列数 (${columnNames.size}) / Value count must equal column count"
            }
        }
        rows.addAll(values)
    }

    /**
     * 构建 DataFrame
     * Build DataFrame
     */
    fun build(): DataFrame<T> {
        val df = DataFrame<T>(rows.size, columnNames.size, columnNames)
        for ((rowIdx, row) in rows.withIndex()) {
            for ((colIdx, value) in row.withIndex()) {
                df.set(rowIdx, colIdx, value)
            }
        }
        return df
    }
}

/**
 * 便捷函数：创建 DataFrame
 * Convenience function: Create DataFrame
 *
 * @param columns 列名到列数据的键值对 / Pairs of column name to column data
 * @return DataFrame 实例 / DataFrame instance
 */
fun <T> dataFrameOf(
    vararg columns: Pair<String, List<T?>>
): DataFrame<T> {
    val map = columns.associate { it }
    return DataFrame.fromMap(map)
}

/**
 * 便捷函数：从行创建 DataFrame
 * Convenience function: Create DataFrame from rows
 *
 * @param columnNames 列名列表 / List of column names
 * @param rows 行数据列表 / List of row data
 * @return DataFrame 实例 / DataFrame instance
 */
fun <T> dataFrameFromRows(
    columnNames: List<String>,
    rows: List<List<T?>>
): DataFrame<T> {
    val df = DataFrame<T>(rows.size, columnNames.size, columnNames)
    for ((rowIdx, row) in rows.withIndex()) {
        require(row.size == columnNames.size) {
            "行 $rowIdx 的长度 (${row.size}) 与列数 (${columnNames.size}) 不匹配 / Row length mismatch"
        }
        for ((colIdx, value) in row.withIndex()) {
            df.set(rowIdx, colIdx, value)
        }
    }
    return df
}

/** DataFrame 类型别名（与 DataFrame 等价）/ DataFrame type alias (equivalent to DataFrame) */
typealias DataFrame2<T> = DataFrame<T>
