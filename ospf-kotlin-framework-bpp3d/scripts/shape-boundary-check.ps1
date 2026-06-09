param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"

$projectPath = (Resolve-Path -Path $ProjectRoot).Path
if ((Test-Path (Join-Path $projectPath "pom.xml")) -and (Test-Path (Join-Path $projectPath "bpp3d-application"))) {
    $scanRoot = $projectPath
} elseif (Test-Path (Join-Path $projectPath "ospf-kotlin-framework-bpp3d")) {
    $scanRoot = Join-Path $projectPath "ospf-kotlin-framework-bpp3d"
} else {
    throw "Cannot locate ospf-kotlin-framework-bpp3d root from '$projectPath'."
}

if (-not (Get-Command rg -ErrorAction SilentlyContinue)) {
    throw "rg not found. Please install ripgrep."
}

$sourceGlob = "**/src/main/**/*.kt"
$violations = @()
$allowlistEntries = @{}
$fixHints = @{
    CuboidTypeBoundOutOfAllowList = "Prefer ItemCuboid, ItemView, ItemPlacement2/3, or a domain-specific alias; keep T : Cuboid<T> only in infrastructure or explicitly classified compatibility factories."
    CuboidWildcardOutOfAllowList = "Replace Cuboid<*> with ItemCuboid or AnyPlacement domain APIs; runtime dispatch should use Any only when the boundary is documented."
    AbstractCuboidOutOfAllowList = "Expose ItemCuboid or PackingShape3 at business boundaries instead of AbstractCuboid<...>."
    CuboidViewOutOfAllowList = "Expose ItemView, BlockView, or BinLayerView aliases instead of raw CuboidView."
    CuboidViewWildcardOutOfAllowList = "Do not expose CuboidView<*> outside infrastructure; use ItemView or typed domain placement aliases."
    CuboidViewReceiverExtensionOutOfAllowList = "Do not add CuboidView receiver extensions in business code; put behavior on ItemView, ItemPlacement, or ItemContainer placement APIs."
    ItemDomainPlacementFactoryCuboidBound = "Keep placement factory Cuboid bounds only inside typed factory internals or the documented BLA generic projection entry."
    ItemDomainInternalBinCuboidBound = "Keep Bin Cuboid bounds behind the internal constructor and typed bin factories."
    QuantityCuboid3OutOfAllowList = "Keep QuantityCuboid3 in infrastructure geometry only; business code should use ItemCuboid or shape-domain APIs."
    QuantityRectangle2OutOfAllowList = "Keep QuantityRectangle2 in infrastructure projection only; business code should use placement/projection domain aliases."
    LayerAssignmentGenericLimitCuboidBound = "Keep generic layer-assignment limit cuboid bounds only behind protected base constructors and item-specific public factories."
    BoundingCuboidOutOfRendererDto = "BoundingCuboid should remain renderer/shape metadata, not a business geometry fallback."
    QuantityPlacementTypeOutOfAllowList = "Use ItemPlacement2/3, BlockPlacement2/3, BinLayerPlacement, or ItemContainerPlacement aliases."
    DirectQuantityPlacementConstructorOutOfFactory = "Create placements through placement2Of/placement3Of or narrower domain factories; do not call QuantityPlacement constructors directly."
    DirectQuantityPlacementConstructorInBusinessTest = "Business tests should create placements through itemPlacement2/3Of, blockPlacement2/3Of, binLayerPlacementOf, or other typed factories; keep direct QuantityPlacement constructor coverage in infrastructure tests only."
    Placement2GenericFactoryUseOutOfAllowList = "Use itemPlacement2Of or blockPlacement2Of outside the BLA generic projection search; do not add new business callers of placement2Of."
    DirectBinConstructorOutOfFactory = "Create bins through layerBinOf, itemBinOf, or blockBinOf; do not call the generic Bin constructor from business code."
    ApplicationDirectItemLimitFactory = "Use item-specific limit factories that hide Cuboid generic constraints from application code."
    DuplicatedCylinderUnsupportedContract = "Keep cylinder unsupported messages in CylinderShapeContract; application and domain services should call the shared item-domain contract instead of duplicating message text."
    CylinderCapabilityPathSourceOutOfContract = "Keep cylinder capability path source and cuboid-only predicate strings in CylinderShapeContract; production callers should pass CylinderCapabilityPath instead of duplicating path metadata."
    DuplicatedPackingGeometryUnsupportedContract = "Keep final packing geometry unsupported messages in PackingGeometryContract; packer, renderer, and analyzer paths should call the shared packing-domain contract instead of duplicating message text."
    KnownCoordinatePlacementBypassOutOfAllowList = "Use toLayerPlacement for generated candidates; toKnownCoordinateLayerPlacement is reserved for already-fixed final coordinates and the generic known-coordinate validation path."
    HorizontalCylinderAxisInGenerationOutOfAllowList = "Keep Axis3.X/Z in layer-generation production code limited to the axis-aware CirclePackingLayerGenerator path with verified footprint, support, and solver coverage."
    HorizontalCylinderGenerationGuardMissing = "CirclePackingLayerGenerator must call requireAxisAwareCylinderCandidate so X/Z cylinders can only enter the verified axis-aware candidate path."
    GeneratedPlacementAdapterGuardMissing = "LayerPlacementAdapter must call requireVerifiedGeneratedCylinderCandidate with ApplicationLayerPlacementCandidate so generated placements cannot bypass axis-aware provenance."
    DfsMlhsCylinderGuardMissing = "DFS/MLHS cuboid-only search entry points must call the shared requireNoCylinderItemsForCuboidOnlyPath contract through the block-loading guard."
    SimpleBlockCylinderGuardMissing = "SimpleBlockGenerator must call requireSupportedCylinderItemForSimpleBlock with SimpleBlockCandidate before generating blocks."
    PackageAttributeSupportGuardMissing = "PackageAttribute.supportPackingShape must call requireUprightVerticalCylinderSupport with PackageAttributeSupport before accepting stacking or hanging support."
    ItemMergeCylinderGuardMissing = "ItemMerger merge variants must call requireNoCylinderItemsForCuboidOnlyPath with their dedicated CylinderCapabilityPath."
    PatternPlacementCylinderGuardMissing = "Pattern placement must reject cylinders through the shared cuboid-only unsupported contract."
    ProgramCandidateShapeSpecGuardMissing = "Layer generation program-demand adapters must preserve PackingProgram.shape.shapeSpec when creating ActualItem instances."
    MaterialPackerShapeSpecGuardMissing = "MaterialPacker must preserve the selected PackingProgram shapeSpec when it emits packaged ActualItem instances."
    GurobiCsvDiscreteRadiusGuardMissing = "Gurobi CSV shape metadata parsing must reject continuous radius/diameter intervals unless a concrete fixed radius is provided."
    GurobiCsvContinuousRadiusKeyGuardMissing = "Gurobi CSV shape metadata parsing must require radius_weight_function_key to carry a concrete selected radius."
    ContinuousCylinderRadiusProductionGuardMissing = "PackageShape.toPackingShapeOrNull must require radiusWeightFunctionKey production use to carry a concrete selected radius."
    ContinuousCylinderRadiusSelectionResultGuardMissing = "Continuous-radius production must expose an explicit selected-radius result object, not just a metadata string."
    ContinuousCylinderRadiusOptimizationGapReportGuardMissing = "Continuous-radius unsupported solver-native paths must be represented by a typed gap report shared by production and CSV guards."
    ContinuousCylinderRadiusSolverPrototypeGuardMissing = "Continuous-radius solver-native work must keep a typed variable prototype wired into CSV guard diagnostics instead of silently fixing interval-only radius metadata."
    ContinuousCylinderRadiusSolverContextGuardMissing = "Column generation solver context must carry continuous-radius solver prototypes into RMP, final MILP, and packing snapshots."
    ContinuousCylinderRadiusLayerGenerationGuardMissing = "CirclePackingLayerGenerator must require concrete radius metadata before generating candidates."
    HorizontalCylinderGeneratedStackSupportGuardMissing = "CirclePackingLayerGenerator must keep horizontal cylinder generated stacking/hanging limited to verified cuboid support candidates with single/multi/heterogeneous axis coverage, 3D geometry, and stacking policy checks."
    HorizontalCylinderStackingSupportGuardMissing = "ItemPlacement3.enabledStackingOn must keep horizontal cylinder automatic support limited to verified floor or cuboid support coverage."
    HorizontalCylinderSupportCoverageSharedGuardMissing = "Item stacking and final packing guards must use the shared horizontal-cylinder cuboid support coverage helper."
    FinalPackingGeometryGuardMissing = "Packer.invoke and PackingRendererAdapter.toSchema must call requirePackedBinShapeGeometry so known-coordinate final packing/rendering cannot bypass real shape geometry checks."
    FinalPackingLayerAxisGuardMissing = "Packer.invoke must call the shared same-layer cylinder axis guard before dumping final bins."
}

