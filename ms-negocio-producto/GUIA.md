# Guía paso a paso: construir `ms-negocio-producto` desde Spring Initializr

Microservicio que gestiona **productos**, **entradas/salidas** y **stock** (inventario), con arquitectura en capas y principios SOLID.

---

## 1. Crear el proyecto en https://start.spring.io/

| Campo | Valor |
|---|---|
| Project | **Maven** |
| Language | **Java** |
| Spring Boot | la **3.x** estable que ofrezca la página (este proyecto usa 3.1.3; si ya no aparece, elige la 3.x más cercana) |
| Group | `com.homerios` |
| Artifact | `ms-negocio-producto` |
| Name | `ms-negocio-producto` |
| Description | Microservicio de productos e inventario |
| Package name | `com.homerios.msnegocioproducto` |
| Packaging | **Jar** |
| Java | **17** |

**Dependencies** (botón *ADD DEPENDENCIES*):

| Dependencia | Para qué sirve |
|---|---|
| Spring Web | API REST (controladores, JSON, Tomcat embebido) |
| Spring Data JPA | Acceso a base de datos con repositorios (Hibernate) |
| Validation | Validar los datos de entrada (`@NotBlank`, `@Min`...) |
| Spring Boot Actuator | Endpoint de salud `/actuator/health` |
| PostgreSQL Driver | Conexión a PostgreSQL |

Pulsa **GENERATE**, descomprime el `.zip` y ábrelo en tu IDE. Verifica que arranque la estructura base:

```bash
mvn -q test
```

> Al generar el proyecto, Initializr crea la clase `MsNegocioProductoApplication` con `@SpringBootApplication`. Es el punto de entrada y no necesita cambios.

---

## 2. Crear la base de datos

Ejecuta el script [`../BD_PRODUCTO.sql`](../BD_PRODUCTO.sql) (pgAdmin o `psql`) en **dos pasos**:

**a) Conectado a la base `postgres`**, crea la base de datos:

```sql
CREATE DATABASE "BD_PRODUCTO"
    WITH OWNER = postgres ENCODING = 'UTF8'
    LC_COLLATE = 'Spanish_Peru.1252' LC_CTYPE = 'Spanish_Peru.1252'
    CONNECTION LIMIT = -1;
```

**b) Conectado a `BD_PRODUCTO`**, crea el esquema y las tablas:

```sql
CREATE SCHEMA IF NOT EXISTS sys;

-- Producto: el stock_actual es el saldo vigente; se modifica solo mediante movimientos.
CREATE TABLE IF NOT EXISTS sys.producto
(
    id_producto  BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(30)    NOT NULL,
    nombre       VARCHAR(150)   NOT NULL,
    descripcion  VARCHAR(255),
    precio       NUMERIC(12, 2) NOT NULL,
    stock_actual INTEGER        NOT NULL DEFAULT 0,
    stock_minimo INTEGER        NOT NULL DEFAULT 0,
    activo       BOOLEAN        NOT NULL DEFAULT TRUE,
    version      BIGINT         NOT NULL DEFAULT 0, -- bloqueo optimista
    CONSTRAINT uq_producto_codigo UNIQUE (codigo),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    CONSTRAINT ck_producto_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo >= 0)
);

-- Kardex: historial inmutable de ingresos y salidas.
CREATE TABLE IF NOT EXISTS sys.movimiento_inventario
(
    id_movimiento   BIGSERIAL PRIMARY KEY,
    id_producto     BIGINT       NOT NULL REFERENCES sys.producto (id_producto),
    tipo            VARCHAR(10)  NOT NULL,
    cantidad        INTEGER      NOT NULL,
    stock_resultante INTEGER     NOT NULL,
    motivo          VARCHAR(255),
    fecha_registro  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('ENTRADA', 'SALIDA')),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0)
);

CREATE INDEX IF NOT EXISTS ix_movimiento_producto_fecha
    ON sys.movimiento_inventario (id_producto, fecha_registro DESC);
```

El script crea la base `BD_PRODUCTO`, el esquema `sys` y dos tablas:

- `sys.producto`: datos del producto y su **stock actual** y **stock mínimo**.
- `sys.movimiento_inventario`: historial (kardex) de cada **ENTRADA** o **SALIDA**, con el stock resultante.

Las restricciones `CHECK` impiden precios o stocks negativos y cantidades de movimiento menores a 1: la base protege los datos aunque falle la aplicación.

---

## 3. Configurar la aplicación (`src/main/resources/application.yml`)

### 3.1 ¿Qué es este archivo?

`application.yml` es el archivo de **configuración externa** de Spring Boot. Se lee **una vez, al arrancar**, y define cómo se comporta la aplicación sin tocar el código Java: en qué puerto escucha, a qué base de datos se conecta, cómo trabaja Hibernate, qué endpoints de monitoreo expone.

Ideas clave:
- Vive en `src/main/resources/`, que es la carpeta de recursos que Spring Boot busca automáticamente.
- Initializr genera un `application.properties` vacío. Aquí se **renombra** a `application.yml`; ambos formatos hacen lo mismo, pero YAML es más legible porque agrupa por jerarquía.
- Separar la configuración del código permite cambiar la base de datos o el puerto **sin recompilar**.

**Cómo se lee el formato YAML:**
- La estructura se marca con **sangría de 2 espacios** (nunca tabulaciones).
- `clave: valor` define una propiedad; una clave sin valor abre un grupo.
- Los grupos anidados forman el nombre completo de la propiedad. Por ejemplo:

```yaml
spring:
  datasource:
    url: jdbc:...
```

equivale a la propiedad `spring.datasource.url` (en el `.properties` sería `spring.datasource.url=jdbc:...`).

### 3.2 El archivo completo

```yaml
server:
  port: 8085

spring:
  application:
    name: ms-negocio-producto
  datasource:
    url: jdbc:postgresql://localhost:5432/BD_PRODUCTO
    username: postgres
    password: admin
    driver-class-name: org.postgresql.Driver
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: sys

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

Tiene **tres bloques principales**: `server`, `spring` y `management`.

### 3.3 Bloque `server`: el servidor web

```yaml
server:
  port: 8085
```

Define el **puerto HTTP** en el que escucha el Tomcat embebido. Por defecto Spring Boot usa el 8080; aquí se cambia al 8085 para que no choque con los demás microservicios del taller ni con el Config Server (8890). Con esto la API queda en `http://localhost:8085`.

### 3.4 Bloque `spring.application`: identidad del servicio

```yaml
spring:
  application:
    name: ms-negocio-producto
```

El **nombre lógico** del microservicio. Por ahora solo aparece en los logs. Cuando se integre a Eureka será el nombre con el que se registre, y el API Gateway lo usará para enrutar las peticiones.

### 3.5 Bloque `spring.datasource`: conexión a la base de datos

```yaml
  datasource:
    url: jdbc:postgresql://localhost:5432/BD_PRODUCTO
    username: postgres
    password: admin
    driver-class-name: org.postgresql.Driver
```

| Propiedad | Significado |
|---|---|
| `url` | Dirección de la base de datos (ver desglose abajo) |
| `username` | Usuario de PostgreSQL |
| `password` | Contraseña del usuario |
| `driver-class-name` | Clase del driver JDBC. Spring Boot la deduce de la URL, por lo que es opcional; se deja por claridad |

