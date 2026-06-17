# ospf-kotlin-math/chaotic_operator

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 的混沌系统吸引子和迭代映射。提供 88+ 著名混沌系统的实现，用于仿真、可视化和研究。

所有算子类均已泛型化为 `data class Xxx<V : FloatingNumber<V>>`，支持任意浮点类型。生成器类为 `Flt64` 特定，便于使用。

## 可用系统

### 3D 连续系统（微分方程）

| 系统                                     | 参数                                          | 描述                          |
|----------------------------------------|---------------------------------------------|-----------------------------|
| `LorenzSystem`                         | a, b, c, h                                  | 经典洛伦兹吸引子（蝴蝶效应），1963       |
| `LorenzAttractor`                      | sigma, rho, beta, h                         | 洛伦兹吸引子（物理参数命名）            |
| `LorenzMod1Attractor`                  | alpha, beta, delta, zeta, h                 | 洛伦兹修正模型 1                  |
| `LorenzMod2Attractor`                  | alpha, beta, delta, zeta, h                 | 洛伦兹修正模型 2                  |
| `Lorenz84Model`                        | a, b, f, g, h                               | 洛伦兹 1984 大气模型              |
| `ChenSystem`                           | a, b, c, h                                  | 陈氏吸引子，洛伦兹的对偶              |
| `ChenCelikovskyAttractor`              | alpha, beta, delta, h                       | 广义陈氏系统                     |
| `ChenLeeAttractor`                     | alpha, beta, delta, h                       | 陈-李变体                      |
| `CoulletAttractor`                     | alpha, beta, delta, zeta, h                 | 库莱特系统                      |
| `BurkeShawAttractor`                   | zeta, nu, h                                 | 伯克-肖系统                     |
| `BoualiAttractor`                      | alpha, zeta, h, c4, c15, c005               | 布阿里系统                      |
| `AizawaAttractor`                      | alpha, beta, gamma, delta, epsilon, zeta, h | 相泽吸引子                      |
| `AnishchenkoAstakhovAttractor`         | mu, eta, h                                  | 阿尼先科-阿斯塔霍夫系统              |
| `ArneodoAttractor`                     | alpha, beta, delta, h                       | 阿内奥多吸引子                    |
| `ChuaAttractor`                        | alpha, beta, delta, epsilon, zeta, h        | 蔡氏电路吸引子                    |
| `ChuasCircuit`                         | a, b, c, d, h                               | 蔡氏电路（分段线性）                |
| `Brusselator`                          | a, b, h                                     | 布鲁塞尔振子化学模型                |
| `RosslerAttractor`                     | alpha, beta, zeta, h                        | 罗斯勒吸引子，1976                |
| `ThomasAttractor`                      | beta, h                                     | 托马斯吸引子（正弦型）               |
| `ThomasCyclicallySymmetricAttractor`   | b, h                                        | 托马斯循环对称变体                 |
| `NoseHooverAttractor`                  | alpha, h                                    | Nose-Hoover 恒温器             |
| `HalvorsenAttractor`                   | alpha, h                                    | Halvorsen 吸引子（旋转对称）        |
| `HadleyAttractor`                      | alpha, beta, delta, zeta, h                 | Hadley 大气模型                |
| `NewtonLeipnikAttractor`               | alpha, beta, h, c10, c5, c04                | Newton-Leipnik 双涡卷          |
| `RucklidgeAttractor`                   | alpha, kappa, h                             | Rucklidge 流体对流              |
| `ShimizuMoriokaAttractor`              | alpha, beta, h                              | Shimizu-Morioka 激光模型        |
| `RabinovichFabrikantEquation`          | a, b, h                                     | Rabinovich-Fabrikant 等离子波   |
| `DadrasAttractor`                      | gamma, epsilon, zeta, rho, sigma, h         | Dadras 吸引子                  |
| `DequanLiAttractor`                    | alpha, beta, delta, epsilon, zeta, rho, h   | Dequan Li 吸引子               |
| `FinanceAttractor`                     | alpha, beta, zeta, h                        | 金融市场模型                     |
| `GenesioTesiAttractor`                 | alpha, beta, delta, h                       | Genesio-Tesi 系统              |
| `LiuChenAttractor`                     | alpha, beta, delta, epsilon, zeta, xi, rho, h | Liu-Chen 吸引子              |
| `WangSunAttractor`                     | alpha, beta, delta, epsilon, zeta, xi, h    | Wang-Sun 吸引子                |
| `SakaryaAttractor`                     | alpha, beta, h                              | Sakarya 吸引子                 |
| `YuWangAttractor`                      | alpha, beta, delta, zeta, h                 | Yu-Wang 吸引子（指数型）           |
| `WimolBanlueAttractor`                 | alpha, h                                    | Wimol-Banlue 吸引子（正切型）      |
| `RayleighBenardAttractor`              | alpha, beta, gamma, h                       | Rayleigh-Benard 热对流          |
| `QiChenAttractor`                      | alpha, beta, zeta, h                        | Qi-Chen 吸引子（增强洛伦兹）         |
| `LuChenAttractor`                      | alpha, beta, zeta, h                        | Lu-Chen 双涡卷                 |
| `LuChenSystem`                         | a, b, c, d, h                               | Lu-Chen 系统（洛伦兹-陈过渡）        |
| `DuffingEquation`                      | alpha, beta, gamma, delta, omega, h         | 达芬受迫振荡器                    |
| `FourWingAttractor`                    | alpha, beta, delta, zeta, kappa, h          | 四翼吸引子                      |
| `HindmarshRoseModel`                   | a, b, c, d, s, r, xr, i, h                 | Hindmarsh-Rose 神经元模型        |
| `BiologyChaoticModel`                  | a, b, c, r                                  | 生物混沌模型                     |
| `CapacitanceEquation`                  | a, b, c, d, e, h                            | 电容混沌方程                     |
| `CircuitChaotic`                       | a, b, c, d                                  | 电子电路混沌                     |
| `ChuaCircuit`                          | a, b, c, d, h                               | 蔡氏电路模型                     |
| `DoublePendulumSystem`                 | m, l, g, h                                  | 双摆系统                       |
| `CoupledLorenzAttractor`               | beta, gamma1, gamma2, epsilon, omicron, h   | 耦合洛伦兹系统                    |

