/**
 * 求解管线
 * Solving Pipeline
 *
 * 定义约束管线、列生成管线和启发式分析管线的接口层次。
 * Defines the interface hierarchy for constraint pipelines, column generation pipelines,
 * and heuristic analysis pipelines.
 */
package fuookami.ospf.kotlin.framework.model

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sum
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.Model
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*

/**
 * 约束管线接口
 * Constraint pipeline interface
 *
 * @param M 模型类型 / Model type
 */
interface Pipeline<in M : Model<*>> : MetaConstraintGroup {
    /**
     * 注册管线到模型
     * Register pipeline to model
     *
     * @param model 目标模型 / Target model
     */
    fun register(model: M) {
        if (model is MetaModel<*>) {
            model.registerConstraintGroup(this)
        }
    }

    /**
     * 执行管线
     * Execute pipeline
     *
     * @param model 目标模型 / Target model
     * @return 操作结果 / Operation result
     */
    operator fun invoke(model: M): Try

    /**
     * 获取线性模型不可行原因
     * Get linear model infeasible reasons
     *
     * @param iis 线性三元模型视图 / Linear triad model view
     * @return 不可行原因列表 / List of infeasible reasons
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("linearInfeasibleReasons")
    fun infeasibleReasons(iis: LinearTriadModelView): List<String> {
        return emptyList()
    }

    /**
     * 获取二次模型不可行原因
     * Get quadratic model infeasible reasons
     *
     * @param iis 二次四元模型视图 / Quadratic tetrad model view
     * @return 不可行原因列表 / List of infeasible reasons
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("quadraticInfeasibleReasons")
    fun infeasibleReasons(iis: QuadraticTetradModelView): List<String> {
        return emptyList()
    }
}

/**
 * 列生成管线接口
 * Column generation pipeline interface
 *
 * @param Args 参数类型 / Argument type
 * @param Model 元模型类型 / Meta model type
 * @param Map 影子价格映射类型 / Shadow price map type
 */
interface CGPipeline<
        in Args : Any,
        in Model : MetaModel<*>,
        in Map : AbstractShadowPriceMap<Args, Map>
        > : Pipeline<Model> {
    companion object {
        /**
         * 按键刷新影子价格
         * Refresh shadow prices by key
         *
         * @param pipeline 列生成管线 / Column generation pipeline
         * @param shadowPriceMap 目标影子价格映射 / Target shadow price map
         * @param model 元模型 / Meta model
         * @param shadowPrices 对偶解 / Dual solution
         * @param Model 元模型类型 / Meta model type
         * @param Map 映射类型 / Map type
         * @return 操作结果 / Operation result
         */
        fun <
                Model : MetaModel<*>,
                Map : AbstractShadowPriceMap<*, Map>
                > refreshByKeyAsArgs(
            pipeline: CGPipeline<*, Model, Map>,
            shadowPriceMap: Map,
            model: Model,
            shadowPrices: MetaDualSolution
        ): Try {
            val thisShadowPrices = HashMap<ShadowPriceKey, Flt64>()
            for (constraint in pipeline.run { model.constraintsOfGroup() }) {
                val key = (constraint.args as? ShadowPriceKey) ?: continue
                shadowPrices.constraints[constraint]?.let { price ->
                    thisShadowPrices[key] = (thisShadowPrices[key] ?: Flt64.zero) + price
                }
            }
            for ((key, value) in thisShadowPrices) {
                shadowPriceMap.put(ShadowPrice(key, value))
            }

            return ok
        }
    }

    /**
     * 获取影子价格提取器
     * Get shadow price extractor
     *
     * @return 影子价格提取器，可能为 null / Shadow price extractor, may be null
     */
    fun extractor(): ShadowPriceExtractor<@UnsafeVariance Args, @UnsafeVariance Map>? {
        return null
    }

    /**
     * 刷新影子价格
     * Refresh shadow prices
     *
     * @param shadowPriceMap 目标影子价格映射 / Target shadow price map
     * @param model 元模型 / Meta model
     * @param shadowPrices 对偶解 / Dual solution
     * @return 操作结果 / Operation result
     */
    fun refresh(shadowPriceMap: Map, model: Model, shadowPrices: MetaDualSolution): Try {
        val thisShadowPrices = this.run { model.constraintsOfGroup() }
            .mapNotNull {
                if (it.args is ShadowPriceKey) {
                    it to (it.args as ShadowPriceKey)
                } else {
                    null
                }
            }
            .groupBy { it.second }
            .mapNotNull { (key, constraints) ->
                val values = constraints.mapNotNull {
                    shadowPrices.constraints[it.first]
                }
                if (values.isNotEmpty()) {
                    key to values.sum()
                } else {
                    null
                }
            }
        for ((key, value) in thisShadowPrices) {
            shadowPriceMap.put(ShadowPrice(key, value))
        }

        return ok
    }
}

/**
 * 启发式分析管线接口
 * Heuristic analysis pipeline interface
 *
 * @param M 模型类型 / Model type
 */
interface HAPipeline<in M : Model<*>> : Pipeline<M> {
    /**
     * 启发式分析目标值
     * Heuristic analysis objective value
     *
     * @property tag 目标标签 / Objective tag
     * @property value 目标值 / Objective value
     */
    data class Obj(
        val tag: String,
        val value: Flt64
    )