**Desglose de la URL** `jdbc:postgresql://localhost:5432/BD_PRODUCTO`:

```
jdbc:postgresql://  localhost  :5432  /BD_PRODUCTO
└──── protocolo ──┘ └servidor┘ └puerto┘ └─ base de datos ─┘
```

- **JDBC** es la API estándar de Java para hablar con bases de datos; `postgresql` indica qué driver usar (el que trae la dependencia `postgresql` del `pom.xml`).
- `5432` es el puerto estándar de PostgreSQL.
- `BD_PRODUCTO` es la base creada en la sección 2; respeta las **mayúsculas exactas**.

Con estos datos Spring Boot crea automáticamente un **pool de conexiones** (HikariCP): un conjunto de conexiones abiertas que se reutilizan, en lugar de abrir una nueva en cada petición.

> ⚠️ Escribir la contraseña en el archivo es aceptable en un entorno de pruebas. En un entorno real se lee de una variable de entorno: `password: ${DB_PASSWORD}`.

### 3.6 Bloque `spring.jpa`: comportamiento de JPA e Hibernate

```yaml
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: sys
```

**`hibernate.ddl-auto: validate`**
Indica qué hace Hibernate con las tablas al arrancar:

| Valor | Efecto | Uso |
|---|---|---|
| `none` | No hace nada | Producción |
| **`validate`** | **Comprueba** que las entidades coincidan con las tablas; si falta una columna, la app no arranca y dice cuál | **Este proyecto** |
| `update` | Modifica las tablas para ajustarlas a las entidades | Solo prototipos |
| `create` / `create-drop` | Borra y recrea las tablas | Solo pruebas con datos descartables |

Aquí el esquema lo controla el script `BD_PRODUCTO.sql`, no Hibernate; con `validate` obtienes una **alerta temprana** si el código y la base se desincronizan.

**`open-in-view: false`**
Por defecto Spring mantiene la conexión a la base abierta durante toda la petición HTTP, lo que permite consultar datos *desde el controlador* sin darse cuenta. Con `false`, todo acceso a datos debe ocurrir dentro de la capa de servicio (donde está la transacción). Es lo que exige la arquitectura en capas.

**`properties.hibernate.default_schema: sys`**
Las tablas están en el esquema `sys`, no en `public`. Esta propiedad hace que las consultas lo usen por defecto. Las entidades ya lo declaran en `@Table(schema = "sys")`; esta línea es un respaldo.

> Observa que `properties.hibernate.default_schema` es una propiedad **nativa de Hibernate** que Spring simplemente le pasa; por eso va bajo `properties`, mientras que `ddl-auto` es una propiedad que Spring Boot ofrece directamente.

### 3.7 Bloque `management`: monitoreo con Actuator

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

La dependencia **Actuator** agrega endpoints para supervisar la aplicación. Esta configuración publica por HTTP **solo** dos:

| Endpoint | Para qué sirve |
|---|---|
| `/actuator/health` | Responde `{"status":"UP"}` si la app y la base de datos funcionan. Lo usan balanceadores y orquestadores para saber si el servicio está vivo |
| `/actuator/info` | Información de la aplicación (vacío por ahora) |

Limitar la lista es una medida de seguridad: otros endpoints (`env`, `beans`, `metrics`) revelan detalles internos que no deben quedar expuestos.

### 3.8 Qué NO tiene (a propósito)

Los otros microservicios (por ejemplo `ms-negocio-gestion-docente`) solo contienen:

```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8890
```

es decir, **leen toda su configuración de un Config Server**. `ms-negocio-producto` es **autónomo**: toda su configuración está en este archivo, así que arranca sin necesitar la infraestructura (Config Server, Eureka). Al integrarlo, se mueve el contenido al repositorio de configuración y este archivo queda reducido a esas dos líneas.

### 3.9 Cómo comprobar que la configuración es correcta

1. Arranca con `mvn spring-boot:run`.
2. En el log debe aparecer `Tomcat started on port(s): 8085`.
3. Abre `http://localhost:8085/actuator/health` → `{"status":"UP"}`.

Errores frecuentes:

| Mensaje en el log | Causa probable |
|---|---|
| `Connection refused` | PostgreSQL apagado o puerto incorrecto |
| `FATAL: no existe la base de datos "BD_PRODUCTO"` | No se ejecutó el script de la sección 2 (o el nombre no coincide en mayúsculas) |
| `password authentication failed` | `username` o `password` incorrectos |
| `Schema-validation: missing table/column [...]` | Las entidades no coinciden con las tablas (efecto de `validate`) |
| `Port 8085 was already in use` | Otra instancia ya corre en ese puerto: ciérrala o cambia `server.port` |

---

## 4. Conceptos y arquitectura

Antes de escribir código conviene entender **qué** se construye y **por qué** está organizado así.

### 4.1 ¿Qué es un microservicio?

Una aplicación pequeña, con **una responsabilidad de negocio** (aquí: productos e inventario), que:

- se despliega y ejecuta de forma **independiente** (su propio puerto, 8085);
- tiene **su propia base de datos** (`BD_PRODUCTO`), que ningún otro servicio toca directamente;
- se comunica con otros mediante **APIs HTTP/JSON**.

Frente a un *monolito* (una sola aplicación para todo), permite que cada equipo evolucione, despliegue y escale su servicio sin afectar a los demás. En el taller existen `gestion-curso`, `gestion-docente` y `gestion-semestre`; `ms-negocio-producto` sigue el mismo patrón, pero aún **no** se registra en Eureka ni lee del Config Server.

### 4.2 Arquitectura en capas

Separa el código por **responsabilidad técnica**. Cada capa solo habla con la inmediatamente inferior:

```
Cliente (Postman / otro servicio)
      │  JSON  (DTO)
      ▼
┌─────────────┐
│ controller  │  Puerta de entrada HTTP
└─────┬───────┘
      ▼
┌─────────────┐
│  service    │  Reglas de negocio + transacciones
└─────┬───────┘
      ▼
┌─────────────┐
│ repository  │  Acceso a datos
└─────┬───────┘
      ▼
┌─────────────┐
│ PostgreSQL  │
└─────────────┘

Transversales (las usan varias capas): model · dto · mapper · exception
```

| Capa | Pregunta que responde | Qué **sí** hace | Qué **no** debe hacer |
|---|---|---|---|
| **controller** | ¿Cómo llega y sale la petición? | Mapear rutas HTTP, validar (`@Valid`), delegar, definir el código HTTP | Reglas de negocio, consultar la BD |
| **service** | ¿Qué reglas aplican? | Validar reglas, orquestar, abrir la transacción | Conocer HTTP (`ResponseEntity`, rutas) |
| **repository** | ¿Cómo se guarda y se lee? | Consultas a la BD | Reglas de negocio |
| **model** | ¿Qué es un producto y qué puede hacer? | Estado y comportamiento del dominio (`disminuirStock`) | Depender de capas superiores |
| **dto** | ¿Qué datos viajan por la API? | Definir el contrato de entrada/salida | Contener lógica |
| **mapper** | ¿Cómo se convierte entidad ↔ DTO? | Transformar datos | Reglas de negocio |
| **exception** | ¿Cómo se comunica un error? | Representar fallos y traducirlos a HTTP | — |

