param(
    [int]$DatabasePort = 55432,
    [int]$ProductPort = 18085,
    [int]$OverridePort = 19085
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Java\jdk-17' }
$java = Join-Path $javaHome 'bin\java.exe'
$configJar = Join-Path $root 'ms-administration-server-configuration\build\libs\ms-administration-server-configuration-0.0.1-SNAPSHOT.jar'
$productoJar = Join-Path $root 'ms-negocio-producto\target\ms-negocio-producto-0.0.1-SNAPSHOT.jar'
foreach ($file in @($java, $configJar, $productoJar)) {
    if (-not (Test-Path -LiteralPath $file)) { throw "Falta el archivo requerido: $file" }
}
if ($ProductPort -eq $OverridePort -or $ProductPort -eq 8890 -or $OverridePort -eq 8890) {
    throw 'Los puertos de Config Server, producto y precedencia deben ser distintos'
}
foreach ($port in @(8890, $ProductPort, $OverridePort)) {
    $client = [Net.Sockets.TcpClient]::new()
    try {
        $client.Connect('127.0.0.1', $port)
        throw "El puerto $port ya está ocupado; detenga el servicio existente antes de la prueba"
    } catch [Net.Sockets.SocketException] {
        # Conexión rechazada: no hay otro servicio escuchando.
    } finally {
        $client.Close()
    }
}

$runDir = Join-Path ([IO.Path]::GetTempPath()) ('lab12-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runDir | Out-Null
Copy-Item -Path (Join-Path $root 'ms-administration-data-configuration\*.yml') -Destination $runDir
$env:CONFIG_REPO_PATH = 'file:///' + $runDir.Replace('\', '/')
$env:DB_HOST = 'localhost'
$env:DB_PORT = [string]$DatabasePort
$env:DB_USER = 'postgres'
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = 'admin' }
$productoUrl = "http://localhost:$ProductPort"
$overrideUrl = "http://localhost:$OverridePort"

function Wait-Health([string]$url, [string]$service) {
    for ($i = 0; $i -lt 70; $i++) {
        try {
            $health = Invoke-RestMethod -Uri "$url/actuator/health" -TimeoutSec 2
            if ($health.status -eq 'UP') { return }
        } catch { }
        Start-Sleep -Seconds 1
    }
    throw "$service no llegó al estado UP en $url. Revisar los logs en $runDir"
}

function Assert-Equal($actual, $expected, [string]$label) {
    if ([string]$actual -ne [string]$expected) {
        throw "${label}: se esperaba '$expected', se recibió '$actual'"
    }
    Write-Host "OK: $label = $expected"
}

function Start-Java([string[]]$arguments, [string]$name) {
    $stdout = Join-Path $runDir "$name.log"
    $stderr = Join-Path $runDir "$name.err.log"
    return Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $root `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
}

$configProc = $null
$productoProc = $null
try {
    $configProc = Start-Java @('-jar', "`"$configJar`"") 'config-server'
    Wait-Health 'http://localhost:8890' 'Config Server'
    $remote = Invoke-RestMethod -Uri 'http://localhost:8890/ms-negocio-producto/dev'
    if (-not $remote.propertySources) { throw 'Config Server no publicó la configuración dev' }
    Write-Host 'OK: Config Server publica el perfil dev'

    $productoProc = Start-Java @('-Dspring.profiles.active=dev', '-jar', "`"$productoJar`"", "--server.port=$ProductPort") 'producto-dev'
    Wait-Health $productoUrl 'Producto dev'
    $envValue = Invoke-RestMethod -Uri "$productoUrl/actuator/env/inventario.max-unidades-por-movimiento"
    Assert-Equal $envValue.property.value 50 'Límite dev inicial'

    $codigo = 'LAB12-' + [guid]::NewGuid().ToString('N').Substring(0, 10)
    $nuevoProducto = @{ codigo = $codigo; nombre = 'Producto de prueba'; precio = 100; stockMinimo = 5 } | ConvertTo-Json
    $producto = Invoke-RestMethod -Uri "$productoUrl/api/productos" -Method Post -Body $nuevoProducto -ContentType 'application/json'
    if (-not $producto.id) { throw 'La creación del producto no devolvió un identificador' }
    Write-Host "OK: producto creado con ID $($producto.id)"

    $entrada = @{ productoId = $producto.id; cantidad = 60; motivo = 'Prueba de refresco' } | ConvertTo-Json
    $rejected = $false
    try {
        Invoke-RestMethod -Uri "$productoUrl/api/inventario/entradas" -Method Post -Body $entrada -ContentType 'application/json' | Out-Null
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        if ($status -ne 422) { throw "La entrada devolvió HTTP $status en vez de 422" }
        $rejected = $true
    }
    if (-not $rejected) { throw 'Se aceptó una entrada superior al límite dev inicial' }
    Write-Host 'OK: la entrada de 60 unidades fue rechazada con HTTP 422'

    $devConfig = Join-Path $runDir 'ms-negocio-producto-dev.yml'
    $oldContent = [IO.File]::ReadAllText($devConfig)
    $newContent = $oldContent -replace 'max-unidades-por-movimiento:\s*50', 'max-unidades-por-movimiento: 100'
    if ($newContent -eq $oldContent) { throw 'No se encontró el límite dev de 50 en la copia de configuración' }
    [IO.File]::WriteAllText($devConfig, $newContent, [Text.UTF8Encoding]::new($false))
    Invoke-RestMethod -Uri "$productoUrl/actuator/refresh" -Method Post -Body '{}' -ContentType 'application/json' | Out-Null
    $envValue = Invoke-RestMethod -Uri "$productoUrl/actuator/env/inventario.max-unidades-por-movimiento"
    Assert-Equal $envValue.property.value 100 'Límite tras refresh'

    $movimiento = Invoke-RestMethod -Uri "$productoUrl/api/inventario/entradas" -Method Post -Body $entrada -ContentType 'application/json'
    Assert-Equal $movimiento.stockResultante 60 'Stock tras entrada'
    $salida = @{ productoId = $producto.id; cantidad = 57; motivo = 'Prueba de stock bajo' } | ConvertTo-Json
    $movimiento = Invoke-RestMethod -Uri "$productoUrl/api/inventario/salidas" -Method Post -Body $salida -ContentType 'application/json'
    Assert-Equal $movimiento.stockResultante 3 'Stock tras salida'
    if (-not (Select-String -LiteralPath (Join-Path $runDir 'producto-dev.log') -Pattern 'ALERTA STOCK BAJO' -Quiet)) {
        throw 'No se encontró la alerta de stock bajo en el log'
    }
    Write-Host 'OK: alerta de stock bajo registrada'

    Stop-Process -Id $productoProc.Id -Force
    $productoProc = $null
    Start-Sleep -Seconds 2
    $productoProc = Start-Java @('-jar', "`"$productoJar`"", "--server.port=$ProductPort") 'producto-default'
    Wait-Health $productoUrl 'Producto default'
    $envValue = Invoke-RestMethod -Uri "$productoUrl/actuator/env/inventario.max-unidades-por-movimiento"
    Assert-Equal $envValue.property.value 1000 'Límite default'

    Stop-Process -Id $productoProc.Id -Force
    $productoProc = $null
    Start-Sleep -Seconds 2
    $productoProc = Start-Java @('-jar', "`"$productoJar`"", "--server.port=$OverridePort") 'producto-puerto'
    Wait-Health $overrideUrl 'Producto en puerto alternativo'
    $port = Invoke-RestMethod -Uri "$overrideUrl/actuator/env/server.port"
    Assert-Equal $port.property.value $OverridePort 'Precedencia de server.port'
    Write-Host 'LABORATORIO 12 COMPLETADO SIN FALLOS'
} finally {
    if ($productoProc -and -not $productoProc.HasExited) { Stop-Process -Id $productoProc.Id -Force }
    if ($configProc -and -not $configProc.HasExited) { Stop-Process -Id $configProc.Id -Force }
    Write-Host "Logs y configuración temporal: $runDir"
}
