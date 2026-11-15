@echo off
setlocal
cd /d "%~dp0"
echo ============================================
echo          RETAIL STOCK PULSE
echo ============================================
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found. Open a new terminal and check: mvn -version
  pause
  exit /b 1
)
mvn clean compile exec:java
if errorlevel 1 pause
endlocal
