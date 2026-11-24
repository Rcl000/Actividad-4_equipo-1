package com.example.mynotes

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.mynotes.data.Note
import com.example.mynotes.data.NoteDatabase
import com.example.mynotes.data.NoteRepository
import com.example.mynotes.ui.NoteViewModel
import com.example.mynotes.ui.NoteViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Rose = Color(0xFFA5528F)
private val Pink = Color(0xFFD39BC0)
private val LightPink = Color(0xFFF0D7E7)
private val Blue = Color(0xFF8BA8D0)
private val DarkText = Color(0xFF342C3C)

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(NoteRepository(NoteDatabase.getInstance(applicationContext).noteDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Rose, background = LightPink)) {
                NotesApp(viewModel)
            }
        }
    }
}

@Composable
fun NotesApp(viewModel: NoteViewModel) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(Calendar.getInstance().apply { clearTime() }.timeInMillis) }
    var showAll by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }

    // Mostrar todas las tareas inicialmente, y filtrar al seleccionar un día.
    val filtered = notes.filter { note ->
        (showAll || sameDay(note.date, selectedDay)) &&
            (note.title.contains(search, ignoreCase = true) || note.content.contains(search, ignoreCase = true))
    }.sortedWith(compareBy<Note> { it.completed }.thenByDescending { it.date })

    Box(Modifier.fillMaxSize()) {
        // Elegir los fondos segun el tamano real del dispositivo y el modo de color.
        // En tablets (ancho minimo de 600 dp) se usan los fondos horizontales.
        val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
        val darkMode = isSystemInDarkTheme()
        val backgroundRes = when {
            isTablet && darkMode -> R.drawable.fondo_tablet_dark
            isTablet -> R.drawable.fondo_tablet_light
            darkMode -> R.drawable.fondo_dark
            else -> R.drawable.fondo_light
        }
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Mis tareas", tint = Rose)
                Text("Mis tareas", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DarkText)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = "Recordatorios", tint = Color(0xFF607FA7))
                    Spacer(Modifier.width(12.dp))
                    IconButton(
                        onClick = { editing = null; showEditor = true },
                        modifier = Modifier.size(40.dp).background(Blue.copy(alpha = .45f), CircleShape)
                    ) { Icon(Icons.Default.Add, contentDescription = "Agregar tarea", tint = DarkText) }
                }
            }
            Text(monthLabel(), color = DarkText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            WeekStrip(selectedDay) {
                selectedDay = it
                showAll = false
            }
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Todas las tareas" else "Ver todas", color = Rose)
                }
                Spacer(Modifier.weight(1f))
                Text("${filtered.size} tarea(s)", fontSize = 12.sp, color = DarkText)
            }
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("Buscar tareas") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = .55f),
                    unfocusedContainerColor = Color.White.copy(alpha = .55f),
                    focusedBorderColor = Rose,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            Spacer(Modifier.height(14.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay tareas aquí. Presiona + para agregar una 💗", color = DarkText)
                }
            } else {
                val wide = LocalConfiguration.current.screenWidthDp >= 600
                if (wide) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(300.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        gridItems(filtered, key = { it.id }) { note ->
                            TaskCard(note, { editing = note; showEditor = true },
                                { viewModel.delete(note) }, { viewModel.toggleCompleted(note) })
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered, key = { it.id }) { note ->
                            TaskCard(note, { editing = note; showEditor = true },
                                { viewModel.delete(note) }, { viewModel.toggleCompleted(note) })
                        }
                    }
                }
            }
        }
        if (showEditor) {
            TaskEditor(initial = editing,
                onDismiss = { showEditor = false },
                onSave = { viewModel.save(it); showEditor = false })
        }
    }
}

