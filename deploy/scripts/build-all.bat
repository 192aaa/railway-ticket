@echo off
chcp 65001 >nul
setlocal

cd /d "%~dp0..\.."

echo ==============================
echo   铁路订票系统 - 一键打包脚本 (Windows)
echo ==============================
echo.

echo [1/2] 打包后端 JAR...
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
    echo ❌ 后端打包失败
    pause
    exit /b 1
)

for /f "delims=" %%f in ('dir /b /o-n target\*.jar ^| findstr /v original ^| findstr /v sources') do set "JAR_FILE=target\%%f" & goto :found_jar
:found_jar
echo ✅ 后端 JAR: %JAR_FILE%
echo.

echo [2/2] 打包前端...
cd frontend
if not exist node_modules (
    echo 正在安装前端依赖...
    call npm install
    if errorlevel 1 (
        echo ❌ 前端依赖安装失败
        cd ..
        pause
        exit /b 1
    )
)
call npm run build
if errorlevel 1 (
    echo ❌ 前端打包失败
    cd ..
    pause
    exit /b 1
)
cd ..
echo ✅ 前端打包完成: frontend\dist
echo.

echo ==============================
echo   打包全部完成！
echo ==============================
echo.
echo 后端 JAR:  %JAR_FILE%
echo 前端静态:  frontend\dist\
echo.
pause