package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@Composable
fun LessonCreationScreen(navController: NavController, viewModel: CourseViewModel, courseId: Int) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Create Lesson", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        Spacer(modifier = Modifier.height(8.dp))

        TextField(value = content, onValueChange = { content = it }, label = { Text("Content") })
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                isLoading = true
                viewModel.createLesson(courseId, title, content) { success ->
                    isLoading = false
                    if (success) {
                        navController.popBackStack() // 回到上一頁
                    } else {
                        errorMessage = "Failed to create lesson"
                    }
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Create Lesson")
            }
        }
    }
}
