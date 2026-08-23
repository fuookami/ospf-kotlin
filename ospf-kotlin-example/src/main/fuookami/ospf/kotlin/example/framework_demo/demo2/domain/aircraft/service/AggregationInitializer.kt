package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Initializes the aircraft aggregation from input request data.
 * 从输入请求数据初始化飞机聚合。
*/
data object AggregationInitializer {

    /**
     * Initialize the aircraft aggregation from the given request data.
     * 从给定的请求数据初始化飞机聚合。
     *
     * @param input 请求输入数据 / The request input data.
    * @return 初始化的聚合结果 / The initialized aggregation result.
    */
    operator fun invoke(input: RequestDTO): Ret<Aggregation> {
        if (input.aircraftType == AircraftTypeInput.Unknown) {
            return Failed(
                ErrorCode.IllegalArgument,
                "未知机型不能初始化飞机聚合 / Unknown aircraft type cannot initialize aircraft aggregation"
            )
        }
        if (input.positions.isEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "飞机至少需要一个可用舱位 / Aircraft requires at least one available position"
            )
        }
        if (input.positions.size > UByte.MAX_VALUE.toInt() + 1) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位数量超过装载顺序索引上限：${input.positions.size} / Position count exceeds loading-order index limit: ${input.positions.size}"
            )
        }
        if (input.positions.any { it.name.isBlank() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位名称不能为空 / Position name must not be blank"
            )
        }
        if (input.positions.map { it.name }.distinct().size != input.positions.size) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位名称必须唯一 / Position names must be unique"
            )
        }
        if (input.positions.any { !it.maxWeight.isFinite() || it.maxWeight <= 0.0 }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位最大载重必须为正且有限 / Position maximum weight must be positive and finite"
            )
        }
        if (input.positions.any { !it.longitudinalArm.isFinite() || !it.lateralArm.isFinite() }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位力臂必须为有限值 / Position arms must be finite"
            )
        }
        if (input.positions.any { !it.length.isFinite() || it.length <= 0.0 || !it.area.isFinite() || it.area <= 0.0 }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "舱位长度和面积必须为正且有限 / Position length and area must be positive and finite"
            )
        }

        val aircraftModel = AircraftModel(AircraftMinorModel(input.aircraftType.name))
        val formula = Formula(
            aircraftModel = aircraftModel,
            lip = Quantity(Flt64.zero, aircraftModel.lengthUnit),
            chord = Quantity(Flt64.one, aircraftModel.lengthUnit),
            standardDatum = Quantity(Flt64.zero, aircraftModel.lengthUnit),
            forceDistanceCoefficient = Flt64.one,
            doiCorrection = Quantity(Flt64.zero, aircraftModel.torqueUnit)
        )
        val fuselage = Fuselage(
            liferaft = null,
            dow = Quantity(Flt64.zero, aircraftModel.weightUnit),
            doi = Quantity(Flt64.zero, aircraftModel.torqueUnit),
            balancedArm = Quantity(Flt64.zero, aircraftModel.lengthUnit)
        )
        val basePositions = input.positions.mapIndexed { index, position ->
            val longitudinalArm = Flt64(position.longitudinalArm)
            val lateralArm = Flt64(position.lateralArm)
            val halfLength = Flt64(position.length / 2.0)
            Position(
                aircraftModel = aircraftModel,
                id = UInt64(index.toULong()),
                spaceName = position.name,
                sizeCode = "N",
                frontArm = Quantity(longitudinalArm - halfLength, aircraftModel.lengthUnit),
                backArm = Quantity(longitudinalArm + halfLength, aircraftModel.lengthUnit),
                leftArm = Quantity(lateralArm - Flt64(0.5), aircraftModel.lengthUnit),
                rightArm = Quantity(lateralArm + Flt64(0.5), aircraftModel.lengthUnit),
                volume = Quantity(Flt64.one, aircraftModel.volumeUnit),
                offsets = HashMap(),
                location = PositionLocation.normalMain,
                linearLoadingOrder = UInt8(index.toUByte()),
                declaredArea = Quantity(Flt64(position.area), aircraftModel.areaUnit)
            )
        }
        for ((index, position) in basePositions.withIndex()) {
            position._loadingOrder = LoadingOrder(
                location = position.location.location,
                order = UInt8(index.toUByte()),
                directPrec = if (index == 0) emptySet() else setOf(basePositions[index - 1]),
                directSucc = if (index == basePositions.lastIndex) emptySet() else setOf(basePositions[index + 1])
            )
        }
        val adjacentNeighbours = basePositions.zipWithNext { first, second ->
            Neighbour(NeighbourType.Physics, first to second)
        }
        val neighbours = hashMapOf<NeighbourType, List<Neighbour>>(
            NeighbourType.Physics to adjacentNeighbours,
            NeighbourType.IndirectPhysics to adjacentNeighbours.map { neighbour ->
                Neighbour(NeighbourType.IndirectPhysics, neighbour.pair)
            },
            NeighbourType.LinearLoadingOrder to adjacentNeighbours.map { neighbour ->
                Neighbour(NeighbourType.LinearLoadingOrder, neighbour.pair)
            },
            NeighbourType.TopologicalLoadingOrder to adjacentNeighbours.map { neighbour ->
                Neighbour(NeighbourType.TopologicalLoadingOrder, neighbour.pair)
            }
        )
        val defaultFuelTank = FuelTank(
            type = FuelTankType.Main,
            name = "DEFAULT",
            maxVolume = Quantity(Flt64.zero, aircraftModel.fuelVolumeUnit),
            balancedArm = FuelTankBalancedArm(emptyList())
        )
        val defaultFuel = FuelConstant(
            density = Quantity(Flt64.zero, aircraftModel.fuelDensityUnit),
            weight = Quantity(Flt64.zero, aircraftModel.weightUnit),
            index = Quantity(Flt64.zero, aircraftModel.torqueUnit)
        )

        return Ok(Aggregation(
            regNo = RegNo("DEFAULT"),
            aircraftModel = aircraftModel,
            formula = formula,
            fuselage = fuselage,
            fuelTanks = Quantity(defaultFuelTank, NoneUnit),
            fuel = FlightPhase.entries.associateWith { defaultFuel },
            decks = listOf(Deck(
                location = DeckLocation.Main,
                doors = emptyList(),
                positions = basePositions,
                doorUbieties = emptyMap()
            )),
            neighbours = neighbours
        ))
    }
}