**¿Por qué capas?**
- **Cambios aislados:** cambiar de PostgreSQL a otra BD toca el repositorio; cambiar de REST a mensajería toca el controlador. El servicio queda igual.
- **Pruebas fáciles:** cada capa se prueba por separado.
- **Lectura clara:** cualquier desarrollador sabe dónde buscar cada cosa.

**Regla de dependencia:** las flechas solo bajan. El controlador nunca llama al repositorio; el repositorio nunca conoce al controlador.

### 4.3 Recorrido de una petición

Ejemplo: `POST /api/inventario/salidas` con `{"productoId":1,"cantidad":4,"motivo":"Venta"}`.

```
1. Postman envía el JSON.
2. Spring (Jackson) lo convierte en un MovimientoRequest.
3. @Valid comprueba las restricciones (cantidad >= 1...). Si falla → 400.
4. InventarioController.registrarSalida() llama a inventarioService.registrarSalida().
5. InventarioServiceImpl abre una TRANSACCIÓN y:
     a. busca el Producto (ProductoRepository)       → si no existe: 404
     b. verifica que esté activo                      → si no: 422
     c. TipoMovimiento.SALIDA.aplicar(...) descuenta  → si no alcanza: 422
     d. crea y guarda el MovimientoInventario
6. Al terminar el método se hace COMMIT: se guardan el nuevo stock y el movimiento.
   Si algo falló antes, se hace ROLLBACK y no se guarda nada.
7. MovimientoMapper convierte la entidad en MovimientoResponse.
8. El controlador responde 201 Created con el JSON.
```

Si en cualquier punto se lanza una excepción, `GlobalExceptionHandler` la intercepta y responde con el JSON de error y el código HTTP adecuado.

### 4.4 Conceptos de Spring que usa el proyecto

**Inversión de control (IoC) e inyección de dependencias (DI).**
En lugar de que una clase cree sus dependencias con `new`, las **recibe** de fuera:

```java
public InventarioController(InventarioService inventarioService) {   // Spring "inyecta" la implementación
    this.inventarioService = inventarioService;
}
```

Spring crea los objetos (llamados **beans**) al arrancar y los entrega donde se necesitan. Ventajas: bajo acoplamiento, y en pruebas se puede entregar un objeto falso (*mock*).

**Estereotipos:** las anotaciones que marcan una clase como bean y expresan su rol.

| Anotación | Rol |
|---|---|
| `@RestController` | Controlador REST (devuelve JSON) |
| `@Service` | Lógica de negocio |
| `@Repository` | Acceso a datos |
| `@Component` | Componente genérico (los mappers) |
| `@RestControllerAdvice` | Manejador global de errores |

**Autoconfiguración (`@SpringBootApplication`).** Spring Boot examina las dependencias del `pom.xml` y configura todo automáticamente: si ve `spring-boot-starter-web` levanta Tomcat; si ve el driver de PostgreSQL y `spring.datasource`, crea la conexión. Por eso hay tan poco código de configuración.

**Starters.** Dependencias "paquete" (`spring-boot-starter-web`, `-data-jpa`...) que traen todas las librerías necesarias ya compatibles entre sí.

### 4.5 Persistencia: JPA, Hibernate y Spring Data

- **JPA** es el estándar de Java para mapear objetos a tablas (ORM, *Object-Relational Mapping*).
- **Hibernate** es la implementación de JPA que usa Spring Boot.
- **Spring Data JPA** genera los repositorios a partir de interfaces.

Correspondencia en este proyecto:

| Java | Base de datos |
|---|---|
| Clase `Producto` con `@Entity` | Tabla `sys.producto` |
| Atributo `stockActual` con `@Column(name="stock_actual")` | Columna `stock_actual` |
| Objeto `Producto` | Una fila |
| `ProductoRepository.save(p)` | `INSERT` o `UPDATE` |
| `findByActivoTrue()` | `SELECT ... WHERE activo = true` |

**Dirty checking:** dentro de una transacción, si modificas una entidad cargada (`producto.disminuirStock(4)`), Hibernate detecta el cambio y emite el `UPDATE` al hacer commit, sin llamar a `save`. Por eso `actualizar(...)` y `desactivar(...)` no invocan al repositorio.

**Carga perezosa (`FetchType.LAZY`):** `MovimientoInventario` no carga el `Producto` hasta que se usa. Por eso se desactiva `open-in-view` y todo acceso a datos ocurre dentro de la capa de servicio.

### 4.6 DTO frente a entidad

| | Entidad (`Producto`) | DTO (`ProductoResponse`) |
|---|---|---|
| Representa | La tabla / el dominio | El contrato de la API |
| Quién la usa | Servicios y repositorios | Controladores y clientes |
| Puede cambiar por | Cambios en la BD o el negocio | Cambios en lo que la API ofrece |

Exponer la entidad directamente acopla la API a la base de datos y permite que el cliente modifique campos que no debería (por ejemplo `stockActual`). Con DTOs **tú decides** qué entra y qué sale.

### 4.7 API REST y códigos HTTP

REST modela **recursos** (`/api/productos`) y usa los verbos HTTP para las acciones:

| Verbo | Acción | Ejemplo |
|---|---|---|
| GET | Leer | `GET /api/productos/1` |
| POST | Crear / ejecutar una operación | `POST /api/inventario/entradas` |
| PUT | Reemplazar / actualizar | `PUT /api/productos/1` |
| DELETE | Eliminar / desactivar | `DELETE /api/productos/1` |

| Código | Significado | Cuándo lo usa este proyecto |
|---|---|---|
| 200 OK | Éxito | Consultas y actualizaciones |
| 201 Created | Recurso creado | Crear producto, registrar movimiento |
| 204 No Content | Éxito sin cuerpo | Desactivar producto |
| 400 Bad Request | Petición mal formada o inválida | Validación o JSON incorrecto |
| 404 Not Found | No existe | Producto inexistente |
| 409 Conflict | Choque de concurrencia | Dos operaciones simultáneas sobre el mismo producto |
| 422 Unprocessable Entity | Datos válidos pero viola una regla | Stock insuficiente, código duplicado |

### 4.8 Transacciones

Una **transacción** agrupa varias operaciones en una unidad **todo o nada** (propiedad *atomicidad*). Una salida de inventario hace dos cambios: descontar el stock y registrar el movimiento. Si el segundo falla, el primero se **revierte** (*rollback*); nunca queda un descuento sin su registro. `@Transactional` en la capa de servicio lo logra. `readOnly = true` en las consultas indica que no modificarán datos y permite optimizar.

### 4.9 Validación en tres niveles

1. **API (Bean Validation):** `@NotBlank`, `@Min(1)`, `@DecimalMin`... rechazan datos mal formados en la puerta, antes de llegar al servicio.
2. **Dominio:** `Producto.disminuirStock` rechaza salidas mayores al stock, sin importar quién lo invoque.
3. **Base de datos:** las restricciones `CHECK` y `UNIQUE` son la última red de seguridad.

Es *defensa en profundidad*: cada nivel protege lo que le corresponde.

### 4.10 Concurrencia: bloqueo optimista

Si dos usuarios sacan stock del mismo producto al mismo tiempo, podrían leer ambos "10", restar cada uno y guardar, perdiendo un descuento. El campo `@Version` evita esto: cada `UPDATE` incluye `WHERE version = ?` e incrementa la versión. La segunda operación encuentra la versión cambiada, falla con `ObjectOptimisticLockingFailureException` y el cliente recibe **409**, pudiendo reintentar. Se llama *optimista* porque no bloquea filas: asume que los conflictos son raros y los detecta si ocurren.

