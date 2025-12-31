package fuookami.ospf.kotlin.utils.math.ordinary

import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*

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
