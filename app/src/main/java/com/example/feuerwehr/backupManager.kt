package com.example.feuerwehr

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

object BackupManager {

    suspend fun exportiereBackup(context: Context, repository: TrainingRepository) {
        val sharedPrefs = context.getSharedPreferences("FeuerwehrPrefs", Context.MODE_PRIVATE)
        val alleEintraege = repository.getAlleFuerExport()

        val backupJson = JSONObject()

        // 1. Profil-Daten einpacken
        val profilJson = JSONObject().apply {
            put("vorname", sharedPrefs.getString("vorname", ""))
            put("name", sharedPrefs.getString("name", ""))
            put("ortsfeuerwehr", sharedPrefs.getString("ortsfeuerwehr", ""))
            put("ausbildungsstart", sharedPrefs.getString("ausbildungsstart", ""))
        }
        backupJson.put("profil", profilJson)

        // 2. Trainingseinträge einpacken
        val eintraegeArray = JSONArray()
        alleEintraege.forEach { entry ->
            val eJson = JSONObject().apply {
                put("modulId", entry.modulId)
                put("datum", entry.datum)
                put("startStunde", entry.startStunde)
                put("startMinute", entry.startMinute)
                put("endeStunde", entry.endeStunde)
                put("endeMinute", entry.endeMinute)
                put("ausbilder", entry.ausbilder)
                put("bemerkung", entry.bemerkung)
                put("qs", entry.qs.name) // Speichert "QS1" oder "QS2"
            }
            eintraegeArray.put(eJson)
        }
        backupJson.put("eintraege", eintraegeArray)

        // 3. Datei speichern
        val dateiname = "MGA_Backup_${LocalDate.now()}.json"
        val file = File(context.cacheDir, dateiname)
        file.writeText(backupJson.toString(4))

        // 4. Teilen
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Sicherung teilen"))
    }

    suspend fun importiereBackup(context: Context, uri: Uri, repository: TrainingRepository, vm: AppViewModel) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return
            val backup = JSONObject(content)

            // 1. Profil wiederherstellen
            if (backup.has("profil")) {
                val p = backup.getJSONObject("profil")
                val v = p.optString("vorname")
                val n = p.optString("name")
                val o = p.optString("ortsfeuerwehr").ifBlank { p.optString("ortswehr") } // Kompatibilität mit alten Backups
                val s = p.optString("ausbildungsstart")

                context.getSharedPreferences("FeuerwehrPrefs", Context.MODE_PRIVATE).edit().apply {
                    putString("vorname", v)
                    putString("name", n)
                    putString("ortsfeuerwehr", o)
                    putString("ausbildungsstart", s)
                    apply()
                }
                vm.updateUserProfile(UserProfile(v, n, o, s))
            }

            // 2. Einträge wiederherstellen
            if (backup.has("eintraege")) {
                val array = backup.getJSONArray("eintraege")
                val importListe = mutableListOf<TrainingEntry>()

                for (i in 0 until array.length()) {
                    val e = array.getJSONObject(i)

                    // WICHTIG: Hier nutzen wir jetzt benannte Parameter (modulId = ...)
                    // Damit Room und Kotlin genau wissen, welcher Wert in welches Feld gehört.
                    importListe.add(
                        TrainingEntry(
                            id = 0, // Neue ID vergeben lassen
                            modulId = e.getString("modulId"),
                            datum = e.getString("datum"),
                            startStunde = e.getInt("startStunde"),
                            startMinute = e.getInt("startMinute"),
                            endeStunde = e.getInt("endeStunde"),
                            endeMinute = e.getInt("endeMinute"),
                            ausbilder = e.optString("ausbilder", ""),
                            bemerkung = e.optString("bemerkung", ""),
                            qs = if (e.optString("qs") == "QS2") QS.QS2 else QS.QS1
                        )
                    )
                }
                repository.importiereAlle(importListe)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}