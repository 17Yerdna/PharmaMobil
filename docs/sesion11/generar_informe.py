"""Genera el informe verificable de la actividad autónoma de la sesión 11."""

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    HRFlowable,
    Image,
    Flowable,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


HERE = Path(__file__).resolve().parent
PDF = HERE / "S11_ActividadAutonoma_Mestanza_Bazan.pdf"
FONT_DIR = Path(r"C:\Windows\Fonts")
pdfmetrics.registerFont(TTFont("Times", str(FONT_DIR / "times.ttf")))
pdfmetrics.registerFont(TTFont("Times-Bold", str(FONT_DIR / "timesbd.ttf")))
pdfmetrics.registerFont(TTFont("Times-Italic", str(FONT_DIR / "timesi.ttf")))
pdfmetrics.registerFontFamily("Times", normal="Times", bold="Times-Bold", italic="Times-Italic")

NAVY = colors.HexColor("#17365A")
BLUE = colors.HexColor("#31577B")
PALE = colors.HexColor("#EDF3F8")
GOLD = colors.HexColor("#C39635")
GRAY = colors.HexColor("#4E5964")

body = ParagraphStyle("body", fontName="Times", fontSize=12, leading=17, alignment=TA_JUSTIFY, spaceAfter=9)
sub = ParagraphStyle("sub", fontName="Times-Bold", fontSize=15.5, leading=19, textColor=BLUE, spaceBefore=16, spaceAfter=8)
heading = ParagraphStyle("heading", fontName="Times-Bold", fontSize=16, leading=20, textColor=NAVY, spaceBefore=5, spaceAfter=9)
caption = ParagraphStyle("caption", fontName="Times-Italic", fontSize=11, leading=14, textColor=GRAY, alignment=TA_LEFT, spaceBefore=5)
cell = ParagraphStyle("cell", fontName="Times", fontSize=11, leading=14)
cell_head = ParagraphStyle("cellhead", parent=cell, fontName="Times-Bold", textColor=colors.white)
cover_univ = ParagraphStyle("univ", fontName="Times-Bold", fontSize=17, leading=21, textColor=NAVY, alignment=TA_CENTER)
cover_fac = ParagraphStyle("fac", fontName="Times", fontSize=13, leading=18, alignment=TA_CENTER)
cover_kind = ParagraphStyle("kind", fontName="Times-Bold", fontSize=15, leading=20, textColor=BLUE, alignment=TA_CENTER)
cover_title = ParagraphStyle("title", fontName="Times-Bold", fontSize=23, leading=29, textColor=NAVY, alignment=TA_CENTER)
cover_sub = ParagraphStyle("subtitle", fontName="Times-Italic", fontSize=14, leading=19, alignment=TA_CENTER)
cover_meta = ParagraphStyle("meta", fontName="Times", fontSize=12, leading=17)


def p(text, style=body):
    return Paragraph(text, style)


def table(headers, rows, widths):
    data = [[p(x, cell_head) for x in headers]] + [
        [x if isinstance(x, (Flowable, list)) else p(str(x), cell) for x in row] for row in rows
    ]
    item = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    item.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), .45, colors.HexColor("#BBC8D3")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, PALE]),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return item


def figure(number, filename, description):
    source = HERE / "capturas" / filename
    width, height = ImageReader(str(source)).getSize()
    display_width = 215 if height > width else 430
    img = Image(str(source), width=display_width, height=display_width * height / width)
    return [img, p(f"<b>Figura {number}.</b> {description}", caption)]


def footer(canvas, doc):
    if doc.page == 1:
        return
    canvas.saveState()
    width, _ = A4
    canvas.setStrokeColor(colors.HexColor("#C9D4DE"))
    canvas.line(51, 45, width - 51, 45)
    canvas.setFont("Times", 9)
    canvas.setFillColor(GRAY)
    canvas.drawString(51, 31, "PharmaMobil · Desarrollo de Aplicaciones Móviles · Sesión 11")
    canvas.drawRightString(width - 51, 31, str(doc.page))
    canvas.restoreState()


story = []
logo = HERE / "logo_upeu.png"
story += [Spacer(1, 38), Image(str(logo), width=350, height=350 * 194 / 800), Spacer(1, 21)]
story += [p("UNIVERSIDAD PERUANA UNIÓN", cover_univ), Spacer(1, 7)]
story += [p("Facultad de Ingeniería y Arquitectura<br/>Escuela Profesional de Ingeniería de Sistemas", cover_fac)]
story += [Spacer(1, 56), HRFlowable(width="26%", thickness=2, color=GOLD, hAlign="CENTER"), Spacer(1, 24)]
story += [p("INFORME DE ACTIVIDAD AUTÓNOMA · SESIÓN 11", cover_kind), Spacer(1, 15)]
story += [p("Persistencia local y arquitectura Offline-First en PharmaMobil", cover_title), Spacer(1, 16)]
story += [p("SQLDelight 2.1.0 · SQLite · sincronización de productos · Kotlin Multiplatform", cover_sub)]
story += [Spacer(1, 58)]
story += [table(["Dato académico", "Información"], [
    ("Asignatura", "Desarrollo de Aplicaciones Móviles"),
    ("Estudiante", "Andrey Fidel Mestanza Bazán"),
    ("Docente", "Mg. Benjamín David Reyna Barreto"),
    ("Ciclo y semestre", "VI ciclo · 2026-II"),
    ("Fecha", "21 de octubre de 2026"),
], [133, 356]), Spacer(1, 19), p("Lima, Perú · 2026", cover_fac), PageBreak()]

