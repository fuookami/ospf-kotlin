package fuookami.ospf.kotlin.framework.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

interface Pipeline<M : ModelInterface> {
    val name: String

    fun register(model: M) {
        if (model is MetaModel<*>) {
            model.registerConstraintGroup(name)
        }
    }

    operator fun invoke(model: M): Try<Error>
}

interface CGPipeline<Model : MetaModel<*>, Map : ShadowPriceMap<Map>> : Pipeline<Model> {
    fun extractor(): ShadowPriceExtractor<Map>? {
        return null
    }

    fun refresh(map: Map, model: Model, shadowPrices: List<Flt64>): Try<Error> {
        return Ok(success)
    }
}

interface HAPipeline<M : ModelInterface> : Pipeline<M> {
    data class Obj(
        val tag: String,
        val value: Flt64
    )

    override operator fun invoke(model: M): Try<Error> = Ok(success)

    operator fun invoke(model: M, solution: List<Flt64>): Result<Obj, Error> =
        when (val obj = calculate(model, solution)) {
            is Ok -> if (obj.value != null) {
                Ok(Obj(this.name, obj.value!!))
            } else {
                Failed(Err(ErrorCode.ORSolutionInvalid, this.name))
            }

            is Failed -> Failed(obj.error)
        }

    fun calculate(model: M, solution: List<Flt64>): Result<Flt64?, Error>

    fun check(model: M, solution: List<Flt64>): Try<Error> = when (val obj = calculate(model, solution)) {
        is Ok -> if (obj.value != null) {
            Ok(success)
        } else {
            Failed(Err(ErrorCode.ORSolutionInvalid, this.name))
        }

        is Failed -> Failed(obj.error)
    }
}

typealias PipelineList<M> = List<Pipeline<M>>

operator fun <M : ModelInterface> PipelineList<M>.invoke(model: M): Try<Error> {
    for (pipeline in this) {
        pipeline.register(model)
        when (val ret = pipeline(model)) {
            is Ok -> {}
            is Failed -> {
                return Failed(ret.error)
            }
        }
    }
    return Ok(success)
}

typealias CGPipelineList<Model, Map> = List<CGPipeline<Model, Map>>
typealias HAPipelineList<M> = List<HAPipeline<M>>
