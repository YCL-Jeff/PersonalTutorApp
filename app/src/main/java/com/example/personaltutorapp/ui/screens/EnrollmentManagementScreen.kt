package com.example.personaltutorapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.model.User
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.example.personaltutorapp.ui.viewmodel.EnrichedEnrollment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollmentManagementScreen(
    navController: NavController,
    courseId: String,
    courseTitle: String,
    viewModel: CourseViewModel = hiltViewModel()
) {
    val enrichedEnrollments by viewModel.enrichedEnrollments.collectAsState()
    val isLoading by viewModel.isLoadingEnrichedEnrollments.collectAsState()
    val error by viewModel.enrichedEnrollmentsError.collectAsState()

    LaunchedEffect(courseId) {
        Log.d("EnrollmentScreen", "Fetching enriched enrollments for courseId: $courseId")
        viewModel.fetchEnrichedEnrollmentsForCourse(courseId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manage Enrollments - $courseTitle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading enrollment requests...",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Failed to load enrollment data.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.fetchEnrichedEnrollmentsForCourse(courseId) }) {
                            Text("Retry")
                        }
                    }
                }
                enrichedEnrollments.isEmpty() -> {
                    Text(
                        text = "No enrollment requests at the moment.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(
                            items = enrichedEnrollments,
                            key = { enrichedItem ->
                                enrichedItem.enrollment.enrollmentId
                                    ?: enrichedItem.student?.uid
                                    ?: enrichedItem.hashCode().toString()
                            }
                        ) { enrichedItem ->
                            EnrollmentRequestItem(
                                enrichedEnrollment = enrichedItem,
                                courseTitleFromArg = courseTitle,
                                onAction = { enrollmentId, newStatus, student ->
                                    viewModel.updateEnrollmentStatus(enrollmentId, newStatus, student, courseTitle) { success, message ->
                                        snackbarMessage = message ?: if (success) {
                                            "Status updated to $newStatus"
                                        } else {
                                            "Failed to update status, please try again"
                                        }
                                        showSnackbar = true
                                        if (success) {
                                            viewModel.fetchEnrichedEnrollmentsForCourse(courseId)
                                        }
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

@Composable
private fun EnrollmentRequestItem(
    enrichedEnrollment: EnrichedEnrollment,
    courseTitleFromArg: String,
    onAction: (enrollmentId: String, newStatus: String, student: User?) -> Unit
) {
    val enrollment = enrichedEnrollment.enrollment
    val student = enrichedEnrollment.student

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Student: ${student?.displayName?.takeIf { it.isNotEmpty() }
                    ?: student?.id?.takeIf { it.isNotEmpty() } // Use 'id' instead of 'name'
                    ?: "Unknown Student (AuthUID: ${enrollment.studentId})"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            student?.email?.let { email ->
                Text(
                    text = "Email: $email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enrollment Status: ${enrollment.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (enrollment.status) {
                    "pending" -> MaterialTheme.colorScheme.tertiary
                    "accepted" -> Color(0xFF4CAF50)
                    "rejected" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (enrollment.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            enrollment.enrollmentId?.let { id ->
                                onAction(id, "rejected", student)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                    Button(
                        onClick = {
                            enrollment.enrollmentId?.let { id ->
                                onAction(id, "accepted", student)
                            }
                        }
                    ) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}