story += [p("1. Introducción y objetivos", heading)]
story += [p("Una aplicación farmacéutica necesita conservar la consulta del inventario y permitir registrar productos cuando la conexión no está disponible. En PharmaMobil, SQLDelight convierte SQLite en la fuente local observada por la interfaz. El repositorio envía los registros pendientes a PharmaSoft cuando se restablece la red y actualiza después el catálogo local.")]
story += [p("El objetivo de esta actividad fue demostrar: (1) un esquema local para productos y estado pendiente; (2) consulta reactiva desde la base; (3) altas sin red con identificadores negativos; (4) reconciliación local transaccional con el identificador del servidor; y (5) conservación del inventario ante un error de conexión.")]
story += [p("Alcance de la validación", sub)]
story += [p("La lógica reside principalmente en <b>shared/commonMain</b>. Hay creadores de driver Android e iOS, pero las pruebas ejecutadas y capturas de esta entrega son de Android; no se afirma haber compilado ni probado iOS. La verificación usa un emulador Android, pruebas de host con SQLite en memoria y MockEngine, un archivo SQLite exportado y una consulta autenticada al backend local.")]
story += [p("2. Diseño e implementación", heading)]
story += [p("El esquema <b>Producto.sq</b> define ocho columnas: id, nombre, precio, stock, activo, imagen, categoria y pendiente. Las consultas <b>listarTodos</b> y <b>listarPendientes</b> alimentan la lectura; <b>insertar</b>, <b>borrarPorId</b>, <b>borrarSincronizados</b> y <b>siguienteIdLocal</b> sostienen las operaciones de persistencia. No existe una consulta SQL llamada reemplazarIdTemporal.")]
story += [table(["Componente", "Papel en el flujo"], [
    ("ProductoLocal", "Convierte las consultas SQLDelight en Flow, asigna IDs negativos y ejecuta la sustitución del ID dentro de una transacción SQLite."),
    ("ProductoRepository", "Contrato de dominio para observar, registrar, sincronizar y consultar la última hora de sincronización."),
    ("ProductoRepositorioOffline", "Coordina SQLite y Ktor. Un Mutex compartido serializa registro y sincronización para impedir publicaciones concurrentes duplicadas."),
    ("ProductoViewModel", "Presenta el Flow local, el aviso de conectividad y el estado de productos pendientes."),
], [145, 344])]
story += [Spacer(1, 8), p("El alta sin red guarda primero el producto con ID negativo y pendiente = 1. La sincronización publica los pendientes, recibe un ID positivo y, en una transacción local, borra la fila temporal e inserta la respuesta remota con pendiente = 0. Después descarga los productos del servidor sin borrar los pendientes que todavía no se hayan enviado. El Mutex evita que un alta simultánea con una sincronización publique dos veces el mismo registro.")]

story += [PageBreak(), p("3. Evidencias de ejecución en Android", heading)]
story += [p("Las capturas siguientes corresponden al recorrido visible en el emulador. Cada una documenta el estado de la interfaz, no una medición de tiempo ni una prueba de reinicio completo de la aplicación.")]
pair = [
    figure(1, "captura_s11_02_modo_avion_cache.png", "Inventario visible en modo avión. Los productos previamente persistidos permanecen consultables sin respuesta HTTP."),
    figure(2, "captura_s11_03_alta_offline_id_negativo.png", "Alta de «Ibuprofeno 800 mg Forte» sin red: ID temporal negativo, distintivo pendiente y aviso de un producto por sincronizar."),
]
story += [table(["Lectura local sin red", "Alta local pendiente"], [[pair[0], pair[1]]], [244, 245]), PageBreak()]
story += [p("3. Evidencias de ejecución en Android (continuación)", heading)]
pair = [
    figure(3, "captura_s11_04_sincronizacion_reemplazo_id.png", "Tras reconectar, el producto aparece con ID #006 y sin distintivo pendiente. El mensaje transitorio de alta local aún visible no prueba por sí solo la sincronización."),
    figure(4, "captura_s11_05_error_red_resiliencia.png", "El aviso «Modo sin conexión» convive con el catálogo local; un fallo de red no vacía la lista visible."),
]
story += [table(["Reconciliación visible", "Resiliencia ante error"], [[pair[0], pair[1]]], [244, 245])]
story += [Spacer(1, 11), p("La captura anteriormente rotulada como «Database Inspector» se excluye como evidencia del IDE porque su procedencia no pudo verificarse. Se reemplaza por una consulta reproducible del archivo SQLite exportado en la sección siguiente.")]

