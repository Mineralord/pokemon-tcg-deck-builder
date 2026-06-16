@echo off
rem ============================================================
rem  Abre la app de cartas Pokemon por HTTP (localhost).
rem  Necesario para que el EXPLORADOR y el espanol en vivo
rem  funcionen: abrir el HTML con doble clic (file://) los bloquea.
rem  Doble clic en este archivo cada vez que quieras usar la app.
rem ============================================================
cd /d "%~dp0"

rem Inicia un mini-servidor en segundo plano (ventana minimizada)
start "Servidor Pokemon TCG" /min python -m http.server 8765

rem Espera un segundo a que arranque y abre el navegador
timeout /t 1 /nobreak >nul
start "" "http://localhost:8765/pokemon-deck-builder.html"

echo.
echo  La app se abrio en: http://localhost:8765/pokemon-deck-builder.html
echo  Deja abierta la ventana minimizada "Servidor Pokemon TCG" mientras la uses.
echo  (Cierra esa ventana para apagar el servidor.)
echo.
timeout /t 4 /nobreak >nul
