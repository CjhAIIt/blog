# SSH反向隧道 - PowerShell版本（推荐）
# 在本地服务器上运行，建立到公网服务器的反向隧道

param(
    [switch]$Install,
    [switch]$Start,
    [switch]$Stop,
    [switch]$Status
)

$PUBLIC_IP = "101.35.79.76"
$PUBLIC_USER = "ubuntu"
$LOCAL_PORT = "8012"
$TUNNEL_PORT = "9012"
$SERVICE_NAME = "BlogTunnel"
$SCRIPT_PATH = $PSScriptRoot

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "SSH反向隧道管理 - PowerShell" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 检查SSH
$sshPath = Get-Command ssh -ErrorAction SilentlyContinue
if (-not $sshPath) {
    Write-Host "❌ 未找到SSH命令" -ForegroundColor Red
    Write-Host "请确保已安装Git Bash或Windows OpenSSH" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ SSH命令可用: $($sshPath.Source)" -ForegroundColor Green
Write-Host ""

# 创建隧道脚本
$tunnelScript = @"
`$PUBLIC_IP = "$PUBLIC_IP"
`$PUBLIC_USER = "$PUBLIC_USER"
`$LOCAL_PORT = "$LOCAL_PORT"
`$TUNNEL_PORT = "$TUNNEL_PORT"

Write-Host "🔗 建立SSH反向隧道..." -ForegroundColor Cyan
Write-Host "本地: localhost:`$LOCAL_PORT -> 公网: localhost:`$TUNNEL_PORT" -ForegroundColor Yellow
Write-Host ""

while (`$true) {
    try {
        ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL -N -R `$TUNNEL_PORT`:localhost:`$LOCAL_PORT `$PUBLIC_USER@`$PUBLIC_IP
    } catch {
        Write-Host "❌ 隧道连接失败: `$_" -ForegroundColor Red
        Write-Host "5秒后重试..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
}
"@

$tunnelScriptPath = Join-Path $SCRIPT_PATH "tunnel-worker.ps1"

if ($Install) {
    Write-Host "📋 安装隧道服务..." -ForegroundColor Cyan

    # 保存隧道脚本
    $tunnelScript | Out-File -FilePath $tunnelScriptPath -Encoding UTF8 -Force
    Write-Host "✅ 隧道脚本已保存: $tunnelScriptPath" -ForegroundColor Green

    # 创建任务计划程序任务
    $taskAction = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$tunnelScriptPath`""
    $taskTrigger = New-ScheduledTaskTrigger -AtStartup
    $taskSettings = New-ScheduledTaskSettingSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -RunOnlyIfNetworkAvailable

    Register-ScheduledTask -TaskName $SERVICE_NAME -Action $taskAction -Trigger $taskTrigger -Settings $taskSettings -Force | Out-Null

    Write-Host "✅ 隧道服务已安装" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 配置信息:" -ForegroundColor Cyan
    Write-Host "  公网服务器: $PUBLIC_IP" -ForegroundColor Yellow
    Write-Host "  用户: $PUBLIC_USER" -ForegroundColor Yellow
    Write-Host "  本地端口: $LOCAL_PORT" -ForegroundColor Yellow
    Write-Host "  隧道端口: $TUNNEL_PORT" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "💡 下次启动时隧道将自动建立" -ForegroundColor Green
    Write-Host ""
    exit 0
}

if ($Start) {
    Write-Host "🚀 启动隧道服务..." -ForegroundColor Cyan
    Start-ScheduledTask -TaskName $SERVICE_NAME -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2

    $task = Get-ScheduledTask -TaskName $SERVICE_NAME -ErrorAction SilentlyContinue
    if ($task.State -eq "Running") {
        Write-Host "✅ 隧道服务已启动" -ForegroundColor Green
    } else {
        Write-Host "⚠️  隧道服务状态: $($task.State)" -ForegroundColor Yellow
    }
    exit 0
}

if ($Stop) {
    Write-Host "⏹️  停止隧道服务..." -ForegroundColor Cyan
    Stop-ScheduledTask -TaskName $SERVICE_NAME -ErrorAction SilentlyContinue
    Write-Host "✅ 隧道服务已停止" -ForegroundColor Green
    exit 0
}

if ($Status) {
    Write-Host "📊 隧道服务状态:" -ForegroundColor Cyan
    $task = Get-ScheduledTask -TaskName $SERVICE_NAME -ErrorAction SilentlyContinue
    if ($task) {
        Write-Host "  名称: $($task.TaskName)" -ForegroundColor Yellow
        Write-Host "  状态: $($task.State)" -ForegroundColor Yellow
        Write-Host "  路径: $($task.TaskPath)" -ForegroundColor Yellow
    } else {
        Write-Host "❌ 隧道服务未安装" -ForegroundColor Red
    }
    exit 0
}

# 默认：直接建立隧道
Write-Host "🔗 建立SSH反向隧道..." -ForegroundColor Cyan
Write-Host "配置信息:" -ForegroundColor Yellow
Write-Host "  公网服务器: $PUBLIC_IP" -ForegroundColor Yellow
Write-Host "  用户: $PUBLIC_USER" -ForegroundColor Yellow
Write-Host "  本地端口: $LOCAL_PORT" -ForegroundColor Yellow
Write-Host "  隧道端口: $TUNNEL_PORT" -ForegroundColor Yellow
Write-Host ""
Write-Host "请输入密码: Cjh041217@" -ForegroundColor Cyan
Write-Host ""

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=NUL -N -R "${TUNNEL_PORT}:localhost:${LOCAL_PORT}" "${PUBLIC_USER}@${PUBLIC_IP}"

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ SSH连接失败" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ SSH反向隧道已建立！" -ForegroundColor Green
Write-Host ""
Write-Host "🌐 访问地址: http://$PUBLIC_IP:8012" -ForegroundColor Cyan
Write-Host ""
Write-Host "按Ctrl+C可以断开隧道" -ForegroundColor Yellow
