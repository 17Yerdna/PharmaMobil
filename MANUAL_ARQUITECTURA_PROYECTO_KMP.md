# 📘 Manual de Arquitectura e Ingeniería Móvil KMP
## Guía de Fundamentos, Patrones de Diseño y Recetas Técnicas para Proyectos Multiplataforma
> **Institución:** Universidad Peruana Unión (UPeU) — Escuela Profesional de Ingeniería de Sistemas  
> **Asignatura:** Desarrollo de Aplicaciones Móviles  
> **Cátedra:** Mg. Benjamín David Reyna Barreto  
> **Tecnologías Base:** Kotlin Multiplatform (KMP 2.1+), Compose Multiplatform (CMP 1.7+), Koin 4.x, Ktor Client 3.x, SQLDelight 2.1.0, Material 3.  
> **Propósito:** Documento de transferencia de contexto técnico para nuevos proyectos. Establece la arquitectura base obligatoria y provee recetas modulares avanzadas que se activan según el alcance específico de cada aplicación.

---

# 1. Alcance y Decisiones de Arquitectura

Antes de escribir una sola línea de código, el desarrollador debe situar su proyecto en el nivel arquitectónico correspondiente. No todas las aplicaciones móviles requieren el stack completo; añadir complejidad innecesaria (como persistencia relacional o clientes REST en apps que no los demandan) vulnera los principios de diseño pragmático.

### 1.1. Matriz de Proporcionalidad del Alcance

| Nivel de Aplicación | Cuándo Aplica | Componentes Requeridos | Componentes que NO Deben Incluirse |
| :--- | :--- | :--- | :--- |
| **Nivel 1: App Local Pura** | Utilidades, herramientas personales, calculadoras, registro local sin cuenta de usuario. | • Capa de Dominio pura.<br>• Compose Multiplatform UI.<br>• ViewModel con `StateFlow` (UDF).<br>• Inyección de dependencias con Koin.<br>• Persistencia local simple o SQLite si hay volumen. | • Ktor Client.<br>• Autenticación JWT / RBAC.<br>• Identificadores temporales negativos.<br>• Mecanismos de sincronización remota. |
| **Nivel 2: App Cliente REST** | Catálogos de consulta, clientes de APIs públicas o privadas donde el servidor es la única autoridad y no se opera sin internet. | • Todo el Nivel 1.<br>• Ktor Client con serialización JSON.<br>• DTOs y Mappers desacoplados.<br>• Manejo de 5 estados de error de red. | • Base de datos SQLite embebida.<br>• Cola de mutaciones offline.<br>• IDs temporales negativos. |
| **Nivel 3: App Empresarial Segura** | Sistemas de gestión multiusuario (roles), paneles administrativos, aplicaciones corporativas autenticadas. | • Todo el Nivel 2.<br>• Autenticación JWT con `ktor-client-auth`.<br>• Almacenamiento seguro cifrado (`AlmacenSeguro`).<br>• Control de acceso basado en roles (RBAC) en UI y navegación. | • Persistencia offline compleja si la política corporativa exige conectividad permanente. |
| **Nivel 4: Sistema Offline-First Crítico** | Aplicaciones de campo, despacho clínico, ventas en ruta o inventarios donde la pérdida de señal no puede detener la operación. | • Todo el Nivel 3.<br>• SQLite local como Única Fuente de Verdad (SSOT).<br>• Altas offline con IDs temporales negativos y bandera pendiente.<br>• Sincronización atómica con exclusión mutua (`Mutex`).<br>• Sesión de solo lectura offline. | • Ninguno; aplica el stack de ingeniería completo. |

> **Regla de Decisión:** La arquitectura base (Nivel 1: Clean Architecture, UDF, Compose Multiplatform, Dominio puro y Koin) es la base común de todo proyecto. Los módulos de red REST, seguridad JWT y persistencia Offline-First son **recetas modulares** que se incorporan únicamente cuando los requisitos funcionales del proyecto final lo justifiquen.

---

# PARTE I: CONTEXTO BASE Y FUNDAMENTOS KMP
*(Común a todo proyecto móvil, independientemente de su alcance)*

---

# 2. Inicio de Proyecto KMP, Source Sets y Flujo Git

### 2.1. Estructura Estándar de Directorios y Source Sets
Kotlin Multiplatform organiza el código separando la lógica compartida de los enlaces de plataforma:

