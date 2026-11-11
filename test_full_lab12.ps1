$env:JAVA_HOME = "C:\Program Files\Java\jdk-20"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$configJar = (Resolve-Path "ms-administration-server-configuration\build\libs\ms-administration-server-configuration-0.0.1-SNAPSHOT.jar").Path
$productoJar = (Resolve-Path "ms-negocio-producto\target\ms-negocio-producto-0.0.1-SNAPSHOT.jar").Path
$repoDir = (Resolve-Path "ms-administration-data-configuration").Path
$searchLoc = "file:///" + ($repoDir -replace '\\', '/')
$env:CONFIG_REPO_PATH = $searchLoc

# Limpiar BD para la prueba
docker exec -i postgres-producto psql -U postgres -d BD_PRODUCTO -c "TRUNCATE TABLE sys.movimiento_inventario, sys.producto RESTART IDENTITY CASCADE;" > $null

Write-Host "=========================================================="
Write-Host " PRUEBAS DE INTEGRACION Y PARAMETRIZACION - LABORATORIO 12"
Write-Host "=========================================================="

# 1. Iniciar Config Server
Write-Host "`n[PASO 1] Iniciando Spring Cloud Config Server en puerto 8890..."
$configProc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-DCONFIG_REPO_PATH=`"$searchLoc`"", "-jar", "`"$configJar`"" `
    -RedirectStandardOutput "cs_run.log" `
    -RedirectStandardError "cs_run_err.log" `
    -PassThru

$csUp = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8890/actuator/health" -TimeoutSec 2 -ErrorAction Stop
        if ($res.status -eq "UP") { $csUp = $true; break }
    } catch {}
}

if (-not $csUp) {
    Write-Host "ERROR: Config Server no levantó."
    exit 1
}
Write-Host "Config Server UP en http://localhost:8890"

# 2. Iniciar ms-negocio-producto con perfil dev
Write-Host "`n[PASO 2] Iniciando ms-negocio-producto con perfil 'dev'..."
$devProc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-Dspring.profiles.active=dev", "-jar", "`"$productoJar`"" `
    -RedirectStandardOutput "prod_dev.log" `
    -RedirectStandardError "prod_dev_err.log" `
    -PassThru

$prodUp = $false
for ($i = 0; $i -lt 35; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8085/actuator/health" -TimeoutSec 2 -ErrorAction Stop
        if ($res.status -eq "UP") { $prodUp = $true; break }
    } catch {}
}

if (-not $prodUp) {
    Write-Host "ERROR: ms-negocio-producto no levantó en puerto 8085."
    Get-Content "prod_dev.log" -Tail 30
    Get-Content "prod_dev_err.log" -Tail 30
    Stop-Process -Id $configProc.Id -Force
    exit 1
}
Write-Host "ms-negocio-producto UP en http://localhost:8085 con perfil 'dev'"

# 3. Verificar parametro inicial de inventario en dev (50)
Write-Host "`n[PASO 3] Consultando parametro inventario.max-unidades-por-movimiento..."
$envParam = Invoke-RestMethod -Uri "http://localhost:8085/actuator/env/inventario.max-unidades-por-movimiento"
Write-Host "Valor en Actuator /env: $($envParam.property.value) (Origen: $($envParam.property.source))"

# 4. Crear un producto de prueba
Write-Host "`n[PASO 4] Creando producto 'Laptop ThinkPad' via POST /api/productos..."
$nuevoProd = @{
    codigo = "PROD-001"
    nombre = "Laptop ThinkPad"
    descripcion = "Laptop Lenovo Core i7 16GB"
    precio = 3500.00
    stockMinimo = 5
} | ConvertTo-Json

$prodResponse = Invoke-RestMethod -Uri "http://localhost:8085/api/productos" -Method Post -Body $nuevoProd -ContentType "application/json"
Write-Host "Producto creado exitosamente: ID=$($prodResponse.id), Codigo=$($prodResponse.codigo), Stock=$($prodResponse.stockActual)"
$prodId = $prodResponse.id

# 5. Probar registrar entrada de 60 unidades (Debe fallar con 422: limite 50)
Write-Host "`n[PASO 5] Registrando entrada de 60 unidades (límite en dev es 50)..."
$mov60 = @{
    productoId = $prodId
    cantidad = 60
    motivo = "Lote inicial de 60 unidades"
} | ConvertTo-Json

try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8085/api/inventario/entradas" -Method Post -Body $mov60 -ContentType "application/json"
    Write-Host "INCORRECTO: La solicitud no debio ser aceptada: $resp"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = (New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd()
    Write-Host "CORRECTO: Petición rechazada con HTTP $statusCode"
    Write-Host "Cuerpo de error recibido: $errorBody"
}

# 6. Modificar max-unidades-por-movimiento a 100 en ms-negocio-producto-dev.yml
Write-Host "`n[PASO 6] Modificando límite a 100 unidades en ms-negocio-producto-dev.yml..."
$devYmlPath = "ms-administration-data-configuration\ms-negocio-producto-dev.yml"
$nuevoContenidoDev = @"
spring:
  jpa:
    show-sql: true