function Get-AllowListKey {
    param(
        [string]$Check,
        [string]$Suffix
    )

    return "$Check|$Suffix"
}

function Register-AllowSuffixes {
    param(
        [string]$Check,
        [string[]]$AllowSuffixes
    )

    foreach ($suffix in $AllowSuffixes) {
        $key = Get-AllowListKey -Check $Check -Suffix $suffix
        if (-not $script:allowlistEntries.ContainsKey($key)) {
            $script:allowlistEntries[$key] = [PSCustomObject]@{
                Check = $Check
                Suffix = $suffix
                Hit = $false
            }
        }
    }
}

function Get-MatchedAllowSuffix {
    param(
        [string]$FilePath,
        [string[]]$AllowSuffixes
    )

    foreach ($suffix in $AllowSuffixes) {
        if ($FilePath.EndsWith($suffix, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $suffix
        }
    }
    return $null
}

function Mark-AllowSuffixHit {
    param(
        [string]$Check,
        [string]$Suffix
    )

    $key = Get-AllowListKey -Check $Check -Suffix $Suffix
    if ($script:allowlistEntries.ContainsKey($key)) {
        $script:allowlistEntries[$key].Hit = $true
    }
}

function Add-TokenViolation {
    param(
        [string]$Check,
        [string]$Pattern,
        [string[]]$AllowSuffixes,
        [string[]]$IncludeSuffixes = @(),
        [string[]]$ExcludeSuffixes = @(),
        [string[]]$ScanGlob = @($sourceGlob)
    )

    Register-AllowSuffixes -Check $Check -AllowSuffixes $AllowSuffixes

    $lines = foreach ($glob in $ScanGlob) {
        rg -n --no-heading --color never $Pattern $scanRoot -g $glob -S
    }
    foreach ($line in $lines) {
        $match = [regex]::Match($line, "^([A-Za-z]:.*?):([0-9]+):(.*)$")
        if (-not $match.Success) {
            continue
        }

        $filePath = $match.Groups[1].Value.Replace("\", "/")
        $lineNumber = $match.Groups[2].Value
        $text = $match.Groups[3].Value

        if ($IncludeSuffixes.Count -gt 0 -and (Get-MatchedAllowSuffix -FilePath $filePath -AllowSuffixes $IncludeSuffixes) -eq $null) {
            continue
        }
        if ($ExcludeSuffixes.Count -gt 0 -and (Get-MatchedAllowSuffix -FilePath $filePath -AllowSuffixes $ExcludeSuffixes) -ne $null) {
            continue
        }

        $matchedSuffix = Get-MatchedAllowSuffix -FilePath $filePath -AllowSuffixes $AllowSuffixes
        if ($matchedSuffix -eq $null) {
            $script:violations += [PSCustomObject]@{
                Check = $Check
                File = $filePath
                Line = $lineNumber
                Text = $text
            }
        } else {
            Mark-AllowSuffixHit -Check $Check -Suffix $matchedSuffix
        }
    }
}

function Add-RequiredPatternViolation {
    param(
        [string]$Check,
        [string]$FilePath,
        [string]$Pattern,
        [string]$MissingText
    )

    if (Test-Path $FilePath) {
        $content = Get-Content -Path $FilePath -Raw
        if (-not [regex]::IsMatch($content, $Pattern)) {
            $script:violations += [PSCustomObject]@{
                Check = $Check
                File = $FilePath.Replace("\", "/")
                Line = 1
                Text = $MissingText
            }
        }
    } else {
        $script:violations += [PSCustomObject]@{
            Check = $Check
            File = $FilePath.Replace("\", "/")
            Line = 1
            Text = "Required file was not found, so guard cannot be verified."
        }
    }
}

Add-TokenViolation -Check "CuboidTypeBoundOutOfAllowList" -Pattern "T\s*:\s*Cuboid<T>" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Cuboid.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Placement.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Projection.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericContainerCore.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericProjectionPlacementCore.kt"
) -ExcludeSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementFactory.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Bin.kt",
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/DemandConstraint.kt",
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/VolumeMinimization.kt"
)

Add-TokenViolation -Check "ItemDomainPlacementFactoryCuboidBound" -Pattern "T\s*:\s*Cuboid<T>" -AllowSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementFactory.kt"
) -IncludeSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementFactory.kt"
)

