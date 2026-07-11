package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

/**
 * Stowage mode enumeration defining the operational mode for cargo loading.
 * 配载模式枚举，定义货物装载的运行模式。
*/
enum class StowageMode {
    /**
     * Predistribution mode for pre-allocating cargo to positions.
     * 预配载模式，用于将货物预分配到舱位。
    */
    Predistribution,

    /**
     * Full-load mode for maximizing cargo loading.
     * 全配载模式，用于最大化货物装载。
    */
    FullLoad,

    /**
     * Weight recommendation mode for suggesting ULD weight assignments.
     * 建议打板模式，用于推荐集装器重量分配。
    */
    WeightRecommendation {
        override val withMacOptimization: Boolean = false
        override val withSoftSecurity: Boolean = false
        override val withPayloadMaximization: Boolean = true
    };

    /** Whether MAC optimization is enabled for this mode. / 是否启用重心优化 */
    open val withMacOptimization: Boolean = true
    /** Whether soft security constraints are enabled for this mode. / 是否启用软性安全约束 */
    open val withSoftSecurity: Boolean = true
    /** Whether payload maximization is enabled for this mode. / 是否启用业载最大化 */
    open val withPayloadMaximization: Boolean = false
}
