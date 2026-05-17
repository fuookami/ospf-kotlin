#!/usr/bin/env pwsh

param(
    [string]$BusinessRoot = "E:\workspace\poit\poit-or",
    [string]$OutputPath = "docs/p7-business-compat-matrix.md",
    [int]$TopImports = 10
)

$ErrorActionPreference = "Stop"

$projects = @(
    [pscustomobject]@{ Name = "APS"; Directory = "poit-or-aps" },
    [pscustomobject]@{ Name = "CSP1D"; Directory = "poit-or-csp1d" },
    [pscustomobject]@{ Name = "BOP"; Directory = "poit-or-bop" },
    [pscustomobject]@{ Name = "PSP"; Directory = "poit-or-psp" }
)

$patterns = @(
    [pscustomobject]@{ Key = "LinearMetaModel"; Regex = "\bLinearMetaModel\b" },
    [pscustomobject]@{ Key = "QuadraticMetaModel"; Regex = "\bQuadraticMetaModel\b" },
    [pscustomobject]@{ Key = "AbstractLinearMetaModel"; Regex = "\bAbstractLinearMetaModel\b" },
    [pscustomobject]@{ Key = "AbstractQuadraticMetaModel"; Regex = "\bAbstractQuadraticMetaModel\b" },
    [pscustomobject]@{ Key = "Pipeline/PipelineList"; Regex = "\b(Pipeline|PipelineList)\b" },
    [pscustomobject]@{ Key = "LinearIntermediateSymbolsN"; Regex = "\bLinearIntermediateSymbols[1-4]?\b" },
    [pscustomobject]@{ Key = "LinearExpressionSymbolsN"; Regex = "\bLinearExpressionSymbols[1-4]?\b" },
    [pscustomobject]@{ Key = "QuadraticIntermediateSymbolsN"; Regex = "\bQuadraticIntermediateSymbols[1-4]?\b" },
    [pscustomobject]@{ Key = "BinVariableN"; Regex = "\bBinVariable[1-4]?\b" },
    [pscustomobject]@{ Key = "UIntVariableN"; Regex = "\bUIntVariable[1-4]?\b" },
    [pscustomobject]@{ Key = "UReal/PctVariableN"; Regex = "\b(URealVariable[1-4]?|PctVariable[1-4]?)\b" },
    [pscustomobject]@{ Key = "vectorView/belongsTo"; Regex = "\b(vectorView|belongsTo)\b" },
    [pscustomobject]@{ Key = "sum/qsum"; Regex = "\b(q?sum|sumVars|qsumVars)\b" },
    [pscustomobject]@{ Key = "Function symbols"; Regex = "\b(AbsFunction|BinaryzationFunction|CeilingFunction|FloorFunction|MaskingFunction|MaxFunction|MinFunction|SlackFunction|SlackRangeFunction|XorFunction|SemiFunction|ULPFunction|LinearFunctionSymbol|QuadraticFunctionSymbol)\b" },
    [pscustomobject]@{ Key = "Solver builders"; Regex = "\b(LinearSolverBuilder|QuadraticSolverBuilder|ColumnGeneratorSolverBuilder|ColumnGenerationSolverBuilder)\b" },
    [pscustomobject]@{ Key = "Solver calls"; Regex = "\b(GurobiLinearSolver|ScipLinearSolver|GurobiQuadraticSolver|ScipQuadraticSolver|ColumnGenerationSolver|solve\()\b" }
)

function Get-KtFiles {
    param([string]$Root)
    if (-not (Test-Path $Root)) {
        return @()
    }
    return @(
        Get-ChildItem -Path $Root -Recurse -Filter "*.kt" -File |
            Where-Object { $_.FullName -notmatch "[/\\]target[/\\]" }
    )
}

function Get-Text {
    param([System.IO.FileInfo]$File)
    return Get-Content -Path $File.FullName -Raw -Encoding UTF8
}

function Count-Regex {
    param([System.IO.FileInfo[]]$Files, [string]$Regex)
    $count = 0
    foreach ($file in $Files) {
        $count += [regex]::Matches((Get-Text $file), $Regex).Count
    }
    return $count
}

function Get-PatternMap {
    param([System.IO.FileInfo[]]$Files)
    $map = [ordered]@{ "Kotlin files" = $Files.Count }
    foreach ($pattern in $patterns) {
        $map[$pattern.Key] = Count-Regex -Files $Files -Regex $pattern.Regex
    }
    return $map
}

function Get-TopImports {
    param([System.IO.FileInfo[]]$Files, [int]$Limit)
    $counts = @{}
    foreach ($file in $Files) {
        $matches = Select-String -Path $file.FullName -Pattern "^\s*import\s+fuookami\.ospf\.kotlin\..+" -Encoding UTF8
        foreach ($match in $matches) {
            $import = ($match.Line -replace "^\s*import\s+", "").Trim()
            if (-not $counts.ContainsKey($import)) {
                $counts[$import] = 0
            }
            $counts[$import]++
        }
    }
    return @(
        $counts.GetEnumerator() |
            Sort-Object -Property @{ Expression = "Value"; Descending = $true }, @{ Expression = "Key"; Ascending = $true } |
            Select-Object -First $Limit
    )
}

