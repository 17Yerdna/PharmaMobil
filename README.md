This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

## Sesión 07 — Cliente Ktor y Consumo GET

### Configuración del API REST
- **URL Base:** `https://api.escuelajs.co/api/v1/`
- **Endpoint consumido:** `GET /products?limit=10`
- **Cabeceras:** `Content-Type: application/json`

### Estructura de DTOs (`ProductoDto`)
| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `Long` | Identificador único del producto |
| `title` | `String` | Nombre o título del producto (mapeado a `nombre` en dominio) |
| `price` | `Double` | Precio unitario del producto (mapeado a `precio` en dominio) |
| `description` | `String` | Descripción detallada |
| `images` | `List<String>` | Lista de URLs de imágenes del producto |
| `category` | `CategoriaDto` | Categoría asociada (`id`, `name`) |

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…