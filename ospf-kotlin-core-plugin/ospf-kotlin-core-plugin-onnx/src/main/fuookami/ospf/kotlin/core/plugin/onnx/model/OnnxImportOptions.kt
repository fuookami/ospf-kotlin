/**
 * ONNX 导入选项 / ONNX import options
*/
package fuookami.ospf.kotlin.core.plugin.onnx.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 导入模式 / Import mode
 *
 * @property maxDegree 允许的最大表达式次数 / maximum allowed expression degree
*/
enum class OnnxImportMode(val maxDegree: Int) {
    /** 仅允许线性 / linear only */
    Linear(1),

    /** 允许线性与二次 / linear and quadratic allowed */
    Quadratic(2)
}

/**
 * 激活近似配置 / Activation approximation configuration
 *
 * @property segments 分段线性近似的段数，段数越大越精确也越贵 / segment count of the piecewise-linear approximation; more segments are more accurate and more expensive
*/
data class OnnxActivationApproximation(val segments: Int = 8) {

    init {
        require(segments in 2..1024) {
            "分段数必须在 2..1024 之间 / segment count must be within 2..1024"
        }
    }
}

/**
 * 显式输入范围覆盖 / Explicit input bound override
 *
 * 使用代数类型 [Flt64] 而不是原生 `Double`：范围会直接与变量的 `ExpressionRange<Flt64>` 求交，
 * 用代数类型可省掉一次转换，并与模型输出 `Ret<LinearIntermediateSymbol<Flt64>>` 的类型一致。
 * Uses the algebraic type [Flt64] instead of a native `Double`: the bounds intersect directly with a
 * variable's `ExpressionRange<Flt64>`, so the algebraic type removes one conversion and keeps the type
 * consistent with `Ret<LinearIntermediateSymbol<Flt64>>` on the model outputs.
 *
 * @property lower 下界 / lower bound
 * @property upper 上界 / upper bound
*/
data class OnnxBounds(val lower: Flt64, val upper: Flt64) {

    init {
        require(lower.isFinite() && upper.isFinite()) {
            "输入范围必须有限 / input bounds must be finite"
        }
        require(lower <= upper) {
            "输入下界不得大于上界 / input lower bound must not exceed the upper bound"
        }
    }
}

/**
 * 导入选项 / Import options
 *
 * @property mode 导入模式 / import mode
 * @property activationApproximation 激活近似配置 / activation approximation configuration
 * @property inputBounds 按 ONNX 输入名给出的有限范围覆盖 / finite bound overrides keyed by ONNX input name
 * @property inputShapes 按 ONNX 输入名给出的具体形状覆盖，用于把动态维度具体化 / concrete shape overrides keyed by ONNX input name, used to materialize dynamic dimensions
 * @property supportedOpset 支持的主域 opset 区间 / supported main-domain opset range
*/
data class OnnxImportOptions(
    val mode: OnnxImportMode = OnnxImportMode.Linear,
    val activationApproximation: OnnxActivationApproximation = OnnxActivationApproximation(),
    val inputBounds: Map<String, OnnxBounds> = emptyMap(),
    val inputShapes: Map<String, List<Long>> = emptyMap(),
    val supportedOpset: IntRange = 13..24
) {
    init {
        for ((name, shape) in inputShapes) {
            require(shape.all { it > 0 }) {
                "输入 '$name' 的具体形状必须全为正数 / the concrete shape of input '$name' must be entirely positive"
            }
        }
    }
}
