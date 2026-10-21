# PharmaMobil — Multiplatform Mobile Pharmacy System

This is a Kotlin Multiplatform (KMP) project targeting Android and iOS with Compose Multiplatform.

* [/iosApp](./iosApp/iosApp) contains the iOS application entry point and SwiftUI wrapper.
* [/shared](./shared/src) contains the shared business logic, domain models, data repositories, Ktor HTTP client, and Compose Multiplatform UI.
  - [commonMain](./shared/src/commonMain/kotlin) is common code for all targets (Clean Architecture: presentation, domain, data).
  - [androidMain](./shared/src/androidMain/kotlin) contains Android-specific platform bindings (Ktor OkHttp engine).
  - [iosMain](./shared/src/iosMain/kotlin) contains iOS-specific platform bindings (Ktor Darwin engine).

---

## Conectividad REST con Ktor Client

### 1. Arquitectura de Red y Configuración
El cliente HTTP está construido sobre **Ktor Client 3.x** integrado con **Koin** para inyección de dependencias y **kotlinx.serialization** para la negociación de contenido JSON.

- **URL Base:** `https://api.escuelajs.co/api/v1/`
- **Motor HTTP Android:** `io.ktor:ktor-client-okhttp`
- **Motor HTTP iOS:** `io.ktor:ktor-client-darwin`
- **Configuración de Serialización:**
  ```kotlin
  install(ContentNegotiation) {
      json(Json {
          ignoreUnknownKeys = true
          isLenient = true
          encodeDefaults = true
      })
  }
  ```
- **Timeouts:** `requestTimeoutMillis = 15000`, `connectTimeoutMillis = 10000`.

---

### 2. Catálogo de Endpoints REST (CRUD)

| Método | Ruta | Parámetros | Respuesta Esperada | Códigos de Error |
|---|---|---|---|---|
| `GET` | `/products` | `offset: Int`, `limit: Int` (Query) | `200 OK` — Arreglo JSON de productos | `500 Internal Server Error` |
| `GET` | `/products/{id}` | `id: Long` (Path) | `200 OK` — Objeto JSON de un producto | `400 Bad Request`, `404 Not Found` |
| `POST` | `/products/` | Cuerpo JSON (`title`, `price`, `description`, `categoryId`, `images`) | `201 Created` — Objeto JSON creado | `400 Bad Request`, `401 Unauthorized` |
| `PUT` | `/products/{id}` | `id: Long` (Path) + Cuerpo JSON | `200 OK` — Objeto JSON actualizado | `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/products/{id}` | `id: Long` (Path) | `200 OK` — `true` / Confirmación | `400 Bad Request`, `404 Not Found` |

---

### 3. Diccionario de DTOs y Mapeo al Dominio

#### DTO: `ProductoDto`
| Campo JSON | Tipo Kotlin | Obligatorio | Valor por Defecto | Campo en el Dominio (`Producto`) |
|---|---|---|---|---|
| `id` | `Long` | Sí | — | `Producto.id` |
| `title` | `String` | Sí | — | `Producto.nombre` |
| `price` | `Double` | Sí | — | `Producto.precio` |
| `description` | `String` | No | `""` | `Producto.descripcion` |
| `images` | `List<String>` | No | `emptyList()` | `Producto.imagen` (primer elemento limpio) |
| `category` | `CategoriaDto?` | No | `null` | `Producto.categoria` (`name` o `"General"`) |

#### DTO: `CategoriaDto`
| Campo JSON | Tipo Kotlin | Obligatorio | Valor por Defecto | Campo en el Dominio (`Categoria`) |
|---|---|---|---|---|
| `id` | `Long` | Sí | — | `Categoria.id` |
| `name` | `String` | Sí | — | `Categoria.nombre` |

---

### 4. Resumen de Pruebas de Conectividad y Resiliencia

