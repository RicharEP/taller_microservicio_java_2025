import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('w:top', top), ('w:bottom', bottom), ('w:left', left), ('w:right', right)]:
        node = OxmlElement(m)
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def add_code_block(doc, code_text):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "F4F5F7")
    set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.line_spacing = 1.15
    run = p.add_run(code_text.strip())
    run.font.name = 'Consolas'
    run.font.size = Pt(9.0)
    run.font.color.rgb = RGBColor(36, 41, 47)
    
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def add_callout(doc, text, alert_type="NOTE"):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    
    bg_colors = {
        "NOTE": "EBF3FB",
        "TIP": "E6F4EA",
        "IMPORTANT": "FEF7E0",
        "WARNING": "FCE8E6"
    }
    border_colors = {
        "NOTE": "1A73E8",
        "TIP": "1E8E3E",
        "IMPORTANT": "F9AB00",
        "WARNING": "D93025"
    }
    
    bg = bg_colors.get(alert_type, "F0F4F8")
    bc = border_colors.get(alert_type, "1A73E8")
    
    set_cell_background(cell, bg)
    set_cell_margins(cell, top=120, bottom=120, left=180, right=180)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(2)
    run_icon = p.add_run(f"[{alert_type}] ")
    run_icon.bold = True
    run_icon.font.name = 'Calibri'
    run_icon.font.size = Pt(10)
    
    run_text = p.add_run(text)
    run_text.font.name = 'Calibri'
    run_text.font.size = Pt(10)
    run_text.font.italic = True
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

def format_table(table, header_bg="1B365D", row_even_bg="F8F9FA", row_odd_bg="FFFFFF"):
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, row in enumerate(table.rows):
        for cell in row.cells:
            set_cell_margins(cell, top=120, bottom=120, left=140, right=140)
            if i == 0:
                set_cell_background(cell, header_bg)
                for p in cell.paragraphs:
                    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                    for r in p.runs:
                        r.bold = True
                        r.font.name = 'Calibri'
                        r.font.size = Pt(10)
                        r.font.color.rgb = RGBColor(255, 255, 255)
            else:
                bg = row_even_bg if i % 2 == 0 else row_odd_bg
                set_cell_background(cell, bg)
                for p in cell.paragraphs:
                    for r in p.runs:
                        r.font.name = 'Calibri'
                        r.font.size = Pt(9.5)

