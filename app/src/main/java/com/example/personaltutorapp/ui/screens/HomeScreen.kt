package com.example.personaltutorapp.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.example.personaltutorapp.ui.viewmodel.EnrolledCourse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    courseViewModel: CourseViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Learning Platform") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1E88E5),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF3F4F6))
            // Removed Modifier.verticalScroll() to avoid nesting with LazyColumn
        ) {
            CourseProgress(navController = navController, viewModel = courseViewModel)
        }
    }
}

@Composable
fun CourseProgress(navController: NavHostController, viewModel: CourseViewModel) {
    val enrolledCourses by viewModel.getEnrolledCourses().collectAsState(initial = emptyList())
    var isLoading by remember { mutableStateOf(true) }

    // Log when courses are received
    LaunchedEffect(enrolledCourses) {
        isLoading = false
        Log.d("CourseProgress", "Received ${enrolledCourses.size} enrolled courses: ${enrolledCourses.map { "${it.course.title} (status: ${it.enrollmentStatus})" }}")
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "My Course Progress",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            enrolledCourses.isEmpty() -> {
                Text(
                    text = "No enrolled or pending courses",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(enrolledCourses.filter { it.course.courseId != null && it.course.title.isNotEmpty() }) { enrolledCourse ->
                        enrolledCourse.course.courseId?.let { courseId ->
                            val progress by viewModel.getCourseProgress(courseId).collectAsState(initial = 0)
                            ProgressCard(
                                title = enrolledCourse.course.title,
                                progress = progress,
                                enrollmentStatus = enrolledCourse.enrollmentStatus,
                                courseId = courseId,
                                onClick = {
                                    if (enrolledCourse.enrollmentStatus == "accepted") {
                                        Log.d("CourseProgress", "Navigating to lessonScreen/$courseId")
                                        navController.navigate("lessonScreen/$courseId")
                                    } else {
                                        Log.d("CourseProgress", "Cannot navigate to lessonScreen/$courseId: Enrollment status is ${enrolledCourse.enrollmentStatus}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressCard(
    title: String,
    progress: Int,
    enrollmentStatus: String,
    courseId: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enrollmentStatus == "accepted", // Disable click for pending courses
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (enrollmentStatus == "accepted") "Enrolled" else "Pending Approval",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enrollmentStatus == "accepted") Color(0xFF4CAF50) else Color(0xFFFFA500),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$progress% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}