package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonProgressScreen(
    navController: NavController,
    viewModel: CourseViewModel = hiltViewModel(),
    courseId: String
) {
    val lessons by viewModel.getLessonsByCourse(courseId).collectAsState(initial = emptyList())
    val courseProgress by viewModel.getCourseProgress(courseId).collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Course progress
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            LinearProgressIndicator(
                progress = { courseProgress / 100f },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
            )
            Text(
                text = "$courseProgress%",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Lessons list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (lessons.isEmpty()) {
                item {
                    Text(
                        text = "No lessons available",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                items(lessons) { lesson ->
                    LessonProgressItem(
                        lesson = lesson,
                        navController = navController,
                        courseId = courseId
                    )
                }
            }
        }

        // Back button
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
fun LessonProgressItem(
    lesson: Lesson,
    navController: NavController,
    courseId: String
) {
    var isLessonCompleted by remember { mutableStateOf(false) }
    val viewModel = hiltViewModel<CourseViewModel>()
    
    // Check if lesson is completed by user
    LaunchedEffect(lesson.lessonId) {
        lesson.lessonId?.let { lessonId ->
            val userLessons = viewModel.firestore.collection("userLessons")
                .whereEqualTo("userId", viewModel.getCurrentUserId())
                .whereEqualTo("lessonId", lessonId)
                .whereEqualTo("completed", true)
                .get()
                .await()
            
            isLessonCompleted = !userLessons.isEmpty
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            if (isLessonCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Not Completed",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Lesson title
            Text(
                text = "Lesson Content", 
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )

            // Completion status
            Text(
                text = if (isLessonCompleted) "Completed" else "Not Completed",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLessonCompleted) Color.Green else Color.Gray
            )
        }
    }
}