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

function Is-AllowedFile {
    param(
        [string]$FilePath,
        [string[]]$AllowSuffixes
    )

    foreach ($suffix in $AllowSuffixes) {
        if ($FilePath.EndsWith($suffix, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }
    return $false
}

function Add-TokenViolation {
    param(
        [string]$Check,
        [string]$Pattern,
        [string[]]$AllowSuffixes
    )

    $lines = rg -n --no-heading --color never $Pattern $scanRoot -g $sourceGlob -S
    foreach ($line in $lines) {
        $match = [regex]::Match($line, "^([A-Za-z]:.*?):([0-9]+):(.*)$")
        if (-not $match.Success) {
            continue
        }

        $filePath = $match.Groups[1].Value.Replace("\", "/")
        $lineNumber = $match.Groups[2].Value
        $text = $match.Groups[3].Value

        if (-not (Is-AllowedFile -FilePath $filePath -AllowSuffixes $AllowSuffixes)) {
            $script:violations += [PSCustomObject]@{
                Check = $Check
                File = $filePath
                Line = $lineNumber
                Text = $text
            }
        }
    }
}

Add-TokenViolation -Check "CuboidTypeBoundOutOfAllowList" -Pattern "T\s*:\s*Cuboid<T>" -AllowSuffixes @(
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Cuboid.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Placement.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/Projection.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericContainerCore.kt",
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/GenericProjectionPlacementCore.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementFactory.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Bin.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PlacementPlaneMapping.kt",
    "/bpp3d-domain-bla-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/bla/service/BottomUpLeftJustifiedAlgorithm.kt",
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/DemandConstraint.kt",
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/VolumeMinimization.kt"
)

Add-TokenViolation -Check "CuboidWildcardOutOfAllowList" -Pattern "\bCuboid\s*<\s*\*\s*>" -AllowSuffixes @(
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/Item.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/service/LoadingOrderCalculator.kt",
    "/bpp3d-domain-layer-assignment-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/layer_assignment/service/limits/DemandConstraint.kt"
)

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
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/ItemModelAliases.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/model/PackageAttribute.kt",
    "/bpp3d-domain-item-context/src/main/fuookami/ospf/kotlin/framework/bpp3d/domain/item/service/LoadingOrderCalculator.kt"
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
    "/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/QuantityGeometryGeneric.kt",
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

Add-TokenViolation -Check "ApplicationDirectItemLimitFactory" -Pattern "\b(DemandConstraint|VolumeMinimization)\.forItem\s*\(" -AllowSuffixes @()

if ($violations.Count -eq 0) {
    Write-Host "SHAPE_BOUNDARY_PASS"
    exit 0
}

Write-Host "SHAPE_BOUNDARY_FAIL: $($violations.Count)"
$violations |
    Sort-Object Check, File, Line |
    ForEach-Object {
        Write-Host "$($_.Check):$($_.File):$($_.Line):$($_.Text)"
    }

exit 1
