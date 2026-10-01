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