package fuookami.ospf.kotlin.example.column_generation_demo.demo1

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.model.*

data class Product(
    val length: UInt64,
    val demand: UInt64
) : AutoIndexed()

data class CuttingPlan(
    val products: Map<Product, UInt64>
) : AutoIndexed()

class SPM : ShadowPriceMap<SPM>() {}
