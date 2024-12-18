@echo off

chcp 65001
REM Define the PAPER_URL and destination directory
set "PAPER_URL=https://api.papermc.io/v2/projects/paper/versions/1.21.3/builds/80/downloads/paper-1.21.3-80.jar"
set "SCRIPT_DIR=%~dp0"
set "DEST_DIR=%SCRIPT_DIR%..\server"
set "PAPER_FILE=%DEST_DIR%\paper.jar"
set "EULA_FILE=%DEST_DIR%\eula.txt"
set "RUN_FILE=%DEST_DIR%\run.bat"

REM Create the destination directory if it doesn't exist
if not exist "%DEST_DIR%" (
    mkdir "%DEST_DIR%"
)

REM Check if paper.jar already exists
if not exist "%PAPER_FILE%" (
    REM Download paper.jar
    echo Downloading paper.jar at %PAPER_FILE%...
    powershell -Command "Invoke-WebRequest -Uri %PAPER_URL% -OutFile %PAPER_FILE%"
) else (
    echo paper.jar already exists at %PAPER_FILE%, skipping download.
)

REM Create eula.txt and set eula=true
echo Creating eula.txt...
echo #By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA).> "%EULA_FILE%"
echo eula=true>> "%EULA_FILE%"

REM Create run.bat with the specified command
echo Creating run.bat...
echo @echo off> "%RUN_FILE%"
echo cd /d "%%~dp0">> "%RUN_FILE%"
echo java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=localhost:5005 -Xmx1024M -Xms1024M -jar paper.jar>> "%RUN_FILE%"

REM Make run.bat executable
echo Setup complete. Run .\server\run.bat to start the server.