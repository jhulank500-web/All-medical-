package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.local.ChatMessageDao
import com.example.data.local.NoteDao
import com.example.data.model.ChatMessage
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkspaceRepository(
    private val noteDao: NoteDao,
    private val chatMessageDao: ChatMessageDao
) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()

    suspend fun insertNote(note: Note): Long = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNoteById(id: Int) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    suspend fun insertMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        chatMessageDao.insertMessage(message)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatMessageDao.clearHistory()
    }

    suspend fun generateAiResponse(
        prompt: String,
        systemInstruction: String? = null,
        chatHistory: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing. Please set your GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        // Build contents list
        val contents = mutableListOf<Content>()
        
        // Add historic messages (role must be "user" or "model" as expected by Gemini API)
        for (msg in chatHistory) {
            val roleName = if (msg.sender == "user") "user" else "model"
            contents.add(Content(role = roleName, parts = listOf(Part(text = msg.message))))
        }
        
        // Add the current prompt
        contents.add(Content(role = "user", parts = listOf(Part(text = prompt))))

        val sysInstructionContent = systemInstruction?.let {
            Content(parts = listOf(Part(text = it)))
        }

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = sysInstructionContent
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "I couldn't generate a response. Please try again."
        } catch (e: Exception) {
            "Error: ${e.message ?: "Unknown error"}"
        }
    }
}
