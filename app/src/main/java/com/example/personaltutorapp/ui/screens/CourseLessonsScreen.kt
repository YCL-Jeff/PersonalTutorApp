package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.model.Lesson
import com.example.personaltutorapp.model.StudentLessonStatus
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import android.util.Log

@Composable
fun CourseLessonsScreen(
    courseId: Int,
    navController: NavController,
    viewModel: CourseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val course by viewModel.getCourseByIdFlow(courseId).collectAsState(initial = null)
    val lessons by viewModel.getLessonsByCourse(courseId).collectAsState(initial = emptyList())
    val currentUser by authViewModel.currentUser.collectAsState()
    val studentId = currentUser?.uid
    val studentEmail = currentUser?.email ?: ""
    
    // 获取课程名称
    val courseName = course?.title ?: ""
    
    // 从 LessonProgress 获取进度
    val progressFromLessonProgress by viewModel.getCourseProgressFromLessonProgress(studentEmail, courseName)
        .collectAsState(initial = 0)
    
    // 如果 LessonProgress 中没有进度，则使用 StudentLessonStatus 中的进度
    val courseProgress by studentId?.let {
        viewModel.getStudentProgressInCourse(it, courseId)
            .collectAsState(initial = progressFromLessonProgress)
    } ?: remember { mutableStateOf(progressFromLessonProgress) }
    
    // 获取学生课时完成状态
    val lessonStatusList by studentId?.let {
        viewModel.getLessonStatusesForStudent(it, courseId)
            .collectAsState(initial = emptyList())
    } ?: remember { mutableStateOf(emptyList<StudentLessonStatus>()) }
    
    LaunchedEffect(lessonStatusList) {
        Log.d("CourseLessonsScreen", "Lesson statuses: ${lessonStatusList.map { "Lesson ${it.lessonId}: ${it.isCompleted}" }}")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部导航栏
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
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course?.title ?: "Course Details",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = course?.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // 进度条
        LinearProgressIndicator(
            progress = { courseProgress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(8.dp)
        )
        
        Text(
            text = "Progress: $courseProgress%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.End)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 课时列表
        Text(
            text = "Lessons",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (lessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val sortedLessons = lessons.sortedBy { it.order }
            // 获取已完成课程的ID列表
            val completedLessonIds = lessonStatusList
                .filter { it.isCompleted }
                .map { it.lessonId }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sortedLessons) { lesson ->
                    val isCompleted = lessonStatusList.any { 
                        it.lessonId == lesson.lessonId && it.isCompleted 
                    }
                    
                    // 根据进度判断课程是否解锁
                    val isUnlocked = viewModel.isLessonUnlocked(lesson.order, courseProgress)
                    
                    LessonListItem(
                        lesson = lesson,
                        isCompleted = isCompleted,
                        isUnlocked = isUnlocked,
                        onClick = {
                            if (isUnlocked) {
                                navController.navigate("lessonScreen/$courseId/${lesson.lessonId}")
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Continue button (goes to the first unlocked incomplete lesson)
            val nextUnlockedLesson = sortedLessons.find { lesson ->
                // 未完成且已解锁的第一课
                val isCompleted = lessonStatusList.any { 
                    it.lessonId == lesson.lessonId && it.isCompleted 
                }
                
                val isUnlocked = viewModel.isLessonUnlocked(lesson.order, courseProgress)
                
                !isCompleted && isUnlocked
            }
            
            nextUnlockedLesson?.let { lesson ->
                Button(
                    onClick = {
                        navController.navigate("lessonScreen/$courseId/${lesson.lessonId}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (courseProgress == 0) "Start Course" else "Continue Learning")
                }
            } ?: run {
                // All available lessons completed
                if (completedLessonIds.size == sortedLessons.size) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Course Completed!")
                    }
                }
            }
        }
    }
}

@Composable
fun LessonListItem(
    lesson: Lesson,
    isCompleted: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked, onClick = onClick),
        colors = if (!isUnlocked) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        else 
            CardDefaults.cardColors()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.dp, Color.Gray, CircleShape)
                        .background(Color.Transparent, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (!isUnlocked) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Lesson ${lesson.order}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!isUnlocked) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!isUnlocked) {
                Text(
                    text = "Locked",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
} 