# Manual de ejecución del taller de microservicios Java

Este manual parte de Windows y PowerShell. Todos los comandos se ejecutan desde la raíz del repositorio, salvo donde se indica otra carpeta. Las contraseñas `admin` y los endpoints Actuator expuestos son solo para desarrollo local.

## 1. Requisitos

- JDK 17 disponible y `JAVA_HOME` apuntando a su carpeta. El módulo de curso también compila con Java 17.
- Maven 3.9 o posterior para `ms-negocio-producto`.
- Docker Desktop con motor Linux activo para PostgreSQL 15.
- PowerShell y conexión a internet para la primera descarga de dependencias Gradle y Maven.
- Puertos libres: 8890 (Config Server), 8889 (Eureka), 8888 (gateway), 8092 (curso), 8093 (docente), 8094 (semestre), 18085 (producto) y 55432 (PostgreSQL). En algunos equipos Windows reserva el puerto 8085; por eso los ejemplos usan 18085.

```powershell
git clone https://github.com/RicharEP/taller_microservicio_java_2025.git
cd taller_microservicio_java_2025
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'  # ajustar a su instalación
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -version
docker info --format '{{.ServerVersion}}'
```

Si Maven no está en `PATH`, sustituya `mvn` por la ruta completa a `mvn.cmd`.

## 2. Iniciar PostgreSQL y crear esquemas

El puerto 55432 evita choques con PostgreSQL local. `docker-compose.yml` crea `BD_PRODUCTO` e inicializa sus dos tablas con `BD_PRODUCTO.sql`.

```powershell
$env:PRODUCTO_DB_PORT = '55432'
docker compose up -d --wait postgres-producto
docker compose ps
docker exec postgres-producto psql -U postgres -d BD_PRODUCTO -c '\dt sys.*'
```

Para ejecutar también curso, docente y semestre, cree una vez las otras bases y aplique sus scripts. Si ya existen, omita la línea `CREATE DATABASE` correspondiente y vuelva a aplicar el SQL solo si necesita crear tablas faltantes.

```powershell
foreach ($nombre in @('CURSO', 'DOCENTE', 'SEMESTRE')) {
    docker exec postgres-producto psql -U postgres -d postgres -c "CREATE DATABASE `"BD_$nombre`";"
    docker cp "BD_$nombre.sql" "postgres-producto:/tmp/BD_$nombre.sql"
    docker exec postgres-producto psql -v ON_ERROR_STOP=1 -U postgres -d "BD_$nombre" -f "/tmp/BD_$nombre.sql"
}
```

Los scripts SQL se ejecutan **dentro** de su base correspondiente. No incluyen `DROP DATABASE` ni `CREATE DATABASE`; Docker Compose crea `BD_PRODUCTO` y el bloque anterior crea las demás.

## 3. Preparar variables de entorno

Configure estas variables en cada terminal desde el que vaya a arrancar un servicio. Ajuste la contraseña si cambió la del contenedor.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:DB_HOST = 'localhost'
$env:DB_PORT = '55432'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'admin'
$env:CONFIG_SERVER_URL = 'http://localhost:8890'
```

Para Config Server, ejecute además este comando desde la raíz del repositorio:

```powershell
$env:CONFIG_REPO_PATH = 'file:///' + (Resolve-Path .\ms-administration-data-configuration).Path.Replace('\', '/')
```

## 4. Compilar y probar

Cada módulo Gradle incluye su propio wrapper. Compile los módulos administrativos y de gestión con el JDK 17. Compilar por primera vez puede tardar porque descarga dependencias.

```powershell
foreach ($modulo in @(
    'ms-administration-server-configuration',
    'ms-administration-server-record',
    'ms-administration-gateway',
    'ms-negocio-gestion-curso',
    'ms-negocio-gestion-docente',
    'ms-negocio-gestion-semestre'
)) {
    Push-Location $modulo
    .\gradlew.bat bootJar --no-daemon
    Pop-Location
}
Push-Location ms-negocio-producto
mvn package
Pop-Location
```

Para ejecutar las suites verificadas:

```powershell
Push-Location ms-negocio-gestion-semestre
.\gradlew.bat test --no-daemon
Pop-Location
Push-Location ms-negocio-producto
mvn test
Pop-Location
```

## 5. Iniciar los servicios en orden

