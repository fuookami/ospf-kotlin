package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.heuristic.*

@Serializable
internal data class NativeState(
    val individuals: JsonElement = JsonArray(emptyList()),
    val vectors: List<List<Double>> = emptyList(),
    val counters: List<Int> = emptyList(),
    val solutions: JsonElement = JsonArray(emptyList()),
    val children: List<NativeState> = emptyList()
)

/** 各骨架的原生状态编码，不序列化闭包 / Native skeleton codecs; closures are never serialized. */
object RunnerStateCodecs {
    private val record = SearchCodecs.serialized(NativeState.serializer())

    private fun <S> native(
        encode: (S) -> Ret<NativeState>,
        decode: (NativeState) -> Ret<S>
    ): SearchCodec<S> {
        val writer = encode
        val reader = decode
        return object : SearchCodec<S> {
            override fun encode(value: S): Ret<JsonElement> = writer(value).then { record.encode(it) }
            override fun decode(value: JsonElement): Ret<S> = record.decode(value).then(reader)
        }
    }

    /** 种群状态编码 / Population state codec.
     * @param problem 问题编码定义 / Problem codecs
     * @return 编码器 / Codec
     */
    fun <O, V> population(problem: SearchProblem<O, V>): SearchCodec<List<Individual<O, V>>>
        where V : RealNumber<V>, V : NumberField<V> = SearchCodecs.list(SearchCodecs.individual(problem))

    /** 岛模型的嵌套种群状态编码 / Nested population-state codec for island runners.
     * @param problem 问题编码定义 / Problem codecs
     * @return 编码器 / Codec
     */
    fun <O, V> islands(problem: SearchProblem<O, V>): SearchCodec<List<List<Individual<O, V>>>>
        where V : RealNumber<V>, V : NumberField<V> = SearchCodecs.list(population(problem))

    /** 单解状态编码 / Trajectory state codec.
     * @param problem 问题编码定义 / Problem codecs
     * @return 编码器 / Codec
     */
    fun <O, V> trajectory(problem: SearchProblem<O, V>): SearchCodec<Individual<O, V>?>
        where V : RealNumber<V>, V : NumberField<V> = SearchCodecs.nullable(SearchCodecs.individual(problem))

    /** 粒子状态编码 / Particle state codec.
     * @param problem 问题编码定义 / Problem codecs
     * @return 编码器 / Codec
     */
    fun <O, V> particles(problem: SearchProblem<O, V>): SearchCodec<List<ParticleState<O, V>>>
        where V : RealNumber<V>, V : NumberField<V> {
        val entries = population(problem)
        return native(
            encode = { state ->
                entries.encode(state.flatMap { listOf(it.individual, it.best) }).map { values ->
                    NativeState(individuals = values, vectors = state.map { it.velocity.map { v -> v.toDouble() } })
                }
            },
            decode = { saved ->
                entries.decode(saved.individuals).then { values ->
                    if (values.size != saved.vectors.size * 2 || saved.vectors.indices.any { saved.vectors[it].size != values[it * 2].solution.size } || saved.vectors.any { row -> row.any { !it.isFinite() } }) searchInvalid("invalid particles")
                    else Ok(saved.vectors.indices.map { index -> ParticleState(
                        individual = values[index * 2],
                        velocity = saved.vectors[index].map { Flt64(it) },
                        best = values[index * 2 + 1]
                    ) })
                }
            }
        )
    }

    /** CLPSO 状态编码 / CLPSO state codec.
     * @param problem 问题编码定义 / Problem codecs
     * @return 编码器 / Codec
     */
    fun <O, V> clpso(problem: SearchProblem<O, V>): SearchCodec<List<ClpsoParticle<O, V>>>
        where V : RealNumber<V>, V : NumberField<V> {
        val entries = population(problem)
        return native(
            encode = { state ->
                entries.encode(state.flatMap { listOf(it.individual, it.best) }).map { values ->
                    NativeState(
                        individuals = values,
                        vectors = state.map { it.velocity.map { v -> v.toDouble() } },
                        children = state.map { NativeState(counters = listOf(it.stagnation) + it.exemplars) }
                    )
                }
            },
            decode = { saved ->
                entries.decode(saved.individuals).then { values ->
                    if (values.size != saved.vectors.size * 2 || saved.children.size != saved.vectors.size ||
                        saved.children.indices.any { index ->
                            val counters = saved.children[index].counters
                            counters.size != values[index * 2].solution.size + 1 || counters.firstOrNull()?.let { it < 0 } == true ||
                                counters.drop(1).any { it !in saved.vectors.indices } || saved.vectors[index].size != values[index * 2].solution.size
                        } || saved.vectors.any { row -> row.any { !it.isFinite() } }) searchInvalid("invalid CLPSO state")
                    else Ok(saved.vectors.indices.map { index ->
                        ClpsoParticle(
                            individual = values[index * 2],
                            velocity = saved.vectors[index].map { Flt64(it) },
                            best = values[index * 2 + 1],
                            exemplars = saved.children[index].counters.drop(1),
                            stagnation = saved.children[index].counters.first()
                        )
                    })
                }
            }
        )
    }

