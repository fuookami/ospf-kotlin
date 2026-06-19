/**
 * 整数区间
 * Integer Range
 *
 * 为任意整数类型提供可迭代的区间实现，支持指定步长的正向和反向迭代。
 * Provides iterable range implementation for arbitrary integer types, supporting forward and backward iteration with specified steps.
 */
package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.operator.*

/**
 * 获取区间的最后一个元素
 * Get the last element of the progression
 *
 * @param start 起始值
 * @param end 结束值
 * @param step 步长
 * @param constants 数值常量
 * @return 最后一个元素，步长非法时返回 null
 */
private fun <I> getProgressionLastElementOrNull(
    start: I,
    end: I,
    step: I,
    constants: RealNumberConstants<I>
): I? where I : PlusGroup<I>, I : Integer<I>, I : Rem<I, I> = when {
    step > constants.zero -> {
        if (start >= end) end
        else end - (end - start) % step
    }

    step < constants.zero -> {
        if (start <= end) end
        else end + (start - end) % -step
    }

    else -> null
}

/**
 * 整数迭代器
 * Integer iterator
 *
 * @param I 整数类型
 */
internal class IntegerIterator<I>(
    first: I,
    last: I,
    val step: I,
    constants: RealNumberConstants<I>
) : Iterator<I> where I : Integer<I> {
    private val finalElement: I = last
    private var hasNext: Boolean = if (step > constants.zero) {
        first <= last
    } else {
        first >= last
    }
    private var next: I = if (hasNext) {
        first
    } else {
        finalElement
    }

    override fun hasNext() = hasNext

    override fun next(): I {
        val value = next
        if (value == finalElement) {
            if (!hasNext) {
                throw kotlin.NoSuchElementException()
            } else {
                hasNext = false
            }
        } else {
            next += step
        }
        return value
    }
}

/**
 * 整数区间
 * Integer range
 *
 * 表示一个可迭代的整数区间，支持指定步长的正向和反向遍历。
 * Represents an iterable integer range, supporting forward and backward traversal with specified steps.
 *
 * @param I 整数类型
 * @property start 起始值
 * @property endInclusive 结束值（包含）
 * @property step 步长
 */
class IntegerRange<I>(
    override val start: I,
    override val endInclusive: I,
    step: I,
    private val constants: RealNumberConstants<I>
) : Iterable<I>, ClosedRange<I>, Contains<I> where I : Integer<I>, I : PlusGroup<I>, I : Rem<I, I> {
    val step: I = step
    private val validStep: Boolean get() = isValidIntegerStep(step)

    val first: I by ::start
    val last: I by lazy {
        getProgressionLastElementOrNull(
            start = start,
            end = endInclusive,
            step = step,
            constants = constants
        ) ?: endInclusive
    }

    /**
     * 指定步长
     * Specify step size
     *
     * @param step 步长值 / Step size value
     * @return 指定步长后的新区间 / New range with specified step size
     */
    infix fun step(step: I): IntegerRange<I> {
        return IntegerRange(
            start = start,
            endInclusive = endInclusive,
            step = step,
            constants = constants
        )
    }

    /**
     * 指定步长，非法步长返回失败
     * Specify step size, returning failure for invalid steps
     *
     * @param step 步长值 / Step size value
     * @return 指定步长后的新区间结果 / Result of new range with specified step size
     */
    fun stepSafe(step: I): Ret<IntegerRange<I>> {
        return if (!isValidIntegerStep(step)) {
            Failed(ErrorCode.IllegalArgument, invalidIntegerStepMessage(step))
        } else {
            Ok(
                IntegerRange(
                    start = start,
                    endInclusive = endInclusive,
                    step = step,
                    constants = constants
                )
            )
        }
    }

    /**
     * 指定步长，非法步长返回 null
     * Specify step size, returning null for invalid steps
     *
     * @param step 步长值 / Step size value
     * @return 指定步长后的新区间或 null / New range with specified step size, or null
     */
    fun stepOrNull(step: I): IntegerRange<I>? {
        return if (!isValidIntegerStep(step)) {
            null
        } else {
            IntegerRange(
                start = start,
                endInclusive = endInclusive,
                step = step,
                constants = constants
            )
        }
    }

    override fun iterator(): Iterator<I> {
        return if (validStep) {
            IntegerIterator(
                first = first,
                last = last,
                step = step,
                constants = constants
            )
        } else {
            emptyList<I>().iterator()
        }
    }

    override fun contains(value: I): Boolean {
        return validStep && if (step > constants.zero) {
            first <= value && value <= last
        } else {
            last <= value && value <= first
        }
    }

    override fun isEmpty(): Boolean {
        return !validStep || if (step > constants.zero) {
            first > last
        } else {
            first < last
        }
    }

    override fun toString(): String {
        return if (!validStep) {
            "empty step $step"
        } else if (step > constants.zero) {
            "$first..$last step $step"
        } else {
            "$first downTo $last step ${-step}"
        }
    }
}

