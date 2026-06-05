param(
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"

$projectPath = Resolve-Path -Path $ProjectRoot
$frameworkRoot = Join-Path $projectPath "ospf-kotlin-framework-bpp3d"
$moduleGeometryRoot = Join-Path $projectPath "ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/math/geometry"
$legacyGeometryRoot = Join-Path $frameworkRoot "bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/geometry"
$infrastructureRoot = Join-Path $frameworkRoot "bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure"
$infrastructureTestRoot = Join-Path $frameworkRoot "bpp3d-infrastructure/src/test/kotlin/fuookami/ospf/kotlin/framework/bpp3d/infrastructure"

if (-not (Get-Command rg -ErrorAction SilentlyContinue)) {
    throw "rg not found. Please install ripgrep."
}

if (-not (Test-Path $moduleGeometryRoot)) {
    throw "Module geometry root not found: $moduleGeometryRoot"
}

$script:violations = @()

function Add-RgViolations {
    param(
        [string]$Check,
        [string]$Pattern,
        [string]$Root
    )

    $lines = rg -n --no-heading --color never $Pattern $Root -S
    foreach ($line in $lines) {
        $match = [regex]::Match($line, "^([A-Za-z]:.*?):([0-9]+):(.*)$")
        if (-not $match.Success) {
            continue
        }
        $script:violations += [PSCustomObject]@{
            Check = $Check
            File = $match.Groups[1].Value.Replace("\", "/")
            Line = $match.Groups[2].Value
            Text = $match.Groups[3].Value
        }
    }
}

Add-RgViolations -Check "GeometryInfraScalar" -Pattern "InfraScalar|Quantity<\s*InfraScalar\s*>" -Root $moduleGeometryRoot
Add-RgViolations -Check "GeometryBusinessTerms" -Pattern "bottomSupport|loadingRate|amount|enabledOrientations|kerf|cuttingPattern|packingRate|\bbin\b|\bplate\b" -Root $moduleGeometryRoot
Add-RgViolations -Check "GeometryForbiddenImport" -Pattern "^import\s+.*framework\.(bpp3d|bpp2d|csp2d)" -Root $moduleGeometryRoot
Add-RgViolations -Check "LegacyGeometryImportInInfrastructure" -Pattern "^import\s+fuookami\.ospf\.kotlin\.framework\.bpp3d\.infrastructure\.geometry\." -Root $infrastructureRoot
if (Test-Path $infrastructureTestRoot) {
    Add-RgViolations -Check "LegacyGeometryImportInInfrastructureTest" -Pattern "^import\s+fuookami\.ospf\.kotlin\.framework\.bpp3d\.infrastructure\.geometry\." -Root $infrastructureTestRoot
}

$legacyFiles = @()
if (Test-Path $legacyGeometryRoot) {
    $legacyFiles = Get-ChildItem -Path $legacyGeometryRoot -Filter "*.kt" | Select-Object -ExpandProperty Name
}
foreach ($legacyFile in $legacyFiles) {
    $script:violations += [PSCustomObject]@{
        Check = "LegacyGeometryFileNotAllowed"
        File = (Join-Path $legacyGeometryRoot $legacyFile).Replace("\", "/")
        Line = "1"
        Text = "Legacy geometry compatibility file should be removed before release"
    }
}

$bridgePattern = "(Cuboid3|Cuboid3View|Cylinder3|Box3|Placement2|Placement3|Rectangle2|PlanePoint2|PlanePoint3|PlaneVector3)\s*<\s*InfraScalar\s*>"
$bridgeLines = rg -n --no-heading --color never $bridgePattern $infrastructureRoot -g "**/*.kt" -S
$bridgeAllowRules = @()
$bridgeAllowRuleHits = @{}
foreach ($rule in $bridgeAllowRules) {
    $bridgeAllowRuleHits[$rule] = $false
}

foreach ($line in $bridgeLines) {
    $match = [regex]::Match($line, "^([A-Za-z]:.*?):([0-9]+):(.*)$")
    if (-not $match.Success) {
        continue
    }

    $filePath = $match.Groups[1].Value.Replace("\", "/")
    $isAllowed = $false
    foreach ($rule in $bridgeAllowRules) {
        if ($filePath -match $rule) {
            $isAllowed = $true
            $bridgeAllowRuleHits[$rule] = $true
            break
        }
    }

    if (-not $isAllowed) {
        $script:violations += [PSCustomObject]@{
            Check = "InfraScalarBridgeOutOfAllowList"
            File = $filePath
            Line = $match.Groups[2].Value
            Text = $match.Groups[3].Value
        }
    }
}

foreach ($rule in $bridgeAllowRules) {
    if (-not $bridgeAllowRuleHits[$rule]) {
        $script:violations += [PSCustomObject]@{
            Check = "StaleInfraScalarBridgeAllowRule"
            File = $rule
            Line = "1"
            Text = "bridge allow rule has no current match"
        }
    }
}

if ($script:violations.Count -eq 0) {
    Write-Host "GEOMETRY_BOUNDARY_PASS"
    exit 0
}

Write-Host "GEOMETRY_BOUNDARY_FAIL: $($script:violations.Count)"
$script:violations |
    Sort-Object Check, File, Line |
    ForEach-Object {
        Write-Host "$($_.Check):$($_.File):$($_.Line):$($_.Text)"
    }
exit 1
