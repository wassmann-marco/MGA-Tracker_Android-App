package com.example.feuerwehr

import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val InfoBgColor = Color(0xFFF5F5F5)

// Hilfsfunktion zum Auslesen der Version
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "Unbekannt"
    } catch (e: Exception) {
        "1.x"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val profile by vm.userProfile.collectAsState()

    // Version hier einmalig abrufen
    val appVersion = remember { getAppVersion(context) }

    var vorname by remember { mutableStateOf(profile.vorname) }
    var nachname by remember { mutableStateOf(profile.name) }
    var ortswehr by remember { mutableStateOf(profile.ortsfeuerwehr) }
    var startDatum by remember { mutableStateOf(profile.ausbildungsstart) }

    val calendar = Calendar.getInstance().also { cal ->
        if (startDatum.isNotBlank()) {
            try {
                val parts = startDatum.split(".")
                if (parts.size == 3) cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
            } catch (_: Exception) { }
        }
    }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            startDatum = String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(profile) {
        vorname = profile.vorname
        nachname = profile.name
        ortswehr = profile.ortsfeuerwehr
        startDatum = profile.ausbildungsstart
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Benutzerprofil",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = FeuerwehrRot,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = vorname,
            onValueChange = { vorname = it },
            label = { Text("Vorname") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = nachname,
            onValueChange = { nachname = it },
            label = { Text("Nachname") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = ortswehr,
            onValueChange = { ortswehr = it },
            label = { Text("Ortsfeuerwehr") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = startDatum,
            onValueChange = { startDatum = it },
            label = { Text("Ausbildungsstart") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() },
            readOnly = true,
            enabled = true,
            trailingIcon = {
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Kalender öffnen",
                        tint = FeuerwehrRot
                    )
                }
            },
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                vm.updateUserProfile(
                    UserProfile(vorname, nachname, ortswehr, startDatum)
                )
                Toast.makeText(context, "Profil erfolgreich gespeichert!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FeuerwehrRot),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Profil speichern", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = InfoBgColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Über den Ausbildungs-Tracker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ich hoffe diese App kann euch dabei unterstützen, euren Ausbildungsstand im Blick zu behalten.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Entwickelt von:",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Marco Waßmann / FF Hehlen",
                    fontWeight = FontWeight.Bold,
                    color = FeuerwehrRot,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                // HIER WIRD DIE VERSION DYNAMISCH ANGEZEIGT
                Text(
                    text = "Version $appVersion",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = InfoBgColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gut zu wissen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Dieser Tracker ist ein Hilfsmittel für dich, kein offizielles Dokument. Maßgeblich für deinen Ausbildungsstand sind allein deine Daten in FeuerON bzw. im Dienstbuch. Ich gebe mein Bestes, die App aktuell zu halten – trotzdem können sich Fehler einschleichen.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}