Add-TokenViolation -Check "Placement2GenericFactoryUseOutOfAllowList" -Pattern "\bplacement2Of\s*\(" -AllowSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementFactory.kt",
    "/bpp3d-domain-bla-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/bla/service/BottomUpLeftJustifiedAlgorithm.kt"
)

Add-TokenViolation -Check "ItemDomainInternalBinCuboidBound" -Pattern "T\s*:\s*Cuboid<T>" -AllowSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Bin.kt"
) -IncludeSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Bin.kt"
)

Add-TokenViolation -Check "LayerAssignmentGenericLimitCuboidBound" -Pattern "T\s*:\s*Cuboid<T>" -AllowSuffixes @(
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/DemandConstraint.kt",
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/VolumeMinimization.kt"
) -IncludeSuffixes @(
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/DemandConstraint.kt",
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/VolumeMinimization.kt"
)

Add-TokenViolation -Check "CuboidWildcardOutOfAllowList" -Pattern "\bCuboid\s*<\s*\*\s*>" -AllowSuffixes @()

Add-TokenViolation -Check "AbstractCuboidOutOfAllowList" -Pattern "\bAbstractCuboid\s*<" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Cuboid.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Orientation.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Projection.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Container.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericContainerCore.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericProjectionPlacementCore.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/PackingShape.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/ShadowPriceMap.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/ItemModelAliases.kt"
)

Add-TokenViolation -Check "CuboidViewOutOfAllowList" -Pattern "\bCuboidView\b" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Cuboid.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Placement.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Projection.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementFactory.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Item.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Block.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Layer.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/ItemContainer.kt"
)

Add-TokenViolation -Check "CuboidViewWildcardOutOfAllowList" -Pattern "\bCuboidView\s*<\s*\*\s*>" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Cuboid.kt"
)

Add-TokenViolation -Check "CuboidViewReceiverExtensionOutOfAllowList" -Pattern "\b(fun|val)\s*<[^>]+>\s+CuboidView\s*<" -AllowSuffixes @()

Add-TokenViolation -Check "QuantityCuboid3OutOfAllowList" -Pattern "\bQuantityCuboid3\b" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Cuboid.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Placement.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericContainerCore.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericProjectionPlacementCore.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/ProjectivePlaneGeometryMapping.kt"
)

Add-TokenViolation -Check "QuantityRectangle2OutOfAllowList" -Pattern "\bQuantityRectangle2\b" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Placement.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/QuantityGeometrySpike.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericProjectionPlacementCore.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/ProjectivePlaneGeometryMapping.kt"
)

Add-TokenViolation -Check "BoundingCuboidOutOfRendererDto" -Pattern "\bBoundingCuboid\b" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/dto/RendererDTO.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/PackingShape.kt",
    "/bpp3d-domain-packing-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/packing/service/PackingRendererAdapter.kt"
)

