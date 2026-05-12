/**
 * 爱因斯坦求和表示法模块
 * Einstein Notation Module
 *
 * 提供编译期类型安全的爱因斯坦求和操作。
 * Provides compile-time type-safe Einstein summation operations.
 *
 * 核心概念 / Core Concepts:
 *
 * 爱因斯坦求和表示法的核心是隐式求和约定：当一个索引在表达式中出现两次时，自动对该索引进行求和。
 * The core of Einstein notation is the implicit summation convention:
 * when an index appears twice in an expression, it is automatically summed over.
 *
 * 使用示例 / Usage Examples:
 *
 * ```kotlin
 * import fuookami.ospf.kotlin.math.multiarray.einsum.*
 *
 * // 创建矩阵用于矩阵乘法
 * // Create matrices for matrix multiplication
 * val a = MultiArray.newWith(Shape2(2, 3), 1.0)
 * val b = MultiArray.newWith(Shape2(3, 4), 2.0)
 *
 * // 方法 1：使用便捷函数
 * // Method 1: Using convenience functions
 * val c1 = matmul(a, b, 0.0)
 *
 * // 方法 2：使用字符串表示法
 * // Method 2: Using string notation
 * val c2 = einsumDouble(a, b, "ij,jk->ik")
 *
 * // 方法 3：使用 DSL
 * // Method 3: Using DSL
 * val c3 = einsteinDouble(a, b).matmul()
 * ```
 */
package fuookami.ospf.kotlin.multiarray.einsum

// ============================================================================
// 模块导出 / Module Exports
// ============================================================================

// 索引标签和索引列表 / Index labels and index lists
// IndexLabel, IndexList, findCommonIndices, removeIndices, mergeIndices

// 错误类型 / Error types
// EinsumError

// 张量表达式 / Tensor expressions
// TensorExpr, tensorExpr

// 操作函数 / Operation functions
// matmul, dot, trace, outer, transpose, contract

// 通用 einsum / Generic einsum
// einsum, einsumDouble, einsumFloat, einsumInt

// DSL API
// Einstein, einstein, einsteinDouble, einsteinFloat, einsteinInt