1. **Respuesta Exitosa (`200 OK`):** Consumo paginado con renderizado en `LazyVerticalGrid` e imágenes asíncronas con Coil 3.
2. **Recurso Inexistente (`404 Not Found`):** Captura controlada de `ClientRequestException` y visualización de mensaje informativo con botón de reintento.
3. **Sin Conexión (`Offline`):** Manejo de `IOException` / `ConnectException` con aviso claro de desconexión sin bloquear el hilo principal.
4. **Tiempo de Espera Agotado (`Timeout`):** Detección de `HttpRequestTimeoutException` ante retrasos severos de red.
5. **Robustez ante Campos Desconocidos:** Configuración `ignoreUnknownKeys = true` que evita `SerializationException` ante campos imprevistos devueltos por la API.

---

### Ejecución de Pruebas y Compilación

- **Pruebas Unitarias:** `./gradlew :shared:testAndroidHostTest`
- **Compilación Android Debug:** `./gradlew :androidApp:assembleDebug`
- **Instalación en Dispositivo/Emulador:** `./gradlew :androidApp:installDebug`

---

## Código específico de plataforma

El proyecto implementa un desacoplamiento estricto entre la lógica multiplataforma (`commonMain`) y las capacidades dependientes del sistema operativo mediante los patrones `expect`/`actual` e Inyección de Dependencias con Koin.

### Inventario de Capacidades Nativas

| Capacidad | Clasificación | Mecanismo | Firma del expect / Contrato | Rutas de Archivos en Repositorio | API usada en Android | API usada en iOS | Propósito |
|---|---|---|---|---|---|---|---|
| **Detección de Plataforma** | Infraestructura previa | `expect` / `actual` (función) | `expect fun getPlatform(): Platform` | • `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/Platform.kt`<br>• `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/Platform.android.kt`<br>• `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/Platform.ios.kt` | `AndroidPlatform` con `Build.VERSION.SDK_INT` | `IOSPlatform` con `UIDevice.currentDevice` | Identificación básica de plataforma. |
| **Formateo Monetario** | Sesión 09 | `expect` / `actual` (función) | `expect fun formatearSoles(valor: Double): String` | • `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.kt`<br>• `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.android.kt`<br>• `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.ios.kt` | `NumberFormat` con `Locale("es", "PE")` | `NSNumberFormatter` con locale `es_PE` | Formateo monetario en moneda peruana (`S/ 8.50`). |
| **Compartir Producto** | Sesión 09 | Interfaz + Koin DI | `interface Compartidor {`<br>`&nbsp;&nbsp;fun compartir(texto: String)`<br>`}` | • `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/platform/Compartidor.kt`<br>• `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/CompartidorAndroid.kt`<br>• `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/CompartidorIos.kt` | `Intent.ACTION_SEND` con `Context` y chooser | `UIActivityViewController` modal | Delegación del diálogo nativo del sistema para compartir. |
| **Módulo de Inyección** | Sesión 09 | `expect` / `actual` (módulo) | `expect val platformModule: Module` | • `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.kt`<br>• `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.android.kt`<br>• `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/di/PlatformModule.ios.kt` | `module` con inyección contextual de `Context` | `module` sin contexto | Provisión de dependencias de plataforma en Koin. |
| **Información del Dispositivo** | 3ra Capacidad (Autónoma) | `expect` / `actual` (clase) | `expect class InfoDispositivo()`<br>`val sistema: String`<br>`val version: String`<br>`val modelo: String` | • `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.kt`<br>• `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.android.kt`<br>• `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.ios.kt` | `Build.VERSION` y `Build.MODEL` | `UIDevice.currentDevice` | Visualización de información del dispositivo cliente. |

### Verificación de Aislamiento
- `commonMain` permanece 100% puro, sin dependencias ni imports de `android.*`, `java.*` o `platform.*`.
- El compilador de Kotlin Multiplatform garantiza en tiempo de compilación (`compileAndroidMain` / `compileIosMain`) la existencia de las declaraciones `actual` correspondientes para cada declaración `expect`.

---

## Sesión 10: Autenticación JWT, Almacenamiento Seguro y Control de Roles (RBAC)