Add-TokenViolation -Check "QuantityPlacementTypeOutOfAllowList" -Pattern "\bQuantityPlacement[23]\s*<" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Cuboid.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Placement.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Projection.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Container.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericProjectionPlacementCore.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Item.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/ItemContainer.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Block.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Layer.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementFactory.kt"
)

Add-TokenViolation -Check "DirectQuantityPlacementConstructorOutOfFactory" -Pattern "\bQuantityPlacement[23]\s*\(" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Placement.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Projection.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementFactory.kt"
)

Add-TokenViolation -Check "DirectQuantityPlacementConstructorInBusinessTest" -Pattern "\bQuantityPlacement[23]\s*\(" -AllowSuffixes @() -ScanGlob @(
    "bpp3d-domain-*/src/test/**/*.kt",
    "bpp3d-application/src/test/**/*.kt",
    "bpp3d-application/src/gurobi-test/**/*.kt"
)

Add-TokenViolation -Check "DirectBinConstructorOutOfFactory" -Pattern "\bBin\s*\(" -AllowSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Bin.kt"
) -ScanGlob "**/src/**/*.kt"

Add-TokenViolation -Check "ApplicationDirectItemLimitFactory" -Pattern "\b(DemandConstraint|VolumeMinimization)\.forItem\s*\(" -AllowSuffixes @()

Add-TokenViolation -Check "DuplicatedCylinderUnsupportedContract" -Pattern "Unsupported cylinder axis|Unsupported cylinder orientation|Unsupported cylinder top-layer|Unsupported cylinder stacking and hanging|Unsupported coordinate-less cylinder stacking and hanging|Unsupported continuous cylinder radius optimization|Unsupported cylinder in|only Axis3\.Y is allowed|only upright orientations are allowed|side/lie stacking is not allowed|only upright Axis3\.Y items are allowed|horizontal cylinders require verified 3D placement support coverage|radiusWeightFunctionKey requires a concrete selected radius result|cuboid-only and does not provide verified cylinder geometry yet" -AllowSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/CylinderShapeContract.kt"
)

Add-TokenViolation -Check "CylinderCapabilityPathSourceOutOfContract" -Pattern "source\s*=\s*""(LayerPlacementAdapter\.toLayerPlacement|CirclePackingLayerGenerator|PileLayerGenerator|PackageAttribute\.supportPackingShape|SimpleBlockGenerator|DFS/MLHS|ItemMerger\.merge|ItemMerger\.mergePiles|ItemMerger\.mergeBlocks|ItemMerger\.mergePatternBlocks|ItemMerger\.mergeHollowSquareBlocks|Pattern|known-coordinate final packing|renderer final packing|DepthBoundaryLayerOrientationPolicy)""|pathPredicate\s*=\s*""(DFS/MLHS space-splitting path is|item merge paths are|pattern placement paths are)""" -AllowSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/CylinderShapeContract.kt"
)

Add-TokenViolation -Check "DuplicatedPackingGeometryUnsupportedContract" -Pattern "type=horizontal_support|type=outside_bin|type=overlap|must be placed on bin floor or cuboid support coverage|is outside bin|overlaps item|mixes cylinder axes" -AllowSuffixes @(
    "/bpp3d-domain-packing-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/packing/service/PackingGeometryContract.kt"
)

Add-TokenViolation -Check "KnownCoordinatePlacementBypassOutOfAllowList" -Pattern "\.toKnownCoordinateLayerPlacement\s*\(" -AllowSuffixes @(
    "/bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/LayerPlacementAdapter.kt",
    "/bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/ColumnGenerationPackingAnalyzer.kt"
)

Add-TokenViolation -Check "HorizontalCylinderAxisInGenerationOutOfAllowList" -Pattern "Axis3\.(X|Z)" -AllowSuffixes @() -ScanGlob @(
    "bpp3d-domain-layer-generation-context/src/main/**/*.kt"
)

