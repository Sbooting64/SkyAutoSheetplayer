@echo off
setlocal enabledelayedexpansion
title Ejecutor de SkyAutoSheetplayer
color 0b

echo ===========================================
echo   Iniciando SkyAutoSheetplayer - Sky:cotl
echo ===========================================

:: Verificar que estamos en el directorio correcto
if not exist "src\SkyMusicPlayer" (
    echo [ERROR] No se encuentra la carpeta src\SkyMusicPlayer
    echo Ejecuta este archivo desde la raiz del proyecto.
    pause
    exit /b 1
)

:: Crear carpeta de salida si no existe
if not exist "bin\classes" mkdir bin\classes

echo Compilando archivos fuente...
:: Compila todos los archivos .java de manera recursiva
javac -encoding UTF-8 -d bin\classes src\SkyMusicPlayer\*.java 2>&1

if !errorlevel! neq 0 (
    echo [ERROR] Hubo un problema al compilar el codigo.
    echo Asegurate de tener JDK instalado y en el PATH.
    echo Verifica los mensajes de error arriba.
    pause
    exit /b 1
)

echo Compilacion exitosa. Iniciando programa...
:: Ejecuta la clase principal
java -cp bin\classes SkyMusicPlayer.SkyMusicPlayer

if !errorlevel! neq 0 (
    echo.
    echo [AVISO] El programa se cerro o no se encontro la clase principal especificada.
    echo Si el nombre de la clase principal es distinto, edita este .bat.
)

pause
exit /b 0
