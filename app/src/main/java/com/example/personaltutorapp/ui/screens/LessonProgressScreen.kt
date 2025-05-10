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
import androidx.navigation.NavController
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@Composable
fun LessonProgressScreen(navController: NavController, viewModel: CourseViewModel, courseId: Int) {
    var courseWithLessons by remember { mutableStateOf<Pair<Any?, List<Lesson>>?>(null) }
    var courseProgress by remember { mutableStateOf(0) }
    
    LaunchedEffect(courseId) {
        viewModel.getCourseWithLessons(courseId).collect { result ->
            courseWithLessons = result
        }
        
        viewModel.getCourseProgress(courseId).collect { progress ->
            courseProgress = progress
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Course title and progress
        courseWithLessons?.first?.let { course ->
            Text(
                text = "Course: ${(course as? com.example.personaltutorapp.model.Course)?.title ?: ""}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
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
        }
        
        // Lessons list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            courseWithLessons?.second?.let { lessons ->
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
            } ?: item {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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
fun LessonProgressItem(lesson: Lesson, navController: NavController, courseId: Int) {
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
            if (lesson.isCompleted) {
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
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            
            // Completion status
            Text(
                text = if (lesson.isCompleted) "Completed" else "Not Completed",
                style = MaterialTheme.typography.bodyMedium,
                color = if (lesson.isCompleted) Color.Green else Color.Gray
            )
        }
    }
}