```
Raíz del Proyecto
├── androidApp/                         # Contenedor anfitrión Android (Application, MainActivity)
├── iosApp/                             # Contenedor anfitrión iOS (SwiftUI App, Xcode Project)
├── shared/                             # Módulo común de lógica de negocio y presentación CMP
│   └── src/
│       ├── commonMain/                 # 100% Kotlin puro: Dominio, Casos de Uso, UI Compose
│       │   ├── kotlin/.../domain/      # Modelos, interfaces de repositorios, casos de uso
│       │   ├── kotlin/.../presentation/# Composables, ViewModels, estados de UI, temas
│       │   └── kotlin/.../di/          # Módulos comunes de Koin
│       ├── androidMain/                # Enlaces nativos Android (Context, drivers específicos)
│       ├── iosMain/                    # Enlaces nativos iOS (UIKit, Foundation, Keychain)
│       ├── commonTest/                 # Pruebas unitarias multiplataforma (Mocks, Fakes)
│       └── androidHostTest/            # Pruebas de integración sobre la JVM del entorno de desarrollo
├── gradle/
│   └── libs.versions.toml              # Catálogo centralizado de versiones y dependencias
└── build.gradle.kts                    # Script raíz de configuración
```

### 2.2. Flujo Mínimo y Convenciones de Git `[OBLIGATORIO PARA ESTE CURSO]`
1. **Ramas Temáticas:** El trabajo nunca se realiza directamente sobre `main` o `master`. Se crean ramas por funcionalidad:
   - Formato: `feature/<nombre-caracteristica>-<apellido>` (ej. `feature/offline-storage-mestanza`).
2. **Commits Atómicos y Mensajes Convencionales:**
   - Cada commit debe compilar y contener una unidad lógica de cambio indivisible.
   - Prefijos estándar:
     - `feat:` Nueva funcionalidad para el usuario.
     - `fix:` Corrección de un defecto o error.
     - `refactor:` Reorganización de código sin alteración de comportamiento.
     - `test:` Inclusión o ajuste de pruebas unitarias.
     - `docs:` Modificaciones en documentación técnica o entregables.

---

# 3. Fundamentos de Kotlin Moderno

### 3.1. Null-Safety e Inmutabilidad Estricta `[OBLIGATORIO PARA ESTE CURSO]`
- **Inmutabilidad por Defecto:** Uso preferente de `val` sobre `var`. Los modelos se diseñan con `data class`, permitiendo mutaciones puras mediante el método `.copy()`.
- **Manejo Seguro de Nulos:** Erradicación del operador de aserción no nula (`!!`). Se utilizan operadores seguros:
  - Navegación segura: `objeto?.propiedad`.
  - Operador Elvis para valores por defecto o salidas tempranas: `val id = dto.id ?: return Result.failure(...)`.
  - Transformación con ámbito: `texto?.takeIf { it.isNotBlank() }?.let { procesar(it) }`.

### 3.2. Colecciones Funcionales y Transformaciones
Procesamiento declarativo sin bucles mutables indexados:
- Transformación y filtrado: `.map { ... }`, `.filter { ... }`, `.filterNotNull()`.
- Búsqueda determinista: `.firstOrNull { ... }`, `.any { ... }`, `.all { ... }`.
- Agregaciones seguras: `.fold(...)`, `.associateBy { ... }`, `.groupBy { ... }`.

### 3.3. Corrutinas, Cancelación Cooperativa y Flujos Reactivos (`Flow`)
- **Despachadores (`Dispatchers`):**
  - `Dispatchers.Main`: Hilo principal donde normalmente se recolectan los flujos reactivos y se actualiza el estado de presentación. *(Nota conceptual: La recomposición en Compose no la ejecuta el despachador de hilos directamente; la recomposición la dispara automáticamente el sistema de Snapshots ante cambios en el estado observable).*
  - `Dispatchers.Default`: Tareas intensivas de cálculo numérico, serialización/deserialización JSON pesada o filtrado masivo en memoria.
  - En Kotlin Multiplatform moderno, las bibliotecas oficiales de red (Ktor) y base de datos (SQLDelight) administran sus propios hilos de E/S de forma nativa sin requerir envoltorios manuales.
- **Cancelación Cooperativa:** Las corrutinas en KMP deben respetar la cancelación. Jamás atrapar `CancellationException` de forma genérica sin relanzarla:
  ```kotlin
  try {
      ejecutarOperacionLenta()
  } catch (e: CancellationException) {
      throw e // Obligatorio para permitir la cancelación del CoroutineScope
  } catch (e: Exception) {
      // Manejo de error de negocio
  }
  ```
