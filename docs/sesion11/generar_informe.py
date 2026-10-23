"""
Generador del Informe Académico Formal y Verificable - Sesión 11: Actividad Autónoma
Desarrollo de Aplicaciones Móviles - Universidad Peruana Unión (UPeU)
Estudiante: Andrey Fidel Mestanza Bazán
Docente: Mg. Benjamin David Reyna Barreto
Ciclo y Semestre: VI Ciclo - Semestre 2026-2
Fecha: 26 de octubre de 2026
"""

from pathlib import Path
import os
import sys

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT, TA_RIGHT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    HRFlowable,
    Image,
    Flowable,
    KeepTogether,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

# ----------------------------------------------------------------------
# Configuración de Rutas y Fuentes Tipográficas Oficiales (Times New Roman)
# ----------------------------------------------------------------------
HERE = Path(__file__).resolve().parent
PDF_PATH = HERE / "S11_ActividadAutonoma_Mestanza_Bazan.pdf"
CAPTURES_DIR = HERE / "capturas"

FONT_DIR = Path(r"C:\Windows\Fonts")
pdfmetrics.registerFont(TTFont("Times", str(FONT_DIR / "times.ttf")))
pdfmetrics.registerFont(TTFont("Times-Bold", str(FONT_DIR / "timesbd.ttf")))
pdfmetrics.registerFont(TTFont("Times-Italic", str(FONT_DIR / "timesi.ttf")))
pdfmetrics.registerFont(TTFont("Times-BoldItalic", str(FONT_DIR / "timesbi.ttf")))
pdfmetrics.registerFontFamily(
    "Times",
    normal="Times",
    bold="Times-Bold",
    italic="Times-Italic",
    boldItalic="Times-BoldItalic"
)

# ----------------------------------------------------------------------
# Paleta de Colores Institucional y Sobria
# ----------------------------------------------------------------------
NAVY = colors.HexColor("#17365A")       # Primario títulos / encabezados
BLUE = colors.HexColor("#31577B")       # Secundario subtítulos
PALE = colors.HexColor("#F2F5F8")       # Fondos alternados de tabla
GOLD = colors.HexColor("#C39635")       # Líneas de acento
GRAY = colors.HexColor("#4A5568")       # Texto secundario / leyendas
BORDER_GRAY = colors.HexColor("#CBD5E1") # Rejilla de tablas
WHITE = colors.white
CODE_BG = colors.HexColor("#F8FAFC")

# ----------------------------------------------------------------------
# Estilos Tipográficos según Normas de la Actividad (Times New Roman)
# ----------------------------------------------------------------------
# Título 16 pt, Subtítulo 15.5 pt, Cuerpo 12 pt, Tablas 11 pt, Interlineado 1.15
body = ParagraphStyle(
    "body",
    fontName="Times",
    fontSize=12,
    leading=17,
    alignment=TA_JUSTIFY,
    spaceAfter=8
)

body_bold = ParagraphStyle(
    "body_bold",
    fontName="Times-Bold",
    fontSize=12,
    leading=17,
    alignment=TA_JUSTIFY,
    spaceAfter=8
)

heading = ParagraphStyle(
    "heading",
    fontName="Times-Bold",
    fontSize=16,
    leading=20,
    textColor=NAVY,
    spaceBefore=14,
    spaceAfter=8,
    keepWithNext=True
)

subheading = ParagraphStyle(
    "subheading",
    fontName="Times-Bold",
    fontSize=15.5,
    leading=19,
    textColor=BLUE,
    spaceBefore=12,
    spaceAfter=6,
    keepWithNext=True
)

caption = ParagraphStyle(
    "caption",
    fontName="Times-Italic",
    fontSize=10,
    leading=13,
    textColor=GRAY,
    alignment=TA_CENTER,
    spaceBefore=4,
    spaceAfter=8
)

cell = ParagraphStyle(
    "cell",
    fontName="Times",
    fontSize=11,
    leading=14,
    alignment=TA_LEFT
)

cell_bold = ParagraphStyle(
    "cell_bold",
    parent=cell,
    fontName="Times-Bold"
)

cell_code = ParagraphStyle(
    "cell_code",
    parent=cell,
    fontName="Times",
    fontSize=10,
    leading=13
)

cell_head = ParagraphStyle(
    "cellhead",
    parent=cell,
    fontName="Times-Bold",
    fontSize=11,
    leading=14,
    textColor=WHITE,
    alignment=TA_CENTER
)

script_speaker = ParagraphStyle(
    "script_speaker",
    fontName="Times-Bold",
    fontSize=11.5,
    leading=15,
    textColor=NAVY,
    spaceBefore=4,
    spaceAfter=2,
    keepWithNext=True
)

script_body = ParagraphStyle(
    "script_body",
    fontName="Times",
    fontSize=11,
    leading=15,
    alignment=TA_JUSTIFY,
    spaceAfter=6
)

# Estilos de Portada
cover_univ = ParagraphStyle(
    "cover_univ",
    fontName="Times-Bold",
    fontSize=17,
    leading=22,
    textColor=NAVY,
    alignment=TA_CENTER
)

cover_fac = ParagraphStyle(
    "cover_fac",
    fontName="Times",
    fontSize=13,
    leading=18,
    alignment=TA_CENTER
)

cover_kind = ParagraphStyle(
    "cover_kind",
    fontName="Times-Bold",
    fontSize=15,
    leading=20,
    textColor=BLUE,
    alignment=TA_CENTER
)

cover_title = ParagraphStyle(
    "cover_title",
    fontName="Times-Bold",
    fontSize=22,
    leading=28,
    textColor=NAVY,
    alignment=TA_CENTER
)

