package com.example.personaltutorapp.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.personaltutorapp.model.CourseProgress
import com.example.personaltutorapp.model.Enrollment
import com.example.personaltutorapp.model.User
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.example.personaltutorapp.ui.viewmodel.EnrichedEnrollment
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
    var isInitialFetchComplete by remember { mutableStateOf(false) }

    LaunchedEffect(allCourses) {
        isInitialFetchComplete = true
    }

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
                tutorCourses.isEmpty() -> {
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
    val enrichedEnrollments by viewModel.getEnrichedEnrollmentsForCourse(courseId)
        .collectAsState(initial = emptyList())
    val pendingEnrollmentsCount = enrichedEnrollments.count { it.enrollment.status == "pending" }
    
    // Collect accepted enrollments only for display in the CourseProgress section
    val acceptedEnrollments = enrichedEnrollments.filter { it.enrollment.status == "accepted" }
    var showStudentProgress by remember { mutableStateOf(false) }

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
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedButton(
                onClick = {
                    val encodedTitle = URLEncoder.encode(course.title, StandardCharsets.UTF_8.name())
                    navController.navigate("enrollmentManagement/$courseId/$encodedTitle")
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Filled.People,
                    contentDescription = "Manage Enrollments",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Manage Enrollments ${if (pendingEnrollmentsCount > 0) "($pendingEnrollmentsCount Pending)" else ""}")
            }

            // Add CourseProgress button if there are accepted enrollments
            if (acceptedEnrollments.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showStudentProgress = !showStudentProgress },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(if (showStudentProgress) "Hide Student Progress" else "View Student Progress")
                }

                // Show student progress when expanded
                if (showStudentProgress) {
                    acceptedEnrollments.forEach { enrichedEnrollment ->
                        enrichedEnrollment.student?.let { student ->
                            student.id?.let { studentId ->
                                StudentProgressItem(
                                    courseId = courseId,
                                    student = student,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                    
                    if (acceptedEnrollments.isEmpty()) {
                        Text(
                            text = "No enrolled students yet",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("lessonCreation/$courseId") },
                modifier = Modifier.fillMaxWidth()
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
    }
}

// Uncomment and update StudentProgressRowItem to StudentProgressItem
@Composable
private fun StudentProgressItem(
    courseId: String,
    student: User,
    viewModel: CourseViewModel
) {
    val studentCustomId = student.id
    if (studentCustomId.isNullOrEmpty()) {
        Log.e("StudentProgressItem", "Student custom ID is null or empty. Cannot display progress for student: ${student.displayName}")
        Text("Error: Student ID missing for ${student.displayName}", color = MaterialTheme.colorScheme.error)
        return
    }

    val studentProgress by viewModel.getCourseProgressForStudent(courseId, studentCustomId)
        .collectAsState(initial = null)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = student.displayName.takeIf { !it.isNullOrEmpty() } ?: student.email ?: studentCustomId,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                studentProgress?.let { progress ->
                    if (progress.totalLessons > 0) {
                        val progressPercent = (progress.completedLessons * 100 / progress.totalLessons)
                        Text(
                            text = "Progress: $progressPercent%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            when {
                studentProgress == null -> {
                    Text("Loading progress...", style = MaterialTheme.typography.bodySmall)
                }
                studentProgress!!.totalLessons > 0 -> {
                    val currentProgressValue = studentProgress!!.completedLessons.toFloat() / studentProgress!!.totalLessons.toFloat()
                    LinearProgressIndicator(
                        progress = { currentProgressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(top = 4.dp),
                    )
                    Text(
                        text = "${studentProgress!!.completedLessons}/${studentProgress!!.totalLessons} lessons",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
                else -> {
                    Text("No lessons in course to track progress.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}