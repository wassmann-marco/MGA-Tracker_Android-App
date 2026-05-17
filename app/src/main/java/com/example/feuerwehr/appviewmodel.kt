package com.example.feuerwehr

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.min

// --- 1. MODELS ---

@Entity(tableName = "training_entries")
data class TrainingEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val modulId: String,
    val datum: String,
    val startStunde: Int,
    val startMinute: Int,
    val endeStunde: Int,
    val endeMinute: Int,
    val ausbilder: String = "",
    val bemerkung: String = "",
    val qs: QS = QS.QS1
)

data class UserProfile(
    val vorname: String,
    val name: String,
    val ortsfeuerwehr: String,
    val ausbildungsstart: String
)

enum class QS { QS1, QS2 }

data class Modul(
    val id: String,
    val name: String,
    val sollStunden: Double,
    val qs: QS
)

val alleModule = listOf(
    // --- QUALIFIKATIONSSTUFE 1 (Summe: 37,0 UE) ---
    Modul("1.2", "Unfallversicherung", 2.0, QS.QS1),
    Modul("3.1", "Fahrzeugkunde Theorie", 1.0, QS.QS1),
    Modul("4.1", "Persönliche & erweiterte Ausrüstung", 1.0, QS.QS1),
    Modul("4.2", "Löschgeräte, Schläuche, Armaturen", 2.0, QS.QS1),
    Modul("4.3", "Geräte für die einfache techn. Hilfeleistung", 3.0, QS.QS1),
    Modul("4.4", "Rettungsgeräte - Knoten und Stiche", 2.0, QS.QS1),
    Modul("4.5", "Rettungsgeräte - Leitern", 3.0, QS.QS1),
    Modul("4.6", "Rettungsgeräte - Sonstige", 2.0, QS.QS1),
    Modul("4.7", "Beleuchtungs- und Warngeräte", 3.0, QS.QS1),           // KORREKT: Keine KatS Stunden
    Modul("5.1", "Erste Hilfe", 9.0, QS.QS1),
    Modul("5.2", "Physische & psychische Belastungen im Einsatz", 3.0, QS.QS1),
    Modul("6.0", "Verhalten bei Gefahr", 4.0, QS.QS1),
    Modul("6.0-K", "Verhalten bei Gefahr (KatS)", 1.0, QS.QS1),

    // --- QUALIFIKATIONSSTUFE 2 (Summe: 110,0 UE) ---
    Modul("1.1", "Organisation der Feuerwehr", 1.0, QS.QS2),
    Modul("2.0", "Brennen und Löschen", 3.0, QS.QS2),
    Modul("3.2", "Fahrzeugkunde Praxis", 3.0, QS.QS2),
    Modul("5.3", "Erste Hilfe Fortbildung", 9.0, QS.QS2),
    Modul("7.0", "Rettung (Technisch/Eis/Höhen/Tiefen)", 9.0, QS.QS2),
    Modul("7.0-K", "Rettung (KatS)", 1.0, QS.QS2),              // Plan: 10 UE (davon 2*)
    Modul("8.1", "Einheiten im Löscheinsatz - Praxis", 16.0, QS.QS2),
    Modul("8.2", "Einsatzübung Löscheinsatz", 30.0, QS.QS2),
    Modul("9.1", "Einheiten im Hilfeleistungseinsatz", 5.0, QS.QS2),
    Modul("9.2", "Einsatzübung Techn. Hilfe", 5.0, QS.QS2),
    Modul("9.2-K", "Einsatzübung Techn. Hilfe (KatS)", 3.0, QS.QS2),
    Modul("10.1", "ABC-Gefahrstoffe - Kennzeichnung", 3.0, QS.QS2),
    Modul("10.2", "ABC-Gefahrstoffe - Gefahren/Verhalten", 3.0, QS.QS2),
    Modul("11.0", "Sprechfunk-Einstiegsmodul", 8.0, QS.QS2),
    Modul("12.0", "Objektkunde", 5.0, QS.QS2),
    Modul("13.1-K", "Grundlagen Zivil- & KatS ", 2.0, QS.QS2),
    Modul("13.2-K", "Besondere Gefahren im Zivilschutz, Kampfmittel (KatS)", 3.0, QS.QS2),
    Modul("13.3-K", "Sonderfahrzeuge (KatS)", 2.0, QS.QS2)
)

