package fuookami.ospf.kotlin.example.framework_demo.demo3


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.model.*

data class Product(
    val length: UInt64,
    val demand: UInt64
) : AutoIndexed(Product::class)

data class CuttingPlan(
    val products: Map<Product, UInt64>
) : AutoIndexed(CuttingPlan::class)

class ShadowPriceMap : AbstractShadowPriceMap<Product, ShadowPriceMap>()

