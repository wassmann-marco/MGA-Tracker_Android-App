package com.example.feuerwehr

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(vm: AppViewModel, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- STATES ---
    var datum by remember { mutableStateOf("") }
    var startStunde by remember { mutableIntStateOf(16) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endeStunde by remember { mutableIntStateOf(18) }
    var endeMinute by remember { mutableIntStateOf(0) }
    var ausgewähltesModul by remember { mutableStateOf(alleModule[0]) }
    var ausbilder by remember { mutableStateOf("") }
    var expandedModul by remember { mutableStateOf(false) }

    // Ausbilder-Vorschläge
    val bekannteAusbilder by vm.bekannteAusbilder.collectAsState()
    var showVorschlaege by remember { mutableStateOf(false) }

    val dauerMinuten = (endeStunde * 60 + endeMinute) - (startStunde * 60 + startMinute)
    val dauerText = if (dauerMinuten > 0) {
        val h = dauerMinuten / 60
        val m = dauerMinuten % 60
        if (m == 0) "Dauer: ${h}h" else "Dauer: ${h}h ${m}min"
    } else "Ungültiger Zeitraum"

    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(context, { _, y, m, d ->
        datum = String.format("%02d.%02d.%04d", d, m + 1, y)
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val startTimePicker = TimePickerDialog(context, { _, h, m ->
        startStunde = h; startMinute = m
    }, startStunde, startMinute, true)

    val endTimePicker = TimePickerDialog(context, { _, h, m ->
        endeStunde = h; endeMinute = m
    }, endeStunde, endeMinute, true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("Neuer Eintrag", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
        Spacer(Modifier.height(24.dp))

        // --- DATUM ---
        Text("Datum", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        OutlinedTextField(
            value = datum,
            onValueChange = { datum = it },
            modifier = Modifier.fillMaxWidth().clickable { datePicker.show() },
            readOnly = true,
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text("z.B. 17.04.2026") },
            trailingIcon = {
                IconButton(onClick = { datePicker.show() }) {
                    Icon(Icons.Default.DateRange, null, tint = FeuerwehrRot)
                }
            }
        )

        Spacer(Modifier.height(24.dp))

        // --- ZEITRAUM ---
        Text("Zeitraum", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeDisplayColumn("Von", startStunde, startMinute) { startTimePicker.show() }
                    Text("—", fontSize = 24.sp, color = Color.LightGray)
                    TimeDisplayColumn("Bis", endeStunde, endeMinute) { endTimePicker.show() }
                }
                Spacer(Modifier.height(12.dp))
                Text(dauerText, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- MODUL AUSWAHL ---
        Text("Modul", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        ExposedDropdownMenuBox(
            expanded = expandedModul,
            onExpandedChange = { expandedModul = !expandedModul }
        ) {
            OutlinedTextField(
                value = "${ausgewähltesModul.id} – ${ausgewähltesModul.name}",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModul) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(8.dp)
            )
            ExposedDropdownMenu(expanded = expandedModul, onDismissRequest = { expandedModul = false }) {
                alleModule.forEach { modul ->
                    DropdownMenuItem(
                        text = { Text("${modul.id} – ${modul.name}") },
                        onClick = {
                            ausgewähltesModul = modul
                            expandedModul = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- AUSBILDER (Autocomplete) ---
        Text("Verantwortlicher Ausbilder", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        val gefilterteVorschlaege = bekannteAusbilder.filter {
            it.contains(ausbilder, ignoreCase = true) && it != ausbilder
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = ausbilder,
                onValueChange = {
                    ausbilder = it
                    showVorschlaege = true
                },
                placeholder = { Text("Name des Ausbilders") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            // Das Vorschlags-Menü (DropdownMenuProperties entfernt, um Fehler zu vermeiden)
            DropdownMenu(
                expanded = showVorschlaege && gefilterteVorschlaege.isNotEmpty(),
                onDismissRequest = { showVorschlaege = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                gefilterteVorschlaege.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            ausbilder = name
                            showVorschlaege = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- SPEICHERN BUTTON ---
        Button(
            onClick = {
                if (datum.isEmpty() || ausbilder.isEmpty()) {
                    Toast.makeText(context, "Bitte alle Felder ausfüllen!", Toast.LENGTH_SHORT).show()
                } else {
                    vm.neuerEintrag(
                        TrainingEntry(
                            datum = datum,
                            startStunde = startStunde,
                            startMinute = startMinute,
                            endeStunde = endeStunde,
                            endeMinute = endeMinute,
                            modulId = ausgewähltesModul.id,
                            ausbilder = ausbilder
                        )
                    )
                    Toast.makeText(context, "Eintrag gespeichert! 🚒", Toast.LENGTH_SHORT).show()
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (datum.isNotEmpty() && ausbilder.isNotEmpty()) FeuerwehrRot else Color.LightGray),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Eintrag speichern", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun TimeDisplayColumn(label: String, h: Int, m: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeBox(String.format("%02d", h))
            Text(" : ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            TimeBox(String.format("%02d", m))
        }
    }
}

@Composable
fun TimeBox(text: String) {
    Box(
        modifier = Modifier
            .size(width = 45.dp, height = 55.dp)
            .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
    }
}