- **Flujos (`Flow`, `StateFlow`):**
  - `Flow`: Flujo de datos frío para streams de persistencia.
  - `StateFlow`: Flujo caliente con estado que retiene el último valor emitido; representa el estado canónico expuesto por los ViewModels.

---

# 4. UI Declarativa con Compose Multiplatform

### 4.1. Modelo Mental: Composición vs Recomposición
Compose construye la interfaz ejecutando funciones composables. Cuando un estado observable cambia, solo las funciones que leen dicho estado se **recomponen**.
- **Regla de Rendimiento:** Las funciones composables deben ser rápidas, idempotentes y carentes de efectos secundarios directos. Los efectos colaterales se confinan a manejadores de eventos o bloques `LaunchedEffect`.

### 4.2. State Hoisting (Elevación de Estado) y `remember` vs `ViewModel` `[OBLIGATORIO PARA ESTE CURSO]`
- **`remember` / `rememberSaveable`:** Confinado estrictamente a **estado efímero visual** de la interfaz (ej. si un menú desplegable está abierto, la posición de scroll o la animación de un acordeón).
- **`ViewModel`:** Almacena todo el **estado operativo y de negocio** (listados, formularios, datos del usuario, errores de validación). 
- **Anti-patrón prohibido:** Almacenar formularios o entidades de negocio en variables locales con `remember` dentro de la UI. Dicha práctica provoca que los datos se pierdan al rotar la pantalla o recomponer el árbol.

### 4.3. Modifiers y Reglas de Encadenamiento
El orden de encadenamiento de los `Modifier` altera directamente el layout:
```kotlin
Modifier
    .fillMaxWidth()
    .padding(16.dp)        // Margen exterior
    .background(Color.White, shape = RoundedCornerShape(8.dp))
    .clickable { onClick() } // Zona cliqueable delimitada por el fondo
    .padding(12.dp)        // Padding interior del contenido
```

### 4.4. Theming Centralizado con Material 3
La apariencia de la aplicación se centraliza en tokens semánticos (Color, Tipografía, Formas) compatibles con modo claro y oscuro:

```kotlin
package pe.edu.upeu.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaletaClara = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF97F0FF),
    background = Color(0xFFFBFDFD),
    surface = Color(0xFFFBFDFD),
    error = Color(0xFFBA1A1A)
)

private val PaletaOscura = darkColorScheme(
    primary = Color(0xFF4FD8EB),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    background = Color(0xFF191C1D),
    surface = Color(0xFF191C1D),
    error = Color(0xFFFFB4AB)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colores = if (darkTheme) PaletaOscura else PaletaClara
    MaterialTheme(
        colorScheme = colores,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
```

### 4.5. Navegación Declarativa Tipada con `Screen`
La navegación se modela mediante jerarquías selladas que eliminan rutas de texto arbitrarias:

```kotlin
package pe.edu.upeu.app.navigation

sealed class Screen(val ruta: String, val titulo: String) {
    data object Principal : Screen("principal", "Panel Principal")
    data object Catalogo : Screen("catalogo", "Catálogo General")
    data object Perfil : Screen("perfil", "Mi Perfil")
}
```

---

# 5. Capa de Dominio Pura (Plantillas Neutras)

El dominio modela el negocio sin asumir nombres de dominio preconcebidos ni estructuras fijas. A continuación se presentan las plantillas universales de implementación:

### 5.1. Entidad de Dominio con Control de Invariantes `[OBLIGATORIO PARA ESTE CURSO]`
Una entidad encapsula su identidad y protege sus reglas mediante validación interna en construcción:

```kotlin
package pe.edu.upeu.app.domain.model

data class Recurso(
    val id: Long = 0L,
    val identificador: String,
    val nombre: String,
    val metricaPrincipal: Double,
    val cantidadUnidades: Int,
    val activo: Boolean = true
) {
    init {
        // Validación de invariantes de negocio
        require(identificador.isNotBlank()) { "El identificador no puede estar vacío." }
        require(nombre.trim().length >= 3) { "El nombre debe contener al menos 3 caracteres." }
        require(metricaPrincipal > 0.0) { "La métrica principal debe ser un valor positivo." }
        require(cantidadUnidades >= 0) { "Las unidades no pueden ser negativas." }
    }

    // Reglas de negocio puras calculadas
    val requiereAtencion: Boolean get() = cantidadUnidades <= UMBRAL_CRITICO
    val esRegistroPendiente: Boolean get() = id < 0L // Solo aplica si hay sincronización

    fun ajustarCantidad(diferencia: Int): Recurso {
        val nuevaCantidad = cantidadUnidades + diferencia
        require(nuevaCantidad >= 0) { "Saldo insuficiente para completar el ajuste." }
        return copy(cantidadUnidades = nuevaCantidad)
    }

    companion object {
        const val UMBRAL_CRITICO = 5
    }
}
```

