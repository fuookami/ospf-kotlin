/**
 * 常见爱因斯坦运算实现
 * Common Einstein operations implementation
 *
 * 提供矩阵乘法、点积、迹等常见运算的便捷函数。
 * Provides convenience functions for common operations like matrix multiplication, dot product, and trace.
 */
package fuookami.ospf.kotlin.multiarray.einsum

import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

// ============================================================================
// 矩阵乘法 / Matrix Multiplication
// ============================================================================

/**
 * 矩阵乘法
 * Matrix multiplication
 *
 * 计算 C = A @ B，其中 A 的形状为 [m, k]，B 的形状为 [k, n]，结果 C 的形状为 [m, n]。
 * Computes C = A @ B, where A has shape [m, k], B has shape [k, n], and result C has shape [m, n].
 *
 * 爱因斯坦表示法：A_ij * B_jk -> C_ik
 * Einstein notation: A_ij * B_jk -> C_ik
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.matmul
 *
 * val a = MultiArray.newBy(Shape2(2, 3)) { i, _ -> i.toDouble() }
 * val b = MultiArray.newBy(Shape2(3, 2)) { i, _ -> (i + 1).toDouble() }
 *
 * val c = matmul(a, b)
 * // 结果形状: 2x2
 * // Result shape: 2x2
 * ```
 *
 * @param a 左矩阵，形状 [m, k]
 * @param b 右矩阵，形状 [k, n]
 * @param zero 零值（用于初始化）
 * @return 结果矩阵，形状 [m, n]
 * @throws EinsumError 如果维度不匹配或形状不兼容
 */
fun <T : Ring<T>> matmul(
    a: AbstractMultiArray<T, *>,
    b: AbstractMultiArray<T, *>,
    zero: T
): MultiArray<T, DynShape> {
    // 验证维度
    // Validate dimensions
    val aDim = a.shape.dimension
    val bDim = b.shape.dimension

    if (aDim != 2 || bDim != 2) {
        throw EinsumError.DimensionMismatch(
            expected = 2,
            actual = maxOf(aDim, bDim),
            description = "Matrix multiplication requires 2D arrays"
        )
    }

    // 获取形状
    // Get shapes
    val aRows = a.shape[0]
    val aCols = a.shape[1]
    val bRows = b.shape[0]
    val bCols = b.shape[1]

    if (aCols != bRows) {
        throw EinsumError.IncompatibleShapes(
            shape1 = listOf(aRows, aCols),
            shape2 = listOf(bRows, bCols),
            description = "Matrix dimensions don't align: ${aRows}x${aCols} and ${bRows}x${bCols}"
        )
    }

    // 创建结果矩阵
    // Create result matrix
    val resultShape = DynShape(intArrayOf(aRows, bCols))
    val result = MutableMultiArray.newWith(resultShape, zero)

    // 执行矩阵乘法
    // Perform matrix multiplication
    for (i in 0 until aRows) {
        for (j in 0 until bCols) {
            var sum = zero
            for (k in 0 until aCols) {
                val aVal = a[intArrayOf(i, k)]
                val bVal = b[intArrayOf(k, j)]
                sum = sum + aVal * bVal
            }
            result[intArrayOf(i, j)] = sum
        }
    }

    return result.toImmutable()
}

/**
 * 矩阵乘法（使用默认零值）
 * Matrix multiplication (using default zero)
 *
 * 要求元素类型有默认零值。
 * Requires element type to have default zero value.
 *
 * @param a 左矩阵
 * @param b 右矩阵
 * @return 结果矩阵
 */
inline fun <reified T : Ring<T>> matmul(
    a: AbstractMultiArray<T, *>,
    b: AbstractMultiArray<T, *>
): MultiArray<T, DynShape> {
    val zero = when (T::class) {
        Double::class -> 0.0 as T
        Float::class -> 0.0f as T
        Int::class -> 0 as T
        Long::class -> 0L as T
        else -> throw EinsumError.UnsupportedOperation(
            "Cannot infer zero value for type ${T::class}. Please provide zero explicitly."
        )
    }
    return matmul(a, b, zero)
}

// ============================================================================
// 点积 / Dot Product
// ============================================================================

