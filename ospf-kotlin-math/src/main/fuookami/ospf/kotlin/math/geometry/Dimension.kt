/**
 * Dimension（维度定义）
 * Dimension Definitions
 *
 * 提供几何空间维度类型的定义，用于类型安全的几何计算。
 * Provides dimension type definitions for geometric spaces, enabling type-safe geometric calculations.
 *
 * 主要类型：
 * Main types:
 * - Dimension: 维度接口，定义空间维度大小 / Dimension interface, defining spatial dimension size
 * - Dim1: 一维空间（用于直线上的点）/ 1D space (for points on a line)
 * - Dim2: 二维空间（平面几何）/ 2D space (plane geometry)
 * - Dim3: 三维空间（立体几何）/ 3D space (solid geometry)
 *
 * 应用场景：几何算法的维度约束、类型安全的点/向量定义。
 * Applications: dimension constraints for geometric algorithms, type-safe point/vector definitions.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

interface Dimension {
    val size: Int
    val indices get() = 0 until size
}

data object Dim1 : Dimension {
    override val size = 1
}

data object Dim2 : Dimension {
    override val size = 2
}

data object Dim3 : Dimension {
    override val size = 3
}




