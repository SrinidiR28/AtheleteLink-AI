@echo off
setlocal
cd /d "%~dp0backend"
echo ========================================================
echo        AthleteLink AI - Starting Full-Stack Server
echo ========================================================
echo Access in browser: http://localhost:8080
echo Mobile Wi-Fi URL:  http://10.65.24.238:8080 (or your Wi-Fi IPv4:8080)
echo ========================================================

if exist "target\athletelink-backend-1.0.0.jar" (
    echo Launching pre-compiled JAR...
    java -jar target\athletelink-backend-1.0.0.jar
) else (
    echo Running with Maven...
    mvn spring-boot:run
)
pause