/**
 * 向量点积
 * Vector dot product
 *
 * 计算两个向量的点积（内积）。
 * Computes the dot product (inner product) of two vectors.
 *
 * 爱因斯坦表示法：a_i * b_i -> （标量）
 * Einstein notation: a_i * b_i -> (scalar)
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.dot
 *
 * val a = MultiArray.newBy(Shape1(3)) { i, _ -> (i + 1).toDouble() }
 * val b = MultiArray.newBy(Shape1(3)) { i, _ -> (i + 1).toDouble() }
 *
 * val result = dot(a, b)
 * // 结果: 1*1 + 2*2 + 3*3 = 14.0
 * // Result: 1*1 + 2*2 + 3*3 = 14.0
 * ```
 *
 * @param a 第一个向量
 * @param b 第二个向量
 * @param zero 零值
 * @return 点积结果（标量）
 * @throws EinsumError 如果不是向量或长度不匹配
 */
fun <T : Ring<T>> dot(
    a: AbstractMultiArray<T, *>,
    b: AbstractMultiArray<T, *>,
    zero: T
): T {
    // 验证维度
    // Validate dimensions
    val aDim = a.shape.dimension
    val bDim = b.shape.dimension

    if (aDim != 1 || bDim != 1) {
        throw EinsumError.DimensionMismatch(
            expected = 1,
            actual = maxOf(aDim, bDim),
            description = "Dot product requires 1D vectors"
        )
    }

    if (a.size != b.size) {
        throw EinsumError.IncompatibleShapes(
            shape1 = listOf(a.size),
            shape2 = listOf(b.size),
            description = "Vector lengths don't match"
        )
    }

    var result = zero
    for (i in 0 until a.size) {
        result = result + a[i] * b[i]
    }

    return result
}

/**
 * 点积（使用默认零值）
 * Dot product (using default zero)
 */
inline fun <reified T : Ring<T>> dot(
    a: AbstractMultiArray<T, *>,
    b: AbstractMultiArray<T, *>
): T {
    val zero = when (T::class) {
        Double::class -> 0.0 as T
        Float::class -> 0.0f as T
        Int::class -> 0 as T
        Long::class -> 0L as T
        else -> throw EinsumError.UnsupportedOperation(
            "Cannot infer zero value for type ${T::class}. Please provide zero explicitly."
        )
    }
    return dot(a, b, zero)
}

// ============================================================================
// 迹 / Trace
// ============================================================================

/**
 * 矩阵迹
 * Matrix trace
 *
 * 计算方阵的迹（对角元素之和）。
 * Computes the trace of a square matrix (sum of diagonal elements).
 *
 * 爱因斯坦表示法：A_ii -> （标量）
 * Einstein notation: A_ii -> (scalar)
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.trace
 *
 * val a = MultiArray.newBy(Shape2(3, 3)) { i, vec ->
 *     if (vec[0] == vec[1]) 1.0 else 0.0  // 单位矩阵
 * }
 *
 * val result = trace(a)
 * // 结果: 3.0
 * // Result: 3.0
 * ```
 *
 * @param a 方阵
 * @param zero 零值
 * @return 迹（标量）
 * @throws EinsumError 如果不是方阵
 */
fun <T : Ring<T>> trace(
    a: AbstractMultiArray<T, *>,
    zero: T
): T {
    val shape = IntArray(a.shape.dimension) { a.shape[it] }

    if (shape.size != 2) {
        throw EinsumError.UnsupportedOperation(
            "Trace only defined for 2D matrices"
        )
    }

    if (shape[0] != shape[1]) {
        throw EinsumError.UnsupportedOperation(
            "Trace only defined for square matrices"
        )
    }

    val n = shape[0]
    var result = zero

    for (i in 0 until n) {
        result = result + a[intArrayOf(i, i)]
    }

    return result
}

/**
 * 迹（使用默认零值）
 * Trace (using default zero)
 */
inline fun <reified T : Ring<T>> trace(a: AbstractMultiArray<T, *>): T {
    val zero = when (T::class) {
        Double::class -> 0.0 as T
        Float::class -> 0.0f as T
        Int::class -> 0 as T
        Long::class -> 0L as T
        else -> throw EinsumError.UnsupportedOperation(
            "Cannot infer zero value for type ${T::class}. Please provide zero explicitly."
        )
    }
    return trace(a, zero)
}

