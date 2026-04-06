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
 * @author OSPF Kotlin Team
 * @since 1.0.0
 * @see MultiArray
 */
package fuookami.ospf.kotlin.multiarray

/**
 * 可空值包装类
 * Nullable value wrapper class
 *
 * 用于在 MultiArray 中存储可能为空的值。
 * Used to store potentially null values in MultiArray.
 *
 * @param T 值类型
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
     * 获取指定位置的值
     * Get value at specified position
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
     */
    fun getByName(row: Int, columnName: String): T? {
        val col = columnIndex[columnName] ?: throw IllegalArgumentException("列名不存在：$columnName / Column name not found")
        return get(row, col)
    }

    /**
     * 设置指定位置的值
     * Set value at specified position
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
     */
    fun setByName(row: Int, columnName: String, value: T?) {
        val col = columnIndex[columnName] ?: throw IllegalArgumentException("列名不存在：$columnName / Column name not found")
        set(row, col, value)
    }

    /**
     * 获取指定行的所有值
     * Get all values in a row
     */
    fun getRow(row: Int): List<T?> {
        require(row in 0 until nrows) { "行索引越界：$row / Row index out of bounds" }
        return data[row].toList()
    }

    /**
     * 获取指定列的所有值
     * Get all values in a column
     */
    fun getColumn(col: Int): List<T?> {
        require(col in 0 until ncols) { "列索引越界：$col / Column index out of bounds" }
        return (0 until nrows).map { data[it][col] }
    }

    /**
     * 通过列名获取列
     * Get column by column name
     */
    fun getColumnByName(columnName: String): List<T?> {
        val col = columnIndex[columnName] ?: throw IllegalArgumentException("列名不存在：$columnName / Column name not found")
        return getColumn(col)
    }

    /**
     * 转换为可空元素的 MultiArray
     * Convert to MultiArray with nullable elements
     *
     * 使用包装类 NullableValue 来存储可能为空的值。
     * Uses wrapper class NullableValue to store potentially null values.
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
     */
    fun select(vararg columnNames: String): DataFrame<T> {
        val colIndices = columnNames.map { name ->
            columnIndex[name] ?: throw IllegalArgumentException("列名不存在：$name / Column name not found")
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
        return newDf
    }

    /**
     * 过滤行
     * Filter rows
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
     */
    fun toMap(): Map<String, List<T?>> {
        return columnNames.associateWith { name -> getColumnByName(name) }
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
         */
        fun <T> empty(vararg columnNames: String): DataFrame<T> {
            return DataFrame(0, columnNames.size, columnNames.toList())
        }

        /**
         * 使用构建器创建 DataFrame
         * Create DataFrame using builder
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
 */
class DataFrameBuilder<T>(
    private val columnNames: List<String>
) {
    private val rows = mutableListOf<List<T?>>()

    fun row(vararg values: T?) {
        require(values.size == columnNames.size) {
            "值的数量 (${values.size}) 必须等于列数 (${columnNames.size}) / Value count must equal column count"
        }
        rows.add(values.toList())
    }

    fun rows(values: List<List<T?>>) {
        for (row in values) {
            require(row.size == columnNames.size) {
                "值的数量 (${row.size}) 必须等于列数 (${columnNames.size}) / Value count must equal column count"
            }
        }
        rows.addAll(values)
    }

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
 */
fun <T> dataFrameFromRows(
    columnNames: List<String>,
    rows: List<List<T?>>
): DataFrame<T> {
    val df = DataFrame<T>(rows.size, columnNames.size, columnNames)
    for ((rowIdx, row) in rows.withIndex()) {
        require(row.size == columnNames.size) {
            "行 ${rowIdx} 的长度 (${row.size}) 与列数 (${columnNames.size}) 不匹配 / Row length mismatch"
        }
        for ((colIdx, value) in row.withIndex()) {
            df.set(rowIdx, colIdx, value)
        }
    }
    return df
}

// 类型别名 / Type aliases
typealias DataFrame2<T> = DataFrame<T>