> **Aclaración de Plantilla Neutra:** Atributos como `metricaPrincipal`, `cantidadUnidades` o la comprobación `id < 0L` son ejemplos ilustrativos para recursos numéricos y sincronizables. En aplicaciones de agenda, notas, usuarios o mensajería, el modelo contendrá exclusivamente los atributos y reglas inherentes a su propio contexto.

### 5.2. Estados Finitos de Negocio (`sealed interface`)
Los estados operativos de cualquier proceso se representan mediante tipos sellados:

```kotlin
package pe.edu.upeu.app.domain.model

sealed interface EstadoOperacion {
    data object EnBorrador : EstadoOperacion
    data object Registrado : EstadoOperacion
    data object EnProceso : EstadoOperacion
    data object Finalizado : EstadoOperacion
    data class Cancelado(val motivo: String) : EstadoOperacion
}
```

### 5.3. Caso de Uso Universal con Validación Funcional (`Result<T>`)
Cada caso de uso expone una única responsabilidad y retorna tipos funcionales explícitos:

```kotlin
package pe.edu.upeu.app.domain.usecase

import pe.edu.upeu.app.domain.model.Recurso
import pe.edu.upeu.app.domain.repository.RecursoRepository

class RegistrarRecursoUseCase(
    private val repository: RecursoRepository
) {
    fun validar(identificador: String, nombre: String, metrica: String, cantidad: String): Result<Recurso> {
        val idLimpio = identificador.trim()
        val nomLimpio = nombre.trim()

        if (idLimpio.isBlank()) return Result.failure(IllegalArgumentException("El identificador es obligatorio."))
        if (nomLimpio.length < 3) return Result.failure(IllegalArgumentException("El nombre debe poseer al menos 3 caracteres."))

        val metricaDouble = metrica.toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Ingrese un valor numérico válido para la métrica."))
        if (metricaDouble <= 0.0) return Result.failure(IllegalArgumentException("La métrica debe ser mayor a cero."))

        val cantidadInt = cantidad.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("La cantidad debe ser un número entero."))
        if (cantidadInt < 0) return Result.failure(IllegalArgumentException("La cantidad no puede ser negativa."))

        return Result.success(
            Recurso(
                id = 0L,
                identificador = idLimpio,
                nombre = nomLimpio,
                metricaPrincipal = metricaDouble,
                cantidadUnidades = cantidadInt
            )
        )
    }

    suspend operator fun invoke(id: String, nombre: String, metrica: String, cantidad: String): Result<Recurso> {
        val validacion = validar(id, nombre, metrica, cantidad)
        if (validacion.isFailure) return validacion

        return try {
            val persistido = repository.registrar(validacion.getOrThrow())
            Result.success(persistido)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

# 6. Inyección de Dependencias Multiplataforma con Koin 4.x `[OBLIGATORIO PARA ESTE CURSO]`

Koin permite la Inversión de Control (IoC) sin reflexión compleja ni generadores de código de compilación pesados.

### 6.1. Organización Modular de Contenedores
Las dependencias se agrupan en módulos según su capa arquitectónica:

```kotlin
package pe.edu.upeu.app.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.edu.upeu.app.domain.usecase.RegistrarRecursoUseCase
import pe.edu.upeu.app.presentation.recurso.RecursoViewModel

// Módulo de Casos de Uso (Dominio)
val domainModule = module {
    factoryOf(::RegistrarRecursoUseCase)
}

