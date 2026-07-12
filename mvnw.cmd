@echo off
setlocal
set MAVEN_VERSION=3.9.16
set ROOT_DIR=%~dp0
set DOWNLOAD_DIR=%ROOT_DIR%.mvn\download
set MAVEN_HOME=%DOWNLOAD_DIR%\apache-maven-%MAVEN_VERSION%
set ARCHIVE=%DOWNLOAD_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip
set URL=https://dlcdn.apache.org/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%DOWNLOAD_DIR%" mkdir "%DOWNLOAD_DIR%"
  echo Downloading Apache Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%URL%' -OutFile '%ARCHIVE%'; Expand-Archive -Path '%ARCHIVE%' -DestinationPath '%DOWNLOAD_DIR%' -Force"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