// ============================================================================
// 外积 / Outer Product
// ============================================================================

/**
 * 向量外积
 * Vector outer product
 *
 * 计算两个向量的外积，生成矩阵。
 * Computes the outer product of two vectors, producing a matrix.
 *
 * 爱因斯坦表示法：a_i * b_j -> C_ij
 * Einstein notation: a_i * b_j -> C_ij
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.outer
 *
 * val a = MultiArray.newBy(Shape1(2)) { i, _ -> (i + 1).toDouble() }  // [1, 2]
 * val b = MultiArray.newBy(Shape1(3)) { i, _ -> (i + 1).toDouble() }  // [1, 2, 3]
 *
 * val result = outer(a, b)
 * // 结果形状: 2x3
 * // Result shape: 2x3
 * // 内容: [[1, 2, 3], [2, 4, 6]]
 * ```
 *
 * @param a 第一个向量
 * @param b 第二个向量
 * @param zero 零值
 * @return 外积矩阵
 * @throws EinsumError 如果不是向量
 */
fun <T : Ring<T>> outer(
    a: AbstractMultiArray<T, *>,
    b: AbstractMultiArray<T, *>,
    zero: T
): MultiArray<T, DynShape> {
    // 验证维度
    // Validate dimensions
    val aDim = a.shape.dimension
    val bDim = b.shape.dimension

    if (aDim != 1 || bDim != 1) {
        throw EinsumError.DimensionMismatch(
            expected = 1,
            actual = maxOf(aDim, bDim),
            description = "Outer product requires 1D vectors"
        )
    }

    val aLen = a.size
    val bLen = b.size

    // 创建结果矩阵
    // Create result matrix
    val resultShape = DynShape(intArrayOf(aLen, bLen))
    val result = MutableMultiArray.newWith(resultShape, zero)

    // 计算外积
    // Calculate outer product
    for (i in 0 until aLen) {
        for (j in 0 until bLen) {
            result[intArrayOf(i, j)] = a[i] * b[j]
        }
    }

    return result.toImmutable()
}

/**
 * 外积（使用默认零值）
 * Outer product (using default zero)
 */
inline fun <reified T : Ring<T>> outer(
    a: AbstractMultiArray<T, *>,
    b: AbstractMultiArray<T, *>
): MultiArray<T, DynShape> {
    val zero = when (T::class) {
        Double::class -> 0.0 as T
        Float::class -> 0.0f as T
        Int::class -> 0 as T
        Long::class -> 0L as T
        else -> throw EinsumError.UnsupportedOperation(
            "Cannot infer zero value for type ${T::class}. Please provide zero explicitly."
        )
    }
    return outer(a, b, zero)
}

// ============================================================================
// 转置 / Transpose
// ============================================================================

/**
 * 矩阵转置
 * Matrix transpose
 *
 * 返回矩阵的转置。
 * Returns the transpose of a matrix.
 *
 * 爱因斯坦表示法：A_ij -> B_ji
 * Einstein notation: A_ij -> B_ji
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.transpose
 *
 * val a = MultiArray.newBy(Shape2(2, 3)) { i, vec ->
 *     vec[0] * 10 + vec[1]
 * }
 *
 * val result = transpose(a)
 * // 结果形状: 3x2
 * // Result shape: 3x2
 * ```
 *
 * @param a 输入矩阵
 * @return 转置矩阵
 * @throws EinsumError 如果不是二维矩阵
 */
