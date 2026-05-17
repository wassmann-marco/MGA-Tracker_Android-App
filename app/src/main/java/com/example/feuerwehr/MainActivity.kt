@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.feuerwehr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- Zentrale Farbdefinitionen ---
val FeuerwehrRot  = Color(0xFF7D0B23)
val FeuerwehrGold = Color(0xFFFFCC00)
val HellGrau      = Color(0xFFF4F4F4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary    = FeuerwehrRot,
                    secondary  = FeuerwehrGold,
                    background = HellGrau,
                    surface    = Color.White,
                    onPrimary  = Color.White
                )
            ) {
                RootApp()
            }
        }
    }
}

@Composable
fun RootApp() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }
    if (showSplash) {
        SplashScreen()
    } else {
        MainApp()
    }
}

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FeuerwehrRot),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value).padding(horizontal = 20.dp)
        ) {
            Text(text = "Ausbildungstracker für die", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
            Text(text = "MGA", color = FeuerwehrGold, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "in Niedersachsen", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Normal)

            Spacer(Modifier.height(50.dp))

            // Falls du kein Logo hast, kannst du diese Image-Box auch entfernen,
            // ansonsten zieht er sich das Standard-Icon.
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Logo",
                modifier = Modifier.size(300.dp)
            )

            Spacer(Modifier.height(25.dp))

            Text(
                "Retten · Löschen · Bergen · Schützen",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    OVERVIEW("Dashboard", Icons.Default.Home),
    PROGRESS("Modul-Check", Icons.Default.Star),
    ADD("Neuer Eintrag", Icons.Default.Add),
    PROFILE("Profil", Icons.Default.Person)
}

@Composable
fun MainApp(vm: AppViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf(Tab.OVERVIEW) }
    var menuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pendingImportUri by vm.pendingImportUri.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.setPendingImport(it) } }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { vm.abbrechenImport() },
            icon = { Icon(Icons.Default.Info, null, tint = FeuerwehrRot) },
            title = { Text("Daten importieren") },
            text = { Text("Wie sollen die Daten importiert werden?\n\n• Hinzufügen: Zusammenführen mit bestehenden Daten.\n• Ersetzen: Alle vorhandenen Daten werden gelöscht.") },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri!!
                        vm.abbrechenImport()
                        scope.launch {
                            try {
                                vm.repository.loescheAlle()
                                BackupManager.importiereBackup(context, uri, vm.repository, vm)
                                Toast.makeText(context, "Daten ersetzt!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FeuerwehrRot)
                ) { Text("Ersetzen") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.abbrechenImport() }) { Text("Abbrechen", color = Color.Gray) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        val uri = pendingImportUri!!
                        vm.abbrechenImport()
                        scope.launch {
                            try {
                                BackupManager.importiereBackup(context, uri, vm.repository, vm)
                                Toast.makeText(context, "Backup eingespielt!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("Hinzufügen", color = FeuerwehrRot) }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ausbildungs-Tracker", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FeuerwehrRot),
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Daten sichern (Export)") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    BackupManager.exportiereBackup(context, vm.repository)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Daten einspielen (Import)") },
                            leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick  = { currentTab = tab },
                        icon     = { Icon(tab.icon, contentDescription = null) },
                        label    = { Text(tab.label, fontSize = 11.sp) },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor = FeuerwehrRot,
                            selectedTextColor = FeuerwehrRot,
                            indicatorColor    = FeuerwehrRot.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                Tab.OVERVIEW -> OverviewScreen(
                    vm = vm,
                    onNavigateToProgress = { targetQs ->
                        vm.selectedQs.value = targetQs // Sagt dem ViewModel, welcher Tab auf sein soll
                        currentTab = Tab.PROGRESS      // Wechselt die Ansicht nach unten
                    }
                )
                Tab.PROGRESS -> ProgressScreen(vm = vm)
                Tab.ADD -> AddEntryScreen(
                    vm = vm,
                    onSaved = { currentTab = Tab.OVERVIEW }
                )
                Tab.PROFILE -> ProfileScreen(vm = vm)
            }
        }
    }
}