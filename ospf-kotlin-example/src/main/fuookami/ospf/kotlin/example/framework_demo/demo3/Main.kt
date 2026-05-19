package fuookami.ospf.kotlin.example.framework_demo.demo3


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*

class CSP {
    private val length = UInt64(1000UL)
    private val products: List<Product> = arrayListOf(
        Product(UInt64(450UL), UInt64(97UL)),
        Product(UInt64(360UL), UInt64(610UL)),
        Product(UInt64(310UL), UInt64(395UL)),
        Product(UInt64(140UL), UInt64(211UL)),
    )

    suspend operator fun invoke(): Try {
        val initialCuttingPlans = InitialSolutionGenerator(length, products)
        when (initialCuttingPlans) {
            is Failed -> {
                return Failed(initialCuttingPlans.error)
            }

            is Fatal -> {
                return Fatal(initialCuttingPlans.errors)
            }

            is Ok -> {}
        }
        val rmp = RMP(length, products, initialCuttingPlans.value)
        val sp = SP()
        var i = UInt64.zero
        while (true) {
            val spm = rmp(i)
            when (spm) {
                is Failed -> {
                    return Failed(spm.error)
                }

                is Fatal -> {
                    return Fatal(spm.errors)
                }

                is Ok -> {}
            }
            val newCuttingPlan = sp(i, length, products, spm.value)
            when (newCuttingPlan) {
                is Failed -> {
                    return Failed(newCuttingPlan.error)
                }

                is Fatal -> {
                    return Fatal(newCuttingPlan.errors)
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

            is Fatal -> {
                return Fatal(solution.errors)
            }

            is Ok -> {
                println(
                    solution.value.asIterable().joinToString(";") {
                        "${
                            it.key.products.asIterable()
                                .joinToString(",") { product -> "${product.key.length} * ${product.value}" }
                        }: ${it.value}"
                    })
            }
        }
        return ok
    }

    private fun reducedCost(cuttingPlan: CuttingPlan, shadowPrices: ShadowPriceMap) = Flt64.one -
            cuttingPlan.products.entries.fold(Flt64.zero) { acc, entry ->
                acc + shadowPrices(entry.key) * entry.value.toFlt64()
            }
}









