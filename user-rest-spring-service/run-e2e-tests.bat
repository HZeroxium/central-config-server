@echo off
REM E2E Test Runner Script for User REST Spring Service (Windows)
REM This script runs end-to-end tests against the running system

echo 🚀 Starting E2E Test Runner for User REST Spring Service
echo ==================================================

REM Check if Docker Compose is running
echo 📋 Checking if system is running...
docker-compose ps | findstr "user-rest-spring-service.*Up" >nul
if %errorlevel% neq 0 (
    echo ❌ System is not running. Please start it first:
    echo    docker-compose up -d
    echo    Wait for all services to be healthy, then run this script again.
    exit /b 1
)

echo ✅ System is running

REM Wait for services to be ready
echo ⏳ Waiting for services to be ready...
timeout /t 10 /nobreak >nul

REM Check service health
echo 🏥 Checking service health...
curl -f http://localhost:8083/actuator/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ user-rest-spring-service is not healthy
    exit /b 1
)

echo ✅ All services are healthy

REM Run E2E tests
echo 🧪 Running E2E tests...
cd /d "%~dp0"

REM Run the E2E tests
gradlew.bat e2eTest --info

echo ✅ E2E tests completed successfully!
echo ==================================================