// Módulo de ViewModels (Presentación)
val viewModelModule = module {
    viewModelOf(::RecursoViewModel)
}
```

### 6.2. Inicialización Multiplataforma
- **Android (`MainApplication.kt`):**
  ```kotlin
  class MainApplication : Application() {
      override fun onCreate() {
          super.onCreate()
          startKoin {
              androidContext(this@MainApplication)
              modules(platformModule, dataModule, domainModule, viewModelModule)
          }
      }
  }
  ```
- **iOS (`iOSApp.swift`):**
  ```swift
  import SwiftUI
  import Shared

  @main
  struct iOSApp: App {
      init() {
          // El puente generado por Kotlin/Native depende del nombre del archivo (KoinIos.kt -> KoinIosKt).
          // Además, como 'init' es palabra reservada en Swift, el compilador antepone automáticamente 'do'
          // a cualquier función que empiece con 'init' (initKoinIos() -> doInitKoinIos()).
          KoinIosKt.doInitKoinIos()
      }
      var body: some Scene {
          WindowGroup { ContentView() }
      }
  }
  ```

---

# 7. Capacidades Nativas e Interoperabilidad Kotlin–Swift

### 7.1. Matriz de Decisión: `expect/actual` vs Interfaz + Koin `[OBLIGATORIO PARA ESTE CURSO]`

```
                           ¿Requiere la capacidad nativa
                     acceder al Contexto del SO o a la UI?
                                     │
                    ┌────────────────┴────────────────┐
                   SÍ                                NO
                    ▼                                 ▼
         [ Interfaz en Dominio ]             [ expect / actual ]
         Inyectada mediante Koin          Función o clase estática pura
         (Ej. Compartir, GPS, Cámara)     (Ej. Formateo, HW info)
```

### 7.2. Interoperabilidad Kotlin–Swift (Exportación y Consumo)
Cuando Kotlin Multiplatform compila para iOS genera un binario dinámico o estático (`Shared.framework`):
1. **Modelos y Funciones:** Se exportan como clases de Objective-C accesibles desde Swift. Las funciones de nivel superior se agrupan en clases de plataforma (ej. `FormatoKt.formatearMoneda(valor:)`).
2. **Jerarquías Selladas (`sealed class`):** En Swift se reciben como una jerarquía de clases. Para evaluarlas de forma exhaustiva en Swift, se recomienda realizar un casteo secuencial o emplear herramientas de soporte (como SKIE) que generan enums nativos de Swift.
3. **Corrutinas y `Flow` en Swift:** 
   - Una función `suspend fun operacion(): T` de Kotlin se exporta a Swift como `func operacion() async throws -> T`.
   - Un `Flow<T>` de Kotlin se consume en Swift mediante manejadores de suscripción o adaptadores reactivos.

---

# PARTE II: RECETAS MODULARES AVANZADAS
*(Activar exclusivamente si el alcance del proyecto lo requiere)*

---

# 8. Receta: Cliente de Red REST con Ktor 3.x `[SOLO SI APLICA AL ALCANCE]`

### 8.1. Configuración del Cliente HTTP con Motores Nativos
Centraliza la conexión usando el motor `OkHttp` en Android y `Darwin` en iOS:

```kotlin
package pe.edu.upeu.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun crearHttpClient(engine: HttpClientEngine, esDepuracion: Boolean = false): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Evita roturas por campos nuevos en el servidor
                isLenient = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000L
            connectTimeoutMillis = 10_000L
            socketTimeoutMillis = 15_000L
        }
        if (esDepuracion) {
            install(Logging) { level = LogLevel.ALL }
        }
    }
}
```

### 8.2. Paginación y Respuestas de Operación CRUD (HTTP 201, 204, 400, 422)
Las APIs empresariales gestionan catálogos mediante esquemas de paginación y códigos de estado semánticos:

#### Esquema de Respuesta Paginada
```kotlin
package pe.edu.upeu.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginaDto<T>(
    val contenido: List<T>,
    val numeroPagina: Int,
    val tamanioPagina: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val esUltima: Boolean
)
```

#### Manejo de Códigos de Respuesta en la Capa Remota
```kotlin
package pe.edu.upeu.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import pe.edu.upeu.app.data.remote.dto.*

class RecursoApi(private val client: HttpClient) {

    // GET Paginado (200 OK)
    suspend fun listar(pagina: Int, limite: Int): PaginaDto<RecursoResponseDto> =
        client.get("recursos") {
            parameter("page", pagina)
            parameter("size", limite)
        }.body()

    // POST Creación (201 Created)
    suspend fun crear(dto: RecursoRequestDto): RecursoResponseDto =
        client.post("recursos") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body()

