package fuookami.ospf.kotlin.multiarray

import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

// 固定HEAD的优化前参照，仅用于差分验证。 / Frozen pre-optimization HEAD reference for differential tests only.
internal object FastSumReference {
    fun <T> AbstractMultiArray<T, *>.sumAxisSafe(axis: Int, zero: T): Ret<MultiArray<T, DynShape>> where T : Ring<T> {
        val ndim = shape.dimension
        if (axis !in 0 until ndim) {
            return Failed(ErrorCode.IllegalArgument, "Axis $axis out of bounds (max: ${ndim - 1}).")
        }

        // Get current shape / 获取当前形状
        val currentShape = IntArray(ndim) { shape[it] }

        // Calculate new shape (remove summed axis) / 计算新形状（移除求和轴）
        val newShapeDims = currentShape.filterIndexed { i, _ -> i != axis }.toIntArray()
        val newShape = DynShape(newShapeDims)

        // Create mutable result array for accumulation / 创建可变结果数组用于累加
        val result = MutableMultiArray.newWith(newShape, zero)

        // Iterate all elements and accumulate to result / 遍历所有元素并累加到结果
        for (linearIdx in 0 until size) {
            val vector = when (val result = shape.vector(linearIdx)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // Calculate result coordinates (remove summed axis) / 计算结果坐标（移除求和轴）
            val resultVector = vector.filterIndexed { i, _ -> i != axis }.toIntArray()

            // Accumulate / 累加
            val resultLinearIdx = when (val indexResult = result.shape.index(resultVector)) {
                is Ok -> indexResult.value
                is Failed -> return Failed(indexResult.error)
                is Fatal -> return Fatal(indexResult.errors)
            }
            result[resultLinearIdx] = result[resultLinearIdx] + this[linearIdx]
        }

        return Ok(result.toImmutable())
    }

    /** 沿指定轴求和 / Sum along a specified axis */
    fun <T> AbstractMultiArray<T, *>.sumAxis(axis: Int, zero: T): Ret<MultiArray<T, DynShape>> where T : Ring<T> {
        return sumAxisSafe(axis, zero)
    }

    // ============================================================================
    // sumAxes - Sum along multiple axes
    // ============================================================================

    /**
     * 沿多个轴求和 / Sum along multiple axes
     *
     * 返回一个移除指定轴后的新 MultiArray。 / Returns a new MultiArray with the specified axes removed.
     *
     * @param axes 求和轴索引数组 / The axes to sum along
     * @param zero 零值（累加初始值） / The zero value for type T
     * @return 移除求和轴后的新数组结果 / Result of a new MultiArray with the summed axes removed
    */
    fun <T> AbstractMultiArray<T, *>.sumAxesSafe(axes: IntArray, zero: T): Ret<MultiArray<T, DynShape>> where T : Ring<T> {
        if (axes.isEmpty()) {
            // No axes to sum, return copy with DynShape / 无求和轴，返回 DynShape 副本
            val currentShape = IntArray(shape.dimension) { shape[it] }
            return Ok(MultiArray.newBy(DynShape(currentShape)) { i, _ -> this[i] })
        }

        val ndim = shape.dimension
        for (axis in axes) {
            if (axis !in 0 until ndim) {
                return Failed(ErrorCode.IllegalArgument, "Axis $axis out of bounds (max: ${ndim - 1}).")
            }
        }

        // Get current shape / 获取当前形状
        val currentShape = IntArray(ndim) { shape[it] }

        // Calculate new shape (remove all summed axes) / 计算新形状（移除所有求和轴）
        val axesSet = axes.toSet()
        val newShapeDims = currentShape.filterIndexed { i, _ -> i !in axesSet }.toIntArray()
        val newShape = DynShape(newShapeDims)

        // Create mutable result array for accumulation / 创建可变结果数组用于累加
        val result = MutableMultiArray.newWith(newShape, zero)

        // Iterate all elements and accumulate to result / 遍历所有元素并累加到结果
        for (linearIdx in 0 until size) {
            val vector = when (val result = shape.vector(linearIdx)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // Calculate result coordinates (remove summed axes) / 计算结果坐标（移除求和轴）
            val resultVector = vector.filterIndexed { i, _ -> i !in axesSet }.toIntArray()

            // Accumulate / 累加
            val resultLinearIdx = when (val indexResult = result.shape.index(resultVector)) {
                is Ok -> indexResult.value
                is Failed -> return Failed(indexResult.error)
                is Fatal -> return Fatal(indexResult.errors)
            }
            result[resultLinearIdx] = result[resultLinearIdx] + this[linearIdx]
        }

        return Ok(result.toImmutable())
    }

    /** 沿多个轴求和 / Sum along multiple axes */
    fun <T> AbstractMultiArray<T, *>.sumAxes(axes: IntArray, zero: T): Ret<MultiArray<T, DynShape>> where T : Ring<T> {
        return sumAxesSafe(axes, zero)
    }

    // ============================================================================
    // cumsumAxis - Cumulative sum along an axis (prefix sum)
    // ============================================================================

    /**
     * 沿指定轴累积求和（前缀和） / Cumulative sum along an axis (prefix sum)
     *
     * 返回一个形状相同的新 MultiArray，其中每个元素是指定轴方向上的累积和。 / Returns a new MultiArray with the same shape, where each element
     * is the cumulative sum along the specified axis.
     *
     * @param axis 累积求和轴索引 / The axis to cumsum along
     * @param zero 零值（累加初始值） / The zero value for type T
     * @return 包含累积和的新数组结果 / Result of a new MultiArray with cumulative sums
    */
    fun <T> AbstractMultiArray<T, *>.cumsumAxisSafe(axis: Int, zero: T): Ret<MultiArray<T, DynShape>> where T : Ring<T> {
        val ndim = shape.dimension
        if (axis !in 0 until ndim) {
            return Failed(ErrorCode.IllegalArgument, "Axis $axis out of bounds (max: ${ndim - 1}).")
        }

        // Get current shape / 获取当前形状
        val currentShape = IntArray(ndim) { shape[it] }

        // Create mutable result array (same shape) / 创建可变结果数组（相同形状）
        val newShape = DynShape(currentShape)
        val result = MutableMultiArray.newWith(newShape, zero)

        // For each "slice" along other dimensions, compute cumulative sum / 对每个切片沿指定轴计算累积和
        for (linearIdx in 0 until size) {
            val vector = when (val result = shape.vector(linearIdx)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // If this is the first element along the axis, start fresh / 若为轴方向首个元素，直接赋值
            // Otherwise, add previous cumulative sum / 否则加上前一个累积和
            if (vector[axis] == 0) {
                result[linearIdx] = this[linearIdx]
            } else {
                // Get the previous element's coordinates / 获取前一个元素的坐标
                val prevVector = vector.copyOf()
                prevVector[axis] = vector[axis] - 1
                val prevLinearIdx = when (val indexResult = shape.index(prevVector)) {
                    is Ok -> indexResult.value
                    is Failed -> return Failed(indexResult.error)
                    is Fatal -> return Fatal(indexResult.errors)
                }

                result[linearIdx] = result[prevLinearIdx] + this[linearIdx]
            }
        }

        return Ok(result.toImmutable())
    }

    /** 沿指定轴累积求和 / Cumulative sum along an axis */
    fun <T> AbstractMultiArray<T, *>.cumsumAxis(axis: Int, zero: T): Ret<MultiArray<T, DynShape>> where T : Ring<T> {
        return cumsumAxisSafe(axis, zero)
    }
}