### 4.11 Anatomía del proyecto

```
ms-negocio-producto/
├── pom.xml                      ← dependencias y versión de Java/Spring Boot (Maven)
├── GUIA.md
├── postman/                     ← colección de pruebas
└── src/
    ├── main/
    │   ├── java/com/homerios/msnegocioproducto/
    │   │   ├── MsNegocioProductoApplication.java   ← punto de entrada (main)
    │   │   ├── controller/   ProductoController, InventarioController
    │   │   ├── service/      ProductoService, InventarioService   (interfaces)
    │   │   │   └── impl/     ProductoServiceImpl, InventarioServiceImpl
    │   │   ├── repository/   ProductoRepository, MovimientoInventarioRepository
    │   │   ├── model/        Producto, MovimientoInventario, TipoMovimiento
    │   │   ├── dto/          *Request, *Response, ErrorResponse
    │   │   ├── mapper/       ProductoMapper, MovimientoMapper
    │   │   └── exception/    excepciones + GlobalExceptionHandler
    │   └── resources/
    │       └── application.yml                     ← configuración (puerto, BD, JPA)
    └── test/java/.../ProductoTest.java             ← pruebas unitarias
```

- **`pom.xml`**: declara las dependencias; Maven las descarga. El `spring-boot-starter-parent` fija versiones compatibles.
- **`MsNegocioProductoApplication`**: contiene `main(...)`. `@SpringBootApplication` activa la autoconfiguración y el escaneo de componentes de **su paquete y subpaquetes** (por eso todo cuelga de `com.homerios.msnegocioproducto`).
- **`application.yml`**: configuración externa leída al arrancar.
- **`src/test`**: pruebas, separadas del código de producción.

---

## 5. Construcción paso a paso (código completo)

Crea los paquetes y las clases **en este orden**, de adentro hacia afuera, para que cada paso compile con lo anterior. Todas las clases están dentro del paquete base `com.homerios.msnegocioproducto`; los `package` e `import` de cada archivo ya están incluidos.

> Tip: tras cada paso ejecuta `mvn -q compile`. Los errores aparecen cuando aún no creaste una clase referenciada; se resuelven al terminar el paso siguiente.

### Paso 5.1: Excepciones de negocio (`exception`)

> 💡 **Concepto: excepciones como flujo de error.** En vez de devolver códigos o `null`, el servicio *lanza* una excepción que describe el problema. Así el flujo normal queda limpio y el manejo de errores se concentra en un solo lugar (paso 5.8). Heredan de `RuntimeException` (no verificadas), por lo que no obligan a declarar `throws` en cada método.

Se crean primero porque la entidad `Producto` lanza `StockInsuficienteException`. Crea el paquete `exception`.

`ReglaNegocioException` es la base de todas las violaciones de reglas; `StockInsuficienteException` la **hereda** (principio L de SOLID).

📄 `com/homerios/msnegocioproducto/exception/RecursoNoEncontradoException.java`

```java
package com.homerios.msnegocioproducto.exception;

public class RecursoNoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RecursoNoEncontradoException(String recurso, Long id) {
		super(recurso + " con id " + id + " no existe");
	}
}
```

📄 `com/homerios/msnegocioproducto/exception/ReglaNegocioException.java`

```java
package com.homerios.msnegocioproducto.exception;

/** Violación de una regla de negocio (p. ej. código duplicado, producto inactivo). */
public class ReglaNegocioException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}
}
```

📄 `com/homerios/msnegocioproducto/exception/StockInsuficienteException.java`

```java
package com.homerios.msnegocioproducto.exception;

public class StockInsuficienteException extends ReglaNegocioException {

	private static final long serialVersionUID = 1L;

	public StockInsuficienteException(String codigo, int disponible, int solicitado) {
		super("Stock insuficiente para el producto " + codigo + ": disponible " + disponible + ", solicitado "
				+ solicitado);
	}
}
```

---

### Paso 5.2: Entidades (`model`)

> 💡 **Concepto: entidad rica.** Una entidad *anémica* solo tiene datos y getters/setters; toda la lógica queda en los servicios. Aquí `Producto` protege sus propias reglas: `disminuirStock` impide dejar el stock negativo. Los atributos son privados, el constructor `protected` sin argumentos es solo para JPA, y no hay `setStockActual`: **la única forma de cambiar el stock es a través de movimientos**. `@Id` + `GenerationType.IDENTITY` delega el id a la columna autoincremental de PostgreSQL.

Crea el paquete `model`. Aquí vive la **lógica del dominio**.

- `TipoMovimiento`: enum con comportamiento (principio O). Cada tipo sabe cómo afectar el stock.
- `Producto`: sin setters públicos; el stock solo cambia con `incrementarStock`/`disminuirStock`. `@Version` da bloqueo optimista.
- `MovimientoInventario`: registro inmutable del kardex (`updatable = false`).

> Orden sugerido: `TipoMovimiento` → `Producto` → `MovimientoInventario`. El IDE marcará error en `TipoMovimiento` hasta crear `Producto`; es normal.

📄 `com/homerios/msnegocioproducto/model/TipoMovimiento.java`

```java
package com.homerios.msnegocioproducto.model;

/**
 * Tipo de movimiento de inventario. Cada tipo sabe cómo afectar el stock,
 * de modo que agregar un nuevo tipo (p. ej. AJUSTE) no modifica el servicio.
 */
public enum TipoMovimiento {

	ENTRADA {
		@Override
		public void aplicar(Producto producto, int cantidad) {
			producto.incrementarStock(cantidad);
		}
	},
	SALIDA {
		@Override
		public void aplicar(Producto producto, int cantidad) {
			producto.disminuirStock(cantidad);
		}
	};

	public abstract void aplicar(Producto producto, int cantidad);
}
```

📄 `com/homerios/msnegocioproducto/model/Producto.java`

```java
package com.homerios.msnegocioproducto.model;

import java.math.BigDecimal;

import com.homerios.msnegocioproducto.exception.StockInsuficienteException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "producto", schema = "sys")
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String codigo;

	@Column(nullable = false, length = 150)
	private String nombre;

	@Column(length = 255)
	private String descripcion;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal precio;

	@Column(name = "stock_actual", nullable = false)
	private int stockActual;

	@Column(name = "stock_minimo", nullable = false)
	private int stockMinimo;

	@Column(nullable = false)
	private boolean activo = true;

	@Version
	private long version;

	protected Producto() {
		// requerido por JPA
	}

	public Producto(String codigo, String nombre, String descripcion, BigDecimal precio, int stockMinimo) {
		this.codigo = codigo;
		actualizarDatos(nombre, descripcion, precio, stockMinimo);
	}

	public void actualizarDatos(String nombre, String descripcion, BigDecimal precio, int stockMinimo) {
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.precio = precio;
		this.stockMinimo = stockMinimo;
	}

	public void incrementarStock(int cantidad) {
		this.stockActual += cantidad;
	}

	public void disminuirStock(int cantidad) {
		if (cantidad > stockActual) {
			throw new StockInsuficienteException(codigo, stockActual, cantidad);
		}
		this.stockActual -= cantidad;
	}

	public boolean tieneStockBajo() {
		return stockActual <= stockMinimo;
	}

	public void desactivar() {
		this.activo = false;
	}

	public Long getId() {
		return id;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public int getStockActual() {
		return stockActual;
	}

	public int getStockMinimo() {
		return stockMinimo;
	}

	public boolean isActivo() {
		return activo;
	}
}
```