cover_sub = ParagraphStyle(
    "cover_sub",
    fontName="Times-Italic",
    fontSize=13.5,
    leading=18,
    alignment=TA_CENTER
)

# ----------------------------------------------------------------------
# Funciones Auxiliares de Maquetación
# ----------------------------------------------------------------------
def p(text, style=body):
    return Paragraph(text, style)

def create_table(headers, rows, widths, is_compact=False):
    table_data = [[p(x, cell_head) for x in headers]]
    for row in rows:
        row_cells = []
        for cell_val in row:
            if isinstance(cell_val, Flowable):
                row_cells.append(cell_val)
            else:
                row_cells.append(p(str(cell_val), cell))
        table_data.append(row_cells)

    tbl = Table(table_data, colWidths=widths, repeatRows=1, hAlign="CENTER")
    padding = 2.5 if is_compact else 3.5
    tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("ALIGN", (0, 0), (-1, 0), "CENTER"),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.5, BORDER_GRAY),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, PALE]),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), padding),
        ("BOTTOMPADDING", (0, 0), (-1, -1), padding),
    ]))
    return tbl

def make_fig_content(number, filename, description, target_width, max_h=210):
    source = CAPTURES_DIR / filename
    if not source.exists():
        return [p(f"<b>[Error: Imagen {filename} no encontrada]</b>", caption)]
    reader = ImageReader(str(source))
    width, height = reader.getSize()
    aspect = height / width
    disp_w = target_width
    disp_h = disp_w * aspect
    if disp_h > max_h:
        disp_h = max_h
        disp_w = disp_h / aspect
    img = Image(str(source), width=disp_w, height=disp_h, hAlign="CENTER")
    return [img, Spacer(1, 2), p(f"<b>Figura {number}.</b> {description}", caption)]

def build_figure(number, filename, description, target_width=None):
    source = CAPTURES_DIR / filename
    if not source.exists():
        return [p(f"<b>[Error: Imagen {filename} no encontrada]</b>", caption)]
    if target_width is None:
        reader = ImageReader(str(source))
        width, height = reader.getSize()
        target_width = 115 if height > width else 360
    return make_fig_content(number, filename, description, target_width, max_h=205)

def build_figure_pair(fig_num1, file1, desc1, fig_num2, file2, desc2):
    c1 = make_fig_content(fig_num1, file1, desc1, 110, max_h=160)
    c2 = make_fig_content(fig_num2, file2, desc2, 110, max_h=160)
    tbl = Table([[c1, c2]], colWidths=[240, 240], hAlign="CENTER")
    tbl.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("ALIGN", (0, 0), (-1, -1), "CENTER"),
        ("LEFTPADDING", (0, 0), (-1, -1), 2),
        ("RIGHTPADDING", (0, 0), (-1, -1), 2),
        ("TOPPADDING", (0, 0), (-1, -1), 1),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 1),
    ]))
    return tbl

def draw_footer_and_header(canvas, doc):
    if doc.page == 1:
        return
    canvas.saveState()
    width, height = A4
    
    # Encabezado fino
    canvas.setFont("Times", 8.5)
    canvas.setFillColor(GRAY)
    canvas.drawString(51, height - 35, "PharmaMobil · Desarrollo de Aplicaciones Móviles (Sesión 11)")
    canvas.drawRightString(width - 51, height - 35, "UPeU · Ciclo VI 2026-2")
    canvas.setStrokeColor(BORDER_GRAY)
    canvas.setLineWidth(0.5)
    canvas.line(51, height - 40, width - 51, height - 40)

    # Pie de página con numeración
    canvas.line(51, 45, width - 51, 45)
    canvas.setFont("Times", 9)
    canvas.drawString(51, 31, "Andrey Fidel Mestanza Bazán · Actividad Autónoma Offline-First")
    canvas.drawRightString(width - 51, 31, f"Página {doc.page}")
    canvas.restoreState()

