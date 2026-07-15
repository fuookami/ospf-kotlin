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

/**
 * Dimension - 维度接口
 * Dimension - Dimension interface
 *
 * 定义几何空间的维度大小，用于类型安全的几何计算。
 * Defines the dimension size of geometric space, used for type-safe geometric calculations.
 *
 * @property size 维度大小 / Dimension size
*/
interface Dimension {

    /** 维度大小 / Dimension size */
    val size: Int

    /** 维度索引范围 / Dimension index range */
    val indices get() = 0 until size
}

/**
 * Dim1 - 一维空间
 * Dim1 - 1D space
 *
 * 表示一维空间（直线），维度大小为 1。
 * Represents 1D space (line), with dimension size 1.
*/
data object Dim1 : Dimension {
    override val size = 1
}

/**
 * Dim2 - 二维空间
 * Dim2 - 2D space
 *
 * 表示二维空间（平面），维度大小为 2。
 * Represents 2D space (plane), with dimension size 2.
*/
data object Dim2 : Dimension {
    override val size = 2
}

/**
 * Dim3 - 三维空间
 * Dim3 - 3D space
 *
 * 表示三维空间（立体），维度大小为 3。
 * Represents 3D space (solid), with dimension size 3.
*/
data object Dim3 : Dimension {
    override val size = 3
}

/**
 * Dim4 - 四维空间
 * Dim4 - 4D space
 *
 * 表示四维空间，维度大小为 4。常用于超混沌系统和时空动力学建模。
 * Represents 4D space, with dimension size 4. Commonly used for hyperchaotic systems and spacetime dynamics modeling.
*/
data object Dim4 : Dimension {
    override val size = 4
}
