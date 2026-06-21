package fuookami.ospf.kotlin.quantities

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.unit.Byte as InfoByte
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.dimension.*

class NewUnitsConversionTest {
    // ========================================================================
    // 信息量二进制前缀单位转换测试
    // Information binary prefix units conversion tests
    // ========================================================================

    @Test
    fun `information_KibibyteShouldEqual1024Byte`() {
        val oneKibibyte = Flt64.one * Kibibyte
        val inByte = oneKibibyte.to(InfoByte)!!
        assertEquals(1024.0, inByte.value.toDouble(), 1e-10)
    }

    @Test
    fun `information_KibibyteShouldEqual8192Bit`() {
        val oneKibibyte = Flt64.one * Kibibyte
        val inBit = oneKibibyte.to(Bit)!!
        assertEquals(8192.0, inBit.value.toDouble(), 1e-10)
    }

    @Test
    fun `information_MebibyteShouldEqual1024Kibibyte`() {
        val oneMebibyte = Flt64.one * Mebibyte
        val inKibibyte = oneMebibyte.to(Kibibyte)!!
        assertEquals(1024.0, inKibibyte.value.toDouble(), 1e-10)
    }

    @Test
    fun `information_MebibyteShouldEqual1048576Byte`() {
        val oneMebibyte = Flt64.one * Mebibyte
        val inByte = oneMebibyte.to(InfoByte)!!
        assertEquals(1048576.0, inByte.value.toDouble(), 1e-6)
    }

    @Test
    fun `information_GibibyteShouldEqual1024Mebibyte`() {
        val oneGibibyte = Flt64.one * Gibibyte
        val inMebibyte = oneGibibyte.to(Mebibyte)!!
        assertEquals(1024.0, inMebibyte.value.toDouble(), 1e-10)
    }

    @Test
    fun `information_GibibyteShouldEqual1073741824Byte`() {
        val oneGibibyte = Flt64.one * Gibibyte
        val inByte = oneGibibyte.to(InfoByte)!!
        assertEquals(1073741824.0, inByte.value.toDouble(), 1.0)
    }

    @Test
    fun `information_TebibyteShouldEqual1024Gibibyte`() {
        val oneTebibyte = Flt64.one * Tebibyte
        val inGibibyte = oneTebibyte.to(Gibibyte)!!
        assertEquals(1024.0, inGibibyte.value.toDouble(), 1e-10)
    }

    @Test
    fun `information_TebibyteShouldEqual1099511627776Byte`() {
        val oneTebibyte = Flt64.one * Tebibyte
        val inByte = oneTebibyte.to(InfoByte)!!
        assertEquals(1099511627776.0, inByte.value.toDouble(), 1.0)
    }

    @Test
    fun `information_binaryPrefixUnitsDomainShouldBeContinuous`() {
        assertEquals(QuantityDomain.Continuous, Kibibyte.domain)
        assertEquals(QuantityDomain.Continuous, Mebibyte.domain)
        assertEquals(QuantityDomain.Continuous, Gibibyte.domain)
        assertEquals(QuantityDomain.Continuous, Tebibyte.domain)
    }

    @Test
    fun `information_bitAndByteDomainShouldBeDiscrete`() {
        assertEquals(QuantityDomain.Discrete, Bit.domain)
        assertEquals(QuantityDomain.Discrete, InfoByte.domain)
    }

    // ========================================================================
    // 带宽字节单位转换测试
    // Bandwidth byte units conversion tests
    // ========================================================================

    @Test
    fun `bandwidth_bytePerSecondShouldEqual8BitPerSecond`() {
        val oneBytePerSecond = Flt64.one * BytePerSecond
        val inBitPerSecond = oneBytePerSecond.to(BitPerSecond)!!
        assertEquals(8.0, inBitPerSecond.value.toDouble(), 1e-10)
    }

    @Test
    fun `bandwidth_kilobytePerSecondShouldEqual1000BytePerSecond`() {
        val oneKilobytePerSecond = Flt64.one * KilobytePerSecond
        val inBytePerSecond = oneKilobytePerSecond.to(BytePerSecond)!!
        assertEquals(1000.0, inBytePerSecond.value.toDouble(), 1e-10)
    }

    @Test
    fun `bandwidth_kilobytePerSecondShouldEqual8000BitPerSecond`() {
        val oneKilobytePerSecond = Flt64.one * KilobytePerSecond
        val inBitPerSecond = oneKilobytePerSecond.to(BitPerSecond)!!
        assertEquals(8000.0, inBitPerSecond.value.toDouble(), 1e-10)
    }

    @Test
    fun `bandwidth_megabytePerSecondShouldEqual1000KilobytePerSecond`() {
        val oneMegabytePerSecond = Flt64.one * MegabytePerSecond
        val inKilobytePerSecond = oneMegabytePerSecond.to(KilobytePerSecond)!!
        assertEquals(1000.0, inKilobytePerSecond.value.toDouble(), 1e-10)
    }

    // ========================================================================
    // 长度单位转换测试
    // Length units conversion tests
    // ========================================================================

