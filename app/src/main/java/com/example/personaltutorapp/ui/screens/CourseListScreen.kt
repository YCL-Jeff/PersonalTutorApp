package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import kotlinx.coroutines.flow.map
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(viewModel: CourseViewModel, navController: NavController, isStudent: Boolean) {
    val courses by viewModel.courses.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Courses") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isStudent) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("courseCreation")
                    }
                ) {
                    Text("+")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(courses) { course ->
                CourseItem(
                    course = course,
                    viewModel = viewModel,
                    navController = navController,
                    isStudent = isStudent
                )
            }
        }
    }
}

@Composable
fun CourseItem(
    course: Course,
    viewModel: CourseViewModel,
    navController: NavController,
    isStudent: Boolean
) {
    var courseProgress by remember { mutableStateOf(0) }
    
    // Collect course progress if teacher view
    if (!isStudent) {
        LaunchedEffect(course.courseId) {
            viewModel.getCourseProgress(course.courseId).collect { progress ->
                courseProgress = progress
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!isStudent) {
                // Show progress for teachers
                LinearProgressIndicator(
                    progress = { courseProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Completed $courseProgress%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        if (isStudent) {
                            navController.navigate("courseLessons/${course.courseId}")
                        } else {
                            navController.navigate("lessonProgress/${course.courseId}")
                        }
                    }
                ) {
                    Text(text = if (isStudent) "View Lessons" else "View Progress")
                }
            }
        }
    }
}