package fuookami.ospf.kotlin.math.ordinary

import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.UInt64

class PrimeTest {
    @Test
    fun isPrime() {
        assert(isPrime(UInt64.two))
        assert(isPrime(UInt64.three))
        assert(!isPrime(UInt64(4)))
        assert(isPrime(UInt64.five))
        assert(!isPrime(UInt64(6)))
        assert(isPrime(UInt64(7)))
        assert(!isPrime(UInt64(8)))
        assert(!isPrime(UInt64(9)))
    }
}
