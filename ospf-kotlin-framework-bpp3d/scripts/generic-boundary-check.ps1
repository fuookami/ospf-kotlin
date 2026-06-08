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

$contentChecks = @(
    @{
        Name = "LegacyUpperAny"
        Pattern = "Legacy"
    },
    @{
        Name = "LegacyLowerAny"
        Pattern = "legacy"
    },
    @{
        Name = "LegacyQuantity"
        Pattern = "\bLegacyQuantity\b"
    },
    @{
        Name = "toLegacyModel"
        Pattern = "toLegacyModel\("
    },
    @{
        Name = "toLegacyItems"
        Pattern = "toLegacyItems\("
    },
    @{
        Name = "toLegacyLayers"
        Pattern = "toLegacyLayers\("
    },
    @{
        Name = "toLegacyPlacement3"
        Pattern = "toLegacyPlacement3\("
    },
    @{
        Name = "toLegacy"
        Pattern = "toLegacy\("
    },
    @{
        Name = "toFlt64Quantity"
        Pattern = "toFlt64Quantity\("
    },
    @{
        Name = "asScalarF64"
        Pattern = "asScalarF64\("
    },
    @{
        Name = "toFlt64"
        Pattern = "toFlt64\("
    },
    @{
        Name = "QuantityFlt64"
        Pattern = "QuantityFlt64"
    },
    @{
        Name = "Flt64Token"
        Pattern = "Flt64"
    },
    @{
        Name = "LegacyScalarToken"
        Pattern = "\bLegacyScalar\b"
    },
    @{
        Name = "InfraScalarToken"
        Pattern = "\bInfraScalar\b"
    },
    @{
        Name = "ItemModelScalarToken"
        Pattern = "\bItemModelScalar\b"
    },
    @{
        Name = "LayerAssignmentScalarToken"
        Pattern = "\bLayerAssignmentScalar\b"
    },
    @{
        Name = "ApplicationScalarToken"
        Pattern = "\bApplicationScalar\b"
    },
    @{
        Name = "PackingScalarToken"
        Pattern = "\bPackingScalar\b"
    },
    @{
        Name = "BlockLoadingScalarToken"
        Pattern = "\bBlockLoadingScalar\b"
    },
    @{
        Name = "Bpp3dBlaScalarToken"
        Pattern = "\bBpp3dBlaScalar\b"
    },
    @{
        Name = "LegacyDemandSlicesToken"
        Pattern = "\bLegacyDemandSlices\b"
    },
    @{
        Name = "LegacyCuboidToken"
        Pattern = "\bLegacyCuboid\b"
    },
    @{
        Name = "LegacyCuboidGenericAdapterToken"
        Pattern = "\bLegacyCuboidGenericAdapter\b"
    },
    @{
        Name = "ItemDemandShadowPriceKeyCompatAlias"
        Pattern = "\bItemDemandShadowPriceKey\b"
    },
    @{
        Name = "Bpp3dDemandValueAdapterCompatAlias"
        Pattern = "\bBpp3dDemandValueAdapter\b"
    },
    @{
        Name = "DefaultBpp3dDemandValueAdapterCompatAlias"
        Pattern = "\bDefaultBpp3dDemandValueAdapter\b"
    },
    @{
        Name = "InfraBpp3dDemandEntryCompatAlias"
        Pattern = "\bInfraBpp3dDemandEntry\b"
    },
    @{
        Name = "InfraBpp3dItemDemandCompatAlias"
        Pattern = "\bInfraBpp3dItemDemand\b"
    },
    @{
        Name = "InfraBpp3dMaterialDemandCompatAlias"
        Pattern = "\bInfraBpp3dMaterialDemand\b"
    },
    @{
        Name = "InfraLoadCompatAlias"
        Pattern = "\bInfraLoad\b"
    },
    @{
        Name = "BPP3DShadowPriceExtractorCompatAlias"
        Pattern = "\bBPP3DShadowPriceExtractor\b"
    },
    @{
        Name = "BPP3DCGPipelineCompatAlias"
        Pattern = "\bBPP3DCGPipeline\b"
    },
    @{
        Name = "BPP3DCGPipelineListCompatAlias"
        Pattern = "\bBPP3DCGPipelineList\b"
    },
    @{
        Name = "GenericBPP3DShadowPriceExtractorCompatAlias"
        Pattern = "\bGenericBPP3DShadowPriceExtractor\b"
    },
    @{
        Name = "GenericBPP3DCGPipelineCompatAlias"
        Pattern = "\bGenericBPP3DCGPipeline\b"
    },
    @{
        Name = "AbstractBPP3DCGPipelineListCompatAlias"
        Pattern = "\bAbstractBPP3DCGPipelineList\b"
    },
    @{
        Name = "GenericBPP3DCGPipelineListCompatAlias"
        Pattern = "\bGenericBPP3DCGPipelineList\b"
    },
    @{
        Name = "InfraMaterialPackingDemandCompatAlias"
        Pattern = "\bInfraMaterialPackingDemand\b"
    },
    @{
        Name = "InfraMaterialPackingProgramCandidateCompatAlias"
        Pattern = "\bInfraMaterialPackingProgramCandidate\b"
    },
    @{
        Name = "InfraNumberGenericDomainAliasToken"
        Pattern = "\bInfraNumber(Material|PackageShape|Package|Item|ItemPlacement|BinLayer)\b"
    },
    @{
        Name = "FltXGenericDomainAliasToken"
        Pattern = "\bFltX(Material|PackageShape|Package|Item|ItemPlacement|BinLayer)\b"
    },
    @{
        Name = "InfraCuboidCompatAlias"
        Pattern = "\bInfraCuboid\b"
    },
    @{
        Name = "ProjectionPlaneCompatAlias"
        Pattern = "^\s*typealias\s+(Direction|ZOX|XOY|ZOY)\b"
    },
    @{
        Name = "legacyZeroToken"
        Pattern = "\blegacyZero\b"
    },
    @{
        Name = "legacyOneToken"
        Pattern = "\blegacyOne\b"
    },
    @{
        Name = "legacyTwoToken"
        Pattern = "\blegacyTwo\b"
    },
    @{
        Name = "legacyInfinityToken"
        Pattern = "\blegacyInfinity\b"
    },
    @{
        Name = "legacyNegativeInfinityToken"
        Pattern = "\blegacyNegativeInfinity\b"
    },
    @{
        Name = "legacyScalarToken"
        Pattern = "\blegacyScalar\b"
    },
    @{
        Name = "ScalarTypeAlias"
        Pattern = "^\s*(private\s+)?typealias\s+[A-Za-z0-9_]*Scalar\b"
    }
)