    // PUT Actualización (200 OK o 204 No Content)
    suspend fun actualizar(id: Long, dto: RecursoRequestDto): RecursoResponseDto =
        client.put("recursos/$id") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }.body()

    // DELETE Eliminación (204 No Content)
    suspend fun eliminar(id: Long) {
        val respuesta = client.delete("recursos/$id")
        if (respuesta.status != HttpStatusCode.NoContent && respuesta.status != HttpStatusCode.OK) {
            throw IllegalStateException("Fallo al eliminar recurso: ${respuesta.status}")
        }
    }
}
```

### 8.3. Modelo de Estado de Mutación en Presentación
Para evitar el parpadeo de pantalla (screen flicker) durante operaciones CRUD, el estado de la mutación se aísla de la fase global del listado:

```kotlin
sealed interface OperacionMutacion {
    data object Inactiva : OperacionMutacion
    data class EnCurso(val descripcion: String) : OperacionMutacion
    data class Fallida(val mensaje: String) : OperacionMutacion
    data class Exitosa(val mensaje: String) : OperacionMutacion
}
```

---

# 9. Receta: Seguridad Empresarial, Autenticación JWT y RBAC `[SOLO SI APLICA AL ALCANCE]`

### 9.1. Contrato Multiplataforma y Almacén Seguro Nativo
Las credenciales de acceso jamás se persisten en texto claro.

#### Contrato Común (`commonMain`)
```kotlin
package pe.edu.upeu.app.data.local

interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()
}
```

#### Implementación Android (`EncryptedSharedPreferences` + Keystore)
```kotlin
// androidMain
package pe.edu.upeu.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AlmacenSeguroAndroid(context: Context) : AlmacenSeguro {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "credenciales_seguras",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun guardarToken(token: String) = prefs.edit().putString("jwt_token", token).apply()
    override fun leerToken(): String? = prefs.getString("jwt_token", null)
    override fun borrarToken() = prefs.edit().remove("jwt_token").apply()
}
```

#### Implementación iOS: Keychain Services (`Security.framework`) — *[Implementación Objetivo para macOS]*
> **Nota de Entorno de Desarrollo y Arquitectura:**  
> Esta implementación con `Security.framework` representa la **solución de producción definitiva a compilar y validar en un entorno macOS con Xcode**. En estaciones de trabajo Windows (donde no están disponibles los encabezados nativos de Apple), el proyecto utiliza un adaptador en memoria (`AlmacenSeguroIos` con `private var tokenMemoria: String?`) para garantizar la compilación multiplataforma sin dependencias incompatibles. La receta siguiente se presenta como el diseño técnico objetivo para el despliegue final en iOS:
```kotlin
// iosMain
package pe.edu.upeu.app.data.local

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

@OptIn(ExperimentalForeignApi::class)
class AlmacenSeguroIos : AlmacenSeguro {
    private val servicio = "pe.edu.upeu.app.auth"
    private val cuentaToken = "jwt_token"

    override fun guardarToken(token: String) {
        borrarToken() // Eliminar registro previo para evitar colisión de claves
        val datos = (token as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        memScoped {
            val query = CFDictionaryCreateMutable(null, 4, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(servicio as NSString))
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(cuentaToken as NSString))
            CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(datos))
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)