// --- 2. VIEWMODEL ---

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = TrainingDatabase.getInstance(app)
    val repository = TrainingRepository(db.dao())
    private val prefs = app.getSharedPreferences("FeuerwehrPrefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    val eintraege: StateFlow<List<TrainingEntry>> =
        repository.eintraege.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bekannteAusbilder: StateFlow<List<String>> = repository.eintraege
        .map { liste ->
            liste.map { it.ausbilder }
                .filter { it.isNotBlank() }
                .distinct()
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedQs = MutableStateFlow(QS.QS1)

    private fun loadUserProfile(): UserProfile {
        return UserProfile(
            vorname = prefs.getString("vorname", "") ?: "",
            name = prefs.getString("name", "") ?: "",
            ortsfeuerwehr = prefs.getString("ortsfeuerwehr", "") ?: "",
            ausbildungsstart = prefs.getString("ausbildungsstart", "") ?: ""
        )
    }

    fun updateUserProfile(newProfile: UserProfile) {
        _userProfile.value = newProfile
        prefs.edit().apply {
            putString("vorname", newProfile.vorname)
            putString("name", newProfile.name)
            putString("ortsfeuerwehr", newProfile.ortsfeuerwehr)
            putString("ausbildungsstart", newProfile.ausbildungsstart)
            apply()
        }
    }

    fun neuerEintrag(entry: TrainingEntry) = viewModelScope.launch {
        repository.speichern(entry)
    }

    fun loeschen(entry: TrainingEntry) = viewModelScope.launch {
        repository.loeschen(entry)
    }

    val pendingImportUri = MutableStateFlow<Uri?>(null)
    fun setPendingImport(uri: Uri?) { pendingImportUri.value = uri }
    fun abbrechenImport() { pendingImportUri.value = null }

    // --- BERECHNUNGEN (DECKELUNG EINGEBAUT) ---

    // Reine Ist-Stunden (für Anzeige im Modul-Check)
    fun getGeleisteteStunden(eintraege: List<TrainingEntry>, modulId: String): Double {
        return eintraege.filter { it.modulId == modulId }.sumOf {
            val start = it.startStunde * 60 + it.startMinute
            val ende = it.endeStunde * 60 + it.endeMinute
            val diff = (ende - start).toDouble()
            if (diff > 0) diff else 0.0
        } / 60.0
    }

    // Reine Ist-UE (für Anzeige im Modul-Check)
    fun getIstUE(eintraege: List<TrainingEntry>, modulId: String): Double {
        val stunden = getGeleisteteStunden(eintraege, modulId)
        return (stunden * 60.0) / 45.0
    }

    // NEU: Berechnet nur die UE, die auf das Soll angerechnet werden dürfen (MAXIMAL das Soll)
    fun getAnrechenbareUE(eintraege: List<TrainingEntry>, modul: Modul): Double {
        val tatsaechlicheUE = getIstUE(eintraege, modul.id)
        return min(tatsaechlicheUE, modul.sollStunden)
    }

    // Fortschritt der QS (0.0 bis 1.0) mit Deckelung
    fun qsFortschritt(eintraege: List<TrainingEntry>, stufe: QS): Float {
        val moduleDerStufe = alleModule.filter { it.qs == stufe }
        val sollGesamt = moduleDerStufe.sumOf { it.sollStunden }
        // Wir rechnen hier nur mit den anrechenbaren Stunden pro Modul
        val istAnrechenbar = moduleDerStufe.sumOf { getAnrechenbareUE(eintraege, it) }

        return if (sollGesamt > 0) (istAnrechenbar / sollGesamt).toFloat().coerceIn(0f, 1f) else 0f
    }

    // Gesamtfortschritt (0.0 bis 1.0) über alles mit Deckelung
    fun gesamtFortschritt(eintraege: List<TrainingEntry>): Float {
        val gesamtSoll = alleModule.sumOf { it.sollStunden }
        val gesamtAnrechenbar = alleModule.sumOf { getAnrechenbareUE(eintraege, it) }

        return if (gesamtSoll > 0) (gesamtAnrechenbar / gesamtSoll).toFloat().coerceIn(0f, 1f) else 0f
    }
}