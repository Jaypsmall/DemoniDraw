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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            
            DemonidrawTheme(darkTheme = isDarkMode) {
                // Modo Luz: Fondo plateado suave para que resalte el amarillo
                val backgroundColor = if (isDarkMode) AbyssBlack else Color(0xFFD1D5D8) // ShinySilver-ish
                Surface(color = backgroundColor) {
                    MainScreen(viewModel, isDarkMode, onThemeToggle = { isDarkMode = !isDarkMode })
                }
            }
        }
    }
}

@Composable
fun FadingSeparator(isDarkMode: Boolean) {
    val centerColor = if (isDarkMode) DeepBlood else HellRed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        centerColor,
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun StyledTitle(showEmoji: Boolean = true, isDarkMode: Boolean = true, fontSize: Int = 24, isCentered: Boolean = false) {
    val drawColor = if (isDarkMode) Color.White else AbyssBlack
    Text(
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Red)) {
                append("De")
            }
            withStyle(style = SpanStyle(color = Color.Yellow)) {
                append("moni")
            }
            withStyle(style = SpanStyle(color = drawColor)) {
                append("Draw")
            }
            if (showEmoji) {
                append(" 😈")
            }
        },
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
        modifier = if (isCentered) Modifier.fillMaxWidth() else Modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GestureViewModel, isDarkMode: Boolean, onThemeToggle: () -> Unit) {
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
                modifier = Modifier.width(300.dp),
                drawerContainerColor = backgroundColor,
                drawerContentColor = contentColor
            ) {
                DrawerContent(isDarkMode)
            }
        }
    ) {
        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                TopAppBar(
                    title = { StyledTitle(isDarkMode = isDarkMode, fontSize = 28) },
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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = HellRed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                FadingSeparator(isDarkMode)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DemoniButton(
                        text = "Iniciar Botón Flotante",
                        isDarkMode = isDarkMode,
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
                                Toast.makeText(context, "Servicio iniciado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DemoniButton(
                        text = "Solicitar Acceso Root",
                        isSecondary = true,
                        isDarkMode = isDarkMode,
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val hasRoot = ShellUtils.executeCommand("id")
                                withContext(Dispatchers.Main) {
                                    if (hasRoot) {
                                        Toast.makeText(context, "Root obtenido!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Fallo al obtener Root", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                FadingSeparator(isDarkMode)

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
fun DemoniButton(text: String, onClick: () -> Unit, isSecondary: Boolean = false, isDarkMode: Boolean = true) {
    val borderColor = if (isDarkMode) ShinySilver else AbyssBlack
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSecondary) DeepBlood else HellRed
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, borderColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }
}

@Composable
fun DrawerContent(isDarkMode: Boolean) {
    val contentColor = if (isDarkMode) Color.White else AbyssBlack
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        StyledTitle(isDarkMode = isDarkMode, fontSize = 26, isCentered = true)
        Spacer(modifier = Modifier.height(24.dp))
        FadingSeparator(isDarkMode)
        Spacer(modifier = Modifier.height(24.dp))
        
        DrawerItem(label = "Ajustes", icon = Icons.Default.Menu, contentColor = contentColor)
        DrawerItem(label = "Idiomas", icon = Icons.Default.Menu, contentColor = contentColor)
        DrawerItem(label = "Importar", icon = Icons.Default.Upload, contentColor = contentColor)
        DrawerItem(label = "Exportar", icon = Icons.Default.Download, contentColor = contentColor)
        DrawerItem(label = "Ayuda", icon = Icons.Default.Menu, contentColor = contentColor)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            "v1.0 - Edición Demoniaca",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall,
            color = AshGrey
        )
    }
}

@Composable
fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, contentColor: Color) {
    Surface(
        onClick = { /* TODO */ },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Obsidian),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = HellRed, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = contentColor, fontSize = 16.sp)
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
    // Tarjeta más clara que el fondo en modo luz (Blanco sobre Plateado)
    val cardBg = if (isDarkMode) Obsidian else Color.White
    val textColor = if (isDarkMode) Color.White else AbyssBlack
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick(gesture) }
                )
            },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, if (isDarkMode) DeepBlood else HellRed), // Borde Rojo Demonio
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    fontSize = 18.sp,
                    color = textColor
                )
                Text(
                    text = "Action: ${gesture.action}",
                    fontSize = 14.sp,
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
                colors = IconButtonDefaults.iconButtonColors(contentColor = HellRed)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
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
