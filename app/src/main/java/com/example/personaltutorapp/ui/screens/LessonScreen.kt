package com.example.personaltutorapp.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.MediaItem
import androidx.navigation.NavHostController
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    navController: NavHostController,
    viewModel: CourseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    courseId: String
) {
    var lessonsState by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var isLoadingLessons by remember { mutableStateOf(true) }
    var lessonsError by remember { mutableStateOf<String?>(null) }
    
    val currentUser by authViewModel.currentUser.collectAsState()
    val isStudent = currentUser?.let { user ->
        var isStudent by remember { mutableStateOf(true) }
        LaunchedEffect(user.uid) {
            authViewModel.isTutor(user.uid) { isTutor ->
                isStudent = !isTutor
            }
        }
        isStudent
    } ?: true
    
    LaunchedEffect(courseId) {
        try {
            viewModel.getLessonsByCourse(courseId)
                .collect { lessons ->
                    lessonsState = lessons
                    isLoadingLessons = false
                    lessonsError = null
                }
        } catch (e: Exception) {
            Log.e("LessonScreen", "Error collecting lessons: ${e.message}", e)
            isLoadingLessons = false
            lessonsError = "Failed to load lessons: ${e.message}"
        }
    }

    val visibleLessons = if (isStudent) {
        val sortedLessons = lessonsState.sortedBy { it.order }
        val visibleLessonsList = mutableListOf<Lesson>()
        var lastCompletedOrder = 0

        sortedLessons.forEach { lesson ->
            if (lesson.isCompleted) {
                visibleLessonsList.add(lesson)
                if (lesson.order > lastCompletedOrder) {
                    lastCompletedOrder = lesson.order
                }
            }
        }

        sortedLessons.forEach { lesson ->
            val alreadyVisible = visibleLessonsList.any { it.lessonId == lesson.lessonId }
            if (!alreadyVisible) {
                if (lesson.order == 1 || lesson.order == lastCompletedOrder + 1) {
                    visibleLessonsList.add(lesson)
                }
            }
        }
        visibleLessonsList.sortBy { it.order }
        visibleLessonsList.distinctBy { it.lessonId }
    } else {
        lessonsState.sortedBy { it.order }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Lessons") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Course Lessons",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            when {
                isLoadingLessons -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                lessonsError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lessonsError ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                lessonsState.isEmpty() -> {
                    Text(
                        text = "No lessons available",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = visibleLessons,
                            key = { lesson -> lesson.lessonId ?: lesson.hashCode().toString() }
                        ) { lesson ->
                            key(lesson.lessonId) {
                                LessonItem(
                                    lesson = lesson,
                                    isStudent = isStudent,
                                    snackbarHostState = snackbarHostState,
                                    viewModel = viewModel,
                                    onLessonCompleted = { 
                                        // 空实现，由内部处理
                                    }
                                )
                            }
                        }

                        if (isStudent && visibleLessons.size < lessonsState.size) {
                            item {
                                LockedLessonItem(
                                    remainingCount = lessonsState.size - visibleLessons.size
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonItem(
    lesson: Lesson,
    isStudent: Boolean,
    snackbarHostState: SnackbarHostState,
    viewModel: CourseViewModel,
    onLessonCompleted: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isMediaLoading by remember { mutableStateOf(false) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    var isCompleting by remember { mutableStateOf(false) }
    var completionResult by remember(lesson.lessonId) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(completionResult) {
        completionResult?.let { success ->
            if (success) {
                Log.d("LessonScreen", "Lesson ${lesson.lessonId} marked as completed")
                snackbarHostState.showSnackbar("Lesson marked as completed")
            } else {
                Log.e("LessonScreen", "Failed to mark lesson ${lesson.lessonId} as completed")
                snackbarHostState.showSnackbar("Failed to mark lesson as completed")
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = lesson.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            lesson.mediaUrl?.let { url ->
                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isMediaLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (lesson.mediaType == "pdf") 300.dp else if (lesson.mediaType == "mp4") 200.dp else 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = "Loading media...",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    mediaError != null -> {
                        Text(
                            text = mediaError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    else -> {
                        when (lesson.mediaType) {
                            "pdf" -> {
                                isMediaLoading = true
                                AndroidView(
                                    factory = {
                                        WebView(context).apply {
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    coroutineScope.launch {
                                                        isMediaLoading = false
                                                    }
                                                }

                                                override fun onReceivedError(
                                                    view: WebView?,
                                                    request: android.webkit.WebResourceRequest?,
                                                    error: android.webkit.WebResourceError?
                                                ) {
                                                    coroutineScope.launch {
                                                        isMediaLoading = false
                                                        mediaError = "Failed to load PDF. Please check your network."
                                                    }
                                                }
                                            }
                                            @SuppressLint("SetJavaScriptEnabled")
                                            settings.javaScriptEnabled = true
                                            loadUrl("https://docs.google.com/viewer?url=$url")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                )
                            }
                            "mp3", "mp4" -> {
                                isMediaLoading = true
                                val exoPlayer = remember {
                                    ExoPlayer.Builder(context).build().apply {
                                        setMediaItem(MediaItem.fromUri(url))
                                        prepare()
                                        addListener(object : androidx.media3.common.Player.Listener {
                                            override fun onPlaybackStateChanged(playbackState: Int) {
                                                coroutineScope.launch {
                                                    if (playbackState == androidx.media3.common.Player.STATE_READY) {
                                                        isMediaLoading = false
                                                    } else if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                                        // 可选：播放结束时处理
                                                    }
                                                }
                                            }

                                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                                coroutineScope.launch {
                                                    isMediaLoading = false
                                                    mediaError = "Failed to play media. Please check your network."
                                                }
                                            }
                                        })
                                    }
                                }
                                DisposableEffect(Unit) {
                                    onDispose { exoPlayer.release() }
                                }
                                AndroidView(
                                    factory = { PlayerView(context).apply { player = exoPlayer } },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (lesson.mediaType == "mp4") 200.dp else 100.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lesson.isCompleted) "Completed" else "Not Completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lesson.isCompleted) Color(0xFF4CAF50) else Color.Gray
                )
                if (isStudent && !lesson.isCompleted) {
                    Button(
                        onClick = {
                            lesson.lessonId?.let { id ->
                                isCompleting = true
                                viewModel.completeLesson(id) { success ->
                                    isCompleting = false
                                    completionResult = success
                                }
                            }
                        },
                        enabled = lesson.lessonId != null && !isCompleting,
                        modifier = Modifier.semantics { contentDescription = "Mark lesson as completed" }
                    ) {
                        if (isCompleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isCompleting) "Marking..." else "Mark as Completed")
                    }
                }
            }
        }
    }
}

@Composable
fun LockedLessonItem(remainingCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked lessons",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 8.dp)
            )
            Text(
                text = "$remainingCount more ${if (remainingCount == 1) "lesson" else "lessons"} locked",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                text = "Complete the current lessons to unlock more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}