### 4D 连续系统（超混沌）

| 系统                                    | 参数                        | 描述                    |
|---------------------------------------|---------------------------|-----------------------|
| `QiAttractor`                         | alpha, beta, delta, zeta, h | Qi 超混沌吸引子（4D）       |
| `LorenzStenfloAttractor`              | alpha, beta, delta, zeta, h | Lorenz-Stenflo 系统（4D）|
| `FourScrollHyperChaoticAttractor`     | a, b, c, d, h              | 四涡卷超混沌吸引子（4D）      |

### N 维系统

| 系统                        | 参数              | 描述                  |
|---------------------------|-----------------|---------------------|
| `Lorenz96Model`           | a, h            | Lorenz 96 N 维大气模型   |
| `NBodySystem`             | m, G, h         | N 体引力系统（3D）         |
| `NBodySystemPlane`        | m, G, h         | N 体引力系统（2D）         |
| `LotkaVolterraSystem`     | a, b, c, d, h   | 捕食者-猎物模型            |

### 2D 离散映射

| 系统                           | 参数                    | 描述                    |
|------------------------------|-----------------------|-----------------------|
| `ArnoldsCatMap`              | two                   | 阿诺德猫映射               |
| `BakersMap`                  | -                     | 面包师映射（Flt64，需 floor）  |
| `BogdanovMap`                | epsilon, kappa, mu    | 波格丹诺夫映射              |
| `CircleMap`                  | alpha, beta           | 圆映射                   |
| `ArnoldTongue`               | omega, kappa          | 阿诺德舌可视化              |
| `ChebyshevMap`               | a                     | 切比雪夫多项式映射            |
| `GaussMap`                   | mu                    | 高斯映射                 |
| `DuffingMap`                 | a, b                  | 达芬映射                  |
| `HenonMap`                   | a, b                  | 埃农映射                  |
| `LoziMap`                    | a, b                  | 洛兹映射（分段线性埃农）         |
| `TinkerbellMap`              | a, b, c, d            | 丁克贝尔映射               |
| `IkedaMap`                   | u, t0, t1             | 池田映射（光学混沌）           |
| `GingerbreadmanMap`          | one                   | 姜饼人映射                |
| `MartinIterate`              | a, b, c               | Martin 迭代             |
| `NewtonIterate`              | three, four, twoThirds, two | Newton 分形迭代       |
| `KaplanYorkeMap`             | a, fourPi             | Kaplan-Yorke 映射       |
| `ZaslavskiiMap`              | epsilon, upsilon, r, mu, twoPi | Zaslavskii 映射（等离子体）|
| `SymplecticMap`              | h                     | 辛映射（保面积）             |
| `KickedRotator`              | k                     | 受击转子                  |
| `VanDerPolSystem`            | a, h                  | 范德波尔系统（2D）           |
| `ComplexQuadraticPolynomial` | c, d                  | 复二次映射（Flt64，org.kotlinmath）|
| `ComplexSquaringMap`         | -                     | 复平方映射（Flt64，org.kotlinmath）|

