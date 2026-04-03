package fuookami.ospf.kotlin.math.multiarray

import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.concept.Ring

// ============================================================================
// FastSum - High-performance summation for MultiArray
// ============================================================================

/**
 * Exception thrown when axis index is out of bounds.
 */
class AxisOutOfBoundsException(
    val axis: Int,
    val maxAxis: Int
) : Exception() {
    override val message: String =
        "Axis $axis out of bounds (max: $maxAxis)"
}

// ============================================================================
// sumAll - Sum all elements
// ============================================================================

/**
 * Sum all elements in the array.
 *
 * This is the simplest FastSum operation - iterate and accumulate.
 *
 * @param zero The zero value for type T
 * @return The sum of all elements
 */
fun <T> AbstractMultiArray<T, *>.sumAll(zero: T): T where T : Ring<T> {
    var acc = zero
    for (i in 0 until size) {
        acc += this[i]
    }
    return acc
}

// ============================================================================
// sumAxis - Sum along a single axis
// ============================================================================

/**
 * Sum along a specified axis.
 *
 * Returns a new MultiArray with one fewer dimension.
 *
 * @param axis The axis to sum along (0-indexed)
 * @param zero The zero value for type T
 * @return A new MultiArray with the summed axis removed
 * @throws AxisOutOfBoundsException if axis is out of bounds
 */
fun <T> AbstractMultiArray<T, *>.sumAxis(axis: Int, zero: T): MultiArray<T, DynShape> where T : Ring<T> {
    val ndim = shape.dimension
    if (axis >= ndim) {
        throw AxisOutOfBoundsException(axis, ndim - 1)
    }

    // Get current shape
    val currentShape = IntArray(ndim) { shape[it] }

    // Calculate new shape (remove summed axis)
    val newShapeDims = currentShape.filterIndexed { i, _ -> i != axis }.toIntArray()
    val newShape = DynShape(newShapeDims)

    // Create mutable result array for accumulation
    val result = MutableMultiArray.newWith(newShape, zero)

    // Iterate all elements and accumulate to result
    for (linearIdx in 0 until size) {
        val vector = shape.vector(linearIdx)

        // Calculate result coordinates (remove summed axis)
        val resultVector = vector.filterIndexed { i, _ -> i != axis }.toIntArray()

        // Accumulate
        val resultLinearIdx = result.shape.index(resultVector)
        result[resultLinearIdx] = result[resultLinearIdx] + this[linearIdx]
    }

    return result.toImmutable()
}

// ============================================================================
// sumAxes - Sum along multiple axes
// ============================================================================

/**
 * Sum along multiple axes.
 *
 * Returns a new MultiArray with the specified axes removed.
 *
 * @param axes The axes to sum along
 * @param zero The zero value for type T
 * @return A new MultiArray with the summed axes removed
 * @throws AxisOutOfBoundsException if any axis is out of bounds
 */
fun <T> AbstractMultiArray<T, *>.sumAxes(axes: IntArray, zero: T): MultiArray<T, DynShape> where T : Ring<T> {
    if (axes.isEmpty()) {
        // No axes to sum, return copy with DynShape
        val currentShape = IntArray(shape.dimension) { shape[it] }
        return MultiArray.newBy(DynShape(currentShape)) { i, _ -> this[i] }
    }

    val ndim = shape.dimension
    for (axis in axes) {
        if (axis >= ndim) {
            throw AxisOutOfBoundsException(axis, ndim - 1)
        }
    }

    // Get current shape
    val currentShape = IntArray(ndim) { shape[it] }

    // Calculate new shape (remove all summed axes)
    val axesSet = axes.toSet()
    val newShapeDims = currentShape.filterIndexed { i, _ -> i !in axesSet }.toIntArray()
    val newShape = DynShape(newShapeDims)

    // Create mutable result array for accumulation
    val result = MutableMultiArray.newWith(newShape, zero)

    // Iterate all elements and accumulate to result
    for (linearIdx in 0 until size) {
        val vector = shape.vector(linearIdx)

        // Calculate result coordinates (remove summed axes)
        val resultVector = vector.filterIndexed { i, _ -> i !in axesSet }.toIntArray()

        // Accumulate
        val resultLinearIdx = result.shape.index(resultVector)
        result[resultLinearIdx] = result[resultLinearIdx] + this[linearIdx]
    }

    return result.toImmutable()
}

// ============================================================================
// cumsumAxis - Cumulative sum along an axis (prefix sum)
// ============================================================================

/**
 * Cumulative sum along an axis (prefix sum).
 *
 * Returns a new MultiArray with the same shape, where each element
 * is the cumulative sum along the specified axis.
 *
 * @param axis The axis to cumsum along
 * @param zero The zero value for type T
 * @return A new MultiArray with cumulative sums
 * @throws AxisOutOfBoundsException if axis is out of bounds
 */
fun <T> AbstractMultiArray<T, *>.cumsumAxis(axis: Int, zero: T): MultiArray<T, DynShape> where T : Ring<T> {
    val ndim = shape.dimension
    if (axis >= ndim) {
        throw AxisOutOfBoundsException(axis, ndim - 1)
    }

    // Get current shape
    val currentShape = IntArray(ndim) { shape[it] }

    // Create mutable result array (same shape)
    val newShape = DynShape(currentShape)
    val result = MutableMultiArray.newWith(newShape, zero)

    // For each "slice" along other dimensions, compute cumulative sum
    for (linearIdx in 0 until size) {
        val vector = shape.vector(linearIdx)

        // If this is the first element along the axis, start fresh
        // Otherwise, add previous cumulative sum
        if (vector[axis] == 0) {
            result[linearIdx] = this[linearIdx]
        } else {
            // Get the previous element's coordinates
            val prevVector = vector.copyOf()
            prevVector[axis] = vector[axis] - 1
            val prevLinearIdx = shape.index(prevVector)

            result[linearIdx] = result[prevLinearIdx] + this[linearIdx]
        }
    }

    return result.toImmutable()
}