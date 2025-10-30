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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ph.edu.comteq.notetakingapp.ui.theme.NoteTakingAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteTakingAppTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "notes_list"
                ) {
                    composable("notes_list") {
                        NotesListScreenWithSearch(
                            viewModel = viewModel,
                            onAddNote = {
                                navController.navigate("note_edit/new")
                            },
                            onEditNote = { noteId ->
                                navController.navigate("note_edit/$noteId")
                            }
                        )
                    }

                    composable(
                        route = "note_edit/{noteId}",
                        arguments = listOf(
                            navArgument("noteId") {
                                type = NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val noteIdString = backStackEntry.arguments?.getString("noteId")
                        val noteId = if (noteIdString == "new") null else noteIdString?.toIntOrNull()

                        NoteEditScreen(
                            noteId = noteId,
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesListScreenWithSearch(
    viewModel: NoteViewModel,
    onAddNote: () -> Unit,
    onEditNote: (Int) -> Unit
){
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val allNotes by viewModel.allNotesWithTags.collectAsState(initial = emptyList())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if(isSearchActive){
                SearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                            },
                            onSearch = {},
                            expanded = true,
                            onExpandedChange = { shouldExpand ->
                                if (!shouldExpand) {
                                    isSearchActive = false
                                    searchQuery = ""
                                }
                            },
                            placeholder = {Text("Search notes...")},
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
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
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            }
                        )
                    },
                    expanded = true,
                    onExpandedChange = { shouldExpand ->
                        if (!shouldExpand) {
                            isSearchActive = false
                            searchQuery = ""
                        }
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        val searchResults = if (searchQuery.isEmpty()) {
                            allNotes
                        } else {
                            allNotes.filter { noteWithTags ->
                                noteWithTags.note.title.contains(searchQuery, ignoreCase = true) ||
                                        noteWithTags.note.content.contains(searchQuery, ignoreCase = true) ||
                                        noteWithTags.note.category.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        if (searchResults.isEmpty()) {
                            item {
                                Text(
                                    text = if (searchQuery.isEmpty()) "No notes found" else "No results for \"$searchQuery\"",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            items(searchResults) { note ->
                                NoteCard(
                                    note = note.note,
                                    tags = note.tags,
                                    onClick = {
                                        onEditNote(note.note.id)
                                        isSearchActive = false
                                        searchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = { Text("Notes") },
                    actions = {
                        IconButton(onClick = {isSearchActive = true}) {
                            Icon(Icons.Filled.Search, "Search")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSearchActive) {
                FloatingActionButton(onClick = onAddNote) {
                    Icon(Icons.Filled.Add, "Add Note")
                }
            }
        }
    ) { innerPadding ->
        if (!isSearchActive) {
            NotesListScreen(
                notes = allNotes,
                modifier = Modifier.padding(innerPadding),
                onNoteClick = onEditNote
            )
        }
    }
}

@Composable
fun NotesListScreen(
    notes: List<NoteWithTags>,
    modifier: Modifier,
    onNoteClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                NoteCard(
                    note = note.note,
                    tags = note.tags,
                    onClick = { onNoteClick(note.note.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: Note,
    tags: List<Tag> = emptyList(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtils.formatDateTime(note.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (note.category.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(top = 8.dp, end = 8.dp)
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

            Text(
                text = note.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

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

@Composable
fun TagChip(
    tag: Tag,
    onRemove: (() -> Unit)? = null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Int?,
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var createdAt by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(noteId) {
        noteId?.let { id ->
            val note = viewModel.getNoteById(id)
            note?.let {
                title = it.title
                content = it.content
                category = it.category
                createdAt = it.createdAt
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 10
            )

            androidx.compose.material3.Button(
                onClick = {
                    if (noteId == null) {
                        viewModel.insert(
                            Note(
                                title = title,
                                content = content,
                                category = category,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        viewModel.update(
                            Note(
                                id = noteId,
                                title = title,
                                content = content,
                                category = category,
                                createdAt = createdAt,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}