# ----------------------------------------------------------------------
# Construcción del Documento Académico
# ----------------------------------------------------------------------
def generar_reporte():
    story = []

    # ==================================================================
    # PÁGINA 1: PORTADA OFICIAL
    # ==================================================================
    logo_path = HERE / "logo_upeu.png"
    if logo_path.exists():
        reader_l = ImageReader(str(logo_path))
        lw, lh = reader_l.getSize()
        story += [Spacer(1, 20), Image(str(logo_path), width=320, height=320 * lh / lw, hAlign="CENTER"), Spacer(1, 15)]
    else:
        story += [Spacer(1, 60)]

    story += [p("UNIVERSIDAD PERUANA UNIÓN", cover_univ), Spacer(1, 4)]
    story += [p("FACULTAD DE INGENIERÍA Y ARQUITECTURA<br/>Escuela Profesional de Ingeniería de Sistemas", cover_fac), Spacer(1, 35)]
    story += [HRFlowable(width="30%", thickness=2.5, color=GOLD, hAlign="CENTER"), Spacer(1, 20)]
    story += [p("INFORME DE ACTIVIDAD AUTÓNOMA — SESIÓN 11", cover_kind), Spacer(1, 10)]
    story += [p("Arquitectura Offline-First, Persistencia Multiplataforma y Reconciliación Transaccional en PharmaMobil", cover_title), Spacer(1, 12)]
    story += [p("Implementación canónica en SQLDelight 2.1.0, sincronización reactiva bidireccional,<br/>marca temporal persistente y resiliencia ante contingencias de conectividad", cover_sub), Spacer(1, 40)]

    meta_rows = [
        ("Asignatura:", "Desarrollo de Aplicaciones Móviles"),
        ("Estudiante:", "Andrey Fidel Mestanza Bazán"),
        ("Docente:", "Mg. Benjamin David Reyna Barreto"),
        ("Ciclo y Semestre:", "VI Ciclo · Semestre 2026-2"),
        ("Fecha de Entrega:", "26 de octubre de 2026"),
        ("Repositorio / Rama:", "feature/offline-first-mestanza"),
    ]
    meta_table = create_table(["Dato Académico", "Información Registrada"], meta_rows, [140, 352])
    story += [meta_table, Spacer(1, 25), p("Lima, Perú — 2026", cover_fac), PageBreak()]

    # ==================================================================
    # SECCIÓN 1: INTRODUCCIÓN Y ARQUITECTURA OFFLINE-FIRST
    # ==================================================================
    story += [p("1. Introducción y Arquitectura del Sistema", heading)]
    story += [p(
        "En entornos farmacéuticos y asistenciales, la disponibilidad continua del sistema constituye un "
        "requisito no negociable. Los profesionales de salud y dependientes de botica requieren consultar el "
        "catálogo de medicamentos, registrar nuevas existencias y verificar alertas de stock crítico sin verse "
        "interrumpidos por fallas de cobertura, caídas del enlace de datos o cortes de red local. En este "
        "contexto, la sesión 11 de la asignatura plantea la transición desde una arquitectura basada en peticiones "
        "REST directas hacia un modelo integral <b>Offline-First</b> respaldado por una base de datos local "
        "SQLite gestionada mediante <b>SQLDelight 2.1.0</b> en <b>Kotlin Multiplatform (KMP)</b>."
    )]
    story += [p(
        "La arquitectura desacoplada adoptada en PharmaMobil sitúa la persistencia local como la única fuente "
        "de verdad observada por la interfaz de usuario a través de flujos asíncronos reactivos (<code>Flow</code>). "
        "Cuando el dispositivo pierde conectividad, las consultas de lectura continúan operando instantáneamente "
        "desde SQLite; a su vez, las operaciones de alta se admiten de forma no bloqueante asignando identificadores "
        "locales negativos temporales (ej. -1, -2) y marcando los registros con la bandera booleana <code>pendiente = 1</code>. "
        "Al restablecerse el acceso al backend REST (PharmaSoft), un caso de uso especializado serializado mediante un "
        "<code>Mutex</code> de concurrencia compartido sube los registros pendientes, reconcilia atómicamente los IDs "
        "locales con los definitivos asignados por el servidor, purga las marcas de pendiente y descarga el catálogo consolidado."
    )]

    story += [p("Componentes Clave de la Arquitectura", subheading)]
    comp_rows = [
        ("ProductoLocal", "Encapsula las consultas generadas por SQLDelight. Expone el catálogo reactivo mediante Flow, asigna claves negativas secuenciales automáticas y efectúa la sustitución transaccional del ID sin exponer llamadas bloqueantes a la UI."),
        ("ProductoRepository", "Contrato de dominio puro que abstrae las operaciones de observación, inserción offline, sincronización bidireccional y consulta del timestamp persistido de sincronización."),
        ("ProductoRepositorioOffline", "Implementación orquestadora en shared/commonMain. Coordina SQLite y el cliente Ktor Http. Utiliza un Mutex compartido que impide condiciones de carrera entre mutaciones locales y sincronizaciones en vuelo."),
        ("SessionCoordinator", "Gestor reactivo del estado de autenticación. Administra las transiciones entre Autenticado, NoAutenticado, Expirado y el nuevo modo OfflineSoloLectura para garantizar seguridad sin red."),
        ("AlmacenSeguro", "Contrato multiplataforma implementado con EncryptedSharedPreferences (AES-256) en Android y Keychain en iOS. Almacena de forma cifrada el JWT y la identidad cacheada del usuario."),
        ("ProveedorHora", "Abstracción multiplataforma del reloj del sistema. Facilita la inyección de tiempo determinista en pruebas unitarias y el cálculo de marcas de sincronización exactas."),
    ]
    story += [create_table(["Componente", "Responsabilidad en el Flujo Offline-First"], comp_rows, [150, 342])]
    story += [PageBreak()]

    # ==================================================================
    # SECCIÓN 2: PRODUCTO 1 - PRUEBAS UNITARIAS MULTIPLATAFORMA (7 PTS)
    # ==================================================================
    story += [p("2. Producto 1: Pruebas Unitarias Multiplataforma (7 pts)", heading)]
    story += [p(
        "De estricto acuerdo con la rúbrica de evaluación y los principios de código limpio, la lógica de validación "
        "del repositorio se ubicó íntegramente en <b>shared/src/commonTest</b>. Para sortear la limitación de que "
        "<code>JdbcSqliteDriver</code> pertenece exclusivamente a la plataforma JVM/Android mientras el proyecto compila "
        "también para iOS, se implementó el patrón factoría multiplataforma <code>expect / actual</code> denominado "
        "<b>TestDriverFactory</b>."
    )]
    story += [p(
        "El contrato <code>expect fun crearDriverEnMemoria(): SqlDriver</code> declarado en commonTest se resuelve con "
        "<code>actual</code> mediante <code>JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)</code> en Android, y mediante la "
        "función canónica <code>inMemoryDriver(PharmaDb.Schema)</code> de SQLDelight 2.1.0 Native en iOS. Por su parte, la "
        "verificación de la migración de esquema (<code>1.sqm</code>) que actualiza la base de datos de versión 1 a versión 2 "
        "incorporando la tabla <code>metadata_sincronizacion</code> se ubicó en <code>androidHostTest</code>, garantizando que "
        "las bases de instalaciones antiguas no pierdan sus productos persistidos."
    )]

    story += [p("Resultados de la Suite Automatizada de Pruebas", subheading)]
    test_rows = [
        ("1. listarProductos_emiteProductosDesdeBaseLocal", "commonTest", "Lee catálogo directamente de SQLite sin invocar cliente de red.", "APROBADA"),
        ("2. registrar_asignaIdNegativoYGuardaPendiente", "commonTest", "Inserta ítem con ID -1 y pendiente=1; no contacta a Ktor.", "APROBADA"),
        ("3. sincronizar_subePendientesYActualizaIdLocal", "commonTest", "Sube pendiente con POST, sustituye ID local por el remoto (6) y limpia pendiente.", "APROBADA"),
        ("4. registrarMientrasSincroniza_serializaConMutex", "commonTest", "Simula concurrencia real; el Mutex evita duplicación de registros.", "APROBADA"),
        ("5. sincronizar_sinConexion_preservaProductosYMarcaPendiente", "commonTest", "Ktor MockEngine simula fallo de red; los pendientes quedan intactos.", "APROBADA"),
        ("6. sincronizar_falla_preservaTimestampPrevio", "commonTest", "Verifica que un fallo de red NO actualice la marca de tiempo en SQLite.", "APROBADA"),
        ("7. MigracionEsquemaHostTest.migracion_v1_a_v2_preserva_productos", "androidHostTest", "Crea esquema v1 con datos, ejecuta 1.sqm y valida tabla metadata y registros.", "APROBADA"),
    ]
    story += [create_table(["Nombre del Caso de Prueba", "Ubicación", "Comportamiento y Contrato Verificado", "Estado"], test_rows, [185, 80, 167, 60], is_compact=True)]
    story += [Spacer(1, 8)]

    story += [p(
        "<b>Certificación de Ejecución:</b> La ejecución formal del comando <code>./gradlew :shared:testAndroidHostTest</code> "
        "culminó exitosamente con <b>62 pruebas aprobadas, 0 fallidas y 0 omitidas</b> (incrementando la línea base previa de 54 pruebas). "
        "Respecto al target iOS, en conformidad con la dispensa académica formal acordada con el docente Mg. Benjamin David Reyna Barreto, "
        "se acredita la compatibilidad arquitectural del código fuente multiplataforma y la factoría <code>TestDriverFactory.ios.kt</code>, "
        "dejando constancia técnica de que las pruebas no fueron ejecutadas físicamente en simulador XCTest debido a restricciones del "
        "entorno operativo Windows anfitrión."
    )]
    story += [Spacer(1, 10)]

    # ==================================================================
    # SECCIÓN 3: PRODUCTO 2 - BITÁCORA DE ESCENARIOS OFFLINE (6 PTS)
    # ==================================================================
    story += [PageBreak(), p("3. Producto 2: Bitácora de Escenarios Offline (6 pts)", heading)]
    story += [p(
        "La validación empírica se condujo en un emulador Android oficial (Android 14 / API 34-36, densidad 1080x2400) "
        "interactuando contra la instancia local de backend <b>PharmaSoft</b> (Spring Boot 4.0.7 en puerto 8080 con base PostgreSQL). "
        "Para asegurar máxima integridad forense y consistencia de los archivos SQLite, cada extracción binaria de <code>pharma.db</code> "
        "estuvo precedida por la detención forzada de la aplicación (<code>adb shell am force-stop</code>), garantizando el vaciado "
        "completo y sincronizado de los registros WAL (<code>pharma.db-wal</code>) y memoria compartida (<code>pharma.db-shm</code>)."
    )]

    # --- Escenario 1 ---
    story += [p("Escenario 1: Modo Avión con Caché Previa", subheading)]
    e1_table = create_table(["Parámetro", "Detalle de Verificación"], [
        ("Fecha / Plataforma", "22/10/2026 16:08:22 · Android 14 (API 34) · Dispositivo emulator-5554"),
        ("Pasos de Prueba", "1. Poblar catálogo con 5 productos online. 2. Activar modo avión (svc wifi disable; cmd connectivity airplane-mode enable). 3. Reabrir app en Inventario."),
        ("Comportamiento UI", "Ícono de avión visible en barra de estado; se renderizan los 5 productos locales; banner ámbar ausente (pendiente=0); subtítulo 'Actualizado hace 7 minutos'."),
        ("Inspección SQLite", "SELECT id, nombre, pendiente FROM producto -> 5 filas devueltas (IDs 1 al 5), todas con pendiente = 0. Clave ultimo_sync presente en metadata_sincronizacion."),
        ("Conclusión Técnica", "La UI se alimenta estrictamente del Flow de SQLite local, garantizando lectura fluida sin generar bloqueos ni excepciones en modo desconectado."),
    ], [130, 362])
    story += [e1_table, Spacer(1, 6)]
    story += build_figure(1, "captura_s11_escenario_01_modo_avion_cache.png", "Escenario 1: Catálogo visible en modo avión con ícono activo y datos locales SQLite.")
    story += [Spacer(1, 12)]

    # --- Escenario 2 ---
    story += [p("Escenario 2: Alta de Producto Offline con ID Negativo Temporal", subheading)]
    e2_table = create_table(["Parámetro", "Detalle de Verificación"], [
        ("Fecha / Plataforma", "22/10/2026 16:24:10 · Android 14 (API 34) · Dispositivo emulator-5554"),
        ("Pasos de Prueba", "1. Mantener modo avión activo. 2. Pulsar botón '+ Nuevo'. 3. Diligenciar 'Ibuprofeno 800 mg Forte', precio S/ 12.50, stock 253. 4. Pulsar 'Registrar Producto'."),
        ("Comportamiento UI", "Formulario se cierra; aparece banner ámbar 'Cambios pendientes de sincronización (1)'; nueva tarjeta muestra ID '#00-1' y badge 'Bajo: 253 u.'."),
        ("Inspección SQLite", "SELECT id, nombre, precio, stock, pendiente FROM producto WHERE id < 0 -> (-1, 'Ibuprofeno 800 mg Forte', 12.50, 253, 1). Total en base = 6 productos."),
        ("Conclusión Técnica", "El sistema admite altas no bloqueantes en contingencia asignando ID provisional negativo y persistiendo la marca pendiente=1 para posterior reconciliación."),
    ], [130, 362])
    story += [e2_table, Spacer(1, 6)]
    story += build_figure(2, "captura_s11_escenario_02_alta_offline_id_negativo.png", "Escenario 2: Alta offline con identificador provisional #00-1 y banner de advertencia ámbar.")
    story += [Spacer(1, 12)]

    # --- Escenario 3 ---
    story += [PageBreak(), p("Escenario 3: Reconexión, Reconciliación Transaccional y Verificación Swagger UI", subheading)]
    e3_table = create_table(["Parámetro", "Detalle de Verificación"], [
        ("Fecha / Plataforma", "22/10/2026 16:28:45 · Android 14 (API 34) · Dispositivo emulator-5554"),
        ("Pasos de Prueba", "1. Restablecer conectividad (svc wifi enable; cmd connectivity airplane-mode disable). 2. Disparar sincronización manual/reactiva. 3. Inspeccionar SQLite y Swagger."),
        ("Comportamiento UI", "Banner ámbar desaparece; la tarjeta '#00-1' se transmuta reactivamente en '#006'; subtítulo se refresca a 'Actualizado hace unos momentos'."),
        ("Inspección SQLite", "SELECT id, nombre, pendiente FROM producto -> Fila reconciliada: id=6, pendiente=0. Total productos = 6, suma pendientes = 0. Clave ultimo_sync actualizada."),
        ("Backend PharmaSoft", "GET /api/v1/productos responde 200 OK en Swagger UI autenticado con Bearer JWT, confirmando el ítem ID 6 en base PostgreSQL."),
        ("Conclusión Técnica", "La transacción SQLite sustituye de forma atómica el identificador negativo por el asignado por el servidor sin inconsistencias ni duplicaciones."),
    ], [130, 362])
    story += [e3_table, Spacer(1, 6)]
    story += [build_figure_pair(
        "3A", "captura_s11_escenario_03_reconciliacion_servidor.png", "Reconciliación local: ítem actualizado a ID #006.",
        "3B", "captura_s11_swagger_autenticado.png", "Swagger UI: confirmación del registro ID 6 en PharmaSoft."
    )]
    story += [Spacer(1, 12)]

    # --- Escenario 4 ---
    story += [p("Escenario 4: Primera Apertura sin Red tras 'pm clear'", subheading)]
    e4_table = create_table(["Parámetro", "Detalle de Verificación"], [
        ("Fecha / Plataforma", "22/10/2026 18:25:30 · Android 14 (API 34) · Dispositivo emulator-5554"),
        ("Pasos de Prueba", "1. Ejecutar adb shell pm clear pe.edu.upeu.pharmamobil. 2. Activar modo avión. 3. Iniciar la aplicación por primera vez en frío."),
        ("Comportamiento UI", "La aplicación presenta la pantalla de Login con aviso contextual 'Sin conexión al servidor. Verifique su red' al intentar acceder."),
        ("Inspección SQLite", "run-as ... ls databases/ -> Carpeta inexistente. Sin token ni base local previa, el sistema impide accesos no autorizados a pantallas clínicas."),
        ("Declaración de Diseño", "El equipo técnico declara explícitamente que presentar LoginScreen constituye una interpretación de diseño defensivo: un borrado absoluto con pm clear purga la sesión cifrada y, por gobernanza y seguridad clínica, es mandatorio impedir el ingreso anónimo a inventario."),
    ], [130, 362])
    story += [e4_table, Spacer(1, 6)]
    story += build_figure(4, "captura_s11_escenario_04_primera_apertura_vacia.png", "Escenario 4: Compuerta de seguridad en Login tras pm clear en modo avión.")
    story += [Spacer(1, 12)]

    # --- Escenario 5 ---
    story += [PageBreak(), p("Escenario 5: Sesión Guardada en Modo Offline y Expulsión Reactiva por 401", subheading)]
    e5_table = create_table(["Parámetro", "Detalle de Verificación"], [
        ("Fecha / Plataforma", "22/10/2026 18:57:12 (5A) y 19:28:40 (5B) · Android 14 (API 34) · emulator-5554"),
        ("Pasos Escenario 5A", "1. Iniciar sesión online como Carlos Ramirez Torres. 2. Poner en modo avión y reiniciar. 3. App entra en OfflineSoloLectura con identidad cacheada."),
        ("UI Escenario 5A", "Usuario visible en Drawer y TopBar; catálogo visible; botones '+ Nuevo', Editar y Eliminar ocultos; Clientes omitido del menú por falta de permisos de mutación."),
        ("Pasos Escenario 5B", "1. Con backend configurado con secreto JWT alterado, reactivar Wi-Fi. 2. Disparar revalidación manual vía 'Sincronizar ahora'. 3. Backend rechaza con HTTP 401."),
        ("UI Escenario 5B", "Expulsión inmediata a LoginScreen con banner informativo: 'Tu sesión ha expirado o el token ya no es válido. Ingresa tus credenciales nuevamente.'"),
        ("Almacén Seguro", "Inspección de sesion_segura.xml confirma purga total de claves: 'jwt', 'cached_usuario', 'cached_nombre' y 'cached_permisos' completamente eliminados."),
        ("Conclusión Técnica", "El sistema garantiza acceso offline seguro y de solo lectura mientras no haya red, y audita estrictamente las credenciales al reconectar, expulsando sesiones no válidas."),
    ], [130, 362])
    story += [e5_table, Spacer(1, 6)]
    story += [build_figure_pair(
        "5A", "captura_s11_escenario_05a_modo_offline_solo_lectura.png", "Modo offline solo lectura: identidad cacheada y mutaciones bloqueadas.",
        "5B", "captura_s11_escenario_05b_expulsion_401.png", "Expulsión a Login tras recibir HTTP 401 en revalidación con backend."
    )]
    story += [Spacer(1, 12)]

    # ==================================================================
    # SECCIÓN 4: PRODUCTO 3 - MARCA TEMPORAL EN SQLITE Y LENGUAJE NATURAL (3 PTS)
    # ==================================================================
    story += [PageBreak(), p("4. Producto 3: Persistencia de Marca Temporal y Lenguaje Natural (3 pts)", heading)]
    story += [p(
        "En estricto cumplimiento de la guía de aprendizaje, la marca temporal de última sincronización se desacopló "
        "completamente de la memoria volátil del ViewModel para persistirse de forma canónica en SQLite. Se diseñó la tabla "
        "<b>metadata_sincronizacion</b> estructurada por clave-valor (<code>clave TEXT PRIMARY KEY, timestamp_millis INTEGER NOT NULL</code>), "
        "incorporada en el esquema principal mediante SQLDelight y migrada en instalaciones existentes mediante el script <code>1.sqm</code>."
    )]
    story += [p(
        "Una regla de negocio esencial implementada y verificada es que el timestamp asociado a la clave <code>ultimo_sync</code> "
        "<b>únicamente se actualiza cuando la sincronización concluye con éxito absoluto</b> (subida de todas las altas pendientes "
        "y descarga completa del catálogo remoto). Si ocurre cualquier excepción de conectividad, caída de red o error de servidor, "
        "la transacción no toca el registro de metadatos, preservando fidedignamente el instante en que los datos fueron consistentes por última vez."
    )]

    story += [p("Lógica de Formateo Relativo en Lenguaje Natural", subheading)]
    calc_rows = [
        ("Diferencia Temporal (Δt)", "Expresión Mostrada en Interfaz", "Criterio de Dominio Farmacéutico"),
        ("Sin registro previo (null o 0)", "Nunca se ha sincronizado", "Estado inicial antes de la primera apertura conectada."),
        ("Δt < 1 minuto", "Actualizado hace unos momentos", "Indica datos frescos recién reconciliados con el servidor."),
        ("1 min ≤ Δt < 60 min", "Actualizado hace X minuto(s)", "Permite precisión operativa durante la jornada laboral."),
        ("1 h ≤ Δt < 24 h", "Actualizado hace X hora(s)", "Advierte antigüedad moderada para toma de decisiones de stock."),
        ("Δt ≥ 24 horas (≥ 1 día)", "Actualizado hace más de un día", "Alerta clínica de alta antigüedad; sugiere forzar sincronización."),
    ]
    story += [create_table(["Condición de Tiempo", "Formateo en Lenguaje Natural", "Significado Operativo"], calc_rows, [140, 175, 177])]
    story += [Spacer(1, 8)]

    story += [p(
        "<b>Evidencia Empírica de Lenguaje Natural:</b> Para verificar que el formateo reacciona fidedignamente a marcas temporales "
        "antiguas, se modificó el valor de <code>ultimo_sync</code> en la base local SQLite a un instante situado a 26 horas previas "
        "mediante inyección controlada. Al recargar la vista de Inventario en modo avión, la interfaz presentó reactivamente la etiqueta "
        "<b>'Actualizado hace más de un día'</b> debajo del título institucional, demostrando la fidelidad del cálculo."
    )]
    story += [Spacer(1, 4)]
    story += [build_figure_pair(
        "6A", "captura_s11_escenario_03_reconciliacion_servidor.png", "Timestamp reciente: 'Actualizado hace unos momentos'.",
        "6B", "captura_s11_antiguedad_mas_de_un_dia.png", "Timestamp mayor a 24 horas: 'Actualizado hace más de un día'."
    )]
    story += [Spacer(1, 14)]

    # ==================================================================
    # SECCIÓN 5: PRODUCTO 4 - GUION DE EXPOSICIÓN TÉCNICA (2 PTS)
    # ==================================================================
    story += [p("5. Producto 4: Guion de Exposición Técnica de 8 Minutos (2 pts)", heading)]
    story += [p(
        "A continuación se presenta el guion técnico detallado, estructurado de forma rigurosa para cubrir con precisión "
        "cronometrada los cinco criterios de evaluación de la rúbrica institucional:"
    )]

    # Minuto 0:00 - 1:30
    story += [p("Bloque 1 (0:00 - 1:30): Autenticación JWT y Arquitectura de Sesión Segura", script_speaker)]
    story += [p(
        "<i>«Buenos días docente Mg. Benjamin David Reyna Barreto y compañeros. Presento la sustentación de la actividad autónoma "
        "de la sesión 11 en PharmaMobil. Comenzamos analizando la arquitectura de seguridad y gestión de sesiones. PharmaMobil "
        "se comunica contra el backend PharmaSoft implementado en Spring Boot. Al ingresar credenciales válidas en la pantalla de "
        "Login, el servidor emite un token JWT firmado con algoritmo HMAC-SHA512 que contiene las credenciales, tiempo de expiración "
        "y el conjunto de roles asignados al usuario.</i><br/>"
        "<i>Para almacenar este secreto de forma segura y cumplir con estándares de grado médico, implementamos la abstracción "
        "multiplataforma AlmacenSeguro: en Android hace uso de EncryptedSharedPreferences respaldado por llaves maestras en Android Keystore "
        "bajo esquema AES-256 GCM, mientras que en iOS emplea el Keychain nativo. El SessionCoordinator gestiona de manera reactiva el ciclo "
        "de vida: cuando hay red, valida el token con GET /auth/me; cuando no hay cobertura, restaura una sesión controlada de solo lectura "
        "con identidad cacheada; y si el token caduca o la firma es inválida, el interceptor 401 purga las credenciales y expulsa inmediatamente "
        "a la pantalla de acceso protegiendo los datos clínicos.»</i>",
        script_body
    )]

    # Minuto 1:30 - 3:00
    story += [p("Bloque 2 (1:30 - 3:00): Consumo de Endpoints REST y Ktor Http Client", script_speaker)]
    story += [p(
        "<i>«En el segundo bloque abordamos el consumo de servicios web. Toda la capa de red reside en el módulo shared/commonMain "
        "utilizando Ktor Client 3.1.1. Configuramos un motor HTTP común provisto de plugins clave: ContentNegotiation serializando JSON con "
        "KotlinX Serialization, HttpTimeout con políticas estrictas de conexión y lectura para evitar bloqueos del hilo principal, y "
        "Auth Bearer con HttpResponseValidator para capturar centralizadamente respuestas 401 y 403.</i><br/>"
        "<i>Consumimos los endpoints canónicos de la API REST de PharmaSoft: POST /api/v1/auth/login para la autenticación, GET /api/v1/auth/me "
        "para la revalidación de identidad, GET /api/v1/productos para la sincronización paginada del catálogo general, y POST /api/v1/productos "
        "para dar de alta nuevos fármacos. Todas las peticiones serializan DTOs inmutables mapeados hacia entidades del dominio mediante Result<T>, "
        "asegurando que ningún fallo de red genere caídas inesperadas en la aplicación móvil.»</i>",
        script_body
    )]

    # Minuto 3:00 - 4:30
    story += [p("Bloque 3 (3:00 - 4:30): Manejo Reactivo de Estados de UI y Resiliencia ante Errores", script_speaker)]
    story += [p(
        "<i>«El tercer criterio evalúa el manejo reactivo de la interfaz. Siguiendo la arquitectura MVI y StateFlow en Compose Multiplatform, "
        "la UI no mantiene datos mutables propios; consume el flujo ProductoUiState emitido por ProductoViewModel. Este estado expone "
        "propiedades inmutables como la lista de productos clasificados, bandera de carga estaSincronizando, el indicador booleano avisoRed, "
        "y el texto formateado en lenguaje natural textoAntiguedad.</i><br/>"
        "<i>Frente a situaciones anómalas de red —como desconexión súbita o latencia excesiva— el repositorio atrapa tipadamente excepciones "
        "como SinConexion o TiempoAgotado. En lugar de vaciar la pantalla o mostrar diálogos bloqueantes, la interfaz despliega un aviso visual "
        "no intrusivo informando al operador sobre la contingencia de conectividad, mientras la lista de medicamentos continúa mostrándose "
        "de manera íntegra e ininterrumpida desde la base de datos SQLite local.»</i>",
        script_body
    )]

    # Minuto 4:30 - 6:30
    story += [p("Bloque 4 (4:30 - 6:30): Arquitectura Offline-First, IDs Temporales y Reconciliación Transaccional", script_speaker)]
    story += [p(
        "<i>«Entrando al corazón técnico de la sesión 11: el patrón Offline-First. En PharmaMobil, SQLite no es un caché secundario; "
        "es la única fuente de verdad directa para la interfaz gráfica. Cuando el profesional activa el modo avión y da de alta un medicamento "
        "—como evidenciamos en el Escenario 2 con 'Ibuprofeno 800 mg Forte'— ProductoLocal le asigna un identificador local negativo "
        "secuencial (#00-1) y persiste la tupla con pendiente=1. La UI detecta de inmediato el estado pendiente y enciende un banner ámbar "
        "de advertencia para el farmacéutico.</i><br/>"
        "<i>Al restablecerse la conectividad, el método sincronizar() del ProductoRepositorioOffline entra en acción protegido por un Mutex "
        "compartido. Este cerrojo serializa los registros y la sincronización, garantizando que si el usuario registra un producto mientras la app "
        "está sincronizando, el nuevo ítem no se duplique ni se pierda. La sincronización sube los registros pendientes mediante POST; al recibir "
        "el ID definitivo generado por el backend (#006), ejecuta una transacción atómica en SQLite: elimina el ID provisional, inserta el definitivo "
        "con pendiente=0, actualiza la tabla metadata_sincronizacion con el timestamp actual y descarga el inventario unificado. Finalmente, la "
        "tarjeta se transmuta en la UI a #006 y el banner pendiente se apaga automáticamente.»</i>",
        script_body
    )]

    # Minuto 6:30 - 8:00
    story += [p("Bloque 5 (6:30 - 8:00): Multiplataforma KMP, Demostración en Android y Arquitectura iOS", script_speaker)]
    story += [p(
        "<i>«Para concluir, abordamos la dimensión multiplataforma y las evidencias empíricas. Demostramos la ejecución reproducible de 62 pruebas "
        "unitarias en verde mediante ./gradlew :shared:testAndroidHostTest. En commonTest implementamos seis pruebas canónicas con Ktor MockEngine "
        "y TestDriverFactory multiplataforma, además de la prueba de migración 1.sqm que garantiza la compatibilidad hacia atrás del esquema de base de datos.</i><br/>"
        "<i>En el emulador Android documentamos con total rigor forense los cinco escenarios exigidos: lectura en modo avión, alta con ID negativo, "
        "reconciliación verificada también mediante Swagger UI con Bearer token en el backend, la compuerta defensiva tras pm clear, y la gestión de "
        "sesión offline de solo lectura junto a la expulsión reactiva ante respuestas 401 por token expirado.</i><br/>"
        "<i>Respecto a iOS, dejamos constancia técnica de que toda la lógica de negocio, repositorios y persistencia es 100% compartida en KMP; "
        "la factoría TestDriverFactory.ios.kt implementa inMemoryDriver(PharmaDb.Schema) lista para compilar en Apple Clang/LLVM. Conforme a la "
        "dispensa académica formalmente otorgada por el docente ante la limitación de ejecutar simuladores macOS en entorno Windows, se acredita "
        "la validez arquitectural del código fuente. Con esto demostramos una solución robusta, clínicamente resiliente y técnicamente de excelencia. "
        "Muchas gracias.»</i>",
        script_body
    )]
    story += [Spacer(1, 12)]

    # ==================================================================
    # SECCIÓN 6: CONCLUSIONES Y REFERENCIAS BIBLIOGRÁFICAS
    # ==================================================================
    story += [PageBreak(), p("6. Conclusiones y Referencias Bibliográficas", heading)]
    concl_rows = [
        ("Dimensión", "Arquitectura Previa (REST Directo)", "Arquitectura Final (Offline-First en KMP)"),
        ("Disponibilidad de Datos", "Dependiente al 100% de la latencia y disponibilidad del servidor HTTP.", "Inmediata y reactiva. SQLite actúa como fuente de verdad permanente mediante Flow."),
        ("Operaciones en Contingencia", "Bloqueadas. No se permitía registrar ni consultar medicamentos sin red.", "Totalmente habilitadas. Admite altas no bloqueantes con IDs temporales negativos."),
        ("Consistencia de Datos", "Riesgo de inconsistencias por reintentos ciegos ante pérdida de paquete.", "Atómica y serializada. Mutex compartido y transacciones locales de reconciliación."),
        ("Seguridad y Sesiones", "Falta de definición ante apertura sin red o tokens caducados.", "Modo OfflineSoloLectura auditado; expulsión reactiva ante respuestas HTTP 401."),
        ("Multiplataforma", "Lógica duplicada por plataforma móvil.", "Código de dominio, persistencia y red compartido en Kotlin Multiplatform."),
    ]
    story += [create_table(["Dimensión Técnica", "Línea Base Inicial", "Solución Implementada en Sesión 11"], concl_rows, [110, 185, 197])]
    story += [Spacer(1, 10)]

    story += [p(
        "<b>Conclusiones Académicas:</b> La implementación integral de la arquitectura Offline-First en PharmaMobil "
        "evidencia cómo la ingeniería de software aplicada a la salud transforma contingencias operativas en experiencias de "
        "usuario continuas y seguras. La orquestación entre SQLDelight 2.1.0 y Ktor Client 3.1.1 en shared/commonMain "
        "demuestra la madurez del ecosistema Kotlin Multiplatform para desplegar soluciones clínicas de alto rendimiento. "
        "Los mecanismos de sincronización diferida, asignación de claves provisionales, reconciliación transaccional y "
        "auditoría de identidad cacheada garantizan integridad de datos, cumplimiento de estándares de seguridad y una base "
        "sólida para el escalamiento asistencial."
    )]
    story += [Spacer(1, 6)]

    story += [p("Referencias Bibliográficas y Documentación Técnica", subheading)]
    refs = [
        "1. Universidad Peruana Unión (2026). <i>Guía de Práctica y Rúbrica de Evaluación de la Sesión 11: Persistencia Local y Arquitectura Offline-First</i>. Facultad de Ingeniería y Arquitectura, Escuela Profesional de Ingeniería de Sistemas.",
        "2. Cash App (2024). <i>SQLDelight 2.1.0 Documentation: Multiplatform SQLite Drivers and Reactive Queries</i>. Recuperado de: https://sqldelight.github.io/sqldelight/2.1.0/multiplatform_sqlite/",
        "3. JetBrains (2024). <i>Kotlin Multiplatform: expect/actual declarations and Multiplatform Testing Guidelines</i>. Recuperado de: https://kotlinlang.org/docs/multiplatform-expect-actual.html",
        "4. Google Developers (2024). <i>Android Architecture Guides: Offline-First Applications and Data Layer Best Practices</i>. Android Open Source Project.",
        "5. OWASP Foundation (2024). <i>Mobile Security Testing Guide (MSTG): Secure Storage and Authentication Resilience</i>. Recuperado de: https://mas.owasp.org/",
    ]
    for r in refs:
        story += [p(r, style=cell), Spacer(1, 3)]

    # ------------------------------------------------------------------
    # Compilación del Documento PDF
    # ------------------------------------------------------------------
    doc = SimpleDocTemplate(
        str(PDF_PATH),
        pagesize=A4,
        leftMargin=51,
        rightMargin=51,
        topMargin=50,
        bottomMargin=57,
        title="Informe Sesión 11 - Andrey Fidel Mestanza Bazán",
        author="Andrey Fidel Mestanza Bazán",
        subject="Desarrollo de Aplicaciones Móviles - Actividad Autónoma Sesión 11"
    )

    doc.build(story, onFirstPage=draw_footer_and_header, onLaterPages=draw_footer_and_header)
    print(f"INFORME GENERADO EXITOSAMENTE: {PDF_PATH}")

if __name__ == "__main__":
    generar_reporte()
