[CmdletBinding()]
param(
    [string]$ComputerName = "172.18.8.107",
    [int]$Port = 22,
    [Parameter(Mandatory = $true)]
    [string]$Username,
    [string]$Password,
    [string]$KeyFile,
    [string]$KeyPassphrase = "",
    [string]$RemoteDir = "/home/lab/apps/blog",
    [string]$ServiceName = "blog.service",
    [ValidateSet("user", "system")]
    [string]$ServiceScope = "user",
    [string]$HealthUrl = "http://127.0.0.1:8012/",
    [string]$JarPath = "target/blog-0.0.1-SNAPSHOT.jar",
    [switch]$Build,
    [switch]$SkipTests,
    [switch]$AcceptKey
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Import-Module Posh-SSH

function New-PlainSecureString {
    param([string]$Value)

    return (ConvertTo-SecureString $Value -AsPlainText -Force)
}

function New-SshCredential {
    param(
        [string]$UserName,
        [string]$Password,
        [string]$KeyFile,
        [string]$KeyPassphrase
    )

    if ($KeyFile) {
        return [PSCredential]::new($UserName, (New-PlainSecureString -Value $KeyPassphrase))
    }

    if (-not $Password) {
        throw "Password is required when KeyFile is not provided."
    }

    return [PSCredential]::new($UserName, (New-PlainSecureString -Value $Password))
}

function Invoke-CheckedCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed: $FilePath $($Arguments -join ' ')"
    }
}

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptRoot

Push-Location $repoRoot
try {
    if ($Build -or -not (Test-Path $JarPath)) {
        if (-not $SkipTests) {
            Invoke-CheckedCommand -FilePath ".\mvnw.cmd" -Arguments @("-q", "test")
        }

        Invoke-CheckedCommand -FilePath ".\mvnw.cmd" -Arguments @("-DskipTests", "package")
    }

    $resolvedJar = (Resolve-Path $JarPath).Path
    $resolvedKeyFile = if ($KeyFile) { (Resolve-Path $KeyFile).Path } else { $null }
    $remoteScriptPath = Join-Path $scriptRoot "remote-deploy-preserve-data.sh"
    if (-not (Test-Path $remoteScriptPath)) {
        throw "Missing remote deploy script: $remoteScriptPath"
    }

    $credential = New-SshCredential -UserName $Username -Password $Password -KeyFile $resolvedKeyFile -KeyPassphrase $KeyPassphrase
    $knownHostStore = Get-SSHOpenSSHKnownHost
    $releaseId = Get-Date -Format "yyyyMMdd-HHmmss"
    $remoteReleaseName = "blog-$releaseId.jar"
    $remoteScriptDir = "$RemoteDir/.deploy-tmp"
    $remoteScriptFile = "$remoteScriptDir/remote-deploy-preserve-data.sh"

    $connectionParams = @{
        ComputerName      = $ComputerName
        Credential        = $credential
        Port              = $Port
        ConnectionTimeout = 15
        KeepAliveInterval = 15
    }

    if ($AcceptKey) {
        $connectionParams["AcceptKey"] = $true
    }
    else {
        $connectionParams["KnownHost"] = $knownHostStore
        $connectionParams["ErrorOnUntrusted"] = $true
    }

    if ($resolvedKeyFile) {
        $connectionParams["KeyFile"] = $resolvedKeyFile
    }

    $session = $null
    try {
        $session = New-SSHSession @connectionParams
        $prepCommand = "mkdir -p '$RemoteDir/releases' '$remoteScriptDir'"
        $prepResult = Invoke-SSHCommand -SSHSession $session -Command $prepCommand -TimeOut 30
        if ($prepResult.ExitStatus -ne 0) {
            throw ($prepResult.Output | Out-String)
        }

        Set-SCPItem @connectionParams -Path $resolvedJar -Destination "$RemoteDir/releases" -NewName $remoteReleaseName | Out-Null
        Set-SCPItem @connectionParams -Path $remoteScriptPath -Destination $remoteScriptDir -NewName "remote-deploy-preserve-data.sh" | Out-Null

        $chmodResult = Invoke-SSHCommand -SSHSession $session -Command "chmod 700 '$remoteScriptFile'" -TimeOut 30
        if ($chmodResult.ExitStatus -ne 0) {
            throw ($chmodResult.Output | Out-String)
        }

        $deployCommand = "bash '$remoteScriptFile' '$RemoteDir' '$releaseId' '$ServiceName' '$ServiceScope' '$HealthUrl'"
        $deployResult = Invoke-SSHCommand -SSHSession $session -Command $deployCommand -TimeOut 180
        if ($deployResult.ExitStatus -ne 0) {
            throw ($deployResult.Output | Out-String)
        }

        $cleanupCommand = "rm -f '$remoteScriptFile'"
        Invoke-SSHCommand -SSHSession $session -Command $cleanupCommand -TimeOut 30 | Out-Null

        $deployResult.Output
    }
    finally {
        if ($session) {
            Remove-SSHSession -SSHSession $session | Out-Null
        }
    }
}
finally {
    Pop-Location
}
