package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.personaltutorapp.model.Course
import com.example.personaltutorapp.model.User
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@Composable
fun DashboardScreen(navController: NavController, viewModel: CourseViewModel) {
    val courses by viewModel.courses.collectAsState(initial = emptyList())
    
    // Get teacher courses (Assuming current user is tutor for this screen)
    // ViewModel's init should load courses based on user role

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Teacher Dashboard", 
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.logout {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("You haven't created any courses yet.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f) // Allow list to take available space
            ) {
            items(courses) { course ->
                    CourseStudentProgressCard(course = course, viewModel = viewModel)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp)) // Space before buttons
        
        Button(
            onClick = { navController.navigate("courseCreation") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create New Course")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { navController.navigate("testCreation") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Test")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = {
                viewModel.logout {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

@Composable
fun CourseStudentProgressCard(course: Course, viewModel: CourseViewModel) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    
    // 从LessonProgress获取学生进度
    val studentProgressMap by viewModel.getStudentProgressesFromLessonProgress(course.title)
        .collectAsState(initial = emptyMap())
    
    // 直接从Firestore获取的进度数据
    var directFirestoreProgress by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    // 记录日志用于调试
    LaunchedEffect(course.title) {
        android.util.Log.d("DashboardScreen", "课程${course.title}从LessonProgress获取到${studentProgressMap.size}个学生数据")
        
        // 作为备用，直接从Firestore获取数据
        viewModel.getStudentProgressesDirectlyFromFirestore(course.title) { result ->
            directFirestoreProgress = result
            android.util.Log.d("DashboardScreen", "课程${course.title}直接从Firestore获取到${result.size}个学生数据")
        }
    }
    
    // 从老方法获取的学生-进度数据，作为最后的备用
    val enrolledStudentsProgress by viewModel.getEnrolledStudentsWithProgress(course.courseId)
        .collectAsState(initial = emptyMap())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // 优先使用实时监听的数据
                if (studentProgressMap.isNotEmpty()) {
                    studentProgressMap.forEach { (studentName, progress) ->
                        LessonProgressStudentRow(studentName = studentName, progress = progress)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                // 其次使用直接从Firestore获取的数据
                else if (directFirestoreProgress.isNotEmpty()) {
                    directFirestoreProgress.forEach { (studentName, progress) ->
                        LessonProgressStudentRow(studentName = studentName, progress = progress)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                // 最后使用老方法获取的数据
                else if (enrolledStudentsProgress.isNotEmpty()) {
                    enrolledStudentsProgress.forEach { (user, progress) ->
                        StudentProgressRow(user = user, progress = progress)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                } 
                // 如果所有方法都没有获取到数据，显示无学生提示
                else {
                    Text("No students enrolled yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun LessonProgressStudentRow(studentName: String, progress: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = studentName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$progress%",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .width(100.dp)
                .padding(start = 8.dp)
        )
    }
}

@Composable
fun StudentProgressRow(user: User, progress: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = user.name.takeIf { it.isNotBlank() } ?: user.email ?: user.uid, // Display name, fallback to email/ID
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$progress%",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .width(100.dp)
                .padding(start = 8.dp)
        )
    }
}