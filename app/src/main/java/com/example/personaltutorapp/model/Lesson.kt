//LessonScreen.kt
package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.example.personaltutorapp.ui.viewmodel.LessonWithProgress
import kotlinx.coroutines.flow.snapshotFlow
import kotlinx.coroutines.launch

@Composable
fun LessonScreen(viewModel: CourseViewModel, courseId: Int, lessonId: Int, navController: NavController) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val lessonsWithProgress by viewModel.getLessonsWithProgress(courseId).collectAsState(initial = emptyList())
    val currentLessonWithProgress = lessonsWithProgress.find { it.lesson.lessonId == lessonId }
    
    // 追踪滾動狀態
    val scrolledToBottom = remember { mutableStateOf(false) }
    val completionState = remember { mutableStateOf(false) }
    val isLoadingCompletion = remember { mutableStateOf(false) }
    val showError = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    
    // 組件初始化時設置完成狀態
    LaunchedEffect(currentLessonWithProgress) {
        currentLessonWithProgress?.let {
            completionState.value = it.isCompleted
        }
    }
    
    // 檢測是否滾動到底部
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }.collect { value ->
            if (!scrolledToBottom.value && value >= scrollState.maxValue - 100) {
                scrolledToBottom.value = true
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 標題和鎖定狀態
        currentLessonWithProgress?.let { lessonWithProgress ->
            val lesson = lessonWithProgress.lesson
            
            if (lessonWithProgress.isLocked) {
                // 課程被鎖定，顯示錯誤信息
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "You need to complete previous lessons first!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            } else {
                // 課程內容，可以正常訪問
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = lesson.content,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    // 在底部添加足夠的空間確保用戶必須滾動
                    Spacer(modifier = Modifier.height(500.dp))
                    Text(
                        text = "End of lesson. You can now mark it as completed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 滚动提示
                if (!scrolledToBottom.value && !completionState.value) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "Scroll to the end of the lesson to enable the completion button",
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // 底部的標記完成按鈕
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingCompletion.value) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                isLoadingCompletion.value = true
                                viewModel.markLessonAsCompleted(lessonId, scrolledToBottom.value) { success ->
                                    isLoadingCompletion.value = false
                                    if (success) {
                                        completionState.value = true
                                        // 顯示一個短暫的成功信息，然後返回
                                        coroutineScope.launch {
                                            // 在實際應用中，您可能會使用一個Snackbar
                                            // 這裡我們直接延遲後返回
                                            navController.popBackStack()
                                        }
                                    } else {
                                        errorMessage.value = if (!scrolledToBottom.value) {
                                            "You need to read to the end of the lesson first!"
                                        } else {
                                            "Failed to mark lesson as completed. Try again."
                                        }
                                        showError.value = true
                                    }
                                }
                            },
                            enabled = !completionState.value && scrolledToBottom.value,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scrolledToBottom.value && !completionState.value) 
                                    MaterialTheme.colorScheme.primary 
                                else if (completionState.value)
                                    MaterialTheme.colorScheme.secondary
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (scrolledToBottom.value || completionState.value) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = when {
                                    completionState.value -> "Completed"
                                    scrolledToBottom.value -> "Mark as Completed"
                                    else -> "Scroll to End to Enable"
                                }
                            )
                        }
                    }
                }
                
                // 錯誤信息
                if (showError.value) {
                    AlertDialog(
                        onDismissRequest = { showError.value = false },
                        title = { Text("Error") },
                        text = { Text(errorMessage.value) },
                        confirmButton = {
                            Button(
                                onClick = { showError.value = false }
                            ) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        } ?: run {
            // 課程不存在
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Lesson not found")
            }
        }
    }
}
