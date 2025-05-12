package com.example.personaltutorapp.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book // Keep if used
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Keep if used
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel // Keep if used
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.example.personaltutorapp.ui.viewmodel.EnrolledCourse // Ensure this data class is defined

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(), // Keep if used
    courseViewModel: CourseViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Trigger fetch when the screen is first composed or when user logs in/out
    // authViewModel.currentUser is a StateFlow, so this will re-trigger if user state changes
    val currentUser by authViewModel.currentUser.collectAsState()
    LaunchedEffect(currentUser) { // Keyed on currentUser
        currentUser?.uid?.let {
            Log.d("HomeScreen", "Current user detected: $it, refreshing enrolled courses.")
            courseViewModel.refreshEnrolledCourses()
        } ?: run {
            // Handle case where user is null (logged out), e.g., clear courses or show login prompt
            Log.d("HomeScreen", "No current user, clearing or not fetching enrolled courses.")
            // courseViewModel.clearEnrolledCourses() // You might need a function like this
        }
    }

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
            modifier = modifier // Use the passed modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF3F4F6))
        ) {
            CourseProgress(navController = navController, viewModel = courseViewModel)
        }
    }
}

@Composable
fun CourseProgress(navController: NavHostController, viewModel: CourseViewModel) {
    val enrolledCourses by viewModel.enrolledCoursesUi.collectAsState() // Observe the new StateFlow
    val isLoading by viewModel.isLoadingEnrolledCourses.collectAsState()
    val error by viewModel.enrollmentError.collectAsState() // Observe the error StateFlow

    // Removed LaunchedEffect(Unit) that called refreshEnrolledCourses.
    // Refresh is now triggered by HomeScreen's LaunchedEffect(currentUser).

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "My Courses", // Changed title for clarity
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        when {
            isLoading -> { // Check loading state first
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading your courses...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "An unknown error occurred.", // Use the error message from ViewModel
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error, // Use theme error color
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { viewModel.refreshEnrolledCourses() }) {
                        Text("Retry")
                    }
                }
            }
            enrolledCourses.isEmpty() -> { // If not loading and no error, but list is empty
                Text(
                    text = "You are not enrolled in any courses yet, or no courses found.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = enrolledCourses.filter { it.course.courseId != null && it.course.title.isNotEmpty() },
                        key = { enrolledCourse -> enrolledCourse.course.courseId ?: enrolledCourse.hashCode().toString() }
                    ) { enrolledCourse ->
                        enrolledCourse.course.courseId?.let { courseId ->
                            val progress by viewModel.getCourseProgress(courseId).collectAsState(initial = 0)
                            Log.d("CourseProgress", "Rendering ProgressCard for course: ${enrolledCourse.course.title}, progress: $progress")
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
                                        // Optionally, show a snackbar here
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
        enabled = enrollmentStatus == "accepted",
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) // More distinct disabled state
        )
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
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false) // Prevent title from pushing status text too far
                )
                Spacer(modifier = Modifier.width(8.dp)) // Add some space
                Text(
                    text = if (enrollmentStatus == "accepted") "Enrolled" else "Pending Approval",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enrollmentStatus == "accepted") Color(0xFF4CAF50) else Color(0xFFFFA500), // Orange for pending
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End // Align status to the end
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Only show progress if accepted and progress is meaningful
            if (enrollmentStatus == "accepted") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant // Explicit track color
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
}
