@echo off
setlocal

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto java_ready
for /d %%J in ("%ProgramFiles%\Eclipse Adoptium\jdk-25*") do (
  if exist "%%~fJ\bin\java.exe" set "JAVA_HOME=%%~fJ"
)

:java_ready
if not defined JAVA_HOME (
  echo Java 25 is required. Install it or set JAVA_HOME to a Java 25 installation.
  exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -XshowSettings:properties -version 2>&1 | findstr /C:"java.specification.version = 25" >nul
if errorlevel 1 (
  echo JAVA_HOME must point to Java 25. Current value: %JAVA_HOME%
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "MAVEN_OPTS=-Dmaven.repo.local=%USERPROFILE%\.m2\repository %MAVEN_OPTS%"
call "%~dp0mvnw.cmd" spring-boot:run
exit /b %ERRORLEVEL%