$layerGenerationContextPath = Join-Path $scanRoot "bpp3d-domain-layer-generation-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_generation/LayerGenerationContext.kt"
if (Test-Path $layerGenerationContextPath) {
    $layerGenerationContext = Get-Content -Path $layerGenerationContextPath -Raw
    if (-not [regex]::IsMatch($layerGenerationContext, "class\s+CirclePackingLayerGenerator[\s\S]*?requireAxisAwareCylinderCandidate\s*\(")) {
        $violations += [PSCustomObject]@{
            Check = "HorizontalCylinderGenerationGuardMissing"
            File = $layerGenerationContextPath.Replace("\", "/")
            Line = 1
            Text = "CirclePackingLayerGenerator must call requireAxisAwareCylinderCandidate before generating candidates."
        }
    }
} else {
    $violations += [PSCustomObject]@{
        Check = "HorizontalCylinderGenerationGuardMissing"
        File = $layerGenerationContextPath.Replace("\", "/")
        Line = 1
        Text = "LayerGenerationContext.kt was not found, so CirclePackingLayerGenerator guard cannot be verified."
    }
}

$layerPlacementAdapterPath = Join-Path $scanRoot "bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/LayerPlacementAdapter.kt"
Add-RequiredPatternViolation `
    -Check "GeneratedPlacementAdapterGuardMissing" `
    -FilePath $layerPlacementAdapterPath `
    -Pattern "requireVerifiedGeneratedCylinderCandidate\s*\([\s\S]*?CylinderCapabilityPath\.ApplicationLayerPlacementCandidate" `
    -MissingText "LayerPlacementAdapter.toLayerPlacement must use the shared verified generated candidate guard."

$cylinderUnsupportedGuardPath = Join-Path $scanRoot "bpp3d-domain-block-loading-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/block_loading/service/CylinderUnsupportedGuard.kt"
Add-RequiredPatternViolation `
    -Check "DfsMlhsCylinderGuardMissing" `
    -FilePath $cylinderUnsupportedGuardPath `
    -Pattern "requireNoCylinderItemsForCuboidOnlyPath\s*\([\s\S]*?CylinderCapabilityPath\.DfsMlhsCuboidSearch" `
    -MissingText "CylinderUnsupportedGuard must route DFS/MLHS cylinder rejection through the shared cuboid-only contract."

$simpleBlockGeneratorPath = Join-Path $scanRoot "bpp3d-domain-block-loading-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/block_loading/service/SimpleBlockGenerator.kt"
Add-RequiredPatternViolation `
    -Check "SimpleBlockCylinderGuardMissing" `
    -FilePath $simpleBlockGeneratorPath `
    -Pattern "requireSupportedCylinderItemForSimpleBlock\s*\([\s\S]*?CylinderCapabilityPath\.SimpleBlockCandidate" `
    -MissingText "SimpleBlockGenerator must use the shared simple-block cylinder contract."

$packageAttributePath = Join-Path $scanRoot "bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PackageAttribute.kt"
Add-RequiredPatternViolation `
    -Check "PackageAttributeSupportGuardMissing" `
    -FilePath $packageAttributePath `
    -Pattern "requireUprightVerticalCylinderSupport\s*\([\s\S]*?CylinderCapabilityPath\.PackageAttributeSupport" `
    -MissingText "PackageAttribute.supportPackingShape must use the shared upright vertical cylinder support contract."

$itemMergerPath = Join-Path $scanRoot "bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/service/ItemMerger.kt"
foreach ($mergePath in @(
    "ItemMerge",
    "ItemMergePiles",
    "ItemMergeBlocks",
    "ItemMergePatternBlocks",
    "ItemMergeHollowSquareBlocks"
)) {
    Add-RequiredPatternViolation `
        -Check "ItemMergeCylinderGuardMissing" `
        -FilePath $itemMergerPath `
        -Pattern "requireNoCylinderItemsForCuboidOnlyPath\s*\([\s\S]*?CylinderCapabilityPath\.$mergePath" `
        -MissingText "ItemMerger must use the shared cuboid-only contract for CylinderCapabilityPath.$mergePath."
}

$patternPath = Join-Path $scanRoot "bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Pattern.kt"
Add-RequiredPatternViolation `
    -Check "PatternPlacementCylinderGuardMissing" `
    -FilePath $patternPath `
    -Pattern "unsupportedCylinderCuboidOnlyPathMessage\s*\([\s\S]*?CylinderCapabilityPath\.PatternPlacement" `
    -MissingText "Pattern placement must use the shared cuboid-only unsupported contract."

$programCandidateAdapterPath = Join-Path $scanRoot "bpp3d-domain-layer-generation-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_generation/LayerGenerationProgramCandidateAdapters.kt"
Add-RequiredPatternViolation `
    -Check "ProgramCandidateShapeSpecGuardMissing" `
    -FilePath $programCandidateAdapterPath `
    -Pattern "shapeSpecOverride\s*=\s*program\.shape\.shapeSpec" `
    -MissingText "LayerGenerationProgramCandidateAdapters must copy PackingProgram.shape.shapeSpec into generated ActualItem."

$materialPackerPath = Join-Path $scanRoot "bpp3d-domain-packing-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/packing/service/MaterialPacker.kt"
Add-RequiredPatternViolation `
    -Check "MaterialPackerShapeSpecGuardMissing" `
    -FilePath $materialPackerPath `
    -Pattern "shapeSpecOverride\s*=\s*pack\.shape\.shapeSpec" `
    -MissingText "MaterialPacker must copy the selected package shapeSpec into emitted ActualItem."

$gurobiColumnGenerationTestPath = Join-Path $scanRoot "bpp3d-application/src/gurobi-test/fuookami/ospf/kotlin/framework/bpp3d/application/service/GurobiColumnGenerationTest.kt"
Add-RequiredPatternViolation `
    -Check "GurobiCsvDiscreteRadiusGuardMissing" `
    -FilePath $gurobiColumnGenerationTestPath `
    -Pattern "requireConcreteCsvRadiusMetadata\s*\([\s\S]*?continuousCylinderRadiusOptimizationGapReport\s*\([\s\S]*?hasContinuousRadiusInterval\s*=[\s\S]*?radiusStepMeter\s*==\s*null[\s\S]*?hasContinuousDiameterInterval\s*=[\s\S]*?diameterStepMeter\s*==\s*null" `
    -MissingText "GurobiColumnGenerationTest CSV parser must reject continuous radius/diameter intervals through the typed continuous-radius gap report."

Add-RequiredPatternViolation `
    -Check "GurobiCsvContinuousRadiusKeyGuardMissing" `
    -FilePath $gurobiColumnGenerationTestPath `
    -Pattern "radius_weight_function_key[\s\S]*?PackageShapeSpec\.VerticalCylinder\s*\([\s\S]*?radiusWeightFunctionKey\s*=\s*radiusWeightFunctionKey[\s\S]*?continuousCylinderRadiusOptimizationGapReport\s*\([\s\S]*?source\s*=\s*""Gurobi CSV""[\s\S]*?hasConcreteSelectedRadius\s*=\s*radiusMeter\s*!=\s*null[\s\S]*?hasDiscreteRadiusStep" `
    -MissingText "GurobiColumnGenerationTest CSV parser must require radius_weight_function_key to carry radius_meter through the typed continuous-radius gap report and pass it into the shape spec."

$packagePath = Join-Path $scanRoot "bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Package.kt"
Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusProductionGuardMissing" `
    -FilePath $packagePath `
    -Pattern "requireConcreteCylinderRadiusProductionMetadata\s*\([\s\S]*?source\s*=\s*""PackageShape\.toPackingShapeOrNull""" `
    -MissingText "PackageShape.toPackingShapeOrNull must require a concrete selected radius before emitting production PackingShape3."

$cylinderShapeContractPath = Join-Path $scanRoot "bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/CylinderShapeContract.kt"
Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSelectionResultGuardMissing" `
    -FilePath $cylinderShapeContractPath `
    -Pattern "data\s+class\s+CylinderRadiusSelectionResult[\s\S]*?selectedRadius[\s\S]*?fun\s+PackageShapeSpec\.VerticalCylinder\.continuousRadiusSelectionResult\s*\([\s\S]*?CylinderRadiusSelectionResult[\s\S]*?requireConcreteCylinderRadiusProductionMetadata\s*\([\s\S]*?continuousRadiusSelectionResult" `
    -MissingText "CylinderShapeContract must keep selected continuous-radius results behind a typed result object used by the production guard."

Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusOptimizationGapReportGuardMissing" `
    -FilePath $cylinderShapeContractPath `
    -Pattern "enum\s+class\s+ContinuousCylinderRadiusOptimizationGap[\s\S]*?SolverNativeRadiusIntervalUnsupported[\s\S]*?SolverNativeDiameterIntervalUnsupported[\s\S]*?data\s+class\s+ContinuousCylinderRadiusOptimizationGapReport[\s\S]*?fun\s+continuousCylinderRadiusOptimizationGapReport\s*\([\s\S]*?MissingSelectedRadius[\s\S]*?DiscreteRadiusMetadataConflict" `
    -MissingText "CylinderShapeContract must expose a typed continuous-radius optimization gap report for solver-native unsupported paths."

Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSolverPrototypeGuardMissing" `
    -FilePath $cylinderShapeContractPath `
    -Pattern "data\s+class\s+ContinuousCylinderRadiusSolverPrototype[\s\S]*?variableName[\s\S]*?isProductionReady[\s\S]*?fun\s+continuousCylinderRadiusSolverPrototype\s*\([\s\S]*?continuousCylinderRadiusOptimizationGapReport[\s\S]*?fun\s+PackageShapeSpec\.VerticalCylinder\.continuousRadiusSolverPrototype" `
    -MissingText "CylinderShapeContract must keep a typed solver-native continuous-radius variable prototype and expose it from VerticalCylinder metadata."

Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSolverPrototypeGuardMissing" `
    -FilePath $gurobiColumnGenerationTestPath `
    -Pattern "requireConcreteCsvRadiusMetadata\s*\([\s\S]*?continuousCylinderRadiusSolverPrototype\s*\([\s\S]*?axis\s*=\s*axis[\s\S]*?gapReport\.message\s*\(rowDescription\)\s*\+\s*\(solverPrototype\?\.messageSuffix\(\)" `
    -MissingText "Gurobi CSV radius guard must include the typed continuous-radius solver prototype in interval-only/key-without-radius diagnostics."

$columnGenerationAlgorithmPath = Join-Path $scanRoot "bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/ColumnGenerationAlgorithm.kt"
Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSolverContextGuardMissing" `
    -FilePath $columnGenerationAlgorithmPath `
    -Pattern "data\s+class\s+ColumnGenerationState[\s\S]*?continuousRadiusSolverPrototypes[\s\S]*?fun\s+continuousRadiusSolverPrototypesFromItems\s*\([\s\S]*?continuousRadiusSolverPrototype[\s\S]*?val\s+continuousRadiusSolverPrototypes\s*=\s*continuousRadiusSolverPrototypesFromItems\s*\(items\)[\s\S]*?ColumnGenerationState[\s\S]*?continuousRadiusSolverPrototypes\s*=\s*continuousRadiusSolverPrototypes" `
    -MissingText "ColumnGenerationAlgorithm must extract continuous-radius solver prototypes from items and propagate them through ColumnGenerationState."

$columnGenerationStandardExecutorsPath = Join-Path $scanRoot "bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/ColumnGenerationStandardExecutors.kt"
Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSolverContextGuardMissing" `
    -FilePath $columnGenerationStandardExecutorsPath `
    -Pattern "continuous_radius_solver_prototype_count[\s\S]*?state\.continuousRadiusSolverPrototypes\.size[\s\S]*?continuous_radius_solver_prototype_variables[\s\S]*?state\.continuousRadiusSolverPrototypes\.joinToString[\s\S]*?continuous_radius_solver_prototype_count[\s\S]*?state\.continuousRadiusSolverPrototypes\.size[\s\S]*?continuous_radius_solver_prototype_variables[\s\S]*?state\.continuousRadiusSolverPrototypes\.joinToString" `
    -MissingText "ColumnGenerationStandardExecutors must expose continuous-radius solver prototype count and variables in both RMP and final MILP solve info."

$continuousRadiusSolverRegistrationPlanPath = Join-Path $scanRoot "bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/ContinuousRadiusSolverRegistrationPlan.kt"
Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSolverRegistrationPlanGuardMissing" `
    -FilePath $continuousRadiusSolverRegistrationPlanPath `
    -Pattern "data\s+class\s+ContinuousRadiusSolverVariableRegistrationPlan[\s\S]*?core token-bound support[\s\S]*?continuous_radius_solver_registration_plan_count[\s\S]*?continuous_radius_solver_registration_plan_variables[\s\S]*?continuous_radius_solver_registration_plan_bounds[\s\S]*?continuous_radius_solver_model_registration_blocked_variables[\s\S]*?continuous_radius_solver_model_registration_blocked_reason[\s\S]*?fun\s+continuousRadiusSolverVariableRegistrationPlan\s*\([\s\S]*?registrationBoundDescription" `
    -MissingText "ContinuousRadiusSolverRegistrationPlan must expose the continuous-radius solver registration plan and blocked model-registration reason without adding unsupported symbolic radius variables to the solver model."

Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSolverFinalRegistrationPlanUsageMissing" `
    -FilePath $columnGenerationStandardExecutorsPath `
    -Pattern "val\s+continuousRadiusVariablePlan\s*=\s*continuousRadiusSolverVariableRegistrationPlan\s*\([\s\S]*?state\.continuousRadiusSolverPrototypes[\s\S]*?continuousRadiusVariablePlan\.info\s*\(\)" `
    -MissingText "ColumnGenerationStandardExecutors must use the shared continuous-radius solver registration plan in final MILP solve info."

Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSolverRmpRegistrationPlanUsageMissing" `
    -FilePath $columnGenerationStandardExecutorsPath `
    -Pattern "artifacts\.continuousRadiusVariablePlan\.info\s*\(\)[\s\S]*?val\s+continuousRadiusVariablePlan:\s*ContinuousRadiusSolverVariableRegistrationPlan[\s\S]*?continuousRadiusSolverVariableRegistrationPlan\s*\([\s\S]*?state\.continuousRadiusSolverPrototypes" `
    -MissingText "ColumnGenerationStandardExecutors must use the shared continuous-radius solver registration plan in RMP solve info."

$columnGenerationPackingAnalyzerPath = Join-Path $scanRoot "bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/ColumnGenerationPackingAnalyzer.kt"
Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusSolverContextGuardMissing" `
    -FilePath $columnGenerationPackingAnalyzerPath `
    -Pattern "schemaKpi\[""continuous_radius_solver_prototype_count""\]\s*=\s*state\.continuousRadiusSolverPrototypes\.size\.toString\(\)[\s\S]*?schemaKpi\[""continuous_radius_solver_prototype_variables""\]\s*=\s*state\.continuousRadiusSolverPrototypes\.joinToString[\s\S]*?continuousRadiusSolverVariableRegistrationPlan\s*\([\s\S]*?state\.continuousRadiusSolverPrototypes[\s\S]*?\.info\s*\(\)" `
    -MissingText "ColumnGenerationPackingAnalyzer must carry continuous-radius solver prototype context and registration plan into renderer schema KPI diagnostics."

$layerGenerationContextPath = Join-Path $scanRoot "bpp3d-domain-layer-generation-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_generation/LayerGenerationContext.kt"
Add-RequiredPatternViolation `
    -Check "ContinuousCylinderRadiusLayerGenerationGuardMissing" `
    -FilePath $layerGenerationContextPath `
    -Pattern "requireConcreteCirclePackingRadiusMetadata\s*\([\s\S]*?requireConcreteCylinderRadiusProductionMetadata\s*\([\s\S]*?source\s*=\s*CylinderCapabilityPath\.CirclePackingCandidate\.source[\s\S]*?class\s+CirclePackingLayerGenerator[\s\S]*?requireConcreteCirclePackingRadiusMetadata\s*\([\s\S]*?requireAxisAwareCylinderCandidate" `
    -MissingText "CirclePackingLayerGenerator must require concrete radius metadata through the shared contract before reading item.packingShape."

Add-RequiredPatternViolation `
    -Check "HorizontalCylinderGeneratedStackSupportGuardMissing" `
    -FilePath $layerGenerationContextPath `
    -Pattern "horizontalCylinderSupportedStackCandidates\s*\([\s\S]*?canFullySupportHorizontalCylinder\s*\([\s\S]*?horizontalCylinderSingleHangingSupportPlacements\s*\([\s\S]*?circle-packing-horizontal-hanging-support[\s\S]*?horizontalCylinderRepeatedHangingSupportCount\s*\([\s\S]*?circle-packing-horizontal-hanging-support-multi[\s\S]*?horizontalCylinderRepeatedSupportCount\s*\([\s\S]*?horizontalCylinderHeterogeneousSupportPlacements\s*\([\s\S]*?circlePackingStackedLayerIsGeometryValid\s*\([\s\S]*?enabledStackingOn\s*\([\s\S]*?circle-packing-horizontal-supported-stack-heterogeneous" `
    -MissingText "CirclePackingLayerGenerator must keep generated horizontal cylinder stacking/hanging behind single/multi hanging/multi stack/heterogeneous support coverage, 3D geometry, and stacking-policy checks."

$itemPath = Join-Path $scanRoot "bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Item.kt"
Add-RequiredPatternViolation `
    -Check "HorizontalCylinderStackingSupportGuardMissing" `
    -FilePath $itemPath `
    -Pattern "hasHorizontalCylinderStackingSupportCoverage\s*\([\s\S]*?CylinderPackingShape3[\s\S]*?horizontalCylinderCuboidSupportCoverage[\s\S]*?axis\s*!=\s*Axis3\.Y" `
    -MissingText "ItemPlacement3.enabledStackingOn must keep horizontal cylinder support behind the support-coverage guard."

$supportCoveragePath = Join-Path $scanRoot "bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/HorizontalCylinderSupportCoverage.kt"
Add-RequiredPatternViolation `
    -Check "HorizontalCylinderSupportCoverageSharedGuardMissing" `
    -FilePath $supportCoveragePath `
    -Pattern "data\s+class\s+HorizontalCylinderSupportGeometry[\s\S]*?fun\s+horizontalCylinderCuboidSupportCoverage\s*\([\s\S]*?isCylinder[\s\S]*?intervalsCoverSpan" `
    -MissingText "Horizontal cylinder support coverage must remain centralized in infrastructure."

$packingGeometryGuardPath = Join-Path $scanRoot "bpp3d-domain-packing-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/packing/service/PackingGeometryGuard.kt"
Add-RequiredPatternViolation `
    -Check "HorizontalCylinderSupportCoverageSharedGuardMissing" `
    -FilePath $packingGeometryGuardPath `
    -Pattern "hasHorizontalCylinderSupportCoverage\s*\([\s\S]*?CylinderPackingShape3[\s\S]*?horizontalCylinderCuboidSupportCoverage" `
    -MissingText "Final packing horizontal cylinder support guard must call the shared support coverage helper."

$packerPath = Join-Path $scanRoot "bpp3d-domain-packing-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/packing/service/Packer.kt"
if (Test-Path $packerPath) {
    $packer = Get-Content -Path $packerPath -Raw
    if (-not [regex]::IsMatch($packer, "requireSingleCylinderAxisPerLayer\s*\([\s\S]*?source\s*=\s*""Packer\.invoke""[\s\S]*?\)")) {
        $violations += [PSCustomObject]@{
            Check = "FinalPackingLayerAxisGuardMissing"
            File = $packerPath.Replace("\", "/")
            Line = 1
            Text = "Packer.invoke must call requireSingleCylinderAxisPerLayer before building final packed bins."
        }
    }
    if (-not [regex]::IsMatch($packer, "requirePackedBinShapeGeometry\s*\([\s\S]*?source\s*=\s*""Packer\.invoke""[\s\S]*?\)")) {
        $violations += [PSCustomObject]@{
            Check = "FinalPackingGeometryGuardMissing"
            File = $packerPath.Replace("\", "/")
            Line = 1
            Text = "Packer.invoke must call requirePackedBinShapeGeometry before returning final packed bins."
        }
    }
} else {
    $violations += [PSCustomObject]@{
        Check = "FinalPackingGeometryGuardMissing"
        File = $packerPath.Replace("\", "/")
        Line = 1
        Text = "Packer.kt was not found, so final packing geometry guard cannot be verified."
    }
}

$rendererAdapterPath = Join-Path $scanRoot "bpp3d-domain-packing-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/packing/service/PackingRendererAdapter.kt"
if (Test-Path $rendererAdapterPath) {
    $rendererAdapter = Get-Content -Path $rendererAdapterPath -Raw
    if (-not [regex]::IsMatch($rendererAdapter, "requirePackedBinShapeGeometry\s*\([\s\S]*?source\s*=\s*""PackingRendererAdapter\.toSchema""[\s\S]*?\)")) {
        $violations += [PSCustomObject]@{
            Check = "FinalPackingGeometryGuardMissing"
            File = $rendererAdapterPath.Replace("\", "/")
            Line = 1
            Text = "PackingRendererAdapter.toSchema must call requirePackedBinShapeGeometry before emitting renderer DTO."
        }
    }
} else {
    $violations += [PSCustomObject]@{
        Check = "FinalPackingGeometryGuardMissing"
        File = $rendererAdapterPath.Replace("\", "/")
        Line = 1
        Text = "PackingRendererAdapter.kt was not found, so renderer geometry guard cannot be verified."
    }
}

foreach ($entry in $allowlistEntries.Values) {
    if (-not $entry.Hit) {
        $violations += [PSCustomObject]@{
            Check = "StaleAllowListEntry"
            File = $entry.Suffix
            Line = 1
            Text = "allowlist suffix has no current match for $($entry.Check)"
        }
    }
}

if ($violations.Count -eq 0) {
    Write-Host "SHAPE_BOUNDARY_PASS"
    exit 0
}

Write-Host "SHAPE_BOUNDARY_FAIL: $($violations.Count)"
$violations |
    Sort-Object Check, File, Line |
    ForEach-Object {
        $hint = $fixHints[$_.Check]
        if ([string]::IsNullOrWhiteSpace($hint)) {
            Write-Host "$($_.Check):$($_.File):$($_.Line):$($_.Text)"
        } else {
            Write-Host "$($_.Check):$($_.File):$($_.Line):$($_.Text) | Hint: $hint"
        }
    }

exit 1