story += [PageBreak(), p("4. Verificaciones reproducibles", heading)]
story += [p("4.1 Base SQLite exportada", sub)]
story += [p("El archivo de evidencia <b>pharma_synced.db</b> se abrió como SQLite. <b>PRAGMA table_info(producto)</b> devuelve las ocho columnas esperadas; <b>SELECT COUNT(*), SUM(pendiente) FROM producto</b> devuelve 6 productos y 0 pendientes. El registro ID 6 contiene «Ibuprofeno 800 mg Forte», precio S/ 15.50 y stock 40. Esta inspección acredita el archivo exportado, pero no sustituye una captura auténtica del Database Inspector de Android Studio.")]
story += [p("4.2 Backend PharmaSoft", sub)]
story += [p("Una solicitud autenticada a <b>GET /api/v1/productos?pagina=0&amp;tamanio=100</b> devolvió seis productos; el ID 6 coincide en nombre, precio y stock con la base exportada. La misma ruta sin JWT respondió <b>401</b>. La verificación se hizo por cliente HTTP; no se presenta como captura de la respuesta en Swagger UI.")]
story += [table(["Fuente", "ID", "Producto", "Precio", "Stock", "Estado"], [
    ("SQLite exportado", "6", "Ibuprofeno 800 mg Forte", "S/ 15.50", "40", "pendiente = 0"),
    ("API autenticada", "6", "Ibuprofeno 800 mg Forte", "S/ 15.50", "40", "respuesta 200"),
], [94, 29, 171, 63, 41, 91])]
story += [p("4.3 Pruebas automatizadas", sub)]
story += [p("La ejecución de <b>:shared:testAndroidHostTest</b> terminó con <b>54 pruebas aprobadas y 0 fallidas</b>. Siete pruebas de <b>ProductoRepositorioOfflineTest</b> usan SQLite en memoria y Ktor MockEngine; incluyen el caso concurrente <b>registrarMientrasSincroniza_noPublicaDosVecesElMismoPendiente</b>, que falló antes de proteger registrar con el Mutex y pasó después. Tres pruebas nuevas de <b>ProductoOfflineViewModelTest</b> residen en commonTest y verifican lectura del catálogo sin red, alta pendiente y reconciliación al reconectar mediante un repositorio falso.")]
story += [p("La captura anterior de Gradle con 50/50 pruebas corresponde a una ejecución histórica. El resultado actual de 54/54 se comunica como salida de la ejecución local; no se reutiliza esa captura como si fuese nueva.")]

story += [PageBreak(), p("5. Análisis y conclusiones", heading)]
story += [table(["Aspecto", "Antes: REST directo", "Ahora: Offline-First"], [
    ("Lectura", "Dependía de la respuesta HTTP.", "Proviene del Flow observado en SQLite."),
    ("Sin red", "No se podía actualizar el catálogo.", "El catálogo ya persistido sigue visible."),
    ("Alta", "Requería servidor disponible.", "Se guarda con ID negativo y marca pendiente."),
    ("Reconciliación", "No aplicaba.", "Se reemplaza el ID dentro de una transacción local."),
], [90, 185, 214])]
story += [Spacer(1, 11), p("La actividad demuestra la disponibilidad local de productos y la continuidad de altas en modo avión. Los IDs negativos separan el estado local del remoto; el Mutex compartido evita duplicar el POST cuando coinciden un registro y una sincronización. Las pruebas de repositorio y ViewModel cubren los flujos centrales, mientras que la respuesta autenticada del backend confirma la presencia del producto sincronizado.")]
story += [p("Límites y trabajo pendiente", sub)]
story += [p("La transacción SQLite es local, no distribuida con el servidor: una caída después de un POST exitoso y antes del reemplazo local podría requerir un mecanismo de idempotencia para evitar duplicados al reintentar. La hora de última sincronización se mantiene en memoria y no debe describirse como persistente tras reinicio. Para una entrega con evidencia visual literal de la guía aún conviene obtener una captura auténtica de Android Studio Database Inspector y otra de la respuesta autenticada en Swagger UI. La implementación y pruebas de iOS quedan fuera del alcance acordado con el docente.")]
story += [p("Referencias de implementación", sub)]
story += [p("Guía práctica y actividad autónoma de la sesión 11, Desarrollo de Aplicaciones Móviles, Universidad Peruana Unión; código fuente de PharmaMobil en los módulos shared y androidApp; documentación oficial de SQLDelight 2.1.0 (Multiplatform SQLite y consultas reactivas).")]

doc = SimpleDocTemplate(str(PDF), pagesize=A4, leftMargin=51, rightMargin=51, topMargin=50, bottomMargin=57,
                        title="PharmaMobil · Actividad autónoma · Sesión 11", author="Andrey Fidel Mestanza Bazán")
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print(PDF)