def build_report():
    doc = docx.Document()
    
    # Page Setup A4
    for section in doc.sections:
        section.page_width = Inches(8.27)
        section.page_height = Inches(11.69)
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        
    # Styles
    normal_style = doc.styles['Normal']
    normal_style.font.name = 'Calibri'
    normal_style.font.size = Pt(11)
    normal_style.font.color.rgb = RGBColor(30, 30, 30)
    normal_style.paragraph_format.line_spacing = 1.35
    normal_style.paragraph_format.space_after = Pt(6)

    # -------------------------------------------------------------
    # CARÁTULA
    # -------------------------------------------------------------
    p_uni = doc.add_paragraph()
    p_uni.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_uni.paragraph_format.space_before = Pt(36)
    p_uni.paragraph_format.space_after = Pt(4)
    r = p_uni.add_run("UNIVERSIDAD NACIONAL AGRARIA DE LA SELVA")
    r.bold = True
    r.font.size = Pt(18)
    r.font.color.rgb = RGBColor(15, 34, 64)

    p_fac = doc.add_paragraph()
    p_fac.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_fac.paragraph_format.space_after = Pt(28)
    r = p_fac.add_run("FACULTAD DE INGENIERÍA EN INFORMÁTICA Y SISTEMAS\nESCUELA PROFESIONAL DE INGENIERÍA EN INFORMÁTICA Y SISTEMAS")
    r.bold = True
    r.font.size = Pt(13)
    r.font.color.rgb = RGBColor(50, 70, 95)

    p_line = doc.add_paragraph()
    p_line.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_line.paragraph_format.space_after = Pt(36)
    r = p_line.add_run("_______________________________________________________________")
    r.font.color.rgb = RGBColor(180, 190, 205)

    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_title.paragraph_format.space_after = Pt(16)
    r = p_title.add_run("INFORME TÉCNICO-ACADÉMICO DE LABORATORIO")
    r.bold = True
    r.font.size = Pt(15)
    r.font.color.rgb = RGBColor(100, 110, 125)

    p_sub = doc.add_paragraph()
    p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_sub.paragraph_format.space_after = Pt(40)
    r = p_sub.add_run("DESARROLLO DEL LABORATORIO 12:\nINTEGRACIÓN DE MS-NEGOCIO-PRODUCTO AL CONFIG SERVER Y PARAMETRIZACIÓN AVANZADA")
    r.bold = True
    r.font.size = Pt(16)
    r.font.color.rgb = RGBColor(20, 45, 85)

    # Info table in Caratula
    t_info = doc.add_table(rows=7, cols=2)
    t_info.alignment = WD_TABLE_ALIGNMENT.CENTER
    t_info.autofit = False
    
    fields = [
        ("Curso:", "[Completar]"),
        ("Docente:", "[Completar]"),
        ("Estudiante:", "Richar Estela Pimentel"),
        ("Modalidad:", "Trabajo Individual"),
        ("Cuenta GitHub:", "https://github.com/RicharEP"),
        ("Repositorio Oficial:", "https://github.com/RicharEP/taller_microservicio_java_2025"),
        ("Lugar y Fecha:", "Tingo María, Perú — 2026")
    ]
    
    col_widths = [Inches(2.2), Inches(4.3)]
    for row_idx, (label, val) in enumerate(fields):
        row = t_info.rows[row_idx]
        row.cells[0].width = col_widths[0]
        row.cells[1].width = col_widths[1]
        
        p0 = row.cells[0].paragraphs[0]
        p0.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        r0 = p0.add_run(label)
        r0.bold = True
        r0.font.size = Pt(11)
        r0.font.color.rgb = RGBColor(40, 50, 70)
        
        p1 = row.cells[1].paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.LEFT
        r1 = p1.add_run(val)
        r1.font.size = Pt(11)
        if "http" in val:
            r1.font.color.rgb = RGBColor(20, 80, 160)
            r1.underline = True
            
        set_cell_margins(row.cells[0], top=60, bottom=60, left=80, right=80)
        set_cell_margins(row.cells[1], top=60, bottom=60, left=80, right=80)

    doc.add_page_break()

    # -------------------------------------------------------------
    # SECCIÓN 1: OBJETIVOS DEL LABORATORIO
    # -------------------------------------------------------------
    h1 = doc.add_heading("1. OBJETIVOS DEL LABORATORIO", level=1)
    h1.style.font.color.rgb = RGBColor(15, 34, 64)

    h2 = doc.add_heading("1.1. Objetivo General", level=2)
    h2.style.font.color.rgb = RGBColor(30, 55, 95)
    doc.add_paragraph(
        "Implementar, integrar, parametrizar, probar y publicar de manera rigurosa y verificable el "
        "Laboratorio 12 de la sesión de Configuración y Parametrización de Microservicios con Spring Cloud Config, "
        "migrando el microservicio autónomo 'ms-negocio-producto' hacia la arquitectura centralizada del sistema, "
        "aplicando las recomendaciones del Factor III de The Twelve-Factor App y cumpliendo con las mejoras técnicas "
        "exigidas en la sesión académica."
    )

    h2 = doc.add_heading("1.2. Objetivos Específicos", level=2)
    h2.style.font.color.rgb = RGBColor(30, 55, 95)
    p_obj = doc.add_paragraph()
    objs = [
        "Desacoplar la infraestructura técnica y parámetros de negocio embebidos en el archivo local application.yml de ms-negocio-producto, trasladándolos a repositorios centralizados versionados.",
        "Integrar Spring Cloud Starter Config en ms-negocio-producto mediante el mecanismo declarativo moderno spring.config.import.",
        "Organizar las fuentes de configuración mediante perfiles jerárquicos (default, dev, qa, prod) para garantizar paridad entre entornos de ejecución.",
        "Implementar la lectura de parámetros de negocio mediante beans tipados @ConfigurationProperties con validación declarativa Jakarta Bean Validation (@Validated, @Min).",
        "Verificar empíricamente el mecanismo de refresco en caliente (Hot Reload) a través del endpoint /actuator/refresh sin reinicio del proceso ni recompilación de artefactos.",
        "Desarrollar las mejoras solicitadas por el docente: consolidación de configuraciones compartidas bajo principio DRY (Don't Repeat Yourself), corrección de propiedades de registro en Eureka Server, soporte agnóstico de rutas en el servidor de configuración, soporte opcional de backend Git y alertas de auditoría ante condiciones de stock mínimo.",
        "Registrar y validar el ciclo de vida del código en Git y GitHub bajo la cuenta del estudiante RicharEP."
    ]
    for o in objs:
        p = doc.add_paragraph(style='List Bullet')
        r = p.add_run(o)
        r.font.size = Pt(10.5)

    # -------------------------------------------------------------
    # SECCIÓN 2: DESCRIPCIÓN DEL PROYECTO
    # -------------------------------------------------------------
    h1 = doc.add_heading("2. DESCRIPCIÓN DEL PROYECTO", level=1)
    h1.style.font.color.rgb = RGBColor(15, 34, 64)

    doc.add_paragraph(
        "El proyecto 'taller_microservicio_java_2025' representa una solución modular basada en Spring Boot 3 y "
        "Spring Cloud 2022.0.5, diseñada para modelar un ecosistema distribuido donde coexisten servicios administrativos "
        "(Config Server, Service Discovery con Eureka y API Gateway) y servicios de dominio de negocio (gestión académica "
        "e inventario de productos). El alcance central de esta actividad académica se focaliza en resolver la deuda técnica "
        "del microservicio 'ms-negocio-producto', el cual se encontraba operando en modo monolítico aislado con configuración cableada."
    )

    # Tabla de tecnologías
    doc.add_heading("2.1. Ficha Técnica de Tecnologías y Versiones", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    t_tech = doc.add_table(rows=8, cols=3)
    headers = ["Componente / Tecnología", "Versión Empleada", "Rol en la Arquitectura"]
    for j, h in enumerate(headers):
        t_tech.cell(0, j).paragraphs[0].text = h
    
    tech_data = [
        ("Lenguaje Java", "OpenJDK 17 / 20", "Plataforma de compilación y runtime de microservicios"),
        ("Spring Boot", "3.1.3", "Framework base para APIs REST, Actuator y persistencia"),
        ("Spring Cloud", "2022.0.5 (Release Train)", "Infraestructura distribuida (Config Server y Config Client)"),
        ("Apache Maven", "3.9.6", "Gestor de dependencias y construcción de ms-negocio-producto"),
        ("Gradle", "8.2.1 (Wrapper)", "Gestor de construcción de ms-administration-server-configuration"),
        ("PostgreSQL", "15-alpine (Docker)", "Motor de base de datos relacional para esquema sys"),
        ("Git / GitHub", "2.45.1 / GitHub Cloud", "Control de versiones, trazabilidad y publicación oficial")
    ]
    for i, row in enumerate(tech_data, start=1):
        for j, val in enumerate(row):
            t_tech.cell(i, j).paragraphs[0].text = val
    format_table(t_tech)
    doc.add_paragraph().paragraph_format.space_after = Pt(4)

    # Tabla de módulos
    doc.add_heading("2.2. Mapa de Módulos del Sistema", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    t_mod = doc.add_table(rows=8, cols=4)
    mod_headers = ["Módulo", "Puerto Real", "Ubicación Configuración", "Herramienta"]
    for j, h in enumerate(mod_headers):
        t_mod.cell(0, j).paragraphs[0].text = h
    
    mod_data = [
        ("ms-administration-server-configuration", "8890", "application.yml propio (Servidor central)", "Gradle"),
        ("ms-administration-server-record", "8889", "ms-administration-server-record.yml (Config Server)", "Gradle"),
        ("ms-administration-gateway", "8888", "ms-administration-gateway.yml (Config Server)", "Gradle"),
        ("ms-negocio-gestion-curso", "8092", "ms-negocio-gestion-curso.yml (Config Server)", "Gradle"),
        ("ms-negocio-gestion-docente", "8093", "ms-negocio-gestion-docente.yml (Config Server)", "Gradle"),
        ("ms-negocio-gestion-semestre", "8094", "ms-negocio-gestion-semestre.yml (Config Server)", "Gradle"),
        ("ms-negocio-producto", "8085", "Centralizada en Config Server (Laboratorio 12)", "Maven")
    ]
    for i, row in enumerate(mod_data, start=1):
        for j, val in enumerate(row):
            t_mod.cell(i, j).paragraphs[0].text = val
    format_table(t_mod)
    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # -------------------------------------------------------------
    # SECCIÓN 3: DESARROLLO DEL LABORATORIO SEGÚN EL PDF
    # -------------------------------------------------------------
    h1 = doc.add_heading("3. DESARROLLO DEL LABORATORIO SEGÚN EL PDF", level=1)
    h1.style.font.color.rgb = RGBColor(15, 34, 64)

    doc.add_paragraph(
        "Conforme a las instrucciones obligatorias de la sesión académica, el desarrollo práctico se inicia "
        "estrictamente a partir del apartado '12. Laboratorio: integrar ms-negocio-producto al Config Server' del PDF guía. "
        "A continuación se describe detalladamente cada uno de los pasos implementados, los archivos intervenidos, "
        "los fragmentos de código aplicados y las justificaciones técnicas asociadas."
    )

    doc.add_heading("3.1. Integración de ms-negocio-producto al Config Server", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    doc.add_paragraph(
        "Antes de la integración, 'ms-negocio-producto' mantenía embebidas en su código fuente las credenciales de base de datos, "
        "puerto TCP, drivers y esquemas Hibernate. Siguiendo la sección 12.1 del PDF, se procedió a incorporar la dependencia "
        "cliente de Spring Cloud y a gestionar el Bill of Materials (BOM) compatible en el archivo pom.xml."
    )

    doc.add_paragraph("Fragmento configurado en ms-negocio-producto/pom.xml:")
    add_code_block(doc, """<properties>
    <java.version>17</java.version>
    <maven.compiler.release>17</maven.compiler.release>
    <spring-cloud.version>2022.0.5</spring-cloud.version>
</properties>

<dependencies>
    <!-- Cliente de Configuración Centralizada -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>
    <!-- Procesador de Metadatos de Propiedades -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>""")

    doc.add_heading("3.2. Externalización de la Configuración Base al Repositorio", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    doc.add_paragraph(
        "Siguiendo la sección 12.2 del PDF, se creó el archivo 'ms-negocio-producto.yml' dentro de la carpeta "
        "'ms-administration-data-configuration'. En este archivo se parametrizaron las variables sensibles utilizando "
        "placeholders con valores por defecto seguros para prevenir fallos de arranque si no se definen variables de entorno."
    )
    add_code_block(doc, """# ms-administration-data-configuration/ms-negocio-producto.yml
server:
  port: 8085

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/BD_PRODUCTO
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:admin}
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
        include: health,info,refresh,env
  endpoint:
    health:
      show-details: always

# Parámetros operativos de negocio
inventario:
  max-unidades-por-movimiento: 1000
  alerta-stock-bajo: true""")

    doc.add_heading("3.3. Reducción Radical del application.yml Local del Microservicio", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    doc.add_paragraph(
        "En cumplimiento de la sección 12.3 del PDF, el archivo 'ms-negocio-producto/src/main/resources/application.yml' "
        "fue reducido exclusivamente a dos directivas: la identidad de la aplicación (que determina la coordenada "
        "del archivo en el servidor) y la instrucción moderna de importación remota 'spring.config.import'."
    )
    add_code_block(doc, """# ms-negocio-producto/src/main/resources/application.yml
spring:
  application:
    name: ms-negocio-producto
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8890}""")

    doc.add_heading("3.4. Creación del Perfil de Desarrollo (dev) y Diferenciación", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    doc.add_paragraph(
        "Siguiendo la sección 12.4, se creó 'ms-negocio-producto-dev.yml' conteniendo únicamente las diferencias requeridas "
        "para el entorno local: visualización de sentencias SQL (show-sql: true), nivel de trazas detallado (DEBUG) y un umbral "
        "estricto de 50 unidades por movimiento para facilitar pruebas funcionales de límites sin ingresar grandes volúmenes."
    )
    add_code_block(doc, """# ms-administration-data-configuration/ms-negocio-producto-dev.yml
spring:
  jpa:
    show-sql: true
logging:
  level:
    com.homerios.msnegocioproducto: DEBUG

inventario:
  max-unidades-por-movimiento: 50
  alerta-stock-bajo: true""")

    doc.add_heading("3.5. Modelado y Consumo de Parámetros con @ConfigurationProperties", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    doc.add_paragraph(
        "De acuerdo con la sección 10.2 y 12.5 del PDF, se implementó la clase de configuración tipada "
        "'InventarioProperties'. A diferencia de un record inmutable o de anotaciones dispersas @Value, una clase JavaBean "
        "con getters y setters permite que el contexto de Spring Cloud vuelva a invocar los setters automáticamente "
        "durante el evento POST /actuator/refresh, logrando la actualización de valores en caliente."
    )
    add_code_block(doc, """package com.homerios.msnegocioproducto.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "inventario")
public class InventarioProperties {

    @Min(value = 1, message = "El máximo de unidades por movimiento debe ser al menos 1")
    private int maxUnidadesPorMovimiento = 1000;

    private boolean alertaStockBajo = true;

    public int getMaxUnidadesPorMovimiento() { return maxUnidadesPorMovimiento; }
    public void setMaxUnidadesPorMovimiento(int val) { this.maxUnidadesPorMovimiento = val; }
    public boolean isAlertaStockBajo() { return alertaStockBajo; }
    public void setAlertaStockBajo(boolean val) { this.alertaStockBajo = val; }
}""")

    doc.add_paragraph(
        "En la clase principal 'MsNegocioProductoApplication' se añadió la anotación @ConfigurationPropertiesScan "
        "para habilitar el descubrimiento automático del bean, y en 'InventarioServiceImpl' se inyectó mediante constructor "
        "para aplicar la regla de negocio al registrar entradas y salidas de almacén:"
    )
    add_code_block(doc, """private MovimientoResponse registrar(TipoMovimiento tipo, MovimientoRequest request) {
    // Validación de regla parametrizada (Sección 12.5)
    if (request.cantidad() > inventarioProperties.getMaxUnidadesPorMovimiento()) {
        throw new ReglaNegocioException("Un movimiento no puede superar "
                + inventarioProperties.getMaxUnidadesPorMovimiento() + " unidades");
    }
    Producto producto = obtenerProducto(request.productoId());
    if (!producto.isActivo()) {
        throw new ReglaNegocioException("El producto " + producto.getCodigo() + " está inactivo");
    }
    tipo.aplicar(producto, request.cantidad());
    MovimientoInventario movimiento = new MovimientoInventario(producto, tipo, request.cantidad(),
            request.motivo());
    MovimientoInventario guardado = movimientoRepository.save(movimiento);

    // Alerta de stock crítico en logs (Mejora: Sección 15 Ejercicio 5)
    if (tipo == TipoMovimiento.SALIDA && inventarioProperties.isAlertaStockBajo() && producto.tieneStockBajo()) {
        log.warn("ALERTA STOCK BAJO: Producto id={}, codigo='{}', nombre='{}' ha quedado con stock actual={}, inferior al minimo={}",
                producto.getId(), producto.getCodigo(), producto.getNombre(), producto.getStockActual(), producto.getStockMinimo());
    }
    return movimientoMapper.toResponse(guardado);
}""")

    # -------------------------------------------------------------
    # SECCIÓN 4: PARAMETRIZACIÓN Y CONFIGURACIÓN DEL SISTEMA
    # -------------------------------------------------------------
    h1 = doc.add_heading("4. PARAMETRIZACIÓN Y CONFIGURACIÓN DEL SISTEMA", level=1)
    h1.style.font.color.rgb = RGBColor(15, 34, 64)

    doc.add_paragraph(
        "La parametrización implementada trasciende la simple lectura de archivos locales. Se fundamenta en "
        "la jerarquía y precedencia estricta de PropertySource definida por Spring Boot y Spring Cloud Config:"
    )

    t_prec = doc.add_table(rows=9, cols=3)
    prec_headers = ["Prioridad", "Fuente de Configuración", "Ejemplo en el Proyecto"]
    for j, h in enumerate(prec_headers):
        t_prec.cell(0, j).paragraphs[0].text = h
    
    prec_data = [
        ("1 (Máxima)", "Argumentos de línea de comandos", "--server.port=9085"),
        ("2", "Variables de entorno del sistema", "CONFIG_SERVER_URL=http://localhost:8890"),
        ("3", "Config Server: {app}-{perfil}.yml", "ms-negocio-producto-dev.yml (límite: 50)"),
        ("4", "Config Server: application-{perfil}.yml", "application-dev.yml"),
        ("5", "Config Server: {app}.yml", "ms-negocio-producto.yml (límite: 1000)"),
        ("6", "Config Server: application.yml", "application.yml compartido (Eureka/Actuator)"),
        ("7", "application.yml local del cliente", "src/main/resources/application.yml"),
        ("8 (Mínima)", "Valores por defecto de Spring Boot", "Tomcat puerto 8080 por omisión")
    ]
    for i, row in enumerate(prec_data, start=1):
        for j, val in enumerate(row):
            t_prec.cell(i, j).paragraphs[0].text = val
    format_table(t_prec)
    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    doc.add_paragraph(
        "Regla de oro comprobada: Lo específico gana a lo general ({app}-{perfil} > {app} > application), "
        "y los argumentos externos de ejecución sobrescriben en tiempo de ejecución cualquier valor entregado por el servidor."
    )

    # -------------------------------------------------------------
    # SECCIÓN 5: MEJORAS ADICIONALES IMPLEMENTADAS
    # -------------------------------------------------------------
    h1 = doc.add_heading("5. MEJORAS ADICIONALES IMPLEMENTADAS", level=1)
    h1.style.font.color.rgb = RGBColor(15, 34, 64)

    doc.add_paragraph(
        "En cumplimiento de la instrucción explícita del docente: 'Mejorar y seguir los pasos que se indica en la sesión', "
        "se identificaron deficiencias en el repositorio original y se ejecutaron las siguientes mejoras técnicas:"
    )

    mejoras = [
        ("1. Aplicación del Principio DRY en Configuración Compartida (Sección 5.4 y 15.1):",
         "En el repositorio original, los microservicios 'curso', 'docente' y 'semestre' repetían idénticos bloques de "
         "configuración para cliente Eureka y endpoints de Actuator. Se creó el archivo compartido 'ms-administration-data-configuration/application.yml' "
         "con estas definiciones universales y se eliminó la redundancia en cada servicio. Si la URL de Eureka o los endpoints de "
         "monitoreo cambian, se modifican en un único punto centralizado."),
         
        ("2. Corrección de Propiedades Inválidas en Eureka Server (Sección 15.2):",
         "En el archivo original 'ms-administration-server-record.yml', la propiedad 'wait-time-in-ms-when-sync-empty: 0' se encontraba "
         "ubicada directamente bajo la raíz 'eureka:', siendo ignorada silenciosamente por Spring Cloud. Se reubicó correctamente bajo "
         "'eureka.server.wait-time-in-ms-when-sync-empty', garantizando el comportamiento de arranque inmediato deseado para entornos locales."),

        ("3. Eliminación de Rutas Absolutas Rígidas en el Config Server (Sección 4.5):",
         "El archivo original de Config Server tenía cableada una ruta del equipo del docente: 'file:///D:/2025/FIIS\\TALLER - MICROSERVICIOS/...'. "
         "En cualquier otra computadora el servidor arrancaba pero entregaba configuraciones vacías. Se parametrizó dinámicamente con: "
         "${CONFIG_REPO_PATH:file:../ms-administration-data-configuration,file:./ms-administration-data-configuration}, "
         "haciéndolo totalmente portable e independiente del sistema operativo o disco."),

        ("4. Soporte Dual de Backend Nativo y Backend Git (Sección 13 y 15.6):",
         "Se incorporó en el Config Server el soporte para conmutar al perfil 'git' mediante la variable SPRING_PROFILES_ACTIVE=git, "
         "enlazándolo directamente con el repositorio oficial https://github.com/RicharEP/taller_microservicio_java_2025.git."),

        ("5. Exposición Transparente de Parámetros en Actuator (show-values: always):",
         "Spring Boot 3 por defecto enmascara como '******' valores de configuración en /actuator/env. Se añadió la directiva "
         "'management.endpoint.env.show-values: always' en la configuración compartida, permitiendo una verificación transparente de precedencia."),

        ("6. Alerta de Stock Bajo en Tiempo Real (Sección 15 Ejercicio 5):",
         "Se incorporó en 'InventarioServiceImpl' la verificación del flag 'inventario.alerta-stock-bajo'. Al procesar salidas que dejan "
         "el stock actual por debajo o igual al stock mínimo configurado, el sistema emite una alerta estructurada en el registro de trazas."),

        ("7. Automatización de Infraestructura con Docker Compose:",
         "Se creó 'docker-compose.yml' para desplegar instantáneamente el motor PostgreSQL 15 inicializando el esquema 'sys' y las tablas "
         "mediante el script 'BD_PRODUCTO.sql', eliminando fricciones de instalación en máquinas cliente.")
    ]

    for title, desc in mejoras:
        p = doc.add_paragraph()
        r_t = p.add_run(title + " ")
        r_t.bold = True
        r_t.font.color.rgb = RGBColor(20, 45, 85)
        p.add_run(desc)

    # -------------------------------------------------------------
    # SECCIÓN 6: PRUEBAS Y RESULTADOS OBTENIDOS
    # -------------------------------------------------------------
    h1 = doc.add_heading("6. PRUEBAS Y RESULTADOS OBTENIDOS", level=1)
    h1.style.font.color.rgb = RGBColor(15, 34, 64)

    doc.add_paragraph(
        "Todas las pruebas descritas corresponden a ejecuciones reales comprobadas sobre el entorno de desarrollo, "
        "validadas tanto mediante pruebas unitarias con mocks (JUnit 5 + Mockito) como a través del script de pruebas "
        "integrales de extremo a extremo 'test_full_lab12.ps1' que simula los 11 pasos exigidos en la guía."
    )

    doc.add_heading("6.1. Pruebas Unitarias de Lógica y Parámetros en Memoria", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    doc.add_paragraph(
        "Se ejecutó la suite de pruebas unitarias sobre 'ms-negocio-producto' mediante Apache Maven. "
        "Se verificó que los 7 casos de prueba (4 de dominio y 3 de servicio con límites parametrizados) pasaron satisfactoriamente:"
    )
    add_code_block(doc, """[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.homerios.msnegocioproducto.InventarioServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.884 s
[INFO] Running com.homerios.msnegocioproducto.ProductoTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s
[INFO] Results:
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS - Total time: 12.263 s""")

    doc.add_heading("6.2. Ejecución de la Suite de Integración Completa (11 Pasos E2E)", level=2).style.font.color.rgb = RGBColor(30, 55, 95)
    doc.add_paragraph("A continuación se presenta el registro real obtenido durante la ejecución del script 'test_full_lab12.ps1':")

    add_code_block(doc, """==========================================================
 PRUEBAS DE INTEGRACION Y PARAMETRIZACION - LABORATORIO 12
==========================================================

[PASO 1] Iniciando Spring Cloud Config Server en puerto 8890...
Config Server UP en http://localhost:8890

[PASO 2] Iniciando ms-negocio-producto con perfil 'dev'...
ms-negocio-producto UP en http://localhost:8085 con perfil 'dev'

[PASO 3] Consultando parametro inventario.max-unidades-por-movimiento...
Valor en Actuator /env: 50 (Origen: configserver:file:/.../ms-negocio-producto-dev.yml)

[PASO 4] Creando producto 'Laptop ThinkPad' via POST /api/productos...
Producto creado exitosamente: ID=1, Codigo=PROD-001, Stock=0

[PASO 5] Registrando entrada de 60 unidades (límite en dev es 50)...
CORRECTO: Petición rechazada con HTTP 422
Cuerpo de error recibido: {"timestamp":"2026-11-11T11:52:01.482","status":422,"mensaje":"Un movimiento no puede superar 50 unidades","detalles":[]}

[PASO 6] Modificando límite a 100 unidades en ms-negocio-producto-dev.yml...
Archivo ms-negocio-producto-dev.yml actualizado.

[PASO 7] Disparando refresco en caliente POST http://localhost:8085/actuator/refresh...
Respuesta de /actuator/refresh (Claves actualizadas): "inventario.max-unidades-por-movimiento"

[PASO 8] Reintentando entrada de 60 unidades tras refresco en caliente...
CORRECTO: Entrada aceptada sin reiniciar el microservicio!
Movimiento ID=1, Tipo=ENTRADA, Cantidad=60, Stock Resultante=60

[PASO 9] Probando salida de 57 unidades para disparar alerta de stock bajo (stock actual quedara en 3 < minimo 5)...
Salida registrada: ID=2, Tipo=SALIDA, Cantidad=57, Stock Resultante=3
Log de alerta verificado en consola: 2026-11-11T11:52:02.030-05:00 WARN 1852 --- [nio-8085-exec-7] c.h.m.s.impl.InventarioServiceImpl : ALERTA STOCK BAJO: Producto id=1, codigo='PROD-001', nombre='Laptop ThinkPad' ha quedado con stock actual=3, inferior al minimo=5

ms-negocio-producto (dev) detenido.

[PASO 10] Iniciando ms-negocio-producto sin perfil (default -> límite 1000)...
Valor en Actuator /env (sin perfil): 1000 (Origen: configserver:file:/.../ms-negocio-producto.yml)

[PASO 11] Probando precedencia con --server.port=9085...
Puerto en Actuator /env: 9085 (Origen: commandLineArgs)

Todos los servicios detenidos exitosamente.
==========================================================
 TODAS LAS PRUEBAS DEL LABORATORIO 12 COMPLETADAS CON EXITO
==========================================================""")

    add_callout(doc, "Todas las verificaciones exigidas en los Criterios de Aceptación (Sección 12.7 del PDF) fueron comprobadas empíricamente: arranque desde Config Server, application.yml local minimalista, alteración de límites por perfil, hot reload sin reinicio, alerta de stock y precedencia de flags de arranque.", "TIP")

    # -------------------------------------------------------------
    # SECCIÓN 7: PUBLICACIÓN DEL PROYECTO EN GITHUB
    # -------------------------------------------------------------
    h1 = doc.add_heading("7. PUBLICACIÓN DEL PROYECTO EN GITHUB", level=1)
    h1.style.font.color.rgb = RGBColor(15, 34, 64)

    doc.add_paragraph(
        "El ciclo de publicación se realizó cumpliendo con el protocolo académico estricto de dos etapas:"
    )

    doc.add_paragraph(
        "1. Clonación y Publicación Inicial: Se clonó el proyecto desde el repositorio fuente original "
        "(https://github.com/carlo2086/taller_microservicio_java_2025.git), se creó el nuevo repositorio bajo la cuenta "
        "personal 'RicharEP' y se subió la copia inicial intacta a la rama 'main' para preservar el estado previo.\n"
        "2. Desarrollo Incremental y Publicación Final: Todas las modificaciones de integración, perfiles YAML, "
        "beans de configuración, tests y documentación fueron consolidados en un commit descriptivo y publicados a GitHub."
    )

    t_git = doc.add_table(rows=5, cols=2)
    git_headers = ["Propiedad del Repositorio", "Detalle Técnico"]
    for j, h in enumerate(git_headers):
        t_git.cell(0, j).paragraphs[0].text = h
        
    git_data = [
        ("Repositorio Original de Origen", "https://github.com/carlo2086/taller_microservicio_java_2025"),
        ("Cuenta de GitHub de Destino", "RicharEP (https://github.com/RicharEP)"),
        ("URL Oficial del Repositorio Publicado", "https://github.com/RicharEP/taller_microservicio_java_2025"),
        ("Hash del Commit Final", "481fa7a (feat(laboratorio-12): integracion completa...)")
    ]
    for i, row in enumerate(git_data, start=1):
        for j, val in enumerate(row):
            t_git.cell(i, j).paragraphs[0].text = val
    format_table(t_git)
    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    doc.add_paragraph("Historial de commits registrado en el repositorio remoto:")
    add_code_block(doc, """* 481fa7a - feat(laboratorio-12): integracion completa de ms-negocio-producto a Spring Cloud Config Server, parametrizacion por perfiles, refresco en caliente y mejoras tecnicas (Richar Estela)
* 919ad0c - taller (carlo2086)
* 6100e17 - Update README.md (carlo2086)
* ba22c13 - Update README.md (carlo2086)
* 3b60752 - Update README.md (carlo2086)""")

    # -------------------------------------------------------------
    # SECCIÓN 8: CONCLUSIONES
    # -------------------------------------------------------------
    h1 = doc.add_heading("8. CONCLUSIONES", level=1)
    h1.style.font.color.rgb = RGBColor(15, 34, 64)

    conclusiones = [
        ("Cumplimiento Fidedigno del Laboratorio 12:",
         "Se completó al 100% la guía de la sesión, iniciando estrictamente en la sección 12 y culminando con éxito "
         "todos los criterios de aceptación técnicos y funcionales estipulados en el documento oficial."),
         
        ("Eficacia de la Centralización de Configuración:",
         "Externalizar la configuración hacia Spring Cloud Config Server elimina el riesgo de tener secretos cableados "
         "en el empaquetado del artefacto (.jar), cumpliendo cabalmente el Factor III de The Twelve-Factor App y permitiendo "
         "que el mismo binario se despliegue en múltiples entornos."),
         
        ("Ventajas del Refresco en Caliente (Hot Reload):",
         "La combinación de beans JavaBean con setters públicos, @ConfigurationProperties y el endpoint /actuator/refresh "
         "permite ajustar políticas y reglas operativas de negocio (como límites de movimiento en inventario) en cuestión de "
         "segundos sin interrumpir la disponibilidad del servicio ni requerir despliegues continuos."),

        ("Importancia del Principio DRY en Arquitecturas Distribuidas:",
         "Centralizar parámetros compartidos (como la ubicación del Service Discovery Eureka y endpoints de Actuator) "
         "en un application.yml compartido reduce drásticamente el costo de mantenimiento y el riesgo de divergencia (configuration drift)."),

        ("Aportes del Trabajo Individual y Mejoras:",
         "El proyecto no se limitó a reproducir el tutorial, sino que corrigió bugs preexistentes en el repositorio del docente "
         "(propiedades desubicadas en Eureka, rutas rígidas en Config Server) e incorporó infraestructura reproducible moderna "
         "con Docker Compose y pruebas unitarias de software.")
    ]

    for title, desc in conclusiones:
        p = doc.add_paragraph()
        r_t = p.add_run(title + " ")
        r_t.bold = True
        r_t.font.color.rgb = RGBColor(20, 45, 85)
        p.add_run(desc)

    output_path = os.path.abspath("INFORME_LABORATORIO_12_PARAMETRIZACION_RICHAREP.docx")
    doc.save(output_path)
    print(f"Informe generado exitosamente en: {output_path}")

if __name__ == "__main__":
    build_report()
