/**
 * 域定律
 * Field Laws
 *
 * 提供域代数结构定律验证类，验证乘法交换律和乘法逆元存在性，继承自 RingLaw 验证环定律。
 * Provides field algebraic structure law validation class, verifying multiplicative commutativity and multiplicative inverse existence, inheriting from RingLaw for ring law verification.
 */
package fuookami.ospf.kotlin.math.algebra.law

class FieldLaw<Self>(
    private val samples: Collection<Self>,
    private val add: (Self, Self) -> Self,
    private val mul: (Self, Self) -> Self,
    private val zero: Self,
    private val one: Self,
    private val negate: (Self) -> Self,
    private val reciprocal: (Self) -> Self,
    private val isZero: (Self) -> Boolean,
    private val equal: (Self, Self) -> Boolean
) {
    private val ring = RingLaw(
        samples = samples,
        add = add,
        mul = mul,
        zero = zero,
        one = one,
        negate = negate,
        equal = equal
    )

    fun multiplicativeCommutative(): Boolean {
        for (lhs in samples) {
            for (rhs in samples) {
                if (!equal(mul(lhs, rhs), mul(rhs, lhs))) {
                    return false
                }
            }
        }
        return true
    }

    fun multiplicativeInverse(): Boolean {
        for (value in samples) {
            if (isZero(value)) {
                continue
            }
            val inverse = reciprocal(value)
            if (!equal(mul(value, inverse), one)) {
                return false
            }
            if (!equal(mul(inverse, value), one)) {
                return false
            }
        }
        return true
    }

    fun validate(): Boolean {
        return ring.validate()
            && multiplicativeCommutative()
            && multiplicativeInverse()
    }
}

