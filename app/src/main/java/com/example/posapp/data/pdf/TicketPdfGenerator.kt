package com.example.posapp.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.posapp.domain.model.ItemCarrito
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.posrepuestos.app.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TicketPdfGenerator(private val context: Context) {

    // Colores corporativos
    private val azulOscuro = DeviceRgb(21, 101, 192)
    private val verdeAiterp = DeviceRgb(76, 175, 80)
    private val grisClaro = DeviceRgb(245, 245, 245)
    private val blanco = DeviceRgb(255, 255, 255)

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
        impuestoPorcentaje: Double,
        usuarioAtendio: String = "Sistema"
    ): File {
        val fileName = "ticket_$numeroVenta.pdf"
        val file = File(context.cacheDir, fileName)

        val writer = PdfWriter(file)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)

        // ✅ Márgenes reducidos para optimizar espacio
        document.setMargins(20f, 25f, 20f, 25f)

        // === ENCABEZADO CON LOGO ===
        agregarEncabezado(document)

        // === RECUADRO DE TICKET ===
        agregarRecuadroTicket(document, numeroVenta)

        // === INFORMACIÓN DE VENTA ===
        agregarInfoVenta(document, fecha, metodoPago, usuarioAtendio)

        // === DATOS DEL CLIENTE ===
        agregarDatosCliente(document, clienteNombre, clienteDocumento, clienteTelefono, clienteEmail)

        // === TABLA DE PRODUCTOS ===
        agregarTablaProductos(document, items)

        // === TOTALES ===
        agregarTotales(document, subtotalSinIVA, montoIVA, total, impuestoPorcentaje)

        // === CÓDIGO QR ===
        agregarCodigoQR(document, numeroVenta, total)

        // === PIE DE PÁGINA ===
        agregarPiePagina(document)

        document.close()
        return file
    }

    private fun agregarEncabezado(document: Document) {
        val tablaEncabezado = Table(UnitValue.createPercentArray(floatArrayOf(30f, 70f)))
            .useAllAvailableWidth()
            .setMarginBottom(10f) // ✅ Reducido de 20f

        // Logo
        try {
            // ✅ MÉTODO CORRECTO para drawable
            val logoDrawable = context.resources.getDrawable(R.drawable.logo_nexusti, null)
            val logoBitmap = (logoDrawable as android.graphics.drawable.BitmapDrawable).bitmap

            val stream = ByteArrayOutputStream()
            logoBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val logoBytes = stream.toByteArray()

            val logoImage = Image(ImageDataFactory.create(logoBytes))
                .setWidth(120f)
                .setHorizontalAlignment(HorizontalAlignment.LEFT)

            tablaEncabezado.addCell(
                Cell().add(logoImage)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(0f)
            )
        } catch (e: Exception) {
            // Fallback si no hay logo
            tablaEncabezado.addCell(
                Cell().add(
                    Paragraph("NEXUS TI\nSOLUTIONS")
                        .setFontSize(14f)
                        .setBold()
                        .setFontColor(azulOscuro)
                ).setBorder(Border.NO_BORDER)
            )
        }


        // Datos de la empresa
        val datosEmpresa = Cell().add(
            Paragraph("NEXUS TI SOLUTIONS EIRL")
                .setFontSize(13f) // ✅ Reducido de 16f
                .setBold()
                .setFontColor(azulOscuro)
        ).add(
            Paragraph("RUC: 20601234567")
                .setFontSize(9f) // ✅ Reducido de 10f
                .setMarginTop(2f)
        ).add(
            Paragraph("Av. Lima 449 - Sanluis")
                .setFontSize(9f)
        ).add(
            Paragraph("Tel: 935783488")
                .setFontSize(9f)
        )
        datosEmpresa.setBorder(Border.NO_BORDER)
        datosEmpresa.setTextAlignment(TextAlignment.RIGHT)
        tablaEncabezado.addCell(datosEmpresa)

        document.add(tablaEncabezado)
    }

    private fun agregarRecuadroTicket(document: Document, numeroVenta: String) {
        val recuadro = Paragraph("TICKET DE VENTA\nN° $numeroVenta")
            .setFontSize(14f) // ✅ Reducido de 16f
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setBackgroundColor(azulOscuro)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(10f) // ✅ Reducido de 15f
            .setBorder(SolidBorder(azulOscuro, 2f))
            .setMarginBottom(10f) // ✅ Reducido de 20f

        document.add(recuadro)
    }

    private fun agregarInfoVenta(document: Document, fecha: Date, metodoPago: String, usuario: String) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val tablaInfo = Table(UnitValue.createPercentArray(floatArrayOf(35f, 65f)))
            .useAllAvailableWidth()
            .setMarginBottom(8f) // ✅ Reducido de 15f

        tablaInfo.addCell(createCellInfo("Fecha:", true))
        tablaInfo.addCell(createCellInfo(dateFormat.format(fecha), false))

        tablaInfo.addCell(createCellInfo("Método de Pago:", true))
        tablaInfo.addCell(createCellInfo(metodoPago, false))

        tablaInfo.addCell(createCellInfo("Atendió por:", true))
        tablaInfo.addCell(createCellInfo(usuario, false))

        document.add(tablaInfo)
    }

    private fun agregarDatosCliente(
        document: Document,
        nombre: String,
        documento: String,
        telefono: String,
        email: String
    ) {
        document.add(
            Paragraph("DATOS DEL CLIENTE")
                .setFontSize(10f) // ✅ Reducido de 12f
                .setBold()
                .setFontColor(azulOscuro)
                .setMarginTop(8f) // ✅ Reducido de 15f
                .setMarginBottom(5f) // ✅ Reducido de 8f
        )

        val tablaCliente = Table(UnitValue.createPercentArray(floatArrayOf(25f, 75f)))
            .useAllAvailableWidth()
            .setMarginBottom(8f) // ✅ Reducido de 15f

        tablaCliente.addCell(createCellInfo("Cliente:", true))
        tablaCliente.addCell(createCellInfo(nombre, false))

        tablaCliente.addCell(createCellInfo("DNI/RUC:", true))
        tablaCliente.addCell(createCellInfo(documento, false))

        if (telefono.isNotBlank()) {
            tablaCliente.addCell(createCellInfo("Teléfono:", true))
            tablaCliente.addCell(createCellInfo(telefono, false))
        }

        if (email.isNotBlank()) {
            tablaCliente.addCell(createCellInfo("Email:", true))
            tablaCliente.addCell(createCellInfo(email, false))
        }

        document.add(tablaCliente)
    }

    private fun agregarTablaProductos(document: Document, items: List<ItemCarrito>) {
        document.add(
            Paragraph("DETALLE DE PRODUCTOS")
                .setFontSize(10f) // ✅ Reducido de 12f
                .setBold()
                .setFontColor(azulOscuro)
                .setMarginTop(8f) // ✅ Reducido de 15f
                .setMarginBottom(5f) // ✅ Reducido de 8f
        )

        val tablaProductos = Table(UnitValue.createPercentArray(floatArrayOf(10f, 45f, 20f, 25f)))
            .useAllAvailableWidth()
            .setMarginBottom(8f) // ✅ Reducido de 15f

        // Encabezados
        tablaProductos.addHeaderCell(createHeaderCell("Cant."))
        tablaProductos.addHeaderCell(createHeaderCell("Producto"))
        tablaProductos.addHeaderCell(createHeaderCell("P. Unit."))
        tablaProductos.addHeaderCell(createHeaderCell("Subtotal"))

        // Items con filas alternas
        items.forEachIndexed { index, item ->
            val bgColor: DeviceRgb = if (index % 2 == 0) grisClaro else blanco

            tablaProductos.addCell(
                createCellProducto("${item.cantidad}", TextAlignment.CENTER, bgColor)
            )
            tablaProductos.addCell(
                createCellProducto(item.producto.nombre, TextAlignment.LEFT, bgColor)
            )
            tablaProductos.addCell(
                createCellProducto("$ ${String.format("%.2f", item.producto.precio)}", TextAlignment.RIGHT, bgColor)
            )
            tablaProductos.addCell(
                createCellProducto("$ ${String.format("%.2f", item.subtotal)}", TextAlignment.RIGHT, bgColor)
            )
        }

        document.add(tablaProductos)
    }

    private fun agregarTotales(
        document: Document,
        subtotal: Double,
        iva: Double,
        total: Double,
        porcentajeIva: Double
    ) {
        val tablaTotales = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            .useAllAvailableWidth()
            .setMarginTop(5f) // ✅ Reducido de 10f

        // Subtotal
        tablaTotales.addCell(
            createCellTotal("Subtotal (sin IVA):", false, TextAlignment.RIGHT)
        )
        tablaTotales.addCell(
            createCellTotal("$ ${String.format("%.2f", subtotal)}", false, TextAlignment.RIGHT)
        )

        // IVA
        tablaTotales.addCell(
            createCellTotal("IVA (${porcentajeIva.toInt()}%):", false, TextAlignment.RIGHT)
        )
        tablaTotales.addCell(
            createCellTotal("$ ${String.format("%.2f", iva)}", false, TextAlignment.RIGHT)
        )

        // Total destacado
        tablaTotales.addCell(
            createCellTotal("TOTAL:", true, TextAlignment.RIGHT)
                .setFontSize(14f) // ✅ Reducido de 16f
                .setBackgroundColor(grisClaro)
        )
        tablaTotales.addCell(
            createCellTotal("$ ${String.format("%.2f", total)}", true, TextAlignment.RIGHT)
                .setFontSize(18f) // ✅ Reducido de 20f
                .setFontColor(verdeAiterp)
                .setBackgroundColor(grisClaro)
        )

        document.add(tablaTotales)
    }

    private fun agregarCodigoQR(document: Document, numeroVenta: String, total: Double) {
        try {
            val qrContent = "VENTA:$numeroVenta|TOTAL:$total|FECHA:${Date().time}"
            val qrBitmap = generarQRCode(qrContent, 120, 120) // ✅ Reducido de 150

            val stream = ByteArrayOutputStream()
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val qrImage = Image(ImageDataFactory.create(stream.toByteArray()))
                .setWidth(100f) // ✅ Reducido de 120f
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(10f) // ✅ Reducido de 20f

            document.add(qrImage)

            document.add(
                Paragraph("Escanea para validar")
                    .setFontSize(8f) // ✅ Reducido de 9f
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic()
                    .setMarginTop(3f) // ✅ Reducido de 5f
            )
        } catch (e: Exception) {
            // Si falla el QR, continuar sin él
        }
    }

    private fun agregarPiePagina(document: Document) {
        document.add(
            Paragraph("¡Gracias por su compra!")
                .setFontSize(12f) // ✅ Reducido de 14f
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(azulOscuro)
                .setMarginTop(15f) // ✅ Reducido de 30f
        )

        document.add(
            Paragraph("Conserve su ticket como garantía")
                .setFontSize(9f) // ✅ Reducido de 10f
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginTop(3f) // ✅ Reducido de 5f
        )
    }

    // === FUNCIONES AUXILIARES ===

    private fun createCellInfo(text: String, isBold: Boolean): Cell {
        val cell = Cell().add(
            Paragraph(text).apply {
                if (isBold) setBold()
                setFontSize(9f) // ✅ Reducido de 10f
            }
        )
        cell.setPadding(3f) // ✅ Reducido de 5f
        cell.setBorder(Border.NO_BORDER)
        return cell
    }

    private fun createHeaderCell(text: String): Cell {
        val cell = Cell().add(
            Paragraph(text)
                .setBold()
                .setFontSize(9f) // ✅ Reducido de 10f
                .setTextAlignment(TextAlignment.CENTER)
        )
        cell.setBackgroundColor(azulOscuro)
        cell.setFontColor(ColorConstants.WHITE)
        cell.setPadding(6f) // ✅ Reducido de 8f
        return cell
    }

    private fun createCellProducto(
        text: String,
        alignment: TextAlignment,
        bgColor: DeviceRgb
    ): Cell {
        val cell = Cell().add(
            Paragraph(text)
                .setFontSize(8f) // ✅ Reducido de 9f
                .setTextAlignment(alignment)
        )
        cell.setBackgroundColor(bgColor)
        cell.setPadding(4f) // ✅ Reducido de 6f
        cell.setBorder(SolidBorder(DeviceRgb(220, 220, 220), 0.5f))
        return cell
    }

    private fun createCellTotal(
        text: String,
        isBold: Boolean,
        alignment: TextAlignment
    ): Cell {
        val cell = Cell().add(
            Paragraph(text).apply {
                if (isBold) setBold()
                setFontSize(10f) // ✅ Tamaño consistente
                setTextAlignment(alignment)
            }
        )
        cell.setPadding(5f) // ✅ Reducido de 8f
        cell.setBorder(Border.NO_BORDER)
        return cell
    }

    private fun generarQRCode(content: String, width: Int, height: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }
}
