package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Minimizes stowage at positions beside main deck doors to keep door areas clear.
 * 最小化主甲板门旁位置的装载以保持门区畅通。
 *
 * @property items 货物项目列表 / The list of cargo items
 * @property positions 装载位置列表 / The list of stowage positions
 * @property deck 带有门位置的主甲板配置 / The main deck configuration with door ubieties
 * @property stowage 装载分配矩阵 / The stowage assignment matrix
 * @property coefficient 每个项目的惩罚系数函数 / The penalty coefficient function per item
*/
class MainDeckDoorEmptyLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val deck: Deck,
    private val stowage: Stowage,
    private val coefficient: (Item) -> Flt64 = { Flt64.one },
    override val name: String = "main_deck_door_empty_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            sum(items.mapIndexed { i, item ->
                coefficient(item) * sum(positions.mapIndexedNotNull { j, position ->
                    if (deck.ubieties(position.base).any { it.type == DoorUbietyType.Beside }) {
                        stowage.stowage[i, j]
                    } else {
                        null
                    }
                })
            }),
            "main deck door empty"
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}