fun <T : Any> transpose(a: AbstractMultiArray<T, *>): MultiArray<T, DynShape> {
    val shape = IntArray(a.shape.dimension) { a.shape[it] }

    if (shape.size != 2) {
        throw EinsumError.UnsupportedOperation(
            "Transpose only defined for 2D matrices"
        )
    }

    val transposedShape = DynShape(intArrayOf(shape[1], shape[0]))

    // 空矩阵安全处理：如果任一维度为0，直接返回空数组
    // Empty matrix safety: if any dimension is 0, return empty array directly
    if (shape[0] == 0 || shape[1] == 0) {
        // 空矩阵不需要初始化值，lambda不会被调用
        // Empty matrices don't need initialization value, lambda won't be called
        return MutableMultiArray.newBy<T, DynShape>(transposedShape) { _, _ ->
            throw IllegalStateException("Empty matrix should not call initialization lambda")
        }.toImmutable()
    }

    val result = MutableMultiArray.newBy<T, DynShape>(transposedShape) { _, _ ->
        a[0]  // 用第一个元素初始化，后续会被覆盖
    }

    for (i in 0 until shape[0]) {
        for (j in 0 until shape[1]) {
            result[intArrayOf(j, i)] = a[intArrayOf(i, j)]
        }
    }

    return result.toImmutable()
}

// ============================================================================
// 张量缩并 / Tensor Contraction
// ============================================================================

/**
 * 沿指定轴的张量缩并
 * Tensor contraction along specified axes
 *
 * 爱因斯坦表示法中，缩并是对公共索引进行求和的操作。
 * In Einstein notation, contraction is summing over common indices.
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.contract
 *
 * val a = MultiArray.newWith(Shape2(2, 3), 1.0)
 * val b = MultiArray.newWith(Shape2(3, 4), 2.0)
 *
 * val result = contract(a, 1, b, 0)  // 等价于 matmul
 * // Equivalent to matmul
 * ```
 *
 * @param a 第一个张量
 * @param axisA 第一个张量的缩并轴
 * @param b 第二个张量
 * @param axisB 第二个张量的缩并轴
 * @param zero 零值
 * @return 缩并结果
 * @throws EinsumError 如果轴越界或维度不匹配
 */
fun <T : Ring<T>> contract(
    a: AbstractMultiArray<T, *>,
    axisA: Int,
    b: AbstractMultiArray<T, *>,
    axisB: Int,
    zero: T
): MultiArray<T, DynShape> {
    val aShape = IntArray(a.shape.dimension) { a.shape[it] }
    val bShape = IntArray(b.shape.dimension) { b.shape[it] }

    if (axisA < 0 || axisB < 0 || axisA >= aShape.size || axisB >= bShape.size) {
        throw EinsumError.IndexOutOfBounds(
            index = if (axisA < 0 || axisA >= aShape.size) axisA else axisB,
            maxIndex = if (axisA < 0 || axisA >= aShape.size) aShape.size - 1 else bShape.size - 1
        )
    }

    if (aShape[axisA] != bShape[axisB]) {
        throw EinsumError.IncompatibleShapes(
            shape1 = aShape.toList(),
            shape2 = bShape.toList(),
            description = "Contraction axis dimensions don't match: " +
                         "a[${axisA}]=${aShape[axisA]} vs b[${axisB}]=${bShape[axisB]}"
        )
    }

    // 计算输出形状
    // Calculate output shape
    val outShape = mutableListOf<Int>()
    for ((i, dim) in aShape.withIndex()) {
        if (i != axisA) outShape.add(dim)
    }
    for ((i, dim) in bShape.withIndex()) {
        if (i != axisB) outShape.add(dim)
    }

    val outDynShape = DynShape(outShape.toIntArray())
    val result = MutableMultiArray.newWith(outDynShape, zero)

    // 优化：预计算 strides 并使用输出驱动迭代
    // Optimization: Pre-compute strides and use output-driven iteration
    val contractionSize = aShape[axisA]

    // 预计算所有数组的 strides
    // Pre-compute strides for all arrays
    val aStrides = computeStrides(aShape)
    val bStrides = computeStrides(bShape)
    val outShapeArray = outShape.toIntArray()
    val outStrides = computeStrides(outShapeArray)

    // 提取非缩并轴的维度信息，用于构建坐标映射
    // Extract non-contraction axis dimensions for coordinate mapping
    val aNonContractDims = aShape.indices.filter { it != axisA }.toList()
    val bNonContractDims = bShape.indices.filter { it != axisB }.toList()

    // 输出驱动迭代：遍历输出位置，然后遍历缩并轴
    // Output-driven iteration: iterate over output positions, then contraction axis
    val outSize = result.size
    val aNonContractSize = a.size / contractionSize
    val bNonContractSize = b.size / contractionSize

    // 预计算非缩并轴的 stride 映射
    // Pre-compute stride mappings for non-contraction axes
    val aOutStrides = IntArray(aNonContractDims.size) { outStrides[it] }
    val bOutStrides = IntArray(bNonContractDims.size) { outStrides[aNonContractDims.size + it] }

    // 遍历输出位置
    // Iterate over output positions
    for (outLinear in 0 until outSize) {
        var sum = zero

        // 遍历缩并轴
        // Iterate over contraction axis
        for (k in 0 until contractionSize) {
            // 将输出索引分解为坐标
            // Decompose output index to coordinates
            var remaining = outLinear
            var aLinear = k * aStrides[axisA]
            var bLinear = k * bStrides[axisB]

            // 计算输入数组的非缩并轴坐标贡献
            // Calculate non-contraction axis contributions for input arrays
            for (i in aNonContractDims.indices) {
                val outCoord = remaining / aOutStrides[i]
                remaining %= aOutStrides[i]
                val aAxisIdx = aNonContractDims[i]
                aLinear += outCoord * aStrides[aAxisIdx]
            }

            for (i in bNonContractDims.indices) {
                val outCoord = remaining / bOutStrides[i]
                remaining %= bOutStrides[i]
                val bAxisIdx = bNonContractDims[i]
                bLinear += outCoord * bStrides[bAxisIdx]
            }

            sum = sum + a[aLinear] * b[bLinear]
        }

        result[outLinear] = sum
    }

    return result.toImmutable()
}