### 1D 映射

| 系统                           | 参数                    | 描述                    |
|------------------------------|-----------------------|-----------------------|
| `LogisticMap`                | a                     | 逻辑斯蒂映射 x → ax(1-x)    |
| `TentMap`                    | mu                    | 帐篷映射（分段线性）           |
| `SineMap`                    | mu                    | 正弦映射                  |
| `ExponentialMap`             | c                     | 指数映射 z → exp(z) + c    |
| `SinusoidalMap`              | mu                    | 正弦平方映射               |
| `SingerMap`                  | mu, c786, c2323, c2875, c1330 | Singer 映射（四次多项式） |
| `GaussIteratedMap`           | a, b                  | 高斯迭代映射               |
| `DyadicTransformation`       | two, one              | 二进制变换 x → 2x mod 1    |
| `IntervalExchangeTransformation` | lambda, pi        | 区间交换变换               |
| `SinusMap`                   | c23, c2               | Sinus 映射               |

### 分形系统（`fractal` 包）

| 系统                           | 参数              | 描述                    |
|------------------------------|-----------------|-----------------------|
| `MandelbrotSet`              | c               | Mandelbrot 集迭代 z → z² + c |
| `JuliaSet`                   | c               | Julia 集迭代 z → z² + c     |
| `MultiJuliaSet`              | c, n            | 多重 Julia 集 z → zⁿ + c    |

### 三涡卷统一混沌系统

| 系统                                          | 参数                              | 描述          |
|---------------------------------------------|---------------------------------|-------------|
| `ThreeScrollUnifiedChaoticSystemTsucs1Attractor` | alpha, beta, delta, epsilon, zeta, rho, h | TSUCS1 变体 |
| `ThreeScrollUnifiedChaoticSystemTsucs2Attractor` | alpha, beta, delta, zeta, rho, h       | TSUCS2 变体 |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.chaotic.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.geometry.point4

// 洛伦兹吸引子（泛型）
val lorenz = LorenzSystem(a = Flt64(10.0), b = Flt64(28.0), c = Flt64(8.0 / 3.0), h = Flt64(0.01))

// 生成轨迹
var state = point3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
val trajectory = mutableListOf(state)
repeat(10000) { state = lorenz(state); trajectory.add(state) }

// 使用 Generator 惰性迭代
val generator = LorenzSystemGenerator(a = Flt64(10.0), b = Flt64(28.0), c = Flt64(8.0 / 3.0), h = Flt64(0.01))
val first100 = List(100) { generator() }

// 4D 超混沌系统
val qi = QiAttractor(alpha = Flt64(30.0), beta = Flt64(10.0), delta = Flt64(10.0), zeta = Flt64(1.0), h = Flt64(0.001))
var state4d = point4(Flt64(1.0), Flt64(1.0), Flt64(1.0), Flt64(1.0))
repeat(1000) { state4d = qi(state4d) }

// 1D 映射
val logistic = LogisticMap(a = Flt64(3.9))
var x = Flt64(0.5)
repeat(100) { x = logistic(x) }

// N 体模拟
val nbody = NBodySystem(m = listOf(Flt64(1.0), Flt64(1.0), Flt64(1.0)))
var bodyState = listOf(
    Pair(point3(Flt64(-1.0), Flt64(0.0), Flt64(0.0)), point3(Flt64(0.0), Flt64(0.5), Flt64(0.0))),
    Pair(point3(Flt64(1.0), Flt64(0.0), Flt64(0.0)), point3(Flt64(0.0), Flt64(-0.5), Flt64(0.0))),
    Pair(point3(Flt64(0.0), Flt64(1.0), Flt64(0.0)), point3(Flt64(0.5), Flt64(0.0), Flt64(0.0)))
)
repeat(1000) { bodyState = nbody(bodyState) }
```

## 架构

- **算子**（无状态）：`data class Xxx<V : FloatingNumber<V>>(...) : Extractor<Output, Input>` — 纯函数，不可变
- **生成器**（有状态）：`data class XxxGenerator(...) : Generator<Output>` — 持有可变状态，Flt64 特定
- **4D 系统**使用 `Point<Dim4, V>`（已添加到 geometry 模块）
- **N 维系统**使用 `List<Flt64>` 作为变长状态向量
- **非标准常量**通过构造参数传入；`companion object` 提供 Flt64 默认值

## 相关链接

- [主 README](../../README.md)
- [Fractal Operator 模块](../fractal/README_ch.md)
- [Geometry 模块](../geometry/README_ch.md)
