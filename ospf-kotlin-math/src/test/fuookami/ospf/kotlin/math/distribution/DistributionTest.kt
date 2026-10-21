/**
 * 概率分布采样测试 / Probability distribution sampling tests
*/
package fuookami.ospf.kotlin.math.distribution

import kotlin.math.*
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 确定性伪随机熵源，保证统计测试可复现 / Deterministic pseudo-random entropy source that keeps statistical tests reproducible
 *
 * @property seed 初始种子 / Initial seed
*/
private class SeededEntropy(seed: Long = 20240521L) : Generator<Flt64> {
    private var state = seed

    override fun invoke(): Flt64 {
        state = state * 6364136223846793005L + 1442695040888963407L
        val bits = (state ushr 11).toDouble() / (1L shl 53).toDouble()
        return Flt64(bits)
    }
}

private const val SAMPLE_AMOUNT = 20000

/**
 * 采样指定数量并解包为 Double 列表 / Sample the requested amount and unwrap into a Double list
 *
 * @param amount 采样数量 / Number of samples
 * @return 采样值列表 / List of sampled values
*/
private fun Distribution.samples(amount: Int = SAMPLE_AMOUNT): List<Double> {
    val entropy = SeededEntropy()
    return List(amount) {
        when (val result = invoke(entropy)) {
            is Ok -> result.value.value
            else -> fail("sampling failed / 采样失败: $result")
        }
    }
}

/**
 * 计算样本方差 / Compute the sample variance
 *
 * @return 方差 / Variance
*/
private fun List<Double>.variance(): Double {
    val mean = average()
    var accumulated = 0.0
    for (value in this) {
        val difference = value - mean
        accumulated += difference * difference
    }
    return accumulated / size
}

/**
 * 计算样本中位数 / Compute the sample median
 *
 * @return 中位数 / Median
*/
private fun List<Double>.median(): Double {
    val sorted = sorted()
    val middle = size / 2
    return if (size % 2 == 0) {
        (sorted[middle - 1] + sorted[middle]) / 2.0
    } else {
        sorted[middle]
    }
}

class DistributionTest {

    @Test
    fun uniformShouldMatchTheoreticalMoments() {
        val samples = Uniform(Flt64(-2.0), Flt64(4.0)).samples()
        assertEquals(1.0, samples.average(), 0.05)
        assertEquals(3.0, samples.variance(), 0.15)
        assertTrue(samples.all { it >= -2.0 && it <= 4.0 })
    }

    @Test
    fun gaussianShouldMatchStandardNormalMoments() {
        val samples = Gaussian().samples()
        assertEquals(0.0, samples.average(), 0.03)
        assertEquals(1.0, samples.variance(), 0.06)
    }

    @Test
    fun cauchyShouldBeCenteredAndHeavyTailed() {
        val samples = Cauchy().samples()
        assertEquals(0.0, samples.median(), 0.05)
        val farTail = samples.count { abs(it) > 5.0 }.toDouble() / samples.size
        assertEquals(0.126, farTail, 0.03)
    }

    @Test
    fun exponentialShouldMatchTheoreticalMean() {
        val samples = Exponential(Flt64(2.0)).samples()
        assertEquals(0.5, samples.average(), 0.02)
    }

    @Test
    fun gammaShouldMatchTheoreticalMean() {
        val samples = Gamma(Flt64(2.0), Flt64(3.0)).samples()
        assertEquals(6.0, samples.average(), 0.15)
    }

    @Test
    fun betaShouldStayInUnitIntervalWithTheoreticalMean() {
        val samples = Beta(Flt64(2.0), Flt64(5.0)).samples()
        assertTrue(samples.all { it > 0.0 && it < 1.0 })
        assertEquals(2.0 / 7.0, samples.average(), 0.01)
    }

    @Test
    fun studentTShouldBeCentered() {
        val samples = StudentT(Flt64(5.0)).samples()
        assertTrue(samples.all { it.isFinite() })
        assertEquals(0.0, samples.average(), 0.08)
    }

    @Test
    fun levyShouldBeSymmetricWithHeavyTails() {
        val samples = Levy(Flt64(1.5)).samples()
        assertTrue(samples.all { it.isFinite() })
        assertEquals(0.0, samples.median(), 0.1)
        val extreme = samples.maxOf { abs(it) }
        assertTrue(extreme > 10.0, "Lévy samples should contain far outliers / 莱维样本应包含远处离群值")
    }

    @Test
    fun truncatedShouldRespectBounds() {
        val samples = Truncated(
            distribution = Gaussian(standardDeviation = Flt64(10.0)),
            lowerBound = Flt64(-1.0),
            upperBound = Flt64(1.0)
        ).samples()
        assertTrue(samples.all { it >= -1.0 && it <= 1.0 })
    }

    @Test
    fun directionSamplerShouldProduceUnitVectors() {
        val entropy = SeededEntropy()
        val sampler = DirectionSampler(3)
        repeat(200) {
            when (val result = sampler(entropy)) {
                is Ok -> {
                    var squaredNorm = 0.0
                    for (component in result.value) {
                        squaredNorm += component.value * component.value
                    }
                    assertEquals(1.0, sqrt(squaredNorm), 1.0e-9)
                }

                else -> fail("direction sampling failed / 方向采样失败: $result")
            }
        }
    }

    @Test
    fun invalidParametersShouldReturnFailed() {
        val entropy = SeededEntropy()
        assertTrue(Uniform(Flt64(2.0), Flt64(1.0)).invoke(entropy) is Failed)
        assertTrue(Gaussian(standardDeviation = Flt64(-1.0)).invoke(entropy) is Failed)
        assertTrue(Cauchy(scale = Flt64.zero).invoke(entropy) is Failed)
        assertTrue(Exponential(rate = Flt64.zero).invoke(entropy) is Failed)
        assertTrue(Gamma(shape = Flt64.zero).invoke(entropy) is Failed)
        assertTrue(Beta(alpha = Flt64.zero).invoke(entropy) is Failed)
        assertTrue(StudentT(degreesOfFreedom = Flt64.zero).invoke(entropy) is Failed)
        assertTrue(Levy(alpha = Flt64(3.0)).invoke(entropy) is Failed)
        assertTrue(
            Truncated(
                distribution = Gaussian(),
                lowerBound = Flt64(1.0),
                upperBound = Flt64(-1.0)
            ).invoke(entropy) is Failed
        )
        assertTrue(DirectionSampler(0).invoke(entropy) is Failed)
    }
}
