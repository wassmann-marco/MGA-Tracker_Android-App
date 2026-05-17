package com.example.feuerwehr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun OverviewScreen(vm: AppViewModel, onNavigateToProgress: (QS) -> Unit = {}) {
    val eintraege by vm.eintraege.collectAsState()
    val profile by vm.userProfile.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- LOGIK: ANRECHENBARE UE & ABGESCHLOSSENE MODULE ---
    fun getAnrechenbareUE(modul: Modul): Double {
        val istUE = vm.getIstUE(eintraege, modul.id)
        return if (istUE > modul.sollStunden) modul.sollStunden else istUE
    }

    fun isModulAbgeschlossen(modul: Modul): Boolean {
        return vm.getIstUE(eintraege, modul.id) >= modul.sollStunden
    }

    val qs1Module = alleModule.filter { it.qs == QS.QS1 }
    val qs1Soll = qs1Module.sumOf { it.sollStunden }
    val qs1IstGedeckelt = qs1Module.sumOf { getAnrechenbareUE(it) }
    val qs1Prozent = if (qs1Soll > 0) (qs1IstGedeckelt / qs1Soll).toFloat().coerceIn(0f, 1f) else 0f

    val qs2Module = alleModule.filter { it.qs == QS.QS2 }
    val qs2Soll = qs2Module.sumOf { it.sollStunden }
    val qs2IstGedeckelt = qs2Module.sumOf { getAnrechenbareUE(it) }
    val qs2Prozent = if (qs2Soll > 0) (qs2IstGedeckelt / qs2Soll).toFloat().coerceIn(0f, 1f) else 0f

    val gesamtSoll = qs1Soll + qs2Soll
    val gesamtIstGedeckelt = qs1IstGedeckelt + qs2IstGedeckelt
    val gesamtProzent = if (gesamtSoll > 0) (gesamtIstGedeckelt / gesamtSoll).toFloat().coerceIn(0f, 1f) else 0f

    // Statistik-Werte
    val gesamtIstEcht = alleModule.sumOf { vm.getIstUE(eintraege, it.id) }
    val anzahlAbgeschlossen = alleModule.count { isModulAbgeschlossen(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
                Text("MGA Niedersachsen", fontSize = 12.sp, color = Color.Gray)
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val file = PdfExport.erstellen(context, eintraege)
                                withContext(Dispatchers.Main) { PdfExport.teilen(context, file) }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    },
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "PDF",
                    fontWeight = FontWeight.Bold,
                    color = FeuerwehrRot,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- GESAMTFORTSCHRITT KARTE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FeuerwehrRot),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("Gesamtfortschritt", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${(gesamtProzent * 100).toInt()}%", color = FeuerwehrGold, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { gesamtProzent },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)),
                    color = FeuerwehrGold,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- QUALIFIKATIONSSTUFEN ---
        Text("Qualifikationsstufen", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        QSCard("QS 1 Einsatzfähigkeit", qs1IstGedeckelt, qs1Soll, qs1Prozent) {
            onNavigateToProgress(QS.QS1)
        }
        Spacer(modifier = Modifier.height(12.dp))
        QSCard("QS 2 Truppmitglied", qs2IstGedeckelt, qs2Soll, qs2Prozent) {
            onNavigateToProgress(QS.QS2)
        }

        val letzteEintraege = eintraege.sortedByDescending {
            try { java.time.LocalDate.parse(it.datum, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) }
            catch (_: Exception) { java.time.LocalDate.MIN }
        }.take(3)
        if (letzteEintraege.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Zuletzt eingetragen", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            letzteEintraege.forEach { e ->
                val modulName = alleModule.find { it.id == e.modulId }?.name ?: e.modulId
                val ue = ((e.endeStunde * 60 + e.endeMinute) - (e.startStunde * 60 + e.startMinute)) / 45.0
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${e.datum} • ${e.ausbilder}", fontSize = 12.sp, color = Color.Gray)
                            Text(modulName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(String.format("%.1f UE", ue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- STATS ROW (KORRIGIERT: Label "Module abgeschlossen") ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(String.format("%.1f", gesamtIstEcht), "UE Gesamt")
                StatItem(String.format("%.1f", gesamtIstGedeckelt), "UE Anrechenbar")
                StatItem("$anzahlAbgeschlossen / ${alleModule.size}", "Module abgeschlossen")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- AUSBILDUNGSZEITRAUM ---
        Text("Ausbildungszeitraum", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        ZeitraumCard(profile.ausbildungsstart)

        Spacer(modifier = Modifier.height(24.dp))

        // --- INFO FOOTER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEFEF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = FeuerwehrRot, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Basierend auf:\nNLBK Musterausbildungsplan für die modulare Grundlagenausbildung in Niedersachsen\nStand: 18.06.2025",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun QSCard(titel: String, ist: Double, soll: Double, progress: Float, onClick: () -> Unit) {
    val emoji = when {
        progress >= 1f -> "🤩"
        progress >= 0.5f -> "🙂"
        progress >= 0.2f -> "😐"
        else -> "😟"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titel, fontWeight = FontWeight.Bold, color = FeuerwehrRot, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${String.format("%.1f", ist)} / ${soll.toInt()} UE", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                    color = if (progress >= 1f) Color(0xFF2E7D32) else FeuerwehrRot,
                    trackColor = Color.LightGray.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(emoji, fontSize = 32.sp)
        }
    }
}

@Composable
fun StatItem(wert: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(wert, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
        Text(label, fontSize = 9.sp, color = Color.Gray, lineHeight = 10.sp)
    }
}

@Composable
fun ZeitraumCard(startDatumStr: String) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val startDate = try {
        LocalDate.parse(startDatumStr, formatter)
    } catch (e: Exception) {
        LocalDate.now()
    }

    val endDate = startDate.plusYears(3)
    val today = LocalDate.now()

    val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toFloat()
    val daysPassed = ChronoUnit.DAYS.between(startDate, today).toFloat().coerceAtLeast(0f)
    val daysLeft = ChronoUnit.DAYS.between(today, endDate).coerceAtLeast(0)

    val progress = if (totalDays > 0) (daysPassed / totalDays).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Zeitlicher Fortschritt", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text("$daysLeft Tage bis Ende", fontWeight = FontWeight.Bold, color = FeuerwehrRot, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = FeuerwehrRot,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Start: ${startDate.format(formatter)}", fontSize = 10.sp, color = Color.Gray)
                Text("Ende: ${endDate.format(formatter)}", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}