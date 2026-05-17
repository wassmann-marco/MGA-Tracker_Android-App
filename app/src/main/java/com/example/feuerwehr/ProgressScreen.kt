package com.example.feuerwehr

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val datumFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private fun parseEntryDate(datum: String): LocalDate = try {
    LocalDate.parse(datum, datumFormatter)
} catch (_: Exception) { LocalDate.MIN }

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val eintraege by vm.eintraege.collectAsState()
    val selectedQs by vm.selectedQs.collectAsState()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // --- BERECHNUNGEN FÜR DIE TABS ---
    fun getStatsForStufe(stufe: QS): Pair<Float, Double> {
        val module = alleModule.filter { it.qs == stufe }
        val soll = module.sumOf { it.sollStunden }
        val istGedeckelt = module.sumOf { m ->
            val ist = vm.getIstUE(eintraege, m.id)
            if (ist > m.sollStunden) m.sollStunden else ist
        }
        val prozent = if (soll > 0) (istGedeckelt / soll).toFloat().coerceIn(0f, 1f) else 0f
        return Pair(prozent, soll)
    }

    val statsQS1 = getStatsForStufe(QS.QS1)
    val statsQS2 = getStatsForStufe(QS.QS2)
    val moduleDerStufe = alleModule.filter { it.qs == selectedQs }

    // --- LOGIK FÜR AUTO-FILL QS1 ---
    fun qs1SchnellAusfuellen() {
        val heute = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.now())
        alleModule.filter { it.qs == QS.QS1 }.forEach { modul ->
            val istUE = vm.getIstUE(eintraege, modul.id)
            val fehltUE = modul.sollStunden - istUE
            if (fehltUE > 0) {
                val gesamtMinuten = (fehltUE * 45).toInt()
                val h = gesamtMinuten / 60
                val m = gesamtMinuten % 60
                vm.neuerEintrag(
                    TrainingEntry(
                        modulId = modul.id,
                        datum = heute,
                        startStunde = 18,
                        startMinute = 0,
                        endeStunde = 18 + h,
                        endeMinute = m,
                        ausbilder = "System (Auto)",
                        bemerkung = "Automatisch vervollständigt",
                        qs = QS.QS1
                    )
                )
            }
        }
        Toast.makeText(context, "QS 1 abgeschlossen! 🚒", Toast.LENGTH_SHORT).show()
    }

    // --- SICHERHEITS-DIALOG FÜR AUTO-FILL ---
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = FeuerwehrRot) },
            title = { Text("QS 1 abschließen?") },
            text = { Text("Möchtest du alle fehlenden Module der QS 1 automatisch als abgeschlossen markieren?") },
            confirmButton = {
                Button(
                    onClick = {
                        qs1SchnellAusfuellen()
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("Ja, abschließen") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
        Text("Modul-Check", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
        Spacer(modifier = Modifier.height(16.dp))

        // --- TABS / UMSCHALTER ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QSSelectorBox(
                label = "QS 1",
                prozent = statsQS1.first,
                isSelected = selectedQs == QS.QS1,
                modifier = Modifier.weight(1f),
                onClick = { vm.selectedQs.value = QS.QS1 }
            )
            QSSelectorBox(
                label = "QS 2",
                prozent = statsQS2.first,
                isSelected = selectedQs == QS.QS2,
                modifier = Modifier.weight(1f),
                onClick = { vm.selectedQs.value = QS.QS2 }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Module für ${if(selectedQs == QS.QS1) "Einsatzfähigkeit" else "Truppmitglied"}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))

        // --- LISTE ---
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(moduleDerStufe) { modul ->
                ModulItem(modul, vm, eintraege)
            }

            // --- AUTO-FILL BUTTON AM ENDE DER LISTE ---
            if (selectedQs == QS.QS1 && statsQS1.first < 1f) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .clickable { showDialog = true },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = FeuerwehrGold)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Alle QS 1 Module abschließen",
                                color = FeuerwehrGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QSSelectorBox(label: String, prozent: Float, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) FeuerwehrRot else Color.White
    val highlightColor = if (isSelected) FeuerwehrGold else FeuerwehrRot
    val labelColor = if (isSelected) FeuerwehrGold else Color.DarkGray
    val borderColor = if (isSelected) FeuerwehrGold else Color.LightGray

    val emoji = when {
        prozent >= 1f -> "🤩"
        prozent >= 0.5f -> "🙂"
        prozent >= 0.2f -> "😐"
        else -> "😟"
    }

    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = labelColor)
                Spacer(Modifier.width(10.dp))
                Text(emoji, fontSize = 30.sp)
            }
            Text("${(prozent * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 28.sp, color = highlightColor)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { prozent },
                modifier = Modifier.fillMaxWidth(0.85f).height(8.dp).clip(RoundedCornerShape(50)),
                color = highlightColor,
                trackColor = if (isSelected) Color.White.copy(alpha = 0.3f) else Color.LightGray
            )
        }
    }
}

@Composable
fun ModulItem(modul: Modul, vm: AppViewModel, eintraege: List<TrainingEntry>) {
    var expanded by remember { mutableStateOf(false) }
    var zuLoeschen by remember { mutableStateOf<TrainingEntry?>(null) }

    if (zuLoeschen != null) {
        AlertDialog(
            onDismissRequest = { zuLoeschen = null },
            icon = { Icon(Icons.Default.Warning, null, tint = FeuerwehrRot) },
            title = { Text("Eintrag löschen?") },
            text = { Text("Dieser Eintrag wird unwiderruflich gelöscht.") },
            confirmButton = {
                Button(
                    onClick = { vm.loeschen(zuLoeschen!!); zuLoeschen = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FeuerwehrRot)
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { zuLoeschen = null }) { Text("Abbrechen", color = Color.Gray) }
            }
        )
    }
    val istUE = vm.getIstUE(eintraege, modul.id)
    val sollUE = modul.sollStunden
    val progress = if (sollUE > 0) (istUE / sollUE).toFloat().coerceIn(0f, 1f) else 0f
    val isDone = istUE >= sollUE

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Modul ${modul.id}", fontSize = 11.sp, color = Color.Gray)
                    Text(modul.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (isDone) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (isDone) Color(0xFF2E7D32) else FeuerwehrRot,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${String.format("%.1f", istUE)} / ${sollUE.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // --- AUFGEKLAPPTE ANSICHT MIT LÖSCHFUNKTION ---
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                    val modulEintraege = eintraege.filter { it.modulId == modul.id }
                        .sortedByDescending { parseEntryDate(it.datum) }
                    if (modulEintraege.isEmpty()) {
                        Text("Keine Einträge", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    } else {
                        modulEintraege.forEach { e ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${e.datum}: ${e.ausbilder}", fontSize = 12.sp)
                                    if (e.bemerkung.isNotBlank()) {
                                        Text(e.bemerkung, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }

                                val ue = ((e.endeStunde*60+e.endeMinute)-(e.startStunde*60+e.startMinute))/45.0
                                Text("${String.format("%.1f", ue)} UE", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.width(12.dp))

                                // --- LÖSCH-BUTTON (DAS X) ---
                                IconButton(
                                    onClick = { zuLoeschen = e },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Löschen",
                                        tint = FeuerwehrRot,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}