### 1. Arquitectura de Seguridad y Flujo de Sesión
La autenticación se implementa mediante tokens **JSON Web Tokens (JWT)** contra el backend PharmaSoft (Spring Boot 3.3.4):
- **Endpoint de Login:** `POST /api/v1/auth/login` con payload `{ "username": "...", "password": "..." }`.
- **Endpoint de Perfil:** `GET /api/v1/auth/me` con header `Authorization: Bearer <token>`, mapeando los permisos y datos del empleado activo.
- **Ktor Client Auth Plugin:** Configurado con `bearer { loadTokens { ... } sendWithoutRequest { ... } }`, inyectando automáticamente el encabezado Bearer en todas las rutas protegidas.
- **Manejo Global de 401 (Sesión Expirada):** Un `HttpResponseValidator` centralizado en `HttpClientFactory` intercepta cualquier respuesta HTTP 401 en llamadas protegidas, borra el token persistido del almacén seguro y transiciona el `SessionCoordinator` a `SesionEstado.Expirado`, forzando la redirección al Login sin estados inconsistentes.
- **Saneamiento de Registros (Logging):** En compilaciones de depuración (`BuildConfig.DEBUG`), los encabezados `Authorization` son saneados automáticamente mediante `sanitizeHeader { it == HttpHeaders.Authorization }`, evitando la exposición de tokens en Logcat. Fuera de depuración, el nivel de registro se desactiva a `LogLevel.NONE`.

### 2. Almacenamiento Seguro de Credenciales por Plataforma
La interfaz `AlmacenSeguro` desacopla la persistencia segura mediante Inyección de Dependencias (Koin):
- **Android (`AlmacenSeguroAndroid`):** Utiliza `EncryptedSharedPreferences` de `androidx.security:security-crypto`, cifrando las claves con `AES256_SIV` y los valores con `AES256_GCM` mediante una `MasterKey` respaldada por Android Keystore.
- **iOS (`AlmacenSeguroIos`):** Para el entorno de desarrollo en Windows cuenta con un adaptador en memoria; el diseño arquitectónico previsto para compilación nativa en macOS contempla la API nativa de **Keychain Services** de iOS (`kSecClassGenericPassword`) con nivel de protección `kSecAttrAccessibleAfterFirstUnlock`.

### 3. Usuarios de Prueba y Matriz de Control de Acceso (RBAC)

| Usuario | Contraseña | Rol Principal | Permisos Clave | Módulos Visibles en UI | Acciones en Catálogo |
|---|---|---|---|---|---|
| `admin` | `Admin123` | `ROLE_ADMIN` | `CATALOGO_LEER`, `CATALOGO_ESCRIBIR`, `CLIENTES_LEER`, `CLIENTES_ESCRIBIR`, `PEDIDOS_LEER`, `PEDIDOS_ESCRIBIR` | Inicio, Inventario, Clientes, Pedidos | Lectura, Crear, Editar, Eliminar |
| `vendedor` | `Vendedor123` | `ROLE_VENDEDOR` | `CATALOGO_LEER`, `CLIENTES_LEER`, `CLIENTES_ESCRIBIR`, `PEDIDOS_LEER`, `PEDIDOS_ESCRIBIR` | Inicio, Inventario, Clientes, Pedidos | **Solo Lectura** (Botón Nuevo, Editar y Eliminar ocultos) |
| `almacen` | `Almacen123` | `ROLE_ALMACEN` | `CATALOGO_LEER`, `CATALOGO_ESCRIBIR`, `PEDIDOS_LEER` | Inicio, Inventario, Pedidos (**Clientes Oculto**) | Lectura, Crear, Editar, Eliminar |

> **Nota de Seguridad de Laboratorio:** La pantalla de Login dispone de selectores rápidos (chips) que únicamente autocompletan el nombre de usuario de prueba. Por directriz de seguridad estricta, ninguna contraseña está incrustada en el código fuente ni en la interfaz gráfica.

