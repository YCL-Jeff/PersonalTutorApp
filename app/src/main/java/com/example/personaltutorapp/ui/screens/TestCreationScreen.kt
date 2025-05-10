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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import androidx.compose.foundation.clickable

data class EligibleStudent(
    val name: String,
    val email: String,
    val courseType: String
)

@Composable
fun TestCreationScreen(
    navController: NavController,
    viewModel: CourseViewModel = hiltViewModel()
) {
    // 获取所有进度为100%的学生
    val completedStudents = remember { mutableStateOf<List<EligibleStudent>>(emptyList()) }
    var selectedStudent by remember { mutableStateOf<EligibleStudent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var testCreated by remember { mutableStateOf(false) }
    
    // 当屏幕加载时，获取完成进度为100%的学生
    LaunchedEffect(Unit) {
        isLoading = true
        android.util.Log.d("TestCreationScreen", "正在加载完成课程的学生数据")
        viewModel.getStudentsWithFullProgress { students ->
            completedStudents.value = students
            android.util.Log.d("TestCreationScreen", "获取到 ${students.size} 个完成课程的学生")
            
            // 如果没有找到学生，尝试直接查询特定学生
            if (students.isEmpty()) {
                android.util.Log.d("TestCreationScreen", "未找到完成课程的学生，尝试直接查询特定学生")
                // 尝试直接查询 Jiamu 的记录
                viewModel.checkSpecificStudentProgress("1657542530@qq.com") { student ->
                    if (student != null) {
                        android.util.Log.d("TestCreationScreen", "直接查询到学生: ${student.name}, 课程: ${student.courseType}")
                        completedStudents.value = listOf(student)
                    } else {
                        android.util.Log.d("TestCreationScreen", "直接查询特定学生也未成功")
                    }
                }
            } else {
                students.forEach { student ->
                    android.util.Log.d("TestCreationScreen", "学生: ${student.name}, 课程: ${student.courseType}, 邮箱: ${student.email}")
                }
            }
            
            isLoading = false
        }
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
            
            Text(
                text = "Create Test",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (completedStudents.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No students have completed any courses yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else if (testCreated) {
            // 显示成功创建测试的消息
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Test created successfully!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    selectedStudent?.let {
                        Text(
                            text = "A ${it.courseType} test has been created for ${it.name}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Back to Dashboard")
                    }
                }
            }
        } else {
            // 显示学生列表和选择界面
            Text(
                text = "Students who have completed courses:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(completedStudents.value) { student ->
                    StudentTestCard(
                        student = student,
                        isSelected = student == selectedStudent,
                        onClick = { selectedStudent = student }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    selectedStudent?.let {
                        // 创建测试逻辑
                        testCreated = true
                    }
                },
                enabled = selectedStudent != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Test")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 添加返回教师主页按钮
            OutlinedButton(
                onClick = { navController.navigate("teacherDashboard") {
                    popUpTo("teacherDashboard") { inclusive = false }
                }},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("返回教师主页")
            }
        }
    }
}

@Composable
fun StudentTestCard(
    student: EligibleStudent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isSelected) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else 
            CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "Course: ${student.courseType}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Email: ${student.email}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Text(
                text = "100%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
} 