package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: CourseViewModel = hiltViewModel()
) {
    val tutorCourses by viewModel.tutorCourses.collectAsState(initial = emptyList())
    val allCourses by viewModel.courses.collectAsState(initial = emptyList())
    val tutorId by viewModel.currentTutorCustomId.collectAsState()
    var isInitialFetchComplete by remember { mutableStateOf(false) }

    // Update isInitialFetchComplete when courses and tutor ID are fetched
    LaunchedEffect(allCourses, tutorId) {
        if (allCourses.isNotEmpty() || tutorId != null) {
            isInitialFetchComplete = true
        }
    }

    // Loading state: show loading only if fetch is not complete
    val isLoading = !isInitialFetchComplete

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutor Dashboard") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("courseCreation") }) {
                Icon(Icons.Filled.Add, contentDescription = "Create New Course")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "My Courses",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                "Loading your courses...",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                tutorCourses.isEmpty() && isInitialFetchComplete -> {
                    Text(
                        text = "You haven't created any courses yet. Click the '+' button to create a new course!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(tutorCourses, key = { course -> course.courseId ?: course.title }) { course ->
                            course.courseId?.let { courseId ->
                                TutorCourseItem(
                                    course = course,
                                    courseId = courseId,
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TutorCourseItem(
    course: Course,
    courseId: String,
    viewModel: CourseViewModel,
    navController: NavHostController
) {
    val enrolledStudents: List<Enrollment> by viewModel.getEnrolledStudents(courseId)
        .collectAsState(initial = emptyList())
    val pendingEnrollmentsCount = enrolledStudents.count { it.status == "pending" }

    val progress: Int by viewModel.getCourseProgress(courseId)
        .collectAsState(initial = 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Subject: ${course.subject}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Enrolled Students: ${enrolledStudents.filter { it.status == "accepted" }.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (pendingEnrollmentsCount > 0) {
                    Text(
                        text = "$pendingEnrollmentsCount Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Course Progress:",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { navController.navigate("lessonProgress/$courseId") }
                ) {
                    Text("View Lessons")
                }

                Button(
                    onClick = { navController.navigate("lessonCreation/$courseId") }
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add Lesson",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Lesson")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val encodedTitle = URLEncoder.encode(course.title, StandardCharsets.UTF_8.name())
                    navController.navigate("enrollmentManagement/$courseId/$encodedTitle")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.People,
                    contentDescription = "Manage Enrollments",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Manage Enrollment Requests ${if (pendingEnrollmentsCount > 0) "($pendingEnrollmentsCount)" else ""}")
            }
        }
    }
}