### 4. Comandos de Verificación y Compilación
- **Ejecutar Pruebas Unitarias (Lógica Común, RBAC y MockEngine):**
  ```bash
  .\gradlew.bat :shared:testAndroidHostTest
  ```
- **Compilar APK Android Debug:**
  ```bash
  .\gradlew.bat :androidApp:assembleDebug
  ```

---

## Sesión 11: Persistencia Local con SQLite (SQLDelight) y Arquitectura Offline-First Reactiva

### 1. Arquitectura de Persistencia y Flujo Offline-First
Se implementa una arquitectura reactiva donde la base de datos local SQLite embebida actúa como la **única fuente de verdad** para la interfaz de usuario:
- **SQLDelight 2.1.0 Multiplataforma:** Esquema normalizado en `shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/Producto.sq` con 8 columnas: `id`, `nombre`, `precio`, `stock`, `activo`, `imagen`, `categoria`, `pendiente`.
- **Drivers de Base de Datos KMP:**
  - **Android (`CreadorDeDriverAndroid`):** `AndroidSqliteDriver(PharmaDb.Schema, context, "pharma.db")`.
  - **iOS (`CreadorDeDriverIos`):** `NativeSqliteDriver(PharmaDb.Schema, "pharma.db")`.
- **Eliminación de la Carga Imperativa REST:** La interfaz de usuario (`ProductoScreen`) no depende de recargas manuales (`cargarProductos()` y `ListarProductosUseCase` fueron eliminados). En su lugar, el `ProductoViewModel` observa continuamente `observarProductosUseCase()`, emitiendo actualizaciones automáticas a la UI ante cualquier mutación en SQLite.
- **Altas Offline y Claves Negativas Temporales:** Cuando se crea un medicamento sin red (modo avión), el repositorio genera un identificador temporal negativo (`-1, -2, ...`) mediante la consulta `SELECT COALESCE(MIN(id), 0) - 1 FROM producto WHERE id < 0;` y guarda el registro con `pendiente = 1`.
- **Reconciliación y Reemplazo Atómico:** Al restablecer la red y ejecutar la sincronización, el repositorio envía los pendientes al backend Spring Boot (`PharmaSoft`), recibe el ID positivo permanente generado por la secuencia remota (ej. `id = 6`) y ejecuta una actualización atómica en SQLite (`UPDATE producto SET id = :nuevoId, pendiente = 0 WHERE id = :idTemporal;`).
- **Resiliencia y Exclusión Mutua:** Se utiliza `Mutex` en `ProductoRepositorioOffline` para evitar condiciones de carrera durante la sincronización, propagando errores de red tipados (`ErrorApiException`) sin corromper el caché local ni alterar la marca temporal de sincronización exitosa.

### 2. Evidencias de Ejecución (5 Escenarios Validados)
Todas las evidencias se encuentran documentadas en el informe oficial `docs/sesion11/S11_ActividadAutonoma_Mestanza_Bazan.pdf` y la carpeta `docs/sesion11/capturas/`:
1. `captura_s11_01_database_inspector.png`: Inspección de `pharma.db` y tabla `producto` con 8 columnas y 6 registros persistidos.
2. `captura_s11_02_modo_avion_cache.png`: Catálogo cargado desde caché SQLite en Modo Avión (ícono `✈` visible).
3. `captura_s11_03_alta_offline_id_negativo.png`: Medicamento creado offline con ID temporal `#00-1`, badge `⏳ Pendiente de sincronización` y banner de pendientes.
4. `captura_s11_04_sincronizacion_reemplazo_id.png`: Sincronización exitosa con reemplazo atómico a `#006` y retiro de badge pendiente.
5. `captura_s11_05_error_red_resiliencia.png`: Manejo resiliente de error de conexión (banner `⚠️ Modo sin conexión`), manteniendo intacto el inventario local.
6. `captura_s11_06_pruebas_unitarias.png`: Suite completa de 50/50 pruebas unitarias superadas en `:shared:testAndroidHostTest` (100% éxito).