Abra una terminal PowerShell distinta para cada proceso. En cada una, vuelva a la raíz del repositorio y configure las variables del paso 3. Deje cada terminal abierta mientras usa el sistema.

**Terminal 1 — Config Server:**

```powershell
Push-Location ms-administration-server-configuration
.\gradlew.bat bootRun
```

Compruebe `http://localhost:8890/actuator/health` y `http://localhost:8890/ms-negocio-producto/dev`.

**Terminal 2 — Eureka:**

```powershell
Push-Location ms-administration-server-record
.\gradlew.bat bootRun
```

Compruebe `http://localhost:8889`.

**Terminales 3, 4 y 5 — servicios de negocio:** ejecute un comando en cada terminal.

```powershell
# Curso, terminal 3
Push-Location ms-negocio-gestion-curso
.\gradlew.bat bootRun
```

```powershell
# Docente, terminal 4
Push-Location ms-negocio-gestion-docente
.\gradlew.bat bootRun
```

```powershell
# Semestre, terminal 5
Push-Location ms-negocio-gestion-semestre
.\gradlew.bat bootRun
```

**Terminal 6 — producto:**

```powershell
Push-Location ms-negocio-producto
java -jar .\target\ms-negocio-producto-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=18085
```

Compruebe `http://localhost:18085/actuator/health`. En `dev`, el límite de movimiento es 50; sin `--spring.profiles.active=dev`, el límite base es 1000.

**Terminal 7 — gateway:**

```powershell
Push-Location ms-administration-gateway
.\gradlew.bat bootRun
```

Compruebe `http://localhost:8888/actuator/health`. Espere a que Eureka registre los servicios antes de probar rutas descubiertas por el gateway.

Con todos los servicios activos, estas consultas deben responder:

```powershell
Invoke-RestMethod http://localhost:8092/cursos
Invoke-RestMethod http://localhost:8093/docentes
Invoke-RestMethod http://localhost:8094/semestres
Invoke-RestMethod http://localhost:8888/ms-negocio-gestion-curso/cursos
```

`GET /curso/{id}` y `GET /docente/{id}` devuelven 404 si no existe el registro. Las altas `POST /curso` y `POST /docente` devuelven 201; los borrados `DELETE` devuelven 204 si se completan o 404 si el ID no existe.

## 6. Prueba integral automática del laboratorio 12

Detenga antes los procesos de Config Server y producto iniciados en el paso 5: el script levanta sus propias instancias y necesita los puertos 8890, 18085 y 19085 libres. Mantenga PostgreSQL activo. El script copia los YAML a una carpeta temporal, crea un producto con código único y deja logs en la ruta temporal que imprime; no trunca tablas ni cambia los YAML versionados.

```powershell
# Desde la raíz del repositorio
powershell -NoProfile -ExecutionPolicy Bypass -File .\test_full_lab12.ps1 `
    -DatabasePort 55432 -ProductPort 18085 -OverridePort 19085
```

El resultado esperado termina con `LABORATORIO 12 COMPLETADO SIN FALLOS`. La prueba verifica Config Server, perfil `dev`, rechazo HTTP 422, refresco de configuración, entradas y salidas, alerta de stock, perfil base y precedencia de argumentos de arranque.

## 7. Detener el entorno

En las terminales de los servicios, presione `Ctrl+C`. Para detener solo el contenedor PostgreSQL sin borrar datos mediante comandos de eliminación:

```powershell
docker compose stop postgres-producto
```

Al volver a arrancar con `docker compose up -d --wait postgres-producto`, compruebe si las bases opcionales siguen presentes antes de repetir el paso 2. No use `docker compose down -v` si desea conservar datos.

## Rutas importantes

| Archivo o carpeta | Uso |
| --- | --- |
| `docker-compose.yml` | PostgreSQL local |
| `BD_PRODUCTO.sql`, `BD_CURSO.sql`, `BD_DOCENTE.sql`, `BD_SEMESTRE.sql` | Esquemas de cada servicio |
| `ms-administration-data-configuration/` | Configuración central por servicio y perfil |
| `test_full_lab12.ps1` | Prueba integral del laboratorio 12 |
| `docs/INFORME_CORRECCIONES_Y_MEJORAS.docx` | Informe detallado de cambios |
| `docs/MANUAL_EJECUCION.md` | Este manual |