    /**
     * 执行管线（默认空操作）
     * Execute pipeline (default no-op)
     *
     * @param model 目标模型 / Target model
     * @return 操作结果 / Operation result
     */
    override operator fun invoke(model: M): Try = ok

    /**
     * 执行启发式分析
     * Execute heuristic analysis
     *
     * @param model 目标模型 / Target model
     * @param solution 解向量 / Solution vector
     * @return 启发式分析目标值 / Heuristic analysis objective value
     */
    operator fun invoke(model: M, solution: List<Flt64>): Ret<Obj> =
        when (val obj = calculate(model, solution)) {
            is Ok -> if (obj.value != null) {
                Ok(Obj(this.name, obj.value!!))
            } else {
                Failed(Err(ErrorCode.ORSolutionInvalid, this.name))
            }

            is Failed -> Failed(obj.error)

            is Fatal -> Fatal(obj.errors)
        }

    /**
     * 计算目标值
     * Calculate objective value
     *
     * @param model 目标模型 / Target model
     * @param solution 解向量 / Solution vector
     * @return 目标值，可能为 null / Objective value, may be null
     */
    fun calculate(model: M, solution: List<Flt64>): Ret<Flt64?>

    /**
     * 检查解的有效性
     * Check solution validity
     *
     * @param model 目标模型 / Target model
     * @param solution 解向量 / Solution vector
     * @return 操作结果 / Operation result
     */
    fun check(model: M, solution: List<Flt64>): Try = when (val obj = calculate(model, solution)) {
        is Ok -> if (obj.value != null) {
            ok
        } else {
            Failed(Err(ErrorCode.ORSolutionInvalid, this.name))
        }

        is Failed -> Failed(obj.error)

        is Fatal -> Fatal(obj.errors)
    }
}

/**
 * 管线列表类型别名
 * Pipeline list type alias
 *
 * @param M 模型类型 / Model type
 */
typealias PipelineList<M> = List<Pipeline<M>>

/**
 * 执行管线列表中的所有管线
 * Execute all pipelines in pipeline list
 *
 * @param model 目标模型 / Target model
 * @param M 模型类型 / Model type
 * @return 操作结果 / Operation result
 */
operator fun <M : Model<*>> PipelineList<M>.invoke(model: M): Try {
    for (pipeline in this) {
        pipeline.register(model)
        when (val ret = pipeline(model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(ret.error)
            }

            is Fatal -> {
                return Fatal(ret.errors)
            }
        }
    }
    return ok
}

/**
 * 列生成管线列表类型别名
 * Column generation pipeline list type alias
 *
 * @param Args 参数类型 / Argument type
 * @param Model 模型类型 / Model type
 * @param Map 映射类型 / Map type
 */
typealias CGPipelineList<Args, Model, Map> = List<CGPipeline<Args, Model, Map>>
/**
 * 启发式分析管线列表类型别名
 * Heuristic analysis pipeline list type alias
 *
 * @param M 模型类型 / Model type
 */
typealias HAPipelineList<M> = List<HAPipeline<M>>

