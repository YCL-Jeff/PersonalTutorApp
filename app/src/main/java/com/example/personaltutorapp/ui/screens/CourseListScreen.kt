package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val filteredCourses by viewModel.filterCourses(searchQuery, selectedSubject).collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    var requestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

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
                    onClick = { navController.navigate("courseCreation") }
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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = selectedSubject,
                onValueChange = { selectedSubject = it },
                label = { Text("Filter by subject") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
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
            .clickable {
                course.courseId?.let { id ->
                    if (isStudent) {
                        navController.navigate("enrollmentRequest/$id")
                    } else {
                        navController.navigate("courseProgress/$id")
                    }
                }
            }
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

            Text(
                text = "Subject: ${course.subject}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!isStudent) {
                LinearProgressIndicator(
                    progress = { courseProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Average Progress: $courseProgress%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
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
                            navController.navigate("courseProgress/$id")
                        }
                    }
                },
                enabled = !isRequesting,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = if (isStudent) "Request to Join" else "View Details")
            }
        }
    }
}