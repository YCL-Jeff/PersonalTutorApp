package com.example.personaltutorapp.ui.screens

import android.net.Uri
import android.util.Log // 导入 Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCreationScreen(
    navController: NavController,
    viewModel: CourseViewModel = hiltViewModel(),
    courseId: String
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var order by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // 文件选择器
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // 检查文件大小（限制为 50MB）
            val fileSize = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    descriptor.statSize
                } ?: 0L
            } catch (e: Exception) {
                Log.e("LessonCreationScreen", "Error checking file size: ${e.message}", e)
                0L
            }
            if (fileSize > 50 * 1024 * 1024) { // 50MB 限制
                errorMessage = "File is too large. Please upload a file smaller than 50MB."
                return@let
            }

            mediaUri = it
            mediaType = when (uri.toString().substringAfterLast(".").lowercase()) {
                "pdf" -> "pdf"
                "mp3" -> "mp3"
                "mp4" -> "mp4"
                else -> null
            }
            if (mediaType == null) {
                errorMessage = "Unsupported file type. Please upload PDF, MP3, or MP4."
                mediaUri = null
            } else {
                errorMessage = null
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            snackbarHostState.showSnackbar("Lesson created successfully")
            showSuccessMessage = false
            navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Lesson") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            Text(text = "Create Lesson", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                isError = title.isBlank() && errorMessage != null,
                supportingText = { if (title.isBlank() && errorMessage != null) Text("Title is required") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth(),
                isError = content.isBlank() && errorMessage != null,
                supportingText = { if (content.isBlank() && errorMessage != null) Text("Content is required") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = order,
                onValueChange = { order = it },
                label = { Text("Order (e.g., 1, 2, 3)") },
                modifier = Modifier.fillMaxWidth(),
                isError = (order.isBlank() || order.toIntOrNull() == null) && errorMessage != null,
                supportingText = { if ((order.isBlank() || order.toIntOrNull() == null) && errorMessage != null) Text("Order must be a valid number") }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { filePicker.launch("*/*") },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload Media")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Media (PDF, MP3, MP4)")
                }
                if (mediaUri != null) {
                    IconButton(
                        onClick = {
                            mediaUri = null
                            mediaType = null
                            errorMessage = null
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear selected file"
                        )
                    }
                }
            }
            mediaUri?.let {
                Text(
                    text = "Selected: ${it.lastPathSegment}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (title.isBlank() || content.isBlank() || order.isBlank()) {
                        errorMessage = "All fields are required"
                        return@Button
                    }
                    val orderInt = order.toIntOrNull()
                    if (orderInt == null) {
                        errorMessage = "Order must be a valid number"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    viewModel.createLesson(
                        courseId = courseId,
                        title = title,
                        content = content,
                        order = orderInt,
                        mediaUri = mediaUri,
                        mediaType = mediaType
                    ) { success ->
                        isLoading = false
                        if (success) {
                            showSuccessMessage = true
                        } else {
                            errorMessage = "Failed to create lesson. Please check your network and try again."
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Text("Create Lesson")
                }
            }
        }
    }
}