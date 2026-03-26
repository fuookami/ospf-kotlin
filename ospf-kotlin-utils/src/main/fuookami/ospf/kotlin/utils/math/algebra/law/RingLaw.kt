package fuookami.ospf.kotlin.utils.math.algebra.law

class RingLaw<Self>(
    private val samples: Collection<Self>,
    private val add: (Self, Self) -> Self,
    private val mul: (Self, Self) -> Self,
    private val zero: Self,
    private val one: Self,
    private val negate: (Self) -> Self,
    private val equal: (Self, Self) -> Boolean
) {
    private val additiveGroup = GroupLaw(
        samples = samples,
        add = add,
        zero = zero,
        negate = negate,
        equal = equal
    )

    fun additiveCommutative(): Boolean {
        for (lhs in samples) {
            for (rhs in samples) {
                if (!equal(add(lhs, rhs), add(rhs, lhs))) {
                    return false
                }
            }
        }
        return true
    }

    fun multiplicativeAssociative(): Boolean {
        for (lhs in samples) {
            for (mid in samples) {
                for (rhs in samples) {
                    val left = mul(mul(lhs, mid), rhs)
                    val right = mul(lhs, mul(mid, rhs))
                    if (!equal(left, right)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun multiplicativeIdentity(): Boolean {
        for (value in samples) {
            if (!equal(mul(value, one), value)) {
                return false
            }
            if (!equal(mul(one, value), value)) {
                return false
            }
        }
        return true
    }

    fun distributive(): Boolean {
        for (lhs in samples) {
            for (mid in samples) {
                for (rhs in samples) {
                    val leftDistribute = mul(lhs, add(mid, rhs))
                    val leftExpected = add(mul(lhs, mid), mul(lhs, rhs))
                    if (!equal(leftDistribute, leftExpected)) {
                        return false
                    }

                    val rightDistribute = mul(add(lhs, mid), rhs)
                    val rightExpected = add(mul(lhs, rhs), mul(mid, rhs))
                    if (!equal(rightDistribute, rightExpected)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun validate(): Boolean {
        return additiveGroup.validate()
            && additiveCommutative()
            && multiplicativeAssociative()
            && multiplicativeIdentity()
            && distributive()
    }
}

