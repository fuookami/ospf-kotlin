package fuookami.ospf.kotlin.example.column_generation_demo.demo1

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

class CSP {
    private val length = UInt64(1000UL)
    private val products: List<Product> = arrayListOf(
        Product(UInt64(450UL), UInt64(97UL)),
        Product(UInt64(360UL), UInt64(610UL)),
        Product(UInt64(310UL), UInt64(395UL)),
        Product(UInt64(140UL), UInt64(211UL)),
    )

    operator fun invoke(): Try<Error> {
        val initialCuttingPlans = InitialSolutionGenerator(length, products)
        when (initialCuttingPlans) {
            is Failed -> {
                return Failed(initialCuttingPlans.error)
            }

            is Ok -> {}
        }
        val rmp = RMP(products, initialCuttingPlans.value)
        val sp = SP()
        var i = UInt64.zero
        while (true) {
            val spm = rmp(i)
            when (spm) {
                is Failed -> {
                    return Failed(spm.error)
                }

                is Ok -> {}
            }
            val newCuttingPlan = sp(i, length, products, spm.value)
            when (newCuttingPlan) {
                is Failed -> {
                    return Failed(newCuttingPlan.error)
                }

                is Ok -> {}
            }
            if (reducedCost(newCuttingPlan.value, spm.value) geq Flt64.zero
                || !rmp.addColumn(newCuttingPlan.value)
            ) {
                break
            }
            ++i
        }
        when (val solution = rmp()) {
            is Failed -> {
                return Failed(solution.error)
            }

            is Ok -> {}
        }
        return Ok(success)
    }

    private fun reducedCost(cuttingPlan: CuttingPlan, shadowPrices: SPM) = Flt64.one /
            -Flt64(
                cuttingPlan.products.asIterable()
                    .sumOf { (product, amount) -> (shadowPrices(product) * amount.toFlt64()).toDouble() })
}
