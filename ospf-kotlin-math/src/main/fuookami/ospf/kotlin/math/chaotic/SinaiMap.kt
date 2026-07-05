/*
 * Copyright 2024 Fuookami, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fuookami.ospf.kotlin.math.chaotic

import fuookami.ospf.kotlin.utils.math.arithmetic.*

/**
 * 西奈映射，一种混沌映射。
 * Sinai map, a type of chaotic map.
 *
 * @property r 混沌参数，控制映射的混沌行为。
 * @property r The chaos parameter that controls the chaotic behavior of the map.
 * @property x0 初始值，默认为 0.5。
 * @property x0 The initial value, defaulting to 0.5.
 */
data class SinaiMap(
    val r: Double,
    val x0: Double = 0.5
) : ChaoticMap {
    override fun invoke(times: UInt64): Sequence<Double> {
        return sequence {
            var y = x0
            repeat(times.toInt()) {
                y = r * y * (1.0 - y)
                yield(y)
            }
        }
    }
}
