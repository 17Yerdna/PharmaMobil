# Sesión 11: Persistencia Local con SQLDelight y Estrategia Offline-First (Plan Definitivo)

> **Para desarrolladores / agentes:** SUB-HABILIDAD REQUERIDA: Usar `executing-plans` para ejecutar este plan tarea por tarea. Cada paso utiliza la sintaxis de casillas de verificación (`- [ ]`) para seguimiento estricto. **NOTA:** Este documento es exclusivamente un plan de trabajo; no ejecutar código hasta su aprobación explícita.

**Meta:** Implementar una arquitectura offline-first robusta en PharmaMobil (Kotlin Multiplatform) usando SQLDelight 2.1.0 para persistencia local SQLite reactiva, sincronización bidireccional resiliente con backend Spring Boot PharmaSoft, y generar el entregable académico formal provisional (`S11_ActividadAutonoma_Mestanza_Bazan.pdf`) respaldado por las diapositivas y guía de la sesión, incluyendo bitácora de 5 escenarios reales en emulador y suite de pruebas unitarias.

**Arquitectura:** Se adopta el principio de **fuente única de la verdad (SSOT)** local: la UI de Compose Multiplatform consume **única y exclusivamente** el `Flow<List<Producto>>` emitido por la base de datos local SQLite mediante SQLDelight a través de `ObservarProductosUseCase`.
- Se erradica por completo la recarga imperativa manual REST (`cargarProductos()` y `ListarProductosUseCase`), asegurando que tras cualquier mutación local (crear, actualizar, eliminar o simular) sea la emisión reactiva de SQLite la que actualice automáticamente la UI sin parpadeos ni peticiones redundantes.
- Las operaciones de escritura guardan inmediatamente en local asignando un ID negativo temporal (`minOf(menor, 0L) - 1L`) con bandera `pendiente = 1`.
- En el `init` del `ProductoViewModel`, se conecta la recolección del Flow y **se invoca inmediatamente `sincronizar()`**, asegurando que en la primera apertura con red se descargue el catálogo remoto y se pueble la base SQLite local de manera transparente.
- En el recolector del Flow, se actualiza dinámicamente `totalPendientes = productos.count { it.id < 0L }`, permitiendo que el banner offline muestre el conteo exacto de registros pendientes y que el mensaje de feedback diferencie si un fármaco fue *"guardado localmente; pendiente de sincronización"* o registrado en el servidor.
- La sincronización con el backend está protegida por un `Mutex` y ejecuta todas las llamadas de red mediante la función utilitaria existente `ejecutarLlamada { ... }.getOrThrow()`:
  1. **Subir pendientes:** Por cada registro pendiente, se envía el POST remoto (`remoto.crear(...)`). Al responder con éxito el servidor, se reemplaza atómicamente dentro de una transacción SQLite el ID negativo por el ID positivo oficial asignado por el servidor con `pendiente = 0`.
  2. **Descargar catálogo:** Se obtiene la lista completa de productos del servidor (`remoto.listar(...)`) y se reemplazan únicamente los registros sincronizados (`pendiente = 0`), manteniendo intactos los posibles pendientes.
  3. **Manejo de errores:** `sincronizar()` **no silencia fallos de red**; propaga la excepción tipada (`ErrorApiException(ErrorApi.SinConexion)`) para que el ViewModel active `avisoRed = true` y evite actualizar erróneamente la hora de última sincronización. Solo `registrar()` captura conectividad/timeout para devolver éxito local inmediato.

**Tech Stack:** Kotlin Multiplatform (2.4.10), Compose Multiplatform (1.11.1), SQLDelight (2.1.0) con drivers `AndroidSqliteDriver`, `NativeSqliteDriver` y `JdbcSqliteDriver` (en memoria para tests), Ktor Client (3.1.3), Koin (4.0.2), Material 3, SQLite.

