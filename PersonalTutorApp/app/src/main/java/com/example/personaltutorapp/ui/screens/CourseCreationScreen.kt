package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun CourseCreationScreen(viewModel: CourseViewModel, navController: NavController) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            viewModel.createCourse(title, description, subject)
            navController.popBackStack() // 返回上一頁
        }) {
            Text("Create Course")
        }
    }
}