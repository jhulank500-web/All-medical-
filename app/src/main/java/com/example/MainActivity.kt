package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ChatMessage
import com.example.data.model.Note
import com.example.ui.WorkspaceViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    Dashboard,
    Chat,
    Notes
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    WorkspaceAppContainer(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun WorkspaceAppContainer(
    modifier: Modifier = Modifier,
    viewModel: WorkspaceViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B)  // Slate 800
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentScreen) {
                    AppScreen.Dashboard -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { currentScreen = AppScreen.Chat },
                        onNavigateToNotes = { currentScreen = AppScreen.Notes }
                    )
                    AppScreen.Chat -> ChatScreen(viewModel = viewModel)
                    AppScreen.Notes -> NotesScreen(viewModel = viewModel)
                }
            }

            // Bottom Navigation Bar
            NavigationBar(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .testTag("navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.Dashboard,
                    onClick = { currentScreen = AppScreen.Dashboard },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.Dashboard) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF22D3EE), // Cyan 400
                        selectedTextColor = Color(0xFF22D3EE),
                        unselectedIconColor = Color(0xFF94A3B8), // Slate 400
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.Chat,
                    onClick = { currentScreen = AppScreen.Chat },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.Chat) Icons.Filled.Forum else Icons.Outlined.Forum,
                            contentDescription = "Assistant Chat"
                        )
                    },
                    label = { Text("AI Assistant", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFC084FC), // Purple 400
                        selectedTextColor = Color(0xFFC084FC),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("nav_chat")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.Notes,
                    onClick = { currentScreen = AppScreen.Notes },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.Notes) Icons.Filled.StickyNote2 else Icons.Outlined.StickyNote2,
                            contentDescription = "Notes Hub"
                        )
                    },
                    label = { Text("Notes Hub", fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF34D399), // Emerald 400
                        selectedTextColor = Color(0xFF34D399),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("nav_notes")
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: DASHBOARD SCREEN
// ==========================================

@Composable
fun DashboardScreen(
    viewModel: WorkspaceViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val quote by viewModel.dailyQuote.collectAsStateWithLifecycle()
    val isQuoteLoading by viewModel.isQuoteLoading.collectAsStateWithLifecycle()

    val formattedTime = remember {
        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header with Dynamic Date & Hour
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp
                )
                Text(
                    text = "Aura Workspace",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF22D3EE),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Daily AI Quote / Inspiration Card (with beautiful modern gradient box)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspiration_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4C1D95).copy(alpha = 0.6f), // Deep Violet
                                    Color(0xFF1E1B4B).copy(alpha = 0.8f)  // Dark Blue-Indigo
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Inspiration",
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI DAILY REFLECTION",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA78BFA),
                                    letterSpacing = 1.5.sp
                                )
                            }

                            IconButton(
                                onClick = { viewModel.generateDailyInspiration() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("refresh_inspiration_button")
                            ) {
                                if (isQuoteLoading) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFA78BFA),
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Refresh Quote",
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = quote,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        // Stats Card Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Notes count card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onNavigateToNotes() }
                        .testTag("stats_notes_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = "Notes Count",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "${notes.size} Notes",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Workspace Docs",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // AI Summaries status card
                val summaryCount = notes.count { it.aiSummary != null }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onNavigateToNotes() }
                        .testTag("stats_ai_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesomeMotion,
                                contentDescription = "AI Summaries",
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "$summaryCount Summarized",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "AI Optimized",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Launch Actions Section
        item {
            Text(
                text = "Quick Services",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToChat() }
                    .testTag("quick_chat_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFC084FC).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = "Chat icon",
                            tint = Color(0xFFC084FC)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Consult Aura AI",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Ask questions, brainstorming, or debug code",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Go",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNotes() }
                    .testTag("quick_note_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF34D399).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCard,
                            contentDescription = "Notes icon",
                            tint = Color(0xFF34D399)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Smart Notes",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Log ideas & let AI structure or summarize them",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Go",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: CHAT WITH GEMINI ASSISTANT
// ==========================================

@Composable
fun ChatScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Automatically scroll to bottom when a new message is added
    LaunchedEffect(messages.size, isChatLoading) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFFC084FC))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = "Aura AI",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Aura Assistant",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isChatLoading) "Aura is thinking..." else "Powered by Gemini 3.5 Flash",
                        color = if (isChatLoading) Color(0xFFC084FC) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isChatLoading) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Row {
                if (isTtsSpeaking) {
                    IconButton(
                        onClick = { viewModel.stopSpeaking() },
                        modifier = Modifier.testTag("stop_speech_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeOff,
                            contentDescription = "Stop Speech",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.clearChatHistory() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear History",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Divider(color = Color(0xFF334155), thickness = 1.dp)

        // Chat Log
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) {
                // Warm Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forum,
                        contentDescription = "Empty chat",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Initiate Conversation",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Aura AI is ready. You can ask for assistance, help write note drafts, or code solutions.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Suggestion Chips
                    Text(
                        text = "Suggested Questions:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val suggestions = listOf(
                        "Draft a product launch checklist",
                        "How can I stay focused working from home?",
                        "Suggest 5 features for a custom database app"
                    )

                    suggestions.forEach { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    textInput = suggestion
                                }
                                .testTag("suggestion_pill_${suggestion.take(10)}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = suggestion,
                                color = Color(0xFFC084FC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        val isUser = message.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF8B5CF6), CircleShape)
                                        .align(Alignment.Top),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SmartToy,
                                        contentDescription = "Aura Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) Color(0xFF4F46E5) else Color(0xFF1E293B)
                                ),
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .testTag("chat_message_bubble_${message.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = message.message,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )

                                    // Toolbar for AI replies (speak, copy)
                                    if (!isUser) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.speak(message.message)
                                                    Toast.makeText(context, "Speaking message...", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.VolumeUp,
                                                    contentDescription = "Speak",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(message.message))
                                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = "Copy",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (isUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                        .align(Alignment.Top),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "User Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Floating Loading Bubble
                    if (isChatLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF8B5CF6), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SmartToy,
                                        contentDescription = "Aura Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.widthIn(max = 120.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFFC084FC),
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                        Text(
                                            text = "Aura typing...",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Send Message Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Aura anything...", color = Color(0xFF64748B)) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .testTag("chat_input_field"),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }
                    }
                )
            )

            FloatingActionButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendMessage(textInput)
                        textInput = ""
                    }
                },
                containerColor = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// SCREEN 3: NOTES ORGANIZER HUB
// ==========================================

@Composable
fun NotesScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val isNoteActionLoading by viewModel.isNoteActionLoading.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedNoteForDetail by remember { mutableStateOf<Note?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "Idea", "Task", "Work", "Personal")

    // Filtered Notes
    val filteredNotes = remember(notes, searchQuery, selectedCategoryFilter) {
        notes.filter { note ->
            val matchesSearch = note.title.contains(searchQuery, ignoreCase = true) || 
                                note.content.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "All" || note.category == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Notes Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Notes Organizer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Store logs & optimize with Gemini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF34D399)
                )
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF34D399),
                contentColor = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(40.dp)
                    .widthIn(min = 100.dp)
                    .testTag("add_note_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
                    Text("Add Note", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search your notes...", color = Color(0xFF64748B)) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search", tint = Color(0xFF64748B)) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .testTag("notes_search_bar")
        )

        // Horizontal Category Filter Scroller
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategoryFilter == category
                val backgroundCol by animateColorAsState(if (isSelected) Color(0xFF34D399) else Color(0xFF1E293B))
                val textCol by animateColorAsState(if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundCol)
                        .border(1.dp, if (isSelected) Color(0xFF34D399) else Color(0xFF334155), RoundedCornerShape(20.dp))
                        .clickable { selectedCategoryFilter = category }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("category_filter_$category"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        color = textCol,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Notes List/Grid
        if (filteredNotes.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.StickyNote2,
                    contentDescription = "No notes",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No notes found",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = if (searchQuery.isNotEmpty()) "Try modifying your search filter" else "Create a new note to start capturing ideas",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1), // Simple and ultra clean
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotes) { note ->
                    val isCurrentActionRunning = isNoteActionLoading == note.id

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedNoteForDetail = note }
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                            .testTag("note_card_${note.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Category and Timestamp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(getCategoryColor(note.category).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = note.category.uppercase(Locale.getDefault()),
                                        color = getCategoryColor(note.category),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                Text(
                                    text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(note.timestamp)),
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Title & Content
                            Text(
                                text = note.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = note.content,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )

                            // AI Summary section (if generated)
                            if (note.aiSummary != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF22D3EE).copy(alpha = 0.08f))
                                        .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.AutoAwesome,
                                                contentDescription = "AI Summary",
                                                tint = Color(0xFF22D3EE),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "GEMINI SUMMARY",
                                                color = Color(0xFF22D3EE),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.aiSummary,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            // Quick AI Actions Panel
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFF334155), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Summarize button
                                    Button(
                                        onClick = { viewModel.summarizeNote(note) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF22D3EE).copy(alpha = 0.15f),
                                            contentColor = Color(0xFF22D3EE)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("summarize_note_${note.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isCurrentActionRunning) {
                                                CircularProgressIndicator(
                                                    color = Color(0xFF22D3EE),
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 1.5.dp
                                                )
                                            } else {
                                                Icon(imageVector = Icons.Filled.Compress, contentDescription = "Summarize", modifier = Modifier.size(12.dp))
                                            }
                                            Text("Summarize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Improve content button
                                    Button(
                                        onClick = { viewModel.improveNoteContent(note) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFC084FC).copy(alpha = 0.15f),
                                            contentColor = Color(0xFFC084FC)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("improve_note_${note.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.EditNote, contentDescription = "Refine", modifier = Modifier.size(14.dp))
                                            Text("Refine Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteNote(note.id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("delete_note_${note.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Create/Add Note Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("Idea") }
        val sheetCategories = listOf("Idea", "Task", "Work", "Personal")
        val isTitleGenerating = isNoteActionLoading == -1

        AlertDialog(
            onDismissRequest = { if (!isTitleGenerating) showAddDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Create Smart Note", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Category:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sheetCategories.forEach { category ->
                            val selected = newCategory == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) getCategoryColor(category) else Color(0xFF334155))
                                    .clickable { newCategory = category }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    color = if (selected) Color(0xFF0F172A) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title (Optional)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("AI can generate a title if left blank", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_note_title_input")
                    )

                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Note Content", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("Write your thoughts, guidelines, or logs here...", color = Color(0xFF64748B)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        minLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_note_content_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContent.isNotBlank()) {
                            viewModel.createNote(newTitle, newContent, newCategory)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = newContent.isNotBlank() && !isTitleGenerating,
                    modifier = Modifier.testTag("save_note_confirm_button")
                ) {
                    if (isTitleGenerating) {
                        CircularProgressIndicator(color = Color(0xFF0F172A), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save Note", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    enabled = !isTitleGenerating
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
        )
    }

    // Modal: View Note Detail Dialog
    selectedNoteForDetail?.let { note ->
        AlertDialog(
            onDismissRequest = { selectedNoteForDetail = null },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(note.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(getCategoryColor(note.category).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = note.category.uppercase(Locale.getDefault()),
                            color = getCategoryColor(note.category),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = note.content,
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }

                    if (note.aiSummary != null) {
                        item {
                            Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF22D3EE).copy(alpha = 0.08f))
                                    .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = "AI Summary",
                                            tint = Color(0xFF22D3EE),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "GEMINI SUMMARY",
                                            color = Color(0xFF22D3EE),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.aiSummary,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedNoteForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399), contentColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .testTag("note_detail_dialog")
        )
    }
}

// Category Color Helper
fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.getDefault())) {
        "idea" -> Color(0xFFFBBF24)     // Amber 400
        "task" -> Color(0xFFF43F5E)     // Rose 500
        "work" -> Color(0xFF38BDF8)     // Sky 400
        "personal" -> Color(0xFFC084FC) // Purple 400
        else -> Color(0xFF94A3B8)       // Slate 400
    }
}