    /** SHADE 成功历史及外部档案编码 / SHADE history and external archive codec.
     * @param problem 问题编码定义 / Problem codecs
     * @return 编码器 / Codec
     */
    fun <O, V> shade(problem: SearchProblem<O, V>): SearchCodec<ShadeState<O, V>>
        where V : RealNumber<V>, V : NumberField<V> {
        val entries = population(problem)
        val solutions = SearchCodecs.list(SearchCodecs.list(problem.valueCodec))
        return native(
            encode = { state ->
                entries.encode(state.population).then { population ->
                    solutions.encode(state.archive).map { archive ->
                        NativeState(
                            individuals = population,
                            vectors = listOf(state.memory.memoryF, state.memory.memoryCr),
                            counters = listOf(state.memory.cursor),
                            solutions = archive
                        )
                    }
                }
            },
            decode = { saved ->
                if (saved.vectors.size != 2 || saved.vectors[0].isEmpty() || saved.vectors[0].size != saved.vectors[1].size ||
                    saved.counters.size != 1 || saved.counters[0] !in saved.vectors[0].indices ||
                    saved.vectors.any { row -> row.any { !it.isFinite() || it !in 0.0..1.0 } }) searchInvalid("invalid SHADE memory")
                else entries.decode(saved.individuals).then { population ->
                    solutions.decode(saved.solutions).then { archive ->
                        if (archive.any { !problem.validSolution(it) }) searchInvalid("invalid SHADE archive dimensions")
                        else Ok(ShadeState(
                            population = population,
                            memory = ShadeMemory(
                            memoryF = saved.vectors[0],
                            memoryCr = saved.vectors[1],
                            cursor = saved.counters[0]
                        ),
                            archive = archive
                        ))
                    }
                }
            }
        )
    }

    /** CMA-ES 分布与演化路径编码 / CMA-ES distribution and evolution path codec.
     * @param problem 问题编码定义 / Problem codecs
     * @return 编码器 / Codec
     */
    fun <O, V> cmaEs(problem: SearchProblem<O, V>): SearchCodec<CmaEsState<O, V>>
        where V : RealNumber<V>, V : NumberField<V> {
        val best = trajectory(problem)
        return native(
            encode = { state ->
                best.encode(state.best).map { entry ->
                    NativeState(
                        individuals = entry,
                        vectors = listOf(state.mean, listOf(state.sigma), state.pathSigma, state.pathCovariance) + state.covariance,
                        counters = listOf(state.generation)
                    )
                }
            },
            decode = { saved ->
                val vectors = saved.vectors
                if (vectors.size < 4 || vectors[0].size != problem.model.tokens.tokensInSolver.size || vectors[1].size != 1 || vectors[1][0] <= 0.0 || saved.counters.size != 1 || saved.counters[0] < 0 ||
                    vectors.any { row -> row.any { !it.isFinite() } } || vectors[2].size != vectors[0].size ||
                    vectors[3].size != vectors[0].size || vectors.drop(4).size != vectors[0].size ||
                    vectors.drop(4).any { it.size != vectors[0].size }) searchInvalid("invalid CMA-ES distribution")
                else best.decode(saved.individuals).map { entry ->
                    CmaEsState(
                        mean = vectors[0],
                        sigma = vectors[1][0],
                        covariance = vectors.drop(4),
                        pathSigma = vectors[2],
                        pathCovariance = vectors[3],
                        generation = saved.counters[0],
                        best = entry
                    )
                }
            }
        )
    }
}