📄 `com/homerios/msnegocioproducto/model/MovimientoInventario.java`

```java
package com.homerios.msnegocioproducto.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Registro inmutable del kardex: una entrada o salida de un producto. */
@Entity
@Table(name = "movimiento_inventario", schema = "sys")
public class MovimientoInventario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_producto", nullable = false, updatable = false)
	private Producto producto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 10)
	private TipoMovimiento tipo;

	@Column(nullable = false, updatable = false)
	private int cantidad;

	@Column(name = "stock_resultante", nullable = false, updatable = false)
	private int stockResultante;

	@Column(length = 255, updatable = false)
	private String motivo;

	@Column(name = "fecha_registro", nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;

	protected MovimientoInventario() {
		// requerido por JPA
	}

	public MovimientoInventario(Producto producto, TipoMovimiento tipo, int cantidad, String motivo) {
		this.producto = producto;
		this.tipo = tipo;
		this.cantidad = cantidad;
		this.stockResultante = producto.getStockActual();
		this.motivo = motivo;
		this.fechaRegistro = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Producto getProducto() {
		return producto;
	}

	public TipoMovimiento getTipo() {
		return tipo;
	}

	public int getCantidad() {
		return cantidad;
	}

	public int getStockResultante() {
		return stockResultante;
	}

	public String getMotivo() {
		return motivo;
	}

	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}
}
```

---

### Paso 5.3: Repositorios (`repository`)

> 💡 **Concepto: repositorio.** Es la abstracción de "una colección de entidades guardadas". `JpaRepository<Producto, Long>` ya trae `save`, `findById`, `findAll`, `deleteById`... (`Long` es el tipo del id). Solo declaras lo extra: Spring interpreta `existsByCodigo` como `SELECT ... WHERE codigo = ?`.

Basta con interfaces: Spring Data genera la implementación. `findByProductoIdOrderByFechaRegistroDesc` se construye **a partir del nombre del método**; `findConStockBajo` usa JPQL.

📄 `com/homerios/msnegocioproducto/repository/ProductoRepository.java`

```java
package com.homerios.msnegocioproducto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.homerios.msnegocioproducto.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

	boolean existsByCodigo(String codigo);

	List<Producto> findByActivoTrue();

	@Query("select p from Producto p where p.activo = true and p.stockActual <= p.stockMinimo order by p.stockActual")
	List<Producto> findConStockBajo();
}
```

📄 `com/homerios/msnegocioproducto/repository/MovimientoInventarioRepository.java`

```java
package com.homerios.msnegocioproducto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.homerios.msnegocioproducto.model.MovimientoInventario;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

	List<MovimientoInventario> findByProductoIdOrderByFechaRegistroDesc(Long idProducto);
}
```

---

### Paso 5.4: DTOs (`dto`)

> 💡 **Concepto: record y Bean Validation.** Un `record` es una clase inmutable que genera constructor, getters (`codigo()`), `equals` y `toString` automáticamente, ideal para transportar datos. Las anotaciones de `jakarta.validation` declaran las reglas junto al campo; no hacen nada por sí solas hasta que el controlador usa `@Valid`.

Los `record` definen el contrato de la API. **Nunca se exponen las entidades.** Las anotaciones (`@NotBlank`, `@Min`...) se activan con `@Valid` en el controlador.

Observa que `ProductoRequest` **no tiene** `stockActual`: el stock solo se modifica con movimientos.

📄 `com/homerios/msnegocioproducto/dto/ProductoRequest.java`

```java
package com.homerios.msnegocioproducto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
		@NotBlank @Size(max = 30) String codigo,
		@NotBlank @Size(max = 150) String nombre,
		@Size(max = 255) String descripcion,
		@NotNull @DecimalMin("0.00") BigDecimal precio,
		@Min(0) int stockMinimo) {
}
```

📄 `com/homerios/msnegocioproducto/dto/ProductoResponse.java`

```java
package com.homerios.msnegocioproducto.dto;

import java.math.BigDecimal;

public record ProductoResponse(
		Long id,
		String codigo,
		String nombre,
		String descripcion,
		BigDecimal precio,
		int stockActual,
		int stockMinimo,
		boolean stockBajo,
		boolean activo) {
}
```

📄 `com/homerios/msnegocioproducto/dto/MovimientoRequest.java`

```java
package com.homerios.msnegocioproducto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimientoRequest(
		@NotNull Long productoId,
		@Min(1) int cantidad,
		@Size(max = 255) String motivo) {
}
```

📄 `com/homerios/msnegocioproducto/dto/MovimientoResponse.java`

```java
package com.homerios.msnegocioproducto.dto;

import java.time.LocalDateTime;

import com.homerios.msnegocioproducto.model.TipoMovimiento;

public record MovimientoResponse(
		Long id,
		Long productoId,
		String codigoProducto,
		TipoMovimiento tipo,
		int cantidad,
		int stockResultante,
		String motivo,
		LocalDateTime fechaRegistro) {
}
```

📄 `com/homerios/msnegocioproducto/dto/StockResponse.java`

```java
package com.homerios.msnegocioproducto.dto;

public record StockResponse(
		Long productoId,
		String codigo,
		String nombre,
		int stockActual,
		int stockMinimo,
		boolean stockBajo) {
}
```

📄 `com/homerios/msnegocioproducto/dto/ErrorResponse.java`

```java
package com.homerios.msnegocioproducto.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
		LocalDateTime timestamp,
		int status,
		String mensaje,
		List<String> detalles) {

	public static ErrorResponse of(int status, String mensaje) {
		return new ErrorResponse(LocalDateTime.now(), status, mensaje, List.of());
	}

	public static ErrorResponse of(int status, String mensaje, List<String> detalles) {
		return new ErrorResponse(LocalDateTime.now(), status, mensaje, detalles);
	}
}
```

---

### Paso 5.5: Mappers (`mapper`)

> 💡 **Concepto: mapper.** Evita escribir la conversión en cada servicio o controlador. En proyectos grandes suele generarse con librerías como MapStruct; aquí se escribe a mano para que el taller sea transparente.

Convierten entidad ↔ DTO. Separar esta tarea mantiene limpios al servicio y al controlador (principio S).

📄 `com/homerios/msnegocioproducto/mapper/ProductoMapper.java`

```java
package com.homerios.msnegocioproducto.mapper;

import org.springframework.stereotype.Component;

import com.homerios.msnegocioproducto.dto.ProductoRequest;
import com.homerios.msnegocioproducto.dto.ProductoResponse;
import com.homerios.msnegocioproducto.dto.StockResponse;
import com.homerios.msnegocioproducto.model.Producto;

@Component
public class ProductoMapper {

	public Producto toEntity(ProductoRequest request) {
		return new Producto(request.codigo(), request.nombre(), request.descripcion(), request.precio(),
				request.stockMinimo());
	}

	public ProductoResponse toResponse(Producto p) {
		return new ProductoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
				p.getStockActual(), p.getStockMinimo(), p.tieneStockBajo(), p.isActivo());
	}

	public StockResponse toStockResponse(Producto p) {
		return new StockResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getStockActual(), p.getStockMinimo(),
				p.tieneStockBajo());
	}
}
```

