package com.example.personaltutorapp.ui.screens // 或者您放置 HomeScreen 的包

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController // 為了預覽需要
import com.example.personaltutorapp.ui.theme.PersonalTutorAppTheme // 為了預覽需要
import com.example.personaltutorapp.ui.viewmodel.AuthViewModel
import com.example.personaltutorapp.ui.viewmodel.CourseViewModel
import java.text.SimpleDateFormat // <<< 導入 SimpleDateFormat
import java.util.Calendar         // <<< 導入 Calendar
import java.util.Locale           // <<< 導入 Locale
import java.util.Date             // <<< 導入 Date (用於 Calendar 和 Formatter 互動)

// 主 Composable for the Home Screen
@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    courseViewModel: CourseViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // 根 Composable (Column) 使用傳入的 modifier
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .verticalScroll(rememberScrollState()) // 允許垂直滾動
    ) {
        HomeTopBar(
            onNotificationClick = { navController.navigate("notifications") },
            onSearchClick = { navController.navigate("search") }
        )

        // --- 使用修改後的 DateSelector ---
        DateSelector() // <<< 使用 Calendar 的版本

        TodayCourses(navController = navController)
        CourseProgress(navController = navController)
    }
}

// --- HomeTopBar (保持不變) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    onNotificationClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text("Learning Platform") },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF1E88E5),
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        actions = {
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }
    )
}


// --- 修改後的 DateSelector (使用 Calendar 和 SimpleDateFormat) ---
@Composable
fun DateSelector() {
    // 狀態：儲存當前選中的 Calendar 實例，初始為今天
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance(Locale.UK)) }

    // 格式化器 (使用 SimpleDateFormat)
    val topDateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.UK) }
    val dayNumberFormatter = remember { SimpleDateFormat("d", Locale.UK) }
    val dayNameFormatter = remember { SimpleDateFormat("E", Locale.UK) } // 'E' for short day name (e.g., "Mon")

    // 計算選中日期所在星期的星期一
    val startOfWeekCalendar = remember(selectedCalendar) {
        val cal = selectedCalendar.clone() as Calendar
        cal.firstDayOfWeek = Calendar.MONDAY // 設置星期一為一週的開始
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // 滾動到當週的星期一
        cal
    }

    // 生成本週一到週五的 Calendar 列表
    val weekDates = remember(startOfWeekCalendar) {
        List(5) { i ->
            val dayCal = startOfWeekCalendar.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_MONTH, i) // 從星期一開始加天數
            dayCal
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // 頂部日期顯示和切換按鈕
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左箭頭：切換到上一周
            IconButton(onClick = {
                val prevWeek = selectedCalendar.clone() as Calendar
                prevWeek.add(Calendar.WEEK_OF_YEAR, -1)
                selectedCalendar = prevWeek
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Previous week")
            }
            // 顯示選中的日期
            Text(
                text = topDateFormatter.format(selectedCalendar.time), // 使用 formatter 格式化 Calendar 的 Date
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // 右箭頭：切換到下一周
            IconButton(onClick = {
                val nextWeek = selectedCalendar.clone() as Calendar
                nextWeek.add(Calendar.WEEK_OF_YEAR, 1)
                selectedCalendar = nextWeek
            }) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Next week")
            }
        }
        Spacer(Modifier.height(8.dp))

        // 下方星期和日期選擇
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            weekDates.forEach { dateCal ->
                val isSelected = isSameDay(dateCal, selectedCalendar) // 判斷是否為選中日期
                val dayOfWeekText = dayNameFormatter.format(dateCal.time) // 獲取星期簡稱

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedCalendar = dateCal } // 點擊選擇該日期
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    // 顯示星期簡稱
                    Text(
                        text = dayOfWeekText,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    // 顯示日期數字
                    Text(
                        text = dayNumberFormatter.format(dateCal.time),
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

// Helper function to check if two Calendar instances represent the same day
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}


// --- TodayCourses, CourseCard, CourseProgress, ProgressCard (保持不變) ---
@Composable
fun TodayCourses(navController: NavHostController) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Today's Courses", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        CourseCard("Python Programming", "10:00 AM - 11:30 AM", "Ongoing") {
            // navController.navigate("courseDetail/python") // 實際導航邏輯
        }
        Spacer(modifier = Modifier.height(8.dp))
        CourseCard("Web Design Workshop", "2:00 PM - 3:30 PM", "Not Started") {
            // navController.navigate("courseDetail/web") // 實際導航邏輯
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCard(title: String, time: String, status: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                status,
                color = if (status == "Ongoing") Color(0xFF4CAF50) else Color.Gray,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CourseProgress(navController: NavHostController) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("My Course Progress", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        ProgressCard("Python Programming", 60) {
            // navController.navigate("lessonProgress/python_course_id") // 實際導航邏輯
        }
        Spacer(modifier = Modifier.height(8.dp))
        ProgressCard("Web Design Intro", 30) {
            // navController.navigate("lessonProgress/web_course_id") // 實際導航邏輯
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressCard(title: String, progress: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress / 100f }, // Lambda syntax for progress
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "$progress% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


// --- 預覽區 (現在應該可以正常工作) ---
@Preview(showBackground = true, name = "Home Screen Preview UK")
@Composable
fun HomeScreenPreview() {
    PersonalTutorAppTheme {
        // 預覽修改後的 DateSelector (或整個 HomeScreen)
        // 為了簡單起見，只預覽 DateSelector
        DateSelector()
        // 如果想預覽整個螢幕，但預覽不需要 NavController 或 ViewModel:
        /*
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
             HomeTopBar(onNotificationClick = {}, onSearchClick = {})
             DateSelector()
             // 可以添加靜態的 TodayCourses 和 CourseProgress 預覽
        }
        */
    }
}