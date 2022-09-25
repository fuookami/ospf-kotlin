package fuookami.ospf.kotlin.example.column_generation_demo.demo1

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.model.*

data class Product(
    override val index: Int,
    val length: UInt64,
    val demand: UInt64
) : Indexed

data class CuttingPlan(
    val products: Map<Product, UInt64>,
    override val index: Int = Indexed.nextIndex<CuttingPlan>()
) : Indexed

class SPM : ShadowPriceMap<SPM>() {}
