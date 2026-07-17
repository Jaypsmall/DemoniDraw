package com.jaylizapp.demonidraw

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

class MainActivity : ComponentActivity() {
    private val viewModel: GestureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var isEnglish by remember { mutableStateOf(false) }
            
            DemonidrawTheme(darkTheme = isDarkMode) {
                // Modo Luz: Fondo plateado suave para que resalte el amarillo
                val backgroundColor = if (isDarkMode) AbyssBlack else Color(0xFFD1D5D8) // ShinySilver-ish
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
                        Color(0xFFC0C0C0), // Plata
                        Color.White,       // Brillo central
                        Color(0xFFC0C0C0), // Plata
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun StyledTitle(showEmoji: Boolean = true, fontSize: Int = 24, isCentered: Boolean = false) {
    val titleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f),
        offset = Offset(6f, 6f),
        blurRadius = 12f
    )

    val styledTitle = buildAnnotatedString {
        withStyle(style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
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
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = fontSize.sp),
        textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
        modifier = if (isCentered) Modifier.fillMaxWidth() else Modifier
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

    val backgroundColor = if (isDarkMode) AbyssBlack else Color(0xFFD1D5D8)
    val contentColor = if (isDarkMode) Color.White else AbyssBlack

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = backgroundColor,
                modifier = Modifier.width(300.dp) 
            ) {
                DrawerContent(isDarkMode, isEnglish, onLanguageToggle)
            }
        }
    ) {
        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                Column {
                    TopAppBar(
                        title = { StyledTitle(fontSize = 28) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = contentColor)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = backgroundColor,
                            titleContentColor = contentColor
                        ),
                        actions = {
                            IconButton(onClick = onThemeToggle) {
                                Icon(
                                    Icons.Default.Nightlight,
                                    contentDescription = "Toggle Theme",
                                    tint = contentColor
                                )
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
                // --- BOTONES PRINCIPALES ---
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

                Spacer(modifier = Modifier.height(16.dp))
                FadingSeparator()

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
        }
    }
}

@Composable
fun DemoniButton(text: String, onClick: () -> Unit, isSecondary: Boolean = false) {
    val containerColor = if (isSecondary) DeepBlood else HellRed
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp)
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
                shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), offset = Offset(2f, 2f), blurRadius = 4f)
            ),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DrawerContent(isDarkMode: Boolean, isEnglish: Boolean, onLanguageToggle: () -> Unit) {
    val contentColor = if (isDarkMode) Color.White else AbyssBlack
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        StyledTitle(fontSize = 26, isCentered = true)
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 32.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        
        DrawerItem(label = if (isEnglish) "Settings" else "Ajustes", icon = Icons.Default.Settings, contentColor = contentColor)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Languages" else "Idiomas", icon = Icons.Default.Language, contentColor = contentColor, onClick = onLanguageToggle)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Import" else "Importar", icon = Icons.Default.Upload, contentColor = contentColor)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Export" else "Exportar", icon = Icons.Default.Download, contentColor = contentColor)
        Spacer(modifier = Modifier.height(16.dp))
        DrawerItem(label = if (isEnglish) "Help" else "Ayuda", icon = Icons.Default.Help, contentColor = contentColor)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val footerColor = if (isDarkMode) AshGrey else Color.DarkGray
            Text(
                text = "DemoniDraw v1.0.1", 
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
fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, contentColor: Color, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
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
            Text(label, color = contentColor, style = MaterialTheme.typography.titleMedium)
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
                if (!isShell) {
                    Text(
                        "Introduce el nombre del paquete (ej: com.whatsapp)",
                        color = AshGrey,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
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
