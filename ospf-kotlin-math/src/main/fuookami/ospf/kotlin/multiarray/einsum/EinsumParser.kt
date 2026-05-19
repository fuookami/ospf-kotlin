/**
 * 通用爱因斯坦求和函数
 * Generic Einstein summation function
 *
 * 提供基于字符串表示法的爱因斯坦求和。
 * Provides Einstein summation based on string notation.
 */
package fuookami.ospf.kotlin.multiarray.einsum

import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

// ============================================================================
// einsum 字符串解析和执行
// ============================================================================

/**
 * 通用爱因斯坦求和（字符串表示法）
 * Generic Einstein summation (string notation)
 *
 * 支持常见的爱因斯坦表示法模式，
 * Supports common Einstein notation patterns:
 *
 * - `"ij,jk->ik"`: 矩阵乘法 / Matrix multiplication
 * - `"i,i->"`: 点积 / Dot product
 * - `"i,j->ij"`: 外积 / Outer product
 * - `"ii->"`: 迌/ Trace
 * - `"ij->ji"`: 转置 / Transpose
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.einsum
 * import fuookami.ospf.kotlin.math.algebra.number.Flt64
 *
 * val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
 * val b = MultiArray.newWith(Shape2(3, 4), Flt64(2.0))
 *
 * // 矩阵乘法
 * // Matrix multiplication
 * val c = einsum(a, b, "ij,jk->ik", Flt64.zero)
 * ```
 *
 * @param a 第一个张里
 * @param b 第二个张里
 * @param notation 爱因斯坦表示法字符串
 * @param zero 零倌
 * @return 结果张量
 * @throws EinsumError 如果表示法不支持或参数不匹配
 */
fun <T : Ring<T>> einsum(
    a: AbstractMultiArray<T, *>,
    b: AbstractMultiArray<T, *>,
    notation: String,
    zero: T
): Any {
    // 解析表示泌
    // Parse notation
    val parts = notation.split("->")
    if (parts.size != 2) {
        throw EinsumError.UnsupportedOperation(
            "Invalid einsum notation: $notation. Expected format: 'inputs->output'"
        )
    }

    val inputs = parts[0].split(",")
    if (inputs.size != 2) {
        throw EinsumError.UnsupportedOperation(
            "Expected 2 inputs in notation, got ${inputs.size}"
        )
    }

    val inputA = inputs[0].trim()
    val inputB = inputs[1].trim()
    val output = parts[1].trim()

    // 根据模式分派到具体操佌
    // Dispatch to specific operations based on pattern
    return when {
        // 矩阵乘法: ij,jk->ik
        inputA.length == 2 && inputB.length == 2 && output.length == 2 -> {
            matmul(a, b, zero)
        }

        // 点积: i,i->
        inputA.length == 1 && inputB.length == 1 && output.isEmpty() &&
        inputA == inputB -> {
            dot(a, b, zero)
        }

        // 外积: i,j->ij
        inputA.length == 1 && inputB.length == 1 && output.length == 2 -> {
            outer(a, b, zero)
        }

        else -> {
            throw EinsumError.UnsupportedOperation(
                "Unsupported einsum pattern: $notation. " +
                "Supported: ij,jk->ik, i,i->, i,j->ij, ii->, ij->ji"
            )
        }
    }
}

/**
 * 单操作数爱因斯坦求和
 * Single operand Einstein summation
 *
 * 支持的模式：
 * Supported patterns:
 * - `"ii->"`: 迌/ Trace
 * - `"ij->ji"`: 转置 / Transpose
 *
 * @param a 输入张量
 * @param notation 爱因斯坦表示法字符串
 * @param zero 零值（迹操作需要）
 * @return 结果
 */
fun <T : Ring<T>> einsum(
    a: AbstractMultiArray<T, *>,
    notation: String,
    zero: T
): Any {
    val parts = notation.split("->")
    if (parts.size != 2) {
        throw EinsumError.UnsupportedOperation(
            "Invalid einsum notation: $notation"
        )
    }

    val input = parts[0].trim()
    val output = parts[1].trim()

    return when {
        // 迌 ii->
        input.length == 2 && output.isEmpty() && input[0] == input[1] -> {
            trace(a, zero)
        }

        // 转置: ij->ji
        input.length == 2 && output.length == 2 &&
        input[0] == output[1] && input[1] == output[0] -> {
            transpose(a)
        }

        else -> {
            throw EinsumError.UnsupportedOperation(
                "Unsupported einsum pattern: $notation. " +
                "Supported: ii->, ij->ji"
            )
        }
    }
}

// ============================================================================
// DSL 风格 API / DSL style API
// ============================================================================

/**
 * Einstein DSL 上下斌
 * Einstein DSL context
 *
 * 提供更流畅的 API 进行爱因斯坦求和操作。
 * Provides a more fluent API for Einstein summation operations.
 *
 * 示例 / Example:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.einstein
 * import fuookami.ospf.kotlin.math.algebra.number.Flt64
 *
 * val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
 * val b = MultiArray.newWith(Shape2(3, 4), Flt64(2.0))
 *
 * val c = einstein(a, b, Flt64.zero).matmul()
 * val d = einstein(a, b, Flt64.zero).outer()
 * ```
 */
class Einstein<T : Ring<T>>(
    private val a: AbstractMultiArray<T, *>,
    private val b: AbstractMultiArray<T, *>? = null,
    private val zero: T
) {
    /**
     * 矩阵乘法
     * Matrix multiplication
     */
    fun matmul(): MultiArray<T, DynShape> {
        return b?.let { matmul(a, it, zero) }
            ?: throw EinsumError.UnsupportedOperation("matmul requires two operands")
    }

    /**
     * 点积
     * Dot product
     */
    fun dot(): T {
        return b?.let { dot(a, it, zero) }
            ?: throw EinsumError.UnsupportedOperation("dot requires two operands")
    }

    /**
     * 外积
     * Outer product
     */
    fun outer(): MultiArray<T, DynShape> {
        return b?.let { outer(a, it, zero) }
            ?: throw EinsumError.UnsupportedOperation("outer requires two operands")
    }

    /**
     * 缩并
     * Contraction
     */
    fun contract(axisA: Int, axisB: Int): MultiArray<T, DynShape> {
        return b?.let { contract(a, axisA, it, axisB, zero) }
            ?: throw EinsumError.UnsupportedOperation("contract requires two operands")
    }

    /**
     * 迹（单操作数，
     * Trace (single operand)
     */
    fun trace(): T {
        if (b != null) {
            throw EinsumError.UnsupportedOperation("trace is a single operand operation")
        }
        return trace(a, zero)
    }

    /**
     * 转置（单操作数）
     * Transpose (single operand)
     */
    fun transpose(): MultiArray<T, DynShape> {
        if (b != null) {
            throw EinsumError.UnsupportedOperation("transpose is a single operand operation")
        }
        return transpose(a)
    }
}

/**
 * 创建 Einstein DSL 上下文（双操作数，
 * Create Einstein DSL context (two operands)
 */
fun <T : Ring<T>> einstein(
    a: AbstractMultiArray<T, *>,
    b: AbstractMultiArray<T, *>,
    zero: T
): Einstein<T> = Einstein(a, b, zero)

/**
 * 创建 Einstein DSL 上下文（单操作数，
 * Create Einstein DSL context (single operand)
 */
fun <T : Ring<T>> einstein(
    a: AbstractMultiArray<T, *>,
    zero: T
): Einstein<T> = Einstein(a, null, zero)