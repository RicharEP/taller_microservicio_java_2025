# Taller de Microservicios Java 2025 — Sesión: Configuración y Parametrización

**Repositorio del Estudiante:** [https://github.com/RicharEP/taller_microservicio_java_2025](https://github.com/RicharEP/taller_microservicio_java_2025)  
**Autor:** Richar Estela Pimentel  
**Institución:** Universidad Nacional Agraria de la Selva (UNAS)  
**Facultad / Escuela:** Ingeniería en Informática y Sistemas  
**Año:** 2026  

---

## 1. Descripción del Laboratorio 12
El presente laboratorio corresponde al **Apartado 12: "Integrar ms-negocio-producto al Config Server"** de la guía oficial de la sesión *"Configuración y Parametrización de microservicios con Spring Cloud Config"*.

Originalmente, el microservicio `ms-negocio-producto` se encontraba aislado y autónomo, manteniendo su configuración (puerto, credenciales de PostgreSQL, configuración JPA y Actuator) acoplada internamente en su archivo `application.yml`. En este taller se implementa la separación estricta entre código y configuración (**Factor III de The Twelve-Factor App**), centralizando la parametrización en `ms-administration-server-configuration`, administrando perfiles por entorno (`default`, `dev`, `qa`, `prod`), y desacoplando reglas de negocio operativas mediante `@ConfigurationProperties` tipadas y refrescables en caliente con `/actuator/refresh`.

---

## 2. Objetivos del Proyecto
* **Centralización de Configuración:** Externalizar la configuración de `ms-negocio-producto` hacia el repositorio `ms-administration-data-configuration` servido por Spring Cloud Config Server.
* **Parametrización por Entornos:** Gestionar variables de infraestructura y reglas operativas según perfiles (`dev` con límite de 50 unidades, `default` con 1000 unidades, etc.).
* **Refresco en Caliente (Hot Reload):** Permitir la actualización dinámica de propiedades de negocio sin reiniciar el microservicio ni recompilar el artefacto `.jar`.
* **Mejoras y Buenas Prácticas:** Aplicar el principio DRY en la configuración compartida (`application.yml`), corregir propiedades desactualizadas, validar parámetros con Bean Validation (`@Min`) e implementar alertas en log ante niveles críticos de stock.

---

## 3. Tecnologías y Versiones Utilizadas
* **Java:** OpenJDK / JDK 17+ (Compilado y validado en Java 20 con bytecode target 17).
* **Spring Boot:** 3.1.3
* **Spring Cloud:** 2022.0.5 (Release Train compatible con Boot 3.1.x)
* **Gestores de Construcción:**
  * Maven 3.9.6 (en `ms-negocio-producto`)
  * Gradle 8.2.1 con Gradle Wrapper (en módulos administrativos y servicios de gestión)
* **Base de Datos:** PostgreSQL 15 (esquema `sys`, tablas `producto` y `movimiento_inventario`)
* **Contenedores:** Docker & Docker Compose para infraestructura de pruebas locales.
* **Control de Versiones:** Git y GitHub.

---

## 4. Estructura General del Proyecto
```text
taller_microservicio_java_2025/
├── docker-compose.yml                      # Orquestación de BD PostgreSQL en contenedor
├── BD_PRODUCTO.sql                         # DDL de esquema y tablas de ms-negocio-producto
├── BD_CURSO.sql, BD_DOCENTE.sql...         # DDL de los demás servicios
├── test_full_lab12.ps1                     # Script automatizado de pruebas E2E (11 pasos)
├── ms-administration-server-configuration/ # Spring Cloud Config Server (Puerto 8890)
│   ├── build.gradle
│   └── src/main/resources/application.yml  # Configuración nativa y soporte backend Git
├── ms-administration-data-configuration/   # Repositorio de configuración YAML
│   ├── application.yml                     # [MEJORA DRY] Configuración compartida (Eureka + Actuator)
│   ├── ms-negocio-producto.yml             # Configuración base centralizada de producto
│   ├── ms-negocio-producto-dev.yml         # Perfil desarrollo (límite 50 unidades, show-sql)
│   ├── ms-negocio-producto-qa.yml          # Perfil QA (límite 200 unidades)
│   ├── ms-negocio-producto-prod.yml        # Perfil producción (variables de entorno, logs INFO)
│   ├── ms-administration-server-record.yml # Configuración Eureka (keys corregidas)
│   └── ms-negocio-gestion-*.yml            # Servicios de cursos, docentes y semestres
└── ms-negocio-producto/                    # Microservicio de inventario y productos (Puerto 8085)
    ├── pom.xml                             # Spring Cloud Starter Config + BOM 2022.0.5
    └── src/main/
        ├── java/com/homerios/msnegocioproducto/
        │   ├── config/InventarioProperties.java # Bean tipado @ConfigurationProperties con setters
        │   ├── service/impl/InventarioServiceImpl.java # Reglas de negocio parametrizadas y alerta
        │   └── MsNegocioProductoApplication.java # @ConfigurationPropertiesScan
        └── resources/application.yml       # Local simplificado: name e import configserver
```

---

## 5. Descripción de los Microservicios Involucrados
1. **`ms-administration-server-configuration` (Puerto 8890):** Servidor HTTP de configuración que expone endpoints REST (`/{app}/{perfil}`, `/{app}-{perfil}.yml`) leyendo archivos YAML del sistema de archivos local (`native`) o desde GitHub (`git`).
2. **`ms-administration-data-configuration`:** Repositorio estructurado de archivos YAML versionados, organizado jerárquicamente por aplicación y perfiles.
3. **`ms-negocio-producto` (Puerto 8085):** Microservicio cliente de catálogo de productos, movimientos de kardex y control de existencias. Consume su configuración técnica y de negocio desde el Config Server.

---

## 6. Integración y Parametrización Implementada

### 6.1. Reducción del `application.yml` local
El archivo local `ms-negocio-producto/src/main/resources/application.yml` fue reducido a su mínima expresión:
```yaml
spring:
  application:
    name: ms-negocio-producto
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8890}
```

### 6.2. Configuración Centralizada en el Servidor
En `ms-administration-data-configuration/ms-negocio-producto.yml`:
* Conexión a BD PostgreSQL parametrizada con placeholders `${DB_HOST:localhost}:${DB_PORT:5432}`.
* Puerto `8085` configurado remotamente.
* Regla de negocio base: `inventario.max-unidades-por-movimiento: 1000`.
* Flag operativo: `inventario.alerta-stock-bajo: true`.

En `ms-administration-data-configuration/ms-negocio-producto-dev.yml`:
* Regla de negocio en desarrollo: `inventario.max-unidades-por-movimiento: 50`.
* Activación de `spring.jpa.show-sql: true` y logs en nivel `DEBUG`.

### 6.3. Clase JavaBean `@ConfigurationProperties`
Se creó `com.homerios.msnegocioproducto.config.InventarioProperties`:
* Uso de JavaBean con setters públicos (requisito fundamental para permitir el re-enlace de valores ante eventos `/actuator/refresh`, a diferencia de un `record` inmutable).
* Validación declarativa con `@Validated` y `@Min(1)`.

### 6.4. Validación en Lógica de Negocio y Alerta de Stock
En `InventarioServiceImpl`:
* Validación de límite máximo por movimiento: si `cantidad > inventarioProperties.getMaxUnidadesPorMovimiento()`, se lanza `ReglaNegocioException` que retorna HTTP 422 Unprocessable Entity.
* Detección de stock crítico: si se registra una `SALIDA` y `stockActual <= stockMinimo` con `alertaStockBajo == true`, se emite un `log.warn(...)`.

---

## 7. Variables de Entorno Disponibles
| Variable | Descripción | Valor por Defecto |
|---|---|---|
| `CONFIG_REPO_PATH` | Ruta absoluta o relativa al directorio de configuraciones | `file:../ms-administration-data-configuration` |
| `CONFIG_SERVER_URL` | URL del Config Server al que se conectan los clientes | `http://localhost:8890` |
| `DB_HOST` | Host del servidor PostgreSQL | `localhost` |
| `DB_PORT` | Puerto del servidor PostgreSQL | `5432` |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `admin` |
| `SPRING_PROFILES_ACTIVE` | Perfil activo de Spring (`dev`, `qa`, `prod`) | `default` |

---

## 8. Orden de Inicio y Ejecución del Sistema

### Paso 1: Iniciar Base de Datos PostgreSQL
Mediante Docker Compose:
```bash
docker compose up -d
```
O conectándose a una instancia local existente de PostgreSQL y ejecutando el script `BD_PRODUCTO.sql`.

### Paso 2: Iniciar el Config Server
```bash
cd ms-administration-server-configuration
.\gradlew.bat bootRun
```
*Endpoint de salud:* `http://localhost:8890/actuator/health`  
*Configuración servida para dev:* `http://localhost:8890/ms-negocio-producto/dev`

### Paso 3: Iniciar `ms-negocio-producto`
En perfil de desarrollo (`dev`):
```bash
cd ms-negocio-producto
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
*Endpoint de salud:* `http://localhost:8085/actuator/health`

---

## 9. Endpoints Principales

| Servicio | Método | Endpoint | Descripción |
|---|---|---|---|
| Config Server | `GET` | `/ms-negocio-producto/dev` | Retorna árbol jerárquico de fuentes de configuración |
| Config Server | `GET` | `/ms-negocio-producto-dev.yml` | Retorna YAML consolidado para perfil dev |
| Config Server | `GET` | `/actuator/health` | Estado de salud del servidor (UP) |
| Producto | `GET` | `/actuator/health` | Estado de salud del microservicio (UP) |
| Producto | `GET` | `/actuator/env/{property}` | Origen y valor efectivo del parámetro |
| Producto | `POST` | `/actuator/refresh` | Refresca en caliente los beans `@ConfigurationProperties` |
| Producto | `POST` | `/api/productos` | Registra un nuevo producto |
| Producto | `GET` | `/api/productos` | Lista todos los productos |
| Producto | `POST` | `/api/inventario/entradas` | Registra ingreso de stock (aplica validación de límite) |
| Producto | `POST` | `/api/inventario/salidas` | Registra egreso de stock (valida y emite alerta) |
| Producto | `GET` | `/api/inventario/stock/{id}` | Consulta existencias de un producto |

---

## 10. Procedimiento de Verificación del Laboratorio (11 Pasos)

El proyecto incluye el script de automatización completo `test_full_lab12.ps1` que ejecuta y valida:

1. **Arranque y verificación del Config Server:** Comprueba `HTTP 200` y `status: UP` en puerto 8890.
2. **Arranque de `ms-negocio-producto` (perfil `dev`):** Se enlaza al Config Server y obtiene el puerto 8085 y la conexión a BD.
3. **Inspección de parámetro en Actuator:** `/actuator/env/inventario.max-unidades-por-movimiento` reporta valor `50` proveniente de `ms-negocio-producto-dev.yml`.
4. **Creación de producto:** Registra `PROD-001` con stock mínimo de 5.
5. **Prueba de límite operativo:** Se intenta registrar una entrada de 60 unidades. El sistema responde `HTTP 422 Unprocessable Entity` con el mensaje:  
   `"Un movimiento no puede superar 50 unidades"`.
6. **Modificación en repositorio:** Se actualiza `max-unidades-por-movimiento: 100` en `ms-negocio-producto-dev.yml`.
7. **Refresco en caliente:** Se ejecuta `POST /actuator/refresh`. La respuesta devuelve `["inventario.max-unidades-por-movimiento"]`.
8. **Reintento sin reiniciar:** Se envía nuevamente la entrada de 60 unidades. El sistema responde `HTTP 201 Created` y actualiza el stock a 60 unidades sin haber reiniciado el proceso.
9. **Prueba de alerta de stock bajo:** Se registra una salida de 57 unidades dejando el stock en 3 (< 5). Se verifica en consola el mensaje `WARN: ALERTA STOCK BAJO`.
10. **Comprobación sin perfil (default):** Se levanta el servicio sin perfil y el límite vuelve a `1000` (definido en el archivo base).
11. **Comprobación de precedencia:** Se levanta con `--server.port=9085`. Actuator confirma que el argumento de línea de comandos prevalece sobre el Config Server (`commandLineArgs`).

Para ejecutar la suite completa:
```powershell
powershell -ExecutionPolicy Bypass -File .\test_full_lab12.ps1
```

---

## 11. Mejoras Técnicas Implementadas (Requerimiento Docente)
1. **Configuración Compartida DRY (`application.yml`):** Centralización de los bloques comunes de Eureka y Actuator en `ms-administration-data-configuration/application.yml`, eliminando redundancia en los microservicios de curso, docente y semestre.
2. **Corrección de Claves Desactualizadas:** Corrección de la propiedad `wait-time-in-ms-when-sync-empty: 0` ubicándola bajo `eureka.server` en `ms-administration-server-record.yml`.
3. **Ruta Agnóstica del Config Server:** Reemplazo de la ruta estática del docente (`D:/2025/FIIS...`) por `${CONFIG_REPO_PATH:file:../ms-administration-data-configuration}`.
4. **Soporte de Backend Git:** Parametrización en el Config Server para conmutar fácilmente entre modo nativo local y lectura directa de GitHub con el perfil `git`.
5. **Auditoría de Parámetros en Actuator:** Configuración de `management.endpoint.env.show-values: always` para inspección técnica transparente sin máscaras innecesarias en desarrollo.
6. **Contenedorización Docker:** Provisión de `docker-compose.yml` para levantar PostgreSQL con inicialización automática del esquema.
7. **Pruebas Automatizadas Unitarias:** Creación de `InventarioServiceTest.java` con JUnit 5 y Mockito para certificar el cumplimiento de las reglas de validación en memoria.

---

## 12. Publicación y Trazabilidad Git
* **Repositorio Remoto Oficial:** `https://github.com/RicharEP/taller_microservicio_java_2025`
* **Rama Principal:** `main`
* **Historial:** Conserva el commit original del repositorio base y commits incrementales documentando la integración del laboratorio 12 y las mejoras técnicas.
