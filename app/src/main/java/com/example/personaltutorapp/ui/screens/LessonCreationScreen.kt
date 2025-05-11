package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCreationScreen(
    navController: NavController,
    viewModel: CourseViewModel,
    courseId: String
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var order by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Create Lesson", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = order,
            onValueChange = { order = it },
            label = { Text("Order (e.g., 1, 2, 3)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (title.isBlank() || content.isBlank() || order.isBlank()) {
                    errorMessage = "All fields are required"
                    return@Button
                }
                val orderInt = order.toIntOrNull()
                if (orderInt == null) {
                    errorMessage = "Order must be a valid number"
                    return@Button
                }
                isLoading = true
                errorMessage = null
                viewModel.createLesson(
                    courseId = courseId,
                    title = title,
                    content = content,
                    order = orderInt,
                    onResult = { success: Boolean ->
                        isLoading = false
                        if (success) {
                            navController.popBackStack()
                        } else {
                            errorMessage = "Failed to create lesson"
                        }
                    }
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Create Lesson")
            }
        }
    }
}