package com.example.personaltutorapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    viewModel: CourseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    courseId: String,
    lessonId: String,
    navController: NavController
) {
    val lessons by viewModel.getLessonsByCourse(courseId).collectAsState(initial = emptyList())
    val currentLesson = lessons.find { it.lessonId == lessonId }
    val currentUser by authViewModel.currentUser.collectAsState()
    val studentId = currentUser?.uid
    val isLoading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 获取下一课信息
    val sortedLessons = lessons.sortedBy { it.order }
    val currentLessonIndex = sortedLessons.indexOfFirst { it.lessonId == lessonId }
    val hasNextLesson = currentLessonIndex >= 0 && currentLessonIndex < sortedLessons.size - 1
    val nextLessonId = if (hasNextLesson) sortedLessons[currentLessonIndex + 1].lessonId else null

    // 完成课程后的导航逻辑
    val completeAndNavigate = {
        if (studentId != null) {
            isLoading.value = true
            scope.launch {
                try {
                    var success = false
                    viewModel.completeLesson(lessonId) { result ->
                        success = result
                    }
                    delay(500) // 短暂延迟确保 Firebase 操作完成
                    isLoading.value = false
                    if (success && hasNextLesson && nextLessonId != null) {
                        navController.navigate("lesson/$courseId/$nextLessonId") {
                            popUpTo("lesson/$courseId/$lessonId") { inclusive = true }
                        }
                    } else if (success) {
                        navController.popBackStack()
                    }
                } catch (e: Exception) {
                    Log.e("LessonScreen", "Error: ${e.message}")
                    isLoading.value = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentLesson?.title ?: "Lesson") },
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
            if (currentLesson != null) {
                Text(
                    text = currentLesson.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val isCompleted = currentLesson.isCompleted
                if (isCompleted) {
                    if (hasNextLesson && nextLessonId != null) {
                        Button(
                            onClick = {
                                navController.navigate("lesson/$courseId/$nextLessonId") {
                                    popUpTo("lesson/$courseId/$lessonId") { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Next Lesson")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Lessons")
                        }
                    }
                } else {
                    Button(
                        onClick = { completeAndNavigate() },
                        enabled = !isLoading.value,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isLoading.value) "Processing..." else "Finish Lesson")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lesson not found",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}