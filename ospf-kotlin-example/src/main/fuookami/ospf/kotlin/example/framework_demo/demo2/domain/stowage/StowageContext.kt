package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage

import fuookami.ospf.kotlin.utils.math.*

/** 堆存上下文，封装贝位、列、排、箱位的尺寸与索引映射 / Stowage context, encapsulating dimensions and index mappings for bays, columns, rows, and slots */
data class StowageContext(
    /** 贝位列表 / List of bays */
    val bays: List<Bay>,
    /** 贝位范围 / Bay boundary range */
    val bayBound: BayBound,
    /** 列数 / Number of columns */
    val columnSize: UInt64,
    /** 排数 / Number of rows */
    val rowSize: UInt64,
    /** 箱位数 / Number of slots */
    val slotSize: UInt64,
) {
    /**
     * 根据一维索引获取贝位 / Get bay by one-dimensional index
     * @param index 一维索引 / One-dimensional index
     * @return 对应的贝位 / The corresponding bay
     */
    fun bay(index: Index1D): Bay {
        return bays[index.toUInt64().toInt()]
    }

    /**
     * 根据二维索引获取列 / Get column by two-dimensional index
     * @param index 二维索引 / Two-dimensional index
     * @return 对应的列 / The corresponding column
     */
    fun column(index: Index2D): Column {
        return Column(
            index = index,
            bay = bay(index[0]),
        )
    }

    /**
     * 根据二维索引获取排 / Get row by two-dimensional index
     * @param index 二维索引 / Two-dimensional index
     * @return 对应的排 / The corresponding row
     */
    fun row(index: Index2D): Row {
        return Row(
            index = index,
            bay = bay(index[0]),
        )
    }

    /**
     * 根据二维索引获取箱位 / Get slot by two-dimensional index
     * @param index 二维索引 / Two-dimensional index
     * @return 对应的箱位 / The corresponding slot
     */
    fun slot(index: Index2D): Slot {
        return Slot(
            index = index,
            bay = bay(index[0]),
        )
    }
}
