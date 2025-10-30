package ph.edu.comteq.notetakingapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDao: NoteDao = AppDatabase.getDatabase(application.applicationContext).noteDao()

    // Track what the user is searching for
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // NEW: Category filter
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Smart notes: shows all notes OR search results
    val allNotes: Flow<List<Note>> = searchQuery.flatMapLatest { query ->
        val category = _selectedCategory.value

        when {
            // Both search and category
            query.isNotBlank() && category != null -> {
                // We'll need to add this query if you want both filters
                noteDao.searchNotes(query)  // For now, just search
            }
            // Just search
            query.isNotBlank() -> noteDao.searchNotes(query)
            // Just category
            category != null -> noteDao.getNotesByCategory(category)
            // No filters - show everything
            else -> noteDao.getAllNotes()
        }
    }

    // NEW: All notes WITH their tags - supports search
    val allNotesWithTags: Flow<List<NoteWithTags>> = searchQuery.flatMapLatest { query ->
        when {
            query.isNotBlank() -> noteDao.searchNotesWithTags(query)
            else -> noteDao.getAllNotesWithTags()
        }
    }

    // NEW: All available categories
    val allCategories: Flow<List<String>> = noteDao.getAllCategories()

    // NEW: All available tags
    val allTags: Flow<List<Tag>> = noteDao.getAllTags()

    // Call this when user types in search box
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Call this to clear the search
    fun clearSearch() {
        _searchQuery.value = ""
    }

    // NEW: Filter by category
    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun clearCategoryFilter() {
        _selectedCategory.value = null
    }

    fun insert(note: Note) = viewModelScope.launch {
        noteDao.insertNote(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        // Update the updatedAt timestamp
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        noteDao.updateNote(updatedNote)
    }

    fun delete(note: Note) = viewModelScope.launch {
        noteDao.deleteNote(note)
    }

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun getNoteWithTags(noteId: Int): NoteWithTags? {
        return noteDao.getNoteWithTags(noteId)
    }

    // ==================== TAG FUNCTIONS ====================

    fun insertTag(tag: Tag) = viewModelScope.launch {
        noteDao.insertTag(tag)
    }

    fun updateTag(tag: Tag) = viewModelScope.launch {
        noteDao.updateTag(tag)
    }

    fun deleteTag(tag: Tag) = viewModelScope.launch {
        noteDao.deleteTag(tag)
    }

    // ==================== NOTE-TAG RELATIONSHIP FUNCTIONS ====================

    // Add a tag to a note
    fun addTagToNote(noteId: Int, tagId: Int) = viewModelScope.launch {
        noteDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    // Remove a tag from a note
    fun removeTagFromNote(noteId: Int, tagId: Int) = viewModelScope.launch {
        noteDao.deleteNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
    }

    // Get all notes that have a specific tag
    fun getNotesWithTag(tagId: Int): Flow<List<Note>> {
        return noteDao.getNotesWithTag(tagId)
    }
}