package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Note
import com.example.data.repository.WorkspaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class WorkspaceViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repository = WorkspaceRepository(db.noteDao(), db.chatMessageDao())

    val notes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI States
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _isNoteActionLoading = MutableStateFlow<Int?>(null) // Note ID currently being processed by AI
    val isNoteActionLoading: StateFlow<Int?> = _isNoteActionLoading.asStateFlow()

    private val _dailyQuote = MutableStateFlow("Tap 'Refresh' to generate your personalized AI daily inspiration.")
    val dailyQuote: StateFlow<String> = _dailyQuote.asStateFlow()

    private val _isQuoteLoading = MutableStateFlow(false)
    val isQuoteLoading: StateFlow<Boolean> = _isQuoteLoading.asStateFlow()

    // Text To Speech
    private var tts: TextToSpeech? = null
    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(application, this)
        generateDailyInspiration()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        } else {
            Log.e("WorkspaceViewModel", "TTS initialization failed")
        }
    }

    fun speak(text: String) {
        if (text.isNotEmpty()) {
            _isTtsSpeaking.value = true
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WORKSPACE_TTS")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isTtsSpeaking.value = false
    }

    // AI Daily Inspiration
    fun generateDailyInspiration() {
        viewModelScope.launch {
            _isQuoteLoading.value = true
            val prompt = "Generate a short, powerful, inspiring productivity quote or reflection (1-2 sentences maximum) designed to motivate a developer/creative workspace. Do not include quotes around the text. Just output the text itself."
            val systemMsg = "You are an inspiring and elegant virtual workspace assistant."
            val response = repository.generateAiResponse(prompt, systemMsg)
            _dailyQuote.value = response
            _isQuoteLoading.value = false
        }
    }

    // Chat Actions
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            // Save User message
            val userMsg = ChatMessage(sender = "user", message = text)
            repository.insertMessage(userMsg)

            _isChatLoading.value = true

            // Fetch the chat history to feed to Gemini
            val currentHistory = chatMessages.value

            val systemMsg = "You are Aura, a brilliant, helpful personal workspace assistant. Your answers should be clear, elegant, markdown-formatted where appropriate, concise, and highly professional. Do not include unsolicited extra items."
            val reply = repository.generateAiResponse(text, systemMsg, currentHistory)

            // Save AI reply
            val aiMsg = ChatMessage(sender = "gemini", message = reply)
            repository.insertMessage(aiMsg)

            _isChatLoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Note Actions
    fun createNote(title: String, content: String, category: String) {
        viewModelScope.launch {
            var finalTitle = title.trim()
            if (finalTitle.isEmpty() && content.isNotBlank()) {
                // Generate a quick smart title
                _isNoteActionLoading.value = -1 // -1 represents new unsaved note title gen
                val prompt = "Create a very short, clean title (3-5 words maximum) summarizing the following content. Do not include quotes or surrounding punctuation:\n\n$content"
                finalTitle = repository.generateAiResponse(prompt, "You are a professional editor.").trim()
                if (finalTitle.startsWith("Error:") || finalTitle.isEmpty()) {
                    finalTitle = "Untitled Note"
                }
                _isNoteActionLoading.value = null
            } else if (finalTitle.isEmpty()) {
                finalTitle = "Untitled Note"
            }
            
            val note = Note(title = finalTitle, content = content, category = category)
            repository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    fun summarizeNote(note: Note) {
        viewModelScope.launch {
            _isNoteActionLoading.value = note.id
            val prompt = "Provide a concise, highly polished 1-2 sentence executive summary of the following note content:\n\n${note.content}"
            val response = repository.generateAiResponse(prompt, "You are an expert executive summarizer. Write clearly, directly, and elegantly.")
            if (!response.startsWith("Error:")) {
                repository.updateNote(note.copy(aiSummary = response))
            }
            _isNoteActionLoading.value = null
        }
    }

    fun improveNoteContent(note: Note) {
        viewModelScope.launch {
            _isNoteActionLoading.value = note.id
            val prompt = "Improve the clarity, grammar, and organization of the following note content while preserving its original meaning and facts. Keep it clean and readable:\n\n${note.content}"
            val response = repository.generateAiResponse(prompt, "You are a professional copywriter. Output ONLY the improved text directly.")
            if (!response.startsWith("Error:")) {
                repository.updateNote(note.copy(content = response))
            }
            _isNoteActionLoading.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}