function Get-Modules {
    param([string]$ProjectPath)
    if (-not (Test-Path $ProjectPath)) {
        return @()
    }
    return @(
        Get-ChildItem -Path $ProjectPath -Directory |
            Where-Object { Test-Path (Join-Path $_.FullName "src") } |
            Sort-Object Name
    )
}

function Add-ProjectSummary {
    param([System.Collections.Generic.List[string]]$Lines, [object[]]$Rows)
    $columns = @("Project", "Kotlin files") + @($patterns | ForEach-Object { $_.Key })
    $Lines.Add("| " + ($columns -join " | ") + " |")
    $Lines.Add("|" + (($columns | ForEach-Object { "---" }) -join "|") + "|")
    foreach ($row in $Rows) {
        $values = @($row.Name)
        foreach ($column in $columns | Select-Object -Skip 1) {
            $values += $row.Map[$column]
        }
        $Lines.Add("| " + ($values -join " | ") + " |")
    }
}

function Add-ModuleSummary {
    param([System.Collections.Generic.List[string]]$Lines, [string]$ProjectName, [object[]]$Rows)
    $columns = @(
        "Module",
        "Kotlin files",
        "LinearMetaModel",
        "QuadraticMetaModel",
        "Pipeline/PipelineList",
        "LinearIntermediateSymbolsN",
        "LinearExpressionSymbolsN",
        "QuadraticIntermediateSymbolsN",
        "BinVariableN",
        "UIntVariableN",
        "UReal/PctVariableN",
        "sum/qsum",
        "Function symbols",
        "Solver builders",
        "Solver calls"
    )
    $Lines.Add("")
    $Lines.Add("## $ProjectName module matrix")
    if ($Rows.Count -eq 0) {
        $Lines.Add("")
        $Lines.Add("No module directory with src was found.")
        return
    }
    $Lines.Add("")
    $Lines.Add("| " + ($columns -join " | ") + " |")
    $Lines.Add("|" + (($columns | ForEach-Object { "---" }) -join "|") + "|")
    foreach ($row in $Rows) {
        $values = @($row.Name)
        foreach ($column in $columns | Select-Object -Skip 1) {
            $values += $row.Map[$column]
        }
        $Lines.Add("| " + ($values -join " | ") + " |")
    }
}

function Add-Imports {
    param([System.Collections.Generic.List[string]]$Lines, [string]$ProjectName, [object[]]$Imports)
    $Lines.Add("")
    $Lines.Add("## $ProjectName top imports")
    $Lines.Add("")
    if ($Imports.Count -eq 0) {
        $Lines.Add("No OSPF imports were found.")
        return
    }
    $Lines.Add("| Import | Count |")
    $Lines.Add("|---|---|")
    foreach ($entry in $Imports) {
        $Lines.Add("| ``$($entry.Key)`` | $($entry.Value) |")
    }
}

$projectRows = @()
$moduleRowsByProject = @{}
$topImportsByProject = @{}

foreach ($project in $projects) {
    $projectPath = Join-Path $BusinessRoot $project.Directory
    $files = Get-KtFiles -Root $projectPath
    $projectRows += [pscustomobject]@{
        Name = $project.Name
        Map = Get-PatternMap -Files $files
    }
    $moduleRows = @()
    foreach ($module in Get-Modules -ProjectPath $projectPath) {
        $moduleFiles = Get-KtFiles -Root $module.FullName
        $moduleRows += [pscustomobject]@{
            Name = $module.Name
            Map = Get-PatternMap -Files $moduleFiles
        }
    }
    $moduleRowsByProject[$project.Name] = $moduleRows
    $topImportsByProject[$project.Name] = Get-TopImports -Files $files -Limit $TopImports
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("# P7 Business Compatibility Matrix")
$lines.Add("")
$generatedAt = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"
$lines.Add("- Generated at: $generatedAt")
$lines.Add("- Business root: ``$BusinessRoot``")
$lines.Add("- Scan scope: APS, CSP1D, BOP, PSP Kotlin sources")
$lines.Add("")
$lines.Add("External direct compile is not the default P7 gate because these projects still pin old OSPF coordinates and old ``core.frontend`` package names. The in-repo ``business-source-compat`` profile is the source-compat fixture for the restored current API surface.")
$lines.Add("")
$lines.Add("## Project matrix")
$lines.Add("")
Add-ProjectSummary -Lines $lines -Rows $projectRows

foreach ($project in $projects) {
    Add-ModuleSummary -Lines $lines -ProjectName $project.Name -Rows $moduleRowsByProject[$project.Name]
}

foreach ($project in $projects) {
    Add-Imports -Lines $lines -ProjectName $project.Name -Imports $topImportsByProject[$project.Name]
}

$content = $lines -join [Environment]::NewLine
if ($OutputPath) {
    $parent = Split-Path -Parent $OutputPath
    if ($parent -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent | Out-Null
    }
    Set-Content -Path $OutputPath -Encoding UTF8 -Value $content
}

Write-Output $content
