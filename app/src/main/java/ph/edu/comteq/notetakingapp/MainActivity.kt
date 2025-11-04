package ph.edu.comteq.notetakingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.edu.comteq.notetakingapp.ui.theme.NoteTakingAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteTakingAppTheme {
                var searchQuery by remember { mutableStateOf("") }
                var isSearchActive by remember { mutableStateOf(false) }
                val notes by viewModel.allNotes.collectAsState(emptyList())

                var isEditorOpen by remember { mutableStateOf(false) }
                var editingNote by remember { mutableStateOf<Note?>(null) }
                var editorSaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                var editorCanSave by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // THE KEY DECISION: Which top bar to show?
                        if (isSearchActive) {
                            SearchBar(
                                modifier = Modifier.fillMaxWidth(),
                                inputField = {
                                    SearchBarDefaults.InputField(
                                        query = searchQuery,
                                        onQueryChange = {
                                            searchQuery = it
                                            viewModel.updateSearchQuery(it)
                                        },
                                        onSearch = {},
                                        expanded = true,
                                        onExpandedChange = { shouldExpand ->
                                            // This is called when the system wants to change expanded state
                                            if (!shouldExpand) {
                                                // User wants to collapse/exit search
                                                isSearchActive = false
                                                searchQuery = ""
                                                viewModel.clearSearch()
                                            }
                                        },
                                        placeholder = {Text("Search notes...")},
                                        leadingIcon = {
                                            IconButton(onClick = {
                                                isSearchActive = false
                                                searchQuery = ""
                                                viewModel.clearSearch()
                                            }) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Close search"
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = {
                                                    searchQuery = ""
                                                    viewModel.clearSearch()
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = "Clear search"
                                                    )
                                                }
                                            }
                                        }
                                    )
                                },
                                expanded = true,
                                onExpandedChange = { shouldExpand ->
                                    // Handle when SearchBar wants to change expanded state
                                    if (!shouldExpand) {
                                        isSearchActive = false
                                        searchQuery = ""
                                        viewModel.clearSearch()
                                    }
                                }
                            ) {
                                // content shown inside the search view
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    if (notes.isEmpty()) {
                                        item {
                                            Text(
                                                text = "No notes found",
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    } else {
                                        items(notes) { note ->
                                            NoteCard(note = note)
                                        }
                                    }
                                }
                            }
                        } else {
                            // NORMAL MODE: Show regular TopAppBar
                            TopAppBar(
                                title = {
                                    Text("Notes")
                                        },
                                actions = {
                                    IconButton(onClick = { isSearchActive = true  }) {
                                        Icon(Icons.Filled.Search, "Search")
                                    }
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = {
                            when {
                                // Editor open and ADD mode: save via FAB
                                isEditorOpen && editingNote == null -> {
                                    if (editorCanSave) editorSaveAction?.invoke()
                                }
                                // Editor open and EDIT mode: do nothing (unclickable behavior)
                                isEditorOpen && editingNote != null -> {
                                    // intentionally no-op
                                }
                                // Not in editor: open add editor
                                else -> {
                                    editingNote = null
                                    isEditorOpen = true
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Add, "Add note")
                        }
                    }
                ) { innerPadding ->
                    Column(Modifier.padding(innerPadding)) {
                        if (isEditorOpen) {
                            NoteEditor(
                                initialNote = editingNote,
                                onSave = { note ->
                                    if (note.id == 0) {
                                        viewModel.insert(note)
                                    } else {
                                        viewModel.update(note)
                                    }
                                    isEditorOpen = false
                                    editingNote = null
                                },
                                onDelete = { note ->
                                    viewModel.delete(note)
                                    isEditorOpen = false
                                    editingNote = null
                                },
                                onCancel = {
                                    isEditorOpen = false
                                    editingNote = null
                                },
                                onRegisterSave = { save, canSave ->
                                    editorSaveAction = save
                                    editorCanSave = canSave
                                }
                            )
                        } else {
                            NotesListScreen(
                                viewModel = viewModel,
                                modifier = Modifier,
                                onNoteClick = { note ->
                                    editingNote = note
                                    isEditorOpen = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun NotesListScreen(viewModel: NoteViewModel, modifier: Modifier, onNoteClick: (Note) -> Unit) {
    val notesWithTags by viewModel.allNotesWithTags.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (notesWithTags.isEmpty()) {
            item {
                Text(
                    text = "No notes found",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            items(notesWithTags) { note ->
                NoteCard(note = note.note, tags = note.tags, onClick = { onNoteClick(note.note) })
            }
        }
    }
}

@Composable
fun NoteEditor(
    initialNote: Note?,
    onSave: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onCancel: () -> Unit,
    onRegisterSave: (saveAction: () -> Unit, canSave: Boolean) -> Unit
) {
    val isEditing = initialNote != null
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var category by remember { mutableStateOf(initialNote?.category ?: "") }

    // Register/refresh the external Save action (for FAB) whenever inputs change
    fun registerExternalSave() {
        val canSaveNow = title.isNotBlank() || content.isNotBlank()
        val saveLambda = {
            val noteToSave = if (isEditing) {
                initialNote!!.copy(
                    title = title.trim(),
                    content = content.trim(),
                    category = category.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                Note(
                    title = title.trim(),
                    content = content.trim(),
                    category = category.trim()
                )
            }
            onSave(noteToSave)
        }
        onRegisterSave(saveLambda, canSaveNow)
    }

    LaunchedEffect(Unit) { registerExternalSave() }
    LaunchedEffect(title, content, category) { registerExternalSave() }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isEditing) "Edit Note" else "Add Note", style = MaterialTheme.typography.titleLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing) {
                    TextButton(onClick = {
                        initialNote?.let { onDelete(it) }
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Title") }
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Content") },
            minLines = 6
        )

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Category (optional)") }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    // Use the same external save logic as FAB
                    registerExternalSave()
                    onRegisterSave({ /* placeholder */ }, title.isNotBlank() || content.isNotBlank())
                    if (title.isNotBlank() || content.isNotBlank()) {
                        val noteToSave = if (isEditing) {
                            initialNote!!.copy(
                                title = title.trim(),
                                content = content.trim(),
                                category = category.trim(),
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            Note(
                                title = title.trim(),
                                content = content.trim(),
                                category = category.trim()
                            )
                        }
                        onSave(noteToSave)
                    }
                },
                enabled = title.isNotBlank() || content.isNotBlank()
            ) {
                Text(if (isEditing) "Update" else "Save")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: Note,
    tags: List<Tag> = emptyList(),  // NEW: Optional tags list
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Date and Category Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtils.formatDateTime(note.updatedAt),  // Changed to updatedAt
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // NEW: Show category if it exists
                if (note.category.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = note.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Title
            Text(
                text = note.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Content preview
            if (note.content.isNotEmpty()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // NEW: Show tags
            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        TagChip(tag = tag)
                    }
                }
            }
        }
    }
}

// NEW: Composable for displaying a single tag
@Composable
fun TagChip(
    tag: Tag,
    onRemove: (() -> Unit)? = null  // Optional: for removing tags
) {
    Surface(
        color = Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            1.dp,
            Color(android.graphics.Color.parseColor(tag.color))
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color(android.graphics.Color.parseColor(tag.color))
            )

            // Optional X button to remove tag
            onRemove?.let {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { it() },
                    tint = Color(android.graphics.Color.parseColor(tag.color))
                )
            }
        }
    }
}