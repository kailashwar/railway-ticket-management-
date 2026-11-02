@echo off
echo ========================================================
echo  RailBook - Push to GitHub
echo ========================================================
echo.
echo GitHub requires a Personal Access Token (PAT) instead of account password.
echo.
set /p TOKEN="Paste your GitHub Token (ghp_...): "
if "%TOKEN%"=="" (
    echo Token cannot be empty.
    pause
    exit /b
)
echo.
echo Pushing to GitHub repository...
git push https://%TOKEN%@github.com/kailashwar/railway-ticket-management-.git main
if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo  SUCCESS: Project pushed to GitHub!
    echo  URL: https://github.com/kailashwar/railway-ticket-management-
    echo ========================================================
) else (
    echo.
    echo [ERROR] Push failed. Please verify your token permissions.
)
pause
