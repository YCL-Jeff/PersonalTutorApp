package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.example.personaltutorapp.ui.viewmodel.LessonWithProgress

@Composable
fun LessonProgressScreen(navController: NavController, viewModel: CourseViewModel, courseId: Int) {
    val courseProgress by viewModel.getStudentCourseProgress(courseId).collectAsState(initial = null)
    val lessonsWithProgress by viewModel.getLessonsWithProgress(courseId).collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 課程進度摘要
        courseProgress?.let { progress ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Course Progress",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.percentage / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${progress.completedLessons}/${progress.totalLessons} lessons completed (${progress.percentage}%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // 課節列表
        Text(
            text = "Lessons",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        LazyColumn {
            items(lessonsWithProgress) { lessonWithProgress ->
                LessonProgressItem(
                    lessonWithProgress = lessonWithProgress,
                    onClick = {
                        navController.navigate("lesson/${courseId}/${lessonWithProgress.lesson.lessonId}")
                    }
                )
            }
        }
    }
}

@Composable
fun LessonProgressItem(lessonWithProgress: LessonWithProgress, onClick: () -> Unit) {
    val lesson = lessonWithProgress.lesson
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lessonWithProgress.isCompleted) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (lessonWithProgress.isLocked) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Circle,
                    contentDescription = "Not completed",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (lessonWithProgress.isCompleted) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (lessonWithProgress.isLocked) {
                    Text(
                        text = "Locked - Complete previous lessons first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
