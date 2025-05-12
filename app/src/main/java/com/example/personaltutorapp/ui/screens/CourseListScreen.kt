package com.example.personaltutorapp.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var filterDropdownExpanded by remember { mutableStateOf(false) }
    var isInitialFetchComplete by remember { mutableStateOf(false) }

    val filteredCourses: List<Course> by viewModel.filterCourses(searchQuery, selectedSubject)
        .collectAsState(initial = emptyList())
    val allCourses by viewModel.courses.collectAsState()

    // Update isInitialFetchComplete when courses are first fetched
    LaunchedEffect(allCourses) {
        if (allCourses.isNotEmpty()) {
            Log.d("CourseListScreen", "Initial courses fetched: ${allCourses.size}")
            isInitialFetchComplete = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var requestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val subjectsForFilter = listOf("", "Programming", "Math", "Science", "English", "History")

    LaunchedEffect(requestResult) {
        requestResult?.let { (success, message) ->
            snackbarHostState.showSnackbar(message)
            requestResult = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Course List") }
            )
        },
        floatingActionButton = {
            if (!isStudent) {
                FloatingActionButton(
                    onClick = { navController.navigate("courseCreation") },
                    modifier = Modifier.semantics { contentDescription = "Create New Course" }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create New Course Icon")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by Title") },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Search Course Titles" },
                    placeholder = { Text("Enter course title") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    IconButton(
                        onClick = { filterDropdownExpanded = true },
                        modifier = Modifier.semantics { contentDescription = "Filter by Subject" }
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Icon")
                    }
                    DropdownMenu(
                        expanded = filterDropdownExpanded,
                        onDismissRequest = { filterDropdownExpanded = false }
                    ) {
                        subjectsForFilter.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(if (subject.isEmpty()) "All Subjects" else subject) },
                                onClick = {
                                    selectedSubject = subject
                                    filterDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isInitialFetchComplete) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            "Loading courses...",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Found ${filteredCourses.size} courses",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (filteredCourses.isEmpty()) {
                    Text(
                        text = "No courses found matching the criteria.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = filteredCourses,
                            key = { course ->
                                course.courseId ?: run {
                                    Log.w("CourseListScreen", "Course ID is null for course: ${course.title}")
                                    course.hashCode().toString()
                                }
                            }
                        ) { course ->
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
    val courseProgress: Int by if (!isStudent && course.courseId != null) {
        viewModel.getCourseProgress(course.courseId!!).collectAsState(initial = 0)
    } else {
        remember { mutableStateOf(0) }
    }

    var isRequesting by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Confirmation dialog for enrollment
    if (showConfirmDialog && isStudent) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Enrollment") },
            text = { Text("Are you sure you want to enroll in '${course.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        course.courseId?.let { id ->
                            val userId = viewModel.getCurrentUserId()
                            if (userId.isEmpty()) {
                                Log.w("CourseItem", "User not logged in for enrollment request")
                                onRequestResult(false, "User not logged in")
                                showConfirmDialog = false
                                return@TextButton
                            }
                            isRequesting = true
                            viewModel.requestEnrollment(id, userId) { success ->
                                isRequesting = false
                                onRequestResult(
                                    success,
                                    if (success) "Enrollment request sent" else "Enrollment request failed"
                                )
                                showConfirmDialog = false
                            }
                        } ?: Log.e("CourseItem", "Course ID is null for ${course.title}")
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                course.courseId?.let { id ->
                    try {
                        if (isStudent) {
                            // Clicking the card does not trigger enrollment; handled by button
                        } else {
                            navController.navigate("lessonProgress/$id")
                        }
                    } catch (e: Exception) {
                        Log.e("CourseItem", "Navigation failed for course ID $id: ${e.message}", e)
                        onRequestResult(false, "Navigation error")
                    }
                } ?: Log.e("CourseItem", "Course ID is null: ${course.title}")
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
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
                color = MaterialTheme.colorScheme.outline
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
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (isStudent) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (!isRequesting && course.courseId != null) {
                            showConfirmDialog = true
                        }
                    },
                    enabled = !isRequesting && course.courseId != null,
                    modifier = Modifier
                        .align(Alignment.End)
                        .semantics { contentDescription = "Request to Join Course" }
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Request to Join")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        course.courseId?.let { id ->
                            navController.navigate("lessonProgress/$id")
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Manage Course")
                }
            }
        }
    }
}