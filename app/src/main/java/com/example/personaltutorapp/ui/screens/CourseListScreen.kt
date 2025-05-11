package com.example.personaltutorapp.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    viewModel: CourseViewModel = hiltViewModel(),
    navController: NavController,
    isStudent: Boolean
) {
    val courses by viewModel.courses.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val filteredCourses by viewModel.filterCourses(searchQuery, selectedSubject).collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    var requestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val subjects = listOf("", "Programming", "Math", "Science", "English", "History")

    LaunchedEffect(requestResult) {
        requestResult?.let { (success, message) ->
            snackbarHostState.showSnackbar(message)
            if (success) navController.popBackStack()
            requestResult = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Courses") }
            )
        },
        floatingActionButton = {
            if (!isStudent) {
                FloatingActionButton(
                    onClick = { navController.navigate("courseCreation") },
                    modifier = Modifier.semantics { contentDescription = "Create new course" }
                ) {
                    Text("+")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search courses by title" },
                placeholder = { Text("Enter course title") },
                trailingIcon = {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.semantics { contentDescription = "Filter by subject" }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                subjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(if (subject.isEmpty()) "All Subjects" else subject) },
                        onClick = {
                            selectedSubject = subject
                            expanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Found ${filteredCourses.size} courses", style = MaterialTheme.typography.bodySmall)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (filteredCourses.isEmpty()) {
                    item {
                        Text(
                            text = "No courses found",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                } else {
                    items(filteredCourses) { course ->
                        CourseItem(
                            course = course,
                            viewModel = viewModel,
                            navController = navController,
                            isStudent = isStudent,
                            onRequestResult = { success, message ->
                                requestResult = Pair(success, message)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseItem(
    course: Course,
    viewModel: CourseViewModel,
    navController: NavController,
    isStudent: Boolean,
    onRequestResult: (Boolean, String) -> Unit
) {
    var courseProgress by remember { mutableStateOf(0) }
    var isRequesting by remember { mutableStateOf(false) }

    if (!isStudent) {
        LaunchedEffect(course.courseId) {
            course.courseId?.let { id ->
                viewModel.getCourseProgress(id).collect { progress ->
                    courseProgress = progress
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                course.courseId?.let { id ->
                    try {
                        if (isStudent) {
                            navController.navigate("enrollmentRequest/$id")
                        } else {
                            navController.navigate("lessonProgress/$id") // 改為 lessonProgress
                        }
                    } catch (e: Exception) {
                        Log.e("CourseItem", "Navigation failed for course $id: ${e.message}", e)
                        onRequestResult(false, "Navigation error")
                    }
                } ?: Log.e("CourseItem", "Course ID is null for ${course.title}")
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Subject: ${course.subject}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!isStudent) {
                LinearProgressIndicator(
                    progress = { courseProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Average Progress: $courseProgress%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    course.courseId?.let { id ->
                        if (isStudent) {
                            val userId = viewModel.getCurrentUserId()
                            if (userId.isEmpty()) {
                                onRequestResult(false, "User not logged in")
                                return@Button
                            }
                            isRequesting = true
                            viewModel.requestEnrollment(id, userId) { success ->
                                isRequesting = false
                                onRequestResult(
                                    success,
                                    if (success) "Request sent successfully" else "Failed to send request"
                                )
                            }
                        } else {
                            navController.navigate("lessonProgress/$id") // 改為 lessonProgress
                        }
                    }
                },
                enabled = !isRequesting,
                modifier = Modifier
                    .align(Alignment.End)
                    .semantics { contentDescription = if (isStudent) "Request to join course" else "View course lessons" }
            ) {
                Text(text = if (isStudent) "Request to Join" else "View Lessons")
            }
        }
    }
}