    @Test
    fun `length_decameterShouldEqual10Meter`() {
        val oneDecameter = Flt64.one * Decameter
        val inMeter = oneDecameter.to(Meter)!!
        assertEquals(10.0, inMeter.value.toDouble(), 1e-10)
    }

    // ========================================================================
    // 体积单位转换测试
    // Volume units conversion tests
    // ========================================================================

    @Test
    fun `volume_cubicDecameterShouldEqual1000CubicMeter`() {
        val oneCubicDecameter = Flt64.one * CubicDecameter
        val inCubicMeter = oneCubicDecameter.to(CubicMeter)!!
        assertEquals(1000.0, inCubicMeter.value.toDouble(), 1e-10)
    }

    @Test
    fun `volume_cubicHectometerShouldEqual1e6CubicMeter`() {
        val oneCubicHectometer = Flt64.one * CubicHectometer
        val inCubicMeter = oneCubicHectometer.to(CubicMeter)!!
        assertEquals(1000000.0, inCubicMeter.value.toDouble(), 1e-6)
    }

    @Test
    fun `volume_cubicKilometerShouldEqual1e9CubicMeter`() {
        val oneCubicKilometer = Flt64.one * CubicKilometer
        val inCubicMeter = oneCubicKilometer.to(CubicMeter)!!
        assertEquals(1000000000.0, inCubicMeter.value.toDouble(), 1.0)
    }

    // ========================================================================
    // 流量单位转换测试
    // Flow rate units conversion tests
    // ========================================================================

    @Test
    fun `flowRate_literPerMinuteShouldEqual1Over60LiterPerSecond`() {
        val oneLiterPerMinute = Flt64.one * LiterPerMinute
        val inLiterPerSecond = oneLiterPerMinute.to(LiterPerSecond)!!
        assertEquals(1.0 / 60.0, inLiterPerSecond.value.toDouble(), 1e-10)
    }

    // ========================================================================
    // 力单位转换测试
    // Force units conversion tests
    // ========================================================================

    @Test
    fun `force_kiloNewtonShouldEqual1000Newton`() {
        val oneKilonewton = Flt64.one * Kilonewton
        val inNewton = oneKilonewton.to(Newton)!!
        assertEquals(1000.0, inNewton.value.toDouble(), 1e-10)
    }

    @Test
    fun `force_megaNewtonShouldEqual1e6Newton`() {
        val oneMeganewton = Flt64.one * Meganewton
        val inNewton = oneMeganewton.to(Newton)!!
        assertEquals(1000000.0, inNewton.value.toDouble(), 1e-6)
    }

    @Test
    fun `force_megaNewtonShouldEqual1000Kilonewton`() {
        val oneMeganewton = Flt64.one * Meganewton
        val inKilonewton = oneMeganewton.to(Kilonewton)!!
        assertEquals(1000.0, inKilonewton.value.toDouble(), 1e-10)
    }

    // ========================================================================
    // 应力单位转换测试
    // Stress units conversion tests
    // ========================================================================

    @Test
    fun `stress_pascalStressShouldConvertToPsi`() {
        val onePascalStress = Flt64.one * PascalStress
        val inPsi = onePascalStress.to(PoundForcePerSquareInch)!!
        assertEquals(1.0 / 6894.757, inPsi.value.toDouble(), 1e-6)
    }

    @Test
    fun `stress_kilopascalStressShouldEqual1000PascalStress`() {
        val oneKilopascalStress = Flt64.one * KilopascalStress
        val inPascalStress = oneKilopascalStress.to(PascalStress)!!
        assertEquals(1000.0, inPascalStress.value.toDouble(), 1e-10)
    }

    @Test
    fun `stress_megapascalStressShouldEqual1e6PascalStress`() {
        val oneMegapascalStress = Flt64.one * MegapascalStress
        val inPascalStress = oneMegapascalStress.to(PascalStress)!!
        assertEquals(1000000.0, inPascalStress.value.toDouble(), 1e-6)
    }

    // ========================================================================
    // 电阻单位转换测试
    // Resistance units conversion tests
    // ========================================================================

    @Test
    fun `resistance_megaohmShouldEqual1e6Ohm`() {
        val oneMegaohm = Flt64.one * Megaohm
        val inOhm = oneMegaohm.to(Ohm)!!
        assertEquals(1000000.0, inOhm.value.toDouble(), 1e-6)
    }

    @Test
    fun `resistance_megaohmShouldEqual1000Kiloohm`() {
        val oneMegaohm = Flt64.one * Megaohm
        val inKiloohm = oneMegaohm.to(Kiloohm)!!
        assertEquals(1000.0, inKiloohm.value.toDouble(), 1e-10)
    }

    // ========================================================================
    // 质量单位转换测试
    // Mass units conversion tests
    // ========================================================================

    @Test
    fun `mass_hectogramShouldEqual100Gram`() {
        val oneHectogram = Flt64.one * Hectogram
        val inGram = oneHectogram.to(Gram)!!
        assertEquals(100.0, inGram.value.toDouble(), 1e-10)
    }