$fileNameChecks = @(
    @{
        Name = "LegacyInFileName"
        Pattern = "Legacy"
    },
    @{
        Name = "legacyInFileName"
        Pattern = "legacy"
    },
    @{
        Name = "Flt64InFileName"
        Pattern = "Flt64"
    },
    @{
        Name = "BridgeInFileName"
        Pattern = "Bridge"
    },
    @{
        Name = "ScalarInFileName"
        Pattern = "Scalar"
    }
)

$violations = @()

$compatDirectories = @(
    "api/compat",
    "model/compat",
    "service/compat"
)

foreach ($dir in $compatDirectories) {
    $dirs = Get-ChildItem -Path $scanRoot -Recurse -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName.Replace("\", "/") -match "/src/main/.*/$([regex]::Escape($dir))$" }
    foreach ($d in $dirs) {
        $violations += [PSCustomObject]@{
            Check = "CompatDirectory"
            File = $d.FullName.Replace("\", "/")
            Line = 1
            Text = "compat directory is forbidden in strict mode"
        }
    }
}

foreach ($check in $contentChecks) {
    $lines = rg -n --no-heading --color never $check.Pattern $scanRoot -g $sourceGlob -S
    foreach ($line in $lines) {
        $match = [regex]::Match($line, "^([A-Za-z]:.*?):([0-9]+):(.*)$")
        if (-not $match.Success) {
            continue
        }

        $filePath = $match.Groups[1].Value.Replace("\", "/")
        $lineNumber = $match.Groups[2].Value
        $text = $match.Groups[3].Value

        $violations += [PSCustomObject]@{
            Check = $check.Name
            File = $filePath
            Line = $lineNumber
            Text = $text
        }
    }
}

$files = rg --files $scanRoot -g $sourceGlob
foreach ($rawPath in $files) {
    $filePath = $rawPath.Replace("\", "/")
    $fileName = [System.IO.Path]::GetFileName($filePath)
    foreach ($check in $fileNameChecks) {
        if ($fileName -cmatch $check.Pattern) {
            $violations += [PSCustomObject]@{
                Check = $check.Name
                File = $filePath
                Line = 1
                Text = "forbidden file name: $fileName"
            }
        }
    }
}

if ($violations.Count -eq 0) {
    Write-Host "STRICT_GENERIC_BOUNDARY_PASS"
    exit 0
}

Write-Host "STRICT_GENERIC_BOUNDARY_FAIL: $($violations.Count)"
$violations |
    Sort-Object Check, File, Line |
    ForEach-Object {
        Write-Host "$($_.Check):$($_.File):$($_.Line):$($_.Text)"
    }
exit 1