📄 `com/homerios/msnegocioproducto/mapper/MovimientoMapper.java`

```java
package com.homerios.msnegocioproducto.mapper;

import org.springframework.stereotype.Component;

import com.homerios.msnegocioproducto.dto.MovimientoResponse;
import com.homerios.msnegocioproducto.model.MovimientoInventario;

@Component
public class MovimientoMapper {

	public MovimientoResponse toResponse(MovimientoInventario m) {
		return new MovimientoResponse(m.getId(), m.getProducto().getId(), m.getProducto().getCodigo(), m.getTipo(),
				m.getCantidad(), m.getStockResultante(), m.getMotivo(), m.getFechaRegistro());
	}
}
```

---

### Paso 5.6: Interfaces de servicio (`service`)

> 💡 **Concepto: programar contra interfaces.** La interfaz es el *contrato*: qué operaciones existen. La implementación es el *cómo*. Los consumidores (controladores) solo conocen el contrato, lo que permite cambiar o simular la implementación sin tocarlos.

Definen **qué** se puede hacer, no **cómo**. Son dos interfaces pequeñas (principio I) y los controladores dependerán de ellas (principio D).

📄 `com/homerios/msnegocioproducto/service/ProductoService.java`

```java
package com.homerios.msnegocioproducto.service;

import java.util.List;

import com.homerios.msnegocioproducto.dto.ProductoRequest;
import com.homerios.msnegocioproducto.dto.ProductoResponse;

public interface ProductoService {

	List<ProductoResponse> listar();

	ProductoResponse buscarPorId(Long id);

	ProductoResponse crear(ProductoRequest request);

	ProductoResponse actualizar(Long id, ProductoRequest request);

	/** Baja lógica: conserva el historial de movimientos. */
	void desactivar(Long id);
}
```

📄 `com/homerios/msnegocioproducto/service/InventarioService.java`

```java
package com.homerios.msnegocioproducto.service;

import java.util.List;

import com.homerios.msnegocioproducto.dto.MovimientoRequest;
import com.homerios.msnegocioproducto.dto.MovimientoResponse;
import com.homerios.msnegocioproducto.dto.StockResponse;

public interface InventarioService {

	MovimientoResponse registrarEntrada(MovimientoRequest request);

	MovimientoResponse registrarSalida(MovimientoRequest request);

	StockResponse consultarStock(Long productoId);

	List<StockResponse> listarStockBajo();

	List<MovimientoResponse> listarMovimientos(Long productoId);
}
```

---

### Paso 5.7: Implementación de los servicios (`service/impl`)

> 💡 **Concepto: capa de servicio.** Es el "cerebro" de la aplicación: coordina repositorios, aplica reglas y define los límites de la transacción. `@Transactional` a nivel de clase cubre todos sus métodos públicos. Observa que `actualizar` y `desactivar` no llaman a `save`: gracias al *dirty checking* (4.5) Hibernate persiste los cambios al hacer commit.

Contienen las reglas de negocio. `@Transactional` garantiza que **el cambio de stock y el registro del movimiento se guardan juntos o no se guarda nada**; las consultas usan `readOnly = true`.

Fíjate en `registrar(...)`: sirve para ENTRADA y SALIDA sin `if` por tipo, delegando en `tipo.aplicar(...)`.

📄 `com/homerios/msnegocioproducto/service/impl/ProductoServiceImpl.java`

```java
package com.homerios.msnegocioproducto.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homerios.msnegocioproducto.dto.ProductoRequest;
import com.homerios.msnegocioproducto.dto.ProductoResponse;
import com.homerios.msnegocioproducto.exception.RecursoNoEncontradoException;
import com.homerios.msnegocioproducto.exception.ReglaNegocioException;
import com.homerios.msnegocioproducto.mapper.ProductoMapper;
import com.homerios.msnegocioproducto.model.Producto;
import com.homerios.msnegocioproducto.repository.ProductoRepository;
import com.homerios.msnegocioproducto.service.ProductoService;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

	private final ProductoRepository productoRepository;
	private final ProductoMapper productoMapper;

	public ProductoServiceImpl(ProductoRepository productoRepository, ProductoMapper productoMapper) {
		this.productoRepository = productoRepository;
		this.productoMapper = productoMapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductoResponse> listar() {
		return productoRepository.findByActivoTrue().stream().map(productoMapper::toResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProductoResponse buscarPorId(Long id) {
		return productoMapper.toResponse(obtener(id));
	}

	@Override
	public ProductoResponse crear(ProductoRequest request) {
		if (productoRepository.existsByCodigo(request.codigo())) {
			throw new ReglaNegocioException("Ya existe un producto con el código " + request.codigo());
		}
		Producto guardado = productoRepository.save(productoMapper.toEntity(request));
		return productoMapper.toResponse(guardado);
	}

	@Override
	public ProductoResponse actualizar(Long id, ProductoRequest request) {
		Producto producto = obtener(id);
		if (!producto.getCodigo().equals(request.codigo())) {
			throw new ReglaNegocioException("El código del producto no puede modificarse");
		}
		producto.actualizarDatos(request.nombre(), request.descripcion(), request.precio(), request.stockMinimo());
		return productoMapper.toResponse(producto);
	}

	@Override
	public void desactivar(Long id) {
		obtener(id).desactivar();
	}

	private Producto obtener(Long id) {
		return productoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
	}
}
```

📄 `com/homerios/msnegocioproducto/service/impl/InventarioServiceImpl.java`

```java
package com.homerios.msnegocioproducto.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homerios.msnegocioproducto.dto.MovimientoRequest;
import com.homerios.msnegocioproducto.dto.MovimientoResponse;
import com.homerios.msnegocioproducto.dto.StockResponse;
import com.homerios.msnegocioproducto.exception.RecursoNoEncontradoException;
import com.homerios.msnegocioproducto.exception.ReglaNegocioException;
import com.homerios.msnegocioproducto.mapper.MovimientoMapper;
import com.homerios.msnegocioproducto.mapper.ProductoMapper;
import com.homerios.msnegocioproducto.model.MovimientoInventario;
import com.homerios.msnegocioproducto.model.Producto;
import com.homerios.msnegocioproducto.model.TipoMovimiento;
import com.homerios.msnegocioproducto.repository.MovimientoInventarioRepository;
import com.homerios.msnegocioproducto.repository.ProductoRepository;
import com.homerios.msnegocioproducto.service.InventarioService;

@Service
@Transactional
public class InventarioServiceImpl implements InventarioService {

	private final ProductoRepository productoRepository;
	private final MovimientoInventarioRepository movimientoRepository;
	private final ProductoMapper productoMapper;
	private final MovimientoMapper movimientoMapper;

	public InventarioServiceImpl(ProductoRepository productoRepository,
			MovimientoInventarioRepository movimientoRepository, ProductoMapper productoMapper,
			MovimientoMapper movimientoMapper) {
		this.productoRepository = productoRepository;
		this.movimientoRepository = movimientoRepository;
		this.productoMapper = productoMapper;
		this.movimientoMapper = movimientoMapper;
	}

	@Override
	public MovimientoResponse registrarEntrada(MovimientoRequest request) {
		return registrar(TipoMovimiento.ENTRADA, request);
	}

	@Override
	public MovimientoResponse registrarSalida(MovimientoRequest request) {
		return registrar(TipoMovimiento.SALIDA, request);
	}

	@Override
	@Transactional(readOnly = true)
	public StockResponse consultarStock(Long productoId) {
		return productoMapper.toStockResponse(obtenerProducto(productoId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StockResponse> listarStockBajo() {
		return productoRepository.findConStockBajo().stream().map(productoMapper::toStockResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MovimientoResponse> listarMovimientos(Long productoId) {
		obtenerProducto(productoId);
		return movimientoRepository.findByProductoIdOrderByFechaRegistroDesc(productoId).stream()
				.map(movimientoMapper::toResponse).toList();
	}

	private MovimientoResponse registrar(TipoMovimiento tipo, MovimientoRequest request) {
		Producto producto = obtenerProducto(request.productoId());
		if (!producto.isActivo()) {
			throw new ReglaNegocioException("El producto " + producto.getCodigo() + " está inactivo");
		}
		tipo.aplicar(producto, request.cantidad());
		MovimientoInventario movimiento = new MovimientoInventario(producto, tipo, request.cantidad(),
				request.motivo());
		return movimientoMapper.toResponse(movimientoRepository.save(movimiento));
	}

	private Producto obtenerProducto(Long id) {
		return productoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
	}
}
```