    @Test
    fun `mass_hectogramShouldEqual0_1Kilogram`() {
        val oneHectogram = Flt64.one * Hectogram
        val inKilogram = oneHectogram.to(Kilogram)!!
        assertEquals(0.1, inKilogram.value.toDouble(), 1e-10)
    }

    // ========================================================================
    // 兼容别名转换测试
    // Compatibility alias conversion tests
    // ========================================================================

    @Test
    fun `alias_minuteAngleShouldEqualArcMinute`() {
        val oneMinuteAngle = Flt64.one * MinuteAngle
        val oneArcMinute = Flt64.one * ArcMinute
        assert(oneMinuteAngle eq oneArcMinute)
    }

    @Test
    fun `alias_secondAngleShouldEqualArcSecond`() {
        val oneSecondAngle = Flt64.one * SecondAngle
        val oneArcSecond = Flt64.one * ArcSecond
        assert(oneSecondAngle eq oneArcSecond)
    }

    @Test
    fun `alias_meterPerSquareSecondShouldEqualMeterPerSecondSquared`() {
        val oneMeterPerSquareSecond = Flt64.one * MeterPerSquareSecond
        val oneMeterPerSecondSquared = Flt64.one * MeterPerSecondSquared
        assert(oneMeterPerSquareSecond eq oneMeterPerSecondSquared)
    }

    @Test
    fun `alias_centimeterPerSquareSecondShouldEqualCentimeterPerSecondSquared`() {
        val oneCentimeterPerSquareSecond = Flt64.one * CentimeterPerSquareSecond
        val oneCentimeterPerSecondSquared = Flt64.one * CentimeterPerSecondSquared
        assert(oneCentimeterPerSquareSecond eq oneCentimeterPerSecondSquared)
    }

    @Test
    fun `alias_kilometerPerSquareSecondShouldEqualKilometerPerSecondSquared`() {
        val oneKilometerPerSquareSecond = Flt64.one * KilometerPerSquareSecond
        val oneKilometerPerSecondSquared = Flt64.one * KilometerPerSecondSquared
        assert(oneKilometerPerSquareSecond eq oneKilometerPerSecondSquared)
    }

    @Test
    fun `alias_inchPerSquareSecondShouldEqualInchPerSecondSquared`() {
        val oneInchPerSquareSecond = Flt64.one * InchPerSquareSecond
        val oneInchPerSecondSquared = Flt64.one * InchPerSecondSquared
        assert(oneInchPerSquareSecond eq oneInchPerSecondSquared)
    }

    @Test
    fun `alias_footPerSquareSecondShouldEqualFootPerSecondSquared`() {
        val oneFootPerSquareSecond = Flt64.one * FootPerSquareSecond
        val oneFootPerSecondSquared = Flt64.one * FootPerSecondSquared
        assert(oneFootPerSquareSecond eq oneFootPerSecondSquared)
    }

    @Test
    fun `alias_kilometersPerSecondShouldEqualKilometerPerSecond`() {
        val oneKilometersPerSecond = Flt64.one * KilometersPerSecond
        val oneKilometerPerSecond = Flt64.one * KilometerPerSecond
        assert(oneKilometersPerSecond eq oneKilometerPerSecond)
    }

    @Test
    fun `alias_tonneShouldEqualTon`() {
        val oneTonne = Flt64.one * Tonne
        val oneTon = Flt64.one * Ton
        assert(oneTonne eq oneTon)
    }

    // ========================================================================
    // 新增单位加减乘除测试
    // New units arithmetic tests
    // ========================================================================

    @Test
    fun `arithmetic_kibibyteAdditionShouldWork`() {
        val twoKiB = Flt64(2.0) * Kibibyte
        val threeKiB = Flt64(3.0) * Kibibyte
        val sum = (twoKiB + threeKiB).orFail()
        assertEquals(5.0, sum.value.toDouble(), 1e-10)
        val inByte = sum.to(InfoByte)!!
        assertEquals(5120.0, inByte.value.toDouble(), 1e-10)
    }

    @Test
    fun `arithmetic_forceUnitConversionAndDivisionShouldWork`() {
        // 2 kN = 2000 N; 2000 N / 2 N = 1000 (dimensionless)
        val twoKilonewton = Flt64(2.0) * Kilonewton
        val inNewton = twoKilonewton.to(Newton)!!
        val twoNewton = Flt64(2.0) * Newton
        val ratio = (inNewton / twoNewton).orFail()
        assertEquals(1000.0, ratio.value.toDouble(), 1e-10)
    }

    @Test
    fun `arithmetic_stressAdditionShouldWork`() {
        val twoKPa = Flt64(2.0) * KilopascalStress
        val threeKPa = Flt64(3.0) * KilopascalStress
        val sum = (twoKPa + threeKPa).orFail()
        assertEquals(5.0, sum.value.toDouble(), 1e-10)
        val inPa = sum.to(PascalStress)!!
        assertEquals(5000.0, inPa.value.toDouble(), 1e-10)
    }
}
