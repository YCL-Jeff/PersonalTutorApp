//LessonScreen.kt
package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import androidx.compose.runtime.getValue // 修正為 getValue

@Composable
fun LessonScreen(viewModel: CourseViewModel, courseId: Int, lessonId: Int, navController: NavController) {
    val lessons by viewModel.getLessonsByCourse(courseId).collectAsState(initial = emptyList())
    val currentLesson = lessons.find { it.lessonId == lessonId }
    val previousLessonsCompleted = lessons.filter { it.order < (currentLesson?.order ?: 0) }.all { it.isCompleted }

    if (currentLesson != null && previousLessonsCompleted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = "Lesson: ${currentLesson.title}", style = MaterialTheme.typography.headlineMedium)
            Button(
                onClick = {
                    viewModel.completeLesson(currentLesson.lessonId) { success ->
                        if (success) {
                            navController.navigate("lessonProgress/$courseId")
                        }
                    }
                },
                enabled = !currentLesson.isCompleted
            ) {
                Text(text = "Mark as Completed")
            }
        }
    }
}