            SecItemAdd(query, null)
            CFRelease(query)
        }
    }

    override fun leerToken(): String? = memScoped {
        val query = CFDictionaryCreateMutable(null, 5, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(servicio as NSString))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(cuentaToken as NSString))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val resultado = alloc<CFTypeRefVar>()
        val estado = SecItemCopyMatching(query, resultado.ptr)
        CFRelease(query)

        if (estado == errSecSuccess) {
            val datos = resultado.value as? NSData
            datos?.let { NSString.create(it, NSUTF8StringEncoding) as String }
        } else null
    }

    override fun borrarToken() {
        memScoped {
            val query = CFDictionaryCreateMutable(null, 3, null, null)
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(servicio as NSString))
            CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(cuentaToken as NSString))
            SecItemDelete(query)
            CFRelease(query)
        }
    }
}
```

### 9.2. Flujo de Sesión Offline y Revalidación Previa a Sincronización
1. **Arranque sin Conexión:** Si el usuario abre la aplicación en modo avión con credenciales guardadas, la aplicación transiciona a `SesionEstado.SoloLecturaOffline` y renderiza el contenido local disponible, evitando bloquear la pantalla con un login imposible de completar.
2. **Restablecimiento de Red:** Al detectar conectividad, el sistema ejecuta **primero** la revalidación del token (`GET /auth/me`).
   - Si el servidor responde **401 Unauthorized**, el interceptor borra el token y transiciona a `SesionEstado.Expirado`.
   - Si responde **200 OK**, la sesión pasa a `SesionEstado.Autenticado`.
   - **Solo tras confirmar permisos y autenticidad** se autoriza la sincronización de datos con el servidor.

---

# 10. Receta: Persistencia Reactiva y Estrategia Offline-First `[SOLO SI APLICA AL ALCANCE]`

### 10.1. Esquema Relacional con SQLDelight (`Recurso.sq`)
```sql
CREATE TABLE entidad_local (
    id INTEGER NOT NULL PRIMARY KEY,
    identificador TEXT NOT NULL,
    nombre TEXT NOT NULL,
    metrica REAL NOT NULL,
    unidades INTEGER NOT NULL,
    activo INTEGER NOT NULL DEFAULT 1,
    pendiente INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE sincronizacion_metadata (
    id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
    ultimo_sync_millis INTEGER NOT NULL,
    ultimo_sync_formateado TEXT NOT NULL
);

listarTodos:
SELECT * FROM entidad_local ORDER BY nombre ASC;

listarPendientes:
SELECT * FROM entidad_local WHERE pendiente = 1 ORDER BY id DESC;

insertar:
INSERT OR REPLACE INTO entidad_local(id, identificador, nombre, metrica, unidades, activo, pendiente)
VALUES (?, ?, ?, ?, ?, ?, ?);

reemplazarIdTemporal:
UPDATE entidad_local
SET id = :nuevoId, pendiente = 0
WHERE id = :idTemporal;

borrarSincronizados:
DELETE FROM entidad_local WHERE pendiente = 0;

siguienteIdNegativo:
SELECT COALESCE(MIN(id), 0) - 1 FROM entidad_local WHERE id < 0;

guardarMetadataSync:
INSERT OR REPLACE INTO sincronizacion_metadata(id, ultimo_sync_millis, ultimo_sync_formateado)
VALUES (1, ?, ?);

obtenerMetadataSync:
SELECT * FROM sincronizacion_metadata WHERE id = 1;
```

### 10.2. Reglas del Repositorio Offline-First
1. **Altas con Identificadores Negativos:** Si no hay red, la entidad se guarda en local con `id = -1, -2...` y `pendiente = 1`.
2. **Reconciliación Atómica:** Al sincronizar, la respuesta con ID permanente positivo del servidor reemplaza la clave negativa dentro de una transacción SQLite indivisible.
3. **Preservación de Pendientes Locales:** La descarga del catálogo remoto borra únicamente registros con `pendiente = 0`. Las altas offline del usuario permanecen intactas.
4. **Timestamp Condicionado:** La marca de tiempo en `sincronizacion_metadata` se guarda **exclusivamente tras el éxito total de la subida y la bajada**. Si cualquiera de las dos falla, el timestamp previo se mantiene intacto.

---

# PARTE III: EVALUACIÓN, CALIDAD Y TESTING

---

# 11. Estrategia de Pruebas Unitarias

### 11.1. Las Seis Pruebas Académicas Requeridas en `commonTest` `[OBLIGATORIO PARA ESTE CURSO]`
La rúbrica académica exige que la suite de seis pruebas se defina en **`commonTest`** usando dobles de prueba (`Fakes` o `MockEngine`), verificando el comportamiento offline, los contratos de repositorio, las transiciones de estado y las emisiones reactivas del `Flow` en milisegundos sin requerir emuladores:

| N.° | Nombre de la Prueba | Qué Debe Verificar | Resultado Esperado |
| :---: | :--- | :--- | :--- |
| **1** | `sincronizacionConRed_pueblaDatos` | Tras sincronizar con red disponible, la base local contiene los datos del servidor. | El `Flow` emite la lista completa de elementos con `pendiente = 0`. |
| **2** | `registroSinRed_guardaPendiente` *(Esencial)* | Al fallar el envío por falta de red, la entidad se persiste con marca de pendiente. | Se genera un ID negativo (`< 0`), `pendiente = 1` y el `Flow` lo emite de inmediato. |
| **3** | `envioAlReconectar_eliminaMarcaPendiente` | Al sincronizar con el servidor activo, las filas pendientes se suben vía POST. | El ID negativo se reemplaza por el ID positivo remoto y `pendiente` pasa a 0. |
| **4** | `descargaNoBorraPendientes` *(Esencial)* | Traer el catálogo completo del servidor no debe purgar las filas creadas offline. | Coexisten las filas recién descargadas y las filas locales pendientes intactas. |
| **5** | `identificadoresSinColision` | Entidades locales y del servidor con numeraciones distintas conviven sin conflicto. | Una entidad con ID `-1` y otra con ID `1` conviven pacíficamente en la tabla. |
| **6** | `sincronizarDosVecesNoDuplica` | La sincronización sucesiva debe ser una operación idempotente. | La cantidad de filas no se altera tras ejecutar sincronizaciones consecutivas. |

> **Orientación de la Cátedra:** Las **pruebas 2 y 4 son esenciales**. Son las que acreditan que el patrón Offline-First está verdaderamente implementado; si alguna de las dos no pasa, el resto de la suite no compensa la evaluación.

### 11.2. Pruebas de Integración Real de SQLDelight en `androidHostTest` `[RECOMENDADO]`
Debido a que `JdbcSqliteDriver.IN_MEMORY` es un artefacto exclusivo de la JVM (`app.cash.sqldelight:sqlite-driver`), no puede enlazarse en pruebas nativas puras de `commonTest` para iOS. Por tanto, la verificación de integración real de la base de datos se delega a **`androidHostTest`**:
- **Validación del Esquema DDL y Consultas SQL:** Ejecución real de sentencias de `Recurso.sq` sobre un SQLite físico en memoria.
- **Concurrencia con `Mutex`:** Verificación de que dos llamadas simultáneas a `sincronizar()` y `registrar()` se serialicen correctamente evitando duplicidad de peticiones HTTP.
- **Resiliencia de Metadatos:** Comprobación de que si la sincronización falla a mitad del proceso, el timestamp previo en la tabla `sincronizacion_metadata` permanece inalterado.

---

# 12. Rúbrica de Cumplimiento y Anti-Patrones Prohibidos

### 12.1. Criterios de Evaluación por Etiquetas

#### `[OBLIGATORIO PARA ESTE CURSO]`
- [x] Separación estricta de capas: Dominio puro, Presentación y Datos.
- [x] Ausencia total de dependencias de plataforma (`android.*`, `java.*`) en `commonMain`.
- [x] Modelado inmutable de entidades con validación estricta en `init { require(...) }`.
- [x] Elevación de estado (State Hoisting): la lógica de negocio reside en `ViewModels`, nunca en `remember`.
- [x] Inyección de dependencias modular y desacoplada con Koin.
- [x] Suite de pruebas unitarias automatizadas pasando limpiamente en `commonTest`.

#### `[RECOMENDADO]`
- [x] Diseño responsivo adaptativo con breakpoints (`BoxWithConstraints` para tablet y móvil).
- [x] Paleta semántica Material 3 con soporte natural para Modo Claro y Modo Oscuro.
- [x] Pruebas de integración sobre el host local (`androidHostTest`).
- [x] Indicador de frescura de datos en pantalla en lenguaje natural (*«Actualizado hace 15 minutos»*).

#### `[SOLO SI APLICA AL ALCANCE]`
- [x] Integración de Ktor Client con manejo de los 5 estados de error de red (para proyectos con backend).
- [x] Almacenamiento seguro nativo con Keystore y Keychain (para proyectos con autenticación JWT).
- [x] Matriz de permisos RBAC para vistas y controles de mutación (para proyectos multiusuario).
- [x] SQLite local reactivo como Única Fuente de Verdad y reconciliación de IDs (para proyectos Offline-First).

---

### 12.2. Catálogo de Anti-Patrones Prohibidos (Penalizados en Evaluación)
1. ❌ **Lógica de negocio en la UI:** Calcular validaciones o totales directamente dentro de los composables o en bloques `onClick`.
2. ❌ **Estados volátiles en vistas:** Guardar datos del modelo con `remember` provocando pérdida de datos ante recomposiciones o rotación de pantalla.
3. ❌ **Contraseñas o tokens quemados:** Dejar cadenas de autenticación incrustadas en el código fuente.
4. ❌ **Llamadas imperativas redundantes:** Invocar funciones manuales de recarga tras operaciones CRUD cuando se dispone de un `Flow` reactivo.
5. ❌ **Edición de entidades pendientes:** Intentar modificar o eliminar registros locales con ID negativo antes de su reconciliación con el servidor.
6. ❌ **Silenciar errores:** Bloques `catch (e: Exception) { }` vacíos que ocultan fallos de red o de base de datos ante el usuario.

---

### 12.3. Comandos de Verificación Rápida
```powershell
# 1. Ejecutar pruebas unitarias multiplataforma (commonTest):
.\gradlew :shared:allTests

# 2. Ejecutar pruebas de integración en host Android:
.\gradlew :shared:testAndroidHostTest

# 3. Compilación limpia del APK:
.\gradlew :androidApp:assembleDebug
```
