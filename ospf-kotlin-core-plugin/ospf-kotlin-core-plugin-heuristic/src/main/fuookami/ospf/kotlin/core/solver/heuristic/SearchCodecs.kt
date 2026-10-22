package fuookami.ospf.kotlin.core.solver.heuristic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*

internal fun <T, R> Ret<T>.then(block: (T) -> Ret<R>): Ret<R> {
    return when (this) {
        is Ok -> block(value)
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}

internal fun <T> searchInvalid(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, "搜索配置或状态无效 / Invalid search configuration or state: $message")
}

internal fun <T> searchFailure(result: Ret<*>): Ret<T> = when (result) {
    is Failed -> Failed(result.error)
    is Fatal -> Fatal(result.errors)
    is Ok -> searchInvalid("expected a failure result")
}

/** 无损 JSON 编码器组合 / Lossless JSON codec combinators. */
object SearchCodecs {
    /** 有限 Flt64 编码，拒绝非法数值 / Finite Flt64 codec rejecting invalid values. */
    val flt64: SearchCodec<Flt64> = object : SearchCodec<Flt64> {
        override fun encode(value: Flt64): Ret<JsonElement> {
            return if (value.toDouble().isFinite()) Ok(JsonPrimitive(value.toString())) else searchInvalid("non-finite value")
        }

        override fun decode(value: JsonElement): Ret<Flt64> {
            val number = (value as? JsonPrimitive)?.content?.toDoubleOrNull()
            return if (number != null && number.isFinite()) Ok(Flt64(number)) else searchInvalid("invalid Flt64")
        }
    }

    /** 由序列化协议创建编码器 / Create a codec from a serializer.
     * @param serializer 值序列化器 / Value serializer
     * @return 编码器 / Codec
     */
    fun <T> serialized(serializer: KSerializer<T>): SearchCodec<T> {
        return object : SearchCodec<T> {
            override fun encode(value: T): Ret<JsonElement> = searchBoundary { Json.encodeToJsonElement(serializer, value) }
            override fun decode(value: JsonElement): Ret<T> = searchBoundary { Json.decodeFromJsonElement(serializer, value) }
        }
    }

    /** 列表编码器 / List codec.
     * @param element 元素编码 / Element codec
     * @return 列表编码 / List codec
     */
    fun <T> list(element: SearchCodec<T>): SearchCodec<List<T>> {
        return object : SearchCodec<List<T>> {
            override fun encode(value: List<T>): Ret<JsonElement> {
                val encoded = ArrayList<JsonElement>()
                for (item in value) {
                    when (val result = searchBoundary { element.encode(item) }.then { it }) {
                        is Ok -> encoded.add(result.value)
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
                return Ok(JsonArray(encoded))
            }

            override fun decode(value: JsonElement): Ret<List<T>> {
                val array = value as? JsonArray ?: return searchInvalid("expected array")
                val decoded = ArrayList<T>()
                for (item in array) {
                    when (val result = searchBoundary { element.decode(item) }.then { it }) {
                        is Ok -> decoded.add(result.value)
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
                return Ok(decoded)
            }
        }
    }

    /** 可空值编码器 / Nullable codec.
     * @param element 元素编码 / Element codec
     * @return 可空编码 / Nullable codec
     */
    fun <T> nullable(element: SearchCodec<T>): SearchCodec<T?> {
        return object : SearchCodec<T?> {
            override fun encode(value: T?): Ret<JsonElement> = if (value == null) Ok(JsonNull) else element.encode(value)
            override fun decode(value: JsonElement): Ret<T?> = if (value == JsonNull) Ok(null) else element.decode(value)
        }
    }

    /** 个体编码器，业务数值不经 Double 中转 / Individual codec without Double conversion.
     * @param problem 问题编码定义 / Problem codecs
     * @return 个体编码 / Individual codec
     */
    fun <O, V> individual(problem: SearchProblem<O, V>): SearchCodec<Individual<O, V>>
        where V : RealNumber<V>, V : NumberField<V> {
        val values = list(problem.valueCodec)
        return object : SearchCodec<Individual<O, V>> {
            override fun encode(value: Individual<O, V>): Ret<JsonElement> {
                return values.encode(value.solution).then { solution ->
                    problem.objectiveCodec.encode(value.fitness).map { objective ->
                        buildJsonObject {
                            put("solution", solution)
                            put("fitness", objective)
                        }
                    }
                }
            }

            override fun decode(value: JsonElement): Ret<Individual<O, V>> {
                val record = value as? JsonObject ?: return searchInvalid("expected individual")
                val solution = record["solution"] ?: return searchInvalid("missing solution")
                val fitness = record["fitness"] ?: return searchInvalid("missing fitness")
                return values.decode(solution).then { decoded ->
                    problem.objectiveCodec.decode(fitness).then { objective ->
                        if (!problem.validSolution(decoded) || !problem.valid(objective)) searchInvalid("invalid individual dimensions or objective")
                        else Ok(SolutionWithFitness(decoded, objective))
                    }
                }
            }
        }
    }
}
