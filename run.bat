@echo off
REM Script para rodar a aplicação no Windows

if "%1"=="run" goto run
if "%1"=="restart" goto restart
if "%1"=="clean" goto clean
if "%1"=="migrate" goto migrate
if "%1"=="jooq" goto jooq
if "%1"=="prod" goto prod

echo Uso: run.bat [comando]
echo.
echo Comandos disponíveis:
echo   run      - Subir containers e rodar aplicação
echo   restart  - Reiniciar containers
echo   clean    - Limpeza completa (remove volumes)
echo   migrate  - Aplicar migrations
echo   jooq     - Regenerar classes JOOQ
echo   prod     - Rodar em modo produção
echo.
goto end

:run
echo Subindo containers...
docker compose up -d
echo Aplicando migrations...
gradlew.bat update
echo Regenerando JOOQ...
gradlew.bat generateJooq
echo Rodando aplicação...
gradlew.bat bootRun
goto end

:restart
echo Reiniciando containers...
docker compose down
docker compose up -d
echo Aplicando migrations...
gradlew.bat update
echo Pronto!
goto end

:clean
echo Limpeza completa...
docker compose down
docker volume prune -f
docker compose up -d
echo Aplicando migrations...
gradlew.bat update
echo Limpeza concluída!
goto end

:migrate
echo Aplicando migrations...
gradlew.bat update
goto end

:jooq
echo Regenerando classes JOOQ...
gradlew.bat generateJooq
goto end

:prod
echo Subindo containers...
docker compose up -d
echo Aplicando migrations...
gradlew.bat update
echo Regenerando JOOQ...
gradlew.bat generateJooq
echo Rodando em modo produção...
set SPRING_PROFILES_ACTIVE=prod
gradlew.bat bootRun
goto end

:end
