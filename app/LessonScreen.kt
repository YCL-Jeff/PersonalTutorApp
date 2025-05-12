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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    navController: NavHostController,
    viewModel: CourseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    courseId: String
) {
    val lessons by viewModel.getLessonsByCourse(courseId).collectAsState(initial = emptyList())
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

    Scaffold(
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
            if (lessons.isEmpty()) {
                Text(
                    text = "No lessons available",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = lessons.sortedBy { it.order },
                        key = { lesson -> lesson.lessonId ?: lesson.hashCode().toString() }
                    ) { lesson ->
                        LessonItem(
                            lesson = lesson,
                            isStudent = isStudent,
                            viewModel = viewModel,
                            onLessonCompleted = { success ->
                                if (success) {
                                    Log.d("LessonScreen", "Lesson ${lesson.lessonId} marked as completed")
                                } else {
                                    Log.e("LessonScreen", "Failed to mark lesson ${lesson.lessonId} as completed")
                                }
                            }
                        )
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
    viewModel: CourseViewModel,
    onLessonCompleted: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* 可添加點擊邏輯，例如查看課程內容 */ },
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

            // 显示多媒体内容
            lesson.mediaUrl?.let { url ->
                Spacer(modifier = Modifier.height(16.dp))
                when (lesson.mediaType) {
                    "pdf" -> {
                        AndroidView(
                            factory = {
                                WebView(context).apply {
                                    webViewClient = WebViewClient()
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
                        val exoPlayer = remember {
                            ExoPlayer.Builder(context).build().apply {
                                setMediaItem(MediaItem.fromUri(url))
                                prepare()
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
                                viewModel.completeLesson(id) { success ->
                                    onLessonCompleted(success)
                                }
                            }
                        },
                        enabled = lesson.lessonId != null,
                        modifier = Modifier.semantics { contentDescription = "Mark lesson as completed" }
                    ) {
                        Text("Mark as Completed")
                    }
                }
            }
        }
    }
}