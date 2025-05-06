package com.example.personaltutorapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController // 使用 NavController 或 NavHostController
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel

@Composable
fun CourseListScreen(
    navController: NavController, // 保持 NavController
    viewModel: CourseViewModel,
    isStudent: Boolean = true,
    modifier: Modifier = Modifier // <<< 添加 Modifier 參數
) {
    var query by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    // 觀察過濾後的課程列表
    val courses by viewModel.filterCourses(query, subject).collectAsState(initial = emptyList())

    // 根 Composable (Column) 使用傳入的 modifier
    Column(
        modifier = modifier // <<< 使用傳入的 modifier
            .fillMaxSize() // 保持填滿大小
            .padding(16.dp) // 保持內邊距
    ) {
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search by Name") },
            modifier = Modifier.fillMaxWidth() // 讓 TextField 寬度填滿
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth() // 讓 TextField 寬度填滿
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 課程列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp) // 添加列表項間距
        ) {
            items(courses) { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    // .padding(8.dp), // Card 之間的間距由 LazyColumn 控制
                    onClick = {
                        if (isStudent) {
                            // 確保 courseId 可用且不為 null
                            course.courseId?.let { id ->
                                navController.navigate("lessonProgress/$id")
                            }
                        } else {
                            // 老師點擊課程卡片，可能跳轉到儀表板或其他管理頁面
                            navController.navigate("dashboard") // 或者 "courseManagement/${course.courseId}"
                        }
                    }
                ) {
                    // 卡片內容
                    Text(
                        text = course.title,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium // 使用主題樣式
                    )
                    // 可以添加更多課程資訊，例如科目
                    // Text(
                    //     text = "科目: ${course.subject}",
                    //     modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    //     style = MaterialTheme.typography.bodySmall,
                    //     color = Color.Gray
                    // )
                }
            }
        }
    }
}
