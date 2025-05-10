//LessonScreen.kt
package com.example.personaltutorapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import com.example.personaltutorapp.model.StudentLessonStatus
import com.example.personaltutorapp.model.Lesson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LessonScreen(
    viewModel: CourseViewModel = hiltViewModel(), 
    authViewModel: AuthViewModel = hiltViewModel(),
    courseId: Int, 
    lessonId: Int, 
    navController: NavController
) {
    val lessons by viewModel.getLessonsByCourse(courseId).collectAsState(initial = emptyList())
    val currentLesson by viewModel.currentLesson.collectAsState()
    val studentLessonStatus by viewModel.currentStudentLessonStatus.collectAsState()
    val course by viewModel.getCourseByIdFlow(courseId).collectAsState(initial = null)
    
    val currentUser by authViewModel.currentUser.collectAsState()
    val studentId = currentUser?.uid
    
    // 用于显示加载状态
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(lessonId, studentId) {
        viewModel.getLesson(lessonId, studentId)
    }

    val scrollState = rememberScrollState()
    val isAtBottom = remember { mutableStateOf(false) }
    
    // Check if scrolled to bottom
    LaunchedEffect(scrollState.maxValue, scrollState.value) {
        isAtBottom.value = scrollState.value >= scrollState.maxValue - 100
    }

    // 获取下一课信息
    val sortedLessons = lessons.sortedBy { it.order }
    val currentLessonIndex = sortedLessons.indexOfFirst { it.lessonId == lessonId }
    val hasNextLesson = currentLessonIndex < sortedLessons.size - 1
    val nextLessonId = if (hasNextLesson) sortedLessons[currentLessonIndex + 1].lessonId else -1
    
    // 完成课程后的导航逻辑
    val completeAndNavigate = {
        if (studentId != null) {
            // 显示加载中状态
            isLoading = true
            
            scope.launch {
                try {
                    // 调用ViewModel完成课程
                    var success = false
                    viewModel.completeLesson(studentId, lessonId, courseId) { result ->
                        success = result
                    }
                    
                    // 短暂延迟确保Firebase操作完成
                    delay(500)
                    
                    // 隐藏加载状态
                    isLoading = false
                    
                    // 导航到下一课
                    if (success && hasNextLesson) {
                        navController.navigate("lessonScreen/$courseId/$nextLessonId") {
                            popUpTo("lessonScreen/$courseId/$lessonId") { inclusive = true }
                        }
                    } else if (success) {
                        // 如果是最后一课，返回课程列表
                        navController.popBackStack()
                    }
                } catch (e: Exception) {
                    // 错误处理
                    Log.e("LessonScreen", "Error: ${e.message}")
                    isLoading = false
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            
            Text(
                text = currentLesson?.title ?: "Lesson",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Lesson content
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = currentLesson?.content ?: "Loading...",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        // Complete button at the bottom
        val isCompleted = studentLessonStatus?.isCompleted ?: false
        
        if (isCompleted) {
            if (hasNextLesson) {
                Button(
                    onClick = { 
                        navController.navigate("lessonScreen/$courseId/$nextLessonId") {
                            popUpTo("lessonScreen/$courseId/$lessonId") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Next Lesson")
                }
            } else {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Back to Lessons")
                }
            }
        } else {
            Button(
                onClick = { completeAndNavigate() },
                enabled = isAtBottom.value && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Processing..." else "Finish Lesson")
            }
        }
    }
}