@Composable
private fun WeekStrip(selectedDay: Long, onSelect: (Long) -> Unit) {
    val calendar = remember { Calendar.getInstance().apply { clearTime(); add(Calendar.DAY_OF_MONTH, -3) } }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        repeat(14) { i ->
            val day = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            val selected = sameDay(selectedDay, day.timeInMillis)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(43.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Pink else Color.White.copy(alpha = .5f))
                    .clickable { onSelect(day.timeInMillis) }.padding(vertical = 9.dp)
            ) {
                Text(day.get(Calendar.DAY_OF_MONTH).toString(), fontSize = 14.sp, color = DarkText)
                Text(SimpleDateFormat("EEE", Locale.getDefault()).format(day.time).take(3).uppercase(),
                    fontSize = 9.sp, color = DarkText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(note: Note, onEdit: () -> Unit, onDelete: () -> Unit, onComplete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(true) }
    val color = when (note.priority) {
        "Alta" -> Color(0xFFCD8FB8)
        "Baja" -> Color(0xFFE8CBDE)
        else -> Blue
    }
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                visible = false
                onDelete()
                true
            } else false
        }
    )
    AnimatedVisibility(visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        SwipeToDismissBox(
            state = dismiss,
            backgroundContent = {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFFC75C72)).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White) }
            }
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(color).clickable { expanded = !expanded }
                    .animateContentSize().padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = note.completed,
                        onCheckedChange = { onComplete() },
                        colors = CheckboxDefaults.colors(checkedColor = Rose)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(note.title, fontWeight = FontWeight.SemiBold, color = DarkText,
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (note.completed) TextDecoration.LineThrough else null)
                        Text(
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(note.date)),
                            fontSize = 11.sp, color = DarkText
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(note.priority, color = Color.White, fontSize = 11.sp,
                        modifier = Modifier.background(Rose.copy(alpha = .6f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 8.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(35.dp)) {
                        Icon(Icons.Default.Edit, "Editar", tint = DarkText)
                    }
                }
                AnimatedVisibility(expanded) {
                    Column(Modifier.padding(start = 16.dp, top = 8.dp)) {
                        Text(note.content.ifBlank { "Sin descripción" }, color = DarkText)
                        // Pellizca la imagen adjunta para ampliarla.
                        if (note.imageUri != null) {
                            var zoom by remember { mutableFloatStateOf(1f) }
                            AsyncImage(
                                model = note.imageUri, contentDescription = "Imagen de la tarea",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().height(150.dp)
                                    .graphicsLayer(scaleX = zoom, scaleY = zoom)
                                    .pointerInput(note.imageUri) {
                                        detectTransformGestures { _, _, scale, _ ->
                                            zoom = (zoom * scale).coerceIn(1f, 3f)
                                        }
                                    }
                            )
                        }
                        Text("Desliza para eliminar", fontSize = 11.sp, color = DarkText.copy(alpha = .7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskEditor(initial: Note?, onDismiss: () -> Unit, onSave: (Note) -> Unit) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember(initial?.id) { mutableStateOf(initial?.content.orEmpty()) }
    var priority by remember(initial?.id) { mutableStateOf(initial?.priority ?: "Baja") }
    var date by remember(initial?.id) { mutableLongStateOf(initial?.date ?: System.currentTimeMillis()) }
    var error by remember { mutableStateOf(false) }
    // La ruta se guarda en Room mediante Note.imageUri.
    var imageUri by remember(initial?.id) { mutableStateOf(initial?.imageUri) }
    val ctx = LocalContext.current
    // OpenDocument concede un URI persistente que podemos volver a leer al reiniciar.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Algunos proveedores no permiten conservar el permiso.
            }
            imageUri = uri.toString()
        }
    }

    val c = Calendar.getInstance().apply { timeInMillis = date }
    val picker = DatePickerDialog(
        ctx,
        { _, year, month, day ->
            date = Calendar.getInstance().apply {
                timeInMillis = date
                set(year, month, day)
            }.timeInMillis
        },
        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
    )
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 570.dp)
                .clip(RoundedCornerShape(30.dp)).background(Color(0xFFFFF0FA))
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (initial == null) "Nueva tarea" else "Editar tarea",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DarkText,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar", tint = Rose) }
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it; error = false },
                placeholder = { Text("Escribe una tarea...") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, isError = error,
                shape = RoundedCornerShape(18.dp)
            )
            if (error) Text("Escribe un título para guardar", color = Color(0xFF9A2445), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(),
                minLines = 2, maxLines = 4, shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text("Prioridad", color = DarkText, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Baja", "Media", "Alta").forEach { item ->
                    FilterChip(
                        selected = priority == item,
                        onClick = { priority = item },
                        label = { Text(item, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Fecha", color = DarkText, fontWeight = FontWeight.Medium)
            TextButton(onClick = { picker.show() }) {
                Text(SimpleDateFormat("dd / MMMM / yyyy", Locale.getDefault()).format(Date(date)), color = Rose)
            }
            Spacer(Modifier.height(10.dp))
            Text("Imagen adjunta", color = DarkText, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (imageUri == null) "Agregar imagen" else "Cambiar imagen")
                }
                if (imageUri != null) {
                    TextButton(onClick = { imageUri = null }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Text("Quitar")
                    }
                }
            }
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Vista previa de la imagen adjunta",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(95.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = .65f))
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (title.isBlank()) { error = true; return@Button }
                    onSave(Note(
                        id = initial?.id ?: 0L,
                        title = title.trim(), content = content.trim(),
                        date = date, priority = priority,
                        completed = initial?.completed ?: false,
                        imageUri = imageUri
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rose)
            ) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Guardar tarea") }
        }
    }
}

private fun Calendar.clearTime() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun sameDay(a: Long, b: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = a }
    val calB = Calendar.getInstance().apply { timeInMillis = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}

private fun monthLabel(): String =
    SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
        .format(Date()).replaceFirstChar { it.uppercase() }
