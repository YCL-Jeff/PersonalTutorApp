package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import androidx.compose.runtime.getValue

@Composable
fun DashboardScreen(navController: NavController, viewModel: CourseViewModel) {
    val courses by viewModel.courses.collectAsState(initial = emptyList())
    val progress by viewModel.getCourseProgress(1).collectAsState(initial = emptyMap()) // 假設 courseId = 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Tutor Dashboard", style = MaterialTheme.typography.headlineLarge)
        LazyColumn {
            items(courses) { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Course: ${course.title}")
                        val enrolledStudents by viewModel.getEnrolledStudents(course.courseId).collectAsState(initial = emptyList())
                        Text(text = "Enrolled Students: ${enrolledStudents.size}")
                        val totalLessons = 10 // 假設每門課程有 10 個課程
                        val progressValue = progress[course.tutorId]?.toFloat()?.div(totalLessons) ?: 0f
                        LinearProgressIndicator(progress = { progressValue }) // 使用新 API
                    }
                }
            }
        }
    }
}