[CmdletBinding()]
param(
    [ValidateSet('doctor', 'up', 'down', 'backend', 'frontend', 'verify')]
    [string]$Command = 'doctor'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$composeFile = Join-Path $projectRoot 'infrastructure/docker-compose.yml'

function Find-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        return $env:JAVA_HOME
    }
    $adoptiumRoot = 'C:\Program Files\Eclipse Adoptium'
    $candidate = Get-ChildItem $adoptiumRoot -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($candidate -and (Test-Path (Join-Path $candidate.FullName 'bin/java.exe'))) {
        return $candidate.FullName
    }
    throw 'JDK 21 was not found. Install Temurin 21 and set JAVA_HOME, then open a new terminal.'
}

function Initialize-Session {
    $env:JAVA_HOME = Find-JavaHome
    if (-not (Test-Path $envFile)) {
        throw "Missing $envFile. Copy .env.example to .env and provide local-only secrets."
    }
}

function Require-Docker {
    try {
        & docker version --format '{{.Server.Version}}' | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw 'Docker daemon is unavailable.'
        }
    }
    catch {
        throw @"
Docker Desktop is unavailable. Start Docker Desktop, then ensure the current Windows user can read
%USERPROFILE%\.docker\config.json and can connect to \\.\pipe\docker_engine. If Docker was installed by
another account, repair ownership or add this user to docker-users, then sign out and sign in again.
"@
    }
}

function Invoke-InProject([string]$workingDirectory, [string]$program, [string[]]$arguments) {
    Push-Location $workingDirectory
    try {
        & $program @arguments
        if ($LASTEXITCODE -ne 0) { throw "$program failed with exit code $LASTEXITCODE" }
    }
    finally {
        Pop-Location
    }
}

Initialize-Session

switch ($Command) {
    'doctor' {
        Write-Host "JAVA_HOME=$env:JAVA_HOME"
        & (Join-Path $env:JAVA_HOME 'bin/java.exe') -version
        node --version
        npm.cmd --version
        Invoke-InProject (Join-Path $projectRoot 'backend') 'cmd.exe' @('/c', 'mvnw.cmd', '-version')
        try { Require-Docker; Write-Host 'Docker: ready' } catch { Write-Warning $_.Exception.Message }
    }
    'up' {
        Require-Docker
        & docker compose --env-file $envFile -f $composeFile up -d
        if ($LASTEXITCODE -ne 0) { throw 'Docker Compose failed to start the local services.' }
        & docker compose --env-file $envFile -f $composeFile ps
        if ($LASTEXITCODE -ne 0) { throw 'Docker Compose could not report the local services.' }
    }
    'down' {
        Require-Docker
        & docker compose --env-file $envFile -f $composeFile down
        if ($LASTEXITCODE -ne 0) { throw 'Docker Compose failed to stop the local services.' }
    }
    'backend' {
        Require-Docker
        Invoke-InProject (Join-Path $projectRoot 'backend') 'cmd.exe' @('/c', 'mvnw.cmd', 'spring-boot:run')
    }
    'frontend' { Invoke-InProject (Join-Path $projectRoot 'frontend') 'npm.cmd' @('run', 'dev') }
    'verify' {
        Require-Docker
        Invoke-InProject (Join-Path $projectRoot 'backend') 'cmd.exe' @('/c', 'mvnw.cmd', 'verify')
        Invoke-InProject (Join-Path $projectRoot 'frontend') 'npm.cmd' @('run', 'lint')
        Invoke-InProject (Join-Path $projectRoot 'frontend') 'npm.cmd' @('run', 'typecheck')
        Invoke-InProject (Join-Path $projectRoot 'frontend') 'npm.cmd' @('test')
        Invoke-InProject (Join-Path $projectRoot 'frontend') 'npm.cmd' @('run', 'build')
    }
}