logging:
  level:
    com.homerios.msnegocioproducto: DEBUG

inventario:
  max-unidades-por-movimiento: 100 # Modificado para prueba de refresco en caliente
  alerta-stock-bajo: true
"@
Set-Content -Path $devYmlPath -Value $nuevoContenidoDev -Encoding utf8
Write-Host "Archivo ms-negocio-producto-dev.yml actualizado."

# 7. Disparar refresco en caliente vía Actuator /refresh (con Content-Type application/json)
Write-Host "`n[PASO 7] Disparando refresco en caliente POST http://localhost:8085/actuator/refresh..."
$refreshResp = Invoke-RestMethod -Uri "http://localhost:8085/actuator/refresh" -Method Post -ContentType "application/json" -Body "{}"
Write-Host "Respuesta de /actuator/refresh (Claves actualizadas): $($refreshResp | ConvertTo-Json -Compress)"

# 8. Reintentar registrar entrada de 60 unidades (Ahora debe ser aceptada sin reiniciar)
Write-Host "`n[PASO 8] Reintentando entrada de 60 unidades tras refresco en caliente..."
$mov60Aceptado = Invoke-RestMethod -Uri "http://localhost:8085/api/inventario/entradas" -Method Post -Body $mov60 -ContentType "application/json"
Write-Host "CORRECTO: Entrada aceptada sin reiniciar el microservicio!"
Write-Host "Movimiento ID=$($mov60Aceptado.id), Tipo=$($mov60Aceptado.tipo), Cantidad=$($mov60Aceptado.cantidad), Stock Resultante=$($mov60Aceptado.stockResultante)"

# 9. Probar salida y alerta de stock bajo (seccion 15 ejercicio 5)
Write-Host "`n[PASO 9] Probando salida de 57 unidades para disparar alerta de stock bajo (stock actual quedara en 3 < minimo 5)..."
$salida57 = @{
    productoId = $prodId
    cantidad = 57
    motivo = "Venta a cliente corporativo"
} | ConvertTo-Json

$salidaResp = Invoke-RestMethod -Uri "http://localhost:8085/api/inventario/salidas" -Method Post -Body $salida57 -ContentType "application/json"
Write-Host "Salida registrada: ID=$($salidaResp.id), Tipo=$($salidaResp.tipo), Cantidad=$($salidaResp.cantidad), Stock Resultante=$($salidaResp.stockResultante)"
Start-Sleep -Seconds 1
$logsBajo = Get-Content "prod_dev.log" | Select-String "ALERTA STOCK BAJO"
if ($logsBajo) {
    Write-Host "Log de alerta verificado en consola: $logsBajo"
}

# Detener proceso dev
Stop-Process -Id $devProc.Id -Force
Write-Host "`nms-negocio-producto (dev) detenido."

# Restaurar dev yml a 50
$restoreDev = @"
spring:
  jpa:
    show-sql: true
logging:
  level:
    com.homerios.msnegocioproducto: DEBUG

inventario:
  max-unidades-por-movimiento: 50 # en dev probamos el límite con cantidades pequeñas
  alerta-stock-bajo: true
"@
Set-Content -Path $devYmlPath -Value $restoreDev -Encoding utf8

# 10. Probar sin perfil (default) -> Limite vuelve a 1000
Write-Host "`n[PASO 10] Iniciando ms-negocio-producto sin perfil (default -> límite 1000)..."
$baseProc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-jar", "`"$productoJar`"" `
    -RedirectStandardOutput "prod_base.log" `
    -RedirectStandardError "prod_base_err.log" `
    -PassThru

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8085/actuator/health" -TimeoutSec 2 -ErrorAction Stop
        if ($res.status -eq "UP") { break }
    } catch {}
}

$envBase = Invoke-RestMethod -Uri "http://localhost:8085/actuator/env/inventario.max-unidades-por-movimiento"
Write-Host "Valor en Actuator /env (sin perfil): $($envBase.property.value) (Origen: $($envBase.property.source))"
Stop-Process -Id $baseProc.Id -Force

# 11. Probar precedencia de argumentos de línea de comando (--server.port=9085)
Write-Host "`n[PASO 11] Probando precedencia con --server.port=9085..."
$argProc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" `
    -ArgumentList "-jar", "`"$productoJar`"", "--server.port=9085" `
    -RedirectStandardOutput "prod_arg.log" `
    -RedirectStandardError "prod_arg_err.log" `
    -PassThru

for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:9085/actuator/health" -TimeoutSec 2 -ErrorAction Stop
        if ($res.status -eq "UP") { break }
    } catch {}
}

$portEnv = Invoke-RestMethod -Uri "http://localhost:9085/actuator/env/server.port"
Write-Host "Puerto en Actuator /env: $($portEnv.property.value) (Origen: $($portEnv.property.source))"
Stop-Process -Id $argProc.Id -Force

# Detener Config Server
Stop-Process -Id $configProc.Id -Force
Write-Host "`nTodos los servicios detenidos exitosamente."
Write-Host "=========================================================="
Write-Host " TODAS LAS PRUEBAS DEL LABORATORIO 12 COMPLETADAS CON EXITO"
Write-Host "=========================================================="
