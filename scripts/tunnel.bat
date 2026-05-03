@echo off
REM SSH反向隧道 - Windows版本
REM 在本地服务器上运行，建立到公网服务器的反向隧道

setlocal enabledelayedexpansion

echo ==========================================
echo SSH反向隧道配置 - Windows
echo ==========================================
echo.

REM 配置
set PUBLIC_IP=101.35.79.76
set PUBLIC_USER=ubuntu
set LOCAL_PORT=8012
set TUNNEL_PORT=9012

echo 配置信息:
echo   公网服务器: %PUBLIC_IP%
echo   用户: %PUBLIC_USER%
echo   本地端口: %LOCAL_PORT%
echo   隧道端口: %TUNNEL_PORT%
echo.

REM 检查SSH是否可用
where ssh >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 未找到SSH命令
    echo 请确保已安装Git Bash或Windows OpenSSH
    pause
    exit /b 1
)

echo ✅ SSH命令可用
echo.

REM 建立SSH反向隧道
echo 🔗 建立SSH反向隧道...
echo 命令: ssh -o StrictHostKeyChecking=no -N -R %TUNNEL_PORT%:localhost:%LOCAL_PORT% %PUBLIC_USER%@%PUBLIC_IP%
echo.
echo 请输入密码: Cjh041217@
echo.

ssh -o StrictHostKeyChecking=no -N -R %TUNNEL_PORT%:localhost:%LOCAL_PORT% %PUBLIC_USER%@%PUBLIC_IP%

if %ERRORLEVEL% NEQ 0 (
    echo ❌ SSH连接失败
    pause
    exit /b 1
)

echo.
echo ✅ SSH反向隧道已建立！
echo.
echo 隧道信息:
echo   本地: localhost:%LOCAL_PORT%
echo   公网服务器: localhost:%TUNNEL_PORT%
echo.
echo 🌐 访问地址: http://%PUBLIC_IP%:8012
echo.
echo 按Ctrl+C可以断开隧道
echo.

pause