---

### Paso 5.8: Manejo global de errores (`exception`)

> 💡 **Concepto: manejo centralizado de errores.** Sin esto, cada controlador tendría `try/catch` repetidos y Spring devolvería respuestas de error con formatos distintos. `@RestControllerAdvice` actúa como "red" global: cualquier excepción que escape de un controlador es capturada por el método `@ExceptionHandler` correspondiente.

`@RestControllerAdvice` captura las excepciones de cualquier controlador y las convierte en un `ErrorResponse` uniforme. Así los servicios lanzan excepciones y **no** construyen respuestas HTTP.

| Excepción | HTTP |
|---|---|
| `RecursoNoEncontradoException` | 404 |
| `ReglaNegocioException` / `StockInsuficienteException` | 422 |
| `MethodArgumentNotValidException` | 400 |
| `HttpMessageNotReadableException` | 400 |
| `ObjectOptimisticLockingFailureException` | 409 |

📄 `com/homerios/msnegocioproducto/exception/GlobalExceptionHandler.java`

```java
package com.homerios.msnegocioproducto.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.homerios.msnegocioproducto.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
		return responder(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException ex) {
		return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> conflictoConcurrencia() {
		return responder(HttpStatus.CONFLICT, "El producto fue modificado por otra operación, reintente");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
		List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
		return ResponseEntity.badRequest().body(ErrorResponse.of(400, "Datos de entrada inválidos", detalles));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> cuerpoIlegible() {
		return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido en UTF-8");
	}

	private ResponseEntity<ErrorResponse> responder(HttpStatus status, String mensaje) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), mensaje));
	}
}
```

---

### Paso 5.9: Controladores (`controller`)

> 💡 **Concepto: controlador delgado.** Su trabajo es *traducir* HTTP: `@RequestMapping` define la ruta base, `@GetMapping`/`@PostMapping`... el verbo, `@PathVariable` toma valores de la URL, `@RequestBody` convierte el JSON en objeto y `@ResponseStatus` fija el código de respuesta. Cero lógica de negocio.

Solo reciben la petición, validan con `@Valid` y delegan al servicio. Inyección **por constructor** con campos `final`.

📄 `com/homerios/msnegocioproducto/controller/ProductoController.java`

```java
package com.homerios.msnegocioproducto.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.homerios.msnegocioproducto.dto.ProductoRequest;
import com.homerios.msnegocioproducto.dto.ProductoResponse;
import com.homerios.msnegocioproducto.service.ProductoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

	private final ProductoService productoService;

	public ProductoController(ProductoService productoService) {
		this.productoService = productoService;
	}

	@GetMapping
	public List<ProductoResponse> listar() {
		return productoService.listar();
	}

	@GetMapping("/{id}")
	public ProductoResponse buscar(@PathVariable Long id) {
		return productoService.buscarPorId(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
		return productoService.crear(request);
	}

	@PutMapping("/{id}")
	public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
		return productoService.actualizar(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desactivar(@PathVariable Long id) {
		productoService.desactivar(id);
	}
}
```

📄 `com/homerios/msnegocioproducto/controller/InventarioController.java`

```java
package com.homerios.msnegocioproducto.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.homerios.msnegocioproducto.dto.MovimientoRequest;
import com.homerios.msnegocioproducto.dto.MovimientoResponse;
import com.homerios.msnegocioproducto.dto.StockResponse;
import com.homerios.msnegocioproducto.service.InventarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

	private final InventarioService inventarioService;

	public InventarioController(InventarioService inventarioService) {
		this.inventarioService = inventarioService;
	}

	@PostMapping("/entradas")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoResponse registrarEntrada(@Valid @RequestBody MovimientoRequest request) {
		return inventarioService.registrarEntrada(request);
	}

	@PostMapping("/salidas")
	@ResponseStatus(HttpStatus.CREATED)
	public MovimientoResponse registrarSalida(@Valid @RequestBody MovimientoRequest request) {
		return inventarioService.registrarSalida(request);
	}

	@GetMapping("/stock/{productoId}")
	public StockResponse consultarStock(@PathVariable Long productoId) {
		return inventarioService.consultarStock(productoId);
	}

	@GetMapping("/stock-bajo")
	public List<StockResponse> listarStockBajo() {
		return inventarioService.listarStockBajo();
	}

	@GetMapping("/movimientos/{productoId}")
	public List<MovimientoResponse> listarMovimientos(@PathVariable Long productoId) {
		return inventarioService.listarMovimientos(productoId);
	}
}
```

---

### Paso 5.10: Pruebas unitarias

> 💡 **Concepto: prueba unitaria.** Verifica una unidad pequeña de forma aislada, rápida y sin base de datos ni Spring. Es posible porque la lógica está en `Producto` y `TipoMovimiento` (dominio rico), no mezclada con la infraestructura.

La lógica de dominio se prueba **sin levantar Spring**, por eso son rápidas. Crea la clase en `src/test/java/com/homerios/msnegocioproducto/`.

```java
package com.homerios.msnegocioproducto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.homerios.msnegocioproducto.exception.StockInsuficienteException;
import com.homerios.msnegocioproducto.model.Producto;
import com.homerios.msnegocioproducto.model.TipoMovimiento;

class ProductoTest {

	private Producto nuevoProducto() {
		return new Producto("P-001", "Laptop", null, new BigDecimal("2500.00"), 2);
	}

	@Test
	void entradaIncrementaStock() {
		Producto p = nuevoProducto();
		TipoMovimiento.ENTRADA.aplicar(p, 10);
		assertEquals(10, p.getStockActual());
	}

	@Test
	void salidaDisminuyeStock() {
		Producto p = nuevoProducto();
		TipoMovimiento.ENTRADA.aplicar(p, 10);
		TipoMovimiento.SALIDA.aplicar(p, 4);
		assertEquals(6, p.getStockActual());
	}

	@Test
	void salidaMayorAlStockLanzaExcepcion() {
		Producto p = nuevoProducto();
		TipoMovimiento.ENTRADA.aplicar(p, 3);
		assertThrows(StockInsuficienteException.class, () -> TipoMovimiento.SALIDA.aplicar(p, 4));
		assertEquals(3, p.getStockActual());
	}

	@Test
	void detectaStockBajo() {
		Producto p = nuevoProducto();
		TipoMovimiento.ENTRADA.aplicar(p, 5);
		TipoMovimiento.SALIDA.aplicar(p, 3);
		assertTrue(p.tieneStockBajo());
	}
}
```