/**
 * 数值整数迭代器
 * Numeric integer iterator
 *
 * @param NI 数值整数类型
 * @param I 整数类型
 */
internal class NumericIntegerIterator<NI, I>(
    first: I,
    last: I,
    step: I,
    constants: RealNumberConstants<I>,
    val ctor: (I) -> NI
) : Iterator<NI> where I : Integer<I> {
    private val impl: IntegerIterator<I> by lazy {
        IntegerIterator(
            first = first,
            last = last,
            step = step,
            constants = constants
        )
    }

    override fun hasNext() = impl.hasNext()
    override fun next(): NI = ctor(impl.next())
}

/**
 * 数值无符号整数区间
 * Numeric unsigned integer range
 *
 * 表示一个可迭代的无符号整数区间，支持指定步长的正向遍历。
 * Represents an iterable unsigned integer range, supporting forward traversal with specified steps.
 *
 * @param NI 数值整数类型
 * @param I 整数类型
 * @property start 起始值
 * @property endInclusive 结束值（包含）
 */
class NumericUIntegerRange<NI, I>(
    override val start: NI,
    override val endInclusive: NI,
    _step: NI,
    private val constants: RealNumberConstants<I>,
    private val ctor: (I) -> NI,
    private val converter: (NI) -> I
) : Iterable<NI>, ClosedRange<NI>, Contains<NI>
        where NI : NumericUIntegerNumber<NI, I>, I : UIntegerNumber<I>, I : PlusGroup<I>, I : Rem<I, I> {
    val step: I by lazy { converter(_step) }
    private val validStep: Boolean by lazy { isValidIntegerStep(step) }

    val first: I by lazy { converter(start) }
    val last: I by lazy {
        getProgressionLastElementOrNull(
            start = converter(start),
            end = converter(endInclusive),
            step = step,
            constants = constants
        ) ?: converter(endInclusive)
    }

    /**
     * 指定步长
     * Specify step size
     *
     * @param step 步长值 / Step size value
     * @return 指定步长后的新区间 / New range with specified step size
     */
    infix fun step(step: NI): NumericUIntegerRange<NI, I> {
        return NumericUIntegerRange(
            start = start,
            endInclusive = endInclusive,
            _step = step,
            constants = constants,
            ctor = ctor,
            converter = converter
        )
    }

    /**
     * 指定步长，非法步长返回失败
     * Specify step size, returning failure for invalid steps
     *
     * @param step 步长值 / Step size value
     * @return 指定步长后的新区间结果 / Result of new range with specified step size
     */
    fun stepSafe(step: NI): Ret<NumericUIntegerRange<NI, I>> {
        val integerStep = converter(step)
        return if (!isValidIntegerStep(integerStep)) {
            Failed(ErrorCode.IllegalArgument, invalidIntegerStepMessage(integerStep))
        } else {
            Ok(
                NumericUIntegerRange(
                    start = start,
                    endInclusive = endInclusive,
                    _step = step,
                    constants = constants,
                    ctor = ctor,
                    converter = converter
                )
            )
        }
    }

    /**
     * 指定步长，非法步长返回 null
     * Specify step size, returning null for invalid steps
     *
     * @param step 步长值 / Step size value
     * @return 指定步长后的新区间或 null / New range with specified step size, or null
     */
    fun stepOrNull(step: NI): NumericUIntegerRange<NI, I>? {
        val integerStep = converter(step)
        return if (!isValidIntegerStep(integerStep)) {
            null
        } else {
            NumericUIntegerRange(
                start = start,
                endInclusive = endInclusive,
                _step = step,
                constants = constants,
                ctor = ctor,
                converter = converter
            )
        }
    }

    override fun iterator(): Iterator<NI> {
        return if (validStep) {
            NumericIntegerIterator(
                first = first,
                last = last,
                step = step,
                constants = constants,
                ctor = ctor
            )
        } else {
            emptyList<NI>().iterator()
        }
    }

    override fun contains(value: NI): Boolean {
        val actualValue = converter(value)
        return validStep && if (step > constants.zero) {
            actualValue in first..last
        } else {
            actualValue in last..first
        }
    }

    override fun isEmpty(): Boolean {
        return !validStep || if (step > constants.zero) {
            first > last
        } else {
            first < last
        }
    }

    override fun toString(): String {
        return if (!validStep) {
            "empty step $step"
        } else if (step > constants.zero) {
            "$first..$last step $step"
        } else {
            "$first downTo $last step ${-step}"
        }
    }
}

private fun <I> isValidIntegerStep(step: I): Boolean where I : Integer<I> {
    return step != step.constants.zero && step != step.constants.minimum
}

private fun <I> invalidIntegerStepMessage(step: I): String where I : Integer<I> {
    return if (step == step.constants.zero) {
        "步长不能为零。 / Step must be non-zero."
    } else {
        "步长必须大于 ${step.javaClass}.minimum 以避免取负溢出。 / " +
                "Step must be greater than ${step.javaClass}.minimum to avoid overflow on negation."
    }
}
