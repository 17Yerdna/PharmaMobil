package pe.edu.upeu.pharmamobil.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Biblioteca de vectores de iconos para PharmaMobil.
 * Implementados directamente con ImageVector para total compatibilidad con Compose Multiplatform
 * sin dependencias externas inestables.
 */
object AppIcons {

    val Menu: ImageVector = ImageVector.Builder(
        name = "Menu",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 18f)
        horizontalLineTo(21f)
        verticalLineTo(16f)
        horizontalLineTo(3f)
        verticalLineTo(18f)
        close()
        moveTo(3f, 13f)
        horizontalLineTo(21f)
        verticalLineTo(11f)
        horizontalLineTo(3f)
        verticalLineTo(13f)
        close()
        moveTo(3f, 6f)
        verticalLineTo(8f)
        horizontalLineTo(21f)
        verticalLineTo(6f)
        horizontalLineTo(3f)
        close()
    }.build()

    val Home: ImageVector = ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(10f, 20f)
        verticalLineTo(14f)
        horizontalLineTo(14f)
        verticalLineTo(20f)
        horizontalLineTo(19f)
        verticalLineTo(12f)
        horizontalLineTo(22f)
        lineTo(12f, 3f)
        lineTo(2f, 12f)
        horizontalLineTo(5f)
        verticalLineTo(20f)
        close()
    }.build()

    val ShoppingCart: ImageVector = ImageVector.Builder(
        name = "ShoppingCart",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(7f, 18f)
        arcTo(2f, 2f, 0f, false, false, 5f, 20f)
        arcTo(2f, 2f, 0f, false, false, 7f, 22f)
        arcTo(2f, 2f, 0f, false, false, 9f, 20f)
        arcTo(2f, 2f, 0f, false, false, 7f, 18f)
        close()
        moveTo(1f, 2f)
        verticalLineTo(4f)
        horizontalLineTo(3f)
        lineTo(6.6f, 11.59f)
        lineTo(5.25f, 14.04f)
        arcTo(2f, 2f, 0f, false, false, 7f, 17f)
        horizontalLineTo(19f)
        verticalLineTo(15f)
        horizontalLineTo(7.42f)
        lineTo(8.1f, 13.75f)
        horizontalLineTo(15.55f)
        arcTo(2f, 2f, 0f, false, false, 17.3f, 12.5f)
        lineTo(20.88f, 6f)
        horizontalLineTo(5.21f)
        lineTo(4.27f, 4f)
        horizontalLineTo(1f)
        close()
        moveTo(17f, 18f)
        arcTo(2f, 2f, 0f, false, false, 15f, 20f)
        arcTo(2f, 2f, 0f, false, false, 17f, 22f)
        arcTo(2f, 2f, 0f, false, false, 19f, 20f)
        arcTo(2f, 2f, 0f, false, false, 17f, 18f)
        close()
    }.build()

    val Person: ImageVector = ImageVector.Builder(
        name = "Person",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 12f)
        arcTo(4f, 4f, 0f, false, false, 8f, 8f)
        arcTo(4f, 4f, 0f, false, false, 12f, 4f)
        arcTo(4f, 4f, 0f, false, false, 16f, 8f)
        arcTo(4f, 4f, 0f, false, false, 12f, 12f)
        close()
        moveTo(12f, 14f)
        curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
        close()
    }.build()

    val List: ImageVector = ImageVector.Builder(
        name = "List",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 13f)
        horizontalLineTo(5f)
        verticalLineTo(11f)
        horizontalLineTo(3f)
        verticalLineTo(13f)
        close()
        moveTo(3f, 17f)
        horizontalLineTo(5f)
        verticalLineTo(15f)
        horizontalLineTo(3f)
        verticalLineTo(17f)
        close()
        moveTo(3f, 9f)
        horizontalLineTo(5f)
        verticalLineTo(7f)
        horizontalLineTo(3f)
        verticalLineTo(9f)
        close()
        moveTo(7f, 13f)
        horizontalLineTo(21f)
        verticalLineTo(11f)
        horizontalLineTo(7f)
        verticalLineTo(13f)
        close()
        moveTo(7f, 17f)
        horizontalLineTo(21f)
        verticalLineTo(15f)
        horizontalLineTo(7f)
        verticalLineTo(17f)
        close()
        moveTo(7f, 7f)
        verticalLineTo(9f)
        horizontalLineTo(21f)
        verticalLineTo(7f)
        horizontalLineTo(7f)
        close()
    }.build()

    val DarkMode: ImageVector = ImageVector.Builder(
        name = "DarkMode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(12.3f, 2f)
        curveTo(6.34f, 2.25f, 1.58f, 7.37f, 2.03f, 13.39f)
        curveTo(2.44f, 18.89f, 6.94f, 23.36f, 12.44f, 23.77f)
        curveTo(18.46f, 24.22f, 23.58f, 19.46f, 23.83f, 13.5f)
        curveTo(22.86f, 13.78f, 21.84f, 13.91f, 20.8f, 13.9f)
        curveTo(15.76f, 13.88f, 11.66f, 9.77f, 11.69f, 4.73f)
        curveTo(11.7f, 3.81f, 11.91f, 2.92f, 12.3f, 2.1f)
        close()
    }.build()

    val LightMode: ImageVector = ImageVector.Builder(
        name = "LightMode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 7f)
        arcTo(5f, 5f, 0f, true, true, 12f, 17f)
        arcTo(5f, 5f, 0f, true, true, 12f, 7f)
        close()
        moveTo(11f, 1f)
        horizontalLineTo(13f)
        verticalLineTo(5f)
        horizontalLineTo(11f)
        close()
        moveTo(11f, 19f)
        horizontalLineTo(13f)
        verticalLineTo(23f)
        horizontalLineTo(11f)
        close()
        moveTo(1f, 11f)
        horizontalLineTo(5f)
        verticalLineTo(13f)
        horizontalLineTo(1f)
        close()
        moveTo(19f, 11f)
        horizontalLineTo(23f)
        verticalLineTo(13f)
        horizontalLineTo(19f)
        close()
    }.build()

    val Search: ImageVector = ImageVector.Builder(
        name = "Search",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(15.5f, 14f)
        horizontalLineTo(14.71f)
        lineTo(14.43f, 13.73f)
        arcTo(6.5f, 6.5f, 0f, true, false, 14f, 5f)
        arcTo(6.5f, 6.5f, 0f, false, false, 7.5f, 11.5f)
        arcTo(6.5f, 6.5f, 0f, false, false, 13.73f, 14.43f)
        lineTo(14f, 14.71f)
        verticalLineTo(15.5f)
        lineTo(19f, 20.49f)
        lineTo(20.49f, 19f)
        lineTo(15.5f, 14f)
        close()
        moveTo(10f, 14.5f)
        arcTo(4.5f, 4.5f, 0f, true, true, 14.5f, 10f)
        arcTo(4.5f, 4.5f, 0f, false, true, 10f, 14.5f)
        close()
    }.build()

    val Email: ImageVector = ImageVector.Builder(
        name = "Email",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 4f)
        horizontalLineTo(4f)
        curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
        lineTo(2f, 18f)
        curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
        horizontalLineTo(20f)
        curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
        verticalLineTo(6f)
        curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
        close()
        moveTo(20f, 8f)
        lineTo(12f, 13f)
        lineTo(4f, 8f)
        verticalLineTo(6f)
        lineTo(12f, 11f)
        lineTo(20f, 6f)
        verticalLineTo(8f)
        close()
    }.build()

    val Phone: ImageVector = ImageVector.Builder(
        name = "Phone",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(6.62f, 10.79f)
        curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
        lineTo(15.41f, 15.18f)
        curveTo(15.69f, 14.9f, 16.08f, 14.82f, 16.43f, 14.94f)
        curveTo(17.55f, 15.31f, 18.76f, 15.51f, 20f, 15.51f)
        curveTo(20.55f, 15.51f, 21f, 15.96f, 21f, 16.51f)
        verticalLineTo(20f)
        curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
        curveTo(10.61f, 21f, 3f, 13.39f, 3f, 4f)
        curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
        horizontalLineTo(7.5f)
        curveTo(8.05f, 3f, 8.5f, 3.45f, 8.5f, 4f)
        curveTo(8.5f, 5.25f, 8.7f, 6.45f, 9.07f, 7.57f)
        curveTo(9.18f, 7.92f, 9.1f, 8.31f, 8.82f, 8.59f)
        lineTo(6.62f, 10.79f)
        close()
    }.build()

    val Info: ImageVector = ImageVector.Builder(
        name = "Info",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        arcTo(10f, 10f, 0f, true, false, 12f, 22f)
        arcTo(10f, 10f, 0f, false, false, 12f, 2f)
        close()
        moveTo(13f, 17f)
        horizontalLineTo(11f)
        verticalLineTo(11f)
        horizontalLineTo(13f)
        verticalLineTo(17f)
        close()
        moveTo(13f, 9f)
        horizontalLineTo(11f)
        verticalLineTo(7f)
        horizontalLineTo(13f)
        verticalLineTo(9f)
        close()
    }.build()

    val Warning: ImageVector = ImageVector.Builder(
        name = "Warning",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(1f, 21f)
        horizontalLineTo(23f)
        lineTo(12f, 2f)
        lineTo(1f, 21f)
        close()
        moveTo(13f, 18f)
        horizontalLineTo(11f)
        verticalLineTo(16f)
        horizontalLineTo(13f)
        verticalLineTo(18f)
        close()
        moveTo(13f, 14f)
        horizontalLineTo(11f)
        verticalLineTo(10f)
        horizontalLineTo(13f)
        verticalLineTo(14f)
        close()
    }.build()

    val CheckCircle: ImageVector = ImageVector.Builder(
        name = "CheckCircle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        arcTo(10f, 10f, 0f, true, false, 12f, 22f)
        arcTo(10f, 10f, 0f, false, false, 12f, 2f)
        close()
        moveTo(10f, 17f)
        lineTo(5f, 12f)
        lineTo(6.41f, 10.59f)
        lineTo(10f, 14.17f)
        lineTo(17.59f, 6.58f)
        lineTo(19f, 8f)
        lineTo(10f, 17f)
        close()
    }.build()

    val Add: ImageVector = ImageVector.Builder(
        name = "Add",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 13f)
        horizontalLineTo(13f)
        verticalLineTo(19f)
        horizontalLineTo(11f)
        verticalLineTo(13f)
        horizontalLineTo(5f)
        verticalLineTo(11f)
        horizontalLineTo(11f)
        verticalLineTo(5f)
        horizontalLineTo(13f)
        verticalLineTo(11f)
        horizontalLineTo(19f)
        verticalLineTo(13f)
        close()
    }.build()

    val Close: ImageVector = ImageVector.Builder(
        name = "Close",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        lineTo(19f, 6.41f)
        close()
    }.build()

    val Refresh: ImageVector = ImageVector.Builder(
        name = "Refresh",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(17.65f, 6.35f)
        curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
        curveTo(7.58f, 4f, 4.01f, 7.58f, 4.01f, 12f)
        curveTo(4.01f, 16.42f, 7.58f, 20f, 12f, 20f)
        curveTo(15.73f, 20f, 18.84f, 17.45f, 19.73f, 14f)
        horizontalLineTo(17.65f)
        curveTo(16.83f, 16.33f, 14.61f, 18f, 12f, 18f)
        curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
        curveTo(6f, 8.69f, 8.69f, 6f, 12f, 6f)
        curveTo(13.66f, 6f, 15.14f, 6.69f, 16.22f, 7.78f)
        lineTo(13f, 11f)
        horizontalLineTo(20f)
        verticalLineTo(4f)
        lineTo(17.65f, 6.35f)
        close()
    }.build()
}

