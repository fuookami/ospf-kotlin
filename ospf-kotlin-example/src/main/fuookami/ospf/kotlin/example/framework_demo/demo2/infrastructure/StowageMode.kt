package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

/**
 * 配载模式
 */
enum class StowageMode {
    /**
     * 预配载
     */
    Predistribution,

    /**
     * 全配载
     */
    FullLoad,

    /**
     * 建议打板
     */
    WeightRecommendation {
        override val withMacOptimization: Boolean = false
        override val withSoftSecurity: Boolean = false
        override val withPayloadMaximization: Boolean = true
    };
    
    open val withMacOptimization: Boolean = true
    open val withSoftSecurity: Boolean = true
    open val withPayloadMaximization: Boolean = false
}
