package com.example.posapp.data.pdf

import android.content.Context
import com.example.posapp.domain.model.ItemCarrito
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TicketPdfGenerator(private val context: Context) {

    fun generarTicket(
        numeroVenta: String,
        fecha: Date,
        metodoPago: String,
        clienteNombre: String,
        clienteDocumento: String,
        clienteTelefono: String,
        clienteEmail: String,
        items: List<ItemCarrito>,
        subtotalSinIVA: Double,
        montoIVA: Double,
        total: Double,
        impuestoPorcentaje: Double
    ): File {
        // Crear archivo temporal
        val fileName = "ticket_$numeroVenta.pdf"
        val file = File(context.cacheDir, fileName)

        // Crear PDF
        val writer = PdfWriter(file)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        document.setMargins(40f, 40f, 40f, 40f)

        // === ENCABEZADO DE LA EMPRESA ===
        val colorPrimario = DeviceRgb(41, 128, 185) // Azul corporativo

        document.add(
            Paragraph("TIGER SOLUTIONS EIRL")
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(colorPrimario)
        )

        document.add(
            Paragraph("RUC: 20449775533")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        document.add(
            Paragraph("Av. Lima 449 - Sanluis")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        document.add(
            Paragraph("Tel: 935783488")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
        )

        // === TÍTULO DEL TICKET ===
        document.add(
            Paragraph("TICKET DE VENTA")
                .setFontSize(16f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(DeviceRgb(240, 240, 240))
                .setPadding(10f)
                .setMarginBottom(15f)
        )

        // === DATOS DE LA VENTA ===
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val tablaDatos = Table(UnitValue.createPercentArray(floatArrayOf(40f, 60f)))
            .useAllAvailableWidth()
            .setMarginBottom(15f)

        tablaDatos.addCell(createCell("Número de Venta:", true))
        tablaDatos.addCell(createCell(numeroVenta, true))

        tablaDatos.addCell(createCell("Fecha:", true))
        tablaDatos.addCell(createCell(dateFormat.format(fecha), true))

        tablaDatos.addCell(createCell("Método de Pago:", true))
        tablaDatos.addCell(createCell(metodoPago, true))

        document.add(tablaDatos)

        // === DATOS DEL CLIENTE ===
        document.add(
            Paragraph("DATOS DEL CLIENTE")
                .setFontSize(12f)
                .setBold()
                .setMarginTop(10f)
                .setMarginBottom(5f)
        )

        val tablaCliente = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .useAllAvailableWidth()
            .setMarginBottom(15f)

        tablaCliente.addCell(createCell("Cliente:", true))
        tablaCliente.addCell(createCell(clienteNombre, false))

        tablaCliente.addCell(createCell("DNI/RUC:", true))
        tablaCliente.addCell(createCell(clienteDocumento, false))

        if (clienteTelefono.isNotBlank()) {
            tablaCliente.addCell(createCell("Teléfono:", true))
            tablaCliente.addCell(createCell(clienteTelefono, false))
        }

        if (clienteEmail.isNotBlank()) {
            tablaCliente.addCell(createCell("Email:", true))
            tablaCliente.addCell(createCell(clienteEmail, false))
        }

        document.add(tablaCliente)

        // === PRODUCTOS ===
        document.add(
            Paragraph("PRODUCTOS")
                .setFontSize(12f)
                .setBold()
                .setMarginTop(10f)
                .setMarginBottom(5f)
        )

        val tablaProductos = Table(UnitValue.createPercentArray(floatArrayOf(10f, 45f, 15f, 15f, 15f)))
            .useAllAvailableWidth()
            .setMarginBottom(15f)

        // Encabezados
        tablaProductos.addHeaderCell(createHeaderCell("Cant."))
        tablaProductos.addHeaderCell(createHeaderCell("Producto"))
        tablaProductos.addHeaderCell(createHeaderCell("P. Unit."))
        tablaProductos.addHeaderCell(createHeaderCell("Subtotal"))

        // Items
        items.forEach { item ->
            tablaProductos.addCell(createCell("${item.cantidad}", false, TextAlignment.CENTER))
            tablaProductos.addCell(createCell(item.producto.nombre, false))
            tablaProductos.addCell(createCell("$${String.format("%.2f", item.producto.precio)}", false, TextAlignment.RIGHT))
            tablaProductos.addCell(createCell("$${String.format("%.2f", item.subtotal)}", false, TextAlignment.RIGHT))
        }

        document.add(tablaProductos)

        // === TOTALES ===
        val tablaTotales = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            .useAllAvailableWidth()
            .setMarginTop(10f)

        tablaTotales.addCell(
            createCell("Subtotal (sin IVA):", true, TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
        )
        tablaTotales.addCell(
            createCell("$${String.format("%.2f", subtotalSinIVA)}", true, TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
        )

        tablaTotales.addCell(
            createCell("IVA (${impuestoPorcentaje.toInt()}%):", true, TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
        )
        tablaTotales.addCell(
            createCell("$${String.format("%.2f", montoIVA)}", true, TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
        )

        tablaTotales.addCell(
            createCell("TOTAL:", true, TextAlignment.RIGHT)
                .setBorder(SolidBorder(1f))
                .setFontSize(14f)
                .setBold()
        )
        tablaTotales.addCell(
            createCell("$${String.format("%.2f", total)}", true, TextAlignment.RIGHT)
                .setBorder(SolidBorder(1f))
                .setFontSize(16f)
                .setBold()
                .setFontColor(colorPrimario)
        )

        document.add(tablaTotales)

        // === PIE DE PÁGINA ===
        document.add(
            Paragraph("¡Gracias por su compra!")
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30f)
                .setItalic()
        )

        document.close()

        return file
    }

    private fun createCell(
        text: String,
        isBold: Boolean,
        alignment: TextAlignment = TextAlignment.LEFT
    ): Cell {
        val cell = Cell().add(
            Paragraph(text).apply {
                if (isBold) setBold()
                setTextAlignment(alignment)
            }
        )
        cell.setPadding(5f)
        cell.setBorder(Border.NO_BORDER)
        return cell
    }

    private fun createHeaderCell(text: String): Cell {
        val cell = Cell().add(
            Paragraph(text)
                .setBold()
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        cell.setBackgroundColor(DeviceRgb(52, 152, 219))
        cell.setFontColor(ColorConstants.WHITE)
        cell.setPadding(8f)
        return cell
    }
}
