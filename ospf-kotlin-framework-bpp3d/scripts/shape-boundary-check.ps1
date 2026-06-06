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
    CuboidViewOutOfAllowList = "Expose ItemView, BlockView, BinLayerView, or PalletLayerView aliases instead of raw CuboidView."
    CuboidViewWildcardOutOfAllowList = "Do not expose CuboidView<*> outside infrastructure; use ItemView or typed domain placement aliases."
    CuboidViewReceiverExtensionOutOfAllowList = "Do not add CuboidView receiver extensions in business code; put behavior on ItemView, ItemPlacement, or ItemContainer placement APIs."
    ItemDomainPlacementFactoryCuboidBound = "Keep placement factory Cuboid bounds only inside typed factory internals or the documented BLA generic projection entry."
    ItemDomainInternalBinCuboidBound = "Keep Bin Cuboid bounds behind the internal constructor and typed bin factories."
    QuantityCuboid3OutOfAllowList = "Keep QuantityCuboid3 in infrastructure geometry only; business code should use ItemCuboid or shape-domain APIs."
    QuantityRectangle2OutOfAllowList = "Keep QuantityRectangle2 in infrastructure projection only; business code should use placement/projection domain aliases."
    LayerAssignmentGenericLimitCuboidBound = "Keep generic layer-assignment limit cuboid bounds only behind protected base constructors and item-specific public factories."
    BoundingCuboidOutOfRendererDto = "BoundingCuboid should remain renderer/shape metadata, not a business geometry fallback."
    QuantityPlacementTypeOutOfAllowList = "Use ItemPlacement2/3, BlockPlacement2/3, BinLayerPlacement, PalletLayerPlacement, or ItemContainerPlacement aliases."
    DirectQuantityPlacementConstructorOutOfFactory = "Create placements through placement2Of/placement3Of or narrower domain factories; do not call QuantityPlacement constructors directly."
    DirectQuantityPlacementConstructorInBusinessTest = "Business tests should create placements through itemPlacement2/3Of, blockPlacement2/3Of, binLayerPlacementOf, or other typed factories; keep direct QuantityPlacement constructor coverage in infrastructure tests only."
    DirectBinConstructorOutOfFactory = "Create bins through layerBinOf, itemBinOf, or blockBinOf; do not call the generic Bin constructor from business code."
    ApplicationDirectItemLimitFactory = "Use item-specific limit factories that hide Cuboid generic constraints from application code."
    DuplicatedCylinderUnsupportedContract = "Keep cylinder unsupported messages in CylinderShapeContract; application and domain services should call the shared item-domain contract instead of duplicating message text."
    KnownCoordinatePlacementBypassOutOfAllowList = "Use toLayerPlacement for generated candidates; toKnownCoordinateLayerPlacement is reserved for already-fixed final coordinates and the generic known-coordinate validation path."
    HorizontalCylinderAxisInGenerationOutOfAllowList = "Do not introduce Axis3.X or Axis3.Z in layer-generation production code until horizontal-cylinder generation has a proven footprint, support, and solver contract."
    HorizontalCylinderGenerationGuardMissing = "CirclePackingLayerGenerator must keep requireVerticalCylinderAxis so X/Z cylinders cannot reuse the Axis3.Y circle-packing plane assumption."
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
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/InfraAliases.kt",
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

Add-TokenViolation -Check "DuplicatedCylinderUnsupportedContract" -Pattern "Unsupported cylinder axis|Unsupported cylinder orientation|Unsupported cylinder top-layer|Unsupported cylinder stacking and hanging|Unsupported cylinder in|only Axis3\.Y is allowed|only upright orientations are allowed|side/lie stacking is not allowed|only upright Axis3\.Y items are allowed|cuboid-only and does not provide verified cylinder geometry yet" -AllowSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/CylinderShapeContract.kt"
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
    if (-not [regex]::IsMatch($layerGenerationContext, "class\s+CirclePackingLayerGenerator[\s\S]*?requireVerticalCylinderAxis\s*\(")) {
        $violations += [PSCustomObject]@{
            Check = "HorizontalCylinderGenerationGuardMissing"
            File = $layerGenerationContextPath.Replace("\", "/")
            Line = 1
            Text = "CirclePackingLayerGenerator must call requireVerticalCylinderAxis before generating candidates."
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
