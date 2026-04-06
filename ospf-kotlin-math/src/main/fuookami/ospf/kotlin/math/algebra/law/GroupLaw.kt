/**
 * 群定律
 * Group Laws
 *
 * 提供群代数结构定律验证类，验证结合律、单位元存在性和逆元存在性。
 * Provides group algebraic structure law validation class, verifying associativity, identity existence, and inverse existence.
 */
package fuookami.ospf.kotlin.math.algebra.law

class GroupLaw<Self>(
    private val samples: Collection<Self>,
    private val add: (Self, Self) -> Self,
    private val zero: Self,
    private val negate: (Self) -> Self,
    private val equal: (Self, Self) -> Boolean
) {
    fun isAssociative(): Boolean {
        for (lhs in samples) {
            for (mid in samples) {
                for (rhs in samples) {
                    val left = add(add(lhs, mid), rhs)
                    val right = add(lhs, add(mid, rhs))
                    if (!equal(left, right)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun hasIdentity(): Boolean {
        for (value in samples) {
            if (!equal(add(value, zero), value)) {
                return false
            }
            if (!equal(add(zero, value), value)) {
                return false
            }
        }
        return true
    }

    fun hasInverse(): Boolean {
        for (value in samples) {
            val inverse = negate(value)
            if (!equal(add(value, inverse), zero)) {
                return false
            }
            if (!equal(add(inverse, value), zero)) {
                return false
            }
        }
        return true
    }

    fun validate(): Boolean {
        return isAssociative() && hasIdentity() && hasInverse()
    }
}

