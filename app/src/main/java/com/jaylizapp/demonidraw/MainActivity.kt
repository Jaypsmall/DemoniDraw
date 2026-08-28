package com.jaylizapp.demonidraw

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.jaylizapp.demonidraw.data.GestureEntry
import com.jaylizapp.demonidraw.service.FloatingService
import com.jaylizapp.demonidraw.ui.theme.*
import com.jaylizapp.demonidraw.util.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: GestureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(true) }
            var isEnglish by rememberSaveable { mutableStateOf(false) }
            
            DemonidrawTheme(darkTheme = isDarkMode) {
                val backgroundColor = if (isDarkMode) AbyssBlack else Color(0xFFD1D5D8)
                Surface(color = backgroundColor) {
                    MainScreen(
                        viewModel, 
                        isDarkMode, 
                        isEnglish,
                        onThemeToggle = { isDarkMode = !isDarkMode },
                        onLanguageToggle = { isEnglish = !isEnglish }
                    )
                }
            }
        }
    }
}

@Composable
fun FadingSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFC0C0C0),
                        Color.White,
                        Color(0xFFC0C0C0),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun StyledTitle(
    showEmoji: Boolean = true, 
    fontSize: Int = 28, 
    isCentered: Boolean = false,
    modifier: Modifier = Modifier
) {
    val titleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f),
        offset = Offset(6f, 6f),
        blurRadius = 12f
    )

    val styledTitle = buildAnnotatedString {
        withStyle(style = SpanStyle(
            color = HellRed,
            fontWeight = FontWeight.ExtraBold,
            shadow = titleShadow
        )) {
            append("De")
        }
        withStyle(style = SpanStyle(
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.ExtraBold,
            shadow = titleShadow
        )) {
            append("moni")
        }
        withStyle(style = SpanStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            shadow = titleShadow
        )) {
            append("Draw")
            if (showEmoji) {
                append(" 😈")
            }
        }
    }

    Text(
        text = styledTitle,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold
        ),
        textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
        modifier = modifier.then(if (isCentered) Modifier.fillMaxWidth() else Modifier)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GestureViewModel, 
    isDarkMode: Boolean, 
    isEnglish: Boolean,
    onThemeToggle: () -> Unit,
    onLanguageToggle: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val gestures by viewModel.gestures.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var gestureToEdit by remember { mutableStateOf<GestureEntry?>(null) }
    var showStorageDialog by remember { mutableStateOf(false) }

    val backgroundColor = if (isDarkMode) AbyssBlack else Color(0xFFD1D5D8)
    val contentColor = if (isDarkMode) Color.White else AbyssBlack

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val json = viewModel.exportGesturesToJson()
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            Toast.makeText(context, if (isEnglish) "Export successful!" else "¡Exportación exitosa!", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    val json = reader.readText()
                    if (json.isNotEmpty()) {
                        viewModel.importGesturesFromJson(json)
                        Toast.makeText(context, if (isEnglish) "Import successful!" else "¡Importación exitosa!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = backgroundColor,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .width(300.dp)
                    .statusBarsPadding() // Empieza debajo de la barra de estado
            ) {
                DrawerContent(
                    isDarkMode = isDarkMode, 
                    isEnglish = isEnglish, 
                    onLanguageToggle = onLanguageToggle,
                    onStorageClick = {
                        showStorageDialog = true
                        scope.launch { drawerState.close() }
                    },
                    onImportClick = {
                        importLauncher.launch("*/*") // Más permisivo con los archivos
                        scope.launch { drawerState.close() }
                    },
                    onExportClick = {
                        val timestamp = System.currentTimeMillis()
                        exportLauncher.launch("DemoniDraw_Backup_$timestamp.json")
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                Column {
                    TopAppBar(
                        title = { StyledTitle() },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = contentColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = backgroundColor,
                            titleContentColor = contentColor,
                            navigationIconContentColor = contentColor,
                            actionIconContentColor = contentColor
                        ),
                        actions = {
                            IconButton(onClick = onThemeToggle) {
                                Crossfade(targetState = isDarkMode, animationSpec = tween(500)) { dark ->
                                    Icon(
                                        if (dark) Icons.Default.DarkMode else Icons.Default.NightsStay,
                                        contentDescription = "Toggle Theme",
                                        tint = contentColor
                                    )
                                }
                            }
                        }
                    )
                    FadingSeparator()
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = HellRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                DemoniButton(
                    text = if (isEnglish) "Start Floating Button" else "Iniciar Botón Flotante",
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:${context.packageName}".toUri()
                            )
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(context, FloatingService::class.java)
                            context.startForegroundService(intent)
                            Toast.makeText(context, if (isEnglish) "Service started" else "Servicio iniciado", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                DemoniButton(
                    text = if (isEnglish) "Request Root Access" else "Solicitar Acceso Root",
                    isSecondary = true,
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val hasRoot = ShellUtils.executeCommand("id")
                            withContext(Dispatchers.Main) {
                                if (hasRoot) {
                                    Toast.makeText(context, if (isEnglish) "Root obtained!" else "Root obtenido!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (isEnglish) "Root failed" else "Fallo al obtener Root", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                GestureList(
                    gestures = gestures,
                    onDelete = { viewModel.deleteGesture(it) },
                    onLongClick = { gestureToEdit = it },
                    isDarkMode = isDarkMode
                )
            }
            
            if (showAddDialog) {
                AddGestureDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, action, isShell ->
                        viewModel.addGesture(name, action, isShell)
                        showAddDialog = false
                        val intent = Intent(context, AddGestureActivity::class.java).apply {
                            putExtra("GESTURE_NAME", name)
                        }
                        context.startActivity(intent)
                    },
                    isDarkMode = isDarkMode
                )
            }

            if (gestureToEdit != null) {
                EditGestureDialog(
                    gesture = gestureToEdit!!,
                    onDismiss = { gestureToEdit = null },
                    onConfirm = { updatedGesture ->
                        viewModel.updateGesture(updatedGesture)
                        gestureToEdit = null
                    },
                    isDarkMode = isDarkMode
                )
            }

            if (showStorageDialog) {
                StorageDialog(
                    onDismiss = { showStorageDialog = false },
                    isEnglish = isEnglish,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun DemoniButton(text: String, onClick: () -> Unit, isSecondary: Boolean = false) {
    val containerColor = if (isSecondary) DeepBlood else HellRed
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 4.dp), // Padding interno mínimo
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(
                elevation = 16.dp, 
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black, 
                spotColor = Color.Black
            )
            .border(
                width = 1.5.dp, 
                color = Color.White.copy(alpha = if (isDark) 0.4f else 0.6f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 10.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text, 
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp, // Un pelín más pequeño para que no corte
                shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), offset = Offset(2f, 2f), blurRadius = 4f)
            ),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1 // Evita que salte de línea y se corte verticalmente
        )
    }
}

@Composable
fun DrawerContent(
    isDarkMode: Boolean, 
    isEnglish: Boolean, 
    onLanguageToggle: () -> Unit, 
    onStorageClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StyledTitle(
            fontSize = 28, 
            isCentered = true, 
            modifier = Modifier.padding(vertical = 32.dp)
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(bottom = 32.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        
        DrawerItem(label = if (isEnglish) "Settings" else "Ajustes", icon = Icons.Default.Settings)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Languages" else "Idiomas", icon = Icons.Default.Language, onClick = onLanguageToggle)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Storage" else "Almacenamiento", icon = Icons.Default.Storage, onClick = onStorageClick)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Import" else "Importar", icon = Icons.Default.Upload, onClick = onImportClick)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Export" else "Exportar", icon = Icons.Default.Download, onClick = onExportClick)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Help" else "Ayuda", icon = Icons.AutoMirrored.Filled.Help)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val footerColor = if (isDarkMode) AshGrey else Color.DarkGray
            Text(
                text = "DemoniDraw v1.0.2",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold, 
                color = footerColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Created by JAYLIZ with ❤️", 
                fontSize = 9.sp, 
                color = footerColor.copy(0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) AbyssBlack else Color.White
    val textColor = if (isDark) SoulWhite else AbyssBlack

    Surface(
        onClick = onClick,
        color = containerColor, 
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = HellRed, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label, 
                color = textColor,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun GestureList(
    gestures: List<GestureEntry>,
    onDelete: (GestureEntry) -> Unit,
    onLongClick: (GestureEntry) -> Unit,
    isDarkMode: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
    ) {
        items(gestures) { gesture ->
            GestureItem(gesture, onDelete, onLongClick, isDarkMode)
        }
    }
}

@Composable
fun GestureItem(
    gesture: GestureEntry,
    onDelete: (GestureEntry) -> Unit,
    onLongClick: (GestureEntry) -> Unit,
    isDarkMode: Boolean
) {
    val cardBg = if (isDarkMode) Obsidian else Color.White
    val textColor = if (isDarkMode) Color.White else AbyssBlack
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick(gesture) }
                )
            },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trigger: ${gesture.name}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Text(
                    text = "Action: ${gesture.action}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AshGrey
                )
                if (gesture.isShellCommand) {
                    Text(
                        text = "ROOT ACCESS",
                        fontSize = 12.sp,
                        color = if (isDarkMode) BrimstoneYellow else Color(0xFFC0A000),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            IconButton(
                onClick = { onDelete(gesture) },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .border(
                        width = 1.dp, 
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), 
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete, 
                    contentDescription = "Delete",
                    tint = HellRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StorageDialog(onDismiss: () -> Unit, isEnglish: Boolean, viewModel: GestureViewModel) {
    val context = LocalContext.current
    val gestures by viewModel.gestures.collectAsState()
    
    fun getBackupFiles(): List<File> {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return dir.listFiles { _, name -> 
            name.endsWith(".json") && name.startsWith("DemoniDraw_Backup_") 
        }?.toList() ?: emptyList()
    }

    var files by remember { mutableStateOf(getBackupFiles()) }
    val currentGesturesCount = gestures.size
    val dialogBg = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Obsidian else Color.White
    val textColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else AbyssBlack

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text(if (isEnglish) "Storage Management 📦" else "Gestión de Datos 📦", color = textColor, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(if (isEnglish) "Internal Cache" else "Caché Interna", color = textColor, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Saved gestures: $currentGesturesCount" else "Gestos guardados: $currentGesturesCount",
                        modifier = Modifier.weight(1f),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { 
                        Toast.makeText(context, if (isEnglish) "Cache feature coming soon!" else "¡Función de caché próximamente!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.DeleteForever, "Clear Cache", tint = Color.Red)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                
                Text(if (isEnglish) "Backups (.json)" else "Respaldos en Carpeta (.json)", color = textColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (files.isEmpty()) {
                    Text(if (isEnglish) "No backups found." else "No se encontraron respaldos.", color = AshGrey)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                        items(files) { file ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = file.name, fontSize = 10.sp, color = textColor, modifier = Modifier.weight(1f), maxLines = 1)
                                IconButton(onClick = { 
                                    file.delete()
                                    files = getBackupFiles() 
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, "Delete File", modifier = Modifier.size(18.dp), tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isEnglish) "CLOSE" else "CERRAR", color = HellRed) }
        }
    )
}

@Composable
fun AddGestureDialog(onDismiss: () -> Unit, onConfirm: (String, String, Boolean) -> Unit, isDarkMode: Boolean) {
    var name by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("") }
    var isShell by remember { mutableStateOf(false) }
    
    val dialogBg = if (isDarkMode) Obsidian else Color.White
    val textColor = if (isDarkMode) Color.White else AbyssBlack

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text("Add Gesture Entry", color = textColor) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = action,
                    onValueChange = { action = it },
                    label = { Text("Action (Command/Package)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isShell,
                        onCheckedChange = { isShell = it },
                        colors = CheckboxDefaults.colors(checkedColor = HellRed)
                    )
                    Text("¿Es comando ROOT/Shell?", color = textColor)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, action, isShell) },
                colors = ButtonDefaults.buttonColors(containerColor = HellRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor)
            }
        }
    )
}

@Composable
fun EditGestureDialog(
    gesture: GestureEntry,
    onDismiss: () -> Unit,
    onConfirm: (GestureEntry) -> Unit,
    isDarkMode: Boolean
) {
    var name by remember { mutableStateOf(gesture.name) }
    var action by remember { mutableStateOf(gesture.action) }
    var isShell by remember { mutableStateOf(gesture.isShellCommand) }
    
    val dialogBg = if (isDarkMode) Obsidian else Color.White
    val textColor = if (isDarkMode) Color.White else AbyssBlack

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = { Text("Edit Gesture Entry", color = textColor) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = action,
                    onValueChange = { action = it },
                    label = { Text("Action (Command/Package)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isShell,
                        onCheckedChange = { isShell = it },
                        colors = CheckboxDefaults.colors(checkedColor = HellRed)
                    )
                    Text("¿Es comando ROOT/Shell?", color = textColor)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(gesture.copy(name = name, action = action, isShellCommand = isShell)) },
                colors = ButtonDefaults.buttonColors(containerColor = HellRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor)
            }
        }
    )
}
