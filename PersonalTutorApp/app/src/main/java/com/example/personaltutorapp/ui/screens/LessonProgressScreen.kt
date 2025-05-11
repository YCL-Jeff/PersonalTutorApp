package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.getValue
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@Composable
fun LessonProgressScreen(viewModel: CourseViewModel, courseId: Int, navController: NavController) {
    val lessons by viewModel.getLessonsByCourse(courseId).collectAsState(initial = emptyList())
    val completedCount = lessons.count { it.isCompleted }
    val totalCount = lessons.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Progress: $completedCount / $totalCount Lessons Completed",
            style = MaterialTheme.typography.headlineMedium
        )
        LazyColumn {
            items(lessons) { lesson ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = lesson.title)
                    Text(text = if (lesson.isCompleted) "Completed" else "Pending")
                }
            }
        }
        Button(onClick = { navController.popBackStack() }) {
            Text(text = "Back to Course")
        }
    }
}