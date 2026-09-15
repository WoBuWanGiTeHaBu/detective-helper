@echo off
rem ============================================================
rem  Detective Helper - dev launcher (Electron shell, no packaging)
rem
rem  Why this file exists:
rem    run-electron-dev.ps1 lives in scripts\ , so
rem      powershell -File scripts\run-electron-dev.ps1
rem    only works when the CURRENT DIRECTORY is the project root.
rem    Opened from anywhere else it fails with
rem      "the argument 'scripts\run-electron-dev.ps1' does not exist".
rem    This wrapper cd's to its own folder first, so double-clicking
rem    works as well as calling it from any prompt.
rem
rem  Usage:
rem    run-dev.cmd              -> build if needed, then launch
rem    run-dev.cmd -SkipBuild   -> skip the Maven build
rem ============================================================
setlocal
cd /d "%~dp0"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-electron-dev.ps1" %*
set "DHA_EXIT=%ERRORLEVEL%"

if not "%DHA_EXIT%"=="0" (
    echo.
    echo [FAILED] exit code %DHA_EXIT%
    echo Copy the output above when reporting the problem.
    echo.
    pause
)

endlocal & exit /b %DHA_EXIT%
