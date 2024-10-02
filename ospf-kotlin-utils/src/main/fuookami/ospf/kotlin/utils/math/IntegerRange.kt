package  fuookami.ospf.kotlin.utils.math

import fuookami.ospf.kotlin.utils.operator.*

@Throws(IllegalArgumentException::class)
private fun <I> getProgressionLastElement(
    start: I,
    end: I,
    step: I,
    constants: RealNumberConstants<I>
): I where I : PlusGroup<I>, I : Integer<I>, I : Rem<I, I> = when {
    step > constants.zero -> {
        if (start >= end) end
        else end - (end - start) % step
    }

    step < constants.zero -> {
        if (start <= end) end
        else end + (start - end) % -step
    }

    else -> throw IllegalArgumentException("Step is zero.")
}

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

class IntegerRange<I>(
    override val start: I,
    override val endInclusive: I,
    val step: I,
    private val constants: RealNumberConstants<I>
) : Iterable<I>, ClosedRange<I> where I : Integer<I>, I : PlusGroup<I>, I : Rem<I, I> {
    init {
        @Throws(IllegalArgumentException::class)
        if (step == step.constants.zero) {
            throw IllegalArgumentException("Step must be non-zero.")
        }
        @Throws(IllegalArgumentException::class)
        if (step == step.constants.minimum) {
            throw IllegalArgumentException("Step must be greater than ${step.javaClass}.minimum to avoid overflow on negation.")
        }
    }

    val first: I by ::start
    val last: I by lazy { getProgressionLastElement(start, endInclusive, step, constants) }

    infix fun step(step: I) = IntegerRange(start, endInclusive, step, constants)

    override fun iterator(): Iterator<I> = IntegerIterator(first, last, step, constants)

    override fun contains(value: I) = if (step > constants.zero) {
        first <= value && value <= last
    } else {
        last <= value && value <= first
    }

    override fun isEmpty() = if (step > constants.zero) {
        first > last
    } else {
        first < last
    }

    override fun toString(): String = if (step > constants.zero) {
        "$first..$last step $step"
    } else {
        "$first downTo $last step ${-step}"
    }
}

internal class NumericIntegerIterator<NI, I>(
    first: I,
    last: I,
    step: I,
    constants: RealNumberConstants<I>,
    val ctor: (I) -> NI
) : Iterator<NI> where I : Integer<I> {
    private val impl: IntegerIterator<I> by lazy { IntegerIterator(first, last, step, constants) }

    override fun hasNext() = impl.hasNext()
    override fun next(): NI = ctor(impl.next())
}

class NumericUIntegerRange<NI, I>(
    override val start: NI,
    override val endInclusive: NI,
    _step: NI,
    private val constants: RealNumberConstants<I>,
    private val ctor: (I) -> NI,
    private val converter: (NI) -> I
) : Iterable<NI>, ClosedRange<NI>
        where NI : NumericUIntegerNumber<NI, I>, I : UIntegerNumber<I>, I : PlusGroup<I>, I : Rem<I, I> {
    val step: I by lazy { converter(_step) }

    init {
        @Throws(IllegalArgumentException::class)
        if (step == step.constants.zero) {
            throw IllegalArgumentException("Step must be non-zero.")
        }
        @Throws(IllegalArgumentException::class)
        if (step == step.constants.minimum) {
            throw IllegalArgumentException("Step must be greater than ${step.javaClass}.minimum to avoid overflow on negation.")
        }
    }

    val first: I by lazy { converter(start) }
    val last: I by lazy { getProgressionLastElement(converter(start), converter(endInclusive), step, constants) }

    infix fun step(step: NI) = NumericUIntegerRange(start, endInclusive, step, constants, ctor, converter)

    override fun iterator(): Iterator<NI> = NumericIntegerIterator(first, last, step, constants, ctor)

    override fun contains(value: NI): Boolean {
        val actualValue = converter(value)
        return if (step > constants.zero) {
            actualValue in first..last
        } else {
            actualValue in last..first
        }
    }

    override fun isEmpty() = if (step > constants.zero) {
        first > last
    } else {
        first < last
    }

    override fun toString(): String = if (step > constants.zero) {
        "$first..$last step $step"
    } else {
        "$first downTo $last step ${-step}"
    }
}
