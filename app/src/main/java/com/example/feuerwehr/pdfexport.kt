package com.example.feuerwehr

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PdfExport {

    private const val PAGE_WIDTH  = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN      = 36f
    private const val ROW_HEIGHT  = 22f
    private const val HEADER_H    = 95f

    private val ColorWeinrot = Color.rgb(128, 0, 0)
    private val ColorGold    = Color.rgb(255, 215, 0)
    private val ColorLightBg = Color.rgb(245, 245, 245)

    fun erstellen(context: Context, eintraege: List<TrainingEntry>): File {
        val sharedPrefs = context.getSharedPreferences("FeuerwehrPrefs", Context.MODE_PRIVATE)
        val vorname = sharedPrefs.getString("vorname", "") ?: ""
        val nachname = sharedPrefs.getString("name", "") ?: ""
        val ortswehr = sharedPrefs.getString("ortswehr", "") ?: ""
        // Key angepasst auf dein restliches System
        val start = sharedPrefs.getString("ausbildungsstart", "") ?: ""
        val inhaberFull = "$vorname $nachname".trim().ifEmpty { "Nicht angegeben" }

        val doc      = PdfDocument()
        val sortiert = eintraege.sortedWith(compareBy({ it.datum }, { it.modulId }))

        val colDatum   = 65f
        val colModul   = 40f
        val colName    = 150f
        val colVon     = 38f
        val colBis     = 38f
        val colStd     = 45f
        val colUE      = 40f
        val colAusbild = 103f

        val cols    = listOf(colDatum, colModul, colName, colVon, colBis, colStd, colUE, colAusbild)
        val headers = listOf("Datum", "Mod", "Bezeichnung", "Von", "Bis", "Std", "UE", "Ausbilder")

        val nutzH       = PAGE_HEIGHT - MARGIN * 2 - HEADER_H - ROW_HEIGHT - 30f
        val rowsPerPage = (nutzH / ROW_HEIGHT).toInt()
        val pages       = sortiert.chunked(maxOf(1, rowsPerPage))
        val gesamtSeiten = maxOf(1, pages.size)

        fun neuePage(pageNr: Int): Pair<PdfDocument.Page, Canvas> {
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNr).create()
            val page = doc.startPage(info)
            return page to page.canvas
        }

        if (pages.isEmpty()) {
            val (page, canvas) = neuePage(1)
            zeichneTitel(canvas, 1, 1, inhaberFull, ortswehr, start)
            zeichneKopfzeile(canvas, headers, cols)
            val p = Paint().apply { textSize = 13f; color = Color.GRAY }
            canvas.drawText("Keine Einträge vorhanden.", MARGIN, MARGIN + HEADER_H + ROW_HEIGHT + 20f, p)
            zeichneFusszeile(canvas)
            doc.finishPage(page)
        } else {
            pages.forEachIndexed { idx, pageEintraege ->
                val (page, canvas) = neuePage(idx + 1)
                zeichneTitel(canvas, idx + 1, gesamtSeiten, inhaberFull, ortswehr, start)
                zeichneKopfzeile(canvas, headers, cols)
                pageEintraege.forEachIndexed { rowIdx, entry ->
                    zeichneZeile(canvas, entry, cols, rowIdx)
                }

                if (idx == gesamtSeiten - 1) {
                    zeichneZusammenfassung(canvas, eintraege, pageEintraege.size)
                }

                zeichneFusszeile(canvas)
                doc.finishPage(page)
            }
        }

        val dateiname = "Ausbildungsnachweis_${nachname.ifEmpty { "Export" }}_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.pdf"
        val file      = File(context.cacheDir, dateiname)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun zeichneTitel(canvas: Canvas, seite: Int, gesamt: Int, name: String, wehr: String, start: String) {
        val bg = Paint().apply { color = ColorWeinrot }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), MARGIN + 85f, bg)

        val titelP = Paint().apply {
            textSize = 16f; color = Color.WHITE; isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("AUSBILDUNGSNACHWEIS FEUERWEHR", MARGIN, MARGIN + 18f, titelP)

        val subP = Paint().apply { textSize = 9f; color = ColorGold; isAntiAlias = true }
        canvas.drawText(
            "Export: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}   |   Seite $seite von $gesamt",
            MARGIN, MARGIN + 34f, subP
        )

        val profilP = Paint().apply {
            textSize = 11f; color = Color.WHITE; isAntiAlias = true
        }
        canvas.drawText("Name: $name", MARGIN, MARGIN + 56f, profilP)

        val wehrText = if(wehr.isNotEmpty()) "Ortswehr: $wehr" else "Ortswehr: -"
        val startText = if(start.isNotEmpty()) "  |  Eintritt: $start" else ""
        canvas.drawText(wehrText + startText, MARGIN, MARGIN + 72f, profilP)
    }

    private fun zeichneKopfzeile(canvas: Canvas, headers: List<String>, cols: List<Float>) {
        val y  = MARGIN + HEADER_H
        var x  = MARGIN
        val bg = Paint().apply { color = Color.rgb(40, 40, 40) }
        canvas.drawRect(MARGIN, y - ROW_HEIGHT + 4f, PAGE_WIDTH - MARGIN, y + 4f, bg)

        val p = Paint().apply {
            textSize = 8.5f; color = Color.WHITE; isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, x + 2f, y - 4f, p)
            x += cols[i]
        }
    }

    private fun zeichneZeile(canvas: Canvas, entry: TrainingEntry, cols: List<Float>, rowIdx: Int) {
        val y  = MARGIN + HEADER_H + ROW_HEIGHT * (rowIdx + 1) + 4f
        var x  = MARGIN

        if (rowIdx % 2 == 1) {
            val bg = Paint().apply { color = ColorLightBg }
            canvas.drawRect(MARGIN, y - ROW_HEIGHT + 4f, PAGE_WIDTH - MARGIN, y + 4f, bg)
        }

        val p = Paint().apply { textSize = 8.5f; color = Color.BLACK; isAntiAlias = true }

        // Modulname suchen (alleModule muss global verfügbar sein)
        val modName = alleModule.find { it.id == entry.modulId }?.name ?: entry.modulId

        val dauerMinuten = (entry.endeStunde * 60 + entry.endeMinute) - (entry.startStunde * 60 + entry.startMinute)
        val realStd = maxOf(0.0, dauerMinuten / 60.0)
        val ue      = maxOf(0.0, dauerMinuten / 45.0)

        // Uhrzeit-Formatierung direkt hier lösen
        val vonText = String.format("%02d:%02d", entry.startStunde, entry.startMinute)
        val bisText = String.format("%02d:%02d", entry.endeStunde, entry.endeMinute)

        val werte = listOf(
            entry.datum,
            entry.modulId,
            modName,
            vonText,
            bisText,
            String.format("%.1f h", realStd),
            String.format("%.1f", ue),
            entry.ausbilder
        )

        werte.forEachIndexed { i, wert ->
            val maxCh = (cols[i] / 5.2f).toInt()
            val text  = if (wert.length > maxCh && i == 2) wert.take(maxCh - 1) + "…" else wert
            canvas.drawText(text, x + 2f, y - 4f, p)
            x += cols[i]
        }

        val lp = Paint().apply { color = Color.rgb(220, 220, 220); strokeWidth = 0.5f }
        canvas.drawLine(MARGIN, y + 4f, PAGE_WIDTH - MARGIN, y + 4f, lp)
    }

    private fun zeichneZusammenfassung(canvas: Canvas, alle: List<TrainingEntry>, rowsOnLastPage: Int) {
        val gesamtMinuten = alle.sumOf { (it.endeStunde * 60 + it.endeMinute) - (it.startStunde * 60 + it.startMinute) }
        val gesamtStd = gesamtMinuten / 60.0
        val gesamtUE  = gesamtMinuten / 45.0

        val y = MARGIN + HEADER_H + ROW_HEIGHT * (rowsOnLastPage + 2) + 10f

        val bgP = Paint().apply { color = Color.rgb(240, 230, 230) }
        canvas.drawRect(MARGIN, y - 15f, PAGE_WIDTH - MARGIN, y + 10f, bgP)

        val p = Paint().apply {
            textSize = 10f; color = ColorWeinrot; isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "GESAMT: ${alle.size} Einträge   |   ${String.format("%.2f", gesamtStd)} Zeitstunden   |   ${String.format("%.1f", gesamtUE)} UE",
            MARGIN + 5f, y, p
        )
    }

    private fun zeichneFusszeile(canvas: Canvas) {
        val y = PAGE_HEIGHT - MARGIN + 10f
        val p = Paint().apply { textSize = 7.5f; color = Color.GRAY; isAntiAlias = true }

        canvas.drawLine(MARGIN, y - 15f, PAGE_WIDTH - MARGIN, y - 15f, p)

        canvas.drawText(
            "Dieser Nachweis basiert auf dem NLBK Musterausbildungsplan MGA Niedersachsen (Stand: 18.06.2025).",
            MARGIN, y, p
        )

        val infoRight = "Generiert mit Feuerwehr-Ausbildungs-Tracker"
        val width = p.measureText(infoRight)
        canvas.drawText(infoRight, PAGE_WIDTH - MARGIN - width, y, p)
    }

    fun teilen(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "PDF teilen/öffnen"))
    }
}