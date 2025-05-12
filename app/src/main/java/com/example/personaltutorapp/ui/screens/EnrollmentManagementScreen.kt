package com.example.personaltutorapp.ui.screens

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
    val enrichedEnrollments by viewModel.getEnrichedEnrollmentsForCourse(courseId)
        .collectAsState(initial = emptyList())

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
                title = { Text("管理報名 - $courseTitle") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            if (enrichedEnrollments.isEmpty()) {
                Text(
                    text = "目前沒有報名請求。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            } else {
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
                                viewModel.updateEnrollmentStatus(enrollmentId, newStatus, student, courseTitle) { success ->
                                    snackbarMessage = if (success) {
                                        "狀態已更新為 $newStatus"
                                    } else {
                                        "更新失敗，請重試"
                                    }
                                    showSnackbar = true
                                }
                            }
                        )
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
    val student = enrichedEnrollment.student // User object with uid, id, displayName, email

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                // Prioritize displayName, fallback to 'id' (custom ID)
                text = "學生: ${student?.displayName?.takeIf { it.isNotEmpty() } ?: student?.id ?: "未知學生 (AuthUID: ${enrollment.studentId})"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            student?.email?.let {
                Text(
                    text = "郵箱: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "報名狀態: ${enrollment.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (enrollment.status) {
                    "pending" -> MaterialTheme.colorScheme.tertiary
                    "accepted" -> Color(0xFF4CAF50) // Green
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