**Especificación y Referencias:**
- Diapositivas de clase: `C:\Users\andre\OneDrive\Desktop\Archivos extra de PharmaMobil\Docs Sesion 11\sesion11_diapositivas_offline_first_z41lsxkpqe.pdf`
- Guía práctica: `C:\Users\andre\OneDrive\Desktop\Archivos extra de PharmaMobil\Docs Sesion 11\sesion11_guia_practica_offline_first_ns4qvn2bio.pdf`
- Documentación Oficial SQLDelight 2.1.0: [https://sqldelight.github.io/sqldelight/2.1.0/](https://sqldelight.github.io/sqldelight/2.1.0/)
- Guía Multiplataforma SQLite SQLDelight 2.1.0: [https://sqldelight.github.io/sqldelight/2.1.0/multiplatform_sqlite/](https://sqldelight.github.io/sqldelight/2.1.0/multiplatform_sqlite/)

---

## Restricciones Globales (Global Constraints)

1. **Eliminación Total de Carga Manual REST:** Se prohíbe cualquier recarga imperativa manual (`cargarProductos`) en el ViewModel tras operaciones CRUD. Toda actualización visual proviene de la reactividad del `Flow` de SQLite.
2. **Módulos y Comandos Gradle Exactos:**
   - Aplicación Android: `:androidApp:assembleDebug` e `:androidApp:installDebug` (no existe `:composeApp`).
   - Suite de pruebas unitarias en `shared`: `.\gradlew :shared:testAndroidHostTest` (no `:shared:testDebugUnitTest`).
   - Módulos de inyección por plataforma: `PlatformModule.android.kt` y `PlatformModule.ios.kt`.
3. **Alcance Multiplataforma Autorizado para Evaluación:**
   Se configuran los drivers e implementaciones nativas de iOS (`CreadorDeDriverIos`, `ProveedorHoraIos`, `NativeSqliteDriver`) para preservar la coherencia y compilación multiplataforma de KMP, pero la ejecución de pruebas instrumentadas, emulación y recolección de evidencias se realizan exclusivamente en Android conforme a la autorización del docente para este curso.
4. **Versión de SQLDelight:** Obligatoriamente `2.1.0` en `gradle/libs.versions.toml` y `shared/build.gradle.kts` con artefactos de `app.cash.sqldelight`.
5. **Esquema de Base de Datos Completo (8 columnas):** La tabla `producto` en `Producto.sq` debe persistir:
   `id`, `nombre`, `precio`, `stock`, `activo`, `imagen`, `categoria` y `pendiente`.
6. **Mapeo Explícito de Tipos:** En SQLite / SQLDelight, `stock INTEGER` es generado como `Long`. El mapeo a dominio `Producto` debe convertir explícitamente `stock = fila.stock.toInt()`, y al insertar en base de datos convertir `producto.stock.toLong()`.
7. **Reutilización de `ejecutarLlamada { ... }.getOrThrow()`:** Toda invocación a `ProductoApi` dentro del repositorio offline debe encapsularse con `ejecutarLlamada` para traducir uniformemente códigos HTTP (401, 403, 404, 409, 500) y timeouts a `ErrorApiException(ErrorApi.*)`.
8. **Sincronización Transparente (Sin Silenciar Errores):**
   - `sincronizar()` propaga cualquier error de red o de servidor. No hace `return@withLock` silencioso.
   - `registrar()` es la única operación que captura conectividad/timeout (`ErrorApi.SinConexion` / `ErrorApi.TiempoAgotado`), conservando el registro pendiente y retornando el `Producto` localmente para garantizar experiencia de usuario optimista.
9. **Caché Completo en `listarActual()`:** En `ProductoLocal`, se expone `listarActual(): List<Producto>` consultando `listarTodos` para proveer todo el catálogo local (sincronizados + pendientes) cuando se requiera lectura sincrónica sin red.
10. **Control Natural de Edición/Eliminación:**
   - Si el producto tiene ID negativo (`id < 0`), se bloquea inmediatamente en el ViewModel indicando: *"No es posible editar o eliminar un producto pendiente de sincronización."*
   - Si el producto tiene ID positivo, se intenta la llamada remota vía caso de uso; si no hay red, la excepción tipada `ErrorApi.SinConexion` se captura en el ViewModel y se muestra en la UI de forma transparente. Sin monitores de red adicionales.
11. **Concurrencia Segura con Mutex:** La función `sincronizar()` se ejecuta dentro de un `Mutex.withLock` para evitar llamadas POST concurrentes duplicadas.
12. **Clean Architecture:** `ProductoViewModel` solo interactúa con casos de uso (`ObservarProductosUseCase`, `SincronizarProductosUseCase`, `RegistrarProductoUseCase`, `ActualizarProductoUseCase`, `EliminarProductoUseCase`).
13. **Entregable Académico Provisional:** El archivo entregable mantendrá el nombre provisional `S11_ActividadAutonoma_Mestanza_Bazan.pdf` (y copia de entrega) respaldado por los 5 escenarios y las 6 pruebas de las diapositivas de clase.

---

## Foco de Revisión (Review Focus - Casos Límite y Modos de Falla)

1. **Ausencia total de `cargarProductos()`:** Ningún método del ViewModel ni mutación CRUD debe invocar recargas manuales; la lista `inventarioCompleto` debe alimentarse exclusivamente del Flow emitido por SQLDelight.
2. **Primera apertura con red:** `ProductoViewModel.init` debe llamar a `sincronizar()` para que el inventario remoto se guarde en SQLite de inmediato sin requerir acción manual del usuario.
3. **Cálculo de pendientes en UI:** `totalPendientes` debe calcularse en el recolector del Flow como `productos.count { it.id < 0L }`, garantizando que el banner se actualice automáticamente tanto al guardar offline como al finalizar la sincronización.
4. **Intento de sincronización sin conexión a internet:** `sincronizar()` debe lanzar `ErrorApiException(ErrorApi.SinConexion)`; el ViewModel debe atraparlo, activar `avisoRed = true` y **no actualizar** el timestamp de `ultimaSincronizacion`.
5. **Reemplazo atómico de ID negativo:** Durante la sincronización de un pendiente, el ID negativo debe eliminarse y reinsertarse con el ID positivo devuelto por PharmaSoft en una única transacción SQLite.
6. **Concurrencia real en sincronización:** Dos llamadas simultáneas disparadas en paralelo deben ingresar ordenadamente al `Mutex`, garantizando que cada pendiente se suba una sola vez.
7. **Edición de producto pendiente:** Intentar editar un producto con ID negativo debe ser rechazado en local antes de cualquier intento de red.
8. **Persistencia tras modo avión con precondición:** Si la app sincronizó previamente con la red activa, al forzar detención, activar modo avión y reabrir, el catálogo debe mostrarse completo desde SQLite.

---

## Estructura de Archivos a Crear y Modificar

### Archivos Nuevos:
- `shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/Producto.sq`: Esquema DDL de 8 columnas y sentencias SQL (`listarTodos`, `listarPendientes`, `insertar`, `borrarPorId`, `borrarSincronizados`, `siguienteIdLocal`).
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHora.kt`: Interfaz común de formateo de hora actual.
- `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHoraAndroid.kt`: Formateador Android con `SimpleDateFormat`.
- `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHoraIos.kt`: Formateador iOS con `NSDateFormatter` (soporte de coherencia KMP).
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriver.kt`: Interfaz común de driver SQLite (alias `DatabaseDriverFactory`).
- `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriverAndroid.kt`: Implementación con `AndroidSqliteDriver`.
- `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriverIos.kt`: Implementación con `NativeSqliteDriver` (soporte de coherencia KMP).
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/local/ProductoLocal.kt`: Persistencia local, mapeo de tipos (`Long` <-> `Int`), transacciones y método `listarActual()`.
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/repository/ProductoRepositorioOffline.kt`: Repositorio offline-first con `Mutex`, `ejecutarLlamada`, propagación de errores en `sincronizar()` y soporte offline en `registrar()`.
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/usecase/SincronizarProductosUseCase.kt`: Caso de uso para disparar sincronización.
- `shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobil/data/repository/ProductoRepositorioOfflineTest.kt`: Suite de 6 pruebas unitarias con `JdbcSqliteDriver.IN_MEMORY` y `MockEngine`.

### Archivos a Modificar:
- `gradle/libs.versions.toml`: Definir versión `sqldelight = "2.1.0"` y artefactos `app.cash.sqldelight`.
- `build.gradle.kts`: Registrar plugin `sqldelight` con `apply false`.
- `shared/build.gradle.kts`: Configurar plugin SQLDelight y dependencias por sourceSet.
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/repository/ProductoRepository.kt`: Incorporar `fun observar(): Flow<List<Producto>>` y `suspend fun sincronizar()`.
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/usecase/ObservarProductosUseCase.kt`: Reescribir para usar el `ProductoRepository` de dominio.
- `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.android.kt`: Registrar `CreadorDeDriverAndroid` y `ProveedorHoraAndroid`.
- `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.ios.kt`: Registrar `CreadorDeDriverIos` y `ProveedorHoraIos`.
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt`: Registrar `PharmaDb`, `ProductoLocal`, `ProductoRepositorioOffline`, `ObservarProductosUseCase`, `SincronizarProductosUseCase` y **retirar `ListarProductosUseCase`**.
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoUiState.kt`: Campos `sincronizando`, `avisoRed`, `ultimaSincronizacion` y `totalPendientes`.
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoViewModel.kt`: Quitar `ListarProductosUseCase`, eliminar `cargarProductos()`, poblar catálogo en `init`, calcular `totalPendientes` en Flow, mensajes de éxito diferenciados y mutaciones reactivas puras.
- `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoScreen.kt`: Banner offline dinámico con número de pendientes, frescura y distintivos.

---

## Tareas de Implementación

### Tarea 1: Configuración de Dependencias SQLDelight 2.1.0 y Módulos Gradle

**Archivos:**
- Modificar: `gradle/libs.versions.toml`
- Modificar: `build.gradle.kts`
- Modificar: `shared/build.gradle.kts`

- [ ] **Paso 1: Crear la rama Git `feature/offline-first-mestanza`**
  ```bash
  git checkout -b feature/offline-first-mestanza
  ```

- [ ] **Paso 2: Declarar dependencias en `gradle/libs.versions.toml`**
  ```toml
  [versions]
  sqldelight = "2.1.0"

  [libraries]
  sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
  sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
  sqldelight-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
  sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
  sqldelight-sqlite = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }

  [plugins]
  sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
  ```

- [ ] **Paso 3: Registrar plugin en `build.gradle.kts` raíz**
  ```kotlin
  alias(libs.plugins.sqldelight) apply false
  ```

- [ ] **Paso 4: Configurar `shared/build.gradle.kts`**
  Aplicar plugin `alias(libs.plugins.sqldelight)`.
  Configurar bloque `sqldelight`:
  ```kotlin
  sqldelight {
      databases {
          create("PharmaDb") {
              packageName.set("pe.edu.upeu.pharmamobil.db")
          }
      }
  }
  ```
  Vincular dependencias en `commonMain`, `androidMain`, `iosMain` y `commonTest`.

- [ ] **Paso 5: Verificar sincronización de dependencias**
  Ejecutar:
  ```powershell
  .\gradlew :shared:dependencies
  ```
  Esperado: `BUILD SUCCESSFUL`

- [ ] **Paso 6: Commit de configuración de dependencias**
  ```bash
  git add gradle/libs.versions.toml build.gradle.kts shared/build.gradle.kts
  git commit -m "build(deps): configurar sqldelight 2.1.0 para persistencia local kmp"
  ```

---

### Tarea 2: Definición del Esquema SQLDelight Completo de 8 Columnas (`Producto.sq`)

**Archivos:**
- Crear: `shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/Producto.sq`

- [ ] **Paso 1: Crear `Producto.sq` con el esquema completo de 8 columnas**
  ```sql
  CREATE TABLE producto (
      id INTEGER NOT NULL PRIMARY KEY,
      nombre TEXT NOT NULL,
      precio REAL NOT NULL,
      stock INTEGER NOT NULL,
      activo INTEGER NOT NULL DEFAULT 1,
      imagen TEXT NOT NULL DEFAULT '',
      categoria TEXT NOT NULL DEFAULT '',
      pendiente INTEGER NOT NULL DEFAULT 0
  );

  listarTodos:
  SELECT * FROM producto ORDER BY nombre ASC;

  listarPendientes:
  SELECT * FROM producto WHERE pendiente = 1 ORDER BY id DESC;

  insertar:
  INSERT OR REPLACE INTO producto(id, nombre, precio, stock, activo, imagen, categoria, pendiente)
  VALUES (?, ?, ?, ?, ?, ?, ?, ?);

  borrarPorId:
  DELETE FROM producto WHERE id = ?;

  borrarSincronizados:
  DELETE FROM producto WHERE pendiente = 0;

  siguienteIdLocal:
  SELECT COALESCE(MIN(id), 0) FROM producto;
  ```

- [ ] **Paso 2: Generar las clases Kotlin con la tarea de SQLDelight**
  Ejecutar:
  ```powershell
  .\gradlew :shared:generateCommonMainPharmaDbInterface
  ```
  Esperado: `BUILD SUCCESSFUL`

- [ ] **Paso 3: Commit del esquema DDL**
  ```bash
  git add shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/Producto.sq
  git commit -m "feat(db): definir esquema completo de producto.sq con 8 columnas y soporte de pendientes"
  ```

---

### Tarea 3: Fábrica de Drivers Multiplataforma y Proveedor de Hora

**Archivos:**
- Crear: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHora.kt`
- Crear: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHoraAndroid.kt`
- Crear: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHoraIos.kt`
- Crear: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriver.kt`
- Crear: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriverAndroid.kt`
- Crear: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriverIos.kt`
- Modificar: `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.android.kt`
- Modificar: `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.ios.kt`
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt`

- [ ] **Paso 1: Implementar `ProveedorHora` común y nativo**
  ```kotlin
  // commonMain
  package pe.edu.upeu.pharmamobil.platform
  interface ProveedorHora {
      fun obtenerHoraActualFormateada(): String
  }

  // androidMain
  package pe.edu.upeu.pharmamobil.platform
  import java.text.SimpleDateFormat
  import java.util.Date
  import java.util.Locale
  class ProveedorHoraAndroid : ProveedorHora {
      override fun obtenerHoraActualFormateada(): String =
          SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
  }

  // iosMain
  package pe.edu.upeu.pharmamobil.platform
  import platform.Foundation.NSDate
  import platform.Foundation.NSDateFormatter
  class ProveedorHoraIos : ProveedorHora {
      override fun obtenerHoraActualFormateada(): String {
          val formatter = NSDateFormatter().apply { dateFormat = "HH:mm:ss" }
          return formatter.stringFromDate(NSDate())
      }
  }
  ```

- [ ] **Paso 2: Implementar `CreadorDeDriver` común y adaptadores nativos**
  ```kotlin
  // commonMain
  package pe.edu.upeu.pharmamobil.data.local
  import app.cash.sqldelight.db.SqlDriver
  interface CreadorDeDriver {
      fun crear(): SqlDriver
  }
  typealias DatabaseDriverFactory = CreadorDeDriver

  // androidMain
  package pe.edu.upeu.pharmamobil.data.local
  import android.content.Context
  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.android.AndroidSqliteDriver
  import pe.edu.upeu.pharmamobil.db.PharmaDb
  class CreadorDeDriverAndroid(private val context: Context) : CreadorDeDriver {
      override fun crear(): SqlDriver =
          AndroidSqliteDriver(PharmaDb.Schema, context, "pharma.db")
  }

  // iosMain
  package pe.edu.upeu.pharmamobil.data.local
  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.native.NativeSqliteDriver
  import pe.edu.upeu.pharmamobil.db.PharmaDb
  class CreadorDeDriverIos : CreadorDeDriver {
      override fun crear(): SqlDriver =
          NativeSqliteDriver(PharmaDb.Schema, "pharma.db")
  }
  ```

- [ ] **Paso 3: Registrar dependencias en `PlatformModule.android.kt` y `PlatformModule.ios.kt`**
  En `PlatformModule.android.kt`:
  ```kotlin
  single<ProveedorHora> { ProveedorHoraAndroid() }
  single<CreadorDeDriver> { CreadorDeDriverAndroid(androidContext()) }
  ```
  En `PlatformModule.ios.kt`:
  ```kotlin
  single<ProveedorHora> { ProveedorHoraIos() }
  single<CreadorDeDriver> { CreadorDeDriverIos() }
  ```

- [ ] **Paso 4: Registrar `PharmaDb` en `AppModule.kt` (`dataModule`)**
  ```kotlin
  single { get<CreadorDeDriver>().crear() }
  single { PharmaDb(get()) }
  ```

- [ ] **Paso 5: Compilar y verificar**
  Ejecutar:
  ```powershell
  .\gradlew :shared:compileKotlinAndroid
  ```
  Esperado: `BUILD SUCCESSFUL`

- [ ] **Paso 6: Commit de módulos nativos**
  ```bash
  git add shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHora.kt \
          shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHoraAndroid.kt \
          shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/ProveedorHoraIos.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriver.kt \
          shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriverAndroid.kt \
          shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/data/local/CreadorDeDriverIos.kt \
          shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.android.kt \
          shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.ios.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt
  git commit -m "feat(platform): configurar creadores de driver y proveedor de hora nativos"
  ```

---

### Tarea 4: Capa de Persistencia Local (`ProductoLocal`) con Mapeo de Tipos y `listarActual()`

**Archivos:**
- Crear: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/local/ProductoLocal.kt`
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt`

**Interfaces:**
- Produces: Clase `ProductoLocal` con:
  - `fun observar(): Flow<List<Producto>>`
  - `fun listarActual(): List<Producto>` (catálogo local completo: sincronizados + pendientes)
  - `fun pendientes(): List<Producto>`
  - `fun guardarPendiente(producto: Producto): Producto`
  - `fun reemplazarIdTemporalPorRemoto(idTemporal: Long, remoto: Producto)`
  - `fun reemplazarSincronizados(remotos: List<Producto>)`

- [ ] **Paso 1: Implementar `ProductoLocal`**
  - Mapear explícitamente tipos entre `pe.edu.upeu.pharmamobil.db.Producto` y el dominio `Producto`:
    - `stock = fila.stock.toInt()` (casteo obligatorio de `Long` a `Int`).
    - `activo = (fila.activo == 1L)`
    - `imagen = fila.imagen`
    - `categoria = fila.categoria`
  - En `observar()`: Emite `base.productoQueries.listarTodos().asFlow().mapToList(Dispatchers.Default).map { lista -> lista.map { it.toDomain() } }`.
  - En `listarActual()`: `base.productoQueries.listarTodos().executeAsList().map { it.toDomain() }`.
  - En `guardarPendiente(producto)`:
    Calcula `val menor = base.productoQueries.siguienteIdLocal().executeAsOne()`
    Genera `val idLocal = minOf(menor, 0L) - 1L`
    Inserta con `id = idLocal`, `stock = producto.stock.toLong()`, `activo = if (producto.activo) 1L else 0L`, `pendiente = 1L`.
    Retorna `producto.copy(id = idLocal)`.
  - En `reemplazarIdTemporalPorRemoto(idTemporal, remoto)`:
    Dentro de `base.productoQueries.transaction { ... }`:
    `borrarPorId(idTemporal)`
    `insertar(remoto.id, remoto.nombre, remoto.precio, remoto.stock.toLong(), if (remoto.activo) 1L else 0L, remoto.imagen, remoto.categoria, 0L)`
  - En `reemplazarSincronizados(remotos)`:
    Dentro de `base.productoQueries.transaction { ... }`:
    `borrarSincronizados()`
    Inserta remotos con `pendiente = 0L`.

- [ ] **Paso 2: Registrar `ProductoLocal` en `AppModule.kt` (`dataModule`)**
  ```kotlin
  singleOf(::ProductoLocal)
  ```

- [ ] **Paso 3: Compilar y verificar**
  Ejecutar:
  ```powershell
  .\gradlew :shared:compileKotlinAndroid
  ```
  Esperado: `BUILD SUCCESSFUL`

- [ ] **Paso 4: Commit de `ProductoLocal`**
  ```bash
  git add shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/local/ProductoLocal.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt
  git commit -m "feat(data): implementar productolocal reactivo, casteo de tipos y listaractual"
  ```

---

### Tarea 5: Repositorio Offline-First (`ProductoRepositorioOffline`), Casos de Uso y Manejo con `ejecutarLlamada`

**Archivos:**
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/repository/ProductoRepository.kt`
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/usecase/ObservarProductosUseCase.kt`
- Crear: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/usecase/SincronizarProductosUseCase.kt`
- Crear: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/repository/ProductoRepositorioOffline.kt`
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt`

- [ ] **Paso 1: Actualizar contrato de `ProductoRepository`**
  ```kotlin
  package pe.edu.upeu.pharmamobil.domain.repository
  import kotlinx.coroutines.flow.Flow
  import pe.edu.upeu.pharmamobil.domain.model.Producto

  interface ProductoRepository {
      fun observar(): Flow<List<Producto>>
      suspend fun sincronizar()
      suspend fun registrar(producto: Producto): Producto
      suspend fun listar(): List<Producto>
      suspend fun obtener(id: Long): Producto
      suspend fun actualizar(producto: Producto): Producto
      suspend fun eliminar(id: Long)
  }
  ```

- [ ] **Paso 2: Reescribir `ObservarProductosUseCase` y crear `SincronizarProductosUseCase`**
  ```kotlin
  // ObservarProductosUseCase.kt
  package pe.edu.upeu.pharmamobil.domain.usecase
  import kotlinx.coroutines.flow.Flow
  import pe.edu.upeu.pharmamobil.domain.model.Producto
  import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

  class ObservarProductosUseCase(private val repository: ProductoRepository) {
      operator fun invoke(): Flow<List<Producto>> = repository.observar()
  }

  // SincronizarProductosUseCase.kt
  package pe.edu.upeu.pharmamobil.domain.usecase
  import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

  class SincronizarProductosUseCase(private val repository: ProductoRepository) {
      suspend operator fun invoke() = repository.sincronizar()
  }
  ```

- [ ] **Paso 3: Implementar `ProductoRepositorioOffline`**
  ```kotlin
  package pe.edu.upeu.pharmamobil.data.repository

  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.sync.Mutex
  import kotlinx.coroutines.sync.withLock
  import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
  import pe.edu.upeu.pharmamobil.data.mapper.toDomain
  import pe.edu.upeu.pharmamobil.data.mapper.toRequest
  import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
  import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
  import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
  import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
  import pe.edu.upeu.pharmamobil.domain.model.Producto
  import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

  class ProductoRepositorioOffline(
      private val local: ProductoLocal,
      private val remoto: ProductoApi
  ) : ProductoRepository {

      private val sincronizacionMutex = Mutex()

      override fun observar(): Flow<List<Producto>> = local.observar()

      override suspend fun registrar(producto: Producto): Producto {
          val guardadoLocal = local.guardarPendiente(producto)
          try {
              sincronizar()
          } catch (e: Exception) {
              // Si falla por red o timeout, registrar() devuelve con exito el producto local
              if (!esErrorDeRed(e)) throw e
          }
          return guardadoLocal
      }

      override suspend fun sincronizar() {
          sincronizacionMutex.withLock {
              // 1. Subir pendientes
              val pendientes = local.pendientes()
              for (item in pendientes) {
                  val creado = ejecutarLlamada {
                      remoto.crear(item.toRequest()).toDomain()
                  }.getOrThrow()
                  local.reemplazarIdTemporalPorRemoto(item.id, creado)
              }

              // 2. Bajar catalogo remoto
              val remotos = ejecutarLlamada {
                  remoto.listar(pagina = 0, tamanio = 100).contenido.map { it.toDomain() }
              }.getOrThrow()

              local.reemplazarSincronizados(remotos)
          }
      }

      override suspend fun listar(): List<Producto> {
          return try {
              val remotos = ejecutarLlamada {
                  remoto.listar(pagina = 0, tamanio = 100).contenido.map { it.toDomain() }
              }.getOrThrow()
              local.reemplazarSincronizados(remotos)
              remotos
          } catch (e: Exception) {
              if (esErrorDeRed(e)) {
                  local.listarActual()
              } else throw e
          }
      }

      override suspend fun obtener(id: Long): Producto {
          return ejecutarLlamada {
              remoto.obtener(id).toDomain()
          }.getOrThrow()
      }

      override suspend fun actualizar(producto: Producto): Producto {
          val actualizado = ejecutarLlamada {
              remoto.actualizar(producto.id, producto.toRequest()).toDomain()
          }.getOrThrow()
          sincronizar()
          return actualizado
      }

      override suspend fun eliminar(id: Long) {
          ejecutarLlamada {
              remoto.eliminar(id)
          }.getOrThrow()
          sincronizar()
      }

      private fun esErrorDeRed(e: Throwable): Boolean {
          val errorApi = (e as? ErrorApiException)?.error
          return errorApi is ErrorApi.SinConexion || errorApi is ErrorApi.TiempoAgotado
      }
  }
  ```

- [ ] **Paso 4: Actualizar `AppModule.kt` con dependencias y casos de uso**
  En `dataModule`:
  ```kotlin
  single<ProductoRepository> { ProductoRepositorioOffline(get(), get()) }
  ```
  En `domainModule`:
  ```kotlin
  factoryOf(::ObservarProductosUseCase)
  factoryOf(::SincronizarProductosUseCase)
  ```

- [ ] **Paso 5: Compilar y verificar**
  Ejecutar:
  ```powershell
  .\gradlew :shared:compileKotlinAndroid
  ```
  Esperado: `BUILD SUCCESSFUL`

- [ ] **Paso 6: Commit del repositorio offline-first**
  ```bash
  git add shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/repository/ProductoRepository.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/usecase/ObservarProductosUseCase.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/usecase/SincronizarProductosUseCase.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/data/repository/ProductoRepositorioOffline.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt
  git commit -m "feat(repo): implementar productorepositoriooffline con sincronizar propagando errores y ejecutarllamada"
  ```

---

### Tarea 6: Suite de Pruebas Unitarias Exhaustiva con Concurrencia Real (`ProductoRepositorioOfflineTest.kt`)

**Archivos:**
- Crear: `shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobil/data/repository/ProductoRepositorioOfflineTest.kt`

- [ ] **Paso 1: Implementar las 6 pruebas unitarias requeridas:**
  1. `debeEmitirProductosGuardadosLocalmenteAlObservar`: Emisión reactiva desde SQLite in-memory (`JdbcSqliteDriver.IN_MEMORY` + `PharmaDb.Schema.create(driver)`).
  2. `debeGuardarProductoConIdNegativoCuandoNoHayRed`: Registro sin conexión asigna ID negativo y persiste localmente.
  3. `debeSubirPendientesYReemplazarIdNegativoPorRemotoAlSincronizar`: Sincronización exitosa sube pendiente, elimina ID temporal y persiste ID positivo con `pendiente = 0`.
  4. `debeMantenerDatosLocalesSiFallaElServidorAlBajar`: Simulación de HTTP 500 en `listar()`; verifica que los registros locales no se pierden y la excepción se propaga.
  5. `debePropagarError401O403AlSincronizarSinMarcarExito`: Simulación de 401/403 en `sincronizar()` lanza `ErrorApiException` hacia el llamador.
  6. `debeEvitarPeticionesDuplicadasBajoSincronizacionesConcurrentes`: Disparo simultáneo con corrutinas (`launch`) hacia `sincronizar()` con delay simulado; verifica que `remoto.crear()` se ejecute una sola vez por pendiente gracias al `Mutex`.

- [ ] **Paso 2: Ejecutar la suite de pruebas unitarias de `shared`**
  Ejecutar:
  ```powershell
  .\gradlew :shared:testAndroidHostTest --tests "pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOfflineTest"
  ```
  Esperado: 6 pruebas pasadas, 0 fallos, 0 errores.

- [ ] **Paso 3: Ejecutar la suite completa del proyecto**
  Ejecutar:
  ```powershell
  .\gradlew :shared:testAndroidHostTest
  ```
  Esperado: 100% de pruebas pasadas (total previo de la suite + 6 nuevas pruebas), 0 fallos, 0 errores.

- [ ] **Paso 4: Commit de pruebas unitarias**
  ```bash
  git add shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobil/data/repository/ProductoRepositorioOfflineTest.kt
  git commit -m "test(offline): agregar 6 pruebas unitarias con mockengine, sqlite in-memory y prueba de concurrencia mutex"
  ```

---

### Tarea 7: Integración Reactiva Pura en Capa de Presentación (`ProductoViewModel` y `ProductoScreen`)

**Archivos:**
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoUiState.kt`
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoViewModel.kt`
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoScreen.kt`
- Modificar: `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt`

- [ ] **Paso 1: Extender `ProductoUiState`**
  ```kotlin
  val sincronizando: Boolean = false,
  val avisoRed: Boolean = false,
  val ultimaSincronizacion: String? = null,
  val totalPendientes: Int = 0
  ```

- [ ] **Paso 2: Eliminar la carga manual imperativa REST y desacoplar `ListarProductosUseCase`:**
  - Retirar `ListarProductosUseCase` del constructor de `ProductoViewModel` y de la definición de `domainModule` en `AppModule.kt`.
  - Eliminar por completo la función `cargarProductos()`.
  - Eliminar todas las llamadas directas a `cargarProductos()` tras mutaciones (`guardarProducto()`, `confirmarEliminacion()` y `simularOperacion403()`).
  - La UI pasa a depender **exclusivamente** de la emisión del `Flow` de SQLite provisto por `observarProductosUseCase()`.

- [ ] **Paso 3: Implementar la reactividad pura en `ProductoViewModel`:**
  - Inyectar: `observarProductosUseCase`, `sincronizarProductosUseCase`, `registrarProductoUseCase`, `actualizarProductoUseCase`, `eliminarProductoUseCase`, `compartidor`, `platformConfig`, `proveedorHora`.
  - En `init`: Recolectar Flow, actualizar contadores y sincronizar catálogo inicial:
    ```kotlin
    init {
        viewModelScope.launch {
            observarProductosUseCase().collect { productos ->
                inventarioCompleto = productos
                val pendientesCount = productos.count { it.id < 0L }
                _uiState.update { it.copy(totalPendientes = pendientesCount) }
                actualizarFaseYContadores()
            }
        }
        sincronizar() // Poblar SQLite automáticamente en la primera apertura con red
    }
    ```
  - Sincronización transparente:
    ```kotlin
    fun sincronizar() {
        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true, avisoRed = false) }
            try {
                sincronizarProductosUseCase()
                val hora = proveedorHora.obtenerHoraActualFormateada()
                _uiState.update { it.copy(sincronizando = false, ultimaSincronizacion = hora, avisoRed = false) }
            } catch (e: Exception) {
                val errorApi = (e as? ErrorApiException)?.error
                if (errorApi is ErrorApi.SinConexion || errorApi is ErrorApi.TiempoAgotado) {
                    _uiState.update { it.copy(sincronizando = false, avisoRed = true) }
                } else {
                    _uiState.update { it.copy(sincronizando = false) }
                    manejarFallo(e)
                }
            }
        }
    }
    ```
  - Mensajes de éxito condicionales en `guardarProducto()` sin llamada a `cargarProductos()`:
    ```kotlin
    resultado.onSuccess { productoGuardado ->
        val mensaje = if (productoGuardado.id < 0L) {
            "Producto '${productoGuardado.nombre}' guardado localmente; pendiente de sincronización."
        } else if (form.esEdicion) {
            "Producto '${productoGuardado.nombre}' actualizado con éxito."
        } else {
            "Producto '${productoGuardado.nombre}' registrado con éxito."
        }
        _uiState.update {
            it.copy(
                operacion = Operacion.Inactiva,
                formulario = FormularioProductoState(),
                mostrarFormulario = false,
                mensajeExito = mensaje
            )
        }
        // No se invoca cargarProductos(); el Flow de SQLite actualiza la lista automáticamente.
    }
    ```
  - Control de edición para IDs negativos: Si `form.id != null && form.id < 0L`, cancelar guardado y emitir mensaje: *"No es posible editar un producto pendiente de sincronización."*
  - Control de eliminación para IDs negativos: Si `productoAEliminar.id < 0L`, cancelar y emitir: *"No es posible eliminar un producto pendiente de sincronización."*
  - Si tienen ID positivo, proceder normalmente con los casos de uso remotos; ante falta de red, `manejarFallo` desplegará *"Sin conexión a internet. Verifique su red."*

- [ ] **Paso 4: Incorporar elementos visuales en `ProductoScreen.kt`**
  - Banner superior cuando `uiState.avisoRed` sea `true` o `totalPendientes > 0`: *"Modo sin conexión - Cambios pendientes de sincronizar: [X]"*.
  - Fila informativa de frescura: *"Última sincronización: [HH:mm:ss]"* con botón de refresco manual.
  - En cada tarjeta de producto: Si `id < 0`, mostrar distintivo visual naranja (Badge / Icono de sincronización pendiente).

- [ ] **Paso 5: Compilar aplicación Android**
  Ejecutar:
  ```powershell
  .\gradlew :androidApp:assembleDebug
  ```
  Esperado: `BUILD SUCCESSFUL`

- [ ] **Paso 6: Commit de capa de presentación**
  ```bash
  git add shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoUiState.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoViewModel.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/presentation/producto/ProductoScreen.kt \
          shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/AppModule.kt
  git commit -m "feat(ui): desacoplar listarproductosusecase, eliminar cargarproductos y conectar reactividad pura con flow sqlite"
  ```

---

### Tarea 8: Ejecución de Escenarios y Recolección de Evidencia Real en Emulador Android

**Archivos:**
- Evidencias: Capturas directas en `docs/evidencias/sesion11/`:
  - `figura1_modo_avion_lectura_cache.png`
  - `figura2_registro_offline_id_negativo.png`
  - `figura3_database_inspector_id_negativo.png`
  - `figura4_reconexion_sincronizacion_exitosa.png`
  - `figura5_database_inspector_id_positivo.png`
  - `figura6_servidor_caido_tolerancia.png`
  - `figura7_persistencia_post_forcestop.png`
  - `figura8_suite_pruebas_unitarias_host.png`

- [ ] **Paso 1: Iniciar backend PharmaSoft Spring Boot**
- [ ] **Paso 2: Instalar APK en el emulador**
  ```powershell
  .\gradlew :androidApp:installDebug
  ```
- [ ] **Paso 3: Ejecutar Escenario 1 - Apertura en Modo Avión (con precondición de sincronización previa)**
  - **Precondición:** Abrir la app con red activa y backend encendido para sincronizar y poblar la base SQLite local.
  - Cerrar la app: `adb shell am force-stop pe.edu.upeu.pharmamobil`.
  - Activar modo avión: `adb shell cmd connectivity airplane-mode enable`.
  - Reabrir la app y capturar pantalla demostrando catálogo completo cargado desde caché SQLite local con banner offline.
- [ ] **Paso 4: Ejecutar Escenario 2 - Alta en Modo Avión e Inspección de ID Negativo**
  - Mantener modo avión y registrar un nuevo fármaco (ej. *"Amoxicilina 500mg Offline"*).
  - Verificar visualmente aparición inmediata en la lista con ID negativo, badge *"Pendiente"* y mensaje *"guardado localmente; pendiente de sincronización"*.
  - Abrir **Database Inspector** en Android Studio sobre `pharma.db` -> tabla `producto` y capturar el registro con `id < 0` y `pendiente = 1`.
- [ ] **Paso 5: Ejecutar Escenario 3 - Reconexión, Sincronización y Reconciliación**
  - Desactivar modo avión: `adb shell cmd connectivity airplane-mode disable`.
  - Disparar sincronización manual.
  - Verificar en la app que el badge desaparece y el timestamp se actualiza.
  - En **Database Inspector**, capturar la tabla mostrando el reemplazo por el ID positivo del servidor con `pendiente = 0`.
- [ ] **Paso 6: Ejecutar Escenario 4 - Tolerancia a Fallos de Backend**
  - Detener temporalmente el backend PharmaSoft manteniendo la red del emulador activa.
  - Intentar sincronizar: la app no colapsa, preserva la lista local y muestra el aviso de conectividad sin alterar el timestamp.
  - Capturar emulador y Logcat.
- [ ] **Paso 7: Ejecutar Escenario 5 - Persistencia tras Cierre Forzado (`force-stop`)**
  - Cerrar forzadamente: `adb shell am force-stop pe.edu.upeu.pharmamobil`.
  - Reabrir la aplicación en modo avión: la base SQLite local mantiene intactos todos los datos.
  - Capturar pantalla del emulador.
- [ ] **Paso 8: Capturar ejecución de pruebas unitarias (`:shared:testAndroidHostTest`) en verde**

---

### Tarea 9: Generación del Entregable Académico Formal (HTML/PDF y Markdown)

**Archivos:**
- Crear: `docs/sesion11/informe_sesion11_offline_first.html`
- Compilar: `C:\Users\andre\OneDrive\Desktop\Archivos extra de PharmaMobil\Docs Sesion 11\S11_ActividadAutonoma_Mestanza_Bazan.pdf` (provisional)
- Copia: `C:\Users\andre\OneDrive\Desktop\Archivos extra de PharmaMobil\Docs Sesion 11\S11_ActividadAutonoma_Mestanza.pdf` (provisional)
- Modificar: `README.md`

**Estructura del Informe Académico:**
1. **Portada Institucional UPeU:** Universidad Peruana Unión, FIIS, Sesión 11: Persistencia local con SQLDelight y estrategia offline-first. Estudiante: Andre Mestanza Bazan. Ciclo 2026-II.
2. **Resumen Ejecutivo:** Resiliencia offline en entornos clínicos y farmacéuticos.
3. **Fundamentación Técnica y Arquitectura:** Diagramas Mermaid de sincronización atómica con `Mutex`, IDs negativos y SQLDelight 2.1.0. Se incluye nota explícita sobre el alcance multiplataforma (drivers iOS configurados para coherencia KMP; verificación y evidencias en Android autorizadas por el docente).
4. **Bitácora de los 5 Escenarios Offline Reales:** Evidencias de emulador, App Inspection (Database Inspector) y Logcat.
5. **Matriz de Pruebas Unitarias Automatizadas:** Resumen de las 6 pruebas de `ProductoRepositorioOfflineTest` con estado PASS y captura de `.\gradlew :shared:testAndroidHostTest`.
6. **Trazabilidad Git y Código Fuente Clave:** Commits en `feature/offline-first-mestanza` y código fuente representativo.
7. **Anexo Opcional:** Banco de preguntas y respuestas para la defensa de la Evaluación de la Unidad 2.

**Pasos de Generación:**
- [ ] **Paso 1: Generar el informe HTML con maquetación A4 UPeU, tablas y estilos de impresión.**
- [ ] **Paso 2: Compilar el PDF provisional con Chrome Headless (`--print-to-pdf`, `--no-pdf-header-footer`) hacia las rutas de entrega.**
- [ ] **Paso 3: Validar visualmente el PDF para garantizar ausencia de saltos huérfanos o desbordes.**
- [ ] **Paso 4: Actualizar `README.md` documentando la arquitectura offline-first.**
- [ ] **Paso 5: Commit explícito de documentación y entregable:**
  ```bash
  git add docs/sesion11/ docs/evidencias/sesion11/ README.md
  git commit -m "docs(sesion11): agregar informe academico formal y actualizar readme con estrategia offline-first"
  ```
- [ ] **Paso 6: Push de la rama remota `feature/offline-first-mestanza`:**
  ```bash
  git push -u origin feature/offline-first-mestanza
  ```

---

## Plan de Verificación

### Comprobaciones Automatizadas:
```powershell
# 1. Generación de interfaces SQLDelight
.\gradlew :shared:generateCommonMainPharmaDbInterface

# 2. Ejecución de la suite completa de pruebas unitarias
.\gradlew :shared:testAndroidHostTest

# 3. Compilación limpia del APK debug para Android
.\gradlew :androidApp:assembleDebug
```

### Comprobaciones Manuales y de Emulador:
1. Inspeccionar en tiempo real mediante **Android Studio Database Inspector** que los registros con ID negativo pasen a ID positivo del servidor tras la sincronización.
2. Confirmar que ninguna mutación CRUD invoca `cargarProductos()`; el catálogo se refresca únicamente cuando SQLite emite en el Flow.
3. Comprobar que en la primera apertura con red se puebla automáticamente la base de datos local SQLite gracias a `sincronizar()` en `init`.
4. Comprobar que si `sincronizar()` falla por red, no se actualiza la hora de sincronización y aparece el banner de aviso con el conteo exacto de `totalPendientes`.
5. Verificar que el entregable PDF final `S11_ActividadAutonoma_Mestanza_Bazan.pdf` cumpla íntegramente los estándares de evidencia y rúbrica UPeU.
