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
import androidx.compose.runtime.setValue

@Composable
fun CourseListScreen(viewModel: CourseViewModel, navController: NavController, isStudent: Boolean = true) {
    var query by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    val courses by viewModel.filterCourses(query, subject).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search by Name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(courses) { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = {
                        if (isStudent) {
                            navController.navigate("lessonProgress/${course.courseId}")
                        } else {
                            navController.navigate("dashboard")
                        }
                    }
                ) {
                    Text(
                        text = course.title,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}