Ejecuta `mvn test`: deben pasar los 4 tests.

---

## 6. Los principios SOLID en este proyecto

### S: Responsabilidad única (*Single Responsibility*)
> Una clase debe tener **una sola razón para cambiar**.

| Clase | Su única responsabilidad |
|---|---|
| `ProductoController` | Traducir HTTP ↔ llamadas al servicio |
| `ProductoServiceImpl` | Reglas de negocio de productos |
| `ProductoRepository` | Persistencia |
| `ProductoMapper` | Convertir entidad ↔ DTO |
| `GlobalExceptionHandler` | Convertir excepciones en respuestas HTTP |

*Contraejemplo (el proyecto de docentes):* el controlador accede al repositorio, copia campos y decide qué devolver: tres razones para cambiar en una clase.

### O: Abierto/Cerrado (*Open/Closed*)
> Abierto a **extensión**, cerrado a **modificación**.

`TipoMovimiento` encapsula cómo afecta cada tipo al stock. `registrar(...)` funciona igual para cualquier tipo. Si mañana se necesita `AJUSTE` o `DEVOLUCION`, se **agrega un valor al enum** y el servicio no se toca. Sin este diseño habría un `if (tipo == ENTRADA) ... else if (tipo == SALIDA)` que crece con cada nuevo caso.

### L: Sustitución de Liskov (*Liskov Substitution*)
> Una subclase debe poder usarse donde se espera su clase base **sin romper el comportamiento**.

`StockInsuficienteException` hereda de `ReglaNegocioException`. El `GlobalExceptionHandler` captura `ReglaNegocioException` y funciona igual con la subclase: devuelve 422 con su mensaje. Del mismo modo, cualquier valor de `TipoMovimiento` respeta el contrato de `aplicar(producto, cantidad)`.

### I: Segregación de interfaces (*Interface Segregation*)
> Mejor varias interfaces **pequeñas y específicas** que una grande.

En vez de un único `ProductoInventarioService` con todo, hay dos interfaces:
- `ProductoService`: catálogo (CRUD).
- `InventarioService`: movimientos y stock.

`ProductoController` solo depende de lo que usa. Un cliente que solo consulta stock no queda acoplado a métodos de creación o edición.

### D: Inversión de dependencias (*Dependency Inversion*)
> Depender de **abstracciones**, no de implementaciones concretas.

Los controladores reciben `ProductoService` (interfaz), no `ProductoServiceImpl`. Spring inyecta la implementación por constructor. Ventajas:
- Se puede cambiar la implementación sin tocar el controlador.
- En pruebas se sustituye el servicio por un *mock*.
- Las dependencias quedan explícitas en el constructor y los campos son `final`.

---

## 7. Otras buenas prácticas aplicadas

- **Dominio rico:** las reglas del stock viven en `Producto`, no dispersas en servicios.
- **Inmutabilidad:** DTOs como `record`; el kardex nunca se modifica.
- **Baja lógica:** desactivar un producto conserva su historial.
- **Transacciones** en la capa de servicio, no en controladores ni repositorios.
- **Nombres que expresan intención:** `registrarEntrada`, `tieneStockBajo`, `findConStockBajo`.
- **Métodos cortos**, un nivel de abstracción por método.
- **Pruebas unitarias** de la lógica de dominio sin levantar Spring (`ProductoTest`).

---

## 8. Ejecutar y probar

```bash
mvn test               # pruebas unitarias
mvn spring-boot:run    # arranca en http://localhost:8085
```

Verifica que esté vivo: http://localhost:8085/actuator/health → `{"status":"UP"}`

Pruebas con Postman: importa [`postman/ms-negocio-producto.postman_collection.json`](postman/ms-negocio-producto.postman_collection.json) y sigue este orden:

1. **Crear producto** → 201, `stockActual: 0`.
2. **Registrar entrada** (10 unidades) → `stockResultante: 10`.
3. **Registrar salida** (4 unidades) → `stockResultante: 6`.
4. **Consultar stock** y **Historial de movimientos**.
5. Carpeta **Casos de error**: salida mayor al stock (422), cantidad 0 (400), producto inexistente (404).

---

## 9. Siguientes pasos (integración al ecosistema)

Cuando se integre al resto de microservicios del taller:

1. Agregar `spring-cloud-starter-config` y `spring-cloud-starter-netflix-eureka-client`.
2. Mover la configuración de conexión al repositorio de Config Server.
3. Anotar la aplicación con `@EnableDiscoveryClient` y registrar la ruta en el API Gateway.

Gracias a la separación en capas, **ninguna de estas tareas obliga a cambiar controladores, servicios ni repositorios**.

---

## 10. Glosario de anotaciones

| Anotación | Dónde | Qué hace |
|---|---|---|
| `@SpringBootApplication` | Clase principal | Activa autoconfiguración y escaneo de componentes |
| `@RestController` | Controlador | Clase REST; devuelve objetos como JSON |
| `@RequestMapping("/api/...")` | Controlador | Ruta base de todos sus métodos |
| `@GetMapping` `@PostMapping` `@PutMapping` `@DeleteMapping` | Método | Asocia un verbo HTTP y una ruta |
| `@PathVariable` | Parámetro | Toma un valor de la URL (`/productos/{id}`) |
| `@RequestBody` | Parámetro | Convierte el JSON del cuerpo en un objeto |
| `@Valid` | Parámetro | Ejecuta las validaciones del DTO |
| `@ResponseStatus` | Método | Fija el código HTTP de la respuesta |
| `@Service` | Servicio | Bean de lógica de negocio |
| `@Component` | Mapper | Bean genérico |
| `@Repository` | Repositorio | Bean de acceso a datos |
| `@Transactional` | Clase / método | Ejecuta dentro de una transacción (`readOnly = true` para consultas) |
| `@RestControllerAdvice` | Clase | Manejo global de excepciones |
| `@ExceptionHandler` | Método | Atiende un tipo de excepción concreto |
| `@Entity` | Entidad | Clase mapeada a una tabla |
| `@Table(name, schema)` | Entidad | Tabla y esquema de la BD |
| `@Id` | Atributo | Clave primaria |
| `@GeneratedValue(IDENTITY)` | Atributo | Id autogenerado por la BD |
| `@Column` | Atributo | Nombre y restricciones de la columna |
| `@Version` | Atributo | Bloqueo optimista (control de concurrencia) |
| `@Enumerated(STRING)` | Atributo | Guarda el enum como texto (`'ENTRADA'`), no como número |
| `@ManyToOne(fetch = LAZY)` | Atributo | Relación muchos-a-uno, carga diferida |
| `@JoinColumn` | Atributo | Columna de la clave foránea |
| `@Query` | Repositorio | Consulta JPQL personalizada |
| `@NotBlank` `@NotNull` `@Size` `@Min` `@DecimalMin` | Campo de DTO | Reglas de validación |
| `@Test` | Método de prueba | Marca una prueba JUnit |