/**
 * 缩并（使用默认零值）
 * Contract (using default zero)
 */
inline fun <reified T : Ring<T>> contract(
    a: AbstractMultiArray<T, *>,
    axisA: Int,
    b: AbstractMultiArray<T, *>,
    axisB: Int
): MultiArray<T, DynShape> {
    val zero = when (T::class) {
        Double::class -> 0.0 as T
        Float::class -> 0.0f as T
        Int::class -> 0 as T
        Long::class -> 0L as T
        else -> throw EinsumError.UnsupportedOperation(
            "Cannot infer zero value for type ${T::class}. Please provide zero explicitly."
        )
    }
    return contract(a, axisA, b, axisB, zero)
}

// ============================================================================
// 辅助函数 / Helper Functions
// ============================================================================

/**
 * 线性索引转坐标
 * Convert linear index to coordinates
 *
 * @param linear 线性索引
 * @param shape 形状列表
 * @return 坐标列表
 */
internal fun linearToCoords(linear: Int, shape: List<Int>): List<Int> {
    val ndim = shape.size
    if (ndim == 0) return emptyList()

    val coords = mutableListOf<Int>()
    var remaining = linear

    for (i in 0 until ndim) {
        val stride = if (i < ndim - 1) {
            shape.subList(i + 1, ndim).fold(1) { acc, dim -> acc * dim }
        } else {
            1
        }
        coords.add(remaining / stride)
        remaining %= stride
    }

    return coords
}

/**
 * 坐标转线性索引
 * Convert coordinates to linear index
 *
 * @param coords 坐标列表
 * @param shape 形状列表
 * @return 线性索引
 */
internal fun coordsToLinear(coords: List<Int>, shape: List<Int>): Int {
    if (coords.isEmpty() || shape.isEmpty()) return 0

    var linear = 0
    var stride = 1

    for (i in shape.indices.reversed()) {
        val dimLen = shape[i]
        if (i < coords.size) {
            linear += coords[i] * stride
        }
        stride *= dimLen
    }

    return linear
}

/**
 * 预计算 stride
 * Pre-compute strides for shape
 *
 * strides[i] = shape[i+1] * shape[i+2] * ... * shape[n-1]
 * 用于快速坐标转线性索引：linear = sum(coords[i] * strides[i])
 * Used for fast coordinate-to-linear conversion: linear = sum(coords[i] * strides[i])
 *
 * @param shape 形状数组
 * @return stride 数组
 */
internal fun computeStrides(shape: IntArray): IntArray {
    val strides = IntArray(shape.size)
    var stride = 1
    for (i in shape.indices.reversed()) {
        strides[i] = stride
        stride *= shape[i]
    }
    return strides
}
